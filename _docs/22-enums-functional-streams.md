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

- Give an enum constant its own method body and say which classic pattern
  that constant-specific body is really expressing.
- Build the enum Singleton idiom and explain what the language itself
  guarantees about it that a hand-written private-constructor class does
  not.
- Satisfy a functional interface with a lambda, a method reference, and an
  enum constant in the same program, and say why a variable typed to that
  interface cannot tell which one produced the value it holds.
- Write a fixed sequence of steps as a plain function that takes two of its
  steps as parameters, and compare what that buys you against a class
  hierarchy with `protected abstract` hooks.
- Compose two `UnaryOperator` values with `andThen` and `compose`, and
  predict when the two orders produce different results.
- Replace a hand-written accumulator loop with a stream pipeline that reads
  as what it computes rather than how it computes it.
- Return `Optional` from a method that does not always have an answer, and
  say why it belongs on a return type and nowhere else.
- Recognize a stream that is harder to read than the loop it replaced, an
  `Optional` reached for out of habit, and an enum that is quietly outgrowing
  itself, and say what to write instead in each case.

{% include excalidraw.html name="22-functional-java-pipeline" caption="Figure 22.1: A nightly fulfillment batch run through four everyday Java 25 constructs, each one doing the work a named pattern usually needs a dedicated class or class hierarchy to do." %}

## Why this matters

Every pattern earlier in this book earned a class, an interface, or a small
hierarchy of both. That is not an accident of how Gang of Four patterns were
first written down in 1994, against a language with no lambdas, no enum
constant bodies, and no `Optional`. Java 25 ships several everyday
constructs that already do what those patterns exist to do, and a team that
reaches for a new interface and three implementing classes every time it
needs to vary an algorithm is paying a tax the language stopped charging
years ago.

This chapter is not about a fourth, fifth, and sixth way to write Strategy,
State, or Command. It is about recognizing that you have very likely
already written one of them this week without naming it: an enum whose
constants behave differently, a lambda passed into a method that calls it
later, a `switch` expression that replaced a chain of `if` statements, a
`.stream().filter().map().collect()` chain that replaced a `for` loop with
an accumulator variable. The skill this book trains is naming the force
correctly and picking the idiom whose shape matches it. Once a construct is
named, the discipline that governs every other pattern in this book,
reading the forces before reaching for the familiar shape, applies here
exactly as it does everywhere else.

The running example is a warehouse's nightly fulfillment batch: price a
packaging surcharge, validate an order against its sales channel's rule,
price the shipment, assign it a manifest number, and notify whoever is
watching. Four everyday constructs carry that whole batch: an enum, a
handful of lambdas and method references, a stream pipeline, and
`Optional`. None of them needed a new interface hierarchy to be written.

## The domain: a nightly fulfillment batch

The example behind this chapter lives in `examples/22-functional-java`. A
`Shipment` is a record carrying a weight, a `DestinationZone`, a
`PackagingTier`, and a declared value in cents, the same `Money`-as-exact-
cents discipline earlier shipping examples in this book use, so a price
comparison in a test never needs an epsilon. A `FulfillmentRecord` is the
outcome of running one shipment through the pipeline: a manifest number and
a total when it is accepted, a reason when it is not.

## Enums carrying behavior: Strategy and Singleton with no new class

`PackagingTier` is a plain three-constant enum, `STANDARD`, `FRAGILE`, and
`OVERSIZED`, and each constant implements `SurchargeRule`, a one-method
functional interface, through its own body:

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

Nothing here is a new idiom this book has not already named. This is
Strategy, a varying algorithm selected by which constant a shipment
carries, expressed as a closed set of three enum constants rather than
three named classes behind an interface. The warehouse's three packaging
tiers are not going to grow an open-ended list the way a carrier catalogue
or a promotion calendar might; they are the entire, fixed set of ways this
warehouse packs a box, and a constant-specific method body gives the
compiler the same completeness guarantee this book's earlier enum-as-
Strategy chapter trained: a fourth tier with no `surchargeCents` body is a
compile error, not a shipment that silently prices itself at zero.

A second, unrelated force in the same domain gets the same free ride from
`enum`. `ManifestSequence` issues a unique manifest number to every shipment
accepted into the batch, and the warehouse needs exactly one of these
counters for the life of the program:

```java
public enum ManifestSequence {

    INSTANCE;

    private final AtomicLong issuedCount = new AtomicLong();

    public String nextManifestId() {
        return "MANIFEST-%06d".formatted(issuedCount.incrementAndGet());
    }
}
```

