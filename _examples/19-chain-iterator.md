---
layout: example
order: 19
title: "Chain of Responsibility and Iterator: approvals and manifests"
description: A shipment-approval chain that stops at the first handler, and a manifest traversed by a hand-written iterator and a stream.
github_path: examples/19-chain-iterator
card_eyebrow: "Chapter 19 · Chain of Responsibility and Iterator"
card_title: "Approvals and manifests"
card_blurb: "A shipment-approval chain (object chain and a functional composition over Optional) that stops at the first handler to take the request, and a ShipmentManifest that implements Iterable with a hand-written iterator plus a stream built on its spliterator. Tests verify routing, the fallback, and traversal order."
---

Two patterns about passing work along and walking a structure. **Chain of
Responsibility** sends an approval request down a line of handlers until one takes
it, shown as an object chain and as a functional composition of
`Function<Request, Optional<Outcome>>` resolved with
`Stream.flatMap(Optional::stream).findFirst()`. **Iterator** gives a
`ShipmentManifest` an `Iterable` implementation with a hand-written zone-order
iterator, plus a stream, and explains why you rarely hand-write an iterator now.

The JUnit 5 tests prove the chain routes to the correct handler and falls back when
none takes the request, and that the iterator and stream traverse in the same order.

## Build, run, observe

```sh
cd examples/19-chain-iterator
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
