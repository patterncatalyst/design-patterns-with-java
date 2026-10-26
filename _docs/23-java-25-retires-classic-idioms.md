---
title: "Where Java 25 retires classic idioms"
order: 23
layout: tutorial
sectionid: docs
part: "idiomatic-java-expressions"
description: "The book's through-line pulled together: an enum singleton retiring double-checked locking, a record retiring a hand-written value class and its defensive copying, a sealed interface with an exhaustive switch retiring Visitor and the instanceof chain, and Optional retiring the Null Object, all in modern Java 25 against one shipping domain."
duration: "35 min"
---

## Learning objectives

By the end of this chapter you can:

- Name four classic idioms this book has built chapter by chapter and the
  Java 25 feature that makes each one unnecessary for new code.
- Build an enum singleton over a thread-safe counter and explain why it
  needs no `synchronized` block where the retired idiom needed one.
- Replace a hand-written value class's getters, `equals`, `hashCode`,
  `toString`, and defensive copying with a record's compact canonical
  constructor.
- Close a varying-shape hierarchy with a sealed interface and write an
  exhaustive pattern-matching switch the compiler checks for completeness.
- Return `Optional` from a lookup and compose past its possible absence
  with `map` and `orElse`, with no null check or stand-in object anywhere
  in the caller.
- Judge the handful of cases where an older idiom is still the right call,
  and recognize when generated code reaches for a retired idiom out of
  habit rather than fit.

{% include excalidraw.html name="23-idiom-retirement-map" caption="Figure 23.1: six idioms this book built by hand, each retired by the Java 25 feature on the right that gets the identical guarantee from the compiler or the runtime instead." %}

## Why this matters

Every chapter in this book has trained the same move: recognize a force in
a real design, name the pattern or idiom that resolves it, then build that
resolution the idiomatic Java way. For a long stretch of this book's own
history, and for a much longer stretch of the language's history, "the
idiomatic Java way" meant writing something by hand. A singleton meant a
`synchronized` method or a carefully ordered `volatile` field. A value
object meant a constructor, three or four accessor methods, an `equals`
override, a matching `hashCode`, and a `toString` nobody enjoyed
maintaining. A closed set of shapes meant either a class hierarchy with a
double-dispatch `accept` method on every member, or a chain of `instanceof`
checks that compiled cleanly with a forgotten case and failed at runtime on
whichever input reached it first. An optional result meant `null`, a
convention every caller had to remember to check, or a Null Object built
and maintained purely to make forgetting safer.

Java 25 retires every one of those by-hand idioms for new code, not by
adding a library on top of the language but by changing what the language
itself guarantees. This chapter is the place this book pulls that
observation together into one lesson instead of leaving it scattered across
four or five separate idioms learned in isolation. The method does not
change: recognize the force, reach for the idiom that answers it. What
changes is which idiom answers it now. A reader who has worked through the
patterns earlier in this book already knows what double-checked locking, a
hand-rolled value class, double-dispatch Visitor, and a Null Object were
solving. This chapter's job is to show that the same forces, construction
discipline, correct equality, exhaustive handling of a closed set, and safe
handling of absence, now resolve to a single language feature apiece,
with less code and a stronger compiler guarantee than the idiom it
replaces ever offered.

## A singleton retires double-checked locking

A warehouse system that hands packages to carriers needs exactly one
source of confirmation numbers, shared by every facility, because two
facilities handing out the same number at the same moment is a correctness
bug, not a performance detail. That is the Singleton force again: exactly
one instance, reachable from anywhere that needs it. `examples/23-idiom-retirement`
answers it with a single-element enum wrapping a thread-safe counter:

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

The JVM constructs `INSTANCE` exactly once, the moment this class loads,
and no code anywhere in this class coordinates that construction by hand.
`AtomicLong.getAndIncrement` is what carries the per-call thread safety
once the singleton already exists, a single compare-and-swap loop with no
lock acquired or released on any call. `ConfirmationNumberGeneratorTest`
races thirty-two threads at `nextConfirmationNumber` simultaneously and
asserts every result came back distinct.

