---
title: "Resource management (Java's RAII)"
order: 5
layout: tutorial
sectionid: docs
part: "java-substrate"
description: "Deterministic cleanup without a destructor: try-with-resources and AutoCloseable as Java's primary answer, and the Execute-Around Method idiom for the cases a resource's open/use/close sequence belongs to a library rather than every call site, taught against a carrier connection, a batch ledger writer, and a pooled tracking-feed handle."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Explain why Java has no deterministic destructor, so every program releases resources explicitly.
- Implement `AutoCloseable` and use try-with-resources so `close()` runs on a normal return and on any thrown exception, checked or unchecked.
- Say why finalizers and the `Cleaner` API don't replace try-with-resources, and what they fail to guarantee.
- Build the Execute-Around Method idiom: a library method that owns a resource's open, use and close and takes the caller's logic as a lambda.
- Recognize a resource whose life outlasts the call that created it, and give it to a registry or pool instead of a per-call block.

## Why this matters

A warehouse system opens a connection to a carrier's booking API for each manifest, writes shipment batches to an append-only ledger file, and holds a subscription to a carrier's live tracking feed while a shipment is in transit. Each costs something while it's open: a socket the carrier meters per tenant, a file handle the operating system caps per process, a feed the carrier bills by the minute. None of them is memory. The garbage collector reclaims a `CarrierConnection` object eventually, but that says nothing about when its socket closes. A batch job that opens a thousand connections in a loop and closes none exhausts the carrier's limit before the collector runs once.

C++'s RAII ties a resource to an object and releases it in a destructor the compiler calls when the scope ends. Java has no destructor. `finalize()` never promised a schedule: the JVM called it when the collector got to the object, if ever. Java's answer is two explicit idioms:

| Idiom | Use when |
| --- | --- |
| try-with-resources over `AutoCloseable` | one block opens, uses and releases one resource |
| Execute-Around Method | the whole open, use and close sequence repeats across call sites and belongs in one library method |

{% include excalidraw.html name="05-resource-lifecycle" caption="Figure 5.1: a try-with-resources scope guarantees close() runs on both the normal exit and an exception unwinding the block, the same guarantee a hand-written try/finally gives when every exit path remembers to ask for it." %}

## The domain: three shipping resources

Three resources, three lifetime shapes:

| Resource | Lifetime | Release |
| --- | --- | --- |
| `CarrierConnection` | one manifest submission | try-with-resources |
| `BatchLedgerWriter` over a `BufferedWriter` | one batch: open, write, commit, close | Execute-Around Method |
| `TrackingFeedHandle` | days, across many polls | `TrackingFeedRegistry`, closed once |

`CarrierConnection` implements `AutoCloseable` directly:

```java
public final class CarrierConnection implements AutoCloseable {

    public static final double MAX_WEIGHT_KG = 70.0;

    private final String carrierEndpoint;
    private boolean open;

    private CarrierConnection(String carrierEndpoint) {
        this.carrierEndpoint = carrierEndpoint;
        this.open = true;
    }

    public static CarrierConnection open(String carrierEndpoint) {
        Objects.requireNonNull(carrierEndpoint, "carrierEndpoint");
        if (carrierEndpoint.isBlank()) {
            throw new IllegalArgumentException("carrierEndpoint must not be blank");
        }
        return new CarrierConnection(carrierEndpoint);
    }

    public ManifestReceipt submitManifest(Manifest manifest) throws ManifestRejectedException {
        if (!open) {
            throw new IllegalStateException("connection to " + carrierEndpoint + " is already closed");
        }
        Objects.requireNonNull(manifest, "manifest");
        if (manifest.weightKg() > MAX_WEIGHT_KG) {
            throw new ManifestRejectedException(carrierEndpoint, manifest.trackingId(), manifest.weightKg(), MAX_WEIGHT_KG);
        }
        return new ManifestReceipt(manifest.trackingId(), carrierEndpoint);
    }

    public boolean isOpen() {
        return open;
    }

    public String carrierEndpoint() {
        return carrierEndpoint;
    }

    @Override
    public void close() {
        open = false;
    }
}
```

`submitManifest` throws the checked `ManifestRejectedException` when a parcel is over the carrier's 70 kg limit, a failure the carrier really reports, so cleanup is exercised on a real exception path.

## Try-with-resources: the compiler generates the release

Before Java 7, the only guaranteed release was a `try`/`finally` with `close()` in the `finally` clause. Try-with-resources replaces it with a declaration in the `try` statement's parentheses, for any `AutoCloseable`. Since Java 9 the parentheses can also name an effectively final variable declared earlier:

```java
        CarrierConnection connection = CarrierConnection.open("fedex-booking.example");
        try (connection) {
            ManifestReceipt receipt = connection.submitManifest(new Manifest("PKG-1002", 3.1));
            System.out.println("Receipt: " + receipt);
        } catch (ManifestRejectedException e) {
            System.out.println("Rejected: " + e.getMessage());
        }
        System.out.println("Connection open after the block? " + connection.isOpen());
```

