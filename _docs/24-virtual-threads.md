---
title: "Virtual threads"
order: 24
layout: tutorial
sectionid: docs
part: "concurrency-patterns-in-the-wild"
description: "What a virtual thread is, why fanning a shipment out to a hundred carrier rate APIs stops being expensive, and the concrete cases, CPU-bound work, pooling, pinning, ThreadLocal at scale, where a virtual thread is the wrong tool."
duration: "35 min"
---

## Learning objectives

By the end of this chapter you can:

- Explain what a virtual thread is in terms of mounting and unmounting a
  carrier (platform) thread, and why that makes blocking I/O nearly free.
- Fan a request out to many blocking collaborators at once with
  `Executors.newVirtualThreadPerTaskExecutor()` and gather every result.
- Cap how many blocking calls are in flight at once with a `Semaphore`
  without reintroducing a bounded thread pool.
- Guard a shared, mutable collaborator that many virtual threads touch
  concurrently with a `ReentrantLock`, and say precisely what that buys you
  on current Java that `synchronized` does not.
- Name the forces that make a workload a poor fit for virtual threads: pure
  CPU-bound computation, pooling them, the narrowed but still-real set of
  pinning causes, and `ThreadLocal` used as a per-thread object cache at
  huge scale.

{% include excalidraw.html name="24-mount-unmount-carriers" caption="Figure 24.1: A handful of carrier (platform) threads serving many virtual threads. Each virtual thread mounts a carrier to run Java code and unmounts the moment it blocks, freeing that carrier for the next task." %}

## Why this matters

A checkout flow that needs a shipping quote from every carrier under
contract has a concurrency problem hiding behind a sequential-looking loop.
Call eight carriers' rate APIs one after another and the checkout waits for
the sum of eight network round trips. Call them with one platform thread
each and the checkout waits for the slowest one, but now it has spent eight
operating-system threads, each with its own megabyte-scale stack, on work
that is almost entirely waiting, not computing. Scale that to a hundred
carriers, or a thousand concurrent checkouts each quoting eight carriers,
and the platform-thread approach runs out of threads before it runs out of
carriers.

A virtual thread resolves that force directly. It is a `Thread` the JVM
schedules onto a small, fixed pool of real operating-system threads, called
carrier threads, and when a virtual thread blocks on I/O the JVM unmounts it
from its carrier and hands that carrier to a different virtual thread that
is ready to run. The blocked virtual thread itself continues to exist as a
small object on the heap, costing nothing in OS resources, until its
blocking operation completes and it remounts, often on a different carrier
than the one it started on. The code inside the virtual thread never knows
any of this happened. It is still the same imperative, blocking,
exception-throwing code a thread-per-request design has always been; only
the cost model underneath it changed. That is the whole appeal: a design
that was expensive at platform-thread scale becomes cheap at virtual-thread
scale with no rewrite to asynchronous or reactive style.

## The domain: quoting a shipment across many carriers

This chapter's example lives in `examples/24-virtual-threads`. A `Shipment`
is a record carrying a shipment identifier and a weight in kilograms.
`RateQuote` is a record pairing a carrier identifier with a quoted amount in
cents. `CarrierGateway` is the one-method contract a real integration would
satisfy with a blocking HTTP call to one carrier's rate API:

```java
@FunctionalInterface
public interface CarrierGateway {
    RateQuote fetchQuote(Shipment shipment, String carrierId) throws InterruptedException;
}
```

`SimulatedCarrierGateway` stands in for the real network call with a
randomized `Thread.sleep` of 120 to 300 milliseconds, long enough to make
the difference between calling carriers concurrently and calling them
sequentially visible at the terminal. Everything downstream, the fan-out
service and its tests, works against the `CarrierGateway` interface and
never needs to know whether a call is simulated or real.

## Fan-out with one virtual thread per carrier call

`RateQuoteFanOutService.fanOut` builds one `Callable<RateQuote>` per carrier
and hands the whole list to an executor created by
`Executors.newVirtualThreadPerTaskExecutor()`:

```java
public List<RateQuote> fanOut(Shipment shipment, List<String> carrierIds) throws InterruptedException {
    List<Callable<RateQuote>> tasks = carrierIds.stream()
            .map(carrierId -> (Callable<RateQuote>) () -> gateway.fetchQuote(shipment, carrierId))
            .toList();
    return runAll(tasks);
}

private List<RateQuote> runAll(List<Callable<RateQuote>> tasks) throws InterruptedException {
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
        List<Future<RateQuote>> futures = executor.invokeAll(tasks);
        List<RateQuote> quotes = new ArrayList<>(futures.size());
        for (Future<RateQuote> future : futures) {
            quotes.add(resultOf(future));
        }
        return List.copyOf(quotes);
    }
}
```

