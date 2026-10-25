---
layout: example
order: 10
title: "State and finite state machines: the shipment lifecycle"
description: A shipment FSM as an enum table and as a sealed interface, plus a probabilistic machine, with illegal transitions refused.
github_path: examples/10-state-machines
card_eyebrow: "Chapter 10 · State and FSM"
card_title: "The shipment lifecycle as a state machine"
card_blurb: "A shipment lifecycle modeled three ways: an enum with a transition table, a sealed interface with an exhaustive pattern-matching switch, and a probabilistic machine driven by an injected RandomGenerator. Tests prove illegal transitions are refused and the two deterministic machines agree."
---

Behavior that depends on a current status, where illegal transitions must be
refused, is a finite state machine. This module models a shipment lifecycle three
idiomatic ways: an **enum with a transition table**, a **sealed interface with an
exhaustive pattern-matching switch** (no `default`, so the compiler flags a
missing case), and a **probabilistic machine** whose transitions are drawn from a
weighted distribution via an injected `RandomGenerator` for testability.

The JUnit 5 tests prove legal transitions succeed and illegal ones are refused,
cross-check that the two deterministic machines agree, and verify the stochastic
machine is reproducible under a seed and lands within tolerance of its target
distribution over many draws.

## Build, run, observe

```sh
cd examples/10-state-machines
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
