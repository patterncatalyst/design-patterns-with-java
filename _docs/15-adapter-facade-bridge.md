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

- Name the force each of Adapter, Facade, and Bridge resolves, and tell the
  three apart from each other.
- Build an Adapter that wraps a third-party client's awkward shape behind
  this app's own interface, by composition rather than inheritance.
- Build a Facade that gives a caller one method over several subsystems, and
  explain what single-call orchestration buys over trusting every caller to
  coordinate those subsystems correctly, in the right order, every time.
- Build a Bridge that lets a notification's kind and its delivery channel
  vary independently, so a new kind or a new channel each arrive as one new
  class, not a new class for every combination.
- Recognize when each of these three techniques is solving a problem a
  direct method call or a visible field would already solve.

## Why this matters

A checkout flow needs a shipping quote, a label, and a customs declaration,
and in practice all three arrive wrapped in friction the pattern itself did
not create. The rate comes from a carrier's own SDK, built to that carrier's
conventions, with its own units and its own method names. Producing a quote,
a label, a customs declaration, and a tracking number is four separate calls
that have to happen together, in the right order, every time, or a bug
ships: a label printed before a tracking number exists to put on it, a
customs form missing from a shipment that legally needs one. And once a
shipment ships, the system has to tell someone, by whichever channel that
customer prefers, in a message that differs by what happened, not by how the
message gets delivered.

None of those three problems is about varying an algorithm or producing a
family of related objects. They're about structure: how an existing,
awkwardly shaped class gets a shape the rest of the code can use; how a
caller gets one thing to call instead of several things to orchestrate; how
two things that each vary on their own, a notification's kind and its
delivery channel, get decoupled so adding one doesn't force a change to the
other. Adapter, Facade, and Bridge are the Gang of Four's names for those
three structural forces, and this chapter builds all three against one
carrier integration so each technique's domain is already familiar by the
time the next one arrives.

{% include excalidraw.html name="15-adapter-facade-bridge-forces" caption="Figure 15.1: Adapter wraps a legacy rate client behind the app's own CarrierGateway interface, Facade fronts four shipping subsystems with one call, and Bridge crosses two notification kinds with two delivery channels without a class per combination." %}

## The domain: a GlobalCourier integration

The example behind this chapter lives in `examples/15-adapter-facade-bridge`,
and its domain is one carrier integration, GlobalCourier, carried through
all three techniques. `Shipment` is a record carrying a weight, a
destination zone, and a declared value, validated once in its compact
constructor so every technique downstream can trust its fields without
re-checking them:

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

`GlobalCourierRateClient` is not a class this book gets to redesign. It
stands in for GlobalCourier's own rate-lookup SDK, written to that vendor's
conventions: weight travels in grams, the destination travels as a numeric
zone code, and a quote comes back as a plain `long`, not this app's own
`Money` type.

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

Every one of this chapter's three techniques touches this one integration
from a different angle: Adapter makes `GlobalCourierRateClient` usable where
this book's code expects its own interface, Facade hides that a quote, a
label, a customs declaration, and a tracking number are four separate calls
at all, and Bridge decouples the shipment-status messages this integration
triggers from how those messages get delivered.

## Adapter: wrapping a legacy rate client

`CarrierGateway` is the target interface: the one shape checkout code, and
the Facade built later in this chapter, depend on. It says nothing
about GlobalCourier, grams, or zone codes.

```java
public interface CarrierGateway {
    Money quote(Shipment shipment);
}
```

`GlobalCourierGatewayAdapter` is the Adapter. It implements `CarrierGateway`,
and it does the translating, kilograms to grams, `DestinationZone` to a zone
code, on every call.

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

The field `legacyClient` is what makes this an object adapter rather than a
class adapter: `GlobalCourierGatewayAdapter` holds a
`GlobalCourierRateClient` by composition instead of extending it. In Java
that choice is close to automatic in a way it is not in every language,
because Java allows a class to extend only one superclass. Extending
`GlobalCourierRateClient` would spend the adapter's one shot at inheritance
on a vendor class it doesn't control, leaving nothing free if a second
legacy client ever needs wrapping behind the same `CarrierGateway`, or if the
adapter itself ever needs to extend something of its own. `GlobalCourierRateClient`
is also marked `final`, which forecloses the question outright, and a lot of
real third-party SDKs ship exactly that way. Composition keeps
`GlobalCourierGatewayAdapter` a small, ordinary class holding a value it
happens to translate for, free to wrap a different vendor's client, or two
of them, without touching its own inheritance at all.