This is the Singleton pattern, and the ceremony it skips says a lot about
what each approach guarantees. A hand-written Singleton needs a private constructor, a static
field, and a static accessor method, and still leaves open questions a
reviewer has to chase down: can reflection call the private constructor
anyway, can deserialization produce a second instance, is the static field
safe to read from two threads without a synchronized block around its
initialization. An enum with one constant answers every one of those
questions by construction. The language guarantees exactly one `INSTANCE`
exists, a guarantee reflection and serialization both respect specifically
because `enum` is a language feature, not library code imitating one.
`AtomicLong` inside that single instance is what makes concurrent access to
the counter itself safe, a separate concern from how many instances of
`ManifestSequence` can ever exist.

## The same contract, satisfied by a lambda

`SurchargeRule` has exactly one abstract method, which makes it a
functional interface whether or not anyone marks it `@FunctionalInterface`.
`PromotionalSurcharges` satisfies that identical contract with
one-expression lambdas instead of enum constants, for surcharges tied to a
sales calendar rather than to a package's physical tier:

```java
public static SurchargeRule holidayPeakSurcharge(long centsPerKg) {
    return shipment -> Math.round(shipment.weightKg() * centsPerKg);
}
```

A variable typed `SurchargeRule` cannot tell whether the value it holds
came from `PackagingTier.FRAGILE` or from a call to
`holidayPeakSurcharge(25)`. That is exactly the property this chapter
trains you to notice: the closed, physical-tier set earns the enum form
because tiers do not come and go with a sales calendar, while a holiday
surcharge earns the lambda form because it is built once for a single
campaign and discarded when the campaign ends, with no file anywhere named
`HolidayPeakSurchargeRule`. `combined()` goes one step further and shows a
capability neither a class hierarchy nor an enum offers without extra
ceremony: taking two `SurchargeRule` values, from either source, and
returning a third one that adds both, with no decorator class declared to
do it.

## Command as a `Runnable` and a custom functional interface

Command wraps a request as an object so it can be queued, logged, or run
later instead of invoked directly. `NotificationCommand` is a one-method
functional interface declaring exactly that shape, and `NotificationLog`'s
two instance methods satisfy it as method references with no class written
to adapt them:

```java
NotificationLog log = new NotificationLog();
FulfillmentNotifier notifier = new FulfillmentNotifier(List.of(log::recordEmail, log::recordSms));
notifier.dispatchAll(shipment);
```

`FulfillmentNotifier` holds a queue of `NotificationCommand` values and
runs every one of them against a shipment, with no idea whether a given
entry in that queue is a method reference, a lambda built at a call site,
or something else matching the same shape. That indifference is the whole
value Command buys: the queue only needs to know the contract, never the
concrete thing satisfying it.

Not every Command needs even a one-method interface of its own. The
nightly batch's closeout step takes no argument and returns nothing, a
shape the platform already declares as `Runnable`:

```java
public static void run(Runnable onComplete) {
    Objects.requireNonNull(onComplete, "onComplete");
    onComplete.run();
}
```

Writing a `BatchCloseoutCommand` interface here to declare a shape
`java.lang.Runnable` already describes exactly is the reinvented-wheel
mistake this book's idiomatic-review discipline flags everywhere else in
the codebase; a Command with no argument and no return value is a
`Runnable`, not a new interface waiting to be written.

## A fixed sequence with two supplied steps

Template Method keeps a sequence of steps fixed while a few of those steps
vary by context. The sequence here is short and absolute: validate, price,
assign a manifest number, in that order, every time. `FulfillmentPipeline`
states it as a single static method instead of an abstract base class:

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

Two of the three steps, `validate` and `price`, arrive as `Function`
parameters, one pair for retail orders and a different pair for wholesale
orders, each supplied at its own call site as a method reference:

```java
FulfillmentPipeline.process(shipment, FulfillmentRules::retailValidate,
        s -> FulfillmentRules.retailPrice(rates, s));
FulfillmentPipeline.process(shipment, FulfillmentRules::wholesaleValidate,
        s -> FulfillmentRules.wholesalePrice(rates, s));
```

The third step, assigning a manifest number through `ManifestSequence`,
never varies and is never exposed as a parameter at all; it is not
something a caller can override by accident the way a forgotten `super`
call can quietly skip an invariant in a class-hierarchy Template Method. A
reviewer confirms the sequence itself never changes by reading this one
method top to bottom. Confirming what varies between retail and wholesale
orders means reading whichever two functions a caller passed in, the same
split a `final` skeleton method and its `protected abstract` hooks draw
with inheritance, drawn here with two parameters instead. Each step
returning `Optional<String>` for validation is not incidental either: an
empty `Optional` reads as "nothing wrong," which says more in the method's
own signature than a `boolean isValid` that tells a caller nothing about
why a shipment failed.

