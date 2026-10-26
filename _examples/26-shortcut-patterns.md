---
layout: example
order: 26
title: "Shortcut patterns: dispatch tables and maps"
description: Routing by key through an enum/EnumMap dispatch and an open Map of function handlers with a fallback.
github_path: examples/26-shortcut-patterns
card_eyebrow: "Chapter 26 · Shortcut patterns"
card_title: "Dispatch tables and maps"
card_blurb: "An EnumMap dispatch over a closed ShipmentEventType (exhaustive, compiler-checked) and an open Map<String, handler> webhook router with runtime registration and a fallback. Tests cover every key and the fallback path. The lightweight alternative when a full Strategy/Command hierarchy is overkill."
---

When a key selects one short handler and a full class hierarchy would be overkill, a
dispatch table is the lightweight answer. This module shows two forms: an
`EnumMap<ShipmentEventType, handler>` over a closed enum (exhaustive and
compiler-checked) and an open `Map<String, BiConsumer<...>>` webhook router with
runtime `register()` and a `getOrDefault()` fallback. The trade-off is explicit:
concise and data-driven, but the open form gives up the compiler's exhaustiveness.

The JUnit 5 tests cover every enum key and every built-in webhook code, the fallback
for an unknown key, and runtime registration replacing a handler.

## Build, run, observe

```sh
cd examples/26-shortcut-patterns
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