The Java Language Specification defines try-with-resources in terms of the `try`/`finally` form the compiler generates. That changes what a reviewer must check:

| | Hand-written `try`/`finally` | try-with-resources |
| --- | --- | --- |
| `close()` call in source | yes, and an edit can delete it, move it below a throwing statement, or miss a second resource | none; the declaration is the release |
| Reviewer verifies | the `finally` exists, is reachable and calls `close()` before anything that throws | the resource is declared in the parentheses |

When `submitManifest` throws for a 95 kg parcel, `close()` runs exactly once before the exception leaves the block, and the connection reports closed afterward. A Mockito spy on a real `CarrierConnection` verifies the single call on both the normal and the exception path.

## Why finalizers and the Cleaner API are not the answer

Java has had two generations of collector-driven cleanup:

| Mechanism | Status | Weakness |
| --- | --- | --- |
| `Object.finalize()` | deprecated in Java 9, deprecated for removal in Java 18 | runs when, or if, the collector visits the object; a subclass can resurrect the object by storing `this` |
| `java.lang.ref.Cleaner` | added in Java 9 as the replacement | avoids resurrection, but still runs on the collector's schedule |

Neither runs on a schedule a program can rely on. The collector decides when to reclaim an unreachable object, and its timing is unspecified by design. A `CarrierConnection` released only by a future collection might hold its socket for seconds, minutes, or the whole run of a short batch job.

Both have one narrow job: a safety net that releases what a careless caller forgot to close, and logs the leak so it gets fixed. The JDK's `FileInputStream` registers a cleanup action in exactly that role, behind the `close()` callers are expected to invoke. A resource that relies on `Cleaner` instead of `AutoCloseable` trades release at a compiler-enforced point for release sometime before the process exits.

## The Execute-Around Method: a library owns the sequence

Every place that writes a shipment batch to the ledger must open the writer, write entries, call `commit()` and close the writer, in that order. Repeating those four steps at each call site is the boilerplate try-with-resources removed for `close()`, one level up.

The Execute-Around Method moves the sequence into one method, and the caller passes only the step that varies, as a lambda. Callers never call `open()`, `commit()` or `close()`, so none can skip the commit or close too early:

```java
            LedgerOperations.withLedger(ledgerPath, writer -> {
                writer.writeEntry(batch);
                return null;
            });
```

`LedgerOperations` is the library side:

```java
public final class LedgerOperations {

    private LedgerOperations() {
    }

    public static <T> T withLedger(Path ledgerPath, LedgerAction<T> action) {
        return withLedgerWriter(() -> BatchLedgerWriter.open(ledgerPath), action);
    }

    // Package-private seam: lets the test suite substitute a spy BatchLedgerWriter
    // for the one withLedger(Path, LedgerAction) would open from disk, so a test can
    // verify close() runs on the exception path without depending on filesystem state.
    static <T> T withLedgerWriter(WriterSupplier supplier, LedgerAction<T> action) {
        try (BatchLedgerWriter writer = supplier.open()) {
            T result = action.writeTo(writer);
            writer.commit();
            return result;
        } catch (IOException e) {
            throw new LedgerWriteException("ledger write failed", e);
        }
    }

    @FunctionalInterface
    interface WriterSupplier {
        BatchLedgerWriter open() throws IOException;
    }
}
```

`LedgerAction<T>` is the functional interface the lambda implements, and it may throw `IOException`:

```java
@FunctionalInterface
public interface LedgerAction<T> {
    T writeTo(BatchLedgerWriter writer) throws IOException;
}
```

Execute-Around builds on try-with-resources rather than replacing it. What it adds is ordering: `commit()` runs only when the action returns normally, and `close()` runs either way. When the action throws an `IOException`, the caller gets a `LedgerWriteException` with the `IOException` as its cause, the writer is closed exactly once, and it's never committed. That last part matters as much as the close: a writer that committed a partial batch before the action failed would record a batch the rest of the system must treat as never written.

Reach for Execute-Around when the whole sequence, not only the close, repeats often enough that one method reads better than a dozen near-identical blocks. A resource opened and closed in one place needs only try-with-resources.

> **🤖 Explore with your agent**
>
> "In `examples/05-resource-management`, add a `CarrierOperations.withConnection(String carrierEndpoint, ConnectionAction<T> action)` Execute-Around method for `CarrierConnection`, where `ConnectionAction<T>` is a `@FunctionalInterface` whose method may throw `ManifestRejectedException`. Open the connection with `CarrierConnection.open` inside a try-with-resources block so `close()` runs on every path. Don't override `finalize()`, don't register a `Cleaner`, and don't call `close()` by hand. Add JUnit 5 tests with a Mockito spy, in the style of `LedgerOperationsTest`, that verify `close()` runs exactly once when the action returns and when it throws."
>
> Check: `mvn -q -f examples/05-resource-management/pom.xml test -Dtest='CarrierConnectionResourceLifecycleTest,LedgerOperationsTest'` still passes, and the new method contains no `close()` call.