Nothing about `CarrierGateway` or any of its callers mentions GlobalCourier.
Swap `GlobalCourierGatewayAdapter` for a different carrier's adapter and
every caller of `CarrierGateway.quote()` keeps working unmodified, which is
the whole point of adapting to a target interface instead of threading the
vendor's shape through the rest of the codebase. `CarrierGateway` is also a
functional interface, with exactly one abstract method, so a test or a demo
can hand a facade a plain lambda in place of a real adapter wherever only
the rate matters and the GlobalCourier wiring doesn't.

## Facade: one call over four subsystems

A shipment needs four things that have to happen in the right order and
land in one consistent result: a rate quote, a label with the right tracking
number already printed on it, a customs declaration for the shipments that
need one, and a tracking URL a customer can visit. `TrackingRegistry` issues
the tracking number and builds that URL; `LabelPrinter` renders the label
text; `CustomsDeclarationService` decides whether a shipment needs a
declaration at all and builds one when it does, returning an empty
`Optional` for every zone but `INTERNATIONAL`.

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

`ShippingFacade` is the Facade. It owns a `CarrierGateway`, a `LabelPrinter`,
a `CustomsDeclarationService`, and a `TrackingRegistry`, and it exposes
exactly one method that matters to a caller: `prepareShipment()`.

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

The sequencing inside `prepareShipment()` is the actual value this Facade
delivers, not the four subsystem calls on their own. The tracking number has
to exist before the label can print it and before the tracking URL can point
at it. A checkout handler calling `trackingRegistry`, `carrierGateway`,
`labelPrinter`, and `customsDeclarationService` directly would have to get
that order right every single time it wrote this logic, in every place it
wrote this logic. `ShippingFacade` gets it right once, in one method, and
every caller inherits that correctness instead of re-deriving it. That's
what a Facade buys beyond convenience: it is not shorter code, it is one
place the sequencing invariant lives instead of being trusted to every
caller's memory.

Nothing about this Facade adds a capability `CarrierGateway`, `LabelPrinter`,
`CustomsDeclarationService`, and `TrackingRegistry` didn't already have
separately. A reviewer could, in principle, call the four subsystems
directly and get the same result for one shipment. The Facade's value shows
up over time and across callers: a second checkout path, or a batch job
preparing a thousand shipments overnight, inherits the correct sequencing
instead of re-implementing it with its own chance to get it wrong. Notice,
too, that `CustomsDeclarationService` decides on its own whether a
declaration applies; `ShippingFacade` never branches on `shipment.zone()`
itself. That's a second form of decoupling riding alongside the
orchestration: the policy for when a declaration is required lives in
exactly one place, not duplicated into every caller of the Facade that might
otherwise be tempted to check the zone itself.

## Bridge: notifications that vary two ways

A shipment triggers notifications, and two things about a notification vary
independently of each other. The kind of event varies: a shipment was
created, or a delivery is delayed, each needing its own message text. The
delivery channel also varies: email, SMS, or whatever a future integration
adds, each needing its own way of sending something.
`DeliveryChannel` is the implementation side of the Bridge, the delivery
mechanism.

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

`Notification` is the abstraction side. It holds a `DeliveryChannel` by
composition, not by inheriting from one, and its `send()` method is the
bridge between the two axes: it asks the concrete notification for its
message text, then hands that text to whichever channel was configured.

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

`ShipmentCreatedNotification` and `DeliveryDelayedNotification` are the
refined abstractions. Each overrides `formatMessage()` with its own text and
leaves `send()`, and the channel it was built with, untouched:

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

This is the structural reason Bridge is a separate pattern from plain
polymorphism. Two refined `Notification` kinds and two `DeliveryChannel`
implementations produce four working combinations, a shipment-created
email, a shipment-created text, a delay email, a delay text, and none of
those four combinations needed its own class. Without the Bridge, the next
instinct is usually a class per combination:
`ShipmentCreatedEmailNotification`, `ShipmentCreatedSmsNotification`,
`DeliveryDelayedEmailNotification`, `DeliveryDelayedSmsNotification`, and the
count keeps multiplying every time either axis grows by one. A third
notification kind or a third channel added to the Bridge's two existing
hierarchies is one new class, not a new row and column of classes crossed
against everything that already exists. `Notification` is a plain,
non-`sealed` abstract class for exactly this reason: both axes stay
open to a reader or a future team adding a kind or a channel without
touching this chapter's code at all.

The shared `send()` method is marked `final`. It's the one piece of
behavior every `Notification` has in common regardless of kind, and sealing
it off from being overridden keeps the bridge itself, asking for a message
and handing it to a channel, from drifting apart between subclasses. A
subclass can change what the message says; it can't change how the message
reaches its channel.

