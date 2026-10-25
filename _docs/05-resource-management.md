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

- Explain why Java has no deterministic destructor, and why that makes
  resource cleanup a problem every Java program has to solve explicitly
  instead of a problem the language solves on its own.
- Implement `AutoCloseable` on a resource type, and use try-with-resources
  to guarantee `close()` runs on both a normal return and a thrown
  exception, checked or unchecked.
- State why finalizers and the Cleaner API are not a substitute for
  try-with-resources, and name the specific guarantees they fail to make.
- Build the Execute-Around Method idiom: a library method that owns a
  resource's open/use/close sequence and takes the caller's business logic
  as a lambda.
- Recognize when a resource's lifetime needs to outlive the scope that
  created it, and reach for a pooled or registry-owned resource instead of
  a per-call try-with-resources block.

## Why this matters

A warehouse system opens a connection to a carrier's booking API every time
it submits a manifest, writes a batch of shipments to an append-only ledger
file before it reports the batch as processed, and holds open a
subscription to a carrier's live tracking feed for as long as a shipment is
in transit. Every one of those is a resource with a cost attached to
staying open: a socket the carrier's API meters per tenant, a file handle
the operating system caps per process, a feed subscription the carrier
bills for by the minute. None of them is memory. The garbage collector will
eventually reclaim the `CarrierConnection` object once nothing references
it, but "eventually" tells you nothing about when the socket itself closes,
and a batch job that opens a thousand connections in a loop without closing
any of them will exhaust the carrier's connection limit long before the
collector runs even one cycle.

C++'s RAII ties a resource's lifetime to an object's lifetime and lets the
compiler call the release at a fixed, specified point: the moment a scope
ends, through a destructor the language itself invokes. Java has no
destructor. The `finalize()` method, which once looked like a Java answer to
the same problem, never promised a schedule either; the JVM called it
whenever its own garbage collector eventually ran, if at all, on an object
the collector happened to visit, and `finalize()` was deprecated for
removal because that lack of a guarantee made it unsafe for exactly the
cases RAII was built for. Java's actual answer is a pair of idioms, both
explicit, both opt-in, and both covered by this chapter against a shipping
domain: try-with-resources over the `AutoCloseable` interface for the
common case of releasing one resource at the end of one block, and the
Execute-Around Method idiom for the case where a resource's entire
open/use/close sequence is common enough across call sites that it belongs
in one library method instead of being retyped at every call site.

{% include excalidraw.html name="05-resource-lifecycle" caption="Figure 5.1: a try-with-resources scope guarantees close() runs on both the normal exit and an exception unwinding the block, the same guarantee a hand-written try/finally gives when every exit path remembers to ask for it." %}

## The domain: three shipping resources

The example behind this chapter lives in `examples/05-resource-management`,
and it carries three resources with three different lifetime shapes.
`CarrierConnection` is a connection to a carrier's booking API, implementing
`AutoCloseable` directly:

```java
public final class CarrierConnection implements AutoCloseable {
    public static final double MAX_WEIGHT_KG = 70.0;

    private final String carrierEndpoint;
    private boolean open;

    public static CarrierConnection open(String carrierEndpoint) { /* ... */ }

    public ManifestReceipt submitManifest(Manifest manifest) throws ManifestRejectedException {
        if (!open) {
            throw new IllegalStateException("connection to " + carrierEndpoint + " is already closed");
        }
        if (manifest.weightKg() > MAX_WEIGHT_KG) {
            throw new ManifestRejectedException(carrierEndpoint, manifest.trackingId(), manifest.weightKg(), MAX_WEIGHT_KG);
        }
        return new ManifestReceipt(manifest.trackingId(), carrierEndpoint);
    }

    @Override
    public void close() {
        open = false;
    }
}
```

`submitManifest` throws the checked `ManifestRejectedException` when a
parcel exceeds the carrier's weight limit, a realistic failure rather than a
`RuntimeException` invented to exercise cleanup. `BatchLedgerWriter` wraps a
real `BufferedWriter` over a ledger file and is also `AutoCloseable`;
`TrackingFeedHandle` models a live tracking-feed subscription and is the
resource this chapter leaves outside a per-call try-with-resources block,
for reasons the "when not to use it" section below covers.

## Try-with-resources: the compiler generates the release

