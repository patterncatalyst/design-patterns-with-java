---
title: "Resource and concurrency patterns"
order: 25
layout: tutorial
sectionid: docs
part: "concurrency-patterns-in-the-wild"
description: "Three idiomatic Java 25 shapes for bounding and serializing concurrent access, taught against a regional parcel-dispatch hub: a bounded connection pool with a blocking acquire, a monitor object that serializes a shared manifest, and an active object that decouples a call from the thread that runs it."
duration: "35 min"
---

## Learning objectives

By the end of this chapter you can:

- Build a bounded resource pool with a `Semaphore` as the hard cap and a
  `BlockingQueue` as the idle free list, and an `AutoCloseable` lease that
  returns a resource to the pool on every exit path, including an exception.
- Explain why pooling a scarce external resource stays worthwhile once
  virtual threads make the calling thread itself cheap to create, and what
  changes about the calculus.
- Build a monitor object that owns its own lock and serializes every public
  method against it, and use a `Condition` to let one thread wait for
  another thread's work to cross a threshold.
- Build an active object that enqueues a request and returns a result
  handle immediately, decoupling the call from whichever thread eventually
  executes it.
- Use `ThreadLocal` to confine a non-thread-safe collaborator to one
  instance per thread, and state the cost that confinement carries.
- Return a defensive, unmodifiable snapshot of shared mutable state instead
  of exposing the state itself, and say when that replaces a lock entirely.
- Recognize when none of these three patterns is needed: a cheap object
  does not belong in a pool, and a record with no shared mutable field does
  not need a lock.

{% include excalidraw.html name="25-bounded-resource-pool" caption="Figure 25.1: a bounded CarrierConnectionPool. A Semaphore caps outstanding connections at the pool's capacity; an idle queue hands a connection back to the next caller instead of building a new one; a caller past capacity blocks and times out rather than failing instantly or silently growing the pool." %}

## Why this matters

A regional dispatch hub calls out to a carrier's tracking API every time a
parcel scans through a checkpoint, and that call needs a connection that is
expensive to open: a DNS lookup, a TCP handshake, often a TLS negotiation
and an authenticated session, work measured in milliseconds against a
tracking update that itself takes microseconds once the connection exists.
Opening a fresh connection for every scan would mean the setup costs more
than the work it enables, every single time. The service also has a shared
dock where several warehouse scanners stage parcels onto the same manifest
at once, where an unsynchronized `add()` call loses updates under
contention. And it drives a physical label printer that can only execute
one job at a time, no matter how many requests arrive concurrently.

These are three different problems with one thing in common: something is
scarce, shared, or single-threaded by nature, and letting every caller
touch it directly breaks under concurrent load. A resource pool, a monitor
object, and an active object are the three idiomatic answers, and despite
looking nothing alike on the page, they belong in one chapter because each
one resolves the same underlying force: bound or serialize access to
something that cannot safely be handed out, shared, or invoked without
discipline. Two of the three, Monitor Object and Active Object, come from
Douglas Schmidt, Michael Stal, Hans Rohnert, and Frank Buschmann's
*Pattern-Oriented Software Architecture, Volume 2*, the catalogue most
production concurrency idioms trace back to, whether or not a codebase
credits the source. The resource pool predates any formal catalogue
entirely, and it is one of the most-used patterns in production software.

Virtual threads change part of this calculus and leave the rest alone.
Creating a thread to handle one request is now cheap enough that pooling
*threads* for that purpose is an anti-pattern: a virtual thread exists to be
created, used once, and discarded. None of that cheapness touches the
carrier connection on the other end of the wire, the shared manifest two
scanners write to at once, or the printer that only accepts one job at a
time. The pool, the monitor, and the active object protect that side of the
problem no matter how cheaply the calling thread was created.

## The domain: a regional dispatch hub

Every example in this chapter lives in `examples/25-resource-concurrency`
and shares one domain. A `CarrierApiConnection` stands in for the expensive
external resource; `CarrierConnectionPool` bounds how many exist at once. A
`DispatchDock` is the loading-dock bay several scanner threads stage
parcels onto; it is the monitor. A `LabelPrintActiveObject` accepts print
requests from any caller and runs them, one at a time, on its own dedicated
worker thread. A `TrackingCodeGenerator` and `DispatchDock`'s own snapshot
method carry the chapter's two supporting idioms, `ThreadLocal` and
defensive immutable copying.

## The bounded resource pool: cap it, reuse it, never leak it

`CarrierConnectionPool` holds two cooperating structures. A `Semaphore`
sized to the pool's capacity is the hard bound: a caller can only proceed
past `acquire()` once a permit is available, so the number of connections
outstanding at any moment can never exceed the capacity the pool was built
with, no matter how many callers contend for one. A `BlockingQueue` holds
whichever connections are currently idle, so a caller whose turn comes up
gets a connection someone already paid to open, not a brand new one.

