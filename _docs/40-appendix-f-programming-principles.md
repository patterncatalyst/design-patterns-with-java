---
title: "Programming principles, reviewed in code"
order: 40
layout: tutorial
sectionid: docs
part: appendices
appendix: "F"
description: "The principle vocabulary beyond SOLID that every Java engineer is expected to recognize, each shown as a before-and-after shipping-domain snippet with the point where it starts costing more than it returns."
---

## Learning objectives

By the end of this appendix you can:

- Name the common programming principles beyond SOLID and say what force each one answers.
- Spot a violation of each in shipping-domain Java and write the version that honors it.
- Say when each principle starts costing more than it returns.
- Tell a principle (why) from a pattern (how).

## Why this matters

A principle names a force; a pattern is a structure that answers one. DRY is a why, and pulling a shared `shipmentTotalCents` method out of two near-duplicates is a how. Encapsulate What Changes is a why, and Strategy is one how. Code review runs on these names, so a reviewer who can say "that's a Law of Demeter chain" saves a paragraph.

Each entry below gives the force, a violation, a fix, and the tension: every principle trades against a counter-force, and knowing the name isn't the same as knowing when to stop. The listings are short illustrations in the shipping domain (carriers, rate quotes, tracking records, invoices), not excerpts from the examples.

{% include excalidraw.html name="40-principle-families" caption="Figure F.1: The programming-principle vocabulary grouped into six families, from a tight SOLID recap through a process family with no code shape at all." %}

## Substitutability and responsibility (SOLID, recapped)

The SOLID chapters build these against a freight-billing engine and an order-notification system: [SOLID I]({{ '/docs/03-solid-first-principles/' | relative_url }}) and [SOLID II]({{ '/docs/04-solid-dependency-inversion/' | relative_url }}).

| Principle | Claim | In the examples |
|---|---|---|
| Single Responsibility | A type has one reason to change | `InvoiceCalculator`, `InvoiceFormatter` and `InMemoryInvoiceRepository` change for a tax rate, a receipt wording and a storage migration |
| Open/Closed | New behavior arrives as new code, not edits to working code | A flat service fee is one new `InvoiceRule`; the calculator is untouched |
| Liskov Substitution | A subtype honors the base type's whole contract | `PercentageDiscountRule` clamps the rate in a method subtypes can't override, so a loyalty variant can't discount past the subtotal |
| Interface Segregation | A client doesn't depend on capabilities it never uses | A fat `NotificationGateway` splits into `MessageSender`, `TemplateRenderer` and `DeliveryStatusProvider`; SMS implements one |
| Dependency Inversion | Policy and detail both depend on an abstraction | The notification policy holds a `MessageSender`, never a concrete email or SMS gateway |

None of the five is free. Each earns its cost once a second case, rule or implementation is in hand or clearly coming.

## Simplicity and scope

### KISS (Keep It Simple)

Fewer moving parts are cheaper to verify and to change.

```java
// Violates KISS: a rule abstraction built for a carrier that currently
// charges exactly one surcharge.
interface SurchargeRule {
    long applyCents(long baseRateCents);
}

final class FuelSurchargeRule implements SurchargeRule {
    @Override
    public long applyCents(long baseRateCents) {
        return baseRateCents / 20;
    }
}
```

```java
// Honors KISS: the one surcharge this carrier charges, expressed as the
// one method it needs.
static long fuelSurchargeCents(long baseRateCents) {
    return baseRateCents / 20; // flat 5%
}
```

Tension: once a second surcharge arrives, KISS yields to Open/Closed. Keeping the method past that point grows a chain of `if` statements, a worse kind of complexity.

### YAGNI (You Aren't Gonna Need It)

Code for a need that hasn't arrived costs as much to read and maintain as code that's used, and nothing exercises it.

```java
// Violates YAGNI: fields added for a multi-currency, multi-leg routing
// feature no carrier integration in this system has built.
record RateQuoteRequest(
        double weightKg,
        DestinationZone zone,
        String currencyCode,   // never set to anything but "USD"
        List<String> routingLegs) { // always empty
}
```

```java
// Honors YAGNI: only the fields a real carrier integration consumes today.
record RateQuoteRequest(double weightKg, DestinationZone zone) {
}
```

Tension: YAGNI doesn't excuse skipping an extension point a second carrier integration is scheduled to need next sprint. Ask whether the need is coming, not whether it's imaginable.

