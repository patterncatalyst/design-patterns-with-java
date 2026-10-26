---
title: "Records, sealed interfaces, and pattern matching"
order: 21
layout: tutorial
sectionid: docs
part: "idiomatic-java-expressions"
description: "A shipping rating engine built from three closed, sealed record hierarchies, ParcelKind, ShippingEvent, and RateRule, operated on by exhaustive pattern-matching switches that use type patterns, a guarded when clause, and record deconstruction patterns nested three levels deep, with no visitor interface and no accept() method anywhere."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Declare a record as a transparent, immutable data carrier, and use a
  compact constructor to enforce a real invariant once, in exactly one
  place, instead of on every caller that builds one.
- Declare a sealed interface over a closed, known set of record kinds, and
  say what the compiler guarantees about a `switch` that dispatches over it.
- Write an exhaustive pattern-matching `switch` with a type pattern, a
  guarded `when` clause, and a record deconstruction pattern, and explain
  what each buys over the others for a given arm.
- Nest a record deconstruction pattern two and three levels deep, reading
  a far-inside component directly into a name with no accessor call
  anywhere in that arm.
- Name, by pattern name, how a closed hierarchy of records plus an external
  exhaustive switch expresses the same forces as State, Visitor, and
  Interpreter, without a class hierarchy, an `accept()` method, or a
  visitor interface anywhere.
- Recognize when an open, growing type set needs ordinary polymorphism
  instead of a sealed switch, and say what to write instead.

## Why this matters

A rating engine has to answer three different questions about the same
shipment, and each question has its own closed set of possibilities behind
it. What kind of parcel is this, a plain box, a fragile item with a
handling note, a cold-chain shipment with a temperature band, or something
too large for a standard belt? What happened to it just now, was a quote
requested, a label bought, a hub scan recorded, an exception raised, or
a delivery confirmed? And given a pricing rule built from a flat amount, a
per-kilogram rate, a zone surcharge, and a floor, nested however a rate
card author chose to nest them, what does the whole rule price out to for
this parcel, to this zone, right now?

Every one of those three questions used to cost a small amount of class
hierarchy, a getter, a hand-written `equals()` and `hashCode()`, defensive
copying wherever a caller might hold a reference a bit too long, and either
an `instanceof` chain with a cast on every branch or a visitor interface
with an `accept()` method bolted onto every type in the hierarchy. Java 25
answers all three with the same small toolkit: a record for the data, a
sealed interface for the closed set, and a pattern-matching `switch` for
the operation. That toolkit is not three unrelated language features bundled
into one chapter by coincidence. A record is a data carrier whose shape the
compiler can see completely. A sealed interface tells the compiler that
shape's possible variants are closed and known. Pattern matching for
`switch` is what lets an operation ask "which variant, and what parts of
it" in one expression, checked for completeness at compile time rather than
at whatever hour a forgotten case first reaches production. Each piece
makes the next one pay off further than it would alone.

This style has a name: data-oriented programming.
The data lives in records that carry no behavior of their own beyond what a
compact constructor validates. The behavior lives in ordinary static
methods, each one an exhaustive `switch` over the sealed set the data
belongs to. Nothing here is a hand-wave at functional programming; it is
the same separation of data from the operations performed on it that the
Gang of Four's State, Visitor, and Interpreter patterns were already
reaching for, done with language support those patterns' original authors
in 1994 did not have.

{% include excalidraw.html name="21-records-sealed-switches" caption="Figure 21.1: ParcelKind's four record variants on the left, three external operations on the right, each one its own exhaustive switch with no accept() method anywhere on the record types." %}

## The domain: parcels, events, and a rate card

The example behind this chapter lives in
`examples/21-records-sealed-patterns`. `ParcelKind` is a sealed interface
over exactly four record kinds: `StandardParcel`, `FragileParcel`,
`RefrigeratedParcel`, and `OversizedParcel`. Every kind carries a
`weightKg`; the other three carry whatever extra data their own handling
needs:

```java
public sealed interface ParcelKind
        permits StandardParcel, FragileParcel, RefrigeratedParcel, OversizedParcel {

    double weightKg();
}

public record RefrigeratedParcel(double weightKg, int minTempC, int maxTempC) implements ParcelKind {

    public RefrigeratedParcel {
        if (weightKg <= 0.0) {
            throw new IllegalArgumentException("weightKg must be positive, was " + weightKg);
        }
        if (minTempC >= maxTempC) {
            throw new IllegalArgumentException(
                    "minTempC (%d) must be less than maxTempC (%d)".formatted(minTempC, maxTempC));
        }
    }
}
```

`RefrigeratedParcel`'s compact constructor runs before any field is
assigned, on every path that ever constructs one, including the canonical
constructor a caller invokes directly. There is exactly one place a
negative weight or an inverted temperature band can be rejected, and it is
this one; no setter, no builder, and no second constructor exists that
could skip it. `ShippingEvent` is a second sealed interface over
`QuoteRequested`, `LabelPurchased`, `HubScan`, `DeliveryException`, and
`Delivered`, each carrying the `ParcelKind` it concerns. `RateRule` is a
third sealed interface over `FlatRate`, `PerKilogram`, `ZoneSurcharge`, and
`MinimumCharge`, a small grammar a rate card author builds by nesting these
four kinds around each other.

## Records: data carriers that validate once and never lie

A record's canonical constructor, accessors, `equals()`, `hashCode()`, and
`toString()` all come from declaring its component list once. What a
record adds beyond a plain immutable class is the compact constructor,
code that runs on every construction path with no field assignment
statements of its own; a parameter reassigned inside a compact constructor
is what the canonical constructor assigns, so validation and
normalization both happen in the same place a reader already expects to
find them. `OversizedParcel`'s compact constructor rejects a non-positive
dimension the same way `RefrigeratedParcel`'s rejects an inverted
temperature band, and `QuoteRequested`'s rejects a blank destination zone:

```java
public record QuoteRequested(ParcelKind parcel, String destinationZone) implements ShippingEvent {

    public QuoteRequested {
        Objects.requireNonNull(parcel, "parcel");
        Objects.requireNonNull(destinationZone, "destinationZone");
        if (destinationZone.isBlank()) {
            throw new IllegalArgumentException("destinationZone must not be blank");
        }
    }
}
```

Nothing about this class resembles the hand-rolled immutable value classes
a Java codebase wrote before records existed: a private final field per
component, a constructor that repeats each parameter name once per field
assignment, a getter per field with no setter, an `equals()` comparing
every field pairwise, a `hashCode()` combining the same fields in the same
order, and a `toString()` listing them again. Every one of those five
pieces is exactly the kind of code a reviewer checks for correctness by
eye and a refactor forgets to update when a sixth component gets added
later. A record makes all five derive from one component list, so a sixth
component added to `QuoteRequested` updates its accessor, its equality,
and its string form automatically; only the compact constructor's
validation is something a human still has to extend by hand, because only
a human knows what the new component's invariant should be.

## Sealed interfaces: a closed set the compiler checks

`ParcelKind`'s `permits` clause names its four implementors explicitly, the
same discipline `ShippingEvent` and `RateRule` apply to their own five and
four. A sealed interface is not merely documentation that says "these are
the intended subtypes, please do not add a fifth without checking first."
It is an instruction to `javac`: no type outside this `permits` list can
ever implement this interface, not from another package in the same
module, not from a different module entirely, not at compile time and not
through any runtime trick that would require unverifiable bytecode. That
closure is what lets `ParcelKinds`, `ShippingEvents`, and `RateRules`, the
three operations classes this example builds, write a `switch` with no
`default` branch at all:

