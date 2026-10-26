# 19 - Chain of Responsibility and Iterator: shipment approval and a zone-bucketed manifest

A shipment isn't approved with one check. It has to clear a weight limit, a
hazmat clearance list, and an embargo list, and anything left over goes to a
human reviewer. A warehouse manifest has a different problem: it sorts
packages into one bucket per destination zone as they arrive, and a caller
listing every shipment shouldn't need to know buckets exist. This example
builds both patterns against that domain, each one two idiomatic Java ways.

## The domain

- `approval/`: `ApprovalRequest` (weight, hazmat flag, destination country)
  and `ApprovalOutcome`, a sealed interface of `Rejected` and
  `NeedsManualReview`. Nothing in this domain is auto-approved; a request
  either fails a check or reaches a human.
- `manifest/`: `Shipment` (tracking number, `DestinationZone`, weight) and
  `ShipmentManifest`, which stores shipments in four zone buckets, not one
  flat list.

## Chain of Responsibility: two expressions

1. **An object chain** (`ApprovalHandler`, `WeightLimitHandler`,
   `HazmatClearanceHandler`, `EmbargoHandler`, `ManualReviewHandler`,
   `ApprovalChains`): each handler's `attempt()` either returns a decision
   or `Optional.empty()` to pass to the next handler. `handle()` is
   `final`, so every handler gets the same call-logging and delegation
   behavior automatically. `ManualReviewHandler` is the fallback with no
   `next`, guaranteeing the chain always reaches a decision.
2. **A functional composition** (`ApprovalChecks`, `FunctionalApprovalChain`):
   the identical three rules as `Function<ApprovalRequest,
   Optional<ApprovalOutcome>>` values, tried in order by a Stream pipeline
   that stops at the first non-empty result. No handler objects, no `next`
   references, nothing to construct beyond three function values and a list.

Both forms stop at the first rule that fires and agree on the same
rejection reason for the same request, which
`FunctionalApprovalChainTest.agreesWithTheObjectChainForTheSameRequest`
checks directly.

## Iterator: two expressions over the same manifest

1. **A hand-written `Iterable`/`Iterator`** (`ShipmentManifest`,
   `ZoneOrderIterator`): `ZoneOrderIterator` flattens the four zone
   buckets into one forward traversal, skipping empty buckets as it goes.
   A caller holding this iterator cannot tell the manifest isn't one flat
   list.
2. **A Stream, with no extra code** (`ShipmentManifest.stream()`): built on
   `Iterable`'s default `spliterator()` method, with no bucket-aware code
   of its own. `filter`/`map`/`collect` compose over the manifest exactly
   as they would over any `List`.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

Standalone from this directory:

```sh
cd examples/19-chain-iterator
mvn test
```

Also picked up by the repo root's aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 15 JUnit 5 cases (AssertJ assertions): the object
chain stops at the correct handler and logs exactly the handlers consulted,
the functional chain agrees with the object chain on the same request, and
the manifest's iterator and Stream traversal produce the identical
zone-ordered sequence.

To see both chain forms route four shipments and both iterator forms walk
the same manifest, run the demo:

```sh
./demo.sh
```

Expected output:

```
== Chain of Responsibility: the object chain ==
US: Rejected by WeightLimitHandler -- shipment weight 55.0 kg exceeds the 40.0 kg limit
  chain visited: WeightLimitHandler
FR: Rejected by HazmatClearanceHandler -- hazmat shipments are not cleared for FR
  chain visited: WeightLimitHandler -> HazmatClearanceHandler
KP: Rejected by EmbargoHandler -- destination KP is under embargo
  chain visited: WeightLimitHandler -> HazmatClearanceHandler -> EmbargoHandler
DE: Needs manual review by ManualReviewHandler -- routed to the manual review queue
  chain visited: WeightLimitHandler -> HazmatClearanceHandler -> EmbargoHandler -> ManualReviewHandler

== Chain of Responsibility: the functional composition ==
US: Rejected by weightLimit -- shipment weight 55.0 kg exceeds the 40.0 kg limit
FR: Rejected by hazmatClearance -- hazmat shipments are not cleared for FR
KP: Rejected by embargo -- destination KP is under embargo
DE: Needs manual review by functionalChain -- no check reached a decision

== Iterator: a hand-written Iterator over a zone-bucketed manifest ==
Inserted INTL, LOC, NAT, NAT, REG order; the Iterator still visits zone order:
  TRK-LOC-1
  TRK-REG-1
  TRK-NAT-1
  TRK-NAT-2
  TRK-INTL-1

== Iterator: the same data through a Stream pipeline ==
National-zone weights (kg): [5.0, 7.5]
```

Notice the `chain visited` line growing by exactly one handler per request:
one for the overweight US shipment, two for the hazmat shipment to France,
three for the embargoed shipment to North Korea, and all four only for the
clean shipment to Germany. That is the chain stopping at the first handler
that acts, shown as an observable fact rather than inferred from the final
decision alone. The manifest section shows the companion fact: shipments
inserted in International, Local, National, National, Regional order come
back out in zone order, because the iterator walks buckets by zone
regardless of insertion order, and the Stream traversal agrees with it
exactly.

## Files

```
examples/19-chain-iterator/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run ChainIteratorDemo
  src/main/java/com/patterncatalyst/designpatterns/ch19/shipping/
    ChainIteratorDemo.java         # exercises both halves of the example
    approval/
      ApprovalRequest.java          # record: weightKg, hazmat, destinationCountry
      ApprovalOutcome.java           # sealed interface: Rejected, NeedsManualReview
      ApprovalHandler.java            # object-chain base class (handle/attempt)
      WeightLimitHandler.java          # expression 1: weight check
      HazmatClearanceHandler.java       # expression 1: hazmat clearance check
      EmbargoHandler.java                # expression 1: embargo check
      ManualReviewHandler.java            # expression 1: the fallback
      ApprovalChains.java                  # wires the object chain together
      ApprovalChecks.java                   # expression 2: the same rules as functions
      FunctionalApprovalChain.java           # expression 2: Stream-driven chain
    manifest/
      DestinationZone.java                    # LOCAL/REGIONAL/NATIONAL/INTERNATIONAL
      Shipment.java                             # record: trackingNumber, zone, weightKg
      ShipmentManifest.java                      # Iterable + hand-written Iterator + stream()
  src/test/java/com/patterncatalyst/designpatterns/ch19/shipping/
    approval/
      ApprovalChainTest.java                      # object chain: routing + call log
      FunctionalApprovalChainTest.java              # functional chain + cross-check
      ApprovalRequestTest.java                       # value-type validation
    manifest/
      ShipmentManifestTest.java                       # iterator + stream traversal
      ShipmentTest.java                                 # value-type validation
```