Before an idiom this simple was common knowledge, the equivalent guarantee
over a lazily built singleton needed double-checked locking: check whether
an instance exists, lock and check again only if it does not, and mark the
backing field `volatile` because the Java Memory Model otherwise permits a
reader to observe a half-constructed object.

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

The enum singleton gets identical construction safety from the language's
class-loading rules instead of from an author getting three separate facts
right at once, the `volatile` modifier, the double check, and the reason
both are needed together. Nobody writing a singleton in current Java
reaches for this shape anymore; the enum form and the initialization-on-demand
holder idiom between them cover every case double-checked locking used to
serve.

## A record retires the hand-written value class

A shipping address and a delivery manifest are both value objects: two
instances with the same contents are the same address or the same
manifest, and neither one has an identity beyond its fields. `ShippingAddress` is a record with a compact canonical
constructor carrying its validation and its one piece of canonicalization:

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

Everything a hand-written value class used to need spelled out, a private
final field per component, a constructor that assigns each one, an
`equals` that compares every field, a `hashCode` consistent with that
`equals`, and a `toString` someone has to update whenever a field is
added, comes from the record declaration itself. The only code an author
still writes is the part specific to this type: what
counts as a valid address, and that the country code's case gets
normalized once rather than compared case-insensitively everywhere it is
read later.

`DeliveryManifest` retires a second, easier-to-miss piece of ceremony:
defensive copying. A hand-written value class wrapping a mutable
collection used to need a copy on the way in, so a caller's later
mutation of the list it passed to the constructor could not reach back
into the object, and a copy or an unmodifiable wrapper on the way out, so
a caller holding the accessor's return value could not mutate the object
through it either. Both of those used to be separate pieces of code an
author had to remember to write and a reviewer had to remember to check
for. A record's compact constructor collapses both into one line:

```java
public record DeliveryManifest(String batchId, List<String> confirmationNumbers) {

    public DeliveryManifest {
        Objects.requireNonNull(batchId, "batchId");
        Objects.requireNonNull(confirmationNumbers, "confirmationNumbers");
        confirmationNumbers = List.copyOf(confirmationNumbers);
    }
}
```

`List.copyOf` produces an unmodifiable list, so the single assignment
inside the compact constructor handles the inbound copy, and the
record-generated accessor handing back that same unmodifiable reference
handles the outbound protection, with no second copy needed on the way
out. `DeliveryManifestTest` proves both directions directly: mutating the
caller's source list after construction leaves the manifest unchanged, and
calling `add` on the list the accessor returns throws
`UnsupportedOperationException` rather than silently succeeding.

## A sealed interface and an exhaustive switch retire Visitor and the instanceof chain

A shipment reports a closed set of events over its lifetime: it is picked
up, it moves through a facility, it is delayed, it is delivered, or it is
lost. Nothing about that set grows casually; adding a sixth kind of event
is a deliberate change to the whole tracking system, not something one
caller does quietly on its own. That closedness is exactly the force a
sealed interface exists to express:

```java
public sealed interface ShipmentEvent {
    record PickedUp(Instant at, String originFacility) implements ShipmentEvent {}
    record InTransit(Instant at, String currentFacility) implements ShipmentEvent {}
    record Delayed(Instant at, String reason, Duration estimatedExtraTime) implements ShipmentEvent {}
    record Delivered(Instant at, String signedBy) implements ShipmentEvent {}
    record Lost(Instant at, String lastKnownFacility) implements ShipmentEvent {}
}
```

Before pattern matching over a sealed hierarchy, a closed set like this
resolved one of two older ways. The double-dispatch Visitor gave every
event type an `accept` method that called back into a visitor interface
carrying one `visit` overload per event kind; adding a sixth event meant
adding a method to the visitor interface and to every class that
implemented it, a change that rippled across files far from the event
types themselves. The plainer alternative, a chain of `instanceof` checks
and casts inside one method, kept the ripple contained to that one method
but traded away the compiler's help: a forgotten branch compiled cleanly
and only failed once a shipment reached the missing case in production.

A pattern-matching switch over `ShipmentEvent` keeps the ripple contained
the way the `instanceof` chain did, and gets the compile-time completeness
check the Visitor needed a second interface to provide:

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