This executor does three things that matter individually. First,
`newVirtualThreadPerTaskExecutor()` does exactly what its name says: every
task submitted to it, `invokeAll` here, gets its own brand-new virtual
thread, never a thread recycled from a pool. Second, `ExecutorService` has
implemented `AutoCloseable` since Java 19, so the try-with-resources block
closes the executor automatically; `close()` requests an orderly shutdown
and waits for every submitted task to finish, which by the time control
reaches the end of the block it already has, because `invokeAll` itself
blocks until every task completes or throws. Third, `invokeAll` returns a
`Future` per task in submission order regardless of completion order, which
is why `resultOf` calls `future.get()` on each one in turn rather than
racing them: the order of the returned list tracks the order of
`carrierIds`, and a caller that wants the cheapest quote can still find it
with a single pass.

Eight carriers launch eight virtual threads here, but the design does not
change shape at a hundred carriers or a thousand. The JVM's default virtual
thread scheduler is a work-stealing pool sized to the number of available
processors; it carries far more virtual threads than that at once by
unmounting the ones that are blocked, which in a rate-quote fan-out is
nearly all of them, nearly all of the time.

## Bounding how many calls are in flight

Not every carrier wants a hundred simultaneous connections. A real rate API
might cap concurrent requests from one client, and respecting that cap is a
different problem from how many virtual threads a program is allowed to
create. `fanOutBounded` still launches one virtual thread per carrier; it
narrows how many of those virtual threads are allowed to be inside the
blocking call at the same time, with a `Semaphore`:

```java
public List<RateQuote> fanOutBounded(Shipment shipment, List<String> carrierIds, int maxConcurrentCalls)
        throws InterruptedException {
    Semaphore inFlight = new Semaphore(maxConcurrentCalls);
    List<Callable<RateQuote>> tasks = carrierIds.stream()
            .map(carrierId -> (Callable<RateQuote>) () -> {
                inFlight.acquire();
                try {
                    return gateway.fetchQuote(shipment, carrierId);
                } finally {
                    inFlight.release();
                }
            })
            .toList();
    return runAll(tasks);
}
```

A virtual thread that cannot acquire a permit blocks on `inFlight.acquire()`
rather than on the carrier call itself, and that block is exactly as cheap
as any other: the JVM unmounts it from its carrier while it waits for a
permit to free up. The example's test suite proves the bound holds by
having every carrier call rendezvous at a `CyclicBarrier` sized to the
limit; the barrier can only fill, and release, once that many calls are
concurrently past the semaphore gate, which both demonstrates the cap is
reached and, by construction of the semaphore in the code under test,
proves it is never exceeded. Running `examples/24-virtual-threads/demo.sh`
shows the effect directly: capping eight carriers to three in flight at once
takes noticeably longer than the unbounded run, because only three calls
are ever blocking at the same instant.

## A shared cache under concurrent load

A fan-out often wants to track the cheapest quote seen so far while calls
are still coming back, and that tracker is a single object every virtual
thread in the fan-out shares. `CarrierRateCache` guards a
`Map<String, RateQuote>` with a `java.util.concurrent.locks.ReentrantLock`
rather than a `synchronized` block:

{% include codetabs.html langs="synchronized|ReentrantLock" %}

```java
// synchronized: a monitor-based critical section. Correct, and since JDK 24
// (JEP 491) a virtual thread that blocks while holding this monitor no
// longer pins its carrier the way it used to on JDK 21 through 23.
private final Object lock = new Object();

public boolean recordIfCheaper(RateQuote quote) {
    synchronized (lock) {
        RateQuote existing = cheapestByCarrier.get(quote.carrierId());
        if (existing == null || quote.quotedCents() < existing.quotedCents()) {
            cheapestByCarrier.put(quote.carrierId(), quote);
            return true;
        }
        return false;
    }
}
```
```java
// ReentrantLock: the same critical section, plus tryLock(), which a
// synchronized block has never offered at any JDK version.
private final ReentrantLock lock = new ReentrantLock();

public boolean tryRecordIfCheaper(RateQuote quote) {
    if (!lock.tryLock()) {
        return false;
    }
    try {
        return recordIfCheaperWhileLocked(quote);
    } finally {
        lock.unlock();
    }
}
```