### Do The Simplest Thing That Could Possibly Work

Write the plain code that passes the case in front of you, and generalize when a second case shows up. It's about implementation order; KISS is about the shape you end with.

```java
// Skips the simplest thing: an interface built for a tracking lookup
// that today has exactly one data source.
interface TrackingStatusSource {
    String statusFor(String trackingId);
}
```

```java
// Does the simplest thing that could possibly work: a direct lookup.
// Promote this to an interface once a second tracking source exists.
static String statusFor(String trackingId, CarrierClient client) {
    return client.lookup(trackingId);
}
```

Tension: "simplest" is where you start, not where you stay once a second carrier's tracking API appears.

### Avoid Premature Optimization

Optimize a bottleneck a profiler has shown you. A guess is usually wrong, and the code built to chase it costs more to maintain than it saves.

```java
// Premature: a hand-rolled object pool for RateQuote, written before any
// profiler has shown quote construction costs anything.
final class RateQuotePool {
    private final RateQuote[] storage = new RateQuote[256];
    private final boolean[] inUse = new boolean[256];

    RateQuote acquire() { /* scan inUse, mark a slot, return it */ return null; }

    void release(RateQuote quote) { /* find the slot, clear it */ }
}
```

```java
// Measured first: a plain list. An initial capacity avoids reallocation
// for the batch sizes this quoting run sees, nothing more.
var quotes = new ArrayList<RateQuote>(expectedShipmentCount);
```

Tension: once a profiler names quote construction as a hot loop under real load, declining to optimize is its own mistake. Measure, then decide.

### Curly's Law (Do One Thing)

A method or class that does one thing is easier to name, test alone and reuse.

```java
// Violates Curly's Law: validating, pricing, and logging in one method;
// none of the three steps can be reused or tested alone.
long quoteAndLogShipment(Shipment shipment, System.Logger logger) {
    if (shipment.weightKg() <= 0.0) {
        throw new IllegalArgumentException("bad weight");
    }
    long rateCents = shipment.weightKg() > 20.0 ? 4_500 : 1_200;
    logger.log(System.Logger.Level.INFO, "quoted {0} cents", rateCents);
    return rateCents;
}
```

```java
// Honors Curly's Law: validate, price, and log are three methods; the
// caller composes them, and each is independently testable.
void validateShipment(Shipment shipment) {
    if (shipment.weightKg() <= 0.0) {
        throw new IllegalArgumentException("bad weight");
    }
}

long rateFor(Shipment shipment) {
    return shipment.weightKg() > 20.0 ? 4_500 : 1_200;
}

void logQuote(long rateCents, System.Logger logger) {
    logger.log(System.Logger.Level.INFO, "quoted {0} cents", rateCents);
}
```

Tension: splitting past the point where anyone would call a piece on its own produces a maze of one-line methods, the same over-segregation Interface Segregation warns against.

## Structure and change

### DRY (Don't Repeat Yourself)

One fact lives in one place, so fixing it once fixes it everywhere. The target is duplicated knowledge, a business rule written twice, not text that happens to look alike.

```java
// Violates DRY: the same subtotal-plus-fuel-surcharge formula written
// once for the quote preview and again for the invoice.
long previewTotalCents(Shipment shipment) {
    long base = rateFor(shipment);
    return base + base / 20;
}

long invoiceTotalCents(Shipment shipment) {
    long base = rateFor(shipment);
    return base + base / 20;
}
```

```java
// Honors DRY: one method both call; a change to the surcharge formula
// has exactly one place to go wrong.
long shipmentTotalCents(Shipment shipment) {
    long base = rateFor(shipment);
    return base + base / 20;
}
```

Tension: two methods that both add two numbers but encode unrelated rules stay separate. Merged, a change to one rule silently changes the other.

### Separation of Concerns

Parsing a request, pricing a shipment and writing to storage live in separate classes, so a change to one doesn't ripple into the others. Single Responsibility says this for one type; Separation of Concerns says it for a module or layer.

```java
// Violates Separation of Concerns: parsing, pricing, and persistence in
// one handler.
void handleQuoteRequest(String requestBody, QuoteDatabase db) {
    Shipment shipment = parseShipmentJson(requestBody);
    long rateCents = rateFor(shipment);
    db.exec("INSERT INTO quotes (weight, rate) VALUES (?, ?)",
            shipment.weightKg(), rateCents);
}
```

