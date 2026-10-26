# 21 - Records, sealed interfaces, and pattern matching: a shipping rating engine

A shipment moves through a closed set of parcel kinds and a closed set of
tracking events, and a rating engine prices it through a small, closed
grammar of rules. This example builds all three as sealed hierarchies of
records, then operates on each one with exhaustive pattern-matching
switches, never a visitor interface and never an `accept()` method.

## The domain

- `ParcelKind`: a sealed interface over `StandardParcel`, `FragileParcel`,
  `RefrigeratedParcel`, and `OversizedParcel`. Each is a record whose
  compact constructor validates its own invariant: a positive weight on
  every kind, a non-blank handling note on `FragileParcel`, a `minTempC`
  strictly below `maxTempC` on `RefrigeratedParcel`, and positive
  dimensions on `OversizedParcel`.
- `ShippingEvent`: a sealed interface over `QuoteRequested`,
  `LabelPurchased`, `HubScan`, `DeliveryException`, and `Delivered`. Each
  carries the `ParcelKind` it concerns.
- `RateRule`: a sealed interface over `FlatRate`, `PerKilogram`,
  `ZoneSurcharge`, and `MinimumCharge`, a small grammar for pricing a
  parcel that a caller builds by nesting these four kinds.
- `Money` tracks cents as a `long`, never a `double`, so a quote is exact.

## Records as data carriers

`ParcelKinds.billableWeightKg()` deconstructs all four `ParcelKind` records
directly in one exhaustive switch, computing an `OversizedParcel`'s
dimensional weight inline and falling back to the other three kinds' own
`weightKg` otherwise. No kind implements a shared method beyond the
interface's own `weightKg()` accessor; the operation lives entirely outside
every record.

## Sealed interfaces and pattern matching for switch

`ShippingEvents.describe()` deconstructs a `QuoteRequested` whose parcel is
specifically a `RefrigeratedParcel` two record layers deep in one nested
record pattern, then falls back to a plain type pattern for every other
combination. `ShippingEvents.requiresPriorityHandling()` binds a
`RefrigeratedParcel` by type inside a `DeliveryException` and narrows that
match further with a guarded `when` clause checking `maxTempC`, the only
event this rating engine treats as urgent enough to page a dispatcher.

## Record deconstruction patterns, nested three levels deep

`RateRules.quote()` recurses through whatever `RateRule` tree a caller
builds, with a guarded `when` clause deciding whether a `ZoneSurcharge`
applies at all. `RateRules.describe()` goes one step further: its first
case deconstructs a `MinimumCharge` wrapping a `ZoneSurcharge` wrapping a
`PerKilogram`, three levels, in a single nested record pattern, reading
`ratePerKg`, `zone`, `surcharge`, and `floor` out in one line with no
accessor call anywhere in that arm.

The identical three pieces, a $1.50/kg rate, a $22.00 `EAST`-zone
surcharge, and a $20.00 floor, nested two different ways price a 12kg
parcel to `EAST` at $40.00 when the surcharge sits inside the floor check,
and $42.00 when the floor check sits inside the surcharge. Nesting, not
just which pieces are present, decides the quote.

`ShippingDemo.java` exercises all three hierarchies against one shipment.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

This example is standalone-buildable from this directory:

```sh
cd examples/21-records-sealed-patterns
mvn test
```

It is also picked up by the repo root's aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 32 JUnit 5 cases (AssertJ assertions), including a
case that quotes two differently-nested `RateRule` trees built from
identical pieces and asserts they produce different results, not just that
each one evaluates without throwing.

To see all three hierarchies run against one shipment rather than just
reading an assertion that says they work, run the demo:

```sh
./demo.sh
```

Expected output:

```
== Records: validation runs once, in the compact constructor ==
Rejected: minTempC (10) must be less than maxTempC (4)

== ParcelKinds: billable weight, actual vs. dimensional ==
Refrigerated parcel billable weight: 8.0kg
Oversized parcel billable weight: 115.2kg

== ShippingEvents: describe(), including a nested record pattern ==
Quote requested for a 8.0kg refrigerated parcel (keep -5C to 4C) to zone EAST
Delivery exception: cold storage unit failed in transit (8.0kg)
Delivered (12.0kg)

== ShippingEvents: requiresPriorityHandling(), a guarded when clause ==
Cold-chain breach (ceiling 0C): true
Routine exception on a standard parcel: false

== RateRules: nesting the same rule pieces decides the quote ==
max($1.50/kg + $5.00 in zone EAST, floor $20.00)
Quote to EAST (surcharge applies, above the floor): $23.00
Quote to WEST (surcharge skipped, floor takes over): $20.00
```

The final block's two different totals, computed from the identical
`RateRule` tree quoted against two different destination zones, is a
guarded `when` clause changing the result, visible at the terminal rather
than only in an assertion.

## Files

```
examples/21-records-sealed-patterns/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run ShippingDemo
  src/main/java/com/patterncatalyst/designpatterns/ch21/shipping/
    Money.java                    # exact integer cents
    ParcelKind.java                 # sealed interface, the closed parcel set
    StandardParcel.java               # record, no extra validation
    FragileParcel.java                 # record, validates a handling note
    RefrigeratedParcel.java              # record, validates minTempC < maxTempC
    OversizedParcel.java                   # record, validates every dimension
    ParcelKinds.java                         # billable weight, exhaustive switch
    ShippingEvent.java                         # sealed interface, the closed event set
    QuoteRequested.java                          # event kind one
    LabelPurchased.java                            # event kind two
    HubScan.java                                     # event kind three
    DeliveryException.java                             # event kind four
    Delivered.java                                       # event kind five
    ShippingEvents.java                                    # describe + priority check
    RateRule.java                                            # sealed interface, the grammar
    FlatRate.java                                              # grammar leaf
    PerKilogram.java                                             # grammar leaf
    ZoneSurcharge.java                                             # grammar combinator
    MinimumCharge.java                                               # grammar combinator
    RateRules.java                                                     # quote + describe
    ShippingDemo.java                                                    # exercises all three hierarchies
  src/test/java/com/patterncatalyst/designpatterns/ch21/shipping/
    ParcelKindTest.java            # compact constructor validation
    ParcelKindsTest.java             # billable weight, every kind
    ShippingEventTest.java             # compact constructor validation
    ShippingEventsTest.java              # describe + priority handling
    RateRuleTest.java                      # compact constructor validation
    RateRulesTest.java                       # quote + describe, including nesting
```
