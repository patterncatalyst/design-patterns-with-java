---
title: "Factory Method and Abstract Factory"
order: 12
layout: tutorial
sectionid: docs
part: "creating-objects-under-control"
description: "Two idiomatic Java 25 ways to keep object creation out of client code, taught against a regional shipping desk: an Abstract Factory that hands out a matching label formatter, rate table, and customs form per region, and a Factory Method that lets a shipment's category decide which inspection checklist runs inside a fixed sequence."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Name the force both patterns answer: client code that needs an object
  without needing to know, or name, the concrete class that produces it.
- Build an Abstract Factory that hands out a family of related products per
  region, and explain why building the family from one factory call is what
  keeps the family internally consistent.
- Resolve a concrete factory at runtime with a static factory method backed
  by a `Supplier<T>` registry, and say what that registry buys you over a
  hand-written `switch`.
- Build a Factory Method as a protected hook inside a fixed creator
  sequence, and tell it apart from Abstract Factory by what each one
  varies: one product per call versus a whole family per call.
- Recognize when either pattern is solving a problem a constructor call
  already solves, and say what to write instead.

## Why this matters

A shipment leaving a regional hub needs three documents that all have to
agree with each other: a shipping label in that region's own format, a rate
quote computed by that region's own pricing rule, and a customs declaration
filled out under that region's own compliance scheme. Mix a label from one
region with a customs declaration from another and the result is not a
smaller bug. It is a parcel a customs officer rejects at the border, carrying
paperwork for a jurisdiction it never entered.

That is a force: the code that hands a shipment to a
region should never have to know which region it is talking to, and
whichever three documents it gets back have to come from the same region
every time. Abstract Factory is the Gang of Four's name for resolving
exactly this force. A single factory call produces every member of a
related family at once, so nothing downstream of that call can end up
holding a mismatched set.

Factory Method answers a narrower version of the same underlying problem:
client code that needs one object without naming its concrete class. Before
a shipment can leave a hub it has to pass an inspection, and which checklist
that inspection runs depends on whether the shipment carries hazardous
materials or not. The sequence that runs the inspection never changes:
validate the shipment, run a checklist, judge the result from the
checklist's own log. Only the one step that decides which checklist to
build does, and that decision belongs to a subclass, not to an `if`
statement the sequence would otherwise have to carry.

{% include excalidraw.html name="12-factory-family-selection" caption="Figure 12.1: ShippingToolkits.forRegion() resolves one ShippingToolkit from a Region value; the chosen toolkit's whole family of LabelFormatter, RateTable, and CustomsForm reaches RegionalShippingDesk together, from one factory call." %}

## The domain: three regions, one shared desk

The example behind this chapter lives in `examples/12-factories`. A
`Shipment` carries a destination country, a weight, a declared value, and
the `Region` it departs from:

```java
public record Shipment(
        String destinationCountry,
        double weightKg,
        long declaredValueCents,
        Region region) {

    public Shipment {
        Objects.requireNonNull(destinationCountry, "destinationCountry");
        Objects.requireNonNull(region, "region");
        if (weightKg <= 0.0) {
            throw new IllegalArgumentException("weightKg must be positive: " + weightKg);
        }
        if (declaredValueCents < 0) {
            throw new IllegalArgumentException("declaredValueCents cannot be negative: " + declaredValueCents);
        }
    }
}
```

Three regions back this example: `NORTH_AMERICA`, `EUROPEAN_UNION`, and
`ASIA_PACIFIC`. Each one implements the same three roles with its own rules.
North America charges a flat base plus a per-kilogram rate and asks for a
USMCA certificate of origin. The European Union charges a weight-tiered rate
with a VAT handling fee folded in and asks for an EORI-number declaration.
Asia-Pacific prices by a declared-value tier and asks for an HS tariff-code
declaration. Neither region's pricing rule or paperwork format matters on
its own. What matters is that `RegionalShippingDesk`, the client this
chapter builds toward, never has to know any of the three exist.

## Abstract Factory: a family that must match

