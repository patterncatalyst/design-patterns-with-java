---
title: "Strategy"
order: 8
layout: tutorial
sectionid: docs
part: "selecting-varying-behavior"
description: "Three idiomatic Java ways to select a varying algorithm, taught against a real shipping-rate example: a Classic Java interface with concrete classes, the same contract assigned as a Java 25 lambda, and a closed set of carriers expressed as enum constants."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Name the forces that make Strategy the right candidate pattern for a
  varying algorithm, and the plainer alternative it competes against.
- Build a Strategy the Classic Java way: an interface, named concrete
  implementations, and a caller that depends on the interface rather than on
  which implementation it holds.
- Build the identical contract as a Java 25 lambda, and explain what a
  one-expression strategy buys you over a dedicated class file.
- Compose two strategies into a third one with a lambda, and say what a class
  hierarchy would need instead to get the same capability.
- Express a closed set of algorithms as enum constants, each carrying its
  own method body, and say why the compiler can enforce completeness there
  in a way a `switch` statement cannot.
- Recognize when a Strategy is solving a problem nobody has yet, and say
  what to write instead.

## Why this matters

A checkout flow has to quote a shipping cost, and the rule for computing
that cost is not fixed. A merchant might ship everything at a flat rate,
price by weight against a published base-plus-per-kilogram table, or run a
distance-and-weight zone table, and the choice is made per carrier contract,
not per line of code. That is a force: the pricing rule varies independently
of the checkout flow around it. Strategy is the Gang of Four's name for
resolving exactly that force, extracting the algorithm that varies into its
own unit and letting the code that uses it depend on what the algorithm does
rather than on which one it is.

What makes Strategy worth a full chapter is that Java gives you more than
one way to resolve the same force, and the right one depends on how the set
of algorithms behaves over time. An interface with named concrete classes
buys you a strategy a running program can swap at will, at the price of a
small ceremony: a file, a constructor, a field, an override, before any
pricing logic runs. A lambda assigned directly to that same interface buys
you the same runtime swappability with none of that ceremony, and composes
with another lambda into a third strategy without a new class anywhere. An
enum, with each constant carrying its own method body, buys you something
neither of the other two can: a closed, enumerable set where the compiler
itself refuses to let a constant go unimplemented. None of the three is "the"
Strategy pattern in this book's catalogue. All three are, and choosing among
them, based on whether the set is open or closed and whether swapping
happens at runtime or at compile time, is itself the design decision this
chapter trains.

{% include excalidraw.html name="08-strategy-forces" caption="Figure 8.1: Strategy's forces: one caller, one contract, and a set of interchangeable algorithms behind it, each expressed a different idiomatic way." %}

## The domain: pricing a shipment at checkout

The example behind this chapter lives in `examples/08-strategy`, and its
domain stays plain, so the three Strategy expressions remain the
focus. A `Shipment` is a record carrying a weight in kilograms, a
`DestinationZone` (`LOCAL`, `REGIONAL`, `NATIONAL`, `INTERNATIONAL`), and a
declared value in cents:

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

Money in this domain is always an integer `Money` record wrapping cents,
never a `double`, so a quote is exact and a test assertion can compare it for
equality without an epsilon anywhere in sight. A `RateQuote` record pairs a
carrier name with a `Money` amount. The pricing math itself lives in one
place, `ShippingMath`, and every one of the three Strategy expressions in
this chapter calls down into it. This separation matters more than it looks.
It means an identical `Shipment` prices identically no matter which
dispatch mechanism quoted it, which is the behavior this example's JUnit 5
and AssertJ suite verifies, not an incidental side effect of three
implementations happening to agree. If this example mixed the pricing rules
into each Strategy expression separately, a bug fixed in one would stay
broken in the other two; keeping the rate math as a shared utility makes
that class of bug impossible rather than merely unlikely.

## Classic Java: an interface with concrete implementations

