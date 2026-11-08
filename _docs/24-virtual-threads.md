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

- Explain a virtual thread as mounting and unmounting a carrier (platform) thread, and why that makes blocking I/O cheap.
- Fan a request out to many blocking collaborators with `Executors.newVirtualThreadPerTaskExecutor()` and gather every result.
- Cap how many blocking calls are in flight with a `Semaphore`, without a bounded thread pool.
- Guard a shared collaborator with a `ReentrantLock`, and say what it buys over `synchronized` now that `synchronized` no longer pins.
- Name the workloads that fit virtual threads badly: CPU-bound work, pooled virtual threads, the remaining pinning causes, and `ThreadLocal` caches at huge scale.

{% include excalidraw.html name="24-mount-unmount-carriers" caption="Figure 24.1: A handful of carrier (platform) threads serving many virtual threads. Each virtual thread mounts a carrier to run Java code and unmounts the moment it blocks, freeing that carrier for the next task." %}

## Why this matters

A checkout that needs a quote from every contracted carrier has a concurrency problem behind a sequential loop. Call eight rate APIs one after another and the checkout waits for the sum of eight round trips. Give each call a platform thread and it waits only for the slowest, but it spends eight operating-system threads, each with a megabyte-scale stack, on work that's almost all waiting. At a hundred carriers, or a thousand checkouts quoting eight each, platform threads run out first.

A virtual thread is a `Thread` the JVM schedules onto a small pool of operating-system threads called carrier threads. When it blocks on I/O, the JVM unmounts it and gives the carrier to another virtual thread that's ready. The blocked thread sits on the heap as a small object until its operation completes, then remounts, often on a different carrier. The code inside is the same imperative, blocking, exception-throwing code thread-per-request has always been; only the cost underneath changed, with no rewrite to asynchronous or reactive style.

## The domain: quoting a shipment across many carriers

The code lives in `examples/24-virtual-threads`. `Shipment` is a record with an identifier and a weight in kilograms; `RateQuote` pairs a carrier identifier with an amount in cents. `CarrierGateway` is the contract a real integration satisfies with a blocking HTTP call:

```java
@FunctionalInterface
public interface CarrierGateway {
    RateQuote fetchQuote(Shipment shipment, String carrierId) throws InterruptedException;
}
```

`SimulatedCarrierGateway` stands in for the network with a random `Thread.sleep` of 120 to 300 ms, enough to make concurrent and sequential calls differ visibly. Everything downstream depends only on `CarrierGateway`.

## Pattern: Virtual-thread fan-out

### The problem

A checkout needs quotes from eight carriers, each a blocking call of up to 300 ms. Sequentially that's up to 2.4 seconds. A fixed platform-thread pool caps how many checkouts can quote at once, and an asynchronous rewrite changes every signature from the gateway up.

### The solution

**A virtual-thread fan-out gives each blocking call its own new virtual thread and waits for all of them, so the request takes as long as its slowest call.**

Use it when:

- the work is mostly waiting on I/O: HTTP, database or message-broker calls;
- the number of calls varies per request, from a few to thousands;
- you want blocking code that reads top to bottom.

### How Java models it

`RateQuoteFanOutService.fanOut` builds one `Callable<RateQuote>` per carrier and hands the list to a virtual-thread-per-task executor:

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

Three properties matter:

- **One new thread per task.** The executor never recycles a thread from a pool.
- **The executor closes itself.** `ExecutorService` has been `AutoCloseable` since JDK 19; `close()` shuts down and waits for submitted tasks, which `invokeAll` has already waited for.
- **Results arrive in submission order.** `invokeAll` returns one `Future` per task in the order of `carrierIds`, whatever order they finish in.

### Scale

The design keeps its shape at a hundred or a thousand carriers. The default scheduler is a work-stealing pool sized to the available processors, and it carries far more virtual threads than that because nearly all of them are unmounted, blocked on a carrier call, nearly all the time.

## Pattern: Bounded in-flight calls

### The problem

A carrier's rate API may cap concurrent requests from one client. Shrinking a thread pool to respect that cap would bring back the pool the fan-out removed, and would limit unrelated work too.

### The solution

**A `Semaphore` around the blocking call limits how many virtual threads are inside it at once, while every task still gets its own virtual thread.**

### How Java models it

`fanOutBounded` launches one virtual thread per carrier and gates the call with permits:

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

A thread waiting for a permit blocks on `acquire()`, and that block is as cheap as any other: the JVM unmounts it until a permit frees up. The cap is the semaphore's permit count, reached and never exceeded. Capping eight carriers at three takes longer than the unbounded run, because only three calls block at once.

{% include excalidraw.html name="24-virtual-thread-timeline" caption="Figure 24.2: the same eight carrier calls over wall-clock time. Unbounded, all eight start at once and the fan-out finishes with the slowest call, around 301 ms; bounded to three in flight, a 4th, 5th, and 6th call only start once a Semaphore permit frees up, stretching the same eight calls to roughly 700 ms." %}

## Shared state under concurrent load

A fan-out often tracks the cheapest quote seen so far while calls return, in one object every virtual thread shares. `CarrierRateCache` guards a `Map<String, RateQuote>` with a `java.util.concurrent.locks.ReentrantLock`:

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

