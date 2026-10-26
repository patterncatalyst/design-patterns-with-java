# 22 - Enums, functional interfaces, streams, and Optional as patterns

A warehouse runs the same nightly fulfillment batch every night: price a
packaging surcharge, validate the order against its sales channel's rule,
price the shipment, assign it a manifest number, and notify whoever is
watching. This example builds that batch entirely out of everyday Java 25
constructs, enums, lambdas, method references, streams, and `Optional`, to
show each one standing in for a classic Gang of Four pattern with no class
hierarchy in sight.

## The domain

- `Shipment`: a `record` with a weight, a `DestinationZone`, a
  `PackagingTier`, and a declared value in cents.
- `Money` tracks cents as a `long`, never a `double`, so a price is exact.
- `FulfillmentRecord`: the outcome of running a shipment through the
  pipeline, with a plain `String manifestId` (empty when rejected) and a
  plain `String rejectionReason` (empty when accepted), never
  `Optional<String>` fields.

## The expressions

1. **Enum carrying behavior** (`PackagingTier.java`): `STANDARD`,
   `FRAGILE`, and `OVERSIZED` each implement the `SurchargeRule` functional
   interface through a constant-specific method body, the Strategy pattern
   as a closed set of three constants.
2. **Enum Singleton** (`ManifestSequence.java`): a single `INSTANCE`
   constant owns the warehouse's one manifest counter. The language
   guarantees exactly one instance exists, with no second constructor path
   a caller or a deserializer could use to produce a second counter.
3. **A lambda satisfying the same functional interface**
   (`PromotionalSurcharges.java`): ad hoc, campaign-scoped surcharges built
   as one-expression lambdas against the identical `SurchargeRule` contract
   `PackagingTier`'s constants implement, plus a `combined()` helper that
   composes two rules from either source into a third.
4. **Command as a custom functional interface and as `Runnable`**
   (`NotificationCommand.java`, `FulfillmentNotifier.java`,
   `BatchCloseout.java`): a queue of notification commands satisfied by
   method references (`log::recordEmail`, `log::recordSms`), and a
   zero-argument closeout step that needs nothing more than the platform's
   own `Runnable`.
5. **Template Method as a function** (`FulfillmentPipeline.java`): one fixed
   sequence, validate, price, assign a manifest number, with the two
   varying steps supplied as `Function` parameters per sales channel and
   the invariant step never exposed as a parameter at all.
6. **Composed pricing adjustments** (`PricingAdjustments.java`): a discount
   and a flat handling fee as `UnaryOperator<Money>` values, chained with
   `andThen` and `compose`, which do not commute.
7. **Streams replacing hand-written loops** (`FulfillmentBatch.java`): a
   sum, a filter, and two `Collectors.groupingBy` pipelines built on an
   `EnumMap` so a tier's declaration order, not its hash code, decides the
   iteration order.
8. **Optional as a return type** (`ZoneRateLookup.java`): a rate table that
   does not cover every zone, returning `Optional<Money>` instead of `null`
   or a sentinel amount. The class stores a plain `Map` field; only the
   lookup method's return type is `Optional`.

`FulfillmentDemo.java` exercises every expression above against one nightly
batch and prints what each one produces.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

This example is standalone-buildable from this directory:

```sh
cd examples/22-functional-java
mvn test
```

It is also picked up by the repo root's aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 47 JUnit 5 cases (AssertJ assertions), including
behavior checks for every expression above, not just a smoke test that each
one runs without throwing. Observed locally on Fedora with Temurin 25.0.2:

```
Tests run: 47, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see every expression run against the same nightly batch rather than just
reading an assertion that says it behaves correctly, run the demo:

```sh
./demo.sh
```

Expected output:

```
== Enum carrying behavior: PackagingTier surcharges ==
RETAIL-1001 (STANDARD): $0.00 surcharge
RETAIL-1002 (FRAGILE): $4.10 surcharge
WHOLESALE-2001 (OVERSIZED): $35.25 surcharge
WHOLESALE-2002 (STANDARD): $0.00 surcharge
RETAIL-1003 (STANDARD): $0.00 surcharge

== Lambda satisfying the same SurchargeRule contract ==
Peak-season surcharge on RETAIL-1002 alone: $0.38
Peak-season surcharge combined with FRAGILE handling: $4.48

== Enum Singleton: one manifest counter for the whole program ==
Manifests issued so far: 0

== Template Method as a function: validate, price, assign a manifest ==
RETAIL-1001 -> MANIFEST-000001, total $6.50
RETAIL-1002 -> rejected: retail orders cap declared value at $500.00
WHOLESALE-2001 -> MANIFEST-000002, total $45.45
WHOLESALE-2002 -> rejected: wholesale orders require at least 20kg
RETAIL-1003 -> MANIFEST-000003, total $3.00
Manifests issued after the batch: 3

== Command: method references and a Runnable ==
Notification log after dispatch: [email:RETAIL-1001, sms:RETAIL-1001]
Batch closeout ran as a plain Runnable.
Manifest closed flag: true

== Function composition: andThen vs. compose do not commute ==
Discount then fee (andThen):  $95.00
Fee then discount (compose):  $94.50

== Streams: declarative pipelines over the batch ==
Total surcharge cents for the batch: 3935
Shipments needing extra handling: [RETAIL-1002, WHOLESALE-2001]
Surcharge cents by tier: {STANDARD=0, FRAGILE=410, OVERSIZED=3525}
Shipment count by tier: {STANDARD=3, FRAGILE=1, OVERSIZED=1}

== Optional: an honest answer for a zone with no base rate ==
LOCAL base rate: $3.00
INTERNATIONAL base rate: not priced
```

Two shipments are rejected before a manifest number is ever assigned, which
is why only three manifests are issued for a batch of five. The
`andThen`/`compose` block prices the same starting amount two different
ways to make the point that a discount applied before a flat fee never
discounts that fee.

## Files

```
examples/22-functional-java/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run FulfillmentDemo
  src/main/java/com/patterncatalyst/designpatterns/ch22/fulfillment/
    DestinationZone.java          # plain value enum, no behavior
    Money.java                     # record: exact integer cents
    Shipment.java                   # record: weight, zone, tier, declared value
    SurchargeRule.java               # the functional interface enum + lambda share
    PackagingTier.java                # enum carrying behavior (Strategy)
    PromotionalSurcharges.java         # lambdas satisfying the same contract
    ManifestSequence.java               # enum Singleton
    NotificationCommand.java             # Command as a functional interface
    NotificationLog.java                  # method-reference targets
    FulfillmentNotifier.java               # Command queue
    BatchCloseout.java                      # Command as plain Runnable
    PricingAdjustments.java                  # composed UnaryOperator<Money>
    ZoneRateLookup.java                       # Optional as a return type
    FulfillmentRecord.java                     # outcome record (no Optional fields)
    FulfillmentRules.java                       # per-channel validate/price steps
    FulfillmentPipeline.java                     # Template Method as a function
    FulfillmentBatch.java                         # stream pipelines
    FulfillmentDemo.java                           # exercises every expression
  src/test/java/com/patterncatalyst/designpatterns/ch22/fulfillment/
    MoneyTest.java
    ShipmentTest.java
    PackagingTierTest.java
    SurchargeRuleTest.java
    ManifestSequenceTest.java
    FulfillmentNotifierTest.java
    BatchCloseoutTest.java
    PricingAdjustmentsCompositionTest.java
    ZoneRateLookupTest.java
    FulfillmentRulesTest.java
    FulfillmentPipelineTest.java
    FulfillmentRecordTest.java
    FulfillmentBatchTest.java
```
