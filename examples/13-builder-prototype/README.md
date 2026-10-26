# 13 - Builder and Prototype: assembling an international shipment

A `Shipment` has three required header fields, several optional ones with
sensible defaults, and one cross-field invariant that only holds once every
field is in place: a shipment headed outside the domestic market must carry
a `CustomsDeclaration`. This example builds that shipment through a
Bloch-style `Shipment.Builder`, derives a changed `CustomsDeclaration`
through a wither method, and clones pre-configured packaging exemplars
through a Prototype catalog.

## The domain

- `Shipment`: required `trackingId`, `carrier`, `destinationCountry`;
  optional `priority` (default `STANDARD`), `insuredValueCents` (default
  `0`), `signatureRequired` (default `false`), `customsDeclaration` (empty
  unless set), `specialHandlingNotes`. No public constructor; every
  instance comes from `Shipment.Builder.build()`.
- `CustomsDeclaration`: an immutable `record` with a compact canonical
  constructor that rejects a blank harmonized code, a blank origin country,
  or a negative declared value.
- `PackagingTemplate`: a mutable, configured packaging exemplar (kind,
  weight, handling code, protective materials) that implements a copy
  constructor and a `copy()` method.
- `PackagingCatalog`: a small named registry of `PackagingTemplate`
  exemplars.

## Builder: validate once, at build()

`Shipment.Builder` is a static nested class with a chained setter per
field. Nothing is checked as the chain runs; `validationError()` runs once,
inside `build()`, checking every required field and the one cross-field
invariant (international destination requires a customs declaration) in a
single pass:

```java
Shipment shipment = Shipment.builder()
        .trackingId("SHIP-4471")
        .carrier("Northline Freight")
        .destinationCountry("CA")
        .customsDeclaration(declaration)
        .build();
```

Leaving out `customsDeclaration()` for a `"CA"` destination throws
`IllegalStateException` from `build()`, never from a setter.

## Record wither: the modern complement

`CustomsDeclaration` is a record. Changing one field means deriving a new
instance with a wither method, `withDeclaredValueCents(long)` or
`withContentsDescription(String)`, rather than mutating the original, which
has no setters to call.

## Prototype: cloning a configured exemplar

`PackagingCatalog.create(name)` clones the named `PackagingTemplate` and
hands back an independent copy. Mutating that copy, or a second copy from
the same name, never reaches back into the registered exemplar or any other
clone, because `PackagingTemplate`'s copy constructor deep-copies its
mutable protective-materials list rather than sharing the original list
reference.

`ShippingDemo.java` exercises all three end to end.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

This example is standalone-buildable from this directory:

```sh
cd examples/13-builder-prototype
mvn test
```

It is also picked up by the repo root's aggregator:

```sh
mvn -f examples/pom.xml test
```

To see the builder, the wither, and the prototype catalog run end to end
rather than just reading an assertion that says each one is correct, run
the demo:

```sh
./demo.sh
```

Expected output:

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

Notice `second` still reports `1.2 kg` and its original two materials after
`first` was mutated: that is `PackagingCatalog.create()`'s independence
guarantee showing up as an observable fact at the terminal, not just as a
passing test.

## Files

```
examples/13-builder-prototype/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run ShippingDemo
  src/main/java/com/patterncatalyst/designpatterns/ch13/shipping/
    ShippingPriority.java         # STANDARD/EXPEDITED/OVERNIGHT
    CustomsDeclaration.java        # record + compact constructor + withers
    Shipment.java                   # Builder.build() validates once
    PackagingTemplate.java           # Prototype: copy constructor + copy()
    PackagingCatalog.java             # named registry of exemplars
    ShippingDemo.java                  # exercises all three end to end
  src/test/java/com/patterncatalyst/designpatterns/ch13/shipping/
    ShipmentBuilderTest.java    # rejects invalid combination, builds valid
    CustomsDeclarationWitherTest.java # wither independence + validation
    PackagingCatalogTest.java    # prototype copy independence
```
