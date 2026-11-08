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

- Say when client code needs an object without naming its concrete class.
- Build an Abstract Factory that hands out a matching family of products per region from one call.
- Resolve a concrete factory at runtime with a static factory method, backed by a `switch` expression or a `Supplier<T>` registry.
- Build a Factory Method as a protected hook inside a fixed creator sequence, and tell it from Abstract Factory by what each varies.
- Recognize when a constructor call is enough.

## Why this matters

A shipment leaving a regional hub needs three documents that agree: a label in the region's format, a rate from the region's pricing rule, and a customs declaration under the region's compliance scheme. A label from one region with a customs form from another is a parcel rejected at the border.

Before it leaves, the shipment also passes an inspection whose checklist depends on whether it carries hazardous materials. The inspection sequence never changes; only the checklist does.

{% include excalidraw.html name="12-factory-family-selection" caption="Figure 12.1: ShippingToolkits.forRegion() resolves one ShippingToolkit from a Region value; the chosen toolkit's whole family of LabelFormatter, RateTable, and CustomsForm reaches RegionalShippingDesk together, from one factory call." %}

A `Shipment` carries a destination country, a weight, a declared value and the `Region` it departs from:

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

| Region | Rate | Customs |
| --- | --- | --- |
| `NORTH_AMERICA` | Flat base plus per kilogram | USMCA certificate of origin |
| `EUROPEAN_UNION` | Weight-tiered plus a VAT handling fee | EORI-number declaration |
| `ASIA_PACIFIC` | Declared-value tier | HS tariff-code declaration |

## Pattern: Abstract Factory

### The problem

`RegionalShippingDesk` needs a label formatter, a rate table and a customs form. If it builds each one with its own region check, nothing stops it from holding a mismatched set, and every new region means editing the desk.

### The solution

An **Abstract Factory** creates a whole family of related products from one object, so a client holding one factory can't end up with products from different families.

### How Java models it

Three product interfaces name no region:

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

`ShippingToolkit` is the factory, one creation method per product. An implementation promises that all three belong to one region:

```java
public interface ShippingToolkit {
    LabelFormatter labelFormatter();
    RateTable rateTable();
    CustomsForm customsForm();
}
```

`NorthAmericaToolkit`, `EuropeanUnionToolkit` and `AsiaPacificToolkit` each return their region's products. The products are `private` records nested in each toolkit, because nothing outside it should name them. The EU label and customs form carry the same hub code:

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

A record suits a product that's stateless once its hub code is fixed: no inheritance, no mutable fields, and generated `equals` and `hashCode`.

### The client

`RegionalShippingDesk` builds all three products from one toolkit in its constructor:

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

The constructor is the one place the guarantee is made. It calls each creation method exactly once, and `process()` never goes back to the factory, so the family is fixed when the desk is built. The desk names no concrete toolkit; a fourth region is a new toolkit with its own three products, and the desk doesn't change.

### Choosing the toolkit at runtime

`ShippingToolkits.forRegion(Region)` is the static factory method checkout calls after reading the departure hub:

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

The map holds a recipe, not a shared instance, so each call builds a fresh toolkit, and a toolkit is built only when its region is asked for. `ShippingToolkits.forRegionName` resolves a configuration string such as `"north america"` to the same toolkit. For a small closed set, a `switch` expression is the simpler first choice:

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

| Use | When |
| --- | --- |
| `switch` expression | The set is closed and small, one team owns it, and you want the compiler to catch a missing case. |
| `Supplier<T>` registry | The set grows without editing the lookup, for example from a plugin or configuration-driven module. |

## Pattern: Factory Method

### The problem

Every inspection validates the shipment, runs one checklist and judges pass or fail from the checklist's log. A standard parcel needs packaging and declared-value checks. A hazardous shipment adds a placard check, a segregation-group check and a check against destinations that embargo hazardous imports. An `if` on the category inside the sequence would tie every new category to an edit of the sequence.

### The solution

A **Factory Method** is a hook in a fixed sequence that a subclass overrides to decide which one product the sequence uses.

### How Java models it

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

