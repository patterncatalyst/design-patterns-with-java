---
layout: example
order: 17
title: "Composite, Flyweight, Null Object: a shipment container tree"
description: A uniform container tree, a shared flyweight pool, and a null-object handler contrasted with Optional.
github_path: examples/17-composite-flyweight-null
card_eyebrow: "Chapter 17 · Composite, Flyweight, Null Object"
card_title: "A shipment container tree"
card_blurb: "A sealed ShipmentNode tree where a parcel and a box answer the same interface, a FlyweightPool interning shared PackagingSpec instances, and a null-object alert handler that removes a null check, with Optional shown as the modern alternative at the boundary. Tests prove aggregation, shared identity, and the safe no-op."
---

Three structural patterns about how objects relate. **Composite** models a
shipment container tree with a sealed `ShipmentNode` interface, so a `Parcel` leaf
and a `ShipmentBox` branch answer the same interface and a caller aggregates
weight and value uniformly. **Flyweight** interns shared immutable `PackagingSpec`
instances through a `FlyweightPool` so many parcels point at one copy. **Null
Object** supplies a do-nothing alert handler that removes a null check, with
`Optional` shown as the modern alternative at the boundary.

The JUnit 5 tests prove the tree aggregates correctly, the flyweight pool returns
the same instance for equal intrinsic state (reference identity), and the null
object is a safe no-op.

## Build, run, observe

```sh
cd examples/17-composite-flyweight-null
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
