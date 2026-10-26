---
layout: example
order: 15
title: "Adapter, Facade, Bridge: composing a shipping subsystem"
description: Wrapping a foreign carrier client, presenting one surface over a subsystem, and splitting an abstraction from its implementation.
github_path: examples/15-adapter-facade-bridge
card_eyebrow: "Chapter 15 · Adapter, Facade, Bridge"
card_title: "Composing a shipping subsystem"
card_blurb: "An Adapter wrapping a foreign courier SDK to the app's CarrierGateway, a Facade presenting one surface over rating, labeling, customs, and tracking, and a Bridge letting notification kinds and delivery channels vary independently. Tests verify the translation, the orchestration order, and the N+M (not N×M) combination."
---

Three structural patterns that compose existing parts. **Adapter** wraps a foreign
courier SDK so it satisfies the app's `CarrierGateway` interface. **Facade**
presents one small surface over a four-part subsystem (rating, labeling, customs,
tracking). **Bridge** lets notification kinds and delivery channels vary
independently, so a new kind or a new channel is one new class, not one per
combination.

The JUnit 5 tests verify the adapter's unit translation per zone (Mockito mocks
the foreign client), the facade's subsystem call order (`InOrder`) plus end-to-end
domestic vs international behavior, and the bridge's kind-by-channel matrix.

## Build, run, observe

```sh
cd examples/15-adapter-facade-bridge
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
