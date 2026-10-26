# 16 - Decorator and Proxy: a shipping-rate quoting pipeline

A checkout pipeline wants a shipping quote computed, surcharged for
high-value shipments, cached so a cart priced three times in one session
does not hit the tariff lookup three times, and logged for an audit trail.
A warehouse scanning app has a different problem behind the same shape: a
real carrier gateway that is expensive to open, and more than one kind of
caller hitting the lookup, not all of them cleared to use it. This example
builds both out against one shared `RateQuoter` interface so the two
patterns are easy to compare side by side.

## The domain

- `Shipment`, `Money`, `RateQuote`, and `ShippingMath` are the same shape
  this book uses elsewhere: a shipment record, exact-cents money, a priced
  quote, and one shared pricing formula every layer calls down into.
- `RateQuoter` is the single contract every decorator layer and both
  proxies implement. A caller holding a `RateQuoter` cannot tell, from the
  interface alone, whether it holds the plain core, a three-layer
  decorator stack, or a proxy standing in front of either one.
- `LiveCarrierGateway` is the core component and the real subject both
  proxies stand in front of. Its `instancesCreated()` counter stands in for
  the cost of opening a real carrier session, so a test can prove a proxy
  deferred that cost rather than just asserting a lookup returned the
  right number.

## Decorator: three layers, one interface, order that matters

- `decorator/LoggingRateQuoter.java` appends one line per quote to a shared
  call log, then forwards unchanged.
- `decorator/CachingRateQuoter.java` memoizes a quote by `Shipment` and
  skips the wrapped `RateQuoter` entirely on a repeat lookup.
- `decorator/SurchargeRateQuoter.java` adds an insurance markup
  proportional to declared value, with a mutable rate.
- `RateQuoters.java` expresses the identical surcharge and logging
  contracts as lambdas instead of named classes, with no change to the
  `RateQuoter` type a caller depends on.

`DecoratorOrderingTest` is the test that matters most in this example: the
same three decorator classes, constructed with the same arguments, produce
a different observable result depending on whether `Surcharge` wraps
`Caching` or `Caching` wraps `Surcharge`. Stacking order decides what each
layer sees, not merely what order log lines print in.

## Proxy: the same interface, a different intent

- `proxy/LazyCarrierGatewayProxy.java` is a hand-written proxy that defers
  constructing the real `LiveCarrierGateway` until the first lookup that
  actually needs one.
- `proxy/RateQuoterAccessControl.java` builds an access-control proxy with
  `java.lang.reflect.Proxy.newProxyInstance` and an `InvocationHandler`
  instead of a hand-written wrapper class. It gates every call on a
  caller's `ClearanceLevel` before forwarding, counts every call it
  intercepts, and changes nothing about what a permitted call returns.

Both proxies pass through a successful call's result untouched. That is
the line between this chapter's two patterns: a Decorator adds new
behavior to what a call returns and is meant to stack; a Proxy controls
whether a call reaches its subject at all and is meant to be transparent.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

Standalone from this directory:

```sh
cd examples/16-decorator-proxy
mvn test
```

Also picked up by the repo root's aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 22 JUnit 5 cases (AssertJ assertions, Mockito for
collaborator verification), including `DecoratorOrderingTest`, which
asserts the reordered stacks disagree, and
`RateQuoterAccessControlTest`, which asserts an under-cleared caller never
reaches the real subject.

To see the decorator stack and both proxies produce real output rather
than just reading an assertion that says they work, run the demo:

```sh
./demo.sh
```

Expected output:

```
== Decorator: surcharge wraps caching wraps logging wraps core ==
First quote (rate 1.5 cents/declared dollar): $9.20
Second quote, rate raised to 3.0 (base still cached): $11.00
Cache misses: 1, cache hits: 1, log lines: 1
The surcharge recomputes fresh on every call, so raising the rate changes the total even though the base rate underneath it only computed once.

== Decorator: reordering it to caching wraps surcharge wraps logging wraps core ==
First quote (rate 1.5 cents/declared dollar): $9.20
Second quote, rate raised to 3.0 (total now cached): $9.20
Cache misses: 1, cache hits: 1, log lines: 1
Caching now sits outside the surcharge, so the already-surcharged total is what gets memoized; raising the rate has no effect until the shipment key changes.

== Proxy: lazy initialization defers the real carrier gateway ==
Real gateway constructed before first quote? false
Quote: RateQuote[quotedBy=air-flat, amount=$7.40]
Real gateway constructed after first quote?  true

== Proxy: java.lang.reflect.Proxy gates access by clearance ==
Warehouse caller: RateQuote[quotedBy=freight-zone, amount=$7.40]
Guest caller denied: access denied: rate lookup requires WAREHOUSE clearance, caller has GUEST
Calls intercepted by the warehouse proxy: 1
```

## Files

```
examples/16-decorator-proxy/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run ShippingDemo
  src/main/java/com/patterncatalyst/designpatterns/ch16/shipping/
    DestinationZone.java          # LOCAL/REGIONAL/NATIONAL/INTERNATIONAL
    Shipment.java                  # record: weightKg, zone, declaredValueCents
    Money.java                      # record: exact integer cents
    RateQuote.java                   # record: quotedBy, amount
    ShippingMath.java                 # shared pricing math
    RateQuoter.java                    # the shared contract both patterns wrap
    LiveCarrierGateway.java             # core component / real subject
    ClearanceLevel.java                  # GUEST / WAREHOUSE / ADMIN
    RateQuoters.java                      # functional decoration factories
    ShippingDemo.java                      # exercises the whole example
    decorator/
      LoggingRateQuoter.java                # named-class decorator
      CachingRateQuoter.java                 # named-class decorator
      SurchargeRateQuoter.java                # named-class decorator
    proxy/
      LazyCarrierGatewayProxy.java            # hand-written Proxy
      RateQuoterAccessControl.java             # java.lang.reflect.Proxy option
  src/test/java/com/patterncatalyst/designpatterns/ch16/shipping/
    DecoratorOrderingTest.java      # order changes the observable result
    RateQuotersFunctionalTest.java   # lambda form matches the named classes
    ShipmentTest.java                 # value-type validation edge cases
    MoneyTest.java                      # exact-cents arithmetic
    decorator/
      CachingRateQuoterTest.java        # cache hit/miss, Mockito verifies calls
      LoggingRateQuoterTest.java         # call log content, Mockito verifies calls
    proxy/
      LazyCarrierGatewayProxyTest.java    # construction deferred, reused
      RateQuoterAccessControlTest.java     # intercepts, guards, counts
```
