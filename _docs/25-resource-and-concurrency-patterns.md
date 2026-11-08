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

- Build a bounded resource pool with a `Semaphore` as the cap, a `BlockingQueue` as the idle list, and an `AutoCloseable` lease that returns the resource on every exit path.
- Explain why pooling a scarce external resource stays worthwhile when virtual threads make the calling thread cheap.
- Build a monitor object that owns its lock, and use a `Condition` to wait for another thread's work to cross a threshold.
- Build an active object that enqueues a request and returns a result handle at once.
- Confine a non-thread-safe collaborator with `ThreadLocal`, and state what that costs.
- Return an unmodifiable snapshot of shared state instead of the state itself.
- Recognize when none of the three patterns is needed.

{% include excalidraw.html name="25-bounded-resource-pool" caption="Figure 25.1: a bounded CarrierConnectionPool. A Semaphore caps outstanding connections at the pool's capacity; an idle queue hands a connection back to the next caller instead of building a new one; a caller past capacity blocks and times out rather than failing instantly or silently growing the pool." %}

## Why this matters

A regional dispatch hub has three things that break under concurrent load:

- **Carrier connections.** Every parcel scan calls a carrier's tracking API. Opening a connection costs a DNS lookup, a TCP handshake, often TLS and an authenticated session, milliseconds against an update that takes microseconds once connected.
- **A shared dock.** Several scanners stage parcels onto one manifest at once, and an unsynchronized `add()` loses updates.
- **A label printer.** It runs one job at a time, however many requests arrive.

Each is scarce, shared or single-threaded, and each needs bounded or serialized access. A resource pool, a monitor object and an active object are the answers. Monitor Object and Active Object come from Schmidt, Stal, Rohnert and Buschmann's *Pattern-Oriented Software Architecture, Volume 2*; the resource pool predates any catalogue.

Virtual threads make the calling thread cheap enough that pooling threads is an anti-pattern. They don't make the carrier connection, the shared manifest or the printer any less scarce. The three patterns protect that side.

## The domain: a regional dispatch hub

The code lives in `examples/25-resource-concurrency`:

| Type | Role |
| --- | --- |
| `CarrierApiConnection` | The expensive external resource |
| `CarrierConnectionPool` | Bounds how many connections exist |
| `DispatchDock` | The monitor several scanner threads stage parcels onto |
| `LabelPrintActiveObject` | Accepts print requests and runs them one at a time on its own worker |
| `TrackingCodeGenerator` | Uses `ThreadLocal` for a per-thread digest |

## Pattern: Resource Pool

### The problem

Each scan needs a carrier connection. Opening one per scan makes setup cost more than the work it enables. Letting the number of connections grow without limit exhausts the carrier's or the hub's capacity under a burst.

### The solution

**A resource pool hands out a bounded set of expensive resources, reuses idle ones, and makes a caller past capacity wait instead of growing the set.**

### How Java models it

`CarrierConnectionPool` combines a `Semaphore` sized to the capacity, which no caller passes without a permit, and a `BlockingQueue` of idle connections, so a caller gets one someone already paid to open:

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

`tryAcquire(timeout, unit)` blocks until a permit frees up or the timeout passes. A caller that can't wait passes a zero timeout. The pool never grows past capacity to spare someone the wait.

### A lease that can't leak

`Lease` implements `AutoCloseable`, so try-with-resources returns the connection to the idle queue and releases the permit on every exit path: a normal return, an early return or an exception:

```java
try (CarrierConnectionPool.Lease lease = pool.acquire(Duration.ofSeconds(1))) {
    System.out.println(lease.connection().fetchTrackingStatus("SHM-7001"));
}  // close() runs here regardless of how the block exits
```

`CarrierConnectionPool` has no `release(connection)` method. With a manual release, one missed call on one exception branch shrinks the pool by one, permanently. With the lease's `close()` as the only way back, nothing leaks.

## Pattern: Monitor Object

### The problem

Eight scanners call `stageParcel` on the same dock at once. The backing `ArrayList` isn't thread-safe, and a dispatcher has to wait until the bay is full. Leaving the locking to callers means one caller who forgets corrupts the manifest.

### The solution

**A monitor object owns its lock and takes it in every public method, so callers never see, name or forget the lock.**

### How Java models it

`DispatchDock` locks at the top of each public method:

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

Eight threads staging 250 parcels each end with exactly 2,000 staged, on every run.

### Intrinsic lock or explicit `Condition`

Java offers two ways to write a monitor, and they trade brevity for capability:

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

`DispatchDock` needs the explicit form because `awaitFullBay()` is a timed, named wait. The dispatcher calls `bayFull.awaitNanos(remaining)` in a loop that rechecks the staged count on every wakeup, so a spurious wakeup can't release it early and a timeout fails instead of hanging. `synchronized` with `wait()` covers the unconditional case; a named condition with its own timeout calls for `ReentrantLock` and `Condition`.

## Pattern: Active Object

### The problem

Callers want to say "print this" and carry on. The printer accepts one job at a time. A synchronous call blocks every caller until its label prints, and concurrent callers would send overlapping jobs.

### The solution

**An active object queues each request and returns a result handle at once, while its own thread runs the requests one at a time.**

### How Java models it

`submitPrint()` wraps the request in a task, adds it to a private `BlockingQueue`, and returns a `CompletableFuture` without running anything on the caller's thread:

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

One worker thread, started in the constructor, takes requests in submission order. A receipt's `printedByThread` is never the submitting thread, and concurrent submissions get distinct sequence numbers running from one to the total, because one queue and one worker linearize every call.

