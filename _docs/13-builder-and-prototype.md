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

- Name the force Builder answers: a product has several required fields,
  several optional ones with sensible defaults, and an invariant that only
  holds once every field is in place, and no single constructor signature
  expresses all three cleanly.
- Build a static nested `Builder` class with a fluent interface, and explain
  why validation belongs only inside `build()`, not scattered across the
  individual setters.
- Build a record with a compact canonical constructor for its required
  fields, and derive a changed copy with a wither method rather than
  mutating the original.
- Name the force Prototype answers: a handful of pre-configured object kinds
  get reused constantly, and reconstructing one from scratch every time
  repeats the same initialization logic for no benefit.
- Build a small catalog that clones a named prototype into a fresh,
  independently owned copy, and say what a copy constructor has to do for
  that independence to hold.
- Recognize when a Builder or a Prototype is solving a problem a plain
  constructor or a record already solves, and say what to write instead.

## Why this matters

A dispatcher assembling an international shipment is filling in three
different kinds of field at once. Some are non-negotiable: a shipment
without a tracking identifier or a carrier is not a shipment, it is a typo
waiting to be caught. Some are optional with a default a dispatcher rarely
overrides: most shipments move at standard priority with no declared
insurance, and requiring every caller to type that out would be noise, not
information. One more constraint holds across the whole shipment regardless
of which fields a caller set: a shipment headed outside the domestic market
without a customs declaration is not valid, no matter how carefully every
other field was filled in.

A single constructor cannot express all three of those forces at once
without becoming unreadable. A constructor that takes every field, required
and optional, forces every caller to pass values for fields they do not
care about, in a parameter order nobody will remember six months from now.
A constructor overload for every combination of optional fields a caller
might want to set is combinatorial, and it grows every time this domain
adds one more optional field. Builder is the Gang of Four's name for
resolving this: a separate object accumulates field values one call at a
time through a fluent interface, applies the defaults for whatever the
caller skipped, and checks the one cross-field invariant exactly once, at
the single moment the product is produced.

A related but distinct force shows up on the packaging side of the same
warehouse. A shipping operation does not invent a new package shape for
every order. A handful of kinds, fragile electronics, bulk dry goods,
refrigerated perishables, come up constantly, each with its own default
weight, handling code, and set of protective materials. Writing out a new
package by hand at every call site duplicates the same defaults dozens of
times over, and a change to one default has to be hunted down everywhere it
was copied. Prototype is the Gang of Four's name for resolving this
instead: keep one configured example of each kind, an exemplar, and get a
new instance by copying it rather than by re-deriving its defaults from
scratch.

Java 25 gives this chapter's Builder a natural neighbor. A record already
expresses "required fields, validated once, at construction" for a value
that never changes once built, through its compact canonical constructor.
When part of that value needs to change, a wither method, a small method
that returns a new instance with one field different, derives the changed
copy without ever mutating the original. Builder and the record-plus-wither
pair are solving adjacent problems: Builder assembles a complex object
gradually from many optional pieces, and a wither derives a focused,
one-field variation of an object that is already complete.

{% include excalidraw.html name="13-builder-and-prototype" caption="Figure 13.1: Shipment.Builder accumulates required and optional fields and validates every invariant once, at build(); PackagingCatalog clones a named PackagingTemplate exemplar into an independent copy." %}

## The domain: assembling an international shipment

The example behind this chapter lives in `examples/13-builder-prototype`. A
`Shipment` carries three required header fields, several optional ones
already holding their defaults, and an `Optional<CustomsDeclaration>` that
is only ever present for a shipment leaving the domestic market:

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

`Shipment` has no public constructor. The only way to get an instance is
through `Shipment.builder()...build()`, which is what lets this class
guarantee that every `Shipment` a caller ever holds already satisfies its
invariants. Money is tracked as integer `insuredValueCents`, so a declared
value compares exactly in a test assertion with no floating-point epsilon
anywhere. The nullable `customsDeclaration` field is exposed through
`Optional<CustomsDeclaration>` on the accessor rather than as the field's
own type, because a record or class field holding `Optional` is the one
place that type is discouraged; wrapping only the return value keeps the
nullability visible to callers without paying that cost internally.

