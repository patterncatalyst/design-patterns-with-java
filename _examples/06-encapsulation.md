---
layout: example
order: 6
title: "Encapsulation and API design: interfaces, factories, records"
description: Hiding implementation behind a public interface and a static factory, with records and defensive copies for honest value types.
github_path: examples/06-encapsulation
card_eyebrow: "Chapter 6 · Encapsulation"
card_title: "Interfaces, static factories, records"
card_blurb: "A shipping gateway behind a public interface reached through a static factory, with a package-private implementation; immutable value types as records; and a type that defensively copies a collection it holds. Tests prove callers cannot mutate internal state."
---

Good encapsulation lets a representation change without touching callers. This
module shows Java's tools for it: a **public interface with a package-private
implementation** reached through a **static factory method**, **records** for
immutable value types, and **defensive copying** of collection fields so a caller
cannot reach in and mutate internal state.

The JUnit 5 tests prove immutability (a returned history cannot be modified),
defensive copying (mutating the caller's list does not leak into the type), and
factory behavior (a cached empty instance is reused).

## Build, run, observe

```sh
cd examples/06-encapsulation
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
