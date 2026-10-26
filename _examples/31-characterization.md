---
layout: example
order: 31
title: "Characterization tests; Fowler vs Feathers"
description: Pinning a gnarly legacy freight calculation with characterization tests, then refactoring safely.
github_path: examples/31-characterization
card_eyebrow: "Chapter 31 · Characterization tests"
card_title: "Pinning behavior before changing it"
card_blurb: "A gnarly legacy freight-quote calculator with three real boundary quirks, pinned by a characterization suite (values computed by running the legacy code, not from a spec), then refactored Fowler-style to a polymorphic carrier model that keeps every pinned case green."
---

When you do not understand inherited code, characterization tests lock in its
current behavior so a later change that alters it is caught. This module pins a
legacy freight-quote calculator (with three real boundary quirks: a truncating
weight tier, a strict residential threshold, a truncating insurance surcharge)
with an 8-row characterization table whose expected values were computed by
actually running the legacy code. A Fowler-style refactor to a sealed carrier
model then keeps every pinned case green.

The chapter contrasts Fowler's Refactoring (clean code, net already present) with
Feathers' legacy-first discipline (build the net, then improve).

## Build, run, observe

```sh
cd examples/31-characterization
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
