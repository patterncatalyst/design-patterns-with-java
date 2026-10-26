# 30 - Dependency-breaking techniques for Java: a hard-wired shipment dispatcher

`LegacyShipmentProcessor` prices a shipment, decides whether it qualifies
for the express lane, and notifies operations by email, exactly what the
inherited version of this class always did. Before this chapter's pass, it
built a concrete mail dependency inside its own constructor, constructed a
surcharge calculator inline, read the wall clock directly, and took a
heavyweight manifest-file parameter whose constructor opens a real file.
None of that logic was a bug; all four shapes made the class impossible to
unit test without a real SMTP server, a real clock, or a real file on
disk. This example applies four of Feathers' dependency-breaking
techniques to open a seam at each dependency, with every existing call
site still compiling.

## The domain

- `Shipment` and `ShipmentDispatchResult` are the value types `dispatch`
  takes and returns.
- `NotificationGateway` is the interface Extract Interface pulls out of the
  mail dependency; `SmtpNotificationGateway` is the production
  implementation.
- `SurchargeCalculator` is the interface Extract and Override Factory
  Method opens around the embedded `new StandardSurchargeCalculator()`
  call; `StandardSurchargeCalculator` is the real eight-percent rule.
- `ManifestSource` is the narrow interface Adapt Parameter extracts;
  `CarrierManifestFile` is the untouched heavyweight legacy type, and
  `CarrierManifestFileAdapter` wraps it to satisfy the narrow contract.

## Four techniques, one class

- **Extract Interface + Parameterize Constructor.** The inherited
  single-argument constructor still exists and still builds a real
  `SmtpNotificationGateway`, by forwarding to a new two-argument
  constructor. Only a test, or `ShipmentDispatchDemo`, calls the
  two-argument form directly, supplying a double instead of a real mail
  dependency.
- **Extract and Override Factory Method.** `createSurchargeCalculator()`
  replaces the embedded `new StandardSurchargeCalculator()` inside
  `dispatch`. `LegacyShipmentProcessorSurchargeTest` subclasses the
  processor to return a Mockito double instead, with `dispatch`'s own
  logic completely unchanged.
- **Subclass and Override Method.** `today()` replaces the inline
  `LocalDate.now()` read inside the express-lane check.
  `LegacyShipmentProcessorExpressLaneTest` subclasses the processor to
  return a fixed date, proving the weekday rule with an exact assertion
  instead of a test that only passes on five days out of seven.
- **Adapt Parameter.** `summarizeManifest` takes a `ManifestSource` instead
  of a `CarrierManifestFile`. `LegacyShipmentProcessorManifestTest` drives
  the method entirely through a Mockito double with no filesystem access,
  and `CarrierManifestFileAdapterTest` confirms the adapter is a faithful
  stand-in for a real manifest file.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

Standalone from this directory:

```sh
cd examples/30-dependency-breaking
mvn test
```

Also picked up by the repo root's aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 21 JUnit 5 cases (AssertJ assertions, Mockito for
collaborator doubles and verification).

To see all four techniques run against production wiring at the terminal,
run the example's own demo script:

```sh
./demo.sh
```

Expected output:

```
== Extract Interface + Parameterize Constructor: LegacyShipmentProcessor ==
[smtp:mail.internal.example] to=ops-supervisor@example.com "Shipment SH-7001 dispatched, total $129.60"
  recorded (no real SMTP call): to=ops-supervisor@example.com "Shipment SH-7002 dispatched, total $86.40"

== Extract and Override Factory Method: surcharge calculator ==
[smtp:mail.internal.example] to=ops-supervisor@example.com "Shipment SH-7003 dispatched, total $108.00"
  live surcharge (8%): 10800 cents
  overridden flat-rate surcharge: 10500 cents

== Subclass and Override Method: express-lane clock ==
  Tuesday dispatch express eligible: true
  Saturday dispatch express eligible: false

== Adapt Parameter: manifest summary ==
  fake source, no file read: FASTFREIGHT: 275 package(s)
  real file via adapter: SLOWHAUL: 48 package(s)
```

The second line of the first block is the chapter's argument in one run:
`testStyleProcessor` is constructed with a recording lambda instead of a
real `SmtpNotificationGateway`, and `dispatch` runs its identical logic,
formatting the identical message, with nothing in `LegacyShipmentProcessor`
aware that anything changed.

## Files

```
examples/30-dependency-breaking/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run ShipmentDispatchDemo
  src/main/java/com/patterncatalyst/designpatterns/ch30/shipping/
    Shipment.java                     # record: id, baseCostCents
    ShipmentDispatchResult.java        # record: shipmentId, totalCostCents, expressLaneEligible
    NotificationGateway.java            # Extract Interface target
    SmtpNotificationGateway.java         # production implementation
    SurchargeCalculator.java              # Extract and Override Factory Method target
    StandardSurchargeCalculator.java       # production 8% rule
    ManifestSource.java                     # Adapt Parameter target
    CarrierManifestFile.java                 # untouched heavyweight legacy type
    CarrierManifestFileAdapter.java            # adapts the heavyweight type to ManifestSource
    LegacyShipmentProcessor.java                # the class this chapter makes testable
    ShipmentDispatchDemo.java                    # exercises all four techniques
  src/test/java/com/patterncatalyst/designpatterns/ch30/shipping/
    ShipmentTest.java                       # value-type validation edge cases
    StandardSurchargeCalculatorTest.java     # the production surcharge rule
    SmtpNotificationGatewayTest.java          # construction and sending
    LegacyShipmentProcessorNotificationTest.java  # Extract Interface + Parameterize Constructor
    LegacyShipmentProcessorSurchargeTest.java      # Extract and Override Factory Method
    LegacyShipmentProcessorExpressLaneTest.java     # Subclass and Override Method
    LegacyShipmentProcessorManifestTest.java         # Adapt Parameter
    CarrierManifestFileAdapterTest.java               # adapter fidelity against a real file
```