Three product interfaces describe what the desk needs, with no mention
anywhere of a specific region:

```java
public interface LabelFormatter {
    String formatLabel(Shipment shipment);
}

public interface RateTable {
    Money quote(Shipment shipment);
}

public interface CustomsForm {
    String declare(Shipment shipment);
}
```

`ShippingToolkit` is the Abstract Factory itself: one interface declaring
one creation method per product above, so a single implementation is a
promise that all three products it returns belong to the same region.

```java
public interface ShippingToolkit {
    LabelFormatter labelFormatter();
    RateTable rateTable();
    CustomsForm customsForm();
}
```

`NorthAmericaToolkit`, `EuropeanUnionToolkit`, and `AsiaPacificToolkit` each
implement all three methods, returning that region's own concrete products.
`EuropeanUnionToolkit.rateTable()` returns an implementation that folds a
VAT handling fee into a weight-tiered base; `EuropeanUnionToolkit.customsForm()`
returns one that writes an EORI-number declaration carrying the same hub
code the label formatter stamped on the label. The product classes
themselves stay package-private inside each toolkit's own file, because
nothing outside that file is ever meant to name them:

```java
public final class EuropeanUnionToolkit implements ShippingToolkit {
    private final String hubCode;

    public EuropeanUnionToolkit(String hubCode) {
        this.hubCode = hubCode;
    }

    @Override
    public RateTable rateTable() {
        return new EuropeanUnionRateTable();
    }

    private record EuropeanUnionRateTable() implements RateTable {
        private static final long TIER_BASE_CENTS = 480;
        private static final long PER_KG_CENTS = 260;
        private static final double VAT_HANDLING_RATE = 0.02;

        @Override
        public Money quote(Shipment shipment) {
            long tieredCents = TIER_BASE_CENTS + Math.round(PER_KG_CENTS * shipment.weightKg());
            long vatHandlingCents = Math.round(shipment.declaredValueCents() * VAT_HANDLING_RATE);
            return Money.ofCents(tieredCents + vatHandlingCents);
        }
    }

    // labelFormatter() and customsForm() follow the same shape.
}
```

A nested `record` works well here precisely because each product is
stateless once its hub code is fixed: no inheritance, no mutable fields, and
an `equals`/`hashCode` pair a test can call without writing either by hand.

## The client: depends only on the abstractions

`RegionalShippingDesk` is the object that uses a region's three products. Its
constructor builds all three from one factory in one call:

```java
public final class RegionalShippingDesk {
    private final LabelFormatter labelFormatter;
    private final RateTable rateTable;
    private final CustomsForm customsForm;

    public RegionalShippingDesk(ShippingToolkit toolkit) {
        Objects.requireNonNull(toolkit, "toolkit");
        this.labelFormatter = toolkit.labelFormatter();
        this.rateTable = toolkit.rateTable();
        this.customsForm = toolkit.customsForm();
    }

    public ProcessedShipment process(Shipment shipment) {
        return new ProcessedShipment(
                labelFormatter.formatLabel(shipment),
                rateTable.quote(shipment),
                customsForm.declare(shipment));
    }
}
```

That constructor is the one place the guarantee gets made. Nothing in
`RegionalShippingDesk` names `EuropeanUnionToolkit` or either of the other
two concrete toolkits, and nothing in `process()` can reach past the three
interface fields to ask which region produced them. A fourth region added
next quarter needs its own toolkit and its own three product classes, and
`RegionalShippingDesk` does not change at all, because it was never written
against a concrete region in the first place. The example's own test suite
checks this directly with a mock `ShippingToolkit`: the constructor calls
`labelFormatter()`, `rateTable()`, and `customsForm()` exactly once each,
and `process()` never calls any of them again, no matter how many shipments
it handles afterward. The family is fixed the moment the desk is built, not
reassembled on every call.

## A static factory method, backed by a Supplier registry

`ShippingToolkits.forRegion(Region)` is the static factory method a checkout
calls once per shipment, after reading which hub it departs from:

