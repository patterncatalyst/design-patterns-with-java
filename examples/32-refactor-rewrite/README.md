# 32 - Refactor, rewrite, or strangler fig

An accessorial fee calculator for shipping (fuel surcharge, residential
delivery fee, liftgate fee, oversize fee) is the component nobody wants to
touch: it has priced every shipment for years, and three of its branches
encode a carrier rate or a fee waiver nobody currently on the team can trace
to a specification. This example pins that legacy component with a
characterization suite, rebuilds it behind the same interface, and routes
traffic between the two with a strangler fig seam.

## The domain

- `ShipmentManifest`: a record carrying a shipment ID, carrier code,
  declared value, weight, and three boolean flags (residential delivery,
  liftgate required, oversized).
- `AccessorialFees`: a record with one field per fee plus a `totalCents()`
  method, so a disagreement names which specific fee broke, not only that
  the total was wrong.
- `AccessorialFeeCalculator`: the stable interface every caller depends on.
- `LegacyAccessorialFeeCalculator`: the inherited component, left in its
  real shape (a long method, string-equality carrier checks, magic
  numbers) because that is the subject of this chapter's part. Three of its
  rules are undocumented business knowledge: a 2019 fuel-rate renegotiation,
  a weight-based residential fee waiver, and a carrier-specific oversize
  penalty.
- `ModernAccessorialFeeCalculator`: the rewrite. Every rate and quirk is
  preserved; only the shape changes (a switch expression instead of an
  if/else chain, named constants instead of magic numbers).
- `MigratedShipmentRegistry`: the cutover policy, an explicit set of
  migrated shipment IDs.
- `StranglerFeeCalculatorRouter`: the routing seam. It implements
  `AccessorialFeeCalculator` itself and delegates to the legacy or modern
  calculator per shipment, by consulting the registry.

## The characterization suite

`AccessorialFeeCharacterizationCases` holds five shipment profiles and the
exact fee totals `LegacyAccessorialFeeCalculator` returns today, captured by
running the legacy calculator rather than derived from a specification that
does not exist. `LegacyAccessorialFeeCalculatorCharacterizationTest` pins
the legacy calculator against this table.
`ModernAccessorialFeeCalculatorAgreementTest` runs the same table against
the modern calculator, then adds one direct engine-to-engine comparison on
a case outside the table, so the rewrite's claim to agree with the legacy
behavior rests on more than one shared source of expected values.

## The routing seam

`StranglerFeeCalculatorRouterTest` stands in both calculators with Mockito
mocks and verifies the interaction, not just the return value: a migrated
shipment reaches the modern calculator and never the legacy one, an
unmigrated shipment reaches the legacy calculator and never the modern one,
and once every shipment has migrated, the legacy calculator is never
touched again (`verifyNoInteractions`).

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

Standalone, from this directory:

```sh
cd examples/32-refactor-rewrite
mvn test
```

Also picked up by the repository root's aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 17 JUnit 5 cases. Observed locally on Fedora with
Temurin 25:

```
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see the migration play out rather than read about it, run the demo,
which steps through three releases from a cold legacy-only start to every
shipment on the modern calculator:

```sh
./demo.sh
```

Expected output:

```
== Release 1: every shipment still quoted by the legacy calculator ==
SHIP-1001 (OCEAN): $150.00
SHIP-1002 (AIR): $137.00
SHIP-1003 (GROUND): $16.00

== Release 2: SHIP-1002 migrated, the other two still legacy ==
SHIP-1001 (OCEAN): $150.00
SHIP-1002 (AIR): $137.00
SHIP-1003 (GROUND): $16.00
shipments migrated so far: 1 of 3

== Release 3: every shipment migrated, legacy calculator is unused ==
SHIP-1001 (OCEAN): $150.00
SHIP-1002 (AIR): $137.00
SHIP-1003 (GROUND): $16.00
shipments migrated so far: 3 of 3
```

Every total is identical across all three releases. That is the strangler
fig's entire argument made visible at a terminal: the cutover is invisible
because the characterization table proved the two calculators agree before
a single shipment was ever routed to the modern one.

## Files

```
examples/32-refactor-rewrite/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run Main
  src/main/java/com/patterncatalyst/designpatterns/ch32/accessorials/
    ShipmentManifest.java           # record: shipmentId, carrierCode, declaredValueCents, weightKg, flags
    AccessorialFees.java             # record: one field per fee, plus totalCents()
    AccessorialFeeCalculator.java     # the stable interface
    LegacyAccessorialFeeCalculator.java  # the inherited component, left in its real shape
    ModernAccessorialFeeCalculator.java   # the rewrite, same rules, cleaner shape
    MigratedShipmentRegistry.java          # the cutover policy
    StranglerFeeCalculatorRouter.java       # the routing seam
    Main.java                                # demo entry point: three releases
  src/test/java/com/patterncatalyst/designpatterns/ch32/accessorials/
    AccessorialFeeCharacterizationCases.java   # the shared, pinned table
    LegacyAccessorialFeeCalculatorCharacterizationTest.java  # pins the legacy calculator
    ModernAccessorialFeeCalculatorAgreementTest.java          # the rewrite against the same table
    StranglerFeeCalculatorRouterTest.java                      # routing and cutover, with Mockito
    MigratedShipmentRegistryTest.java                           # the cutover policy's own behavior
```
