---
title: "Encapsulation and API design"
order: 6
layout: tutorial
sectionid: docs
part: "java-substrate"
description: "A shipping-tracking gateway built behind a public interface with a package-private implementation reached through a static factory, and a Shipment/Manifest/TrackingHistory value model built from records whose compact constructors defend every collection field with List.copyOf."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Hide an implementation class behind a public interface and a static factory, as `List.of` and `Optional.of` do, and say what the package boundary buys over a public constructor.
- Use a static factory to cache a shared instance, return a type the caller never names, or swap an implementation without touching a caller.
- Build a value type as a `record` with a validating compact constructor, and say why an `Instant` field needs no defensive copy where a `Date` would.
- Defend a collection field with `List.copyOf` or `Collections.unmodifiableList`, and say what each guarantees about the caller's list and the one returned.
- Recognize when a record is the wrong shape, and when a factory behind a one-implementation interface solves a problem nobody has.

## Why this matters

A tracking system answers one question: where is this shipment now, and what happened to it before? A checkout flow, a support tool and a billing job all read a shipment's state, and none may rewrite its history. If any holder of a reference can mutate a tracking record, two callers updating one shipment produce a result nobody asked for, and a dispute six weeks later can't establish what the record said when it started.

Java has four tools for drawing that boundary, and they compose:

| Tool | Closes this leak |
| --- | --- |
| public interface, package-private implementation | callers coupling to a concrete class they can then never change |
| static factory method | a constructor that must allocate, must name its class, and can't choose an implementation |
| record | hand-written `equals`, `hashCode`, accessors and non-final fields drifting out of step |
| defensive copy | a mutable object passed in, or handed out, that the type doesn't own |

{% include excalidraw.html name="06-encapsulation-boundary" caption="Figure 6.1: a caller depends only on the ShippingGateway interface and its static factory. The concrete implementation and the representation it owns live entirely behind the package boundary, free to change without the caller noticing." %}

## The domain: tracking a shipment without exposing how

Five records model a shipment:

| Record | Holds |
| --- | --- |
| `TrackingEvent` | one scan: when, where and the `TrackingStatus` |
| `TrackingHistory` | every scan so far, in order |
| `ManifestLineItem` | one line of the bill of lading |
| `Manifest` | the line items and when the manifest was prepared |
| `Shipment` | tracking id, origin, destination, manifest and history |

All five are immutable. `Shipment.withEvent(event)` returns a new `Shipment` with a longer history and leaves the original untouched.

`ShippingGateway` creates and tracks shipments. It's a public interface, and callers get one only through the static factory `ShippingGateway.inMemory()`. The class behind it, `InMemoryShippingGateway`, has no access modifier, which makes it package-private: nothing outside `com.patterncatalyst.designpatterns.ch06.shipping` can name it, construct it or cast to it.

## Public interface, package-private implementation: Java's PImpl

C++ hides a representation behind a pointer to an incomplete type, the PImpl idiom. Java needs no pointer, because the language already separates contract from implementation: an interface declares what a caller can do, and an implementation that isn't public is visible only inside its package.

```java
public interface ShippingGateway {

    Shipment createShipment(String origin, String destination, Manifest manifest);

    Optional<Shipment> find(String trackingId);

    Shipment recordEvent(String trackingId, TrackingEvent event);

    static ShippingGateway inMemory() {
        return new InMemoryShippingGateway();
    }
}
```

```java
final class InMemoryShippingGateway implements ShippingGateway {

    private final Map<String, Shipment> shipmentsByTrackingId = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public Shipment createShipment(String origin, String destination, Manifest manifest) {
        String trackingId = "SHIP-" + sequence.incrementAndGet();
        Shipment shipment = Shipment.created(trackingId, origin, destination, manifest);
        shipmentsByTrackingId.put(trackingId, shipment);
        return shipment;
    }

    @Override
    public Optional<Shipment> find(String trackingId) {
        return Optional.ofNullable(shipmentsByTrackingId.get(Objects.requireNonNull(trackingId, "trackingId")));
    }

    @Override
    public Shipment recordEvent(String trackingId, TrackingEvent event) {
        Objects.requireNonNull(trackingId, "trackingId");
        Objects.requireNonNull(event, "event");
        return shipmentsByTrackingId.compute(trackingId, (id, existing) -> {
            if (existing == null) {
                throw new NoSuchElementException("no shipment tracked under " + trackingId);
            }
            return existing.withEvent(event);
        });
    }
}
```

