---
title: "Singleton and the DI alternative"
order: 14
layout: tutorial
sectionid: docs
part: "creating-objects-under-control"
description: "The two modern, correct ways to build a Java singleton, the enum form and the initialization-on-demand holder idiom, the older double-checked-locking approach they retire, and the dependency-injection alternative that gives the same reachable capability without the hidden global."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Build the enum singleton and name what the language guarantees it: no second instance through reflection or serialization.
- Build the initialization-on-demand holder idiom, and explain why it's lazy and thread-safe with no `synchronized`.
- Say why double-checked locking needed `volatile`, and why both modern forms retire it.
- Name the cost of a global singleton: a dependency no signature declares, and no seam for a test.
- Provide the same capability through constructor injection and substitute a test double for it.
- Judge when a singleton is still the right call.

## Why this matters

A carrier rate table has one defensible shape: one copy, loaded once, read by everything that quotes a price, never mutated. That's the clean case for Singleton. The data is read-only, so the only race is construction, and nothing needs a second, different table.

Most collaborators aren't that clean. Reaching a global for them hides a dependency that every test then has to work around. A constructor that declares its collaborator is the default worth reaching for.

{% include excalidraw.html name="14-singleton-vs-injected-collaborator" caption="Figure 14.1: a global singleton's callers reach into one shared instance with no parameter announcing it; the injected alternative routes the same capability through a constructor a composition root controls." %}

Every version needs one capability, a base per-kilogram rate for a carrier code:

```java
@FunctionalInterface
public interface RateLookup {
    Money baseRateFor(String carrierCode);
}
```

The rate data lives once, in `CarrierRates`, so a corrected rate reaches both singleton forms:

```java
final class CarrierRates {
    private CarrierRates() {}

    static Map<String, Money> baseRatesByCarrierCode() {
        return Map.of(
                "GROUND", Money.ofCents(180),
                "AIR", Money.ofCents(640),
                "FREIGHT", Money.ofCents(95));
    }
}
```

## Pattern: Singleton

### The problem

Every part of the shipping system that quotes a price needs the same rate table. Loading a copy per caller wastes the load and allows two copies to drift apart.

### The solution

A **Singleton** ensures a class has exactly one instance and provides a global point of access to it.

| Form | Created | Thread safety from | Reflection and serialization |
| --- | --- | --- | --- |
| Enum | When the enum class initializes | Class initialization | Guaranteed by the language |
| Initialization-on-demand holder | On the first `getInstance()` | Class initialization | Up to the author |
| Double-checked locking | On the first `getInstance()` | `volatile` plus `synchronized`, written by hand | Up to the author |

### How Java models it

Bloch's recommended form is a single-element enum (*Effective Java*, Item 3):

```java
public enum CarrierRateTable implements RateLookup {
    INSTANCE;

    private final Map<String, Money> ratesByCarrierCode = CarrierRates.baseRatesByCarrierCode();

    @Override
    public Money baseRateFor(String carrierCode) {
        Money rate = ratesByCarrierCode.get(carrierCode);
        if (rate == null) {
            throw new IllegalArgumentException("no base rate for carrier code: " + carrierCode);
        }
        return rate;
    }
}
```

The language supplies two guarantees no author has to remember:

- **Reflection.** `Constructor.newInstance()` on an enum's constructor throws `IllegalArgumentException`, even after `setAccessible(true)`.
- **Serialization.** An enum constant serializes by name and deserializes by looking the name up, so there's no `readResolve()` to write and no way to manufacture a second `INSTANCE`.

### The holder idiom

The enum builds `INSTANCE` when the enum class initializes. That's fine for cheap data, but a heavier resource may need construction deferred to first use:

```java
public final class HolderCarrierRateTable implements RateLookup {
    private final Map<String, Money> ratesByCarrierCode;

    private HolderCarrierRateTable() {
        this.ratesByCarrierCode = CarrierRates.baseRatesByCarrierCode();
    }

    public static HolderCarrierRateTable getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public Money baseRateFor(String carrierCode) {
        Money rate = ratesByCarrierCode.get(carrierCode);
        if (rate == null) {
            throw new IllegalArgumentException("no base rate for carrier code: " + carrierCode);
        }
        return rate;
    }

    private static final class Holder {
        private static final HolderCarrierRateTable INSTANCE = new HolderCarrierRateTable();
    }
}
```

The nested `Holder` isn't initialized until `getInstance()` first touches it. The Java Language Specification runs a class's static initializers exactly once, and every thread that races that initialization waits for it to finish. Sixteen threads calling `getInstance()` at once all get the same reference, and later calls take no lock because there's none left to take.

### What double-checked locking cost

The older route to lazy, thread-safe construction checked for the instance, entered a `synchronized` block only if it was missing, and checked again inside:

```java
// Needs the field declared volatile for correctness, and still reads as a
// harder pattern to get right than it looks: without volatile, a thread can
// observe a half-constructed object through a reordered write.
private static volatile CarrierRateTable instance;

static CarrierRateTable getInstance() {
    if (instance == null) {
        synchronized (CarrierRateTable.class) {
            if (instance == null) {
                instance = new CarrierRateTable();
            }
        }
    }
    return instance;
}
```

`volatile` is required. Without it, the Java Memory Model lets another thread see `instance` become non-null before it sees the constructor's writes to the object's fields. Correctness depends on the `volatile`, the second check and knowing why both are needed. The holder idiom gets the same laziness and safety from class initialization, and the enum covers the eager case, so current Java has no use for double-checked locking in a singleton.

### What a singleton costs elsewhere

`CarrierRateTable` fits because its data is read-only and nothing needs a second copy. `WarehousePricingJob` reaches for it anyway:

```java
public final class WarehousePricingJob {
    public Money quoteBaseRate(String carrierCode) {
        return CarrierRateTable.INSTANCE.baseRateFor(carrierCode);
    }
}
```

Neither the method signature nor the constructor reveals that pricing depends on `CarrierRateTable`; a reader finds out only by opening the method. There's no parameter to substitute another rate source, so a test can only check the job against whatever the global table holds. A withdrawn carrier, a promotional rate or an unavailable rate source has no seam to come through.

## Pattern: Dependency Injection

### The problem

`ShippingQuoteService` needs the same base rate, and it must be testable against rates that differ from production: a promotion, a missing carrier, a failing source.

### The solution

**Dependency injection** hands an object its collaborators, usually through the constructor, instead of letting the object reach for them.

### How Java models it

```java
public final class ShippingQuoteService {
    private final RateLookup rateLookup;

    public ShippingQuoteService(RateLookup rateLookup) {
        this.rateLookup = Objects.requireNonNull(rateLookup, "rateLookup");
    }

    public Money quote(String carrierCode, double weightKg) {
        if (weightKg < 0.0) {
            throw new IllegalArgumentException("weightKg cannot be negative: " + weightKg);
        }
        Money baseRate = rateLookup.baseRateFor(carrierCode);
        return Money.ofCents(Math.round(baseRate.cents() * weightKg));
    }
}
```

The service depends on `RateLookup`, never on `CarrierRateTable` or `HolderCarrierRateTable` by name: the Dependency Inversion Principle. Production passes `CarrierRateTable.INSTANCE` or `HolderCarrierRateTable.getInstance()`; a test passes a Mockito double, a one-line lambda or a hand-written fake. The same class, unchanged, quotes correctly against every one.

Singleton and dependency injection answer different questions. The two singletons already settle how many instances exist: one. Injection settles how code that needs the shared instance gets a reference to it, and the answer stays substitutable however the capability changes later.

## When not to use it

Singleton fits only when all three hold: one instance for the life of the process, effectively immutable once built, and no caller that will plausibly need a different one. `CarrierRateTable` passes; a second rate table would only mean the first was wrong.

| Situation | Write instead |
| --- | --- |
| A collaborator a test or another environment may need to replace | A constructor parameter typed to an interface, like `RateLookup`. Retrofitting it after three classes call `getInstance()` costs more than adding it now. |
| A global used only to avoid passing a parameter through a few layers | Pass the shared object, or restructure the call chain. A shorter parameter list doesn't pay for hidden coupling and a lost test seam. |
| A singleton with mutable state | An injected instance whose lifetime the composition root owns. |

> **🤖 Explore with your agent**
>
> "In `examples/14-singleton`, refactor `WarehousePricingJob` so it takes a `RateLookup` through its constructor, as `ShippingQuoteService` does, instead of calling `CarrierRateTable.INSTANCE` inside `quoteBaseRate`. Keep `CarrierRateTable` as the single-element enum and `HolderCarrierRateTable` as the holder idiom, both reading `CarrierRates`; don't add double-checked locking or a `synchronized` `getInstance()`. Wire the production singletons in at the composition root only, and add a test that passes a lambda `RateLookup` to the refactored job."
>
> Check: `mvn -q -f examples/14-singleton/pom.xml test -Dtest=ShippingQuoteServiceTest,WarehousePricingJobTest`, and confirm `WarehousePricingJob` no longer names `CarrierRateTable.INSTANCE`.

## Build, run, observe

From the repository root, or standalone from `examples/14-singleton`:

```sh
mvn -f examples/pom.xml test
```

```sh
cd examples/14-singleton
mvn test
```

All 18 tests pass. To run all four side by side on a `GROUND` shipment:

```sh
examples/14-singleton/demo.sh
```

```
== Enum singleton: CarrierRateTable ==
Same instance from both references: true
GROUND base rate: $1.80 per kg

== Holder idiom: HolderCarrierRateTable ==
Same instance from both calls: true
GROUND base rate: $1.80 per kg

== Hidden dependency: WarehousePricingJob reaches into the global ==
Warehouse job GROUND quote: $1.80 per kg

== Dependency injection: ShippingQuoteService ==
Production service, 12kg GROUND shipment: $21.60
Promotional service (fake RateLookup), 12kg shipment: $0.12
```

The two singletons and the job agree on $1.80 per kilogram because all read `CarrierRates`. Only the injected service can swap its rate source for a one-line fake. Two experiments, each in a scratch copy:

- **Reach for the global.** In `ShippingQuoteService.quote()`, call `CarrierRateTable.INSTANCE.baseRateFor` instead of `rateLookup`. The Mockito and hand-written-fake checks fail; the service can no longer be substituted.
- **Break the holder.** Make `HolderCarrierRateTable.getInstance()` return `new HolderCarrierRateTable()`. The same-instance check and the 16-thread race check both fail.

## What you can do now

- Build a singleton as a single-element enum, or with a holder class when it must be lazy.
- Retire double-checked locking in favor of class-initialization guarantees.
- Spot a hidden global like `CarrierRateTable.INSTANCE` inside a method body.
- Inject a collaborator through an interface so a test can replace it.
- Reserve Singleton for one immutable instance nothing needs to substitute.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original Singleton entry, including the intent statement `CarrierRateTable` satisfies.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 3 recommends the single-element enum as the best way to implement a singleton, covering its reflection and serialization safety.
- Robert C. Martin, [*Agile Software Development: Principles, Patterns, and Practices*](https://www.pearson.com/store/p/agile-software-development-principles-patterns-and-practices/P200000003363/9780135974445) (Prentice Hall): the Dependency Inversion Principle `ShippingQuoteService` applies.
- The full reading list lives on the [bibliography page]({{ '/bibliography/' | relative_url }}).
