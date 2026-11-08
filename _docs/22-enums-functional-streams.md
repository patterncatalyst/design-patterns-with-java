---
title: "Enums, functional interfaces, streams, and Optional as patterns"
order: 22
layout: tutorial
sectionid: docs
part: "idiomatic-java-expressions"
description: "A nightly shipment fulfillment batch, run entirely through everyday Java 25 constructs: an enum carrying its own algorithm, a lambda satisfying the identical contract, a function standing in for a class hierarchy, composed pricing adjustments, a stream pipeline, and Optional as an explicit answer instead of null."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Give an enum constant its own method body and name the pattern it expresses.
- Write the enum Singleton and say what the language guarantees that a private-constructor class doesn't.
- Satisfy one functional interface with a lambda, a method reference and an enum constant.
- Write a fixed sequence of steps as a function that takes its varying steps as parameters.
- Compose two `UnaryOperator` values with `andThen` and `compose`, and predict when the orders differ.
- Replace an accumulator loop with a stream pipeline.
- Return `Optional` from a method that doesn't always have an answer, and keep it off fields and parameters.
- Recognize a stream, an `Optional` or an enum that has outgrown its fit.

{% include excalidraw.html name="22-functional-java-pipeline" caption="Figure 22.1: A nightly fulfillment batch run through four everyday Java 25 constructs, each one doing the work a named pattern usually needs a dedicated class or class hierarchy to do." %}

## Why this matters

The GoF patterns were written down in 1994 for languages with no lambdas, no enum constant bodies and no `Optional`, so each earned a class, an interface or a small hierarchy. Java has everyday constructs that already do that work. A team that writes a new interface and three implementing classes every time an algorithm varies pays for ceremony the language no longer needs.

You've probably written several of these patterns this week without naming them: an enum whose constants behave differently, a lambda passed into a method that calls it later, a `.stream().filter().map().collect()` chain in place of a loop. Naming the force tells you which construct fits.

The running example is a warehouse's nightly fulfillment batch: price a packaging surcharge, validate an order against its sales channel's rule, price the shipment, assign a manifest number, and notify whoever is watching. An enum, some lambdas and method references, a stream pipeline and `Optional` carry the whole batch.

## The domain: a nightly fulfillment batch

The code lives in `examples/22-functional-java`. A `Shipment` record carries a weight, a `DestinationZone`, a `PackagingTier` and a declared value in cents; `Money` holds exact cents, so price comparisons need no epsilon. A `FulfillmentRecord` is the outcome for one shipment: a manifest number and a total when accepted, a reason when rejected.

## Enums carrying behavior: Strategy and Singleton with no new class

`PackagingTier` has three constants, and each implements the one-method `SurchargeRule` interface in its own body:

```java
public enum PackagingTier implements SurchargeRule {

    STANDARD {
        @Override
        public long surchargeCents(Shipment shipment) {
            return 0L;
        }
    },

    FRAGILE {
        @Override
        public long surchargeCents(Shipment shipment) {
            return 350L + Math.round(shipment.weightKg() * 40);
        }
    },

    OVERSIZED {
        @Override
        public long surchargeCents(Shipment shipment) {
            return 900L + Math.round(shipment.weightKg() * 75);
        }
    }
}
```

This is Strategy: the algorithm varies by which constant a shipment carries, over a closed set of three instead of three classes behind an interface. Packaging tiers don't grow the way a carrier catalogue or a promotion calendar does. A fourth constant with no `surchargeCents` body doesn't compile, so no shipment silently prices at zero.

A manifest counter gets the same treatment. The warehouse needs exactly one for the life of the program:

```java
public enum ManifestSequence {

    INSTANCE;

    private final AtomicLong issuedCount = new AtomicLong();

    public String nextManifestId() {
        return "MANIFEST-%06d".formatted(issuedCount.incrementAndGet());
    }
}
```

This is Singleton. A hand-written one needs a private constructor, a static field and a static accessor, and still leaves a reviewer three questions: can reflection call the constructor, can deserialization make a second instance, and is initialization safe across threads? A one-constant enum answers all three by construction, because reflection and serialization both treat `enum` as a language feature. The `AtomicLong` makes the counter itself safe under concurrent calls, which is a separate concern from how many instances exist.

## The same contract, satisfied by a lambda

`SurchargeRule` has one abstract method, so it's a functional interface with or without `@FunctionalInterface`. `PromotionalSurcharges` satisfies it with lambdas, for surcharges tied to a sales calendar:

```java
public static SurchargeRule holidayPeakSurcharge(long centsPerKg) {
    return shipment -> Math.round(shipment.weightKg() * centsPerKg);
}
```

A `SurchargeRule` variable can't tell whether it holds `PackagingTier.FRAGILE` or `holidayPeakSurcharge(25)`. The fixed physical tiers earn the enum; a holiday surcharge built once for a campaign earns a lambda and no file. `combined()` takes two rules from either source and returns one that adds both, with no decorator class.