## Builder: assembling the shipment through a fluent interface

`Shipment.Builder` is a static nested class, Joshua Bloch's shape for this
pattern, holding the same fields as `Shipment` itself plus a chained setter
for each one:

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

Every setter returns `this`, which is what lets a caller chain calls in
whatever order is convenient rather than filling in a fixed parameter list:

```java
Shipment shipment = Shipment.builder()
        .trackingId("SHIP-4473")
        .carrier("Northline Freight")
        .destinationCountry("CA")
        .customsDeclaration(declaration)
        .build();
```

Nothing is validated as the chain runs. `priority()` does not check that
`trackingId()` was called first, and `customsDeclaration()` does not check
that the shipment already has a carrier. The validation step belongs only
at `build()`, not scattered across the setters, because a half-built
shipment is not finished yet, not an invalid one. `build()` is the single
moment a caller declares the shipment complete:

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

Checking every required field and the one cross-field invariant in one
method, run exactly once, means the claim "a `Shipment` leaving `build()`
is always complete" has exactly one place it could fail to hold, and the
test suite only has to exercise that one method to prove the claim. Leaving
`customsDeclaration()` unset for a `"CA"` destination does not fail where a
reader might expect it to, at the `customsDeclaration()` call or at
`destinationCountry()`; it fails at `build()`, with a message that names
both the missing field and the destination that required it.

## The record and its wither: the modern complement

`CustomsDeclaration` holds the paperwork a customs officer checks: a
harmonized tariff code, an origin country, a contents description, and a
declared value for duty assessment. Every field here is required, and a
record's compact canonical constructor is where that gets checked, the same
validate-once discipline `Shipment.Builder` applies to the shipment that
carries this declaration:

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

A record has no setters, so there is no way to mutate `declaredValueCents`
on an existing `CustomsDeclaration` even if a caller wanted to. Instead,
`withDeclaredValueCents` returns a brand new instance with one field
changed and every other field carried over unchanged. This is not a smaller
version of Builder; it answers a different force entirely. Builder exists
because a `Shipment` is assembled gradually, from a dozen optional pieces,
across several calls before it is complete. A wither exists because a
`CustomsDeclaration` is already complete, and the only thing a caller wants
is a focused variation of it, one field different, with the rest untouched.
Reappraising a shipment's declared value after a customs audit is exactly
that: `declaration.withDeclaredValueCents(29_900L)` produces the revised
declaration without disturbing the original, which a test can still hold a
reference to and compare against.

## Prototype: cloning a pre-configured package

A `PackagingTemplate` is a configured exemplar: a kind of package a
warehouse reuses constantly, each with its own default weight, handling
code, and list of protective materials:

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

`copy()` is one line, but the reason it produces a full, independent copy
comes from the copy constructor behind it, not from anything Prototype
guarantees on its own. `kind`, `weightKg`, and `handlingCode` are plain
values, so assigning them across copies is already independent. The
`protectiveMaterials` list is the one field that needs attention:
`new ArrayList<>(source.protectiveMaterials)` builds a brand new list and
copies the elements into it, rather than assigning the same list reference
to both the source and the copy. Skip that one line and every clone would
share the same underlying list, and adding a protective material to one
package would silently show up on every other package cloned from the same
exemplar, and on the exemplar itself.

`PackagingCatalog` is the small, named registry a dispatcher clones from:

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

`create("fragile-electronics")` calls `copy()` on the registered exemplar
and hands the caller the result. The catalog's own copy of that exemplar is
never handed out and never mutated; every call to `create()` with the same
name returns a new, independent object. This example's test suite checks
exactly that claim directly: clone twice from the same name, mutate one
clone's weight and add a protective material to it, and assert that the
other clone, and a third fresh clone, both still report the exemplar's
original defaults. If `copy()` ever returned the same underlying instance
instead of a fresh one, that test would fail the moment the first clone was
mutated, which is the point of writing the assertion against the mutation
rather than against a clone's initial state alone.

