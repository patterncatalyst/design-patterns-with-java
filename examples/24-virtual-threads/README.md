# 24 - Virtual threads: fanning a rate quote out to many carriers

A checkout needs a rate quote from every carrier under contract before it can
show the cheapest one, and each carrier quote is a blocking network call.
This example fans one `Shipment` out to eight carriers concurrently, one
virtual thread per carrier call, and gathers every quote back.

## The domain

- `Shipment`: a `record` with `shipmentId` and `weightKg`.
- `RateQuote`: a `record` of `carrierId` and `quotedCents`.
- `CarrierGateway`: the one-method contract a real carrier integration would
  implement with a blocking HTTP call. `SimulatedCarrierGateway` stands in
  for that call with a randomized `Thread.sleep`.
- `CarrierRateCache`: a shared, mutable collaborator that many virtual
  threads update concurrently while a fan-out is in flight, guarded by a
  `ReentrantLock`.

## The three things this example demonstrates

1. **Unbounded fan-out** (`RateQuoteFanOutService.fanOut`): builds one
   `Callable` per carrier and runs them all on
   `Executors.newVirtualThreadPerTaskExecutor()`. Every carrier call gets its
   own virtual thread; none of them ever waits for a platform thread to free
   up.
2. **Bounded fan-out** (`RateQuoteFanOutService.fanOutBounded`): the same
   one-virtual-thread-per-carrier shape, but a `Semaphore` caps how many
   carrier calls are allowed to be in flight at once. The virtual thread
   count stays one-per-task; only the concurrent blocking-call count is
   capped, the idiom for a carrier whose rate API enforces its own
   connection limit.
3. **A shared cache under contention** (`CarrierRateCache`): guarded by a
   `ReentrantLock` rather than `synchronized`, not primarily to dodge
   pinning (JDK 24's JEP 491 already fixed that for `synchronized`), but for
   `tryLock()`, a capability `synchronized` has never offered.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

This example is standalone-buildable from this directory:

```sh
cd examples/24-virtual-threads
mvn test
```

It is also picked up by the repo root's aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 14 JUnit 5 cases (AssertJ assertions). The two
concurrency tests prove true concurrency and the bounded limit with
`CyclicBarrier` rendezvous points rather than sleeps or elapsed-time
assertions: if the fan-out ever serialized its carrier calls, the barrier
would never fill and the test would hang, caught by a deadlock-guard
timeout rather than passing quietly.

To see the fan-out's concurrency pay off as elapsed wall-clock time, not
just a passing assertion, run the demo:

```sh
./demo.sh
```

Expected output (exact milliseconds vary by machine):

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

The unbounded run takes roughly as long as the single slowest carrier call,
not the sum of all eight; the bounded run takes longer because only three
calls are ever blocking at once, by design.

## Files

```
examples/24-virtual-threads/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run ShippingDemo
  src/main/java/com/patterncatalyst/designpatterns/ch24/shipping/
    Shipment.java                 # record: shipmentId, weightKg
    RateQuote.java                 # record: carrierId, quotedCents
    CarrierGateway.java             # the blocking-call contract (functional interface)
    CarrierQuoteException.java       # wraps a failed carrier call
    RateQuoteFanOutService.java       # fanOut (unbounded) and fanOutBounded
    CarrierRateCache.java              # ReentrantLock-guarded shared cache
    SimulatedCarrierGateway.java        # blocking stand-in for a real carrier API
    ShippingDemo.java                    # exercises both fan-out shapes
  src/test/java/com/patterncatalyst/designpatterns/ch24/shipping/
    RateQuoteFanOutServiceConcurrencyTest.java        # proves true concurrency (barrier)
    RateQuoteFanOutServiceBoundedConcurrencyTest.java  # proves the bound is respected
    RateQuoteFanOutServiceTest.java                     # non-concurrency behavior
    CarrierRateCacheTest.java                            # lock correctness, incl. under load
    ShipmentTest.java                                     # value-type validation
    RateQuoteTest.java                                     # value-type validation
```