> **AI sidebar.** Ask an LLM pairing partner for "a Strategy interface for
> pricing shipments" and the far more common answer is a new interface plus
> two or three named implementing classes, the shape that appears in the
> most textbooks and therefore the most training data. That answer is not
> wrong, but it is frequently not the smallest correct one. Before accepting
> it, ask the same two questions this chapter trains: is the set of
> algorithms closed and known today, in which case an enum with
> constant-specific bodies is smaller and gets a compile-time completeness
> check with no extra code; and is a given strategy built once at a call site and
> never reused, in which case a lambda assigned to the interface needs no
> file at all. The same caution applies in the other direction. A model
> asked to "validate this order" sometimes reaches for a thrown exception on
> the happy path's unhappy branch, which works but forces every caller into
> a `try`/`catch` for a condition that is not exceptional, a rejected order
> during nightly processing. A validation step that returns `Optional<String>`
> or a small result type states the two outcomes in the method's own
> signature, which is exactly the Template Method shape this chapter builds
> and a stronger default than an exception for an expected, frequent
> outcome.

## Composing adjustments: `andThen` and `compose`

`PricingAdjustments` builds a discount and a flat handling fee as
`UnaryOperator<Money>` values, and `UnaryOperator` inherits `andThen` and
`compose` from `Function` with no code of this example's own:

```java
UnaryOperator<Money> discount = PricingAdjustments.percentDiscount(0.10);
UnaryOperator<Money> fee = PricingAdjustments.flatHandlingFee(500);

discount.andThen(fee).apply(startingPrice);   // discount first, then the fee
discount.compose(fee).apply(startingPrice);   // the fee first, then discount
```

These two do not commute, and the difference is the entire reason this
section exists rather than being folded into the Strategy discussion above.
A ten percent discount applied before a flat five-dollar fee never
discounts that fee; applied after, it does, because the fee is already
baked into the amount the discount scales. Composing functions reads as
declarative math, but the order inside that composition still encodes a
real business decision, and a codebase that reaches for `andThen` without
checking which order the business rule actually requires can produce a
quote that is off by exactly the fee amount's discount share, a bug that
passes every test built against only one of the two possible orders.

## Streams: pipelines that replace a loop

A hand-written total over a shipment batch declares an accumulator, walks
the list, and mutates the accumulator once per element, with the reader
reconstructing "sum of surcharges" from watching the loop body run.
`FulfillmentBatch.totalSurchargeCents` states the same computation as what
it produces:

```java
public static long totalSurchargeCents(List<Shipment> shipments) {
    return shipments.stream()
            .mapToLong(shipment -> shipment.tier().surchargeCents(shipment))
            .sum();
}
```

Grouping follows the same shape. A loop that buckets shipments by tier and
sums each bucket needs a `Map` declared up front, a branch to find or
create each bucket's entry, and a line that adds into whatever that branch
found. `Collectors.groupingBy` with a summing downstream collector states
the finished shape directly:

```java
public static Map<PackagingTier, Long> surchargeByTier(List<Shipment> shipments) {
    return shipments.stream()
            .collect(Collectors.groupingBy(
                    Shipment::tier,
                    () -> new EnumMap<>(PackagingTier.class),
                    Collectors.summingLong(shipment -> shipment.tier().surchargeCents(shipment))));
}
```

Supplying an `EnumMap` as the grouping collector's map factory is itself a
small idiom: because `PackagingTier` is an enum, an `EnumMap`
stores its entries in constant-declaration order rather than in whatever
order a general-purpose `HashMap`'s hash codes happen to produce, so the
same batch produces the same map iteration order on every run.

## Optional: an explicit answer instead of null

A warehouse's rate table does not necessarily cover every zone it might one
day ship to. `ZoneRateLookup.baseRateFor` returns `Optional<Money>` rather
than `null` or a sentinel amount like `Money.ZERO`, because a missing rate
and a shipment priced at zero are not the same fact and should never share
a representation:

```java
public Optional<Money> baseRateFor(DestinationZone zone) {
    return Optional.ofNullable(baseRates.get(zone));
}
```

A caller chains through the two outcomes without a null check anywhere:

```java
String description = lookup.baseRateFor(DestinationZone.INTERNATIONAL)
        .map(Money::toString)
        .orElse("not priced");
```

The detail that matters is where `Optional` appears and where it does
not. `ZoneRateLookup` itself stores a plain `Map<DestinationZone, Money>`
field, never an `Optional` field; `FulfillmentRecord` stores a plain
`String manifestId` and a plain `String rejectionReason`, empty in the case
that does not apply, never `Optional<String>` fields. `Optional` fits on
`baseRateFor`'s return type precisely because a method's answer is
exactly the "present most of the time, absent some of the time" case the
type was designed for. A field that is absent for the entire life of an
object, a rejected shipment's manifest number, is a permanent fact about
that object, not a transient maybe a caller checks once and moves past; a
plain empty `String` or a dedicated accepted/rejected shape says so more
clearly than wrapping the field in a type built for a different problem.

