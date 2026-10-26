# 25 - Resource and concurrency patterns: a regional dispatch hub

A dispatch hub calls a carrier's tracking API on every parcel scan, shares a
loading-dock manifest across several warehouse scanners, and drives a
printer that can only run one job at a time. Three idiomatic Java
concurrency shapes answer those three problems, and this example builds all
three against one domain: a bounded connection pool, a monitor object, and
an active object, plus two supporting idioms, `ThreadLocal` and defensive
immutable snapshots.

## The patterns

1. **Bounded resource pool** (`CarrierConnectionPool.java`): a `Semaphore`
   sized to the pool's capacity caps how many `CarrierApiConnection`
   instances can be outstanding at once; a `BlockingQueue` holds idle
   connections for reuse. `acquire(Duration timeout)` blocks until a permit
   frees up or the timeout elapses, throwing `TimeoutException` rather than
   failing instantly or growing the pool past its bound. `Lease` is an
   `AutoCloseable` returned from `acquire()`; its `close()` is the only path
   back into the idle queue, so there is no separate `release()` method for
   a caller to forget on an exception path.
2. **Monitor object** (`DispatchDock.java`): owns a `ReentrantLock` and a
   `Condition` (`bayFull`), taking the lock at the top of every public
   method so a caller never has to remember to synchronize. `stageParcel()`
   adds a scan and signals the condition once the bay reaches capacity;
   `awaitFullBay(Duration timeout)` blocks the dispatcher thread on that
   condition, with a real timeout, until the bay is full, then returns an
   immutable snapshot of the staged parcels.
3. **Active object** (`LabelPrintActiveObject.java`): `submitPrint()`
   enqueues a request onto a `BlockingQueue` and returns a
   `CompletableFuture` immediately, without running anything on the calling
   thread. A single dedicated worker thread, started once in the
   constructor, drains the queue and executes requests one at a time, in
   submission order.
4. **Supporting idioms**: `TrackingCodeGenerator.java` confines a
   non-thread-safe `MessageDigest` to one instance per calling thread with
   `ThreadLocal`, and `DispatchDock.awaitFullBay()` returns `List.copyOf(...)`
   instead of its internal mutable list, an immutable snapshot that needs no
   lock to share safely once it exists.

`DispatchHubDemo.java` exercises all three patterns and the ThreadLocal
generator end to end.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

This example is standalone-buildable from this directory:

```sh
cd examples/25-resource-concurrency
mvn test
```

It is also picked up by the repository root's aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 19 JUnit 5 cases (AssertJ assertions), including
concurrency suites that run real virtual-thread and platform-thread load
against the pool's bound, the dock's invariant, and the active object's
ordering guarantee, asserted with latches and futures rather than sleeps.

To see all three patterns run end to end rather than just reading an
assertion that says they work, run the demo:

```sh
./demo.sh
```

Expected output (the tracking codes are deterministic SHA-256 hex prefixes
and will match exactly on any machine):

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

Java closes multiple try-with-resources resources in reverse declaration
order, so the second lease acquired closes first and the reused connection
is id 2, not id 1. Both print receipts show
`printedByThread=label-print-worker`, never the thread that called
`submitPrint()`. That is the active object's decoupling showing up as an
observable fact at the terminal, not just as a passing test.

## Files

```
examples/25-resource-concurrency/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run DispatchHubDemo
  src/main/java/com/patterncatalyst/designpatterns/ch25/dispatch/
    ParcelScan.java               # record: parcelId, event
    ShippingLabelRequest.java      # record: parcelId, destination
    PrintReceipt.java               # record: parcelId, sequence, printedByThread
    CarrierApiConnection.java        # the pooled resource
    CarrierApiConnectionFactory.java  # the pool's test seam
    CarrierConnectionPool.java        # bounded pool: Semaphore + BlockingQueue + Lease
    DispatchDock.java                  # monitor: ReentrantLock + Condition
    LabelPrintActiveObject.java         # active object: queue + dedicated worker
    TrackingCodeGenerator.java           # ThreadLocal<MessageDigest>
    DispatchHubDemo.java                  # exercises all three patterns
  src/test/java/com/patterncatalyst/designpatterns/ch25/dispatch/
    CarrierConnectionPoolTest.java  # bound, reuse, blocking-timeout
    DispatchDockTest.java            # invariant under concurrency, await/signal
    LabelPrintActiveObjectTest.java   # decoupling, ordering, exactly-once
    TrackingCodeGeneratorTest.java     # per-thread confinement, cross-thread consistency
    ParcelScanTest.java                 # value-type validation
```
