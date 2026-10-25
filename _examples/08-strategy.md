---
layout: example
order: 8
title: "Strategy: shipping-rate estimation"
description: One shipment, three interchangeable rate strategies, priced identically and proven by JUnit 5.
github_path: examples/08-strategy
card_eyebrow: "Chapter 8 · Strategy"
card_title: "Shipping-rate estimation"
card_blurb: "A checkout picks a carrier rate rule at runtime. The same pattern shown three idiomatic Java ways: a classic interface with concrete classes, a functional interface with lambdas, and an enum whose constants carry their own algorithm."
---

A checkout needs to compute a delivery cost, and the rate rule depends on the
carrier. This demo models a small shipping domain (a `Shipment` with a weight, a
destination zone, and a declared value, priced in a `Money` value type) and
shows the Strategy pattern in three idiomatic Java expressions, all pricing an
identical shipment identically:

1. **Classic interface.** A `RateStrategy` interface with `FlatRateStrategy`,
   `WeightTieredRateStrategy`, and `DistanceRateStrategy`, injected into a
   `ShippingRateCalculator` and swapped at runtime.
2. **Functional interface with lambdas.** The same contract used with lambdas and
   method references, built by the `RateStrategies` factories, with
   `withInsuranceSurcharge()` composing a new strategy on top of an existing one.
3. **Enum strategy.** A `Carrier` enum whose constants (`GROUND`, `AIR_FLAT`,
   `FREIGHT_ZONE`) each carry their own algorithm, the right fit when the set of
   strategies is closed and known at compile time.

The `RateStrategy` contract and the `Shipment`, `Money`, and `RateQuote` value
types are Java `record`s, and the JUnit 5 suite proves all three expressions
produce the identical quote for the same inputs.

## Build, run, observe

With the toolchain from the prerequisites chapter (JDK 25, Maven):

```sh
cd examples/08-strategy
mvn -q test          # 33 tests pass
./demo.sh            # prints all three expressions agreeing on the same quote
```

The module also runs from the aggregator at the repository root with
`mvn -q -f examples/pom.xml test`.
