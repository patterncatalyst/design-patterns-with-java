---
layout: example
order: 14
title: "Singleton and the DI alternative"
description: The enum and holder singletons, and why injecting the collaborator usually beats reaching for a global.
github_path: examples/14-singleton
card_eyebrow: "Chapter 14 · Singleton and DI"
card_title: "Singleton and the DI alternative"
card_blurb: "A carrier rate table as an enum singleton and as an initialization-on-demand holder, a job that reaches a global instance as a hidden dependency, and the dependency-injected alternative that takes the same collaborator through its constructor. Tests show why injection is testable where the global is not."
---

Singleton gives exactly one instance, but a global one is a hidden dependency that
fights testing. This module shows the modern Java forms, the **enum singleton**
(serialization- and reflection-safe) and the **initialization-on-demand holder**
idiom (lazy, thread-safe by class-initialization semantics, no double-checked
locking), and then makes the real argument: prefer **dependency injection**, where
the same capability arrives through a constructor a composition root controls.

The JUnit 5 tests prove the enum and holder each yield one instance (including a
reflection test of the enum's safety and a multi-thread race on the holder), and a
Mockito-backed test shows the injected service is substitutable where a global
would not be.

## Build, run, observe

```sh
cd examples/14-singleton
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