```java
public static double billableWeightKg(ParcelKind parcel) {
    return switch (parcel) {
        case OversizedParcel(var weightKg, var lengthCm, var widthCm, var heightCm) -> {
            double volumetricWeightKg = (lengthCm * widthCm * heightCm) / VOLUMETRIC_DIVISOR;
            yield Math.max(weightKg, volumetricWeightKg);
        }
        case StandardParcel(var weightKg) -> weightKg;
        case FragileParcel(var weightKg, var handlingNote) -> weightKg;
        case RefrigeratedParcel(var weightKg, var minTempC, var maxTempC) -> weightKg;
    };
}
```

`javac` refuses to compile this method unless every one of `ParcelKind`'s
four permitted types has a matching `case`. Add a fifth record to the
`permits` clause tomorrow, for a parcel that needs signature confirmation
on delivery, and this `switch`, along with every other `switch` over
`ParcelKind` anywhere in the codebase, fails to compile until a human adds
a case for it. No code path silently falls through to a `default` that
quietly does the wrong thing for a kind nobody told it about; the build
itself is the enforcement mechanism, caught by whichever developer runs
the compiler next, not by whichever customer's shipment happens to be the
first one routed through the missing branch.

## Pattern matching for switch: type patterns, guards, and deconstruction

A `case` arm in a pattern-matching `switch` can bind the whole matched
value under a type pattern, the way `ShippingEvents.requiresPriorityHandling`
binds a `DeliveryException`'s parcel specifically as a `RefrigeratedParcel`
and then narrows the match further with a guarded `when` clause:

```java
public static boolean requiresPriorityHandling(ShippingEvent event) {
    return switch (event) {
        case DeliveryException(RefrigeratedParcel parcel, var reason) when parcel.maxTempC() <= COLD_CHAIN_CEILING_C ->
                true;
        case DeliveryException e -> false;
        case QuoteRequested q -> false;
        case LabelPurchased l -> false;
        case HubScan h -> false;
        case Delivered d -> false;
    };
}
```

That first arm does two things in one line. `DeliveryException(RefrigeratedParcel parcel, var reason)`
is a record deconstruction pattern that only matches a `DeliveryException`
whose `parcel` component is specifically a `RefrigeratedParcel`, binding it
to the name `parcel` rather than the generic `ParcelKind` the interface
declares; a `DeliveryException` wrapping a `StandardParcel` or an
`OversizedParcel` does not match this arm at all. The `when` clause that
follows runs only once that type check already passed, testing
`parcel.maxTempC()` against a cold-chain ceiling; a `RefrigeratedParcel`
whose ceiling is comfortably above freezing falls through to the plain
`case DeliveryException e -> false` arm immediately below, because a
guarded pattern that fails its guard is a non-match, not an error, and
`switch` keeps trying the remaining arms in order exactly as written. Only
a cold-chain shipment that both is refrigerated and is at serious risk of
spoiling escalates; every other delivery exception, including a different
exception on the exact same refrigerated parcel whose temperature reading
looks fine, does not.

`ShippingEvents.describe` shows the same deconstruction one layer deeper,
reaching inside a `QuoteRequested`'s `ParcelKind` component to check
whether it is specifically a `RefrigeratedParcel`, and if so, pulling that
parcel's own three components out in the same pattern:

```java
case QuoteRequested(RefrigeratedParcel(var weightKg, var minTempC, var maxTempC), var zone) ->
        "Quote requested for a %.1fkg refrigerated parcel (keep %dC to %dC) to zone %s"
                .formatted(weightKg, minTempC, maxTempC, zone);
case QuoteRequested(var parcel, var zone) ->
        "Quote requested for a %.1fkg parcel to zone %s".formatted(parcel.weightKg(), zone);
```

