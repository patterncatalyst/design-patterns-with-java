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

- Declare a record as an immutable data carrier and enforce its invariant once, in a compact constructor.
- Close a set of record kinds with a sealed interface, and say what the compiler then checks about a `switch` over it.
- Write an exhaustive `switch` with a type pattern, a guarded `when` clause and a record deconstruction pattern.
- Nest a deconstruction pattern two and three levels deep to read an inner component with no accessor call.
- Name how a sealed record hierarchy plus an external `switch` covers State, Visitor and Interpreter without an `accept()` method.
- Recognize an open type set that needs ordinary polymorphism instead.

## Why this matters

A rating engine asks three questions about a shipment, and each has a closed set of answers. What kind of parcel is it: standard, fragile, refrigerated or oversized? What just happened to it: a quote, a label, a hub scan, an exception, a delivery? And what does a rate rule, nested however the rate card author nested it, price out to?

Each question used to cost a class hierarchy, getters, a hand-written `equals()` and `hashCode()`, defensive copies, and either an `instanceof` chain with a cast per branch or a visitor interface with an `accept()` on every type. Java answers all three with one toolkit: a record for the data, a sealed interface for the closed set, and a pattern-matching `switch` for the operation. The compiler sees the record's whole shape, knows the variants are closed, and checks the `switch` for completeness at compile time.

The style is called data-oriented programming. Records hold the data and validate it in a compact constructor; static methods hold the behavior, each an exhaustive `switch` over a sealed set. It's the separation of data from operations that State, Visitor and Interpreter were reaching for, with language support those patterns didn't have in 1994.

{% include excalidraw.html name="21-records-sealed-switches" caption="Figure 21.1: ParcelKind's four record variants on the left, three external operations on the right, each one its own exhaustive switch with no accept() method anywhere on the record types." %}

## The domain: parcels, events, and a rate card

The code lives in `examples/21-records-sealed-patterns`. `ParcelKind` is a sealed interface over four records: `StandardParcel`, `FragileParcel`, `RefrigeratedParcel` and `OversizedParcel`. Every kind carries a `weightKg`; the others add what their handling needs:

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

Two more sealed interfaces complete the domain:

| Sealed interface | Permitted records |
| --- | --- |
| `ShippingEvent` | `QuoteRequested`, `LabelPurchased`, `HubScan`, `DeliveryException`, `Delivered`, each carrying its `ParcelKind` |
| `RateRule` | `FlatRate`, `PerKilogram`, `ZoneSurcharge`, `MinimumCharge`, a small grammar a rate card nests |

## Records: data carriers that validate once

Declaring the component list gives a record its canonical constructor, accessors, `equals()`, `hashCode()` and `toString()`. The compact constructor runs on every construction path before the fields are assigned, and a parameter reassigned inside it is what gets stored, so validation and normalization live in one place. With no setter, builder or second constructor, nothing can skip it. `OversizedParcel` rejects a non-positive dimension the same way, and `QuoteRequested` rejects a blank zone:

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

A record retires the hand-rolled immutable value class: a private final field per component, a constructor that assigns each one, a getter per field, and `equals()`, `hashCode()` and `toString()` that each list the fields again. Add a component to `QuoteRequested` and its accessor, equality and string form follow. Only the validation needs a human, because only a human knows the new component's invariant.

## Sealed interfaces: a closed set the compiler checks

The `permits` clause is an instruction to `javac`: no type outside the list may implement the interface, from any package or module. That closure lets `ParcelKinds`, `ShippingEvents` and `RateRules` switch with no `default` branch:

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

Drop one arm and the build stops with `the switch expression does not cover all possible input values`. Add a fifth record to `permits`, for a parcel that needs a signature, and every `switch` over `ParcelKind` fails the same way until someone adds its case. No `default` quietly mishandles a kind it was never told about; the developer who runs the compiler next finds the gap, not the first customer whose parcel takes the missing branch.

## Pattern matching for switch: type patterns, guards, and deconstruction

An arm can deconstruct a record, narrow a component's type, and then test it with a `when` guard:

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

The first arm matches only a `DeliveryException` whose parcel is a `RefrigeratedParcel`, bound as that type rather than as `ParcelKind`. The guard runs after the type check. A guard that fails is a non-match, not an error, so a refrigerated parcel with a safe ceiling falls through to `case DeliveryException e -> false`. Only a cold-chain shipment at risk of spoiling escalates.

`ShippingEvents.describe` reaches one layer deeper and pulls the refrigerated parcel's three components out in the same pattern:

```java
case QuoteRequested(RefrigeratedParcel(var weightKg, var minTempC, var maxTempC), var zone) ->
        "Quote requested for a %.1fkg refrigerated parcel (keep %dC to %dC) to zone %s"
                .formatted(weightKg, minTempC, maxTempC, zone);
case QuoteRequested(var parcel, var zone) ->
        "Quote requested for a %.1fkg parcel to zone %s".formatted(parcel.weightKg(), zone);
```

The first arm binds `weightKg`, `minTempC`, `maxTempC` and `zone` with no `.parcel().weightKg()` chain. The second catches every other parcel kind and reads the weight through the interface accessor. The specific arm must come first. Put the unguarded general arm above it and `javac` rejects the specific one with `this case label is dominated by a preceding case label`; the same holds for the two `DeliveryException` arms above.

