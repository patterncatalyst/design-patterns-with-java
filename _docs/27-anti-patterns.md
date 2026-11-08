---
title: "Anti-patterns and pattern misuse"
order: 27
layout: tutorial
sectionid: docs
part: "concurrency-patterns-in-the-wild"
description: "Recognizing patternitis in a shipping-rate pricing codebase: a singleton surcharge registry holding global mutable state, a factory-of-factories wrapped around an interface with one implementation, a god object that validates, logs, and formats, and a leaky abstraction that exposes its own call order. The simplified rewrite collapses six types into two records and one class that compute the same answer and are testable where the original was not."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Recognize a singleton used as global mutable state and name the cost it adds beyond Singleton's usual trade-offs.
- Recognize a god object by what it knows, not by its line count.
- Recognize speculative generality: a Strategy, a Factory or any interface with one implementation and no second one planned.
- Recognize a leaky abstraction whose public surface makes callers track internal, call-order-dependent state.
- Use the forces as a diagnostic: a pattern applied where no force calls for it is the anti-pattern, whether or not the code works.
- Rewrite an over-engineered design to the smallest shape its requirements justify, with the same results and better testability.

## Why this matters

Each pattern is justified by a force: Strategy when behavior varies by a condition the caller controls, Factory when which concrete type to build is itself a decision, Singleton when exactly one instance must exist and nothing substitutes another. A pattern also has a shape, and an engineer or a code generator can reproduce the shape long after the force has gone, or without checking that it was ever there.

That reflex is patternitis: reaching for a named pattern because its shape is familiar, not because the code calls for it. It rarely shows as one wrong line. It shows as a design that compiles, passes review because every piece looks textbook, and costs far more to read, test and change than the problem underneath.

The domain is a shipment price: multiply a base cost by a fuel surcharge and add a handling fee. The over-engineered version grows one defensible step at a time. The surcharge becomes a singleton because it's convenient to reach. The calculator that reads it comes from a factory because factories decouple construction. The factory comes from a second singleton because a global accessor is right there. The class that ties it together picks up logging and formatting because nothing else owned them. The result needs five types and two singletons to multiply one number by another.

{% include excalidraw.html name="27-anti-patterns" caption="Figure 27.1: the over-engineered design chains a god object through a factory-of-factories and a singleton holding global mutable state; the appropriate design injects one plain record into one class and gets one value back." %}

## Singleton as global mutable state

`ShippingSurchargeRegistry` (`examples/27-anti-patterns/src/main/java/.../overengineered/ShippingSurchargeRegistry.java`) is a correct modern singleton: an enum, so the language guarantees one `INSTANCE`, with its mutable field behind a `volatile` read and a `synchronized` write. The mechanism isn't the problem.

```java
public enum ShippingSurchargeRegistry {
    INSTANCE;

    private volatile double surchargeMultiplier = 1.0;

    public synchronized void setSurchargeMultiplier(double surchargeMultiplier) {
        this.surchargeMultiplier = surchargeMultiplier;
    }

    public double surchargeMultiplier() {
        return surchargeMultiplier;
    }
}
```

The use is. One mutable field, the fuel surcharge multiplier, is something several parts of the pricing pipeline believe they can configure independently. `StandardRateCalculator` reads `ShippingSurchargeRegistry.INSTANCE.surchargeMultiplier()` on every call instead of once at construction. Set the registry to 1.10 and obtain a region A calculator: it quotes $110.00 on $100.00. Set the registry to 1.30 for region B, and region A, untouched, now quotes $130.00.

That's spooky action at a distance: two objects that look independent share one mutable slot and change each other's behavior through no code path visible in either. The structural cost is that one slot can't hold two valid configurations. A program that prices two regions at different rates at the same time can't express that through `ShippingSurchargeRegistry`, and a second singleton only moves the problem to another name.

## Speculative generality: an interface with no second implementation

`RateCalculator` and `RateCalculatorFactory` each have one implementation, `StandardRateCalculator` and `StandardRateCalculatorFactory`, and no second one is planned. That's speculative generality: an abstraction built for a variation that hasn't arrived. Factory Method pays off when choosing the type is a real decision, resolved by a config value, a runtime flag or a registered family of types. `StandardRateCalculatorFactory.createCalculator()` makes no decision:

```java
@Override
public RateCalculator createCalculator() {
    return new StandardRateCalculator();
}
```

A reader opens `RateCalculatorFactory`, confirms it has one implementation, then opens that to find the logic: three hops to code that `new StandardRateCalculator()` would have put at the call site.

`RateCalculatorFactoryProvider` makes it worse. It's a second singleton that exists to hand out the one `RateCalculatorFactory` the program will ever build. A caller writes `RateCalculatorFactoryProvider.INSTANCE.factory().createCalculator()`, four symbols and two singletons deep, to get what `new StandardRateCalculator()` returns in one line. That's a factory of factories: speculative generality compounded with Singleton's hidden dependency, each hiding the other well enough that a reviewer reading one class at a time can miss both.

## The god object: a class that knows everything

`ShipmentOperationsManager` resolves a calculator through both singletons, checks that the base cost is positive, writes an audit log entry per shipment, and formats a receipt. It's about thirty lines of code, so line count isn't the measure. The measure is the one-sentence answer to "what does it do?", and here it needs four clauses: pricing orchestration, input validation, audit logging and presentation formatting. None relates to the others. A bug in the receipt string and a bug in the surcharge math are unrelated failures in one class, one diff and one set of tests.

Its public surface shows it: `operationLog()` sits next to `priceShipment()`, so pricing a shipment has a logging side effect anyone working on pricing has to account for. The Single Responsibility Principle, one reason to change per class, says the same: the manager changes for a pricing rule, a validation rule, a log format or a receipt layout, and any one forces a re-review of all four.

## Leaky abstraction: exposing what should stay hidden

The sharpest bug is `ShipmentOperationsManager.wasLastCalculationSurchargeApplied()`. It answers a real question, whether the latest calculation applied a surcharge, by keeping mutable state on the manager instead of attaching the fact to the `PricedShipmentReceipt` it returned:

```java
lastCalculationSurchargeApplied = ShippingSurchargeRegistry.INSTANCE.surchargeMultiplier() != 1.0;
// ...
public boolean wasLastCalculationSurchargeApplied() {
    return lastCalculationSurchargeApplied;
}
```

Price `ORD-3001` at the default rate, then `ORD-3002` with a surcharge, and the method reports `true`: right for `ORD-3002`, wrong for anyone holding the `ORD-3001` receipt and asking about it. The method answers for whichever shipment was priced last, which its name doesn't disclose and its signature can't express. A leaky abstraction is a type whose public surface fails to hide an internal detail, here that the flag is ordered state tied to the last call, so every caller has to track what the type should have encapsulated.

## The appropriate rewrite: one value, one class

`ShipmentPricingService` replaces `ShippingSurchargeRegistry`, `RateCalculator`, `StandardRateCalculator`, `RateCalculatorFactory`, `StandardRateCalculatorFactory` and `RateCalculatorFactoryProvider`, six types, with two small records and one small class:

```java
public record RateSettings(double surchargeMultiplier, long handlingFeeCents) {
}

public record PricingResult(Money totalCost, boolean surchargeApplied) {
}

public final class ShipmentPricingService {
    private final RateSettings settings;

    public ShipmentPricingService(RateSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public PricingResult price(Money baseCost) {
        long surchargedCents = Math.round(baseCost.cents() * settings.surchargeMultiplier());
        long totalCents = surchargedCents + settings.handlingFeeCents();
        boolean surchargeApplied = settings.surchargeMultiplier() != 1.0;
        return new PricingResult(Money.ofCents(totalCents), surchargeApplied);
    }
}
```

| Problem in the original | Fix in the rewrite |
| --- | --- |
| One global, mutable surcharge slot | `RateSettings`, a value fixed at construction and passed in |
| An interface and a factory with one implementation each | No interface: nothing varies polymorphically |
| A factory reached through a second singleton | `new ShipmentPricingService(settings)` |
| A last-call flag on the manager | `PricingResult.surchargeApplied`, carried on the value it describes |

`RateSettings` is the whole configuration surface the six-type design existed to express: a multiplier and a handling fee, known at construction and never changed under a caller. Validation, logging and formatting would each get their own small type when something gives them a reason to vary on their own: a second log format, a second validation rule, a second receipt layout.

## Proving equivalence, and the testability gap

The rewrite computes the same totals as the original for every base cost and multiplier tried, with the handling fee at zero since only the rewrite has one, and both reject a non-positive base cost with `IllegalArgumentException`.

