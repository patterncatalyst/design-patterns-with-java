# 05 - Resource management (Java's RAII)

Java has no destructor and no deterministic moment a garbage-collected
object's cleanup runs. A resource that must release on a fixed schedule, a
carrier connection, an open file handle, a tracking-feed subscription,
needs something sturdier than hoping the collector gets to it before the
process runs out of sockets. This example works through the two idiomatic
answers: **try-with-resources** over `AutoCloseable`, and the
**Execute-Around Method** idiom, where a library method owns setup and
guaranteed teardown and a caller supplies only the business logic as a
lambda.

## The domain

- `CarrierConnection implements AutoCloseable`: a connection to a carrier's
  booking API. `submitManifest()` throws the checked
  `ManifestRejectedException` when a parcel exceeds `MAX_WEIGHT_KG`, so the
  exception path through a held connection is a realistic domain failure,
  not a contrived `RuntimeException`.
- `BatchLedgerWriter implements AutoCloseable`: appends `ShipmentBatch`
  records to a ledger file on disk, backed by a real `BufferedWriter`.
- `LedgerOperations.withLedger(Path, LedgerAction)`: the Execute-Around
  Method. It opens a `BatchLedgerWriter`, hands it to the caller's lambda,
  commits on success, and closes the writer on every exit path. A caller
  never calls `open()`, `commit()`, or `close()` directly.
- `TrackingFeedHandle` / `TrackingFeedRegistry`: a pooled resource whose
  lifetime outlives any single call. A registry owns one handle per
  carrier endpoint for the life of the application and releases every
  handle at once when it closes, the shape a single-owner
  try-with-resources block does not fit.

## The two idioms

1. **try-with-resources** (`CarrierConnection`): the compiler generates the
   equivalent of a `try`/`finally` block that calls `close()`, and runs it
   on both a normal return and a thrown exception.
   `CarrierConnectionResourceLifecycleTest` proves both paths with a
   Mockito spy and `verify(connection).close()`.
2. **Execute-Around Method** (`BatchLedgerWriter` / `LedgerOperations`):
   manual open/use/close duplicates the same three steps at every call
   site; `LedgerOperations.withLedger` centralizes them once, so a caller
   supplies only the lambda. `LedgerOperationsTest` proves the writer closes
   exactly once whether the lambda returns normally or throws.

`ResourceManagementDemo.java` runs every idiom above against one shipping
scenario and prints the result of each.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

This example is standalone-buildable from this directory:

```sh
cd examples/05-resource-management
mvn test
```

It is also picked up by the repo root's aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 24 JUnit 5 cases (AssertJ assertions, Mockito
spies for the close()-is-guaranteed proofs). Observed locally on Fedora
with Temurin 25:

```
Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see every idiom run against the shipping domain rather than just reading
an assertion that says cleanup happened, run the demo:

```sh
./demo.sh
```

Expected output:

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

The last block is the point of `TrackingFeedRegistry`: one `openHandleCount()`
even after two different tracking ids are polled through the same carrier
endpoint, because the registry hands back the same pooled handle rather than
opening a new one per call.

## Files

```
examples/05-resource-management/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run ResourceManagementDemo
  src/main/java/com/patterncatalyst/designpatterns/ch05/shipping/
    Manifest.java                  # record: trackingId, weightKg
    ManifestReceipt.java            # record: trackingId, carrierEndpoint
    ManifestRejectedException.java   # checked: over the carrier weight limit
    CarrierConnection.java            # AutoCloseable; try-with-resources subject
    ShipmentBatch.java                 # record: batchId, shipmentCount, totalWeightKg
    BatchLedgerWriter.java              # AutoCloseable; file-backed ledger writer
    LedgerWriteException.java            # unchecked wrapper around IOException
    LedgerAction.java                     # functional interface: the caller's lambda
    LedgerOperations.java                  # Execute-Around Method: withLedger()
    TrackingUpdate.java                     # record: trackingId, status
    TrackingFeedHandle.java                  # AutoCloseable; pooled subscription
    TrackingFeedRegistry.java                 # owns one handle per carrier endpoint
    ResourceManagementDemo.java                # exercises every idiom above
  src/test/java/com/patterncatalyst/designpatterns/ch05/shipping/
    ManifestTest.java                 # value-type validation
    ShipmentBatchTest.java             # value-type validation
    CarrierConnectionTest.java          # submission behavior, closed-state guard
    CarrierConnectionResourceLifecycleTest.java # try-with-resources close() proofs
    BatchLedgerWriterTest.java           # file-backed writer behavior
    LedgerOperationsTest.java             # Execute-Around close() proofs (spy)
    TrackingFeedHandleTest.java            # pooled handle behavior
    TrackingFeedRegistryTest.java           # pooling + bulk close
```