Reading that first arm left to right: match a `QuoteRequested`, whose
first component must itself match a `RefrigeratedParcel` pattern,
deconstructed into `weightKg`, `minTempC`, and `maxTempC` directly, with
`zone` bound from `QuoteRequested`'s second component. No code anywhere in
that arm calls `.parcel()` and then `.weightKg()` on the result; the
nesting in the pattern does both steps at once. The second, more general
arm catches every `QuoteRequested` whose parcel is not a
`RefrigeratedParcel`, reading `parcel.weightKg()` through the plain
interface accessor because there is no more specific shape left to
destructure. Order matters here the same way it mattered in
`requiresPriorityHandling`: the specific `RefrigeratedParcel` arm has to
come before the generic one, or the generic arm would catch every
`QuoteRequested` first and the specific arm would never run.

## Record deconstruction patterns, nested three levels deep

`RateRule`'s four kinds are built to be nested around each other, a
`ZoneSurcharge` wrapping a `PerKilogram`, a `MinimumCharge` wrapping that
`ZoneSurcharge`, and so on as deep as a rate card needs. `RateRules.quote`
walks whatever tree a caller built with ordinary recursion, one layer of
pattern per call:

```java
public static Money quote(RateRule rule, ParcelKind parcel, String destinationZone) {
    return switch (rule) {
        case FlatRate(var amount) -> amount;
        case PerKilogram(var ratePerKg) -> ratePerKg.timesWeight(ParcelKinds.billableWeightKg(parcel));
        case ZoneSurcharge(var base, var zone, var surcharge) when zone.equals(destinationZone) ->
                quote(base, parcel, destinationZone).plus(surcharge);
        case ZoneSurcharge(var base, var zone, var surcharge) -> quote(base, parcel, destinationZone);
        case MinimumCharge(var base, var floor) -> {
            Money computed = quote(base, parcel, destinationZone);
            yield computed.isGreaterThan(floor) ? computed : floor;
        }
    };
}
```

`RateRules.describe` goes further, matching three layers in a single
pattern rather than recursing through them one call at a time:

```java
public static String describe(RateRule rule) {
    return switch (rule) {
        case MinimumCharge(ZoneSurcharge(PerKilogram(var ratePerKg), var zone, var surcharge), var floor) ->
                "max(%s/kg + %s in zone %s, floor %s)".formatted(ratePerKg, surcharge, zone, floor);
        case MinimumCharge(var base, var floor) -> "max(%s, floor %s)".formatted(describe(base), floor);
        case ZoneSurcharge(var base, var zone, var surcharge) ->
                "%s + %s in zone %s".formatted(describe(base), surcharge, zone);
        case PerKilogram(var ratePerKg) -> "%s/kg".formatted(ratePerKg);
        case FlatRate(var amount) -> amount.toString();
    };
}
```

The first arm matches a `MinimumCharge` whose `base` is itself a
`ZoneSurcharge`, whose own `base` is itself a `PerKilogram`, reading
`ratePerKg`, `zone`, `surcharge`, and `floor` straight into four local
names in one `case` line. Nothing calls `.base()` three times to get from
the outside of that tree to the inside; the nested pattern does it in one
match. A `MinimumCharge` built around anything other than that exact
three-layer shape, a flat `MinimumCharge(FlatRate, floor)`, for instance,
does not match the first arm and falls through to the second, which
recurses through `describe()` instead of pattern-matching the whole shape
at once. Both are valid Java; the nested form reads better when a single
arm needs to act on a known, specific shape of the whole tree, and
recursion reads better when an arm's logic is the same regardless of
what shape the nested piece happens to be.

