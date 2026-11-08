---
title: "Where Java 25 retires classic idioms"
order: 23
layout: tutorial
sectionid: docs
part: "idiomatic-java-expressions"
description: "The through-line pulled together: an enum singleton retiring double-checked locking, a record retiring a hand-written value class and its defensive copying, a sealed interface with an exhaustive switch retiring Visitor and the instanceof chain, and Optional retiring the Null Object, all in modern Java 25 against one shipping domain."
duration: "35 min"
---

## Learning objectives

By the end of this chapter you can:

- Name four classic idioms and the language feature that makes each unnecessary for new code.
- Build an enum singleton over a thread-safe counter with no `synchronized` block.
- Replace a value class's getters, `equals`, `hashCode`, `toString` and defensive copying with a record and its compact constructor.
- Close a hierarchy with a sealed interface and write an exhaustive pattern-matching switch over it.
- Return `Optional` from a lookup and compose past its absence with `map` and `orElse`.
- Judge when an older idiom is still the right call, and spot generated code that reaches for a retired one.

{% include excalidraw.html name="23-idiom-retirement-map" caption="Figure 23.1: six idioms this book built by hand, each retired by the Java 25 feature on the right that gets the identical guarantee from the compiler or the runtime instead." %}

## Why this matters

For most of Java's history, the idiomatic way meant writing something by hand:

| Force | Hand-written idiom | Its weak point |
| --- | --- | --- |
| Exactly one instance | `synchronized` accessor or double-checked locking over a `volatile` field | Three facts the author must get right together |
| Value equality | Constructor, accessors, `equals`, `hashCode`, `toString`, defensive copies | Every field listed five times, and a new field forgotten in one |
| A closed set of shapes | Visitor with `accept` on every type, or an `instanceof` chain | Visitor ripples across files; the chain compiles with a missing case |
| A result that may be absent | `null` or a Null Object | Every caller must remember the check, or a class exists only to do nothing |

The language now answers each force with one feature, less code and a stronger guarantee from the compiler or the runtime. The method stays the same: name the force, pick the idiom that answers it. What changes is which idiom answers it.

## A singleton retires double-checked locking

Every facility that hands packages to carriers draws confirmation numbers from one source, because two facilities issuing the same number is a correctness bug. That's the Singleton force. `examples/23-idiom-retirement` answers it with a one-element enum over a thread-safe counter:

```java
public enum ConfirmationNumberGenerator {
    INSTANCE;

    private final AtomicLong sequence = new AtomicLong(100_000L);

    public String nextConfirmationNumber(String carrierCode) {
        Objects.requireNonNull(carrierCode, "carrierCode");
        long next = sequence.getAndIncrement();
        return carrierCode.toUpperCase(Locale.ROOT) + "-" + next;
    }
}
```

The JVM creates `INSTANCE` once, when the class is initialized, and no code here coordinates that by hand. `AtomicLong.getAndIncrement` makes each call thread-safe without taking a lock; 32 threads racing on `nextConfirmationNumber` all get distinct numbers.

The older way to get the same guarantee for a lazily built singleton was double-checked locking: check for an instance, lock and check again only if there isn't one, and mark the field `volatile` because the Java Memory Model otherwise lets a reader see a half-constructed object.

```java
// Retired: needs the field declared volatile for correctness, a double
// check around the lock, and an author who remembers why both are
// required together before trusting four lines of code.
private static volatile ConfirmationNumberGenerator instance;

static ConfirmationNumberGenerator getInstance() {
    if (instance == null) {
        synchronized (ConfirmationNumberGenerator.class) {
            if (instance == null) {
                instance = new ConfirmationNumberGenerator();
            }
        }
    }
    return instance;
}
```

The enum gets the same construction safety from the class-initialization rules. Between the enum form and the initialization-on-demand holder idiom, nothing that double-checked locking served is left uncovered.

## A record retires the hand-written value class

A shipping address and a delivery manifest are values: equal contents mean the same address or manifest, and neither has identity beyond its fields. `ShippingAddress` is a record whose compact constructor validates and canonicalizes:

```java
public record ShippingAddress(String street, String city, String postalCode, String countryCode) {

    public ShippingAddress {
        Objects.requireNonNull(street, "street");
        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(postalCode, "postalCode");
        Objects.requireNonNull(countryCode, "countryCode");
        if (street.isBlank()) {
            throw new IllegalArgumentException("street cannot be blank");
        }
        if (city.isBlank()) {
            throw new IllegalArgumentException("city cannot be blank");
        }
        if (countryCode.length() != 2) {
            throw new IllegalArgumentException(
                    "countryCode must be an ISO 3166-1 alpha-2 code: " + countryCode);
        }
        countryCode = countryCode.toUpperCase(Locale.ROOT);
    }
}
```

