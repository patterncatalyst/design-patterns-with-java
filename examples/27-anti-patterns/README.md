# 27 - Anti-patterns and pattern misuse: shipping-rate pricing

A shipment-pricing calculation, multiply a base cost by a fuel surcharge and
add a handling fee, built two ways: an over-engineered design that stacks a
singleton registry, a factory-of-factories, and a god object on top of the
calculation, and the simplified, appropriate form that does the identical
job with one value and one class. Both are modern Java; the contrast is
over-engineered versus appropriate, not old Java versus new Java.

## The over-engineered design (`overengineered/`)

- `ShippingSurchargeRegistry`: an enum singleton, built with the same
  correctness guarantees as any other modern Java singleton, holding one
  mutable fuel surcharge multiplier that more than one caller believes it
  can configure independently.
- `RateCalculator` / `StandardRateCalculator`: an interface with exactly one
  implementation and no second one planned: speculative generality.
- `RateCalculatorFactory` / `StandardRateCalculatorFactory`: a factory
  wrapped around a constructor call that never varies.
- `RateCalculatorFactoryProvider`: a second singleton, built solely to hand
  out the one factory this program will ever construct. A factory of
  factories.
- `ShipmentOperationsManager`: a god object that resolves a calculator
  through both singletons, validates input, writes an audit log, and
  formats a receipt, four unrelated responsibilities in one class, plus a
  leaky abstraction in `wasLastCalculationSurchargeApplied()`, which answers
  for whichever shipment was priced most recently rather than the shipment
  a caller is actually asking about.

## The simplified rewrite

- `RateSettings`: a record pairing a surcharge multiplier and a handling
  fee, the entire configuration surface the six-type over-engineered design
  spent itself expressing.
- `PricingResult`: a record pairing a total cost with whether a surcharge
  was applied, fixing the leaky abstraction by attaching the fact to the
  specific result it describes.
- `ShipmentPricingService`: one class, constructed with one `RateSettings`
  value, that computes a `PricingResult` from a base cost. No singleton, no
  factory, no interface.

## What the tests prove

- `ShippingSurchargeRegistrySpookyActionTest` forces the singleton's cost
  into the open: a "region A" calculator's answer changes when an unrelated
  "region B" call reconfigures the shared registry, with no code touching
  region A's calculator directly.
- `ShipmentOperationsManagerTest` proves the god object's audit-log side
  effect and the leaky `wasLastCalculationSurchargeApplied()` bug directly.
- `EquivalenceTest` drives both designs through a table of base costs and
  surcharge multipliers and asserts the totals agree exactly, plus a shared
  check that both reject a non-positive base cost with the same exception
  type: the simplification is behaviorally safe.
- `ShipmentPricingServiceTest` proves the testability gap: two
  independently configured `ShipmentPricingService` instances never
  interfere with each other, and substituting a different rate is a
  one-line `RateSettings` value with no interface to mock and nothing to
  reset once a test ends.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

This example is standalone-buildable from this directory:

```sh
cd examples/27-anti-patterns
mvn test
```

It is also picked up by the repo root's aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 18 JUnit 5 cases (AssertJ assertions), including a
parameterized equivalence suite across five base-cost and surcharge
combinations.

To see the spooky action at a distance and the independent-configuration
fix side by side at the terminal, rather than read them as an assertion,
run the demo:

```sh
./demo.sh
```

Expected output:

```
== Over-engineered: singleton + factory-of-factories ==
Region A quote (surcharge 1.10): $110.00
ORD-3001 priced via the manager: $110.00
ORD-3002 priced via the manager: $130.00
Region A quote, re-read with no code touching it: $130.00

== Simplified: plain dependency injection of RateSettings ==
Domestic total (surcharge 1.00): $102.50
International total (surcharge 1.35): $144.00
Domestic total, re-read after pricing international: $102.50
```

Region A's quote moves from $110.00 to $130.00 with no code touching
`regionA` directly, while the domestic total stays at $102.50 no matter
what the international pricing call does.

## Files

```
examples/27-anti-patterns/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run ShipmentPricingDemo
  src/main/java/com/patterncatalyst/designpatterns/ch27/shipping/
    Money.java                       # exact integer cents
    RateSettings.java                 # the simplified design's entire config surface
    PricingResult.java                 # total cost + surcharge-applied, paired
    ShipmentPricingService.java         # the simplified, appropriate form
    ShipmentPricingDemo.java             # exercises both designs side by side
    overengineered/
      ShippingSurchargeRegistry.java      # singleton as global mutable state
      RateCalculator.java                  # speculative generality: one impl
      StandardRateCalculator.java           # the only implementation
      RateCalculatorFactory.java             # a factory with no variation
      StandardRateCalculatorFactory.java      # the only implementation
      RateCalculatorFactoryProvider.java       # factory of factories
      PricedShipmentReceipt.java                # formatted receipt value
      ShipmentOperationsManager.java             # god object + leaky abstraction
  src/test/java/com/patterncatalyst/designpatterns/ch27/shipping/
    EquivalenceTest.java            # both designs agree; safe to simplify
    ShipmentPricingServiceTest.java  # independent configuration, no interference
    RateSettingsTest.java            # value-type validation
    MoneyTest.java                    # exact-cents formatting
    overengineered/
      ShippingSurchargeRegistrySpookyActionTest.java  # forces the singleton cost open
      ShipmentOperationsManagerTest.java               # god object + leaky abstraction
```
