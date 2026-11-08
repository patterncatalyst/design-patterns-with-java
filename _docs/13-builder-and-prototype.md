---
title: "Builder and Prototype"
order: 13
layout: tutorial
sectionid: docs
part: "creating-objects-under-control"
description: "A Bloch-style Builder that assembles an international shipment and validates it only once, at build(), a record with wither methods for deriving a changed customs declaration, and a Prototype catalog that clones a named packaging exemplar into an independent copy, taught against a shipping domain."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Say when required fields, optional fields with defaults and a cross-field invariant call for a Builder.
- Build a static nested `Builder` with a fluent interface that validates only in `build()`.
- Validate a record in its compact constructor and derive a changed copy with a wither method.
- Build a catalog that clones a named exemplar into an independent copy, and say what the copy constructor must do for that to hold.
- Recognize when a constructor or a record is enough.

## Why this matters

A dispatcher assembling an international shipment fills in three kinds of field. A tracking identifier, a carrier and a destination are required. Priority, insurance and a signature requirement are optional, with defaults most callers keep. And one rule spans the whole shipment: a shipment leaving the domestic market needs a customs declaration.

One constructor can't express that readably. A constructor taking every field makes callers pass values they don't care about in an order nobody remembers, and an overload per combination of optional fields grows with every new field.

On the packaging side, a warehouse reuses a few package kinds constantly: fragile electronics, bulk dry goods, refrigerated perishables, each with its own default weight, handling code and protective materials. Writing them out at every call site copies the same defaults everywhere, and a change to one default has to be found in all of them.

{% include excalidraw.html name="13-builder-and-prototype" caption="Figure 13.1: Shipment.Builder accumulates required and optional fields and validates every invariant once, at build(); PackagingCatalog clones a named PackagingTemplate exemplar into an independent copy." %}

## Pattern: Builder

### The problem

`Shipment` has three required header fields, several optional ones with defaults, and a customs declaration present only for an international destination:

```java
public final class Shipment {

    public static final String DOMESTIC_COUNTRY = "US";

    private final String trackingId;
    private final String carrier;
    private final String destinationCountry;
    private final ShippingPriority priority;
    private final long insuredValueCents;
    private final boolean signatureRequired;
    private final CustomsDeclaration customsDeclaration;
    private final String specialHandlingNotes;

    private Shipment(Builder builder) {
        this.trackingId = builder.trackingId;
        this.carrier = builder.carrier;
        this.destinationCountry = builder.destinationCountry;
        this.priority = builder.priority;
        this.insuredValueCents = builder.insuredValueCents;
        this.signatureRequired = builder.signatureRequired;
        this.customsDeclaration = builder.customsDeclaration;
        this.specialHandlingNotes = builder.specialHandlingNotes;
    }

    public Optional<CustomsDeclaration> customsDeclaration() {
        return Optional.ofNullable(customsDeclaration);
    }

    // trackingId(), carrier(), priority(), and the rest follow the same shape
}
```

Eight constructor parameters, five of them usually defaulted, invite swapped arguments and repeated defaults at every call site.

### The solution

A **Builder** accumulates an object's fields one call at a time, applies defaults for what the caller skips, and checks the invariants once, when the object is produced.

`Shipment` has no public constructor, so every instance comes from `build()` and already satisfies its invariants. Insured value is integer cents, so it compares exactly. The nullable declaration is exposed as `Optional<CustomsDeclaration>` on the accessor, not as the field's type: the `Optional` Javadoc describes it as intended primarily as a method return type.

### How Java models it

`Shipment.Builder` is a static nested class, Joshua Bloch's shape, with a chained setter per field:

```java
public static final class Builder {
    private String trackingId;
    private String carrier;
    private String destinationCountry;
    private ShippingPriority priority = ShippingPriority.STANDARD;
    private long insuredValueCents = 0L;
    private boolean signatureRequired = false;
    private CustomsDeclaration customsDeclaration;
    private String specialHandlingNotes = "";

    public Builder trackingId(String trackingId) {
        this.trackingId = trackingId;
        return this;
    }

    public Builder carrier(String carrier) {
        this.carrier = carrier;
        return this;
    }

    // destinationCountry(), priority(), insuredValueCents(),
    // signatureRequired(), customsDeclaration(), and specialHandlingNotes()
    // follow the same shape, each returning this

    public Shipment build() {
        String error = validationError();
        if (error != null) {
            throw new IllegalStateException(error);
        }
        return new Shipment(this);
    }
}
```

