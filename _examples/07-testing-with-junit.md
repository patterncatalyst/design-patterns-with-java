---
layout: example
order: 7
title: "Testing with JUnit 5, Mockito, and AssertJ"
description: The test loop every example in this book runs, shown on a real shipping component.
github_path: examples/07-testing-with-junit
card_eyebrow: "Chapter 7 · Testing"
card_title: "JUnit 5, Mockito, and AssertJ"
card_blurb: "A shipping component under test demonstrating the book's testing stack: the JUnit 5 lifecycle, parameterized tests, exception assertions, Mockito mocks and spies with verify and ArgumentCaptor, and AssertJ fluent assertions, all aimed at behavior rather than implementation."
---

This module is the reference for how every example in the book proves its
behavior. A shipping component under test demonstrates **JUnit 5** (the lifecycle,
`@ParameterizedTest`, `assertThrows`, `@Nested`), **Mockito** (mocks, spies,
`verify`, `ArgumentCaptor`, and when to use a real collaborator instead), and
**AssertJ** (fluent `assertThat` chains that read better than a stack of bare
assertions). The tests double as the chapter's worked examples, and they assert
behavior rather than restating the implementation.

## Build, run, observe

```sh
cd examples/07-testing-with-junit
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
