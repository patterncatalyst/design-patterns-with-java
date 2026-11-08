---
title: "Strategy"
order: 8
layout: tutorial
sectionid: docs
part: "selecting-varying-behavior"
description: "Three idiomatic Java 25 ways to select a varying algorithm, taught against a real shipping-rate example: an interface with named classes, the same contract assigned as a lambda, and a closed set of carriers expressed as enum constants."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Say when a varying algorithm calls for Strategy and when a plain method or a `switch` is enough.
- Build a Strategy as an interface with named classes and a caller that depends only on the interface.
- Satisfy the same contract with a lambda, and compose two strategies into a third without a new class.
- Express a closed set of algorithms as enum constants with their own bodies, so the compiler rejects a missing one.

## Why this matters

A checkout flow quotes a shipping cost, and the rule for that cost changes per carrier contract: a flat rate, a base-plus-per-kilogram table, or a distance-and-weight zone table. The pricing rule varies while the checkout flow around it stays the same.

Java gives three idiomatic ways to isolate that rule, and they differ in whether the set of rules is open or closed and whether it's swapped at runtime or fixed at compile time.

{% include excalidraw.html name="08-strategy-forces" caption="Figure 8.1: Strategy's forces: one caller, one contract, and a set of interchangeable algorithms behind it, each expressed a different idiomatic way." %}

## Pattern: Strategy

### The problem

A `Shipment` is a record carrying a weight in kilograms, a `DestinationZone` (`LOCAL`, `REGIONAL`, `NATIONAL`, `INTERNATIONAL`) and a declared value in cents:

```java
public record Shipment(double weightKg, DestinationZone zone, long declaredValueCents) {

    public Shipment {
        Objects.requireNonNull(zone, "zone");
        if (weightKg < 0.0) {
            throw new IllegalArgumentException("weightKg cannot be negative: " + weightKg);
        }
    }
}
```

Checkout must price it under whichever carrier contract applies. Writing every rule into the checkout method ties each new contract to an edit of that method, and a fix to one rule sits beside code for the others.

### The solution

A **Strategy** puts each variant of an algorithm behind one interface, so the code that uses it depends on what the algorithm does rather than on which one it is.

Money is an integer `Money` record wrapping cents, never a `double`, so quotes compare exactly. A `RateQuote` record pairs a carrier name with a `Money` amount. The pricing math lives once, in `ShippingMath`, and every strategy calls into it, so the same `Shipment` prices identically whichever expression quotes it. A rule fixed in `ShippingMath` is fixed for all three.

| Expression | Set of rules | Swapped | Costs |
| --- | --- | --- | --- |
| Interface and named classes | Open | At runtime | A file, constructor, fields and override per rule |
| Lambda assigned to the interface | Open | At runtime | One expression; no type name for DI or a service loader |
| Enum with constant-specific bodies | Closed | Fixed at compile time | A recompile to add a rule |

### How Java models it

`RateStrategy` declares one method, `quote`, and three named classes implement it: `FlatRateStrategy`, `WeightTieredRateStrategy` and `DistanceRateStrategy`.

```java
public interface RateStrategy {
    RateQuote quote(Shipment shipment);
}

public final class WeightTieredRateStrategy implements RateStrategy {
    private final String carrierName;
    private final long baseCents;
    private final long perKgCents;

    public WeightTieredRateStrategy(String carrierName, long baseCents, long perKgCents) {
        this.carrierName = carrierName;
        this.baseCents = baseCents;
        this.perKgCents = perKgCents;
    }

    @Override
    public RateQuote quote(Shipment shipment) {
        long cents = ShippingMath.weightTieredCents(baseCents, perKgCents, shipment.weightKg());
        return new RateQuote(carrierName, Money.ofCents(cents));
    }
}
```

`ShippingRateCalculator` holds one `RateStrategy`, injected through the constructor and reassignable with `useStrategy()`:

```java
public final class ShippingRateCalculator {
    private RateStrategy strategy;

    public ShippingRateCalculator(RateStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    public void useStrategy(RateStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    public RateQuote quote(Shipment shipment) {
        return strategy.quote(shipment);
    }
}
```