Each setter returns `this`, so calls chain in any order:

```java
Shipment shipment = Shipment.builder()
        .trackingId("SHIP-4473")
        .carrier("Northline Freight")
        .destinationCountry("CA")
        .customsDeclaration(declaration)
        .build();
```

### Validate once, at build()

No setter validates. A half-built shipment is unfinished, not invalid, and a setter that checks another field has been set brings back the fixed parameter order Builder removes. `build()` is the one moment the caller says the shipment is complete:

```java
private String validationError() {
    if (trackingId == null || trackingId.isBlank()) {
        return "Shipment.Builder: trackingId is required";
    }
    if (carrier == null || carrier.isBlank()) {
        return "Shipment.Builder: carrier is required";
    }
    if (destinationCountry == null || destinationCountry.isBlank()) {
        return "Shipment.Builder: destinationCountry is required";
    }
    boolean international = !DOMESTIC_COUNTRY.equals(destinationCountry);
    if (international && customsDeclaration == null) {
        return "Shipment.Builder: customsDeclaration is required for an international destination ("
                + destinationCountry + ")";
    }
    return null;
}
```

Every rule lives in one method, so "a `Shipment` from `build()` is complete" has one place to fail. A `"CA"` shipment with no declaration fails at `build()`, with a message naming the missing field and the destination that needs it.

### The record and its wither

`CustomsDeclaration` has four fields, all required: a harmonized tariff code, an origin country, a contents description and a declared value. A record's compact constructor validates them once, the same discipline `build()` applies:

```java
public record CustomsDeclaration(
        String harmonizedCode,
        String originCountry,
        String contentsDescription,
        long declaredValueCents) {

    public CustomsDeclaration {
        Objects.requireNonNull(harmonizedCode, "harmonizedCode");
        Objects.requireNonNull(originCountry, "originCountry");
        Objects.requireNonNull(contentsDescription, "contentsDescription");
        if (harmonizedCode.isBlank()) {
            throw new IllegalArgumentException("CustomsDeclaration: harmonizedCode cannot be blank");
        }
        if (declaredValueCents < 0) {
            throw new IllegalArgumentException(
                    "CustomsDeclaration: declaredValueCents cannot be negative: " + declaredValueCents);
        }
    }

    public CustomsDeclaration withDeclaredValueCents(long newDeclaredValueCents) {
        return new CustomsDeclaration(harmonizedCode, originCountry, contentsDescription, newDeclaredValueCents);
    }
}
```

A record has no setters. `withDeclaredValueCents` returns a new instance with one field changed. After a customs audit, `declaration.withDeclaredValueCents(29_900L)` gives the revised declaration and leaves the original intact.

| Shape | Use when |
| --- | --- |
| Builder | The object is assembled gradually from many optional pieces, with a rule that spans several of them. |
| Record with a wither | The object is complete at construction and callers need one-field variations of it. |

## Pattern: Prototype

### The problem

Each package kind has the same defaults every time it's used. Building one from scratch at each call site repeats those defaults, and nothing ties the copies back to one definition.

### The solution

A **Prototype** keeps one configured exemplar of each kind and produces new instances by copying it.

### How Java models it

`PackagingTemplate` copies itself through a private copy constructor:

```java
public final class PackagingTemplate {
    private final String kind;
    private double weightKg;
    private String handlingCode;
    private final List<String> protectiveMaterials;

    private PackagingTemplate(PackagingTemplate source) {
        this.kind = source.kind;
        this.weightKg = source.weightKg;
        this.handlingCode = source.handlingCode;
        this.protectiveMaterials = new ArrayList<>(source.protectiveMaterials);
    }

    public PackagingTemplate copy() {
        return new PackagingTemplate(this);
    }
}
```

The copy's independence comes from the copy constructor, not from Prototype itself. `kind`, `weightKg` and `handlingCode` are values, so assigning them is enough. `protectiveMaterials` is a mutable list, so the constructor copies it into a new `ArrayList`. Assign the reference instead and every clone shares one list: a material added to one package appears on every package cloned from the same exemplar, and on the exemplar.

`PackagingCatalog` is a named registry of exemplars:

