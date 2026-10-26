# 12 - Factory Method and Abstract Factory: a regional shipping desk

A shipment leaving a regional hub needs three documents that all have to
agree with each other: a label in that region's own format, a rate quote
computed by that region's own rule, and a customs declaration in that
region's own compliance scheme. This example builds that whole family with
an Abstract Factory, and a separate, narrower workflow with a Factory
Method, so the two patterns' different scope is visible side by side.

## The domain

- `Shipment`: a `record` with `destinationCountry`, `weightKg`,
  `declaredValueCents`, and the `Region` it departs from
  (`NORTH_AMERICA`, `EUROPEAN_UNION`, `ASIA_PACIFIC`).
- `Money` tracks cents as a `long`, never a `double`, so a quote is exact.
- `LabelFormatter`, `RateTable`, and `CustomsForm` are the three product
  interfaces a region's toolkit must produce together.

## Abstract Factory: a family that must match

- `ShippingToolkit` declares the three creation methods, one per product.
- `regional/NorthAmericaToolkit.java`, `regional/EuropeanUnionToolkit.java`,
  and `regional/AsiaPacificToolkit.java` each implement all three, returning
  that region's own label format, rate math, and customs scheme. The
  concrete product classes stay package-private inside each toolkit file;
  nothing outside ever names them.
- `ShippingToolkits.forRegion(Region)` is the static factory method that
  decides which toolkit a `Region` value resolves to, backed by a
  `Map<Region, Supplier<ShippingToolkit>>` so a new region only ever means
  one new map entry. `ShippingToolkits.forRegionName(String)` resolves the
  same toolkits from a configuration string.
- `RegionalShippingDesk` is the client: it builds all three products from
  one `ShippingToolkit` call in its constructor and never names a concrete
  region's toolkit anywhere in its own code.

## Factory Method: one product, decided by shipment category

- `ShipmentInspector.inspect(Shipment)` is `final`: it validates the
  shipment, runs one `InspectionChecklist`, and judges pass or fail the
  same way every time.
- `createChecklist()` is the factory method, a hook `StandardShipmentInspector`
  and `HazmatShipmentInspector` each override to plug in their own
  `InspectionChecklist` (`StandardInspectionChecklist`,
  `HazmatInspectionChecklist`). Neither subclass touches `inspect()`.
- `ShipmentInspectors.forCategory(ShipmentCategory)` is a static factory
  method over an exhaustive `switch` expression, resolving which inspector
  subclass a `STANDARD` or `HAZMAT` shipment needs.

`ShippingDemo.java` exercises both patterns against real shipments and
prints the result.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

This example is standalone-buildable from this directory:

```sh
cd examples/12-factories
mvn test
```

It is also picked up by the repo root's aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 30 JUnit 5 cases, asserted with AssertJ and one
Mockito suite that verifies `RegionalShippingDesk` builds all three products
from exactly one `ShippingToolkit` call in its constructor, never a second
time per shipment processed.

To see both patterns produce real output rather than just reading an
assertion that says they work, run the example's own demo script:

```sh
./demo.sh
```

Expected output:

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

Notice that the toolkit looked up by name, `"north america"`, quotes the
identical $15.32 rate and prints the identical label as the toolkit
resolved directly from `Region.NORTH_AMERICA`. That agreement is
`RegionalShippingDesk` depending only on the three product interfaces
showing up as an observable fact at the terminal, however a `ShippingToolkit`
reached its constructor.

## Files

```
examples/12-factories/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run ShippingDemo
  src/main/java/com/patterncatalyst/designpatterns/ch12/shipping/
    Region.java                   # the closed set of hubs
    Shipment.java                  # record: destination, weight, value, region
    Money.java                      # record: exact integer cents
    LabelFormatter.java              # product interface
    RateTable.java                    # product interface
    CustomsForm.java                   # product interface
    ShippingToolkit.java                # the Abstract Factory interface
    ShippingToolkits.java                 # static factory method + Supplier registry
    ProcessedShipment.java                 # record: the family's output
    RegionalShippingDesk.java               # the client; depends on the abstractions
    InspectionChecklist.java                 # Factory Method's one varying product
    InspectionResult.java                     # record: passed + log
    StandardInspectionChecklist.java           # one checklist implementation
    HazmatInspectionChecklist.java              # the other checklist implementation
    ShipmentInspector.java                       # the creator; final inspect() sequence
    StandardShipmentInspector.java                # plugs in the standard checklist
    HazmatShipmentInspector.java                   # plugs in the hazmat checklist
    ShipmentCategory.java                           # STANDARD | HAZMAT
    ShipmentInspectors.java                          # static factory over the inspectors
    ShippingDemo.java                                 # exercises both patterns
    regional/
      NorthAmericaToolkit.java                          # family: USMCA scheme
      EuropeanUnionToolkit.java                           # family: EORI scheme
      AsiaPacificToolkit.java                              # family: HS tariff scheme
  src/test/java/com/patterncatalyst/designpatterns/ch12/shipping/
    MoneyTest.java                     # exact-cents arithmetic
    ShipmentTest.java                   # value-type validation edge cases
    ShippingToolkitsTest.java            # lookup, Supplier registry, name resolution
    RegionalShippingDeskTest.java         # Mockito: one factory call, not per shipment
    ShipmentInspectorTest.java             # the fixed sequence, both checklists
    ShipmentInspectorsTest.java             # category-to-subclass resolution
    regional/
      NorthAmericaToolkitTest.java           # family consistency + rate math
      EuropeanUnionToolkitTest.java           # family consistency + rate math
      AsiaPacificToolkitTest.java              # family consistency + rate math
```
