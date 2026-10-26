---
layout: example
order: 20
title: "Visitor, Memento, Interpreter: operations, snapshots, a tiny grammar"
description: Operations over a sealed manifest via exhaustive switches, a memento snapshot/restore, and a small surcharge expression language.
github_path: examples/20-visitor-memento-interpreter
card_eyebrow: "Chapter 20 · Visitor, Memento, Interpreter"
card_title: "Operations, snapshots, a tiny grammar"
card_blurb: "Visitor as a sealed ManifestEntry with operations expressed as exhaustive pattern-matching switches (no accept() on the nodes), a ManifestDraft memento whose package-private snapshot record hides its internals, and a small SurchargeExpr grammar that evaluates itself by record-pattern switch. Tests cover every operation, restore, and nested expressions."
---

Three behavioral patterns expressed in idiomatic Java 25. **Visitor** adds
operations (cost, customs summary, validation) across a fixed sealed
`ManifestEntry` set as exhaustive pattern-matching switches, with no `accept()`
method on the node types. **Memento** captures and restores a `ManifestDraft`'s
state through a package-private snapshot record that stays hidden across the
package boundary. **Interpreter** expresses a small surcharge grammar as sealed
expression types that evaluate themselves by record-pattern switch.

The JUnit 5 tests cover each operation over every node kind, a memento restore
after a change, and nested-expression evaluation.

## Build, run, observe

```sh
cd examples/20-visitor-memento-interpreter
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