```java
// Honors Separation of Concerns: each concern is a class with one job;
// the handler only sequences them.
void handleQuoteRequest(String requestBody, QuoteDatabase db) {
    Shipment shipment = ShipmentParser.parse(requestBody);
    long rateCents = RateCalculator.rateFor(shipment);
    new QuoteRepository(db).save(shipment, rateCents);
}
```

Tension: every hop between layers has a cost. Separate concerns that change on different schedules, not every place a boundary could be drawn.

### Encapsulate What Changes

Put the part expected to vary behind a stable interface, so the stable part never changes when it does. Strategy answers this force in full.

```java
// Violates the principle: the varying part (how to price) sits inline
// inside the thing that should never vary (checkout).
long checkoutTotalCents(Shipment shipment, String pricingModel) {
    return switch (pricingModel) {
        case "flat" -> 1_200L;
        case "by_weight" -> (long) (shipment.weightKg() * 150);
        default -> throw new IllegalArgumentException("unknown pricing model");
    };
}
```

```java
// Honors the principle: the varying part moves behind an interface;
// checkoutTotalCents never changes again when a third model arrives.
interface RateStrategy {
    long rateForCents(Shipment shipment);
}

long checkoutTotalCents(Shipment shipment, RateStrategy strategy) {
    return strategy.rateForCents(shipment);
}
```

Tension: introduce the interface with the second pricing model, not the first. An interface with one implementation pays the cost with none of the benefit yet.

### Hide Implementation Details

Callers depend on what a type does, not on how it stores its data, so the representation can change without touching them.

```java
// Violates the principle: callers reach into the raw event list, and
// each one re-derives "the latest status" slightly differently.
final class TrackingRecord {
    List<TrackingEvent> events = new ArrayList<>(); // public, order not guaranteed
}
```

```java
// Honors the principle: the representation is private; every caller
// asks the one question the type answers correctly.
final class TrackingRecord {
    private final List<TrackingEvent> events = new ArrayList<>();

    void recordEvent(TrackingEvent event) {
        events.add(event);
    }

    String latestStatus() {
        return events.get(events.size() - 1).status();
    }
}
```

Tension: hiding data every caller legitimately needs, such as iterating every event for an audit, only adds wrapper methods. Hide what varies; expose what callers compute.

### Composition Over Inheritance

Build an object from small collaborators instead of a deep tree inherited for reuse. Inheritance is still right for a real is-a relationship where callers substitute the subtype; that's Liskov's territory.

```java
// Violates the principle: inheritance reused for convenience, not for an
// is-a relationship, reaching into the base type's state.
class TrackingRecord {
    protected final List<TrackingEvent> events = new ArrayList<>();

    void recordEvent(TrackingEvent event) {
        events.add(event);
    }
}

final class PriorityTrackingRecord extends TrackingRecord {
    private boolean priority;

    void recordPriorityEvent(TrackingEvent event) {
        events.add(event); // reaches into the base type's state
        priority = true;
    }
}
```

```java
// Honors the principle: PriorityTracker composes a TrackingRecord
// instead of extending it, and depends only on its public interface.
final class PriorityTracker {
    private final TrackingRecord record = new TrackingRecord();
    private boolean priority;

    void recordPriorityEvent(TrackingEvent event) {
        record.recordEvent(event);
        priority = true;
    }
}
```

Tension: composition that forwards every method of the composed type is inheritance by hand, with more boilerplate and no type relationship. Use composition when the need is reuse, not substitutability.

### Orthogonality

Independent parts stay independent: changing one never forces a change elsewhere through a hidden side effect.

```java
// Violates orthogonality: submitting a label flips an unrelated static
// logging setting, so switching carriers silently changes how tracking
// events are logged elsewhere in the system.
final class FastShipGateway implements CarrierGateway {
    @Override
    public LabelReceipt submitLabelRequest(LabelRequest request) {
        LogFormat.CURRENT = LogFormat.COMPACT; // unrelated global, changed here
        return client.submit(request);
    }
}
```

```java
// Honors orthogonality: submitting a label touches only label
// submission; log formatting is configured once, independently.
final class FastShipGateway implements CarrierGateway {
    @Override
    public LabelReceipt submitLabelRequest(LabelRequest request) {
        return client.submit(request);
    }
}
```