The fields, constructor, `equals`, matching `hashCode` and `toString` come from the declaration. The author writes only what's specific to the type: what counts as a valid address, and that the country code is uppercased once instead of compared case-insensitively wherever it's read.

`DeliveryManifest` retires defensive copying. A value class wrapping a mutable list used to copy on the way in, so the caller's later changes couldn't reach the object, and wrap on the way out, so a caller couldn't mutate the object through the accessor. A compact constructor does both in one line:

```java
public record DeliveryManifest(String batchId, List<String> confirmationNumbers) {

    public DeliveryManifest {
        Objects.requireNonNull(batchId, "batchId");
        Objects.requireNonNull(confirmationNumbers, "confirmationNumbers");
        confirmationNumbers = List.copyOf(confirmationNumbers);
    }
}
```

`List.copyOf` returns an unmodifiable list, so the inbound copy also protects the outbound accessor. Changing the caller's source list afterward leaves the manifest unchanged, and `add` on the returned list throws `UnsupportedOperationException`.

## A sealed interface and an exhaustive switch retire Visitor and the instanceof chain

A shipment reports a closed set of events: picked up, in transit, delayed, delivered, lost. A sixth kind is a deliberate change to the whole tracking system, and a sealed interface says so:

```java
public sealed interface ShipmentEvent {
    record PickedUp(Instant at, String originFacility) implements ShipmentEvent {}
    record InTransit(Instant at, String currentFacility) implements ShipmentEvent {}
    record Delayed(Instant at, String reason, Duration estimatedExtraTime) implements ShipmentEvent {}
    record Delivered(Instant at, String signedBy) implements ShipmentEvent {}
    record Lost(Instant at, String lastKnownFacility) implements ShipmentEvent {}
}
```

The two older answers each cost something. Double-dispatch Visitor gave every event an `accept` that called one `visit` overload per kind, so a sixth event meant editing the visitor interface and every implementor. An `instanceof` chain kept the change in one method but lost the compiler's help: a forgotten branch compiled and failed when a shipment reached it.

A pattern-matching switch keeps the change in one method and adds the completeness check:

```java
public static String describe(ShipmentEvent event) {
    return switch (event) {
        case ShipmentEvent.PickedUp(var at, var facility) ->
                "picked up at " + facility + " on " + at;
        case ShipmentEvent.InTransit(var at, var facility) ->
                "in transit, last seen at " + facility + " on " + at;
        case ShipmentEvent.Delayed(var at, var reason, var extra) ->
                "delayed (" + reason + "), expect an extra "
                        + extra.toMinutes() + " minutes, reported " + at;
        case ShipmentEvent.Delivered(var at, var signedBy) ->
                "delivered on " + at + ", signed by " + signedBy;
        case ShipmentEvent.Lost(var at, var facility) ->
                "lost, last known location " + facility + " as of " + at;
    };
}
```

Each arm deconstructs its record, and there's no `default`. A sixth permitted record without a `case` fails the build. `ShipmentEventNarrator.requiresCustomerNotification` adds a guard that reads the delay's length inside the `case` label:

```java
case ShipmentEvent.Delayed(var at, var reason, var extra)
        when extra.compareTo(Duration.ofHours(2)) > 0 -> true;
```

## Optional retires the Null Object and the scattered null check

A customer may or may not have a preferred carrier on file. A `null` return pushed a check onto every caller; a Null Object spared the callers at the cost of a class whose only job was to do nothing safely. `PreferredCarrierDirectory` returns `Optional<String>`:

```java
public Optional<String> preferredCarrierFor(String customerId) {
    Objects.requireNonNull(customerId, "customerId");
    return Optional.ofNullable(preferredCarrierByCustomerId.get(customerId));
}
```

`ShipmentRouter.carrierToUse` composes past the absence in one expression:

```java
public String carrierToUse(String customerId, String defaultCarrier) {
    return directory.preferredCarrierFor(customerId)
            .map(carrier -> carrier.toUpperCase(Locale.ROOT))
            .orElse(defaultCarrier.toUpperCase(Locale.ROOT));
}
```

`map` runs only when a value is present and `orElse` only when it isn't. A customer with nothing on file falls back to the default carrier, the case a forgotten null check used to miss and a Null Object used to hide.

