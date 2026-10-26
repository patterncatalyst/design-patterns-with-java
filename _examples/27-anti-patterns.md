---
layout: example
order: 27
title: "Anti-patterns and pattern misuse"
description: An over-patterned shipping design taken apart into the simpler form that passes the same tests.
github_path: examples/27-anti-patterns
card_eyebrow: "Chapter 27 · Anti-patterns"
card_title: "Pattern misuse, taken apart"
card_blurb: "An over-engineered pricing design (a mutable-global singleton registry, a one-implementation interface, a factory of factories, a god object with a leaky abstraction) contrasted with the simple ShipmentPricingService. A parameterized equivalence test proves the simple form behaves identically."
---

A pattern applied with no force that calls for it is over-engineering. This module
ships the simple, appropriate form, a `ShipmentPricingService` built from a record
of settings, and contrasts it with a clearly labeled over-engineered exhibit (a
mutable-global singleton registry, a one-implementation interface, a factory of
factories, and a god object with a leaky abstraction). Both are modern Java; the
difference is justified structure versus indirection with no variation point.

A parameterized equivalence test proves the simple form produces the same results as
the over-engineered one, so the simplification is safe, and other tests expose the
exhibit's global-state and leaky-abstraction costs.

## Build, run, observe

```sh
cd examples/27-anti-patterns
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