## Command as a `Runnable` and a custom functional interface

Command wraps a request as an object so it can be queued, logged or run later. `NotificationCommand` is a one-method interface for that, and two `NotificationLog` methods satisfy it as method references:

```java
NotificationLog log = new NotificationLog();
FulfillmentNotifier notifier = new FulfillmentNotifier(List.of(log::recordEmail, log::recordSms));
notifier.dispatchAll(shipment);
```

`FulfillmentNotifier` runs every queued command against a shipment and only knows the contract, never what satisfies it.

The batch's closeout step takes nothing and returns nothing, which the platform already names `Runnable`:

```java
public static void run(Runnable onComplete) {
    Objects.requireNonNull(onComplete, "onComplete");
    onComplete.run();
}
```

A `BatchCloseoutCommand` interface here would reinvent `java.lang.Runnable`. A Command with no argument and no result is a `Runnable`.

## A fixed sequence with two supplied steps

Template Method fixes a sequence and lets some steps vary. Here the sequence is validate, price, assign a manifest number, always in that order, and `FulfillmentPipeline` states it as one static method instead of an abstract base class:

```java
public static FulfillmentRecord process(
        Shipment shipment,
        Function<Shipment, Optional<String>> validate,
        Function<Shipment, Money> price) {
    Optional<String> rejection = validate.apply(shipment);
    if (rejection.isPresent()) {
        return FulfillmentRecord.rejected(shipment, rejection.get());
    }
    Money total = price.apply(shipment);
    String manifestId = ManifestSequence.INSTANCE.nextManifestId();
    return FulfillmentRecord.accepted(manifestId, shipment, total);
}
```

Retail and wholesale orders each pass their own pair of steps:

```java
FulfillmentPipeline.process(shipment, FulfillmentRules::retailValidate,
        s -> FulfillmentRules.retailPrice(rates, s));
FulfillmentPipeline.process(shipment, FulfillmentRules::wholesaleValidate,
        s -> FulfillmentRules.wholesalePrice(rates, s));
```

The manifest step never varies and isn't a parameter, so no caller can skip it the way a forgotten `super` call skips an invariant in a class hierarchy. The split is the same as a `final` skeleton with `protected abstract` hooks, drawn with two parameters. Validation returns `Optional<String>`: empty means nothing is wrong, and a present value says why, which a `boolean isValid` can't.

> **🤖 Explore with your agent**
>
> "In `examples/22-functional-java`, add a marketplace channel as two static methods on `FulfillmentRules`: `marketplaceValidate(Shipment)` returning `Optional<String>` (empty when the order is acceptable, the reason otherwise) and `marketplacePrice(ZoneRateLookup, Shipment)` returning `Money`. Pass them to the existing `FulfillmentPipeline.process` as a method reference and a lambda, the way the retail pair is passed. Don't add an interface, an abstract class or a new pipeline, and don't throw an exception for a rejected order. Leave `ManifestSequence` as the pipeline's own step."
>
> Check: `mvn -q -f examples/22-functional-java/pom.xml test -Dtest=FulfillmentRulesTest,FulfillmentPipelineTest`, and reject it if it declares a new type.

## Composing adjustments: `andThen` and `compose`

`PricingAdjustments` builds a discount and a flat handling fee as `UnaryOperator<Money>` values, which inherit `andThen` and `compose` from `Function`:

```java
UnaryOperator<Money> discount = PricingAdjustments.percentDiscount(0.10);
UnaryOperator<Money> fee = PricingAdjustments.flatHandlingFee(500);

discount.andThen(fee).apply(startingPrice);   // discount first, then the fee
discount.compose(fee).apply(startingPrice);   // the fee first, then discount
```

The two don't commute. On $100.00, a 10% discount followed by the $5.00 fee gives $95.00; the fee followed by the discount gives $94.50, because the discount now scales the fee too. The order inside a composition is a business decision, and a quote composed in the wrong order is off by the discount's share of the fee.

## Streams: pipelines that replace a loop

A hand-written total declares an accumulator and mutates it per element, and the reader reconstructs "sum of surcharges" from the loop body. A stream states the result:

```java
public static long totalSurchargeCents(List<Shipment> shipments) {
    return shipments.stream()
            .mapToLong(shipment -> shipment.tier().surchargeCents(shipment))
            .sum();
}
```

Grouping by tier with a loop needs a `Map` up front, a find-or-create branch and an add. `Collectors.groupingBy` with a summing downstream collector states the finished shape:

```java
public static Map<PackagingTier, Long> surchargeByTier(List<Shipment> shipments) {
    return shipments.stream()
            .collect(Collectors.groupingBy(
                    Shipment::tier,
                    () -> new EnumMap<>(PackagingTier.class),
                    Collectors.summingLong(shipment -> shipment.tier().surchargeCents(shipment))));
}
```

