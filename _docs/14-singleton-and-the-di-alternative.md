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

- Build the enum singleton, Joshua Bloch's recommended form, and name the
  two guarantees the language itself gives it: no second instance via
  reflection, no custom method needed to survive serialization.
- Build the initialization-on-demand holder idiom, and explain why it is
  lazy and thread-safe with no `synchronized` keyword anywhere in the code.
- Explain, in plain terms, why double-checked locking with a `volatile`
  field was the historically tricky way to get the same guarantee, and why
  both modern forms make it unnecessary.
- Name the concrete cost a global singleton imposes: a hidden dependency no
  constructor signature announces, and no seam left for a test to
  substitute something else.
- Rebuild the identical capability as an abstraction injected by
  constructor, and prove the substitutability a global cannot offer with a
  test double.
- Judge when a singleton is still the right call and when dependency
  injection is the stronger engineering choice.

## Why this matters

A carrier rate table has exactly one defensible shape: one copy, loaded
once, read by every part of a shipping system that needs to quote a price,
and never mutated after it loads. That is the Gang of Four's Singleton
intent: ensure a class has only one instance and provide a
global point of access to it. It is also the single cleanest case for the
pattern this book will show, because every force that makes Singleton
risky elsewhere is absent here: the data is read-only, so there is no race
to protect beyond construction itself, and nothing in the system plausibly
needs a second, different rate table.

This chapter teaches Singleton in both of the forms Java gives
you for building it correctly today, then spends the rest of its space on
the harder and more useful lesson: most collaborators in a real codebase
are not this clean, and reaching for a global point of access for them
hides a dependency that fights every test written against it. The
dependency-inversion work already in this book's toolkit, a constructor
that declares its collaborator instead of a method that reaches for one,
is the alternative this chapter builds against a second, more typical
example, and the one worth reaching for by default.

{% include excalidraw.html name="14-singleton-vs-injected-collaborator" caption="Figure 14.1: a global singleton's callers reach into one shared instance with no parameter announcing it; the injected alternative routes the same capability through a constructor a composition root controls." %}

## The domain: pricing a shipment from a carrier rate table

The example behind this chapter lives in `examples/14-singleton`. A
`RateLookup` is the one capability every version in this chapter needs: a
base, per-kilogram rate for a carrier code.

```java
@FunctionalInterface
public interface RateLookup {
    Money baseRateFor(String carrierCode);
}
```

Both singleton forms below satisfy this same contract, and the rate data
itself lives in exactly one shared place, `CarrierRates`, so a correction to
a published rate lands in both idioms at once rather than needing to be
copied twice:

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

Keeping the rate data in one shared utility, the same shared-core move the
Strategy chapter used for its own pricing math, is what makes this
chapter's equivalence claims checkable: a test can assert the enum form and
the holder form answer identically for the same carrier, because both read
from the same table rather than two tables that happen to agree today.

## The enum singleton

Bloch's recommended form for a type that must have exactly one instance is
a single-element enum:

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

Two guarantees come from the language itself, not from any discipline the
author of this class has to remember. An enum type can never be
instantiated a second time through reflection: calling
`Constructor.newInstance()` on an enum's generated constructor throws
`IllegalArgumentException` unconditionally, even after `setAccessible(true)`
strips the usual access check. `CarrierRateTableTest` proves this directly
rather than asserting it from a specification, by catching the exception
the JVM actually throws. The second guarantee is about serialization: an
enum constant serializes by name and deserializes by looking that name back
up among the enum's declared constants, so there is no custom
`readResolve()` method to remember and no way for deserialization to
manufacture a second `INSTANCE`. Every other singleton idiom in Java's
history has needed an author to get one of these two guarantees right by
hand. The enum singleton gets both from the compiler.

## The initialization-on-demand holder idiom

The enum form constructs `INSTANCE` the moment `CarrierRateTable`'s class
is loaded, which is fine for data this cheap but is not always what a
heavier resource needs. The holder idiom defers construction until first
use, with no lock anywhere in the code that runs after that first call:

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