It also does what the singleton can't: two instances with different settings, alive at once, neither moving the other. A domestic service at `new RateSettings(1.0, 250)` prices $100.00 at $102.50 before and after an international service at `new RateSettings(1.35, 900)` prices the same cost at $144.00. Substituting a fake rate is one value, `new RateSettings(0.80, 0)`: no interface to mock, no test double to register, and nothing shared to reset afterward.

> **🤖 Explore with your agent**
>
> "In `examples/27-anti-patterns`, add a minimum charge to the simplified design: a `minimumChargeCents` component on `RateSettings`, validated as non-negative in its compact constructor, and applied in `ShipmentPricingService.price` after the surcharge and handling fee. Keep `ShipmentPricingService` a single final class that takes `RateSettings` in its constructor. Don't add an interface, a factory, a provider, a singleton or any static mutable state, and don't touch the `overengineered` package."
>
> Check: `mvn -q -f examples/27-anti-patterns/pom.xml test -Dtest=ShipmentPricingServiceTest,RateSettingsTest`, then confirm the diff adds no new type.

## When the pattern is still justified

The forces test cuts both ways:

| Pattern | Justified when | `examples/27-anti-patterns` fails it because |
| --- | --- | --- |
| Singleton | Exactly one instance, effectively immutable after construction, never substituted or run in two configurations, such as a read-only table of published carrier rates loaded at startup | The registry is mutated, read by several callers, and two configurations had to live at once |
| Factory | Choosing the type is a real decision from data the caller doesn't control at compile time: a config value naming the carrier integration, a self-registering family of handlers by message type | `RateCalculatorFactory` has one implementation and none scheduled |
| A class with several responsibilities | They're small, stable and have never needed to change apart | Pricing rules, log formats and receipt layouts change on different schedules |

The red flag is independent variability, not size. Once a log format and a pricing rule change on different schedules, owned by different people and reviewed in different pull requests, the bundling class pays the god-object cost and splitting it is worth the file. Before that, splitting four cooperating lines into four classes is the same mistake in reverse: abstraction for a force that hasn't arrived.

Name the forces present in the code, not the ones a pattern's name implies, and let them decide.

## Build, run, observe

From the repository root:

```sh
mvn -f examples/pom.xml test
```

Or standalone:

```sh
cd examples/27-anti-patterns
mvn test
```

All 18 tests pass, including five base-cost and multiplier combinations where both designs agree. The demo prices two regions through each design:

```sh
examples/27-anti-patterns/demo.sh
```

```
== Over-engineered: singleton + factory-of-factories ==
Region A quote (surcharge 1.10): $110.00
ORD-3001 priced via the manager: $110.00
ORD-3002 priced via the manager: $130.00
Region A quote, re-read with no code touching it: $130.00

== Simplified: plain dependency injection of RateSettings ==
Domestic total (surcharge 1.00): $102.50
International total (surcharge 1.35): $144.00
Domestic total, re-read after pricing international: $102.50
```

Region A's quote moves from $110.00 to $130.00 with no code touching `regionA`; the domestic total stays at $102.50. One experiment, in a scratch copy:

- **Read the rate once.** Make `StandardRateCalculator` capture `surchargeMultiplier()` in a `final` field instead of reading it in `calculate`. One test fails: region A keeps $110.00. Each calculator now holds its own rate, but the registry still holds one slot, so a calculator's rate depends on when it was built.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original definition of a pattern as a recurring problem and the core of a solution to it, the definition used here to tell a pattern applied for a reason from one applied by reflex.
- Martin Fowler, [*Refactoring: Improving the Design of Existing Code*](https://martinfowler.com/books/refactoring.html), 2nd ed. (Addison-Wesley): the code-smell catalogue behind the god object and speculative-generality diagnoses, with the refactorings that walk a design back from either one.
- Robert C. Martin, [*Agile Software Development: Principles, Patterns, and Practices*](https://www.pearson.com/store/p/agile-software-development-principles-patterns-and-practices/P200000003363/9780135974445) (Prentice Hall): the Single Responsibility Principle applied to the god object, and the Dependency Inversion Principle the rewrite is built on.
- The full reading list lives on the [bibliography page]({{ '/bibliography/' | relative_url }}).