The Classic Java shape is an interface and a family of named concrete
classes, each implementing the one method the interface declares.
`RateStrategy` declares a single method, `quote`, and three implementations
satisfy it: `FlatRateStrategy`, `WeightTieredRateStrategy`, and
`DistanceRateStrategy`.

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

`ShippingRateCalculator` is the object that uses a strategy. It
owns exactly one `RateStrategy` at a time, injected through the constructor
and reassignable later with `useStrategy()`:

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

`ShippingRateCalculator` never mentions `WeightTieredRateStrategy`,
`FlatRateStrategy`, or `DistanceRateStrategy` by name. It depends only on
`RateStrategy`, so a new carrier's pricing rule can be added by writing a new
class that implements the interface, with nothing in `ShippingRateCalculator`
touched to admit it. That is the Open/Closed Principle showing up as a
concrete, testable fact rather than a slogan: the calculator is open to a
new strategy and closed to modification for adding one. This is also the
expression to reach for when a strategy must be selected from outside the
program itself, for instance a carrier name read from a configuration file
at startup and mapped to a concrete class through a small factory, because a
`RateStrategy` field can hold any one of several concrete types behind a
single, uniform interface, assigned at a point the rest of the program never
has to see.

## Java 25: the same contract with less ceremony

`RateStrategy` declares exactly one abstract method, which makes it a
functional interface whether or not anyone marks it `@FunctionalInterface`.
That single fact is what lets Java 25 code satisfy the identical contract
with a lambda instead of a named class. Nothing about `ShippingRateCalculator`
changes; the field is still typed `RateStrategy`, and it cannot tell whether
the object it holds came from `new WeightTieredRateStrategy(...)` or from an
expression that never declared a class at all.

{% include codetabs.html langs="Classic Java|Java 25" %}

```java
// Classic Java: a dedicated file, a constructor, two fields, an override,
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
// Java 25: the identical contract, satisfied by one expression. No new
// class, no constructor, no field declarations to get right.
public static RateStrategy weightTiered(String carrierName, long baseCents, long perKgCents) {
    return shipment -> new RateQuote(
            carrierName,
            Money.ofCents(ShippingMath.weightTieredCents(baseCents, perKgCents, shipment.weightKg())));
}
```

Both columns produce byte-for-byte identical `RateQuote` values for the same
`Shipment`, which the example's parameterized test suite checks directly
rather than assuming. Reach for the lambda form when a strategy is
built, used once or twice, and discarded, especially one assembled from
values only known at the call site, where a dedicated class would exist
purely to hold a constructor nobody reuses. The named-class form fits better
when a strategy carries enough internal state or behavior that giving it a
name and a file clarifies the code, or when it needs to be discovered by
reflection, dependency injection, or a service-loader mechanism that expects
a type, not a value.

## Composing strategies: an insurance surcharge

A lambda adds one capability a class hierarchy does not provide without
extra code: composition. `withInsuranceSurcharge` takes an existing `RateStrategy` and
returns a new one that adds a surcharge proportional to the shipment's
declared value:

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

Getting the same effect from the Classic Java interface would mean
introducing a decorator class, a second pattern, just to wrap one
`RateStrategy` around another: a class implementing `RateStrategy` that
holds a wrapped `RateStrategy` field, forwards to it, and adds the surcharge
afterward. `withInsuranceSurcharge` needs no new type because a lambda is
already a value that closes over whatever state it needs, here the wrapped
`base` strategy and the surcharge rate. Calling it twice, once per surcharge,
stacks the surcharges without writing a second decorator either; each call
returns a plain `RateStrategy` that is just as composable as the one passed
in. The Decorator pattern earns its name when the thing being wrapped
exposes more than one method a caller needs to see through, not when a
single-method contract like this one can be composed as a value.

## A closed set: carriers as enum constants