`HolderCarrierRateTable` loading has no side effect at all. The nested
`Holder` class, and the `INSTANCE` field inside it, are not touched until
`getInstance()` runs for the first time, which triggers the JVM to load and
initialize `Holder`. The thread safety comes directly from the Java
Language Specification's rules for class initialization: a class's static
initializers run exactly once, and every thread that triggers or races that
initialization blocks until it finishes. Two threads calling
`getInstance()` concurrently for the first time cannot observe two
different objects, and neither thread pays a locking cost on the tenth
call, because by then `Holder` has already finished loading and there is no
lock left to acquire. `HolderCarrierRateTableTest` makes this concrete by
racing sixteen threads at `getInstance()` simultaneously and asserting that
every one of them got back the same reference.

## What double-checked locking used to cost

Before the holder idiom was common knowledge, the standard way to get lazy,
thread-safe construction was double-checked locking: check whether the
instance exists, and only enter a `synchronized` block and construct it if
it does not, checking a second time inside the lock in case another thread
got there first.

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

The `volatile` modifier is required for correctness here, not decoration.
Without it, a thread on another CPU core can observe the reference to `instance`
become non-null before it observes the constructor's writes to that
object's fields, because the Java Memory Model permits that reordering in
the absence of a `volatile` or other synchronization. Getting this pattern
right means remembering `volatile`, remembering the double check, and
remembering why both are required together, three separate facts a reader
has to hold at once to trust a class that otherwise looks like four
ordinary lines of code. The holder idiom gets identical laziness and
identical thread safety from the class-loading guarantees the JVM already
provides, with none of those three facts to get right by hand. This is this
book's first encounter with a Java idiom that an older pattern needed and a
newer one retires outright: nobody writing a singleton in current Java
reaches for double-checked locking, because the holder idiom and the enum
form between them cover every shape that pattern used to serve.

## What a singleton costs everywhere else

`CarrierRateTable` earns its shape because the data behind it is read-only
and nothing in the system needs a second copy. Most collaborators are not
that lucky, and `WarehousePricingJob` shows what happens when the pattern
gets reached for anyway:

```java
public final class WarehousePricingJob {
    public Money quoteBaseRate(String carrierCode) {
        return CarrierRateTable.INSTANCE.baseRateFor(carrierCode);
    }
}
```

Nothing in `quoteBaseRate`'s signature, and nothing in this class's
constructor, reveals that pricing a shipment depends on
`CarrierRateTable`. A reader has to open the method body to discover it,
and `WarehousePricingJobTest` demonstrates the practical consequence
directly: there is no parameter anywhere to substitute a different rate
source, so the only thing a test can assert is that the job's answer
matches whatever the real, global table happens to contain right now. A
failure scenario, a withdrawn carrier, a promotional rate, a rate source
that is temporarily unavailable, has no seam in this class to exercise it
through. The dependency is real; it is just invisible everywhere a reader
or a test would normally look for it.

## The dependency-injection alternative

`ShippingQuoteService` needs the identical capability, a base rate for a
carrier code, and gets it the other way: through a constructor parameter
instead of a static accessor.

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

`ShippingQuoteService` depends on `RateLookup`, never on
`CarrierRateTable` or `HolderCarrierRateTable` by name, the same inversion
the dependency-inversion work earlier in this book trained for a
notification gateway. Because the dependency arrives as a parameter, a
caller controls what it points at: production code can hand it
`CarrierRateTable.INSTANCE` or `HolderCarrierRateTable.getInstance()`
unchanged, and a test can hand it anything that satisfies `RateLookup`,
including a Mockito double.

```java
@Test
void quoteMultipliesTheInjectedBaseRateByWeight() {
    when(rateLookup.baseRateFor("GROUND")).thenReturn(Money.ofCents(200));
    ShippingQuoteService service = new ShippingQuoteService(rateLookup);

    Money quote = service.quote("GROUND", 5.0);

    assertThat(quote).isEqualTo(Money.ofCents(1_000));
    verify(rateLookup).baseRateFor("GROUND");
}
```

The payoff is not just that this test is short. It is that
`ShippingQuoteServiceTest` goes on to run the identical class, unchanged,
against the enum singleton, the holder singleton, and a one-line lambda,
and every one of those runs passes with zero modification to
`ShippingQuoteService` itself. `WarehousePricingJob` cannot make that claim
about anything, because `WarehousePricingJob` has nothing a test, or a
different composition root, can point somewhere else. Singleton and
dependency injection are not competing answers to "how many instances
exist." `CarrierRateTable` and `HolderCarrierRateTable` already settled
that question correctly: exactly one. They are competing answers to a
different question, how does code that needs a shared capability get a
reference to it, and the constructor-injected answer stays substitutable
no matter how the force behind the capability changes later.