On JDK 21 through 23, a virtual thread that blocked inside `synchronized` pinned its carrier, because the JVM tracked monitor ownership by carrier thread; enough of that starved the carrier pool, so the advice was to swap to `ReentrantLock`. [JEP 491](https://openjdk.org/jeps/491), delivered in JDK 24, ties monitor ownership to the virtual thread, so it can unmount while blocked in `synchronized` or `Object.wait()`. Avoiding pinning is no longer a reason to prefer `ReentrantLock`.

`CarrierRateCache` uses `ReentrantLock` for `tryLock()`: `tryRecordIfCheaper` skips a best-effort update when the lock is held instead of queueing behind the holder. `synchronized` has no equivalent.

| Use | When you need |
| --- | --- |
| `synchronized` | An ordinary critical section |
| `ReentrantLock` | `tryLock`, a timed or interruptible acquire, or more than one `Condition` on a lock |

Structured concurrency, which joins, cancels and propagates errors for a group of virtual threads as one block, fits this kind of fan-out. It's still a preview API in JDK 25, so `RateQuoteFanOutService` uses the stable `ExecutorService` shape.

> **🤖 Explore with your agent**
>
> "In `examples/24-virtual-threads`, add a `fanOutWithDeadline(Shipment, List<String>, Duration)` method to `RateQuoteFanOutService` that returns the quotes that arrive before the deadline. Use `Executors.newVirtualThreadPerTaskExecutor()` in try-with-resources and `invokeAll` with a timeout, keep one new virtual thread per carrier call, and keep the result in the order of the carrier ids. Don't create a fixed or cached thread pool, don't pool virtual threads, don't use structured concurrency (it's a preview API), and don't replace `ReentrantLock` in `CarrierRateCache` with `synchronized` or the reverse."
>
> Check: `mvn -q -f examples/24-virtual-threads/pom.xml test -Dtest='RateQuoteFanOutService*'`, then confirm the diff has no `newFixedThreadPool` or `newCachedThreadPool`.

## When not to use virtual threads

| Case | Why it's a poor fit | Use instead |
| --- | --- | --- |
| CPU-bound work (hashing, compression, matrix math) | It never blocks, so it never unmounts, and it competes for a carrier pool sized to the processor count | `Executors.newFixedThreadPool` sized to the cores, or a `ForkJoinPool` for divide-and-conquer |
| Pooling virtual threads | A fixed pool caps concurrency like a platform pool, threads carry an earlier task's `ThreadLocal` state, and idle threads sit in memory to be reused | `newVirtualThreadPerTaskExecutor()`, which has no size to configure |
| `ThreadLocal` as a per-thread cache of an expensive object | A connection, a `SimpleDateFormat` or a buffer gets built a million times for a million short-lived threads | A shared pool sized to the resource; `ScopedValue` (final in JDK 25) for immutable data passed down a call stack |
| Heavy native calls or file I/O | These still hold the carrier | Profile with the `jdk.VirtualThreadPinned` JFR event before assuming virtual threads fixed the problem |

Pinning still exists, at native boundaries the JVM can't unmount across: a virtual thread calling native code through JNI or the Foreign Function and Memory API, and blocking during class initialization. Synchronous file I/O doesn't pin, but it captures the carrier for the call, and the scheduler compensates by temporarily adding carriers. None of these involve `synchronized`, and none occur in `fanOut` or `fanOutBounded`. An HTTP or database client call is the case virtual threads were built for, and it unmounts cleanly.

## Build, run, observe

From the repository root:

```sh
mvn -f examples/pom.xml test
```

Or standalone:

```sh
cd examples/24-virtual-threads
mvn test
```

All 14 tests pass. The concurrency checks rendezvous at a `CyclicBarrier` sized to the expected concurrency instead of measuring elapsed time, so they don't depend on timing. The demo fans one shipment out to eight simulated carriers, unbounded and then capped at three:

```sh
examples/24-virtual-threads/demo.sh
```

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

Elapsed times vary by run. The unbounded run takes about as long as the slowest call; the bounded run takes about twice as long, since only three calls block at once. One experiment, in a scratch copy:

- **Remove the gate.** Delete the `inFlight.acquire()` and `inFlight.release()` calls in `fanOutBounded`. One test fails: more calls than the limit are in flight at once.

## For deeper coverage

- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444): the JEP that
  finalized virtual threads in JDK 21, including its explicit guidance
  against pooling them.
- [JEP 491: Synchronize Virtual Threads without Pinning](https://openjdk.org/jeps/491):
  the JDK 24 change behind the current pinning guidance, with the
  remaining native-boundary pinning cases.
- [Virtual Threads, Oracle's Java documentation](https://docs.oracle.com/en/java/javase/25/core/virtual-threads.html):
  the vendor documentation behind the `ThreadLocal`-at-scale and
  `ScopedValue` guidance.
- Kirill Bobrov, [*Grokking Concurrency*](https://www.manning.com/books/grokking-concurrency)
  (Manning, 2024): language-agnostic concurrency fundamentals, mounting,
  scheduling, blocking versus non-blocking, that transfer directly to
  reasoning about what a virtual thread scheduler does underneath the
  Java API.
- The full reading list lives on the
  [bibliography page]({{ '/bibliography/' | relative_url }}).