`createChecklist()` returns one product. `inspect()` is `final` and calls it once. `StandardShipmentInspector` returns a `StandardInspectionChecklist`; `HazmatShipmentInspector` returns a `HazmatInspectionChecklist`. Neither touches `inspect()`. `ShipmentInspectors.forCategory` picks the subclass with an exhaustive `switch` over the two-member `ShipmentCategory`:

```java
public static ShipmentInspector forCategory(ShipmentCategory category) {
    return switch (category) {
        case STANDARD -> new StandardShipmentInspector();
        case HAZMAT -> new HazmatShipmentInspector();
    };
}
```

### Telling the two apart

| | Abstract Factory | Factory Method |
| --- | --- | --- |
| Varies | A family of related products | One product |
| Lives in | An interface the client is handed | A protected hook in a base class |
| Fixed part | The client's use of the family | The base class's sequence |
| Example | `ShippingToolkit` | `ShipmentInspector.createChecklist()` |

Abstract Factory where one product varies leaves creation methods nobody needs. Factory Method where three products must agree makes the caller make three calls and trust they came from one place, the trust one Abstract Factory call removes.

## When not to use it

| Situation | Write instead |
| --- | --- |
| One region, none coming | Construct the label formatter, rate table and customs form directly in `RegionalShippingDesk`. |
| One way to run the inspection | A method that runs the check inline; add `createChecklist()` when a second category needs a different product at that point. |
| A three-method factory where two methods have one caller and one implementation | A Factory Method: only one product varies. |

> **🤖 Explore with your agent**
>
> "In `examples/12-factories`, add a fourth region as a new `ShippingToolkit` implementation with its own `LabelFormatter`, `RateTable` and `CustomsForm` as private nested records, registered in `ShippingToolkits`' `Supplier<ShippingToolkit>` map. Don't add a branch with inline pricing or customs logic to `forRegion`, and don't change `RegionalShippingDesk`: it must still depend only on `ShippingToolkit` and call each creation method once, in its constructor. Add any new inspection category as a `ShipmentInspector` subclass that overrides only `createChecklist()`, leaving `inspect()` `final`."
>
> Check: `mvn -q -f examples/12-factories/pom.xml test -Dtest=RegionalShippingDeskTest,ShippingToolkitsTest`, and confirm `RegionalShippingDesk` names no concrete toolkit.

## Build, run, observe

From the repository root, or standalone from `examples/12-factories`:

```sh
mvn -f examples/pom.xml test
```

```sh
cd examples/12-factories
mvn test
```

All 30 tests pass. To run both patterns:

```sh
examples/12-factories/demo.sh
```

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

The toolkit looked up by name prints the same label and $15.32 rate as the one built from `Region.NORTH_AMERICA`. Two experiments, each in a scratch copy:

- **Go back to the factory.** Add a second `toolkit.rateTable()` call to `RegionalShippingDesk`'s constructor. The two checks that the desk calls each creation method once fail.
- **Return the wrong product.** Make `HazmatShipmentInspector.createChecklist()` return a `StandardInspectionChecklist`. Both hazmat inspection checks fail, including the embargoed-destination one.

## What you can do now

- Hand a client one `ShippingToolkit` so its products always match.
- Keep products as private nested records that only their toolkit names.
- Resolve a factory with a `switch` for a closed set or a `Supplier<T>` registry for a growing one.
- Vary one product inside a `final` sequence with a protected factory method.
- Use a constructor when there's only one concrete class.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design
  Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480)
  (Addison-Wesley): the original Factory Method and Abstract Factory
  entries, including the "family of related or dependent objects" framing
  the three regional toolkits build on.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991),
  third edition (Addison-Wesley): Item 1 covers static factory methods as
  an alternative to a public constructor, the idiom behind
  `ShippingToolkits.forRegion` and `ShipmentInspectors.forCategory`.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/),
  [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html),
  and [AssertJ documentation](https://assertj.github.io/doc/): the testing
  idiom behind `examples/12-factories`.
- The full reading list lives on the [bibliography page]({{ '/bibliography/' | relative_url }}).
