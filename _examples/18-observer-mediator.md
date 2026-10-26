---
layout: example
order: 18
title: "Observer and Mediator: tracking and dispatch"
description: A shipment subject fanning status changes to listeners, and a dispatch hub that keeps collaborators decoupled.
github_path: examples/18-observer-mediator
card_eyebrow: "Chapter 18 · Observer and Mediator"
card_title: "Tracking and dispatch"
card_blurb: "A ShipmentTracker subject notifies listeners (named and lambda) on a status change, with subscriptions closed via try-with-resources; a DispatchCoordinator mediates between warehouse, carrier, and customer stations that never call each other directly. Tests verify notification, unsubscription, and decoupled routing."
---

Two patterns for coordinating objects without hard-wiring them together. **Observer**
lets a `ShipmentTracker` subject fan a status change out to listeners it never
names (named classes and lambdas, with subscriptions closed through
try-with-resources, and `java.util.concurrent.Flow` noted as the reactive option).
**Mediator** collapses direct references between a warehouse, carrier, and customer
station into one `DispatchCoordinator` hub.

The JUnit 5 tests prove observers are notified on change and an unsubscribed one is
not, and that the mediator routes events so the stations stay decoupled.

## Build, run, observe

```sh
cd examples/18-observer-mediator
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