## Other idioms the same language retires

| Retired | Replacement | What it buys |
| --- | --- | --- |
| Hand-written try/finally | try-with-resources on an `AutoCloseable` | The compiler closes every resource, in reverse order, even when the body throws |
| Fall-through switch statement | Switch expression | Every arm yields a value, there's no fall-through, and a switch expression must cover every case |
| Repeating a local's type on both sides | `var` | Less ceremony where the initializer already shows the type; no new idiom |

> **🤖 Explore with your agent**
>
> "In `examples/23-idiom-retirement`, add `record ReturnedToSender(Instant at, String reason) implements ShipmentEvent {}` inside the sealed `ShipmentEvent` interface. Add a `case ShipmentEvent.ReturnedToSender(var at, var reason)` arm to both `ShipmentEventNarrator.describe` and `ShipmentEventNarrator.requiresCustomerNotification`, deconstructing the record, with notification `true`. Don't add a `default` arm to either switch, and don't add a visitor, an `instanceof` check, a `volatile` field or a `null` return anywhere."
>
> Check: `mvn -q -f examples/23-idiom-retirement/pom.xml test -Dtest=ShipmentEventNarratorTest`; a reply with a `default` arm compiles even when an arm is missing, so reject it.

## When the older idiom is still the right call

- **Singleton.** The enum and the holder idiom need a type built once with no coordination after that. A connection that's closed and reopened, or a certificate that expires and reloads, doesn't fit, and forcing it into a singleton moves the coordination problem into a type that promises callers it has none. Library code that must control when a lock is taken, apart from class loading, can still justify an explicit lock, though a `java.util.concurrent` primitive usually reads better than reimplementing one.
- **Record.** A record needs complete, immutable state at construction and content-based equality. An entity with a database-assigned identity, a mutable accumulator, or any type whose equality tracks identity, such as a persistence-managed entity, isn't a record.
- **Sealed interface.** The owning team must know every implementation. A plugin architecture where third parties add implementations at runtime needs an open interface.
- **`Optional`.** It fits a lookup's return type. As a field, a constructor parameter or a collection element, it usually means the holder shouldn't allow absence at all.

In every case the force in the code picks the idiom, modern or older. Choosing the idiom first and finding a force to justify it gets it backward.

## Build, run, observe

From the repository root:

```sh
mvn -f examples/pom.xml test
```

Or standalone:

```sh
cd examples/23-idiom-retirement
mvn test
```

All 27 tests pass, including the 32-thread race on `ConfirmationNumberGenerator`. The demo runs all four retirements side by side:

```sh
examples/23-idiom-retirement/demo.sh
```

```
== Enum singleton: ConfirmationNumberGenerator ==
First confirmation number:  GROUND-100000
Second confirmation number: GROUND-100001

== Record: ShippingAddress + DeliveryManifest ==
Address (canonicalized country code): ShippingAddress[street=500 Dock Street, city=Akron, postalCode=44301, countryCode=US]
Manifest: DeliveryManifest[batchId=BATCH-7, confirmationNumbers=[GROUND-100000, GROUND-100001]]

== Sealed interface + switch: ShipmentEvent ==
picked up at Akron Hub on 2026-10-20T09:00:00Z (notify customer: false)
delayed (weather), expect an extra 180 minutes, reported 2026-10-21T14:00:00Z (notify customer: true)
delivered on 2026-10-22T11:30:00Z, signed by J. Alvarez (notify customer: true)

== Optional: PreferredCarrierDirectory + ShipmentRouter ==
customer-42 routes to:  FREIGHT-ZONE
customer-99 routes to:  GROUND
```

The 180-minute delay crosses the two-hour threshold, so it notifies the customer. `customer-99` has no preference on file and falls back to the ground carrier. One experiment, in a scratch copy:

- **Drop the defensive copy.** Delete the `List.copyOf` line from `DeliveryManifest`. One test fails: a change to the caller's list after construction now shows up in the manifest.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original Visitor entry, whose double-dispatch shape `ShipmentEvent`'s pattern-matching switch replaces for this closed hierarchy.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 3 on the enum singleton, Item 17 on minimizing mutability, which a record now enforces by construction, and Item 55 on using `Optional` judiciously, including the fields-and-parameters caveat.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/23-idiom-retirement`, including the concurrent-access check on `ConfirmationNumberGenerator`.
- The full reading list lives on the [bibliography page]({{ '/bibliography/' | relative_url }}).