```java
public final class ShippingToolkits {

    private static final Map<Region, Supplier<ShippingToolkit>> TOOLKIT_SUPPLIERS =
            new EnumMap<>(Region.class);

    static {
        TOOLKIT_SUPPLIERS.put(Region.NORTH_AMERICA, () -> new NorthAmericaToolkit("hub-na-01"));
        TOOLKIT_SUPPLIERS.put(Region.EUROPEAN_UNION, () -> new EuropeanUnionToolkit("hub-eu-07"));
        TOOLKIT_SUPPLIERS.put(Region.ASIA_PACIFIC, () -> new AsiaPacificToolkit("hub-apac-14"));
    }

    public static ShippingToolkit forRegion(Region region) {
        Supplier<ShippingToolkit> supplier = TOOLKIT_SUPPLIERS.get(region);
        if (supplier == null) {
            throw new IllegalArgumentException("no toolkit registered for region: " + region);
        }
        return supplier.get();
    }
}
```

A `Map<Region, Supplier<ShippingToolkit>>` is a different
shape from a `Map<Region, ShippingToolkit>`, and the difference matters. The map holds a recipe for
building a toolkit, not a shared toolkit instance, so each call to
`forRegion` constructs a fresh one with its own hub code, and adding a
fourth region means adding one more map entry, never touching the method
body that resolves it. The same decision, which concrete type to build at
runtime, could just as well be written as a `switch` expression over the
closed set of three regions, which is the simpler shape worth reaching for
first when the set is small and fixed:

{% include codetabs.html langs="Switch expression|Supplier registry" %}

```java
// A switch expression is exhaustive over Region today: a fourth region
// added without a matching branch fails to compile. It reads top to
// bottom in one method, which is exactly right for three regions a
// single team owns.
public static ShippingToolkit forRegion(Region region) {
    return switch (region) {
        case NORTH_AMERICA -> new NorthAmericaToolkit("hub-na-01");
        case EUROPEAN_UNION -> new EuropeanUnionToolkit("hub-eu-07");
        case ASIA_PACIFIC -> new AsiaPacificToolkit("hub-apac-14");
    };
}
```
```java
// A Supplier<T> registry trades that compile-time exhaustiveness for a
// data structure another piece of code can extend. Registering a new
// region does not require editing this method's body, which matters once
// toolkits start arriving from a plugin or a configuration-driven module.
private static final Map<Region, Supplier<ShippingToolkit>> TOOLKIT_SUPPLIERS = new EnumMap<>(Region.class);

public static ShippingToolkit forRegion(Region region) {
    Supplier<ShippingToolkit> supplier = TOOLKIT_SUPPLIERS.get(region);
    if (supplier == null) {
        throw new IllegalArgumentException("no toolkit registered for region: " + region);
    }
    return supplier.get();
}
```

Both columns resolve the same three regions to the same concrete toolkits,
which the example's test suite checks by asserting that a toolkit looked up
by `Region.NORTH_AMERICA` and one looked up by the string `"north america"`
through `ShippingToolkits.forRegionName` quote an identical shipment
identically. Reach for the `switch` expression when the set of concrete
types is closed and small enough that one method body stays readable at a
glance. Reach for the `Supplier<T>` registry when the set can grow without
a matching edit to the method that resolves it, which is also the shape to
reach for when a toolkit's construction needs to stay lazy, deferred until
a region is requested rather than built eagerly for every region
whether or not a shipment ever uses it.

## Factory Method: one product, decided by shipment category

Factory Method solves a narrower problem than Abstract Factory, and the
inspection workflow in this same example shows the difference directly.
Before a shipment leaves a hub it has to pass an inspection, and which
checklist runs depends on whether the shipment is a standard parcel or a
hazardous-materials shipment. The sequence that runs the inspection never
changes: validate the shipment, run one checklist, judge pass or fail from
the checklist's own log.

```java
public abstract class ShipmentInspector {

    public final InspectionResult inspect(Shipment shipment) {
        Objects.requireNonNull(shipment, "shipment");

        InspectionChecklist checklist = createChecklist();
        List<String> log = checklist.run(shipment);
        boolean passed = log.stream().noneMatch(line -> line.contains("FAILED"));

        return new InspectionResult(passed, List.copyOf(log));
    }

    protected abstract InspectionChecklist createChecklist();
}
```

