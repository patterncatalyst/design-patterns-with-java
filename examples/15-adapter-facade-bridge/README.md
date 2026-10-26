# 15 - Adapter, Facade, Bridge: a GlobalCourier integration

One carrier integration, GlobalCourier, carried through three structural
techniques so each builds on the last instead of starting over in an
unrelated scenario.

## The domain

- `Shipment`: a `record` with `weightKg`, a `DestinationZone` (`LOCAL`,
  `REGIONAL`, `NATIONAL`, `INTERNATIONAL`), and `declaredValueCents`.
- `Money` tracks cents as a `long`, never a `double`, so a quote is exact.
- `globalcourier.GlobalCourierRateClient` is a stand-in for third-party SDK
  code: it speaks grams and numeric zone codes, not this app's own types, and
  it is `final`, the way a lot of real vendor SDKs ship.

## The three techniques

1. **Adapter** (`CarrierGateway.java`, `GlobalCourierGatewayAdapter.java`):
   the app's own `CarrierGateway` interface says nothing about GlobalCourier.
   `GlobalCourierGatewayAdapter` holds a `GlobalCourierRateClient` by
   composition and translates kilograms to grams and `DestinationZone` to a
   zone code on every call.
2. **Facade** (`ShippingFacade.java`): owns a `CarrierGateway`, a
   `LabelPrinter`, a `CustomsDeclarationService`, and a `TrackingRegistry`,
   and exposes exactly one method, `prepareShipment()`, that calls all four
   in the order the result depends on: a tracking number has to exist before
   the label can print it or the tracking URL can point at it.
3. **Bridge** (`Notification.java`, `DeliveryChannel.java`): `Notification`
   holds a `DeliveryChannel` by composition and delegates delivery to it.
   `ShipmentCreatedNotification` and `DeliveryDelayedNotification` refine
   what the message says; `EmailDeliveryChannel` and `SmsDeliveryChannel`
   refine how it gets delivered. Two kinds and two channels produce four
   working combinations with no class per combination.

`ShippingDemo.java` exercises all three techniques against one integration
and prints what each one produces.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

This example is standalone-buildable from this directory:

```sh
cd examples/15-adapter-facade-bridge
mvn test
```

It is also picked up by the repo root's aggregator once registered there:

```sh
mvn -f examples/pom.xml test
```

Either path runs 28 JUnit 5 cases (AssertJ assertions, Mockito for
collaborator verification), including a parameterized suite that asserts the
Adapter maps every destination zone to the correct legacy code, Mockito
`InOrder` verification that the Facade calls its subsystems in the order its
own result depends on, and a parameterized suite that crosses both
notification kinds with both delivery channels.

To see all three techniques produce real output rather than just reading an
assertion that says they work, run the example's own demo script:

```sh
./demo.sh
```

Expected output:

```
== Adapter: GlobalCourier's legacy SDK behind CarrierGateway ==
Adapted quote: $6.50

== Facade: one call fronts rating, labeling, customs, and tracking ==
quote:    $6.50
label:    GlobalCourier LABEL | weight=3.5kg | zone=REGIONAL | tracking=TRK-1001
tracking: https://track.globalcourier.example/track/TRK-1001
customs:  not required for this zone
customs:  General merchandise, declared value $450.00

== Bridge: two notification kinds, two channels, no class explosion ==
EMAIL to customer@example.com: Your shipment TRK-1001 has been created.
SMS to +1-555-0100: Your shipment TRK-1001 has been created.
EMAIL to customer@example.com: Shipment TRK-1001 is delayed by 6 hours.
SMS to +1-555-0100: Shipment TRK-1001 is delayed by 6 hours.
```

The two `customs:` lines come from the same `CustomsDeclarationService`
called on a `REGIONAL` shipment and then on an `INTERNATIONAL` one: the
service itself decides whether a declaration is needed, so the Facade never
branches on zone.

## Files

```
examples/15-adapter-facade-bridge/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run ShippingDemo
  src/main/java/com/patterncatalyst/designpatterns/ch15/shipping/
    DestinationZone.java          # LOCAL/REGIONAL/NATIONAL/INTERNATIONAL
    Shipment.java                 # record: weightKg, zone, declaredValueCents
    Money.java                    # record: exact integer cents
    globalcourier/
      GlobalCourierRateClient.java  # the foreign SDK the Adapter wraps
    CarrierGateway.java            # Adapter: the app's own target interface
    GlobalCourierGatewayAdapter.java # Adapter: object adapter implementation
    LabelPrinter.java               # Facade subsystem: label rendering
    TrackingRegistry.java           # Facade subsystem: tracking numbers/URLs
    CustomsDeclaration.java         # Facade subsystem: declaration value type
    CustomsDeclarationService.java  # Facade subsystem: declaration policy
    ShipmentManifest.java           # Facade: the one orchestrated result
    ShippingFacade.java             # Facade: prepareShipment()
    DeliveryChannel.java            # Bridge: implementation interface
    EmailDeliveryChannel.java       # Bridge: implementation
    SmsDeliveryChannel.java         # Bridge: implementation
    Notification.java               # Bridge: abstraction
    ShipmentCreatedNotification.java # Bridge: refined abstraction
    DeliveryDelayedNotification.java # Bridge: refined abstraction
    ShippingDemo.java                # exercises all three techniques
  src/test/java/com/patterncatalyst/designpatterns/ch15/shipping/
    GlobalCourierGatewayAdapterTest.java # unit conversion + zone mapping
    ShippingFacadeTest.java              # orchestration order + end-to-end
    CustomsDeclarationServiceTest.java   # declaration policy by zone
    TrackingRegistryTest.java            # number issuance + URL building
    NotificationBridgeTest.java          # kind x channel combinations
    ShipmentTest.java                    # value-type validation edge cases
    MoneyTest.java                       # exact-cents arithmetic
```