> **AI sidebar.** Ask an LLM pairing partner for "a singleton class for X"
> and it will reliably hand back a shape close to `CarrierRateTable` or
> `HolderCarrierRateTable`, because both are well-documented, idiomatic
> Java and common in training data. What it will not do unprompted is ask
> whether X is actually a good fit for Singleton in the first place. Check
> the forces before accepting the shape: is X read-only after it is built,
> and does anything in the codebase already need, or plausibly will need,
> to substitute a different X in a test or a different environment. If the
> answer to the second question is yes, the generated singleton is a
> maintenance cost wearing a familiar name, and `WarehousePricingJob`
> is what that cost looks like once a reviewer has to find the hidden
> dependency by reading every method body. A generated `getInstance()`
> accessor with no constructor parameter anywhere near it, for a type a
> test might plausibly need to fake, is the clearest sign the model reached
> for the familiar shape instead of the one the forces actually call for.

## When not to use it

Singleton is the right choice when every one of three forces holds at once: a
type must have exactly one instance for the life of the process, that
instance is effectively immutable once constructed, and no caller, present
or future, plausibly needs to substitute a different one. `CarrierRateTable`
clears that bar. It is loaded once, never mutated, and a second rate table
would only ever mean the first one was wrong, not that two valid tables
need to coexist.

Most collaborators clear at most one of those three forces, and that is
reason enough to default to dependency injection instead.
`WarehousePricingJob`'s coupling to `CarrierRateTable.INSTANCE` would have
cost nothing extra to express as a `RateLookup` constructor parameter, and
the one thing that coupling takes away, a test's ability to exercise a
failing or changed rate source, is exactly the thing a real pricing job
eventually needs to test. The question worth asking before writing
`INSTANCE` into any class is not "could this reasonably be a singleton" but
"does anything about this collaborator ever need to be substituted," and if
the answer is "probably, eventually," the constructor parameter is
the cheaper decision to make now rather than the expensive one to retrofit
later once three other classes have already learned to call `getInstance()`
directly.

A narrower trap sits next to that one: do not reach for Singleton merely to
avoid passing a parameter down through a few layers of calls. A long
parameter chain is a legitimate complaint about a method's shape, and the
fix for it is usually to pass the one object those layers share, or to
restructure the call chain, not to make that object globally reachable so
no layer has to mention it. A global point of access that exists only to
save a few constructor arguments pays the full cost this chapter describes,
hidden coupling and no test seam, for a benefit that a slightly shorter
parameter list does not justify.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this
loop. From the repository root, the aggregator picks up
`examples/14-singleton` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/14-singleton
mvn test
```

Either path runs the same 18 JUnit 5 cases, asserted with AssertJ,
including a reflection-based proof that the enum singleton rejects a forged
second instance, a 16-thread race against the holder idiom's
`getInstance()`, and a Mockito-based suite that substitutes a double for
`RateLookup` and then reruns the identical service against both production
singletons and a hand-written fake. Observed locally on Fedora with
Temurin 25:

```
Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see the enum singleton, the holder singleton, the hidden-dependency job,
and the DI-based service run side by side rather than just reading an
assertion that says they behave correctly, run the example's own demo
script:

```sh
examples/14-singleton/demo.sh
```

It prices the same `GROUND` shipment through all four:

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

The enum singleton, the holder singleton, and the hidden-dependency job all
agree on `$1.80` per kilogram for `GROUND`, because all three read from the
same shared `CarrierRates` table. The DI-based service's second line shows
the one thing none of the other three can do: swap its entire rate source
for a one-line fake with no change anywhere else.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original Singleton entry, including the intent statement `CarrierRateTable` satisfies.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 3 recommends the single-element enum as the best way to implement a singleton, covering the reflection- and serialization-safety guarantees this chapter's tests prove directly.
- Robert C. Martin, [*Agile Software Development: Principles, Patterns, and Practices*](https://www.pearson.com/store/p/agile-software-development-principles-patterns-and-practices/P200000003363/9780135974445) (Prentice Hall): the Dependency Inversion Principle `ShippingQuoteService` applies, covered in full in the dependency-inversion chapter earlier in this book.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
