---
layout: example
order: 9
title: "Template Method: a fixed order-processing skeleton"
description: One invariant sequence, varying steps supplied by a subclass or by lambdas, proven equivalent.
github_path: examples/09-template-method
card_eyebrow: "Chapter 9 · Template Method"
card_title: "A fixed order-processing skeleton"
card_blurb: "An order-processing pipeline whose sequence is fixed and final, with the varying steps supplied two ways: by overriding an abstract class per channel, and by handing the pipeline lambdas. Tests prove both forms run the same steps in the same order and produce identical results."
---

Template Method fixes the shape of an algorithm and lets the steps vary. This
module runs one order-processing sequence (precondition, validate, price, record)
two idiomatic ways: an **abstract-method skeleton** where each channel overrides
the hooks, and a **functional skeleton** where the varying steps arrive as
lambdas. Retail, wholesale, and marketplace channels are built both ways.

The JUnit 5 tests prove the sequence is invariant (a recording subclass checks
hook order; Mockito `InOrder` checks the functional form), that the skeleton
method is `final`, and that both forms produce an identical result per channel.

## Build, run, observe

```sh
cd examples/09-template-method
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
