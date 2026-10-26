# 29 - Seams and the test safety net

A shipment escalation service and a risk scorer, both inherited exactly as
found: the escalation service reads the wall clock directly and constructs
its own carrier notifier inline, and the risk scorer reaches into a global,
process-wide calendar singleton. Neither hard dependency has a way in for a
test to substitute a double. This example gets both under test with three
different seams before changing anything about what they do.

## The domain

- `Shipment`: a `record` with a tracking `id`, a `promisedDelivery` instant,
  and a `weightKg`.
- `EscalationResult`: a `record` carrying the shipment, whether it was
  escalated, and the reason.
- `CarrierGateway`: the extract-interface seam `ShipmentEscalationService`
  depends on; `SmtpCarrierGateway` is the only concrete implementation
  production code ever constructs.
- `EnterpriseCalendar`: a package-private global dependency with no object
  and no constructor, the hard dependency the risk scorer's subclass-and-override
  seam is built around.

## Before: no seam

- `LegacyShipmentEscalationService` reads `Instant.now()` inline and
  constructs a `new SmtpCarrierGateway()` inside `escalate()`. A
  characterization test can pin wide-margin cases (a shipment a day overdue,
  a shipment a day from its deadline) but cannot reach the exact boundary
  instant, and cannot verify that a notification happened beyond reading the
  result's reason string, because `escalate()` never hands out the gateway
  it built.
- `LegacyShipmentRiskScorer` reaches directly into
  `EnterpriseCalendar.isPeakSeason()`. A characterization test has to call
  the same global dependency itself just to know what to expect, which only
  proves the test agrees with whatever the calendar says today, not that the
  surcharge logic is correct in both seasons.

## After: three seams opened

1. **Object seam** (`ShipmentEscalationService`): the wall-clock read becomes
   a `java.time.Clock` the constructor takes. Production wires in
   `Clock.systemUTC()`; a test wires in `Clock.fixed(...)` to pin the exact
   boundary instant, one nanosecond before it, and one nanosecond after it.
2. **Extract-interface seam** (`ShipmentEscalationService` again): the
   notifier becomes a `CarrierGateway` the constructor takes instead of
   constructing one inline. A test substitutes a Mockito mock and verifies
   `notifyCarrier()` was called, or never called, directly.
3. **Subclass-and-override seam** (`ShipmentRiskScorer`): the one call into
   `EnterpriseCalendar.isPeakSeason()` moves into a protected
   `isHighRiskWindow()` method. A test subclass overrides that one method to
   force both branches on demand, with no constructor change and no
   extracted interface.

`ShipmentEscalationService` also corrects a boundary bug the legacy
comparison hid: `now.isAfter(overdueAt)` treated the exact instant a grace
period elapses as still on time. `!now.isBefore(overdueAt)` fixes it. A
wide-margin equivalence test against the legacy service proves the seam
alone changed nothing; the exact-boundary tests pin the corrected behavior
precisely.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

This example is standalone-buildable from this directory:

```sh
cd examples/29-seams
mvn test
```

It is also picked up by the repo root's aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 16 JUnit 5 cases: three legacy characterization
tests for the escalation service, two for the risk scorer, four seam tests
against the escalation service (one wide-margin equivalence check, three
exact-boundary cases with Mockito verifying the gateway interaction), three
subclass-and-override tests against the risk scorer, plus the value-type and
gateway-logging tests.

To see the legacy and seamed escalation services, and the risk scorer's
production wiring, run side by side rather than just reading an assertion
that says they agree, run the example's own demo script:

```sh
./demo.sh
```

Expected output (the escalated/not-escalated split and the gateway log
depend on the clock reading at run time; the exact timestamps will differ):

```
== Legacy service: hard-wired clock and gateway, no seam ==
legacy SHM-7001: EscalationResult[shipment=Shipment[id=SHM-7001, ...], escalated=true, reason=promised delivery window missed by more than 2h]
legacy SHM-7002: EscalationResult[shipment=Shipment[id=SHM-7002, ...], escalated=false, reason=within the promised delivery window]

== Seamed service: Clock and CarrierGateway injected through the constructor ==
seamed SHM-7001: EscalationResult[shipment=Shipment[id=SHM-7001, ...], escalated=true, reason=promised delivery window missed by more than 2h]
seamed SHM-7002: EscalationResult[shipment=Shipment[id=SHM-7002, ...], escalated=false, reason=within the promised delivery window]
gateway log: [SHM-7001: promised delivery window missed by more than 2h]

== Risk scorer: subclass-and-override seam ==
production risk score for a heavy shipment (today's calendar): 40
```

The legacy and seamed services agree on both shipments, which is the seam's
behavior-preservation proof showing up at the terminal, not just in a
passing test. The gateway log line is new: the legacy service never hands
out a gateway reference for anything outside it to inspect.

## Files

```
examples/29-seams/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run ShipmentEscalationDemo
  src/main/java/com/patterncatalyst/designpatterns/ch29/shipping/
    Shipment.java                      # record: id, promisedDelivery, weightKg
    EscalationResult.java               # record: shipment, escalated, reason
    CarrierGateway.java                  # the extract-interface seam
    SmtpCarrierGateway.java               # the only production implementation
    LegacyShipmentEscalationService.java   # before: hard-wired clock + gateway
    ShipmentEscalationService.java          # after: Clock + CarrierGateway seams
    EnterpriseCalendar.java                  # the global dependency with no seam
    LegacyShipmentRiskScorer.java             # before: calls the global directly
    ShipmentRiskScorer.java                    # after: subclass-and-override seam
    ShipmentEscalationDemo.java                 # exercises all three
  src/test/java/com/patterncatalyst/designpatterns/ch29/shipping/
    LegacyShipmentEscalationServiceCharacterizationTest.java  # pins the legacy behavior
    ShipmentEscalationServiceTest.java                         # object + extract-interface seams
    LegacyShipmentRiskScorerCharacterizationTest.java           # pins the legacy behavior
    ShipmentRiskScorerTest.java                                  # subclass-and-override seam
    ShipmentTest.java                                             # value-type invariants
    SmtpCarrierGatewayTest.java                                    # gateway logging
```