The calculator never names a concrete strategy. A new carrier's rule is a new class that implements `RateStrategy`, and the calculator doesn't change: the Open/Closed Principle as a fact about one file. This is the form to use when the strategy is chosen outside the program, such as a carrier name read from configuration at startup and mapped to a class by a small factory.

### The same contract as a lambda

`RateStrategy` has one abstract method, so it's a functional interface whether or not it carries `@FunctionalInterface`, and a lambda satisfies it. The calculator's field is still typed `RateStrategy` and can't tell a lambda from a named class.

{% include codetabs.html langs="Interface|Lambda" %}

```java
// Interface: a dedicated file, a constructor, two fields, an override,
// before the pricing math runs.
public final class WeightTieredRateStrategy implements RateStrategy {
    private final String carrierName;
    private final long baseCents;
    private final long perKgCents;

    public WeightTieredRateStrategy(String carrierName, long baseCents, long perKgCents) {
        this.carrierName = carrierName;
        this.baseCents = baseCents;
        this.perKgCents = perKgCents;
    }

    @Override
    public RateQuote quote(Shipment shipment) {
        long cents = ShippingMath.weightTieredCents(baseCents, perKgCents, shipment.weightKg());
        return new RateQuote(carrierName, Money.ofCents(cents));
    }
}
```
```java
// Lambda: the identical contract, satisfied by one expression. No new
// class, no constructor, no field declarations to get right.
public static RateStrategy weightTiered(String carrierName, long baseCents, long perKgCents) {
    return shipment -> new RateQuote(
            carrierName,
            Money.ofCents(ShippingMath.weightTieredCents(baseCents, perKgCents, shipment.weightKg())));
}
```

Both produce equal `RateQuote` values for the same `Shipment`.

- **Use the lambda** when a strategy is built from values known at the call site and used once or twice, where a class would exist only to hold a constructor.
- **Use a named class** when the strategy carries enough state or behavior that a name helps, or when dependency injection, reflection or a service loader must find it by type.

### Composing strategies

A lambda is a value that closes over its inputs, so one strategy can wrap another. `withInsuranceSurcharge` returns a strategy that adds a surcharge proportional to the declared value:

```java
public static RateStrategy withInsuranceSurcharge(RateStrategy base, double centsPerDeclaredDollar) {
    return shipment -> {
        RateQuote baseQuote = base.quote(shipment);
        long surchargeCents = ShippingMath.insuranceSurchargeCents(
                shipment.declaredValueCents(), centsPerDeclaredDollar);
        return new RateQuote(baseQuote.quotedBy(), baseQuote.amount().plus(Money.ofCents(surchargeCents)));
    };
}
```

With named classes the same effect needs a decorator: a class that implements `RateStrategy`, holds a wrapped one, forwards to it and adds the surcharge. The result of `withInsuranceSurcharge` is a plain `RateStrategy`, so calling it again stacks a second surcharge. Decorator earns a class of its own when the wrapped type has several methods a caller sees through, not for a single-method contract.

### A closed set as enum constants

Some sets are closed. A company contracts with exactly three carriers, and the list changes only when a new contract is signed, rarely enough that a recompile is acceptable. `Carrier` implements `RateStrategy` with a body per constant:

```java
public enum Carrier implements RateStrategy {
    GROUND {
        @Override
        public RateQuote quote(Shipment shipment) {
            long cents = ShippingMath.weightTieredCents(500, 180, shipment.weightKg());
            return new RateQuote("ground", Money.ofCents(cents));
        }
    },
    AIR_FLAT {
        @Override
        public RateQuote quote(Shipment shipment) {
            return new RateQuote("air-flat", Money.ofCents(999));
        }
    },
    FREIGHT_ZONE {
        @Override
        public RateQuote quote(Shipment shipment) {
            long cents = ShippingMath.distanceZoneCents(shipment.zone(), shipment.weightKg());
            return new RateQuote("freight-zone", Money.ofCents(cents));
        }
    }
}
```