The strategies above are open: a new pricing rule can show up at any time,
built from a configuration value, written by a team that owns one carrier
contract, or assembled at a call site nobody else touches. Some sets are not
open. A company might contract with exactly three carriers, each with its
own rate algorithm, and that list changes only when legal signs a new
contract, an event rare enough that it is reasonable to require a
recompile. `Carrier` expresses that closed set directly as an enum, with
each constant implementing `RateStrategy` through its own body:

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

The reason this earns a place next to the interface and the lambda, rather
than counting as a stylistic variant of either, is what the compiler
guarantees. A method that dispatches on carrier identity the older way,
with a `switch` statement and a `default` branch that throws, only catches a
forgotten carrier at runtime, on whatever shipment happens to exercise it
first.

{% include codetabs.html langs="Classic Java|Java 25" %}

```java
// Classic Java: a switch statement dispatches on carrier identity. Adding
// a fourth carrier compiles cleanly even if this method is never updated;
// the gap only shows up when a shipment for the new carrier hits the
// default branch at runtime.
public static RateQuote rateFor(Carrier carrier, Shipment shipment) {
    switch (carrier) {
        case GROUND:
            return new RateQuote("ground", Money.ofCents(
                    ShippingMath.weightTieredCents(500, 180, shipment.weightKg())));
        case AIR_FLAT:
            return new RateQuote("air-flat", Money.ofCents(999));
        case FREIGHT_ZONE:
            return new RateQuote("freight-zone", Money.ofCents(
                    ShippingMath.distanceZoneCents(shipment.zone(), shipment.weightKg())));
        default:
            throw new IllegalArgumentException("Unknown carrier: " + carrier);
    }
}
```
```java
// Java 25: each constant supplies its own body. Adding a fourth carrier
// without giving it a quote() implementation is a compile error, not a
// runtime surprise on whichever shipment reaches the missing case first.
public enum Carrier implements RateStrategy {
    GROUND { /* quote(Shipment) body */ },
    AIR_FLAT { /* quote(Shipment) body */ },
    FREIGHT_ZONE { /* quote(Shipment) body */ }
    // A fourth constant with no body here fails to compile.
}
```

Where the interface-plus-lambda pair trades ceremony for brevity with no
change in what the compiler checks, the enum trades runtime flexibility for
a stronger compile-time guarantee. A `Carrier` field can never be reassigned
to a fourth, as-yet-unwritten carrier the way a `RateStrategy` field can be
reassigned to a brand-new lambda; the set is fixed the moment the program is
built. That tradeoff is exactly right for a closed set and exactly
wrong for one that is still growing.

> **AI sidebar.** Naming the pattern out loud, "this needs to be a Strategy
> because the carrier rule varies independently of checkout," gives you and
> an LLM pairing partner the same shared vocabulary a human reviewer would
> use. It also sets up a specific thing to check in whatever the model hands
> back: a model trained on a large corpus of Java tutorials reaches for the
> interface-plus-named-classes shape by reflex, because it is the version
> that appears in the most textbooks. Read the forces before accepting that
> shape. If a strategy is built once at a call site and never needs a name
> of its own, a lambda assigned to the same interface is a smaller, equally
> correct answer with no extra file. If the set of algorithms is closed and
> known in full today, an enum with constant-specific bodies is smaller
> still, and it gets you a compile-time completeness check a generated
> interface-and-classes answer will not mention unless you ask for it. A
> generated `RateStrategy` with exactly one implementation and no second one
> planned is the clearest sign the model reached for the familiar shape
> instead of the one the forces call for.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this loop.
From the repository root, the aggregator picks up `examples/08-strategy`
automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/08-strategy
mvn test
```

Either path runs the same 33 JUnit 5 cases, asserted with AssertJ, including
a parameterized suite that checks the Classic Java, lambda, and enum
expressions against each other directly rather than against three separate
expected values that happen to match. Observed locally on Fedora with
Temurin 25.0.2:

```
Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see the three strategies price the identical `Shipment` side by side
rather than just reading an assertion that says they agree, run the
example's own demo script:

```sh
examples/08-strategy/demo.sh
```

