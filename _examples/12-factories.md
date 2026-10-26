---
layout: example
order: 12
title: "Factory Method and Abstract Factory: a regional shipping desk"
description: Choosing a concrete type at runtime, and building a consistent family of related products together.
github_path: examples/12-factories
card_eyebrow: "Chapter 12 · Factories"
card_title: "A regional shipping desk"
card_blurb: "Abstract Factory builds a matching label formatter, rate table, and customs form per region behind one interface; static factory methods backed by a Supplier registry resolve the toolkit; a Factory Method with a hook picks the right shipment inspector. Tests prove each factory returns a consistent family."
---

Factory Method decides which concrete type to build at runtime; Abstract Factory
builds a whole family of related objects in a consistent variant together. This
module models a regional shipping desk: a `ShippingToolkit` per region produces a
matching `LabelFormatter`, `RateTable`, and `CustomsForm`, resolved by a static
factory method backed by a `Supplier` registry; a `ShipmentInspector` factory
method picks the right checklist by category.

The JUnit 5 tests prove each factory returns the correct consistent family for an
input, and a Mockito suite verifies the one-factory-call-per-construction
guarantee.

## Build, run, observe

```sh
cd examples/12-factories
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