The compiler enforces completeness. Add a fourth constant without a `quote` body and the build fails with `Carrier is abstract; cannot be instantiated`. A `switch` statement on carrier identity with a throwing `default` catches the same omission only at runtime, on the first shipment that reaches it. The price is flexibility: a `Carrier` field can't be pointed at a new rule the way a `RateStrategy` field can take a new lambda. That suits a closed set and hurts one that's still growing. *Effective Java* Item 34 covers this idiom.

## When not to use it

| Situation | Write instead |
| --- | --- |
| One fixed algorithm, no credible second one | Call `ShippingMath.weightTieredCents(...)` directly. |
| A few carriers in a short `switch` one team reads | Keep the `switch` until the list grows past a handful, different teams own different carriers, or the choice must come from configuration at runtime. |
| Only a number varies (a per-kilogram rate, a flat fee, a markup) | A configuration value or a small data table. `WeightTieredRateStrategy` already takes `baseCents` and `perKgCents` as constructor arguments; a new subclass per carrier that only changes those two numbers mistakes configuration for behavior. |
| A `RateStrategy` with one implementation and none planned | A method. |

> **🤖 Explore with your agent**
>
> "In `examples/08-strategy`, keep `RateStrategy` as a single-method interface and `ShippingRateCalculator` depending only on it. Put all pricing math in `ShippingMath` and have every strategy call it. Add open-set rules as lambdas in `RateStrategies`, and use a named class only where DI or a service loader needs a type. Add insurance by composing with `withInsuranceSurcharge`, not a decorator class. Keep the contracted carriers as `Carrier` enum constants with constant-specific `quote` bodies, never a `switch` with a throwing `default`."
>
> Check: `mvn -q -f examples/08-strategy/pom.xml test -Dtest=StrategyEquivalenceTest`; the interface, lambda and enum forms must quote the same `Shipment` identically.

## Build, run, observe

From the repository root, or standalone from `examples/08-strategy`:

```sh
mvn -f examples/pom.xml test
```

```sh
cd examples/08-strategy
mvn test
```

All 33 tests pass. To see the three expressions price the same 3.5 kg `REGIONAL` shipment with a declared value of $120:

```sh
examples/08-strategy/demo.sh
```

```
== Interface: named classes implementing RateStrategy ==
Weight-tiered strategy: RateQuote[quotedBy=ground, amount=$11.30]
Distance-zone strategy (swapped at runtime): RateQuote[quotedBy=freight-zone, amount=$11.70]
Flat-rate strategy (new instance): RateQuote[quotedBy=air-flat, amount=$9.99]

== Lambda: the same contract as a one-expression value ==
Weight-tiered lambda: RateQuote[quotedBy=ground, amount=$11.30]
Weight-tiered lambda + insurance (composed): RateQuote[quotedBy=ground, amount=$12.26]

== Enum: a closed set as constants ==
GROUND: RateQuote[quotedBy=ground, amount=$11.30]
AIR_FLAT: RateQuote[quotedBy=air-flat, amount=$9.99]
FREIGHT_ZONE: RateQuote[quotedBy=freight-zone, amount=$11.70]
```

The named class, the lambda and `GROUND` all quote $11.30, because all three call `ShippingMath`. Two experiments, each in a scratch copy:

- **Add a carrier with no body.** Append `RAIL` to `Carrier` with no `quote` body. Compilation fails: `Carrier is abstract; cannot be instantiated`.
- **Let one form drift.** Change `GROUND`'s per-kilogram rate from 180 to 190. The weight-tiered equivalence case for `GROUND` fails; the other 32 pass.

## What you can do now

- Choose between a named class, a lambda and an enum constant by whether the set is open and when it's swapped.
- Keep a caller like `ShippingRateCalculator` dependent only on the interface.
- Compose strategies as values with a function like `withInsuranceSurcharge`.
- Get a compile-time completeness check from constant-specific enum bodies.
- Leave a single rule as a method and a varying number as configuration.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original Strategy entry, with the intent statement all three expressions satisfy.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 34 covers enum constants with constant-specific method bodies as an alternative to a `switch` on an enum's value, the idiom behind `Carrier`.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/08-strategy`.
- The full reading list lives on the [bibliography page]({{ '/bibliography/' | relative_url }}).
