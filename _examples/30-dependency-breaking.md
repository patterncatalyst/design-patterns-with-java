---
layout: example
order: 30
title: "Dependency-breaking techniques for Java"
description: Feathers' techniques adapted to Java to make hard-wired inherited code testable.
github_path: examples/30-dependency-breaking
card_eyebrow: "Chapter 30 · Dependency-breaking"
card_title: "Making hard-wired code testable"
card_blurb: "A hard-wired legacy shipment processor made testable with Extract Interface + Parameterize Constructor, Extract and Override Factory Method, Subclass and Override Method, and Adapt Parameter. Tests prove the now-testable behavior with Mockito doubles, ArgumentCaptor, and @TempDir."
---

Michael Feathers' dependency-breaking techniques, in Java. This module takes a
hard-wired legacy shipment processor and applies four techniques to make it
testable: Extract Interface plus Parameterize Constructor (a narrow collaborator a
test can replace), Extract and Override Factory Method, Subclass and Override
Method (to isolate `LocalDate.now()`), and Adapt Parameter (depend on an interface
you own, not a concrete file type).

The JUnit 5 tests prove the now-testable behavior with Mockito mocks, `verify`,
`ArgumentCaptor`, and real-file `@TempDir` cases.

## Build, run, observe

```sh
cd examples/30-dependency-breaking
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
