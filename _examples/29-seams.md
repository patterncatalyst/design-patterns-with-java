---
layout: example
order: 29
title: "Seams and the test safety net"
description: Getting hard-wired inherited shipping code under test by opening object, interface, and subclass seams.
github_path: examples/29-seams
card_eyebrow: "Chapter 29 · Seams"
card_title: "Getting inherited code under test"
card_blurb: "A legacy escalation service that reads the wall clock inline and news its own carrier client, pinned with characterization tests, then opened at three seams: an injected Clock (object seam), an extracted CarrierGateway (interface seam with a Mockito double), and a protected overridable method (subclass-and-override). Tests hit exact boundary cases."
---

A seam is a place to change behavior without editing in place. This module takes a
legacy shipment-escalation service (hard-wired `Instant.now()` and a `new`
carrier client) and gets it under test: a characterization test pins current
behavior, then three seams make it substitutable, an injected `Clock` (object
seam), an extracted `CarrierGateway` interface (with a Mockito double), and a
protected method opened for subclass-and-override where there is no constructor to
change.

The JUnit 5 + Mockito tests use `Clock.fixed` for exact-boundary cases and verify
the gateway interactions.

## Build, run, observe

```sh
cd examples/29-seams
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