Tension: pushed too far, parts become so decoupled that a real cross-cutting concern, such as tracing one request through label submission and logging, needs its own plumbing to reconnect them.

## Coupling and cohesion

### Minimize Coupling

Fewer, narrower dependencies mean a change ripples into fewer places. Collaborators always agree on something; the question is whether it's a narrow published interface or a wide accidental one.

```java
// High coupling: reaches past CarrierGateway's interface into its
// internal retry queue to answer one question.
boolean hasPendingShipments(CarrierGateway gateway) {
    return !gateway.internalRetryQueue().isEmpty();
}
```

```java
// Lower coupling: depends on one narrow method CarrierGateway publishes;
// the retry queue stays an implementation detail.
boolean hasPendingShipments(CarrierGateway gateway) {
    return gateway.hasPendingRequests();
}
```

Tension: passing primitives instead of a small named type ("stringly typed" calls) swaps coupling to a type for coupling to an unenforced convention every call site must get right.

### Maximize Cohesion

Keep data with the operations that use only that data, so the logic sits next to the fields it touches.

```java
// Low cohesion: logic that only ever uses Shipment's own fields lives
// far from the record, in an unrelated utility class.
record Shipment(double weightKg, DestinationZone zone) {
}

final class RateMath {
    static long baseRateCents(Shipment shipment) {
        return shipment.weightKg() > 20.0 ? 4_500 : 1_200;
    }
}
```

```java
// High cohesion: the rate math depends solely on Shipment's own fields,
// so it moves onto Shipment itself.
record Shipment(double weightKg, DestinationZone zone) {
    long baseRateCents() {
        return weightKg() > 20.0 ? 4_500 : 1_200;
    }
}
```

Tension: moving every method that takes a `Shipment` onto the record, including logic that leans on unrelated types, builds a god object. Move only logic that depends solely on the type's own fields.

### Law of Demeter

A method talks to itself, its parameters, objects it creates and its direct components, not to a stranger's stranger three hops away.

```java
// Violates the Law of Demeter: a chain reaching through three strangers,
// coupling the caller to the full shape of three types.
String postal = invoice.customer().account().billingAddress().postalCode();
```

```java
// Honors the Law of Demeter: Invoice exposes the one fact a caller
// needs; the chain still exists, but only Invoice walks it.
record Invoice(Customer customer) {
    String billingPostalCode() {
        return customer.account().billingAddress().postalCode();
    }
}

String postal = invoice.billingPostalCode();
```

Tension: wrapping every possible chain bloats the near type with pass-through methods. Apply it to chains outside callers walk.

### Command-Query Separation

A method either does something or answers something, so a caller can read a query without worrying it changed state.

```java
// Violates Command-Query Separation: one method reports a value and
// mutates a cursor, so calling it twice to log it gives two answers.
TrackingEvent nextEvent() {
    TrackingEvent event = events.get(cursor);
    cursor++; // side effect hidden inside what reads like a query
    return event;
}
```

```java
// Honors Command-Query Separation: a query that never mutates, and a
// command that never returns a meaningful value.
TrackingEvent peekNextEvent() {
    return events.get(cursor);
}

void advanceCursor() {
    cursor++;
}
```

Tension: an atomic pop-and-return on a queue shared across threads needs both in one call. Some call sites accept a documented command that returns a value rather than take two round trips under one lock.

### Inversion of Control

A framework, feed or caller decides when your code runs, instead of your code polling. Dependency Inversion is the narrower SOLID claim about which abstraction the dependency points at.

```java
// In control: this method polls the feed on its own schedule, deciding
// when to look for an update.
void pollForRateUpdates(RateFeed feed, RateCalculator calculator) throws InterruptedException {
    while (running) {
        if (feed.hasUpdate()) {
            calculator.applyUpdate(feed.latest());
        }
        Thread.sleep(Duration.ofSeconds(1));
    }
}
```

```java
// Control inverted: the feed calls RateCalculator exactly when an
// update exists, and RateCalculator never polls anything.
feed.onRateUpdate(calculator::applyUpdate);
```

Tension: inverting control isn't inverting a dependency. The method reference above can still name a concrete `RateFeed` and violate Dependency Inversion while inverting who calls whom.

## Interoperability and resilience