The missing `public` before `final class` is the whole mechanism. Code in the same package, tests included, can still construct the class. From any other package, `new InMemoryShippingGateway()` is a compile error: "InMemoryShippingGateway is not public in com.patterncatalyst.designpatterns.ch06.shipping; cannot be accessed from outside package". A reviewer doesn't have to enforce the boundary by reading every file, because nothing that crosses it compiles.

The interface, not the class, is what callers and alternate implementations agree on. A second implementation written only in test code, never referring to `InMemoryShippingGateway`, passes the same contract tests.

## Static factory methods: the List.of and Optional.of idiom

`List.of(...)`, `Optional.of(...)` and `Optional.empty()` are static factories on types whose constructors you've likely never called. A static factory can do four things a public constructor can't:

| A static factory can | Here |
| --- | --- |
| have a name | `inMemory()` says what it returns |
| return a subtype of its declared type | a package-private class through a public interface |
| return a cached instance | `TrackingHistory.empty()` returns one shared empty history |
| choose the implementation | a later `inMemory()` or a new factory can return another class with no caller changing |

```java
public record TrackingHistory(List<TrackingEvent> events) {

    private static final TrackingHistory EMPTY = new TrackingHistory(List.of());

    public TrackingHistory {
        Objects.requireNonNull(events, "events");
        events = List.copyOf(events);
    }

    public static TrackingHistory empty() {
        return EMPTY;
    }

    public TrackingHistory withEvent(TrackingEvent event) {
        Objects.requireNonNull(event, "event");
        List<TrackingEvent> appended = new ArrayList<>(events);
        appended.add(event);
        return new TrackingHistory(appended);
    }

    public Optional<TrackingEvent> latest() {
        return events.isEmpty() ? Optional.empty() : Optional.of(events.get(events.size() - 1));
    }
}
```

`TrackingHistory.empty() == TrackingHistory.empty()` is `true`: one object, not two equal ones. `new TrackingHistory(List.of())` allocates every time.

The caller's variable is typed to the interface:

```java
        ShippingGateway gateway = ShippingGateway.inMemory();
```

A caller holding a concrete gateway type is coupled to that class, so moving storage to a database means rewriting the class in place or changing every caller's declared type. A caller holding a `ShippingGateway` is coupled to three methods. A `JdbcShippingGateway` behind a new factory, or behind the same one, changes no caller.

## Records: immutable value types without the ceremony

Before records, an immutable value type meant hand-writing accessors, `equals`, `hashCode` and `toString`, and keeping all four in step with the constructor. A record declares the state once and derives the rest. Its fields are implicitly `final`, so it can't grow a mutator by accident.

```java
public record TrackingEvent(Instant occurredAt, String location, TrackingStatus status) {

    public TrackingEvent {
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(status, "status");
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("location cannot be blank");
        }
    }
}
```

`Instant` has no mutating methods, so `TrackingEvent` needs no defensive copy. A `java.util.Date` field would need one on the way in and on the way out, because a caller could change the `Date` it passed or the one a getter returned. Choosing `Instant` is an encapsulation decision made once, at the type, that removes defensive code from every type that uses it.

## Immutability and defensive copying: the part records do not do for you

A `final` field holding a `List` still points at a list someone else may change. `List.copyOf(items)` in `Manifest`'s compact constructor does two jobs: it copies the caller's list, so later changes to it can't reach the manifest, and the copy is unmodifiable, so `manifest.items()` can't be changed by whoever calls it.

```java
public record Manifest(String manifestId, List<ManifestLineItem> items, Instant preparedAt) {

    public Manifest {
        Objects.requireNonNull(manifestId, "manifestId");
        if (manifestId.isBlank()) {
            throw new IllegalArgumentException("manifestId cannot be blank");
        }
        Objects.requireNonNull(items, "items");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("a manifest needs at least one line item");
        }
        items = List.copyOf(items);
        Objects.requireNonNull(preparedAt, "preparedAt");
    }

    public int totalUnits() {
        return items.stream().mapToInt(ManifestLineItem::quantity).sum();
    }
}
```

Add an item to the caller's `ArrayList` after construction and the manifest still reports one item. Call `manifest.items().add(...)` and it throws `UnsupportedOperationException`.

The same two guarantees can be built in two steps, `Collections.unmodifiableList(new ArrayList<>(items))`. Each step closes a different gap:

| Compact constructor stores | Caller changes its own list afterward | Caller calls `items().add(...)` |
| --- | --- | --- |
| `List.copyOf(items)` | manifest unchanged | `UnsupportedOperationException` |
| `Collections.unmodifiableList(new ArrayList<>(items))` | manifest unchanged | `UnsupportedOperationException` |
| `Collections.unmodifiableList(items)` | the change shows through the view | `UnsupportedOperationException` |
| `new ArrayList<>(items)` | manifest unchanged | the add succeeds |

`unmodifiableList` blocks changes through the view it returns, not changes to the list underneath. `List.copyOf` closes both gaps in one call, which is why `Manifest` uses it.

> **🤖 Explore with your agent**
>
> "In `examples/06-encapsulation`, add a `List<String> handlingNotes` component to the `Manifest` record. In the compact constructor, reject `null` with `Objects.requireNonNull` and store `List.copyOf(handlingNotes)`, so changing the caller's list after construction doesn't reach the manifest and `handlingNotes().add(...)` throws `UnsupportedOperationException`. Don't write a custom accessor, don't use `Collections.unmodifiableList` on the caller's list, and don't make `Manifest` a class. Update the callers in `ShippingDemo` and the tests, and add JUnit 5 tests with AssertJ for both guarantees, in the style of `ManifestTest`."
>
> Check: `mvn -q -f examples/06-encapsulation/pom.xml test -Dtest=ManifestTest` passes, and the compact constructor has exactly one `List.copyOf` line per list component.

## Build, run, observe

From the repository root, through the aggregator, or standalone:

```sh
mvn -f examples/pom.xml test
```

```sh
cd examples/06-encapsulation
mvn test
```

All 19 tests pass, including a parameterized contract test that runs the same assertions against `ShippingGateway.inMemory()` and a second implementation written in test code. Run the demo:

```sh
examples/06-encapsulation/demo.sh
```

It creates a shipment through the factory-built gateway, checks that `TrackingHistory.empty()` returns one cached instance, records two events while the original `Shipment` stays unchanged, then changes a caller's list after handing it to a `Manifest` and tries to change the manifest's own list:

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

The caller's list grew to two, the manifest still reports one, and the direct change is rejected. Two experiments, each in a scratch copy:

- **Wrap without copying.** In `Manifest`, store `Collections.unmodifiableList(items)`. The defensive-copy check fails: the caller's later add shows through the view.
- **Copy without wrapping.** Store `new ArrayList<>(items)`. The immutability check fails: `items().add(...)` succeeds.

## When not to use it

- **A record for a type with identity.** An account whose balance changes on every transaction isn't a value. Two `Account` records with the same id and different balances are unequal by the record's own `equals`, which is backwards for a type that persists across balance changes. Use a record when the type's identity is its data, as for `TrackingEvent` and `Manifest`. Use a class with a stable id and an `equals` on that id when identity and state vary independently.
- **A factory behind a one-implementation interface.** `ShippingGateway` with one implementation and none planned still compiles and passes its tests, and adds a file, an interface to open and a factory call between the caller and the only class that will ever satisfy the contract. When no second gateway is planned, make `InMemoryShippingGateway` a public class with a public constructor, and introduce the interface the day a second implementation is needed.

## For deeper coverage

- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition
  (Addison-Wesley): Item 1 on static factory methods over constructors,
  Item 17 on minimizing mutability, and Item 50 on making defensive copies,
  the three items this chapter's gateway, records, and `List.copyOf`
  sections draw from directly.
- Brian Goetz et al., [*Java Concurrency in Practice*](https://jcip.net/)
  (Addison-Wesley): the chapter on safe publication and immutability covers
  why an immutable value type, correctly built, needs no synchronization to
  share safely between threads, the property `TrackingHistory` and
  `Manifest` both rely on inside `InMemoryShippingGateway`'s
  `ConcurrentHashMap`.
- The official [Java records documentation](https://docs.oracle.com/en/java/javase/25/language/records.html)
  and [`List.copyOf` Javadoc](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/List.html#copyOf(java.util.Collection)):
  the authoritative source for what a compact constructor can rewrite and
  exactly what guarantee `copyOf` makes about the list it returns.
- The full reading list behind this book's opinions lives on the
  [bibliography page]({{ '/bibliography/' | relative_url }}); each citation
  there is a pointer to the full book, not a substitute for reading it.
