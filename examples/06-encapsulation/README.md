# 06 - Encapsulation and API design: a shipping-tracking gateway

Hiding an implementation is not one technique in Java; it is four tools
that work together. This example builds a shipping-tracking domain and
uses all four: a public interface with a package-private implementation,
a static factory method that hides the concrete class entirely, records
as immutable value types, and defensive copying so a shared collection
field can never be mutated from the outside.

## The domain

- `TrackingStatus`: a closed enum (`CREATED`, `PICKED_UP`, `IN_TRANSIT`,
  `OUT_FOR_DELIVERY`, `DELIVERED`, `EXCEPTION`).
- `TrackingEvent`: a `record` of `Instant occurredAt`, `location`, and
  `status`. An `Instant` has no mutating methods, so nothing defends it.
- `TrackingHistory`: a `record` wrapping `List<TrackingEvent>`. The compact
  constructor runs `List.copyOf(events)`, which both copies the caller's
  list and makes it immutable in one call. `empty()` is the `List.of()` /
  `Optional.empty()` caching idiom: every empty history is the same shared
  instance.
- `ManifestLineItem` and `Manifest`: a bill of lading built the same way,
  with `Manifest`'s compact constructor defending its `items` list.
- `Shipment`: the aggregate `record`. `Shipment.created(...)` is the static
  factory callers actually use; `withEvent(...)` returns a new `Shipment`
  rather than mutating the one it was called on.
- `ShippingGateway`: a **public interface** with one **static factory
  method**, `inMemory()`. The class it returns,
  `InMemoryShippingGateway`, carries **no access modifier**, so it is
  package-private: no code outside this package can name it, construct it,
  or cast to it, even though it can hold and call it through the
  `ShippingGateway` reference the factory returned.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

Standalone from this directory:

```sh
cd examples/06-encapsulation
mvn test
```

Also picked up by the repo root's aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 19 JUnit 5 cases (AssertJ assertions), including a
parameterized contract test that runs identical assertions against
`ShippingGateway.inMemory()` and a second, independent implementation
written entirely in the test tree, proving neither side depends on
anything but the interface.

To see the encapsulation guarantees hold at the terminal rather than just
reading an assertion that says they do, run the demo:

```sh
./demo.sh
```

Expected output:

```
== Public interface + package-private implementation ==
Concrete type the caller never names: InMemoryShippingGateway
Created: SHIP-1 (5 units)

== Static factory caching: TrackingHistory.empty() is a shared instance ==
empty() == empty(): true

== Immutability: earlier Shipment references never change ==
original history size:   0
picked-up history size:  1
in-transit history size: 2
latest event status: IN_TRANSIT

== Defensive copying: mutating a caller's list does not reach inside ==
caller's list size now: 2
manifest still reports: 1
manifest.items().add(...) rejected: UnsupportedOperationException
```

## Files

```
examples/06-encapsulation/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run ShippingDemo
  src/main/java/com/patterncatalyst/designpatterns/ch06/shipping/
    TrackingStatus.java           # closed enum
    TrackingEvent.java             # record: occurredAt (Instant), location, status
    TrackingHistory.java            # record: List.copyOf + empty() caching
    ManifestLineItem.java            # record: sku, quantity, description
    Manifest.java                     # record: defends its items list
    Shipment.java                      # record aggregate; created()/withEvent()
    ShippingGateway.java                # public interface + static factory
    InMemoryShippingGateway.java         # package-private implementation
    ShippingDemo.java                     # exercises every guarantee
  src/test/java/com/patterncatalyst/designpatterns/ch06/shipping/
    TrackingHistoryTest.java       # caching, defensive copy, immutability
    ManifestTest.java               # defensive copy, validation
    ShipmentTest.java                # validation, withEvent immutability
    ShippingGatewayTest.java          # factory behavior, caller-side lookups
    ShippingGatewayContractTest.java   # two implementations, one contract
```