The worker gains nothing from being virtual. There's one of it, alive for the program's lifetime; virtual threads pay off when thousands block on I/O at once.

{% include excalidraw.html name="25-active-object-decoupling" caption="Figure 25.2: three callers enqueue onto the same BlockingQueue and get a CompletableFuture back immediately; one dedicated worker thread drains the queue FIFO and completes each future later, off the caller's own thread. A direct synchronous call, by contrast, blocks each caller until the print finishes." %}

## Supporting idioms: ThreadLocal and defensive snapshots

`TrackingCodeGenerator` hashes a parcel id with `MessageDigest`, which isn't thread-safe and costs a provider lookup through `getInstance`. A lock around one shared digest serializes every caller; a fresh `getInstance()` per call repeats the lookup. `ThreadLocal` gives each thread its own digest, created once and reused with no lock:

```java
private final ThreadLocal<MessageDigest> digestPerThread = ThreadLocal.withInitial(() -> {
    try {
        return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException("SHA-256 is a mandatory JDK algorithm", e);
    }
});
```

The value lives as long as its thread, so it pays off only when one thread calls `trackingCodeFor()` repeatedly. A bounded worker pool amortizes one digest per worker over thousands of requests. A virtual thread created per request builds a digest, uses it once and discards it. A `ThreadLocal` on a virtual-thread-per-task executor is a cue to pass the value as a parameter instead.

`DispatchDock.awaitFullBay()` returns `List.copyOf(staged)` rather than its internal list. The snapshot is immutable, so any thread can read it without a lock:

```java
List<ParcelScan> manifest = List.copyOf(staged);
staged.clear();
return manifest;
```

The snapshot replaces locking for that value. A monitor protects state that changes under concurrent writers; an unmodifiable copy protects a value that never changes after it's made.

> **🤖 Explore with your agent**
>
> "In `examples/25-resource-concurrency`, add `awaitEmptyBay(Duration timeout)` to `DispatchDock`. Add a second `Condition` named `bayEmpty` on the existing `ReentrantLock`, signal it in `awaitFullBay()` after `staged.clear()`, and wait with `bayEmpty.awaitNanos(remaining)` in a loop that rechecks `staged.isEmpty()` on every wakeup and throws `IllegalStateException` when the time runs out, as `awaitFullBay` does. Take the lock at the top of the method like every other public method. Don't use `synchronized`, `wait()`/`notifyAll()` or a sleep loop, and don't return or expose the internal `staged` list."
>
> Check: `mvn -q -f examples/25-resource-concurrency/pom.xml test -Dtest=DispatchDockTest`, and confirm `bayFull` waiters are untouched.

## When not to use it

| Pattern | Use it when | Skip it when |
| --- | --- | --- |
| Resource Pool | Building the resource costs more than using it, and demand is bursty | Construction doesn't show in a profile; the pool's semaphore, queue and lease cost more than a fresh instance. Never pool virtual threads |
| Monitor Object | Several threads call the same object and its state must stay consistent across fields | Only one thread reaches the object, or the state never changes after construction: a `ParcelScan` record with final fields needs no lock |
| Active Object | Callers must never block on submission and the receiver needs one serialized timeline (a printer, a single device) | Tasks have no ordering requirement and callers can wait; submit to an executor and let the caller decide whether to block |

For each pattern, put the plain alternative next to it: a direct constructor call, an unsynchronized method, a synchronous call. Let a measured cost or a concurrent-access bug decide.

## Build, run, observe

```sh
cd examples/25-resource-concurrency
mvn test
```

Or through the aggregator:

```sh
mvn -f examples/pom.xml test
```

All 19 tests pass, including the concurrent checks of the pool's bound, the dock's count and the active object's ordering, run on virtual and platform threads. The demo runs all three patterns:

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
SHM-7001 -> bb78dcce4c
SHM-7002 -> 2f0e7adb21
```

The demo opens two leases in one try-with-resources block, and Java closes resources in reverse order, so connection 2 returns to the idle queue first and is the one reused. Both receipts were printed by `label-print-worker`, never by the caller. One experiment, in a scratch copy:

- **Drop the monitor's lock.** Remove `lock.lock()` and `lock.unlock()` from `stageParcel`. Two tests fail: concurrent scans lose parcels, and `signalAll()` throws `IllegalMonitorStateException` without the lock held.

## For deeper coverage

- Douglas C. Schmidt, Michael Stal, Hans Rohnert, and Frank Buschmann,
  [*Pattern-Oriented Software Architecture, Volume 2: Patterns for Concurrent and Networked Objects*](https://www.wiley.com/en-us/Pattern-Oriented+Software+Architecture,+Volume+2,+Patterns+for+Concurrent+and+Networked+Objects-p-9781118725177)
  (Wiley, 2000): the original catalogue entries for Monitor Object and
  Active Object.
- Brian Goetz, Tim Peierls, Joshua Bloch, Joseph Bowbeer, David Holmes, and
  Doug Lea, [*Java Concurrency in Practice*](https://www.pearson.com/en-us/subject-catalog/p/java-concurrency-in-practice/P200000009262)
  (Addison-Wesley, 2006): the mechanics behind `Semaphore`,
  `ReentrantLock`, `Condition`, and `ThreadLocal`, including the reasoning
  behind defensive copying for shared state.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/)
  and [AssertJ documentation](https://assertj.github.io/doc/): the testing
  idiom behind `examples/25-resource-concurrency`.
- The full reading list lives on the
  [bibliography page]({{ '/bibliography/' | relative_url }}).
