---
layout: example
order: 22
title: "Enums, functional interfaces, streams, and Optional as patterns"
description: Everyday functional Java 25 constructs standing in for classic patterns, in a nightly fulfillment batch.
github_path: examples/22-functional-java
card_eyebrow: "Chapter 22 · Functional Java"
card_title: "Patterns as everyday functional Java"
card_blurb: "A nightly shipment-fulfillment batch showing enums carrying behavior (Strategy, Singleton), lambdas and method references as Strategy and Command, a Template Method taking functions, composed UnaryOperator adjustments, stream pipelines with EnumMap grouping, and Optional as an honest return. Tests assert behavior across all of them."
---

Everyday Java 25 constructs are pattern implementations in disguise. This module
runs a nightly fulfillment batch that uses an enum carrying behavior (Strategy and
a Singleton sequence), lambdas and method references as Strategy and Command, a
Template Method expressed as a function taking steps, composed `UnaryOperator`
pricing adjustments (`andThen` and `compose` do not commute), stream pipelines with
`EnumMap`-backed grouping, and `Optional` as an explicit return type (never a
field).

The JUnit 5 tests assert behavior across each construct, including composition
order and present/absent Optional cases.

## Build, run, observe

```sh
cd examples/22-functional-java
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