Before JDK 24, the right-hand column was close to mandatory advice for any
virtual-thread-heavy codebase. A virtual thread that blocked while holding a
`synchronized` monitor pinned its carrier, because the JVM tracked monitor
ownership by the carrier thread rather than the virtual thread riding it;
block inside `synchronized` for long enough, or often enough, across enough
virtual threads, and a program could starve its own carrier pool.
[JEP 491](https://openjdk.org/jeps/491), delivered in JDK 24 and present in
Java 25, rearchitected monitor ownership around the virtual thread itself,
so a virtual thread can now unmount while blocked inside a `synchronized`
block or waiting on `Object.wait()` without pinning anything. Pinning has
not disappeared; it has narrowed to native boundaries the JVM cannot see
across: a virtual thread calling into native code through JNI or
the Foreign Function and Memory API that calls back into blocking Java
code, most legacy synchronous file I/O, and class loading triggered from
inside a blocking class initializer. None of those three involve
`synchronized` at all, so swapping `synchronized` for `ReentrantLock`
specifically to dodge pinning is no longer the argument it once was.

`ReentrantLock` is still the right choice in `CarrierRateCache`, for a
reason that has nothing to do with pinning: `tryLock()`. `tryRecordIfCheaper` lets
a virtual thread that only wants a best-effort update skip the write
entirely if the lock is already held, rather than queue behind whichever
virtual thread holds it. A `synchronized` block offers no equivalent; every
thread that reaches it either acquires the monitor or blocks until it can.
Reach for `ReentrantLock` when you need `tryLock`, a timed acquisition, an
interruptible acquisition, or more than one `Condition` on the same lock.
Reach for `synchronized` for an ordinary critical section with none of
those needs; on current Java, neither choice has a pinning penalty the
other lacks.

Structured concurrency, which lets a group of virtual threads spawned for
one unit of work be cancelled, joined, and have errors propagated together
as a single structured block, is a promising complement to this pattern for
exactly this kind of fan-out. It remains a preview feature through Java 25,
still accumulating API changes release to release, so this book's examples
stick to the stable `ExecutorService` shape above; revisit it once it
finalizes.

> **AI sidebar.** Ask an LLM pairing partner for "the fix for virtual thread
> pinning" and a confident answer naming `synchronized` and recommending a
> blanket swap to `ReentrantLock` is a strong sign the model is drawing on
> training data from before JDK 24 shipped. That answer was correct advice
> for JDK 21 through 23 and is outdated for Java 25, where JEP 491 already
> closed the `synchronized` pinning case the advice is built around. The
> fix for a model giving you a plausible but stale answer is the same fix
> that works on a human collaborator making the same mistake: ask which JDK
> version the claim holds for, and check the JEP or the Oracle
> documentation for the version you are shipping on rather than accepting a
> rule of thumb that used to be true.

## When not to use virtual threads

Virtual threads solve a concurrency problem, not a computation problem, and
applying them outside that scope either does nothing or actively hurts.

CPU-bound work gets no benefit from a virtual thread, and can get worse
throughput from one. The entire advantage comes from unmounting a thread
the instant it blocks and remounting another one in its place; a tight loop
computing a hash, compressing a buffer, or running a matrix multiply never
calls anything the JVM recognizes as a blocking operation, so it never
unmounts. It runs start to finish pinned to one carrier, exactly like a
platform thread would, except now it is competing with every other virtual
thread for a carrier pool sized to the processor count rather than running
on a thread pool sized and tuned for CPU-bound work. A bounded
platform-thread pool, `Executors.newFixedThreadPool` sized to the available
cores, or a `ForkJoinPool` for parallel divide-and-conquer work, remains the
correct tool once the work is compute, not wait.

Pooling virtual threads is a direct anti-pattern, not a style preference.
The entire cost model behind this chapter assumes a virtual thread is
created once per task and discarded, cheaply, when the task finishes; a
pool of long-lived virtual threads reused across many unrelated requests
reintroduces the problems virtual threads exist to remove; a thread wedged
on an earlier task's `ThreadLocal` state, a fixed pool size that caps
concurrency exactly the way a platform-thread pool did, and an object
sitting idle in memory for the sole purpose of being reused later, which is
precisely what a cheap-to-create, cheap-to-discard virtual thread is meant
to replace. `Executors.newVirtualThreadPerTaskExecutor()` enforces the
correct shape by construction: there is no size to configure, because the
right size is one thread per task, always.

`ThreadLocal` is still fully supported on virtual threads, but a pattern
that was reasonable at platform-thread scale can become expensive at
virtual-thread scale. Caching an expensive, mutable, not-thread-safe object
in a `ThreadLocal`, a database connection, a `SimpleDateFormat`, a
reusable buffer, made sense when a program had a few dozen platform threads
and each one could own a cached instance for its lifetime. The same idiom
against a million short-lived virtual threads means a million of that
expensive object gets built and discarded rather than a few dozen. The
object a `ThreadLocal` was caching to avoid creating needs a different
home, a shared pool sized to the carrier count rather than the task count,
or a connection pool the virtual threads borrow from rather than each
owning one outright. For data that only needs to flow down a call stack to
whatever the current thread calls, rather than being cached and mutated,
`ScopedValue`, finalized in Java 25 alongside virtual threads' continued
maturation, is the narrower, immutable-by-design replacement Oracle's own
virtual-thread guidance now points toward.

Finally, pinning is real on Java 25, just smaller than it used to be. A
virtual thread that calls into native code through JNI or the Foreign
Function and Memory API, and that native code calls back into blocking Java
code, pins its carrier for the duration. So does most legacy synchronous
file I/O, which still goes through a native call the JVM cannot unmount
around, and class loading triggered by a blocking class initializer. None
of those three are things `fanOut` or `fanOutBounded` do in this chapter's
example, which is itself the point: an HTTP or database client call is the
common case virtual threads were built for, and it is the case that
unmounts cleanly. A codebase doing heavy synchronous file I/O or frequent
JNI calls from inside a virtual-thread-per-task design is the case worth
profiling with the `jdk.VirtualThreadPinned` JFR event before assuming
virtual threads alone fixed its concurrency problem.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this
loop. From the repository root, the aggregator picks up
`examples/24-virtual-threads` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/24-virtual-threads
mvn test
```

Either path runs the same 14 JUnit 5 cases, asserted with AssertJ. The two
concurrency tests prove their point with `CyclicBarrier` rendezvous points
rather than sleeps or elapsed-time assertions: a barrier sized to the
expected concurrency can only release once that many calls have arrived
concurrently, so a regression that accidentally serialized the fan-out
would hang the test rather than pass it quietly, caught by a
deadlock-guard timeout rather than a flaky timing threshold. Observed
locally on Fedora with Temurin 25.0.2:

```
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see the concurrency pay off as wall-clock time rather than just a
passing assertion, run the example's own demo script:

```sh
examples/24-virtual-threads/demo.sh
```

It fans a shipment out to eight simulated carriers twice, once unbounded
and once capped at three calls in flight:

```
== Fan-out across 8 carriers, one virtual thread per carrier call ==
  acme: 1924 cents
  bolt: 1924 cents
  crate: 1961 cents
  dash: 1924 cents
  ember: 1961 cents
  flux: 1924 cents
  granite: 2035 cents
  harbor: 1998 cents
Elapsed: 301 ms for 8 blocking calls (each sleeps 120-300 ms; a sequential loop would take roughly 1600 ms)
Cheapest overall: RateQuote[carrierId=acme, quotedCents=1924]

== Bounded fan-out: at most 3 carrier calls in flight at once ==
Received 8 quotes in 700 ms with concurrency capped at 3 (still one virtual thread per carrier; only the number of carrier calls allowed to block at once is limited)
```

The unbounded run finishes in roughly the time of the single slowest
carrier call, not the sum of all eight; the bounded run takes longer, by
design, because only three calls are ever blocking at once.

## For deeper coverage

- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444): the JEP that
  finalized virtual threads in JDK 21, including its explicit guidance
  against pooling them.
- [JEP 491: Synchronize Virtual Threads without Pinning](https://openjdk.org/jeps/491):
  the JDK 24 change behind this chapter's corrected pinning guidance, with
  the remaining native-boundary pinning cases this chapter names.
- [Virtual Threads, Oracle's Java documentation](https://docs.oracle.com/en/java/javase/25/core/virtual-threads.html):
  the vendor documentation behind this chapter's `ThreadLocal`-at-scale and
  `ScopedValue` guidance.
- Kirill Bobrov, [*Grokking Concurrency*](https://www.manning.com/books/grokking-concurrency)
  (Manning, 2024): language-agnostic concurrency fundamentals, mounting,
  scheduling, blocking versus non-blocking, that transfer directly to
  reasoning about what a virtual thread scheduler is doing underneath this
  chapter's Java-specific API.
- The full reading list behind this book's opinions lives on the
  [bibliography page]({{ '/bibliography/' | relative_url }}); each citation
  there is a pointer to the full book, not a substitute for reading it.
