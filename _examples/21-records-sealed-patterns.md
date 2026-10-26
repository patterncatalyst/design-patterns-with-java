---
layout: example
order: 21
title: "Records, sealed interfaces, and pattern matching"
description: Data-oriented programming in Java 25, with records, sealed hierarchies, and exhaustive pattern-matching switches.
github_path: examples/21-records-sealed-patterns
card_eyebrow: "Chapter 21 · Records, sealed, pattern matching"
card_title: "Data-oriented programming"
card_blurb: "Sealed ParcelKind, ShippingEvent, and RateRule hierarchies of records, with operations kept as external exhaustive switches (type patterns, guarded when, and record deconstruction nested several levels). Tests verify every switch handles every variant and compact-constructor validation holds."
---

Records, sealed interfaces, and pattern matching form one toolkit for modeling
data and the operations over it. This module models shipping data as sealed
hierarchies of records (`ParcelKind`, `ShippingEvent`, a nestable `RateRule`
pricing grammar) and keeps operations as external **exhaustive pattern-matching
switches**, including guarded patterns and record deconstruction nested several
levels deep, with no `accept()` method on the data types.

The JUnit 5 tests verify each switch handles every variant correctly and the
compact constructors reject invalid data.

## Build, run, observe

```sh
cd examples/21-records-sealed-patterns
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
