---
title: "Adapter, Facade, Bridge"
order: 15
layout: tutorial
sectionid: docs
part: "composing-adapting-structure"
description: "Three structural techniques taught against one GlobalCourier shipping integration: an Adapter that wraps a third-party rate client's awkward shape behind this app's own interface, a Facade that fronts rating, labeling, customs declaration, and tracking with one call, and a Bridge that lets a notification's kind and its delivery channel vary independently."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Tell Adapter, Facade and Bridge apart by the problem each one solves.
- Wrap a third-party client behind your own interface with an object adapter.
- Put a multi-step sequence behind one Facade method so callers can't get the order wrong.
- Split a notification's kind from its delivery channel with a Bridge, so each new kind or channel is one class.
- Recognize when a direct call or a plain field already does the job.

## Why this matters

A checkout flow needs a shipping quote, a label, a customs declaration and a tracking number. The quote comes from a carrier SDK with its own units and method names. The four steps must run in order, or a label prints before its tracking number exists. After the shipment leaves, the customer gets a message that depends on what happened, over a channel that depends on what they prefer.

Each of those is a structural problem: giving an awkward class a usable shape, giving callers one thing to call, and letting two things vary without multiplying classes. Adapter, Facade and Bridge solve them in that order, all against one carrier integration, GlobalCourier.

{% include excalidraw.html name="15-adapter-facade-bridge-forces" caption="Figure 15.1: Adapter wraps a legacy rate client behind the app's own CarrierGateway interface, Facade fronts four shipping subsystems with one call, and Bridge crosses two notification kinds with two delivery channels without a class per combination." %}

`Shipment` is the value every technique receives. Its compact constructor validates the fields once, so nothing downstream re-checks them:

```java
public record Shipment(double weightKg, DestinationZone zone, long declaredValueCents) {

    public Shipment {
        Objects.requireNonNull(zone, "zone");
        if (weightKg < 0.0) {
            throw new IllegalArgumentException("weightKg cannot be negative: " + weightKg);
        }
        if (declaredValueCents < 0) {
            throw new IllegalArgumentException("declaredValueCents cannot be negative: " + declaredValueCents);
        }
    }
}
```

## Pattern: Adapter

### The problem

`GlobalCourierRateClient` stands in for the carrier's own SDK, and you can't redesign it. Weight travels in grams, the destination as a numeric zone code, and the quote comes back as a bare `long` instead of the app's `Money`:

```java
public final class GlobalCourierRateClient {

    private final long surchargeCents;

    public GlobalCourierRateClient(long surchargeCents) {
        this.surchargeCents = surchargeCents;
    }

    // zoneCode: 0 = local, 1 = regional, 2 = national, 3 = international.
    public long rateCentsForParcel(long weightGrams, int zoneCode) {
        long baseCents = weightGrams / 10 + (zoneCode * 150L);
        return baseCents + surchargeCents;
    }
}
```

Threading grams, zone codes and raw cents through checkout ties every caller to one vendor.

### The solution

An **Adapter** converts the interface of an existing class into the interface its clients expect.

Use an Adapter when:

- a class you don't control has the right behavior but the wrong shape;
- you want callers to depend on your interface so the vendor can change.

### How Java models it

`CarrierGateway` is the target interface. It says nothing about GlobalCourier, grams or zone codes:

```java
public interface CarrierGateway {
    Money quote(Shipment shipment);
}
```

`GlobalCourierGatewayAdapter` implements it and translates on every call, kilograms to grams and `DestinationZone` to a zone code:

```java
public final class GlobalCourierGatewayAdapter implements CarrierGateway {

    private final GlobalCourierRateClient legacyClient;

    public GlobalCourierGatewayAdapter(GlobalCourierRateClient legacyClient) {
        this.legacyClient = Objects.requireNonNull(legacyClient, "legacyClient");
    }

    @Override
    public Money quote(Shipment shipment) {
        long weightGrams = Math.round(shipment.weightKg() * 1000.0);
        int zoneCode = zoneToLegacyCode(shipment.zone());
        return Money.ofCents(legacyClient.rateCentsForParcel(weightGrams, zoneCode));
    }

    private static int zoneToLegacyCode(DestinationZone zone) {
        return switch (zone) {
            case LOCAL -> 0;
            case REGIONAL -> 1;
            case NATIONAL -> 2;
            case INTERNATIONAL -> 3;
        };
    }
}
```

Swap in another carrier's adapter and every caller of `CarrierGateway.quote()` keeps working unchanged. Because `CarrierGateway` has one abstract method, a lambda can stand in for it wherever only the rate matters.