## When not to use any of these

Every construct in this chapter pays for itself when the force behind it
is real. Reaching for one out of reflex rather than need produces code that
runs but reads worse than the loop, field, or class it replaced.

A stream pipeline is not automatically clearer than a loop. A computation
with more than one piece of state to track across elements, or a step that
needs to see the previous element's result to decide what to do with the
current one, usually reads better as an explicit loop with named local
variables than as a stream chased through several intermediate operations
just to avoid writing `for`. If a stream pipeline needs a comment
explaining what it does, the loop it replaced probably did not need one,
and that comment is a sign to write the loop back.

`Optional` overuse has a specific, recognizable shape: an `Optional` field
on a class, an `Optional` method parameter, or an `Optional<List<T>>`
return type where an empty `List<T>` already represents "nothing found"
without a wrapper around it. A field that is sometimes absent for the life
of an object is better modeled as two types, one for the present case and
one for the absent case, the way `FulfillmentRecord`'s accepted and
rejected shapes differ in this chapter's own example, rather than as an
`Optional` field a reader has to unwrap every time they touch the object. A
parameter typed `Optional<String>` forces every caller to wrap a plain
value just to call the method, when an overload or a `null`-accepting
parameter documented as such would serve callers better; `Optional`'s
entire value proposition is a method's return type telling its caller
"check before you use this," and that value does not transfer to a
parameter a caller already had to produce.

An enum carrying behavior stops fitting the moment its constants need to
carry meaningfully different shapes of data rather than just different
method bodies over the same shape. `PackagingTier`'s three constants all
compute the same thing, a `long` surcharge, from the same input, a
`Shipment`; that uniformity is what makes constant-specific method bodies
the right fit. A status that needs one piece of data on one branch and a
different piece of data on another, a tracking number on one status, a
cancellation reason on another, is the point where a sealed interface of
per-status records fits better than an enum, because only a sealed
hierarchy lets each variant declare its own fields rather than forcing
every constant to carry the same bare shape or none at all.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this
loop. From the repository root, the aggregator picks up
`examples/22-functional-java` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/22-functional-java
mvn test
```

Either path runs the same 47 JUnit 5 cases, asserted with AssertJ, covering
every construct in this chapter: `PackagingTier`'s per-constant surcharges,
`ManifestSequence`'s single-instance and monotonically increasing counter,
method references and lambdas satisfying `NotificationCommand`, the
`andThen`/`compose` order difference, both accepted and rejected paths
through `FulfillmentPipeline` for two different channels, the stream
pipelines in `FulfillmentBatch`, and both the present and absent cases of
`ZoneRateLookup`. Observed locally on Fedora with Temurin 25.0.2:

```
Tests run: 47, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see every expression run against the same nightly batch rather than
just reading an assertion that says it behaves correctly, run the
example's own demo script:

```sh
examples/22-functional-java/demo.sh
```

It prints the batch moving through every section of this chapter in order:

```
== Enum carrying behavior: PackagingTier surcharges ==
RETAIL-1001 (STANDARD): $0.00 surcharge
RETAIL-1002 (FRAGILE): $4.10 surcharge
WHOLESALE-2001 (OVERSIZED): $35.25 surcharge

== Template Method as a function: validate, price, assign a manifest ==
RETAIL-1001 -> MANIFEST-000001, total $6.50
RETAIL-1002 -> rejected: retail orders cap declared value at $500.00
WHOLESALE-2001 -> MANIFEST-000002, total $45.45

== Function composition: andThen vs. compose do not commute ==
Discount then fee (andThen):  $95.00
Fee then discount (compose):  $94.50

== Optional: an honest answer for a zone with no base rate ==
LOCAL base rate: $3.00
INTERNATIONAL base rate: not priced
```

Two of the five shipments in the full batch are rejected before a manifest
number is ever assigned, which is why the manifest counter only advances
three times. The `andThen`/`compose` block prices the identical starting
amount two different ways specifically to make the non-commuting point
visible at the terminal, not just in a test assertion.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original Strategy, Singleton, Command, and Template Method entries, each satisfied here by an everyday Java 25 construct instead of a dedicated class hierarchy.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 3 on the single-element enum as the best way to implement a Singleton, Item 34 on enum constants with constant-specific method bodies, Items 42 through 44 on lambdas and method references, and Item 55 on returning `Optional` judiciously, all of which this chapter's example applies directly.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/22-functional-java`'s suite, which asserts the behavior of every construct in this chapter rather than just that each one runs without throwing.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
