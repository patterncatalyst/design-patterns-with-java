# 31 - Characterization tests; Fowler vs Feathers

`LegacyFreightQuoteCalculator` is inherited, untested freight-quote logic:
two parallel if/else chains keyed on the same carrier name, a weight
surcharge bucketed by truncating integer division, a residential surcharge
with a strict greater-than threshold, and a declared-value surcharge that
truncates instead of rounds. Nobody wrote a test for any of it. This example
pins its current behavior with characterization tests, quirks included, then
reshapes it with three named Fowler refactorings without changing a single
pinned output.

## The domain

- `FreightQuote`: a `record` both calculators return (`transitDays`,
  `baseCents`, `surchargeCents`, `totalCents`), so their outputs compare
  field by field.
- `LegacyFreightQuoteCalculator`: the inherited "before." Six positional
  arguments, a duplicated carrier dispatch, and three boundary quirks:
  - A 10kg weight tier computed with `(int) (weightKg / 10.0)`: 9.99kg pays
    nothing extra, 10.0kg jumps a full tier.
  - A residential surcharge gated on `weightKg > 20.0`: exactly 20.0kg pays
    nothing, 20.01kg pays $2.50.
  - A declared-value insurance surcharge computed as `(long) (excessCents *
    0.01)`: truncated toward zero instead of rounded to the nearest cent.
- `CarrierProfile` (sealed) / `GroundCarrierProfile` / `AirCarrierProfile`
  / `ExpressCarrierProfile`: Replace Conditional with Polymorphism, one
  object per carrier owning both its transit-day rule and its base rate,
  replacing the legacy class's two separate if/else chains.
- `ShipmentDetails`: Introduce Parameter Object, the six positional
  arguments collapsed into one value.
- `FreightQuoteCalculator`: the refactored "after." Extract Function pulled
  each surcharge rule into its own named method; the carrier dispatch and
  the parameter list are reshaped; every quirk's arithmetic is unchanged.

## Characterization tests

`FreightQuoteCharacterizationTest`:

- `representativeShipments()` is an `@MethodSource` table of eight rows.
  Each row's expected values were not derived from a specification; they
  were recorded by calling the legacy calculator with that row's input and
  writing down what it actually returned.
- `legacyCalculatorMatchesPinnedBehavior` runs that table against
  `LegacyFreightQuoteCalculator`.
- `refactoredCalculatorMatchesPinnedBehavior` runs the identical table
  against `FreightQuoteCalculator`. Every expected value is unchanged; this
  is the proof the refactor preserved behavior.
- The `PinnedQuirks` nested class names each boundary quirk directly (the
  weight-tier cliff, the residential threshold, the truncated insurance
  surcharge) rather than leaving them to be inferred from the table, and
  `refactorReproducesEveryPinnedQuirk` checks the refactor reproduced each
  one, not only the common-path totals.
- The `UnknownCarrier` nested class pins the one error path both
  calculators share: an unrecognized carrier code throws
  `IllegalArgumentException` from both.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

Standalone, from this directory:

```sh
cd examples/31-characterization
mvn test
```

Also picked up by the repository root's aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 22 JUnit 5 cases. Observed locally on Fedora with
Temurin 25:

```
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see the legacy and refactored calculators agree on every shipment, and
the weight-tier cliff pinned by the suite, rather than just reading an
assertion that says so, run the demo:

```sh
./demo.sh
```

Expected output:

```
== Legacy and refactored calculators agree on every shipment ==
ground   A/A -> $5.00  total $5.00  [identical]
ground   A/B -> $6.50  total $6.50  [identical]
air      A/A -> $20.50  total $20.50  [identical]
express  A/B -> $30.02  total $30.02  [identical]

== A quirk the characterization suite pins: the 10kg tier cliff ==
9.99kg:  surcharge $0.00
10.00kg: surcharge $1.50  (0.01kg more weight, $1.50 more surcharge)
```

## Files

```
examples/31-characterization/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run Main
  src/main/java/com/patterncatalyst/designpatterns/ch31/shipping/
    FreightQuote.java              # record: transitDays, baseCents, surchargeCents, totalCents
    LegacyFreightQuoteCalculator.java  # the "before": duplicated dispatch, three boundary quirks
    ShipmentDetails.java              # parameter object (Introduce Parameter Object)
    CarrierProfile.java               # sealed interface (Replace Conditional with Polymorphism)
    GroundCarrierProfile.java
    AirCarrierProfile.java
    ExpressCarrierProfile.java
    FreightQuoteCalculator.java       # the "after": same arithmetic, Fowler-refactored shape
    Main.java                         # demo entry point
  src/test/java/com/patterncatalyst/designpatterns/ch31/shipping/
    FreightQuoteCharacterizationTest.java  # pinned table, quirk-named tests, exception parity
```