### Robustness Principle (Postel's Law)

Be conservative in what you send and liberal in what you accept. At a boundary you don't control, such as a carrier's tracking webhook, tolerate variation coming in and add none going out.

```java
// Violates the Robustness Principle: throws on any field the schema does
// not explicitly list, so a carrier adding an undocumented field breaks
// every shipment update until the parser is patched.
TrackingUpdate parseWebhook(Map<String, String> body) {
    for (String key : body.keySet()) {
        if (!isKnownField(key)) {
            throw new IllegalStateException("unexpected field: " + key);
        }
    }
    return new TrackingUpdate(body.get("status"), body.get("timestamp"));
}
```

```java
// Honors the Robustness Principle: unknown fields are ignored, and a
// missing optional field falls back to a documented default.
TrackingUpdate parseWebhook(Map<String, String> body) {
    return new TrackingUpdate(
            body.getOrDefault("status", "unknown"),
            body.getOrDefault("timestamp", currentTimestampIso8601()));
}
```

Tension: tolerance can hide a real producer bug. Log what was ignored, or a data problem looks like a clean parse.

## Principles for teams and process

These describe how people and schedules behave around code; no compiler checks them.

| Principle | Says | In a shipping system | Tension |
|---|---|---|---|
| **Boy-Scout Rule** | Leave the code you touch a little cleaner than you found it | Rename the `CarrierGateway` field whose name no longer matches while you're there for a bug fix | Cleanup folded into a fix buries the fix; put it in its own small commit |
| **Code For The Maintainer** | Write for a reader without your current context, often you in six months | Name a variable for its quantity, `outstandingBalanceCents`; comment why a rate-limit check exists | Writing for a careless reader adds comments that repeat code and checks for impossible states; write for a competent engineer new to the change |
| **Murphy's Law** | Whatever can go wrong will, usually under load | A rate API times out on peak day; a webhook arrives with a promised field empty | Defend at the boundary (a retrying gateway, a validating intake), not at every downstream call site |
| **Brooks's Law** | Adding people to a late project makes it later | Doubling the engineers on a tracking migration two weeks late doesn't rescue it: ramp-up drains the existing team, and communication paths grow faster than headcount | It describes a late project, not team growth planned with slack ahead of a deadline |
| **Linus's Law** | Given enough eyeballs, all bugs are shallow | A second reviewer on a carrier-integration change catches a retry edge case the author missed | Reviewers without domain context multiply rubber stamps, not scrutiny; review quality beats headcount |

Fred Brooks drew his law from managing IBM's OS/360 project. Linus's Law comes from open-source review culture and is associated with Linus Torvalds.

## For deeper coverage

- David Thomas, Andrew Hunt, [*The Pragmatic Programmer: Your Journey to Mastery*](https://pragprog.com/titles/tpp20/the-pragmatic-programmer-20th-anniversary-edition/) (20th Anniversary Edition, Addison-Wesley / Pragmatic Bookshelf): the source for DRY, orthogonality, and the Robustness Principle.
- Robert C. Martin, [*Clean Code: A Handbook of Agile Software Craftsmanship*](https://www.pearson.com/en-us/subject-catalog/p/clean-code-a-handbook-of-agile-software-craftsmanship/P200000009044/9780132350884) (Prentice Hall): the source for the Boy-Scout Rule, Code For The Maintainer, and the do-one-thing discipline behind Curly's Law.
- Frederick P. Brooks Jr., [*The Mythical Man-Month: Essays on Software Engineering*](https://www.pearson.com/en-us/subject-catalog/p/Brooks-Mythical-Man-Month-Anniversary-Edition-The-Essays-On-Software-Engineering-2nd-Edition/P200000000149/9780132119160) (Anniversary Edition, Addison-Wesley): the source for Brooks's Law.
- Robert C. Martin, [*Agile Software Development: Principles, Patterns, and Practices*](https://www.pearson.com/en-gb/subject-catalog/p/agile-software-development-principles-patterns-and-practices-pearson-new-international-edition/P200000004932/9781292038360) (Prentice Hall): the original statement of the five SOLID principles.
- Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480/9780321700698) (Addison-Wesley): the Gang of Four's advice to favor composition over inheritance and to program to an interface.
- The full reading list is on the [bibliography page]({{ '/bibliography/' | relative_url }}).