Every arm deconstructs its record directly, pulling `at`, `facility`, or
`reason` straight out of the matched pattern rather than calling an
accessor afterward. There is no `default` arm here, and none is needed:
the compiler already knows `ShipmentEvent` permits exactly these five
shapes, and a sixth record added to the `permits` list without a matching
`case` here fails the build rather than waiting for a shipment to exercise
the gap. `ShipmentEventNarrator.requiresCustomerNotification` pushes the
same idiom further with a guarded pattern, matching a `Delayed` event and
reading its `estimatedExtraTime` component directly inside the `case`
label to decide whether the delay is long enough to notify a customer
about:

```java
case ShipmentEvent.Delayed(var at, var reason, var extra)
        when extra.compareTo(Duration.ofHours(2)) > 0 -> true;
```

`ShipmentEventNarratorTest` exercises all five event shapes through
`describe` and both branches of the guarded `Delayed` pattern through
`requiresCustomerNotification`, which is the proof that the exhaustiveness
claim here is a checked fact rather than an assumption about the sealed
hierarchy's current shape.

## Optional retires the Null Object and the scattered null check

A customer may or may not have a preferred carrier on file. That
possible absence used to resolve one of two ways: a lookup returning
`null`, which pushed a null check onto every caller and made forgetting
one check a `NullPointerException` waiting for the right input, or a Null
Object, a stand-in that implemented the same contract as a real preference
and answered every call with a harmless default, which kept callers free
of null checks at the cost of a whole extra class whose only job was to do
nothing safely. `PreferredCarrierDirectory` resolves the same force by
returning `Optional<String>` directly:

```java
public Optional<String> preferredCarrierFor(String customerId) {
    Objects.requireNonNull(customerId, "customerId");
    return Optional.ofNullable(preferredCarrierByCustomerId.get(customerId));
}
```

`ShipmentRouter.carrierToUse` is the caller, and its body never asks
whether what it got back is a real preference or a stand-in, because
`Optional` already carries that distinction in its type:

```java
public String carrierToUse(String customerId, String defaultCarrier) {
    return directory.preferredCarrierFor(customerId)
            .map(carrier -> carrier.toUpperCase(Locale.ROOT))
            .orElse(defaultCarrier.toUpperCase(Locale.ROOT));
}
```

`map` transforms the value only if one is present, and `orElse` supplies
the fallback only if one is not, so the method reads as a single
composed expression instead of a branch. `PreferredCarrierDirectoryTest`
checks both the present and the absent case directly, and
`ShipmentRouterTest` checks that routing falls back correctly when nothing
is on file, which is the scenario a forgotten null check used to miss and
a Null Object used to paper over rather than surface.

## Other idioms the same language release retires

Three more retirements belong in this chapter even without a dedicated
example, because naming them completes the picture this book has been
building one chapter at a time. Try-with-resources retires the
hand-written try/finally block for anything that implements
`AutoCloseable`: the compiler generates the close call, in the right
order for multiple resources, even when the body throws, which used to
require a nested try/finally an author had to get right by hand every
time a method opened more than one resource. A switch expression retires
the fall-through switch statement: every arm yields a value directly, a
missing `break` cannot fall through into the next case because there is no
fall-through to have, and the compiler rejects a switch expression over an
enum or a sealed type that leaves a case unhandled. `var` does not retire
a pattern so much as it retires a tax: repeating a type name once on the
left of a declaration and again on the right, for a local variable whose
type is already obvious from its initializer, adds nothing a reader could
not already infer, and removing that repetition is pure ceremony reduction
with no new idiom underneath it.

> **AI sidebar.** Ask an LLM pairing partner to add a sixth event to
> `ShipmentEvent`, and the risk is not that it writes the record
> incorrectly. It is that it hands back a new `permits` entry with no
> accompanying edit to `ShipmentEventNarrator`, because the model's
> training data is full of `switch` statements with a `default` branch
> that silently absorbs an unhandled case, and a generated edit that adds
> a case to that kind of switch looks complete even when it is not. The
> check that catches this is mechanical: after accepting a new sealed
> subtype, run the build before accepting the rest of the diff. A switch
> over a sealed type with no `default` arm fails to compile the moment a
> case goes unhandled, which turns a model's silent omission into an
> error you see immediately instead of a gap a shipment finds later. The
> same habit shows up in reverse: ask for "a singleton" or "a value class"
> for an unfamiliar domain, and a model trained on a large, older corpus
> will sometimes hand back double-checked locking or a hand-rolled
> `equals`/`hashCode` pair, because both are common in the training
> material even though neither is the idiom current Java rewards. Reading
> the generated code against the four retirements in this chapter is a
> fast, specific check: if a lock, a `volatile` field, a hand-written
> `equals`, or a `null` return shows up, the generated shape reached back
> for an idiom this book's whole arc has already replaced.