`createChecklist()` is the factory method: a single protected hook that
returns one product, not a family of three. `inspect()` is marked `final`
and calls that hook once, in the middle of a sequence it otherwise owns
outright. `StandardShipmentInspector` overrides `createChecklist()` to
return a `StandardInspectionChecklist`, which checks packaging integrity and
declared value and nothing more. `HazmatShipmentInspector` overrides the
same method to return a `HazmatInspectionChecklist`, which adds a placard
check, a segregation-group check, and a check against a short list of
destinations that embargo hazardous imports. Neither subclass touches
`inspect()`: the validation, the sequence, and the pass-or-fail judgment are
inherited unchanged, and the only thing that varies between a standard and
a hazmat inspection is which one product `createChecklist()` hands back.

`ShipmentInspectors.forCategory(ShipmentCategory)` is the static factory
method that resolves which inspector subclass a shipment category needs,
written as an exhaustive `switch` expression over the two-member
`ShipmentCategory` enum, the same idiom this chapter's region lookup used
in its simpler form:

```java
public static ShipmentInspector forCategory(ShipmentCategory category) {
    return switch (category) {
        case STANDARD -> new StandardShipmentInspector();
        case HAZMAT -> new HazmatShipmentInspector();
    };
}
```

That is the test worth applying whenever the two patterns in this chapter's
title seem interchangeable. Abstract Factory varies a whole family of
related products together, and the family is the unit a caller asks for.
Factory Method varies exactly one product inside a sequence that a base
class owns, and the sequence is the thing that stays fixed while a subclass
decides the one part that does not. `RegionalShippingDesk` needed a family
of three documents that all had to agree with each other; `ShipmentInspector`
needed a single decision point inside an otherwise unchanging inspection.
Reaching for Abstract Factory when only one product varies adds two unused
creation methods to an interface nobody calls; reaching for Factory Method
when three related products need to agree with each other forces a caller
to make three separate calls and trust that it got them all from the same
place, which is exactly the trust Abstract Factory's single call exists to
remove.

> **AI sidebar.** Ask an LLM pairing partner to "add a factory" for a
> fourth region or a third shipment category, and a common result is a
> branch added to an existing `if`/`else if` chain or `switch` inside the
> one method that already resolves every other region, with the new
> region's rate logic and customs format written inline right there
> alongside the others. Check the result against the distinction this
> chapter trained: did the model add a new concrete `ShippingToolkit`
> implementing the existing interface, leaving `RegionalShippingDesk` and
> every other toolkit's code untouched, or did it add a branch to
> `forRegion` that now carries a fourth region's pricing rule as an inline
> block nobody can test in isolation? A new `NorthAmericaToolkit`-shaped
> class registered in the `Supplier<T>` map is a correct, idiomatic
> extension. The same branching habit showing up inside
> `RegionalShippingDesk` itself, naming a concrete region's toolkit
> directly instead of depending on `ShippingToolkit`, is the sign the model
> reached past the abstraction this chapter built and back toward the
> thing Abstract Factory exists to remove.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this
loop. From the repository root, the aggregator picks up
`examples/12-factories` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/12-factories
mvn test
```

Either path runs the same 30 JUnit 5 cases, asserted with AssertJ and one
Mockito suite that verifies `RegionalShippingDesk` builds its three products
from exactly one `ShippingToolkit` call, made once in the constructor:

```
Tests run: 30, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see both patterns produce real output rather than just reading an
assertion that says they work, run the example's own demo script:

```sh
examples/12-factories/demo.sh
```

It processes a shipment through two regional toolkits, looks a toolkit up
by a configuration string, runs both inspection categories, and finally
rejects an unrecognized region name:

```
== Abstract Factory: two regional toolkits, one client ==
North America:
  label:    NA-LABEL hub=hub-na-01 dest=Canada wt=4.2kg
  rate:     $15.32
  customs:  USMCA-CERT hub=hub-na-01 dest=Canada declaredValue=18000
European Union:
  label:    EU-LABEL hub:hub-eu-07 to:Germany weight:4.2
  rate:     $19.32
  customs:  EORI-DECLARATION hub:hub-eu-07 dest:Germany value:18000

== Abstract Factory: region chosen by name from configuration ==
Looked up ("north america"):
  label:    NA-LABEL hub=hub-na-01 dest=Canada wt=4.2kg
  rate:     $15.32

== Factory Method: shipment category decides the checklist ==
Standard inspection (PASSED):
  verified declared value is within the standard handling limit
  confirmed packaging integrity for a 4.2kg parcel
Hazmat inspection to Japan (PASSED):
  verified hazardous materials placard is attached and legible
  confirmed segregation group compatibility for a 12.0kg hazardous parcel
  confirmed destination accepts hazardous imports
Hazmat inspection to North Korea (FAILED):
  verified hazardous materials placard is attached and legible
  confirmed segregation group compatibility for a 12.0kg hazardous parcel
  FAILED: destination embargoes hazardous imports

== Abstract Factory: unrecognized region name rejected ==
Rejected unknown region: unknown region name: mars-colony
```

The toolkit looked up by name, `"north america"`, quotes the
identical $15.32 rate and prints the identical label as the toolkit built
directly from `Region.NORTH_AMERICA`. That agreement is
`RegionalShippingDesk` depending only on the three product interfaces
showing up as an observable fact at the terminal, however a
`ShippingToolkit` reached the desk's constructor.

## When not to use Factory Method or Abstract Factory

Both patterns earn their interface when there is more than one concrete
class in hand, or clearly coming, and when a caller must not name the
concrete type itself. Outside that case, both add a layer with nothing
underneath it.

A single region with no second one on any roadmap does not need
`ShippingToolkit`, three product interfaces, and a one-implementation
hierarchy under each. It needs a `LabelFormatter`, a `RateTable`, and a
`CustomsForm` constructed directly in `RegionalShippingDesk`'s own
constructor, with no interface standing between them and their one caller.
Introducing the full Abstract Factory shape ahead of a second region
arriving adds three abstract product interfaces, a lookup method, and a
registry, all defending against mismatched regional paperwork that cannot
happen yet because there is nothing to mismatch.

Factory Method fails the same test in a narrower way. An inspection with
exactly one way to run its one varying step does not need a base class and
a protected hook; it needs a method that runs the check inline. The moment
to introduce `createChecklist()` as a factory method is the moment a second
category, not a second format of the same thing, needs a different product
at that one point in an otherwise identical sequence. A single
`InspectionChecklist` with no standard-versus-hazmat distinction anywhere
in the business rules would make `ShipmentInspector`'s hierarchy pure
overhead: one base class, one subclass, one override, standing in for what
could be a single method with no decision to make.

The sharper trap is reaching for Abstract Factory when Factory Method would
do, or the reverse. A three-method interface where two of the three methods
have exactly one caller and no second implementation anywhere in the
codebase is the Abstract Factory shape used to solve a Factory Method
problem: only one product in that family varies, and the other two methods
exist purely because the interface declares them. The fix is not to delete
the interface. It is to identify which force is in play, a single varying
product inside a fixed sequence or a whole family that must stay
consistent, and reach for the narrower pattern that names it.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design
  Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480)
  (Addison-Wesley): the original Factory Method and Abstract Factory
  entries, including the "family of related or dependent objects" framing
  this chapter's three regional toolkits build on.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991),
  third edition (Addison-Wesley): Item 1 covers static factory methods as
  an alternative to a public constructor, the idiom behind
  `ShippingToolkits.forRegion` and `ShipmentInspectors.forCategory`.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/),
  [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html),
  and [AssertJ documentation](https://assertj.github.io/doc/): the testing
  idiom behind `examples/12-factories`'s suite, which verifies a factory
  returns the right family and that a client builds it once, not just that
  the build exits zero.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