Before Java 7, the only way to guarantee a resource released on every exit
path was a `try`/`finally` block, with the release written explicitly in the
`finally` clause. Try-with-resources retires that boilerplate by replacing
the `try`/`finally` pair with a declaration inside the `try` statement's own
parentheses. Any type implementing `AutoCloseable` qualifies:

```java
try (CarrierConnection connection = CarrierConnection.open(carrierEndpoint)) {
    ManifestReceipt receipt = connection.submitManifest(manifest);
    System.out.println("Receipt: " + receipt);
} catch (ManifestRejectedException e) {
    System.out.println("Rejected: " + e.getMessage());
}
```

The Java Language Specification defines try-with-resources as sugar for the
`try`/`finally` form, generated by the compiler rather than hand-written.
That matters for what a reader and a reviewer have to verify. A hand-written
`finally` clause has a `close()` call a future edit could delete, move above
a statement that throws, or simply forget to add to a second resource opened
in the same method; a reviewer checking whether a method leaks a connection
has to read past the happy path and confirm the `finally` block exists, is
reachable, and calls `close()` and nothing else that might throw before
reaching it. Try-with-resources has no `close()` call anywhere in the source
for a reader to get wrong, because there is nowhere left to put one; the
resource's own declaration is the only place its release is specified, and
the compiler is what reads it.

`CarrierConnectionResourceLifecycleTest` proves the guarantee rather than
asserting it by inspection. A Mockito spy wraps a real `CarrierConnection`,
and the test runs it through try-with-resources twice, once where
`submitManifest` returns normally and once where it throws
`ManifestRejectedException`:

```java
@Test
void closesExactlyOnceOnTheExceptionPath() {
    CarrierConnection connection = spy(CarrierConnection.open("ups-booking.example"));

    assertThatThrownBy(() -> {
        try (connection) {
            connection.submitManifest(new Manifest("PKG-4002", 95.0));
        }
    }).isInstanceOf(ManifestRejectedException.class);

    verify(connection, times(1)).close();
    assertThat(connection.isOpen()).isFalse();
}
```

`verify(connection, times(1)).close()` is the assertion that matters here,
not merely that the test ran without an unhandled exception escaping it.
The exception that `submitManifest` throws propagates out past the
try-with-resources block exactly as `assertThatThrownBy` expects, and
`close()` still ran before that propagation reached the test method,
because the compiler-generated `finally` equivalent sits between the
`submitManifest` call and the exception's exit from the block, not after it.

## Why finalizers and the Cleaner API are not the answer

Java has had two generations of garbage-collector-driven cleanup, and
neither one is a substitute for try-with-resources, for the same underlying
reason. `Object.finalize()`, present since Java 1.0 and formally deprecated
for removal starting in Java 9, let a class register a method the garbage
collector would call before reclaiming an unreachable object. The `Cleaner`
API, added in Java 9 as `finalize()`'s replacement, registers a cleanup
action tied to an object's reachability instead of overriding a method, and
avoids some of `finalize()`'s worse failure modes, like a subclass
resurrecting a supposedly-dying object by storing a reference to `this`
inside `finalize()` itself. Both share the one property that disqualifies
them from this chapter's guarantee: neither runs on a schedule a program can
rely on. The garbage collector decides when, or whether, to reclaim an
unreachable object, and a cleanup action tied to that reclamation inherits
the collector's own timing, which is unspecified by design. A
`CarrierConnection` that only releases when some future garbage-collection
cycle happens to visit it might hold its socket open for seconds, minutes,
or for the rest of a short-lived batch job's entire run, well past the one
manifest submission that needed it.

`Cleaner` and `finalize()` both still have a legitimate, narrow job: a safety
net that releases a resource a careless caller forgot to close explicitly,
logging the leak so it gets fixed, not a primary release mechanism a program
depends on for correctness. The JDK's own `FileInputStream` registers a
`Cleaner` action precisely in that role, as a backstop behind the `close()`
method callers are expected to invoke themselves, through try-with-resources
or otherwise. Writing a shipping resource that relies on `Cleaner` instead
of implementing `AutoCloseable` and expecting try-with-resources to be used
would trade a guarantee, cleanup at a specific, compiler-enforced point, for
a hope, cleanup sometime before the process exits, and no code in this
chapter's example makes that trade.