```java
public final class PackagingCatalog {
    private final Map<String, PackagingTemplate> templates = new HashMap<>();

    public void register(String name, PackagingTemplate exemplar) {
        templates.put(name, exemplar);
    }

    public PackagingTemplate create(String name) {
        PackagingTemplate exemplar = templates.get(name);
        if (exemplar == null) {
            throw new NoSuchElementException("PackagingCatalog: no template registered for " + name);
        }
        return exemplar.copy();
    }
}
```

`create("fragile-electronics")` returns a copy; the registered exemplar is never handed out. Change one clone's weight or materials and the exemplar, earlier clones and later clones keep the original defaults.

## When not to use it

| Situation | Write instead |
| --- | --- |
| Few fields, all required, no optional ones | A record with a compact constructor, as `CustomsDeclaration` is. |
| A type complete at construction that needs one-field changes | A record with a wither or two. |
| One package kind, or packages that share nothing | A plain class filled in at each call site. |
| Cheap construction and few shared defaults | A constructor. `PackagingTemplate` is four fields and a short list; Prototype pays here only for the defaults it centralizes. |

> **🤖 Explore with your agent**
>
> "In `examples/13-builder-prototype`, add an optional field to `Shipment` through `Shipment.Builder`: a setter that returns `this` and checks nothing, a default in the builder's field, and any rule that spans fields in `validationError()`, run only from `build()`. Keep `Shipment`'s constructor private and expose a nullable field as `Optional` on the accessor only. Derive changed `CustomsDeclaration` values with wither methods, never setters. If you add a mutable collection to `PackagingTemplate`, copy it into a new collection in the copy constructor so `PackagingCatalog.create()` returns a fully independent copy."
>
> Check: `mvn -q -f examples/13-builder-prototype/pom.xml test -Dtest=ShipmentBuilderTest,PackagingCatalogTest`; a shared list or a skipped customs check fails it.

## Build, run, observe

From the repository root, or standalone from `examples/13-builder-prototype`:

```sh
mvn -f examples/pom.xml test
```

```sh
cd examples/13-builder-prototype
mvn test
```

All 13 tests pass. To run the builder, the wither and the catalog:

```sh
examples/13-builder-prototype/demo.sh
```

```
== Builder: assembling a domestic shipment ==
SHIP-4471 via Northline Freight to US [EXPEDITED], insured for $0.00

== Builder: rejecting an international shipment with no customs declaration ==
Rejected: Shipment.Builder: customsDeclaration is required for an international destination (CA)

== Builder: a valid international shipment with a customs declaration ==
SHIP-4473 via Northline Freight to CA [STANDARD], customs declared, insured for $0.00

== Record wither: deriving a changed CustomsDeclaration ==
Original:  CustomsDeclaration[harmonizedCode=8517.12, originCountry=US, contentsDescription=Wireless router, declaredValueCents=24900]
Revalued:  CustomsDeclaration[harmonizedCode=8517.12, originCountry=US, contentsDescription=Wireless router, declaredValueCents=29900]

== Prototype: cloning independent packages from one catalog ==
Before mutation: both copies report 1.2 kg
After mutating only the first copy: first=1.6 kg, second=1.2 kg
First materials:  [bubble-wrap, foam-corner, anti-static-bag]
Second materials: [bubble-wrap, foam-corner]
```

The second copy keeps 1.2 kg and its two original materials after the first is changed. Two experiments, each in a scratch copy:

- **Share the list.** In `PackagingTemplate`'s copy constructor, assign `source.protectiveMaterials` instead of copying it. Both catalog independence checks fail.
- **Drop the cross-field rule.** Disable the international customs check in `validationError()`. The check that a `"CA"` shipment without a declaration is rejected fails.

## What you can do now

- Build a complex object with a static nested `Builder` that validates only in `build()`.
- Keep a constructor private so every instance passes through validation.
- Validate a record in its compact constructor and derive changes with withers.
- Clone exemplars from a `PackagingCatalog` with a copy constructor that copies mutable state.
- Choose a record or a constructor when the object is simple.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original Builder and Prototype entries, including the "director" variant of Builder that a single caller-driven fluent chain replaces.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 2 covers the static nested Builder class `Shipment.Builder` follows, including the validate-at-build discipline.
- The [`Optional` Javadoc](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Optional.html): its API note on `Optional` as a method return type.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/13-builder-prototype`.
- The full reading list lives on the [bibliography page]({{ '/bibliography/' | relative_url }}).