## When the older idiom is still the right call

None of these four retirements is unconditional, and reaching for any of
them as a reflex trades one mistake for another.

An enum singleton or a holder idiom needs the type it wraps to be
constructible once with no further coordination after that. A resource
that must be closed and reopened, rotated, or replaced at runtime, a
rate-limited external connection, a certificate that expires and needs
reloading, does not fit either modern singleton shape, and reaching for
one anyway just relocates the coordination problem into a type that
promises callers it will never have one. Lower-level library code that
needs to control exactly when a lock is acquired, independent of class
loading, is one of the few remaining places double-checked locking or an
explicit lock still justifies its complexity, though even there a
`java.util.concurrent` primitive usually reads more clearly than
reimplementing it.

A record needs its state to be complete and immutable at construction. A
domain entity with a database-assigned identity that changes after an
insert, a mutable accumulator a loop updates on every iteration, or any
type whose equality is meant to track identity rather than content, a
persistence-managed entity being the clearest case, does not fit a
record's all-fields equality, and forcing one into record shape trades
away the exact distinction the type was supposed to express.

A sealed interface needs the set of implementations to be closed and
known by the team that owns it. A plugin architecture where third-party
code supplies new implementations at runtime needs an open interface
precisely because the maintainer cannot enumerate every implementation in
advance; sealing that interface would not express a real constraint.

`Optional` fits as a return type for "this lookup might not find
anything," and it stops paying for itself the moment it leaves that role.
A field, a constructor parameter, or a collection element typed as
`Optional` usually means the type that holds it should not have allowed
absence in the first place, or should express it with a smaller sentinel
than a full `Optional` wrapper. The idiom retires a return-type problem,
not every place `null` has ever appeared in a codebase.

The method behind every one of these judgments is the same one this book
opened with: name the force present in a specific piece of code,
and let that force choose the idiom, modern or otherwise, rather than
choosing the idiom first and looking for a force to justify it afterward.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this
loop. From the repository root, the aggregator picks up
`examples/23-idiom-retirement` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/23-idiom-retirement
mvn test
```

Either path runs the same 27 JUnit 5 cases, asserted with AssertJ,
including a 32-thread race against `ConfirmationNumberGenerator`, a pair
of defensive-copy proofs on `DeliveryManifest`, a full sweep of
`ShipmentEventNarrator`'s pattern-matching switches across all five event
shapes, and a present/absent pair of cases against the `Optional`-returning
lookup:

```
Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see all four retirements run side by side rather than just reading an
assertion that says they behave correctly, run the example's own demo
script:

```sh
examples/23-idiom-retirement/demo.sh
```

It prints a confirmation number pair from the enum singleton, a
canonicalized address and a defensively copied manifest, every
`ShipmentEvent` shape narrated through the exhaustive switch, and a
routing decision made both with and without a preference on file:

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

The delayed event's extra time crosses the two-hour notification
threshold, which is why its narration reports `notify customer: true`
while the pickup event, carrying no such state, reports `false`. The
second routing line falls back to the ground carrier with no preference on
file, proving the same composed `Optional` chain handles both outcomes.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original Visitor entry, whose double-dispatch shape `ShipmentEvent`'s pattern-matching switch replaces for this closed hierarchy.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 3 on the enum singleton, Item 17 on minimizing mutability behind the idea a record now enforces by construction, and Item 55 on using `Optional` judiciously, including the fields-and-parameters caveat this chapter's closing section repeats.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/23-idiom-retirement`'s suite, including the concurrent-access proof against `ConfirmationNumberGenerator`.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