## Record deconstruction patterns, nested three levels deep

`RateRule` kinds nest around each other: a `ZoneSurcharge` around a `PerKilogram`, a `MinimumCharge` around that, as deep as a rate card needs. `RateRules.quote` walks the tree by recursion, one layer per call:

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

`RateRules.describe` matches three layers in one pattern instead:

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

The first arm reads `ratePerKg`, `zone`, `surcharge` and `floor` from three layers in one `case` line. Any other `MinimumCharge`, such as one around a `FlatRate`, falls to the second arm, which recurses. Use the nested form when an arm acts on one known shape of the whole tree, and recursion when the logic is the same whatever the inner piece is.

Nesting also decides which rule the tree means. For a 12 kg parcel at $1.50/kg, a $22.00 surcharge to its zone and a $20.00 floor:

| Tree | Evaluation | Quote |
| --- | --- | --- |
| `MinimumCharge(ZoneSurcharge(PerKilogram, ...), floor)` | 18.00 + 22.00 = 40.00, above the floor | $40.00 |
| `ZoneSurcharge(MinimumCharge(PerKilogram, floor), ...)` | max(18.00, 20.00) = 20.00, then + 22.00 | $42.00 |

That's Interpreter's claim: the structure of the expression, not a configuration flag, decides what it evaluates to.

## What this replaces: State, Visitor, and Interpreter by name

None of `ParcelKinds`, `ShippingEvents` or `RateRules` declares a visitor interface, and none of the twelve records declares an `accept()` method.

| GoF pattern | Its problem | The sealed-switch answer |
| --- | --- | --- |
| Visitor | New operations over a fixed structure | A new static method with its own exhaustive `switch`; no record changes |
| State | Behavior keyed on a closed set of conditions | `requiresPriorityHandling` dispatches on which `ShippingEvent` kind is present, not on a status field |
| Interpreter | A small grammar whose nesting decides a result | `RateRules.quote` walks the `RateRule` tree with a `switch`, not with an `evaluate()` per node class |

The patterns' intent survives. A codebase still has to decide that it has a fixed structure, a closed set of conditions or a nesting grammar. What drops is the cost: a `permits` clause the compiler checks and a `switch` with no `default`, instead of a dedicated interface and a parallel method on every type.

> **🤖 Explore with your agent**
>
> "Add a `SignatureRequiredParcel(double weightKg, String recipientName)` record to `examples/21-records-sealed-patterns` and name it in the `permits` clause of `ParcelKind`. Validate a positive weight and a non-blank name in its compact constructor. Add one `case` for it to every `switch` over `ParcelKind` in `ParcelKinds`, `ShippingEvents` and `RateRules`, with no `default` branch anywhere. Do not add an `accept()` method or a visitor interface, and do not change any other record."
>
> Check: `mvn -q -f examples/21-records-sealed-patterns/pom.xml test -Dtest=ParcelKindsTest`, and confirm the diff touches no existing record.

## When not to use it

- **The type set is open.** A carrier integration that registers a fifth `ParcelKind` at runtime can't be sealed, because `permits` needs every implementor compiled together. Use an interface with no `permits` clause and polymorphic methods, and accept that some runtime fallback covers what shows up next.
- **There is one shape.** A type with no sibling variants is a plain record; don't wrap it in a sealed interface.
- **The nesting is deeper than the arm needs.** A three-level pattern like the first arm of `RateRules.describe` earns its density only when the arm acts on that exact shape. Where a shallow `var` binding and one accessor read as clearly, use them.

## Build, run, observe

From the repository root:

```sh
mvn -f examples/pom.xml test
```

Or standalone:

```sh
cd examples/21-records-sealed-patterns
mvn test
```

All 32 tests pass, including the two differently nested `RateRule` trees that quote $40.00 and $42.00. The demo script runs all three hierarchies against one shipment:

```sh
examples/21-records-sealed-patterns/demo.sh
```

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

The last block quotes one tree to two zones; the guarded `ZoneSurcharge` arm decides which path `quote()` takes. Two experiments, each in a scratch copy:

- **Drop a case.** Delete the `RefrigeratedParcel` arm from `billableWeightKg`. Compilation fails with `the switch expression does not cover all possible input values`.
- **Reorder the arms.** Move `case DeliveryException e -> false;` above the guarded arm in `requiresPriorityHandling`. Compilation fails with `this case label is dominated by a preceding case label`.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480/9780321700698) (Addison-Wesley): the original State, Visitor, and Interpreter entries that sealed hierarchies and exhaustive switches answer without a class hierarchy or an `accept()` method.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 17 on minimizing mutability is the discipline behind every record here, and Item 10 on the `equals()` contract is what a record's generated `equals()` satisfies.
- The official Java SE language documentation on [record classes](https://docs.oracle.com/en/java/javase/25/language/records.html), [sealed classes and interfaces](https://docs.oracle.com/en/java/javase/25/language/sealed-classes-interfaces.html), and [pattern matching](https://docs.oracle.com/en/java/javase/25/language/pattern-matching.html): the specification behind the compact constructor, the `permits` clause, and record deconstruction patterns.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/21-records-sealed-patterns`.
- The full reading list lives on the [bibliography page]({{ '/bibliography/' | relative_url }}).
