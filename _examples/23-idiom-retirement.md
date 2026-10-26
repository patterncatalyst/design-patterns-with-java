---
layout: example
order: 23
title: "Where Java 25 retires classic idioms"
description: The modern forms that replace double-checked locking, hand-written value classes, double-dispatch Visitor, and the null check.
github_path: examples/23-idiom-retirement
card_eyebrow: "Chapter 23 · Idiom retirement"
card_title: "The modern forms, working"
card_blurb: "The Java 25 replacements for four classic idioms, as working code: an enum singleton (proven under a 32-thread race) for double-checked locking, a record for a hand-written value class and its defensive copies, a sealed interface plus exhaustive switch for the double-dispatch Visitor, and Optional for the null check."
---

The book's through-line, gathered as working code: the modern Java 25 forms that
make four classic idioms unnecessary. An **enum singleton** (proven under a
32-thread race) replaces double-checked locking; a **record** replaces a
hand-written value class and its defensive copies; a **sealed interface plus
exhaustive switch** replaces the double-dispatch Visitor and the instanceof chain;
**Optional** replaces the null check. The retired idioms are described in the
chapter prose; this module ships only the modern forms, with tests.

## Build, run, observe

```sh
cd examples/23-idiom-retirement
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