## When not to use a per-call try-with-resources block

Both idioms assume one owner acquires, uses and releases a resource inside one call. That's wrong for `TrackingFeedHandle`. A subscription to a carrier's live feed opens when a shipment enters transit and is polled by different parts of the system until delivery, days later. Wrapping each poll in its own `try (TrackingFeedHandle handle = TrackingFeedHandle.subscribe(endpoint))` would open and close a subscription per poll and pay the carrier's setup cost every time.

`TrackingFeedRegistry` owns one handle per carrier endpoint for as long as the application runs. Callers borrow a handle and never close it:

```java
public final class TrackingFeedRegistry implements AutoCloseable {

    private final Map<String, TrackingFeedHandle> handles = new ConcurrentHashMap<>();

    public TrackingFeedHandle handleFor(String carrierEndpoint) {
        Objects.requireNonNull(carrierEndpoint, "carrierEndpoint");
        return handles.computeIfAbsent(carrierEndpoint, TrackingFeedHandle::subscribe);
    }

    public int openHandleCount() {
        return handles.size();
    }

    @Override
    public void close() {
        handles.values().forEach(TrackingFeedHandle::close);
        handles.clear();
    }
}
```

Two calls for the same endpoint return the same handle, and closing the registry releases every handle in one pass, so no caller tracks whether it was the last user of a feed. The registry is still `AutoCloseable` and still closed with try-with-resources, once, at shutdown:

```java
        try (TrackingFeedRegistry registry = new TrackingFeedRegistry()) {
            TrackingUpdate first = registry.handleFor("fedex-booking.example").poll("PKG-1001");
            TrackingUpdate second = registry.handleFor("fedex-booking.example").poll("PKG-1002");
            System.out.println("First poll:  " + first);
            System.out.println("Second poll: " + second);
            System.out.println("Open handles while the registry is in use: " + registry.openHandleCount());
        }
```

The owning scope changed; deterministic release didn't. The same holds for any pooled resource, such as a database connection from a pool or a thread from an executor: the pool, not the borrower, decides when the resource releases.

| Resource's life | Owner |
| --- | --- |
| one call site | a per-call try-with-resources block |
| outlasts the call that first asked for it | a registry or pool, closed once at a coarser scope |

## Build, run, observe

From the repository root, through the aggregator, or standalone:

```sh
mvn -f examples/pom.xml test
```

```sh
cd examples/05-resource-management
mvn test
```

All 24 tests pass, including Mockito spies that verify `close()` runs exactly once on the normal and exception paths, for `CarrierConnection` and for the `BatchLedgerWriter` that `withLedger` opens. Run the demo:

```sh
examples/05-resource-management/demo.sh
```

It releases a connection on the normal path and on a path `ManifestRejectedException` unwinds, commits a ledger batch through the Execute-Around Method, shows the writer closing when the action throws, and polls a pooled feed handle twice before closing the registry:

```
== Java 25: try-with-resources, normal path ==
Receipt: ManifestReceipt[trackingId=PKG-1002, carrierEndpoint=fedex-booking.example]
Connection open after the block? false

== Java 25: try-with-resources, exception path ==
Rejected: carrier ups-booking.example rejected manifest PKG-1003: weight 95.0kg exceeds the 70.0kg limit
Connection open after the exception unwound the block? false

== Java 25: Execute-Around Method writes and commits a ledger batch ==
Ledger committed: BATCH-77,12,48.6

== Java 25: Execute-Around Method still closes when the action throws ==
Ledger write failed: carrier portal rejected the batch upload

== A pooled resource that outlives any one call: TrackingFeedHandle ==
First poll:  TrackingUpdate[trackingId=PKG-1001, status=IN_TRANSIT]
Second poll: TrackingUpdate[trackingId=PKG-1002, status=IN_TRANSIT]
Open handles while the registry is in use: 1
Registry closed; every pooled handle released at once, not per call.
```

Two tracking ids were polled through one carrier endpoint, and the registry opened one subscription for both. Two experiments, each in a scratch copy:

- **Drop try-with-resources from `withLedgerWriter`.** Open the writer with a plain assignment and call `close()` after `commit()`. The exception-path close check fails: when the action throws, the writer is never closed.
- **Subscribe on every call.** Make `handleFor` call `TrackingFeedHandle.subscribe` each time instead of `computeIfAbsent`. The handle-reuse check fails, because two calls for one carrier return two subscriptions.

## For deeper coverage

- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 9 states the rule this chapter builds on directly, prefer try-with-resources to try-finally, and Item 8 covers why finalizers and cleaners are unsuitable as a primary release mechanism.
- The official [Java SE documentation on the try-with-resources statement](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/AutoCloseable.html) and the [`java.lang.AutoCloseable` API reference](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/AutoCloseable.html): the specification behind the compiler-generated guarantee this chapter's tests confirm.
- The official [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html) and [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/): the spy-based testing idiom behind `examples/05-resource-management`'s suite, which verifies `close()` was called rather than only checking that a build exits zero.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