What nesting changes is not just how an arm reads; it changes which rule a
given tree represents. A `MinimumCharge` wrapped around a `ZoneSurcharge`
applies its floor after the surcharge has already been added, so a
zone-destined shipment priced above the floor keeps its surcharge in full.
A `ZoneSurcharge` wrapped around a `MinimumCharge` applies the floor first
and then adds the surcharge on top of whichever is larger, the floor or
the raw per-kilogram price, so even a shipment priced below the floor still
picks up the surcharge. The same three pieces, a per-kilogram rate, a zone
surcharge, and a floor, nested two different ways quote two different
totals for the identical twelve-kilogram shipment to the identical zone:
$40.00 one way, $42.00 the other. That is Interpreter's whole claim, that
the structure itself, not a separate configuration flag, decides what an
expression evaluates to, done here with the same record-pattern
mechanism this chapter already used for `ShippingEvents`.

## What this replaces: State, Visitor, and Interpreter by name

None of the three operations classes in this example, `ParcelKinds`,
`ShippingEvents`, or `RateRules`, declares an interface named after a
visitor, and none of the twelve record types in `ParcelKind`,
`ShippingEvent`, or `RateRule` declares an `accept()` method. That absence
is not an oversight; it is the entire argument this chapter makes. Visitor
answers the force of a fixed structure that keeps growing new operations
over it, and an exhaustive `switch` answers that same force directly: add
`ParcelKinds.anotherOperation()` tomorrow and every existing record stays
untouched, the same guarantee classic Visitor gives through an
`accept()`-and-`visit()` pair, with one fewer interface and zero
boilerplate methods on the data itself. State answers the force of
behavior that depends on which of a known, closed set of conditions
currently holds, and `ShippingEvents.requiresPriorityHandling` answers it
the same way, dispatching on which `ShippingEvent` kind is present rather
than on a flag or a status field checked by hand. Interpreter answers the
force of a small grammar whose nesting decides a computed result, and
`RateRules.quote` is exactly that grammar, walked by a `switch` instead of
by a tree of interpreter objects each implementing its own `evaluate()`
method.

Records plus sealed interfaces plus pattern matching do not retire these
three Gang of Four patterns' intent. A codebase still needs to decide "an
operation over a fixed structure," "behavior keyed on a closed set of
conditions," and "a grammar whose nesting decides an outcome" are real
forces present in its own domain before reaching for any of this chapter's
constructs. What changes is the cost of answering yes. Where the original
patterns needed a dedicated interface and a parallel method on every
participating type, this chapter's toolkit needs a `permits` clause the
compiler already checks and a `switch` with no `default` branch. The force
is the same; the ceremony required to answer it correctly is not.

> **AI sidebar.** Ask an LLM pairing partner to "add a new kind of parcel"
> to this example, and watch for whether the generated change touches a
> file outside the one that declares the new record and the `permits`
> clause that names it. A model that reaches for Visitor's
> `accept()`-and-`visit()` shape out of habit will often add an
> `accept(ParcelVisitor visitor)` method to every existing record just to
> accommodate the new one, even though nothing about this closed, known
> parcel set asked for that. A model asked to add an operation instead,
> "add a method that flags a parcel needing a signature on delivery," is
> more likely to produce the right shape, a new static method in
> `ParcelKinds` with its own exhaustive `switch`, because that is the
> pattern already present to imitate. The more useful check runs the other
> direction: feed the model a `switch` over `ParcelKind` that is missing a
> case for `OversizedParcel` and ask it to find the bug. A model that
> reasons from the `permits` clause will say the build does not compile
> before it says anything about runtime behavior; a model that pattern-
> matches against Java it has seen without checking this specific sealed
> hierarchy may describe a plausible-sounding runtime failure instead,
> missing that `javac` catches this one before the program ever runs.

## When not to use it

A sealed hierarchy and an exhaustive `switch` pay for themselves exactly
when the set of variants is closed and known at compile time, the same
condition that made Visitor and State worth their ceremony in the
first place. A `ParcelKind` that a third-party carrier integration needs
to extend at runtime, registering a fifth kind this codebase has never
seen and will never recompile against, cannot be sealed at all; `permits`
demands every implementor be known and compiled together, which is
precisely what a plugin architecture cannot promise. An open type set that
grows from outside the project wants ordinary polymorphism instead, an
interface with no `permits` clause and a `default` method or two, accepting
that the compiler can no longer prove every case is handled and some
runtime fallback has to cover whatever shows up next.