> **AI sidebar.** Ask an LLM pairing partner for a Builder, and a common
> result validates inside every setter instead of once at `build()`,
> because "fail fast" sounds like good advice in isolation. Read that
> result the way this chapter trained: if `carrier()` throws whenever
> `trackingId()` has not been called yet, the builder has reintroduced the
> fixed-parameter-order problem Builder exists to remove, just spread
> across several methods instead of one constructor. The same generated
> code often skips a real copy constructor for a Prototype and has `copy()`
> return the same mutable list reference the exemplar already holds, which
> looks correct in a demo that never mutates the result and breaks the
> instant two callers do. Check specifically for that: does mutating one
> cloned instance leave every other clone, and the registered exemplar
> itself, unchanged? A generated catalog with no test exercising that
> question has not demonstrated it clones at all, only that it compiles.

## When not to use Builder or Prototype

Builder earns its fluent interface when a product has enough required and
optional fields, with at least one cross-field invariant, that a single
constructor signature would be unreadable or would force every caller to
repeat the same defaults. It does not earn that interface below that bar. A
`CustomsDeclaration` with four required fields and no optional ones does
not need a builder; it needs exactly what it already is, a record with a
compact canonical constructor, the same candidate-non-pattern move this
book has trained since its opening chapters. Introducing a builder ahead of
that complexity adds a class, a chain of methods a reader has to trace back
to a single `build()` call, and a runtime validation check standing in for
what the record's constructor already enforces directly.

Prototype earns its catalog when a handful of pre-configured kinds get
reused often enough that re-deriving their defaults at every call site is
real, visible duplication, and when those kinds are closed enough in number
that a small named catalog pays for its own upkeep. It does not earn a
catalog when there is exactly one kind of package, or when every order's
package differs enough from every other that there is nothing left to
clone. A single `Package` class with no registry, filled in directly at
each call site, serves that case better than a `PackagingTemplate` catalog
with one entry and nothing to clone from. Construction is also only worth
avoiding when it is actually expensive or actually repetitive; a
`PackagingTemplate` built from four plain fields and a short list costs so
little to construct directly that Prototype's only real justification here
is the defaults it centralizes, not the construction cost it avoids. The
test this book keeps returning to still applies: if cloning saves real,
repeated initialization work, Prototype is the right name; if there is
nothing common to clone, a plain constructor is the stronger engineering
choice even with Prototype's name sitting right there.

A related trap is reaching for Builder when what is needed instead is a
record with a wither. If a type is fully formed the moment it is
constructed, with no gradual assembly and no cross-field invariant that
spans several optional calls, a record with one or two wither methods for
the fields that do change is plainer to read than a fluent chain built and
discarded on one line. Builder's value is concentrated in the validation
step at `build()`; a type with nothing meaningful to validate across
several optional fields has already given most of that value away before
paying any of Builder's cost.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this
loop. From the repository root, the aggregator picks up
`examples/13-builder-prototype` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/13-builder-prototype
mvn test
```

Either path runs the same 13 JUnit 5 cases, asserted with AssertJ,
including a suite that checks the builder rejects an invalid combination
and builds a valid shipment, a suite that checks a wither produces an
independent changed copy, and a suite that checks a prototype copy is
independent of its exemplar and of every other copy cloned from it.

To see the builder, the wither, and the prototype catalog run end to end
rather than just reading an assertion that says each one is correct, run
the example's own demo script:

```sh
examples/13-builder-prototype/demo.sh
```

It assembles a domestic shipment, rejects an international one with no
customs declaration, builds a valid international shipment with one, derives
a revalued customs declaration through a wither, and clones two independent
packages from the same registered exemplar:

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

Notice the second copy still reports `1.2 kg` and its original two
materials after the first copy was mutated: that is
`PackagingCatalog.create()`'s independence guarantee showing up as an
observable fact at the terminal, not just as a passing test.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original Builder and Prototype entries, including the "director" variant of Builder this chapter's fluent interface simplifies away in favor of a single caller-driven chain.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 2 covers the static nested Builder class this chapter's `Shipment.Builder` follows directly, including the validate-at-build discipline.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/13-builder-prototype`'s suite, which asserts independence across a prototype clone and across a record wither, not just that the build exits zero.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