### Object adapter, not class adapter

The `legacyClient` field makes this an object adapter: it holds the vendor client instead of extending it.

| | Object adapter (composition) | Class adapter (`extends` the vendor class) |
| --- | --- | --- |
| `final` vendor class | Works | Doesn't compile |
| Public surface | Only `CarrierGateway` | Every public method of the vendor class |
| Second vendor behind the same interface | Add a field or a second adapter | No second superclass to extend |
| Adapter's own superclass | Still free | Spent on the vendor |

Java allows one superclass, and many SDKs ship their classes `final`, as `GlobalCourierRateClient` is. Composition is the default.

## Pattern: Facade

### The problem

Preparing a shipment touches four subsystems. `TrackingRegistry` issues the tracking number and builds the tracking URL, `LabelPrinter` renders the label, `CarrierGateway` quotes the rate, and `CustomsDeclarationService` builds a declaration only when one is needed. The tracking number has to exist before the label and the URL can use it. Every caller that orchestrates these itself has to get that order right, every time.

### The solution

A **Facade** gives a set of subsystems one higher-level interface, so callers make one call instead of coordinating several.

Use a Facade when:

- several subsystems must be called together in a fixed order;
- more than one caller, such as a checkout path and a nightly batch job, needs the same sequence.

### How Java models it

`CustomsDeclarationService` owns the rule for when a declaration applies, and returns an empty `Optional` for every zone except `INTERNATIONAL`:

```java
public final class CustomsDeclarationService {

    public Optional<CustomsDeclaration> declareIfNeeded(Shipment shipment) {
        if (shipment.zone() != DestinationZone.INTERNATIONAL) {
            return Optional.empty();
        }
        Money declaredValue = Money.ofCents(shipment.declaredValueCents());
        String note = "General merchandise, declared value " + declaredValue;
        return Optional.of(new CustomsDeclaration(declaredValue, note));
    }
}
```

`ShippingFacade` holds the four subsystems and exposes `prepareShipment()`:

```java
public final class ShippingFacade {

    private final CarrierGateway carrierGateway;
    private final LabelPrinter labelPrinter;
    private final CustomsDeclarationService customsDeclarationService;
    private final TrackingRegistry trackingRegistry;

    public ShippingFacade(CarrierGateway carrierGateway, LabelPrinter labelPrinter,
            CustomsDeclarationService customsDeclarationService, TrackingRegistry trackingRegistry) {
        this.carrierGateway = Objects.requireNonNull(carrierGateway, "carrierGateway");
        this.labelPrinter = Objects.requireNonNull(labelPrinter, "labelPrinter");
        this.customsDeclarationService = Objects.requireNonNull(customsDeclarationService, "customsDeclarationService");
        this.trackingRegistry = Objects.requireNonNull(trackingRegistry, "trackingRegistry");
    }

    public ShipmentManifest prepareShipment(Shipment shipment, int sequence) {
        Objects.requireNonNull(shipment, "shipment");
        String trackingNumber = trackingRegistry.issueTrackingNumber(sequence);
        Money quote = carrierGateway.quote(shipment);
        String label = labelPrinter.render(shipment, trackingNumber);
        String trackingUrl = trackingRegistry.trackingUrl(trackingNumber);
        Optional<CustomsDeclaration> customsDeclaration = customsDeclarationService.declareIfNeeded(shipment);
        return new ShipmentManifest(quote, label, trackingNumber, trackingUrl, customsDeclaration);
    }
}
```

The Facade adds no capability the subsystems lack. What it adds is one place where the ordering invariant lives: the tracking number is issued first, then the quote, the label and the URL. A second caller inherits the correct order instead of re-deriving it. The Facade also never branches on `shipment.zone()`; the customs rule stays in `CustomsDeclarationService`, so no caller is tempted to copy it.

Don't give the Facade public getters for its subsystems. A `carrierGateway()` accessor next to `prepareShipment()` lets callers skip the method that enforces the order.

## Pattern: Bridge

### The problem

Notifications vary two ways at once. The kind varies: a shipment was created, or a delivery is delayed, each with its own text. The channel varies: email, SMS, and whatever comes next. A class per combination (`ShipmentCreatedEmailNotification`, `ShipmentCreatedSmsNotification`, `DeliveryDelayedEmailNotification`, `DeliveryDelayedSmsNotification`) grows by a whole row or column each time either axis gains a member.

### The solution

A **Bridge** separates an abstraction from its implementation so the two can vary independently.

Use a Bridge when:

- two dimensions vary independently in practice, not in theory;
- you'd otherwise write a class per combination.

### How Java models it

`DeliveryChannel` is the implementation side:

```java
public interface DeliveryChannel {
    String deliver(String recipient, String message);
}

public final class EmailDeliveryChannel implements DeliveryChannel {
    @Override
    public String deliver(String recipient, String message) {
        return "EMAIL to %s: %s".formatted(recipient, message);
    }
}

public final class SmsDeliveryChannel implements DeliveryChannel {
    @Override
    public String deliver(String recipient, String message) {
        return "SMS to %s: %s".formatted(recipient, message);
    }
}
```

`Notification` is the abstraction side. It holds a `DeliveryChannel` by composition, and `send()` asks the subclass for its text and hands it to the channel:

```java
public abstract class Notification {

    private final DeliveryChannel channel;

    protected Notification(DeliveryChannel channel) {
        this.channel = Objects.requireNonNull(channel, "channel");
    }

    public final String send(String recipient) {
        return channel.deliver(recipient, formatMessage());
    }

    protected abstract String formatMessage();
}
```

The refined abstractions override only `formatMessage()`:

```java
public final class ShipmentCreatedNotification extends Notification {

    private final String trackingNumber;

    public ShipmentCreatedNotification(DeliveryChannel channel, String trackingNumber) {
        super(channel);
        this.trackingNumber = Objects.requireNonNull(trackingNumber, "trackingNumber");
    }

    @Override
    protected String formatMessage() {
        return "Your shipment %s has been created.".formatted(trackingNumber);
    }
}
```

Two kinds and two channels give four working combinations with four small classes, not four combination classes. A third kind or a third channel is one more class.

`send()` is `final`, so a subclass can change what the message says but not how it reaches the channel. `Notification` is left non-`sealed` because both axes are meant to stay open for new kinds and channels.

## When not to use it

- **Adapter.** If the vendor client already took a `Shipment` and returned `Money`, an adapter adds a class and a dispatch around a direct call. Don't write one in case a mismatch appears later.
- **Facade.** A class that forwards to one subsystem, or holds four collaborators with no ordering rule between them, is indirection with a pattern name. The sequence inside `prepareShipment()` is what justifies `ShippingFacade`.
- **Bridge.** If the only notification is a shipment-created email, splitting `Notification` from `DeliveryChannel` buys nothing. Introduce the split when a second kind or a second channel is in hand or clearly coming; until then, one class that formats and sends is better.

> **🤖 Explore with your agent**
>
> "In `examples/15-adapter-facade-bridge`, write `GlobalCourierGatewayAdapter` as an object adapter: implement `CarrierGateway`, hold a `GlobalCourierRateClient` in a private final field, and convert `weightKg` to grams and `DestinationZone` to codes 0 to 3 with a switch expression. Don't extend `GlobalCourierRateClient`. Write `ShippingFacade.prepareShipment(Shipment, int)` so it issues the tracking number before quoting, rendering the label and building the tracking URL, leaves the customs decision to `CustomsDeclarationService.declareIfNeeded`, and exposes no getters for its four subsystems."
>
> Check: `mvn -q -f examples/15-adapter-facade-bridge/pom.xml test -Dtest='GlobalCourierGatewayAdapterTest,ShippingFacadeTest'`; reject `extends GlobalCourierRateClient` or a public subsystem getter.

## Build, run, observe

From the repository root, the aggregator builds every example:

```sh
mvn -f examples/pom.xml test
```

Or build this one on its own:

```sh
cd examples/15-adapter-facade-bridge
mvn test
```

All 28 tests pass. The demo adapts a legacy quote, runs the Facade over a domestic and an international shipment, and sends two notification kinds over two channels:

```sh
examples/15-adapter-facade-bridge/demo.sh
```

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

The first `customs:` line comes from a `REGIONAL` shipment and the second from an `INTERNATIONAL` one, and `ShippingFacade` never checks the zone itself.

- **Reorder the Facade.** In a scratch copy, move `carrierGateway.quote(shipment)` above `issueTrackingNumber` in `prepareShipment()`. The manifest is still correct, but the call-order test fails.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original Adapter, Facade, and Bridge entries, including the object-adapter-versus-class-adapter distinction `GlobalCourierGatewayAdapter` follows.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 18, "Favor composition over inheritance," the reasoning behind the object-adapter choice and the `final` vendor class.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/), [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html), and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/15-adapter-facade-bridge`.
- The full reading list lives on the [bibliography page]({{ '/bibliography/' | relative_url }}).
