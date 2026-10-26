---
layout: example
order: 13
title: "Builder and Prototype: assembling a complex shipment"
description: A fluent builder that validates once at build time, records with wither copies, and a prototype cloned from an exemplar.
github_path: examples/13-builder-prototype
card_eyebrow: "Chapter 13 · Builder and Prototype"
card_title: "Assembling a complex shipment"
card_blurb: "A Shipment with many optional parts assembled through a static nested Builder that validates every invariant once at build(), a CustomsDeclaration record with wither copies, and a PackagingTemplate prototype cloned into independent copies. Tests prove invalid combinations are rejected and copies are independent."
---

Builder makes construction read as a sequence of choices and validates the result
once; Prototype makes a new object by copying a configured exemplar. This module
assembles a `Shipment` with many optional parts through a Bloch-style static
nested `Builder` (with a cross-field rule that an international destination
requires a customs declaration), uses a `record` with wither copies for a
`CustomsDeclaration`, and clones a `PackagingTemplate` exemplar into independent
copies.

The JUnit 5 tests prove the builder rejects an invalid combination and builds a
valid object, a wither produces an independent changed copy, and a prototype copy
is independent of its exemplar and its siblings.

## Build, run, observe

```sh
cd examples/13-builder-prototype
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
