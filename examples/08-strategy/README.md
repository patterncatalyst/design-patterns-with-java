# 08 - Strategy: shipping-rate quoting

A checkout needs to quote a shipping cost, and the carrier rate rule is
chosen per contract, not per line of code: flat-rate, weight-tiered, or a
distance-and-weight zone table. This example prices the same `Shipment`
three idiomatic Java ways, side by side, so you can compare the mechanism
each one uses to select behavior without changing the pricing rules
themselves.

## The domain

- `Shipment`: a `record` with `weightKg`, a `DestinationZone` (`LOCAL`,
  `REGIONAL`, `NATIONAL`, `INTERNATIONAL`), and `declaredValueCents`.
- `Money` tracks cents as a `long`, never a `double`, so a quote is exact and
  reproducible in a test assertion.
- `RateQuote` is a `record` of who quoted it (`quotedBy`) and how much
  (`amount`).
- The pricing math itself, in `ShippingMath` (`weightTieredCents`,
  `distanceZoneCents`, `insuranceSurchargeCents`), is shared by all three
  Strategy expressions below. Only the dispatch mechanism differs; an
  identical `Shipment` prices identically no matter which one quoted it.

## The three Strategy expressions

1. **An interface with named classes** (`named/FlatRateStrategy.java`,
   `named/WeightTieredRateStrategy.java`, `named/DistanceRateStrategy.java`):
   named concrete classes implementing the `RateStrategy` interface.
   `ShippingRateCalculator` owns exactly one at a time, injected through the
   constructor and reassignable later with `useStrategy()`.
2. **The same contract as a lambda** (`RateStrategies.java`): the
   identical `RateStrategy` functional interface, satisfied by a one-line
   lambda instead of a named class. `withInsuranceSurcharge()` composes a
   new strategy on top of an existing one, a capability a class hierarchy
   would need a decorator class to match.
3. **A closed set as enum constants** (`Carrier.java`): `GROUND`,
   `AIR_FLAT`, and `FREIGHT_ZONE` each implement `RateStrategy` through their
   own constant-specific method body. This is the company's entire,
   contracted carrier list; the compiler guarantees every constant supplies
   a `quote` body, the way a `switch` with a `default` throw only catches a
   missing case at runtime.

`ShippingDemo.java` exercises all three expressions against one `Shipment`
and prints each quote.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

This example is standalone-buildable from this directory:

```sh
cd examples/08-strategy
mvn test
```

It is also picked up by the repo root's aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 33 JUnit 5 cases (AssertJ assertions), including a
parameterized suite that asserts the interface, lambda, and enum
expressions quote an identical `Shipment` identically, not just that each
one runs without throwing.

To see the three strategies price the same shipment side by side rather
than just reading an assertion that says they agree, run the demo:

```sh
./demo.sh
```

Expected output:

```
== Interface: named classes implementing RateStrategy ==
Weight-tiered strategy: RateQuote[quotedBy=ground, amount=$11.30]
Distance-zone strategy (swapped at runtime): RateQuote[quotedBy=freight-zone, amount=$11.70]
Flat-rate strategy (new instance): RateQuote[quotedBy=air-flat, amount=$9.99]

== Lambda: the same contract as a one-expression value ==
Weight-tiered lambda: RateQuote[quotedBy=ground, amount=$11.30]
Weight-tiered lambda + insurance (composed): RateQuote[quotedBy=ground, amount=$12.26]

== Enum: a closed set as constants ==
GROUND: RateQuote[quotedBy=ground, amount=$11.30]
AIR_FLAT: RateQuote[quotedBy=air-flat, amount=$9.99]
FREIGHT_ZONE: RateQuote[quotedBy=freight-zone, amount=$11.70]

Notice GROUND, the weight-tiered lambda, and WeightTieredRateStrategy
all quote the same amount for the same shipment: one shared pricing
function, three dispatch mechanisms.
```

Notice `GROUND`, "Weight-tiered lambda", and "Weight-tiered strategy" all
quote `$11.30` for the same shipment. That agreement is the shared
`ShippingMath` showing up as an observable fact at the terminal, not just as
a passing test.

## Files

```
examples/08-strategy/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run ShippingDemo
  src/main/java/com/patterncatalyst/designpatterns/ch08/shipping/
    DestinationZone.java         # LOCAL/REGIONAL/NATIONAL/INTERNATIONAL
    Shipment.java                 # record: weightKg, zone, declaredValueCents
    Money.java                     # record: exact integer cents
    RateQuote.java                  # record: quotedBy, amount
    ShippingMath.java                 # shared pricing math
    RateStrategy.java                  # the Strategy contract (functional interface)
    ShippingRateCalculator.java         # the caller; holds one RateStrategy
    RateStrategies.java                  # expression 2: lambdas + composition
    Carrier.java                          # expression 3: enum constants
    ShippingDemo.java                      # exercises all three
    named/
      FlatRateStrategy.java                 # expression 1
      WeightTieredRateStrategy.java          # expression 1
      DistanceRateStrategy.java               # expression 1
  src/test/java/com/patterncatalyst/designpatterns/ch08/shipping/
    StrategyEquivalenceTest.java   # the three expressions quote identically
    ShippingRateCalculatorTest.java # caller-side behavior, including swap
    RateStrategiesCompositionTest.java # insurance surcharge composition
    CarrierTest.java                # closed-set invariants
    ShipmentTest.java                # value-type validation edge cases
    MoneyTest.java                    # exact-cents arithmetic
```