It prints each expression's quote for the same 3.5 kilogram shipment headed
to a `REGIONAL` destination zone with a declared value of $120:

```
== Classic Java: interface + named concrete classes ==
Weight-tiered strategy: RateQuote[quotedBy=ground, amount=$11.30]
Distance-zone strategy (swapped at runtime): RateQuote[quotedBy=freight-zone, amount=$11.70]
Flat-rate strategy (new instance): RateQuote[quotedBy=air-flat, amount=$9.99]

== Java 25: the same contract as a lambda ==
Weight-tiered lambda: RateQuote[quotedBy=ground, amount=$11.30]
Weight-tiered lambda + insurance (composed): RateQuote[quotedBy=ground, amount=$12.26]

== Java 25: a closed set as enum constants ==
GROUND: RateQuote[quotedBy=ground, amount=$11.30]
AIR_FLAT: RateQuote[quotedBy=air-flat, amount=$9.99]
FREIGHT_ZONE: RateQuote[quotedBy=freight-zone, amount=$11.70]
```

The Classic Java "Weight-tiered strategy", the Java 25 "Weight-tiered
lambda", and `GROUND` all quote `$11.30` for the same shipment. That
agreement is the shared `ShippingMath` showing up as an observable fact at
the terminal, not just as a passing test.

## When not to use Strategy

Strategy earns its abstraction when an algorithm varies independently of
the code around it, and when more than one implementation is either in hand
already or clearly coming. It stops paying for itself well before that.

A single fixed algorithm with no credible second implementation does not
need a `RateStrategy` interface, a lambda, or an enum; it needs a method. If
every shipment in a deployment used the same weight-tiered rate table and
nothing planned ever changes that, wrapping
`ShippingMath.weightTieredCents(...)` in any of this chapter's expressions
adds a layer of indirection with nothing underneath it to justify the cost,
whether that cost is a field a caller now has to manage, a lambda a reader
has to trace back to its definition, or an enum constant that could just as
well have been a constant. The plainer non-pattern there is calling
`ShippingMath.weightTieredCents(...)` directly from checkout code: name the
pattern and the plain alternative side by side, and let the forces, not the
pattern's familiarity, decide between them.

A branch that is not expensive to extend is also a sign Strategy is
premature. Three carriers, checked in sequence by a short `switch` inside a
single method that one team reads start to finish, do not need an interface
or an enum of their own; the `switch` is the whole story, visible at a
glance, and a reviewer can confirm it is correct without jumping to three
other files. The point where that calculus flips is the point a growing
`if`/`else if` chain starts to hurt in any codebase: the carrier list grows
past a handful, different carriers' pricing logic is owned by different
teams who should not need to touch one shared method to add a carrier, or a
carrier choice must be swapped at runtime from configuration rather than
fixed in source. Until one of those is true, the plain method or the plain
`switch` is the stronger engineering choice, even with Strategy's name
sitting right there, fresh from this chapter.

Configuration is also not behavior. If the only thing that varies between
carriers is a number, a per-kilogram rate, a flat fee, a markup percentage,
reach for a configuration value or a small data table before reaching for a
Strategy. `WeightTieredRateStrategy`'s constructor already takes
`baseCents` and `perKgCents` as arguments precisely because those two
numbers are configuration, not algorithm; only `distanceZoneCents`'s
zone-to-rate table and the choice of which formula to apply are the actual
variation Strategy exists to isolate. A codebase that builds a new
`RateStrategy` subclass for every carrier that only ever changes its two
numeric parameters has mistaken a configuration problem for a polymorphism
problem.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original Strategy entry, including the intent statement this chapter's three expressions all satisfy in different idioms.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 34 covers enum constants with constant-specific method bodies as a direct alternative to a `switch` on an enum's value, the idiom behind this chapter's `Carrier` expression.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/08-strategy`'s suite, which asserts that all three expressions quote an identical `Shipment` identically rather than just checking that the build exits zero.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
