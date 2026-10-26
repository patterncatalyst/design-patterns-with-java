---
layout: example
order: 33
title: "Final project: a composed shipping system"
description: Builder, State/FSM, Command/undo, Observer, and Strategy working together in one shipment dispatch center.
github_path: examples/33-final-project
card_eyebrow: "Chapter 33 · Final project"
card_title: "A composed shipping system"
card_blurb: "One coherent system wiring five patterns: a ShipmentBuilder that resolves a rate Strategy per carrier, a Shipment that is both a State machine and an Observer subject, undoable Commands with a history, and status listeners. 39 tests prove the composition, including a Mockito InOrder integration test."
---

The final project composes the patterns the book taught into one working shipping
dispatch center: a `ShipmentBuilder` (Builder) resolves a `RateStrategy` per carrier,
a `Shipment` is both the State-machine context and the Observer subject, undoable
`ShipmentCommand`s drive it through its lifecycle with a `CommandHistory`, and status
listeners react to events. Each pattern earns its place by the force it answers.

The JUnit 5 + AssertJ (+ Mockito) tests prove the composed behavior end to end:
building, the lifecycle (illegal transitions refused), command undo restoring state,
a strategy swap, and observers seeing events, with an `InOrder` integration test.

## Build, run, observe

```sh
cd examples/33-final-project
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