The `EnumMap` factory keeps entries in constant-declaration order instead of `HashMap` order, so the same batch iterates the same way on every run.

## Optional: an explicit answer instead of null

The rate table doesn't cover every zone. `ZoneRateLookup.baseRateFor` returns `Optional<Money>` rather than `null` or `Money.ZERO`, because a missing rate and a zero price are different facts:

```java
public Optional<Money> baseRateFor(DestinationZone zone) {
    return Optional.ofNullable(baseRates.get(zone));
}
```

A caller handles both outcomes with no null check:

```java
String description = lookup.baseRateFor(DestinationZone.INTERNATIONAL)
        .map(Money::toString)
        .orElse("not priced");
```

`Optional` sits on the return type and nowhere else. `ZoneRateLookup` stores a plain `Map<DestinationZone, Money>`. `FulfillmentRecord` stores a plain `String manifestId` and `String rejectionReason`, empty where they don't apply. A method's answer that's sometimes absent is what `Optional` is for; a field absent for an object's whole life is a permanent fact, better said by the object's shape.

## When not to use any of these

| Construct | Stops fitting when | Write instead |
| --- | --- | --- |
| Stream | More than one piece of state crosses elements, or a step needs the previous element's result; or the pipeline needs a comment to explain it | An explicit loop with named locals |
| `Optional` | It's a field, a parameter, or wraps a collection (`Optional<List<T>>`) | Two types for present and absent, an overload or a documented nullable parameter, an empty `List<T>` |
| Enum with behavior | Constants need different data, not just different bodies over one shape (a tracking number on one status, a cancellation reason on another) | A sealed interface of per-variant records |

`PackagingTier` fits because every constant computes a `long` from a `Shipment`. An `Optional` parameter makes every caller wrap a value it already has; the type's value is a return type telling the caller to check.

## Build, run, observe

From the repository root:

```sh
mvn -f examples/pom.xml test
```

Or standalone:

```sh
cd examples/22-functional-java
mvn test
```

All 47 tests pass. The demo runs the full batch of five shipments through every construct:

```sh
examples/22-functional-java/demo.sh
```

```
== Enum carrying behavior: PackagingTier surcharges ==
RETAIL-1001 (STANDARD): $0.00 surcharge
RETAIL-1002 (FRAGILE): $4.10 surcharge
WHOLESALE-2001 (OVERSIZED): $35.25 surcharge
WHOLESALE-2002 (STANDARD): $0.00 surcharge
RETAIL-1003 (STANDARD): $0.00 surcharge

== Lambda satisfying the same SurchargeRule contract ==
Peak-season surcharge on RETAIL-1002 alone: $0.38
Peak-season surcharge combined with FRAGILE handling: $4.48

== Enum Singleton: one manifest counter for the whole program ==
Manifests issued so far: 0

== Template Method as a function: validate, price, assign a manifest ==
RETAIL-1001 -> MANIFEST-000001, total $6.50
RETAIL-1002 -> rejected: retail orders cap declared value at $500.00
WHOLESALE-2001 -> MANIFEST-000002, total $45.45
WHOLESALE-2002 -> rejected: wholesale orders require at least 20kg
RETAIL-1003 -> MANIFEST-000003, total $3.00
Manifests issued after the batch: 3

== Command: method references and a Runnable ==
Notification log after dispatch: [email:RETAIL-1001, sms:RETAIL-1001]
Batch closeout ran as a plain Runnable.
Manifest closed flag: true

== Function composition: andThen vs. compose do not commute ==
Discount then fee (andThen):  $95.00
Fee then discount (compose):  $94.50

== Streams: declarative pipelines over the batch ==
Total surcharge cents for the batch: 3935
Shipments needing extra handling: [RETAIL-1002, WHOLESALE-2001]
Surcharge cents by tier: {STANDARD=0, FRAGILE=410, OVERSIZED=3525}
Shipment count by tier: {STANDARD=3, FRAGILE=1, OVERSIZED=1}

== Optional: an honest answer for a zone with no base rate ==
LOCAL base rate: $3.00
INTERNATIONAL base rate: not priced
```

Two of the five shipments are rejected before a manifest number is assigned, so the counter reaches 3. One experiment, in a scratch copy:

- **Add a tier with no body.** Add a `HAZMAT,` constant to `PackagingTier`. Compilation fails with `PackagingTier is abstract; cannot be instantiated`.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original Strategy, Singleton, Command, and Template Method entries, each satisfied here by an everyday construct instead of a dedicated class hierarchy.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 3 on the single-element enum as the best way to implement a Singleton, Item 34 on enum constants with constant-specific method bodies, Items 42 through 44 on lambdas and method references, and Item 55 on returning `Optional` judiciously.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/22-functional-java`.
- The full reading list lives on the [bibliography page]({{ '/bibliography/' | relative_url }}).