```java
public Lease acquire(Duration timeout) throws InterruptedException, TimeoutException {
    if (!permits.tryAcquire(timeout.toNanos(), TimeUnit.NANOSECONDS)) {
        throw new TimeoutException(
                "no carrier connection available within " + timeout + " (capacity=" + capacity + ")");
    }
    CarrierApiConnection connection = idle.poll();
    if (connection == null) {
        connection = factory.create();
        created.incrementAndGet();
    }
    return new Lease(connection);
}
```

`tryAcquire(timeout, unit)` is doing the real work here: it blocks the
calling thread until a permit frees up or the timeout elapses, rather than
failing the instant every connection is in use. A caller that cannot wait
at all passes a zero timeout; a caller willing to wait a bounded amount
passes one. Either way the pool never silently grows past its capacity to
avoid making someone wait, which is exactly the bound a resource pool
exists to enforce.

`Lease` is the half of this idiom that makes it safe under an exception. It
implements `AutoCloseable`, so a caller writes a try-with-resources block
and the lease returns its connection to the idle queue and releases its
permit on every exit path, a normal return, an early return, or an
exception unwinding past it:

```java
try (CarrierConnectionPool.Lease lease = pool.acquire(Duration.ofSeconds(1))) {
    System.out.println(lease.connection().fetchTrackingStatus("SHM-7001"));
}  // close() runs here regardless of how the block exits
```

There is no separate `release(connection)` method anywhere on
`CarrierConnectionPool` for a caller to forget on an error path. That
absence is deliberate: a pool whose only way back to the idle queue is a
lease's `close()` method cannot leak a connection out of the pool the way a
pool with a parallel manual-release method can, where one missed call on
one exception branch permanently shrinks the pool by one.

## The monitor object: an object that serializes itself

`DispatchDock` answers a different question than the pool: not "how many
of these may exist," but "how do several threads touch the same mutable
state without corrupting it." A monitor object's defining trait is that it
owns its own lock and takes it at the top of every public method, so a
caller never sees the lock, never names it, and cannot forget to take it
before touching the dock's staged parcels:

```java
public void stageParcel(ParcelScan scan) {
    lock.lock();
    try {
        staged.add(scan);
        if (staged.size() >= bayCapacity) {
            bayFull.signalAll();
        }
    } finally {
        lock.unlock();
    }
}
```

`ReentrantLock` paired with an explicit `Condition` is one idiomatic way to
write a monitor; Java's intrinsic lock reached through `synchronized` is
the other, and the two trade capability for brevity in opposite directions.

{% include codetabs.html langs="synchronized|ReentrantLock + Condition" %}

```java
// synchronized: concise, but the intrinsic lock gives you exactly one
// implicit condition queue (wait()/notifyAll() on the object's own
// monitor). A dispatcher waiting for "bay full" and a future waiter for
// "bay empty" would have to share that single queue and filter spurious
// wakeups themselves.
public synchronized void stageParcel(ParcelScan scan) {
    staged.add(scan);
    if (staged.size() >= bayCapacity) {
        notifyAll();
    }
}
```

```java
// ReentrantLock + Condition: more ceremony, but bayFull is a condition
// named for exactly the event the dispatcher cares about, awaitNanos()
// accepts a Duration-derived timeout directly, and a second, independent
// condition could be added later without the first one's waiters noticing.
public void stageParcel(ParcelScan scan) {
    lock.lock();
    try {
        staged.add(scan);
        if (staged.size() >= bayCapacity) {
            bayFull.signalAll();
        }
    } finally {
        lock.unlock();
    }
}
```

`DispatchDock` needs the explicit form because `awaitFullBay()` is a timed,
named wait: the dispatcher calls `bayFull.awaitNanos(remaining)` in a loop
that rechecks the staged count on every wakeup, which is what makes the
wait immune to a spurious wakeup and to a timeout that should fail loudly
rather than hang forever. `synchronized` and intrinsic `wait()` could
express the unconditional case, but once a chapter needs a named condition
with its own timeout, `ReentrantLock` and `Condition` are the idiomatic
reach.

The example's concurrency test proves the invariant directly: eight threads
each stage two hundred and fifty parcels against the same dock, and the
final staged count is exactly two thousand, every run, because the lock
inside `stageParcel()` makes a torn or lost update on the backing
`ArrayList` impossible rather than merely unlikely.

## The active object: decoupling the call from the thread that runs it

`LabelPrintActiveObject` answers a third question: what happens when a
caller wants to say "print this" and move on immediately, while the actual
printing happens on its own timeline, one request after another, on a
thread the caller never touches. `submitPrint()` wraps the request in a
task, pushes it onto a private `BlockingQueue`, and returns a
`CompletableFuture` without running anything on the calling thread:

```java
public CompletableFuture<PrintReceipt> submitPrint(ShippingLabelRequest request) {
    CompletableFuture<PrintReceipt> result = new CompletableFuture<>();
    requests.add(() -> {
        PrintReceipt receipt = new PrintReceipt(
                request.parcelId(), sequence.incrementAndGet(), Thread.currentThread().getName());
        result.complete(receipt);
    });
    return result;
}
```

A single dedicated worker thread, started once in the constructor, pulls
requests off that queue in submission order and runs them one at a time,
the way a physical printer can only execute one job regardless of how many
requests arrive. The example's test suite proves the decoupling two ways:
the receipt's `printedByThread` field never matches the submitting test
thread's name, and when several threads submit concurrently, every
submission's sequence number comes back distinct and contiguous from one to
the total, proof that one queue and one worker linearized every call no
matter how many callers raced to enqueue one.

An active object's worker gains nothing from being a virtual thread; there
is exactly one of it, running for the program's whole lifetime, so making
it virtual would add nothing a platform thread does not already provide.
Virtual threads earn their keep when a program needs thousands of them
blocked on I/O at once, not when it needs one dedicated, long-lived
timeline.

## Supporting idioms: ThreadLocal and defensive snapshots

`TrackingCodeGenerator` hashes a parcel id with `MessageDigest`, an object
that is neither thread-safe nor cheap to obtain through
`MessageDigest.getInstance`. A lock around one shared digest would
serialize every caller for no reason tied to the hash itself; a fresh
`getInstance()` call on every invocation pays a provider lookup repeatedly.
`ThreadLocal` is the idiom that splits the difference: each calling thread
gets its own `MessageDigest`, created once and reused across every call
that thread makes afterward, with no lock anywhere in the path.

```java
private final ThreadLocal<MessageDigest> digestPerThread = ThreadLocal.withInitial(() -> {
    try {
        return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException("SHA-256 is a mandatory JDK algorithm", e);
    }
});
```

That confinement has a cost. A `ThreadLocal` value
lives exactly as long as the thread that created it, so reuse only pays off
when the same thread calls `trackingCodeFor()` more than once. A bounded
worker pool handling thousands of requests amortizes one retained
`MessageDigest` per worker across all of them. A virtual thread created
fresh for a single request gets a brand new `MessageDigest` that is used
exactly once and then discarded along with the thread, paying the
allocation this pattern exists to avoid and keeping nothing in return. A
`ThreadLocal` attached to a virtual-thread-per-task executor is a strong
signal to look again at whether the value should be passed as a parameter
instead.

`DispatchDock.awaitFullBay()` carries this chapter's second supporting
idiom. Instead of returning its internal, mutable `List<ParcelScan>`, it
returns `List.copyOf(staged)`, an immutable snapshot a reader can hand to
another thread without a lock protecting it, because nothing can mutate it
after the copy is made:

```java
List<ParcelScan> manifest = List.copyOf(staged);
staged.clear();
return manifest;
```

This is the alternative to locking, not a companion to it: once a piece of
state is captured as an immutable value, sharing it safely needs no
synchronization at all, because there is no mutation left to race against.
A monitor object protects state that changes under concurrent writers; a
defensive, unmodifiable copy protects a value that, once made, never
changes again.

> **AI sidebar.** Ask an LLM pairing partner for a "thread-safe connection
> pool" and a frequent answer returns a raw object from `acquire()` with a
> separate `release(connection)` method the caller must remember to call on
> every exit path, including the exception paths generated code rarely
> walks through. That shape reopens exactly the leak a `Lease` closes: one
> forgotten `release()` call in an error branch shrinks the pool by one,
> permanently, until no connection is left to hand out. Read a generated
> pool against this chapter's shape: does `acquire()` return something
> `AutoCloseable` that a caller uses in try-with-resources, with no parallel
> "give it back" method sitting next to it to forget? A model asked for a
> "thread-safe cache" or "shared counter" in Java also reaches for
> `synchronized` by reflex, because it is the shortest answer that compiles.
> That answer is correct for an unconditional lock, but the moment the
> design needs a caller to wait for a specific condition with a timeout,
> check whether the generated code reached for `ReentrantLock` and a named
> `Condition` instead of layering a manual spin-and-sleep loop on top of
> `synchronized`, which is the shape that shows up when a model tries to
> retrofit a timed wait onto the wrong primitive.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this
loop. This example is standalone-buildable from its own directory:

```sh
cd examples/25-resource-concurrency
mvn test
```

It is also picked up by the repository aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 19 JUnit 5 cases, asserted with AssertJ,
including a concurrency suite that spins up virtual threads and platform
threads to exercise the pool's bound, the dock's invariant, and the active
object's ordering guarantee under real contention rather than a single
sequential call. Observed locally on Fedora with Temurin 25.0.2:

```
Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see all three patterns run end to end rather than reading an assertion
that says they work, run the example's own demo script:

```sh
examples/25-resource-concurrency/demo.sh
```

```
== Connection pool: bounded, blocking, reused ==
status for SHM-7001 via connection-1
status for SHM-7002 via connection-2
Connections created so far: 2
Reused connection id 2; still only 2 connection(s) ever created

== Monitor object: DispatchDock serializes concurrent scanners ==
Bay reached capacity; dispatcher received 4 parcels

== Active object: LabelPrintActiveObject decouples call from execution ==
submitPrint returned immediately on com.patterncatalyst.designpatterns.ch25.dispatch.DispatchHubDemo.main()
PrintReceipt[parcelId=SHM-9001, sequence=1, printedByThread=label-print-worker]
PrintReceipt[parcelId=SHM-9002, sequence=2, printedByThread=label-print-worker]

== ThreadLocal: a per-thread MessageDigest instance ==
SHM-7001 -> <hex code>
SHM-7002 -> <hex code>
```

The lease that comes back second from the try-with-resources block in the
pool section closes first, since Java closes multiple resources in reverse
declaration order, so the reused connection is id 2, not id 1. Both print
receipts show `printedByThread=label-print-worker`, never the thread that
called `submitPrint()`, which is the active object's decoupling showing up
as an observable fact at the terminal, not just as a passing test.

## When not to use it

A resource pool pays for its complexity only when building the resource
costs more than using it, and when demand is bursty enough that reuse saves
real work. A `CarrierApiConnection` clears that bar; a cheap-to-construct
value never should be pooled, because the pool's own bookkeeping, the
semaphore, the idle queue, the lease wrapper, costs more cycles and more
reader attention than constructing a fresh instance every time. If nothing
about building the resource shows up in a profile, skip the pool and
construct directly. The same caution applies doubly to pooling the calling
thread itself once virtual threads are on the table: a virtual thread is
meant to be created and discarded per task, and a pool of them defeats the
entire reason they are cheap.

A monitor object is the right choice when several threads call the *same*
object's methods directly and concurrently, and when the state those
methods touch needs to stay internally consistent across more than one
field at a time. It is the wrong choice when only one thread ever reaches
the object, where the lock's overhead buys nothing, and it is also the
wrong choice when the state in question never changes after it is built. A
`Shipment` or a `ParcelScan` with no setter does not need a lock anywhere
near it; a record with every field final has already solved the sharing
problem a monitor exists to solve, by removing the mutation a lock would
otherwise have to serialize.

An active object is the right choice when a caller must never block on
submission and the receiving side needs one serialized timeline: a
printer, a single outbound device, hardware that cannot accept concurrent
commands. It is overkill for work with no ordering requirement between
tasks and no reason a caller could not wait; that case is better
served by submitting directly to a thread pool or a virtual-thread executor
and letting the caller decide whether to block on the result. Across all
three patterns, the same test applies: name the plain alternative, a direct
constructor call, an unsynchronized method, a synchronous call that blocks
the caller, next to the pattern under consideration, and let a real
measured cost or a real concurrent-access bug decide between them, not the
pattern's familiarity.

## For deeper coverage

- Douglas C. Schmidt, Michael Stal, Hans Rohnert, and Frank Buschmann,
  [*Pattern-Oriented Software Architecture, Volume 2: Patterns for Concurrent and Networked Objects*](https://www.wiley.com/en-us/Pattern-Oriented+Software+Architecture,+Volume+2,+Patterns+for+Concurrent+and+Networked+Objects-p-9781118725177)
  (Wiley, 2000): the original catalogue entries for Monitor Object and
  Active Object this chapter teaches as Java idioms.
- Brian Goetz, Tim Peierls, Joshua Bloch, Joseph Bowbeer, David Holmes, and
  Doug Lea, [*Java Concurrency in Practice*](https://www.pearson.com/en-us/subject-catalog/p/java-concurrency-in-practice/P200000009262)
  (Addison-Wesley, 2006): the deep mechanics behind `Semaphore`,
  `ReentrantLock`, `Condition`, and `ThreadLocal` this chapter uses without
  re-deriving, including the reasoning behind defensive copying for shared
  state.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/)
  and [AssertJ documentation](https://assertj.github.io/doc/): the testing
  idiom behind `examples/25-resource-concurrency`'s suite, which asserts
  bound, order, and invariant guarantees under real concurrent load rather
  than a single sequential call.
- The full reading list behind this book's opinions lives on the
  [bibliography page]({{ '/bibliography/' | relative_url }}); each citation
  there is a pointer to the full book, not a substitute for reading it.
