---
layout: example
order: 32
title: "Refactor, rewrite, or strangler fig"
description: An incremental strangler-fig migration of a legacy fee calculator behind a stable interface.
github_path: examples/32-refactor-rewrite
card_eyebrow: "Chapter 32 · Refactor, rewrite, strangler"
card_title: "A strangler-fig migration"
card_blurb: "A legacy accessorial-fee calculator with undocumented carrier rules, a clean modern reimplementation proven equivalent, and a strangler router behind a stable interface that routes each shipment to old or new by a migration registry, so the cutover is invisible slice by slice."
---

Deciding among in-place refactor, rewrite, and the strangler fig, then a worked
strangler migration. This module keeps a legacy accessorial-fee calculator (with
three undocumented carrier rules) running behind a stable interface, adds a clean
modern reimplementation proven equivalent, and routes each shipment to the legacy
or modern path through a `StranglerFeeCalculatorRouter` driven by a migration
registry. Traffic moves slice by slice and the total fee stays identical across
the cutover.

The JUnit 5 + Mockito tests pin the legacy behavior, prove old and new agree, and
verify the router sends each shipment to the right implementation.

## Build, run, observe

```sh
cd examples/32-refactor-rewrite
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