> **AI sidebar.** Ask an LLM pairing partner to "wrap this third-party rate
> client" and a frequent result reaches for extending it directly:
> `class GlobalCourierGatewayAdapter extends GlobalCourierRateClient
> implements CarrierGateway`. If the vendor class happens to be `final`, the
> compiler catches this immediately; if it is not, the code compiles, and for
> a single vendor type it even works. Check what it costs before accepting
> it: the adapter now exposes every public method `GlobalCourierRateClient`
> has, not just the ones `CarrierGateway` declares, and it has no clean
> answer the day a second vendor's client needs wrapping behind the same
> interface by the same adapter. The object-adapter shape this chapter
> builds, composing the legacy type as a plain field, scales to that case
> without changing shape at all. A second thing worth checking in generated
> Facade code: does the generated method still let a caller reach the
> subsystem objects directly, through a public getter? A `ShippingFacade`
> with a `carrierGateway()` getter next to `prepareShipment()` is a Facade
> that forgot its own job, leaving the sequencing invariant optional for any
> caller willing to skip the one method that enforces it.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this loop.
From the repository root, the aggregator picks up
`examples/15-adapter-facade-bridge` once it is registered there:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/15-adapter-facade-bridge
mvn test
```

Either path runs the same 28 JUnit 5 cases, asserted with AssertJ and
verified against Mockito where a collaborator's exact interaction matters:
a parameterized suite that checks the Adapter maps every `DestinationZone`
to GlobalCourier's numeric code, a Mockito `InOrder` verification that the
Facade calls its subsystems in the order its own result depends on, and a
parameterized suite that crosses both notification kinds with both delivery
channels. Observed locally with Temurin 25:

```
Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see all three techniques produce real output rather than just reading an
assertion that says they work, run the example's own demo script:

```sh
examples/15-adapter-facade-bridge/demo.sh
```

It adapts a legacy quote, runs the Facade over a domestic and an
international shipment, and sends two notification kinds over two channels:

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

Notice the two `customs:` lines: the first comes from a `REGIONAL` shipment,
the second from an `INTERNATIONAL` one, and `ShippingFacade` never checks the
zone itself to decide which line applies. That split is `CustomsDeclarationService`
showing up as an observable fact at the terminal, not just as a passing test
assertion.

## When not to use Adapter, Facade, or Bridge

All three techniques in this chapter earn their place when a specific,
named friction exists: an interface mismatch to bridge, an orchestration
sequence to protect, or two independent axes of variation to decouple. Used
without one of those frictions present, each adds a layer with nothing
underneath it.

Adapter needs an actual mismatch to resolve. If `GlobalCourierRateClient`
already took a `Shipment` and returned a `Money`, wrapping it in
`GlobalCourierGatewayAdapter` would add a class and an interface dispatch
around a method that could be called directly. This adapter is justified
because the vendor client's units and names differ from what the rest of
the codebase expects; writing one preemptively, in case some future client
someday needs wrapping, defends against a mismatch that doesn't exist yet.

Facade needs more than one subsystem with a real sequencing or coordination
rule between them. A single subsystem, called through a method that does
nothing but forward to it, is not a Facade; it is an unnecessary layer of
indirection with a different name. The signal that `ShippingFacade` is
pulling its weight is the four-call sequence inside `prepareShipment()`, not
its four constructor parameters on their own. A class with four constructor
parameters and no sequencing logic between the calls it makes is closer to a
parameter holder than to this pattern.

Bridge needs two axes that vary independently in practice, not one axis
dressed up as two. If this integration only ever sent
`ShipmentCreatedNotification` over email, with no delay notification and no
second channel planned, splitting `Notification` from `DeliveryChannel` buys
nothing: there would be exactly one combination, achievable with one class
and no bridge at all. The moment to introduce the split is the moment a
second kind or a second channel is either in hand or clearly coming. Before
that point, a single class that formats a message and sends it directly is
the stronger engineering choice, even with Bridge's name sitting right there
in the catalogue.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original Adapter, Facade, and Bridge entries, including the object-adapter-versus-class-adapter distinction this chapter's `GlobalCourierGatewayAdapter` follows.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 18, "Favor composition over inheritance," the reasoning behind this chapter's object-adapter choice and its `final` vendor class.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/), [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html), and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/15-adapter-facade-bridge`'s suite, which verifies the Adapter's unit conversion, the Facade's call order, and the Bridge's four kind-and-channel combinations, not just that each one runs without throwing.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