## The Execute-Around Method: a library owns the sequence

Try-with-resources handles the case where one method opens a resource, uses
it, and closes it in the same block. `BatchLedgerWriter` surfaces a
different shape: every place in this codebase that writes a shipment batch
to the ledger needs to open the writer, write one or more entries, call
`commit()` to flush them, and close the writer, in that exact order, every
time. Repeating that four-step sequence at each call site is the same
boilerplate problem try-with-resources solved for `close()` alone, just one
level up: now the open, the commit, and the close are all repeated, not just
the close.

The Execute-Around Method idiom solves this by moving the whole sequence
into one method that a caller invokes with only the one step that actually
varies, what to write, expressed as a lambda. A caller never calls `open()`,
`commit()`, or `close()` directly, so there is nowhere left for a call site
to forget the commit or move the close above it:

```java
LedgerOperations.withLedger(ledgerPath, writer -> {
    writer.writeEntry(batch);
    return null;
});
```

`LedgerOperations.withLedger` is the library method:

```java
public static <T> T withLedger(Path ledgerPath, LedgerAction<T> action) {
    return withLedgerWriter(() -> BatchLedgerWriter.open(ledgerPath), action);
}

static <T> T withLedgerWriter(WriterSupplier supplier, LedgerAction<T> action) {
    try (BatchLedgerWriter writer = supplier.open()) {
        T result = action.writeTo(writer);
        writer.commit();
        return result;
    } catch (IOException e) {
        throw new LedgerWriteException("ledger write failed", e);
    }
}
```

`withLedger` builds on try-with-resources rather than replacing it; the
`BatchLedgerWriter` it opens is still released through the same
compiler-generated guarantee as `CarrierConnection` was in the previous
section. What Execute-Around adds on top is that `commit()` only runs if
the caller's `action` returns normally, and `close()` runs whether it does
or not, both inside one method instead of being re-derived by every caller.
`LedgerAction<T>` is the functional interface that makes the lambda
possible:

```java
@FunctionalInterface
public interface LedgerAction<T> {
    T writeTo(BatchLedgerWriter writer) throws IOException;
}
```

`LedgerOperationsTest` proves the same guarantee this chapter already
proved for try-with-resources alone, now one level up, by spying on the
`BatchLedgerWriter` `withLedger` opens internally and forcing the caller's
lambda to throw:

```java
@Test
void withLedgerClosesExactlyOnceWhenTheActionThrows(@TempDir Path tempDir) throws IOException {
    BatchLedgerWriter writer = spy(BatchLedgerWriter.open(tempDir.resolve("ledger.csv")));

    assertThatThrownBy(() -> LedgerOperations.withLedgerWriter(() -> writer, w -> {
        w.writeEntry(new ShipmentBatch("BATCH-12", 1, 2.0));
        throw new IOException("carrier portal rejected the batch upload");
    })).isInstanceOf(LedgerWriteException.class)
            .hasCauseInstanceOf(IOException.class);

    verify(writer, times(1)).close();
    assertThat(writer.isCommitted()).isFalse();
}
```

`writer.isCommitted()` staying `false` is as important as `close()` being
called exactly once. A ledger writer that commits a partial write before
discovering the action failed would report a batch as recorded that the
rest of the system should treat as never having happened; `withLedger`'s
ordering, commit only after the action returns, close no matter what,
is what keeps a failed write from masquerading as a successful one. Execute-Around
is worth reaching for exactly when this sequence, not just the close, is
repeated often enough that a shared method reads more clearly than a dozen
near-identical `try`/`finally` blocks scattered across the codebase; a
resource opened and closed in exactly one place in the entire program has
no such duplication to remove, and try-with-resources alone already covers
it without the extra indirection of a lambda-taking library method.

> **AI sidebar.** Ask an LLM pairing partner to "release this resource
> safely" against a class that is not `AutoCloseable` and a common result
> reaches for a `finalize()` override or, on a newer suggestion, a
> `Cleaner.register()` call, because both appear in enough older Java
> material that the pattern comes up readily, and neither mention in a
> prompt about "releasing a resource" that specified what schedule the
> release needs to run on. Read a generated cleanup suggestion for that gap
> specifically: does it still run `close()` from the method that acquired
> the resource, via a `try`/`finally` or a try-with-resources block, with
> the garbage-collector-driven method standing in only as a backstop behind
> it, or does it lean on `finalize()` or `Cleaner` as the only release path
> at all? A generated class whose sole cleanup mechanism is tied to garbage
> collection has reached for a safety net as a primary mechanism, and it
> will hold the resource open for an unpredictable stretch of program time
> that a reviewer cannot bound by reading the code.

