---
layout: example
order: 5
title: "Resource management: try-with-resources and Execute-Around"
description: Deterministic cleanup in Java, proven to run on every path including exceptions.
github_path: examples/05-resource-management
card_eyebrow: "Chapter 5 · Resource management"
card_title: "try-with-resources and Execute-Around"
card_blurb: "Shipping resources that implement AutoCloseable and release deterministically through try-with-resources, plus an Execute-Around method that owns setup and teardown around a caller's lambda. Tests prove close() runs on the normal path and when an exception unwinds the block."
---

Java has no deterministic destructors, so cleanup is a language-structured
concern. This module shows the two tools that give the same guarantee RAII gives
in other languages: **try-with-resources** over an `AutoCloseable`, and the
**Execute-Around Method** idiom, where a library method owns the setup and the
guaranteed teardown around a caller's lambda.

Shipping-domain resources (a carrier connection, a batch ledger writer, a
tracking-feed handle) implement `AutoCloseable`. The JUnit 5 tests prove the
release runs on the normal path and when an exception unwinds the block, using a
Mockito spy to confirm `close()` is called exactly once.

## Build, run, observe

```sh
cd examples/05-resource-management
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