The reverse trap is reaching for this toolkit when a single record would
do. A type with exactly one shape and no sibling variants does not need a
sealed interface wrapped around it; a plain record implementing nothing
is the whole answer. And a record deconstruction pattern nested three
layers deep, the way `RateRules.describe`'s first arm is, earns its
density only when an arm needs to act on that exact, specific shape;
forcing every arm in a `switch` into the deepest possible nested
pattern, when a shallow `var` binding and one accessor call would read
just as clearly, trades a small amount of typing for a pattern a future
reader has to parse mentally layer by layer before finding the one piece
it uses.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this
loop. From the repository root, the aggregator picks up
`examples/21-records-sealed-patterns` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/21-records-sealed-patterns
mvn test
```

Either path runs the same 32 JUnit 5 cases, asserted with AssertJ,
including a case that quotes two differently-nested `RateRule` trees built
from identical pieces and asserts they produce different totals, not just
that each one evaluates without throwing. Observed locally on Fedora with
Temurin 25.0.2:

```
Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see all three hierarchies run against one shipment rather than just
reading an assertion that says they work, run the example's own demo
script:

```sh
examples/21-records-sealed-patterns/demo.sh
```

It rejects an invalid `RefrigeratedParcel` from its compact constructor,
computes billable weight for a refrigerated and an oversized parcel,
describes shipping events through the nested record pattern, checks a
guarded `when` clause against a cold-chain breach and a routine exception,
and quotes the identical `RateRule` tree to two different destination
zones:

```
== Records: validation runs once, in the compact constructor ==
Rejected: minTempC (10) must be less than maxTempC (4)

== ParcelKinds: billable weight, actual vs. dimensional ==
Refrigerated parcel billable weight: 8.0kg
Oversized parcel billable weight: 115.2kg

== ShippingEvents: describe(), including a nested record pattern ==
Quote requested for a 8.0kg refrigerated parcel (keep -5C to 4C) to zone EAST
Delivery exception: cold storage unit failed in transit (8.0kg)
Delivered (12.0kg)

== ShippingEvents: requiresPriorityHandling(), a guarded when clause ==
Cold-chain breach (ceiling 0C): true
Routine exception on a standard parcel: false

== RateRules: nesting the same rule pieces decides the quote ==
max($1.50/kg + $5.00 in zone EAST, floor $20.00)
Quote to EAST (surcharge applies, above the floor): $23.00
Quote to WEST (surcharge skipped, floor takes over): $20.00
```

The last block's two different totals for the identical `RateRule` tree
quoted against two different zones is the guarded `when` clause changing
which path `quote()` takes, visible at the terminal rather than only in an
assertion.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480/9780321700698) (Addison-Wesley): the original State, Visitor, and Interpreter entries this chapter's sealed hierarchies and exhaustive switches answer the same forces as, without a class hierarchy or an `accept()` method anywhere.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 17 on minimizing mutability is the discipline behind every record in this chapter, and Item 10 on the `equals()` contract is what a record's generated `equals()` satisfies automatically, with no hand-written implementation to get wrong.
- The official Java SE language documentation on [record classes](https://docs.oracle.com/en/java/javase/25/language/records.html), [sealed classes and interfaces](https://docs.oracle.com/en/java/javase/25/language/sealed-classes-interfaces.html), and [pattern matching](https://docs.oracle.com/en/java/javase/25/language/pattern-matching.html): the authoritative specification behind the compact constructor, the `permits` clause, and the record deconstruction patterns this chapter's example relies on.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/21-records-sealed-patterns`'s suite, which asserts that two differently-nested `RateRule` trees quote different totals and that every compact constructor rejects its specific invalid input, rather than only checking that each class constructs without error.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