## When not to use a per-call try-with-resources block

Try-with-resources and the Execute-Around Method both assume the same
shape: a resource acquired, used, and released inside one call, by one
owner. That shape is wrong for a resource whose useful life is supposed to
span many calls, which is exactly what `TrackingFeedHandle` models. A
subscription to a carrier's live tracking feed is opened once when a
shipment enters transit and polled repeatedly, by different parts of the
system, until the shipment is delivered days later. Wrapping each poll in
its own `try (TrackingFeedHandle handle = TrackingFeedHandle.subscribe(endpoint))`
block would open and immediately close a fresh subscription on every single
poll, paying the carrier's subscription setup cost again and again for a
feed that should have stayed open the whole time.

`TrackingFeedRegistry` is the shape that fits instead: one object owns a
pool of handles, one per carrier endpoint, for as long as the application
itself runs, and callers borrow a handle without ever closing it
themselves:

```java
public TrackingFeedHandle handleFor(String carrierEndpoint) {
    return handles.computeIfAbsent(carrierEndpoint, TrackingFeedHandle::subscribe);
}

@Override
public void close() {
    handles.values().forEach(TrackingFeedHandle::close);
    handles.clear();
}
```

`TrackingFeedRegistryTest` confirms two calls to `handleFor` with the same
carrier endpoint return the identical handle instance, not a new
subscription each time, and that closing the registry releases every
pooled handle in one pass rather than requiring each caller to track
whether it was the last one still using a given feed. The registry itself
is still `AutoCloseable`, and still gets closed with try-with-resources,
once, at the point the whole application shuts down; what changed is the
scope that owns the resource, not whether `AutoCloseable` and deterministic
release still apply. The same reasoning extends past tracking feeds to any
pooled handle, a database connection borrowed from a connection pool, a
thread borrowed from an executor's pool, where the pool, not the borrowing
call, is the thing that should decide when the underlying resource actually
releases. Reach for a per-call try-with-resources block when one call site
is the resource's entire life. Reach for a registry or a pool, closed once
at a coarser scope, the moment a resource's useful life is supposed to
outlast the call that first asked for it.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this loop.
From the repository root, the aggregator picks up
`examples/05-resource-management` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/05-resource-management
mvn test
```

Either path runs the same 25 JUnit 5 cases, asserted with AssertJ, including
two Mockito-spy suites that verify `close()` runs exactly once on both the
normal and the exception path, for `CarrierConnection` directly and for the
`BatchLedgerWriter` that `LedgerOperations.withLedger` opens internally.
Observed locally on Fedora with Temurin 25:

```
Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see every idiom run against the shipping domain rather than just reading
an assertion that says cleanup happened, run the example's own demo script:

```sh
examples/05-resource-management/demo.sh
```

It releases a connection with try-with-resources on the normal path,
releases a second connection with try-with-resources on a path
`ManifestRejectedException` unwinds, writes and commits a ledger batch
through the Execute-Around Method, shows that same method closing the
writer when the action throws, and finally polls a pooled tracking-feed
handle twice before closing the whole registry at once:

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

"Open handles while the registry is in use: 1" is the detail worth reading
twice: two different tracking ids were polled, through the same carrier
endpoint, and the registry opened exactly one subscription for both, not
two, because `handleFor` returns the same pooled handle every time it is
asked for the same endpoint.

## For deeper coverage

- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 9 states the rule this chapter builds on directly, prefer try-with-resources to try-finally, and Item 8 covers why finalizers and cleaners are unsuitable as a primary release mechanism.
- The official [Java SE documentation on the try-with-resources statement](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/AutoCloseable.html) and the [`java.lang.AutoCloseable` API reference](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/AutoCloseable.html): the specification behind the compiler-generated guarantee this chapter's tests confirm.
- The official [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html) and [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/): the spy-based testing idiom behind `examples/05-resource-management`'s suite, which verifies `close()` was called rather than only checking that a build exits zero.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
