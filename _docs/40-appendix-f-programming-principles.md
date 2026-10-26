---
title: "Programming principles, reviewed in code"
order: 40
layout: tutorial
sectionid: docs
part: appendices
appendix: "F"
description: "A code-first tour of the wider principle vocabulary beyond SOLID, the names every Java engineer is expected to recognize on sight, each weighed against a before-and-after shipping-domain Java 25 snippet and the point where the principle starts costing more than it returns."
---

## How to read this appendix

A principle is the force sitting underneath a pattern. A pattern names a
structure built to answer one of those forces directly: Strategy swaps an
algorithm behind an interface, Observer lets a subject broadcast a change
without naming its listeners, Decorator wraps a behavior without touching
the type it wraps. A principle names the force on its own, in a form general
enough to apply whether or not a named pattern is anywhere near the code
that honors it. The SOLID chapters in this book build five of these
principles in full, against a freight-billing engine and an
order-notification system, with a JUnit 5 and Mockito suite proving each
claim rather than describing it. This appendix does two things those
chapters do not: it recaps SOLID in a handful of sentences, pointing back at
that fuller treatment instead of repeating it, and it catalogs the rest of
the vocabulary an engineer is expected to recognize by name, each weighed
against a small before-and-after in this book's shipping domain: carriers,
rate quotes, tracking records, invoices.

The short way to hold the distinction: a principle says *why*, a pattern is
a *how* that answers it. DRY is a why; pulling a shared
`shipmentTotalCents` method out of two near-duplicate methods is a how.
Encapsulate What Changes is a why; Strategy is one how that answers it.
Every entry below follows the same shape: why the principle matters, code
that violates it, code that honors it, and a tension note, because every
principle on this list costs something or trades against a counter-force,
and naming a principle is not the same as knowing when to stop applying it.

{% include excalidraw.html name="40-principle-families" caption="Figure F.1: The programming-principle vocabulary grouped into six families, from a tight SOLID recap through a process family with no code shape at all." %}

## Substitutability and responsibility (SOLID, recapped)

The two SOLID chapters build these five principles in full against a
freight-billing engine and an order-notification system, with a suite that
proves each principle's claim directly instead of only describing it. This
section recaps each one in a sentence or two and points back at that
material rather than restating it.

**Single Responsibility.** A type should have exactly one reason to change.
Splitting a billing engine's calculation, formatting, and persistence into
their own types, `InvoiceCalculator`, `InvoiceFormatter`, and
`InMemoryInvoiceRepository`, answers to three unrelated schedules of
change: a tax-rate update, a receipt-wording change, a storage migration.

**Open/Closed.** A module should be open to new behavior and closed against
edits to behavior that already works. The `InvoiceRule` abstraction lets a
new rule, a flat service fee, arrive as one new type; the calculator that
applies every rule that predates it stays unchanged.

**Liskov Substitution.** A subtype must honor its base type's whole
contract, not only its method signatures. `PercentageDiscountRule` clamps
its discount rate in a method its subtypes cannot override, so a loyalty
variant that only supplies a rate can never discount an invoice past its
own subtotal no matter what rate it computes.

**Interface Segregation.** A client should not be forced to depend on a
capability it never uses. Splitting a fat `NotificationGateway` into
`MessageSender`, `TemplateRenderer`, and `DeliveryStatusProvider` lets an
SMS channel implement exactly the one role it actually has.

**Dependency Inversion.** High-level policy and low-level detail should
both depend on an abstraction, never on each other directly. An
order-notification policy holds a `MessageSender`, never a concrete email
or SMS gateway, so the policy compiles and tests without ever naming a
channel.

The SOLID chapters are also where "when SOLID works against you" lives in
full: none of the five is free, and each earns its cost only once a second
case, a second rule, a second implementation, is in hand or clearly on the
way.

## Simplicity and scope

### KISS (Keep It Simple)

A design with fewer moving parts is cheaper to verify and cheaper to change
later, because there is less structure standing between a reader and the
one fact that matters.

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

Tension: once a second surcharge arrives, KISS yields to the Open/Closed
force; keeping the method simple past that point means a growing chain of
`if` statements, which is a different and worse kind of complexity.

### YAGNI (You Aren't Gonna Need It)

Code built for a need that has not arrived yet costs the same to read,
review, and maintain as code answering a need that has, with nothing
exercising it to prove it is correct.

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

Tension: YAGNI is not license to skip an extension point a second carrier
integration is already scheduled to need next sprint; the test is whether
the need is clearly coming, not whether it is merely imaginable.

### Do The Simplest Thing That Could Possibly Work

Write the plain code that passes the case in front of you before reaching
for a general structure, and promote it once a second case shows up. This
is a discipline about implementation order; KISS is about the shape you
land on once you are done.

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

Tension: "simplest" is a starting point, not a place to stay once a second
carrier's tracking API shows up; it is the same candidate-abstraction
judgment every Strategy-shaped decision in this book trains directly.

### Avoid Premature Optimization

Spend engineering effort on a bottleneck a profiler has shown you, not on a
guess about where one might be. The guess is usually wrong, and the code
built to chase it usually costs more to maintain than the performance it
was meant to buy.

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

Tension: the principle cuts both ways. Once a profiler does name quote
construction as a hot loop under real load, declining to optimize out of
general caution becomes its own mistake; measure, then decide, in either
direction.

### Curly's Law (Do One Thing)

A method or a class that does one thing is easier to name precisely, test
in isolation, and reuse somewhere its sibling logic does not belong.

```java
// Violates Curly's Law: validating, pricing, and logging in one method;
// none of the three steps can be reused or tested alone.
long quoteAndLogShipment(Shipment shipment, Logger logger) {
    if (shipment.weightKg() <= 0.0) {
        throw new IllegalArgumentException("bad weight");
    }
    long rateCents = shipment.weightKg() > 20.0 ? 4_500 : 1_200;
    logger.info("quoted {} cents", rateCents);
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

void logQuote(long rateCents, Logger logger) {
    logger.info("quoted {} cents", rateCents);
}
```

Tension: splitting past the point where a reviewer would want to call a
piece on its own produces a maze of one-line methods, the same
over-segregation failure the Interface Segregation recap above warns
against.

## Structure and change

### DRY (Don't Repeat Yourself)

One fact should live in exactly one place, so fixing it once fixes it
everywhere it is used. The target is duplicated *knowledge*, a business
rule written twice, not duplicated *text* that happens to look similar.

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

Tension: two methods that happen to both add two numbers but encode
unrelated business rules should stay separate; merging them under DRY's
name means a later change to one rule silently changes a rule it was never
meant to touch.

### Separation of Concerns

Keep unrelated concerns, parsing a request, pricing a shipment, writing to
storage, in separate classes, so a change to one does not ripple into code
that has nothing to do with it. Single Responsibility makes the same claim
at the scale of one type; this principle makes it at the scale of a module
or a layer.

```java
// Violates Separation of Concerns: parsing, pricing, and persistence in
// one handler.
void handleQuoteRequest(HttpRequest request, QuoteDatabase db) {
    Shipment shipment = parseShipmentJson(request.body());
    long rateCents = rateFor(shipment);
    db.exec("INSERT INTO quotes (weight, rate) VALUES (?, ?)",
            shipment.weightKg(), rateCents);
}
```

```java
// Honors Separation of Concerns: each concern is a class with one job;
// the handler only sequences them.
void handleQuoteRequest(HttpRequest request, QuoteDatabase db) {
    Shipment shipment = ShipmentParser.parse(request.body());
    long rateCents = RateCalculator.rateFor(shipment);
    new QuoteRepository(db).save(shipment, rateCents);
}
```

Tension: splitting every layer into its own class because "concerns are
separate" ignores the real cost of the hop between them; apply it where
the concerns change on different schedules, not wherever a boundary could
theoretically be drawn.

### Encapsulate What Changes

Isolate the part of a design expected to vary behind a stable interface,
so the part that never varies never has to be touched when the varying
part does. This is the exact force the Strategy pattern answers in full;
the principle names the force on its own, with or without a pattern
attached to it.

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

Tension: reach for the interface once a second pricing model is in hand,
not on the first one; an interface with a single implementation is this
principle's cost with none of its benefit yet.

### Hide Implementation Details

A caller should depend on what a type does, not on how it stores the data
behind it, so the representation can change without touching every caller.

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

Tension: hiding a representation every caller legitimately needs,
iterating every event for an audit, for example, just adds a wrapper
method per field; hide what varies, expose what a caller actually
computes.

### Composition Over Inheritance

Prefer assembling an object out of small collaborators to building a deep
inheritance tree purely to reuse behavior. Inheritance is still the right
choice when a real is-a relationship holds and a caller needs to
substitute the derived type polymorphically; that case belongs to Liskov
Substitution, not to this one.

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

Tension: composition that forwards every single method of the composed
type is reinventing inheritance by hand, with more boilerplate and none of
the type relationship; reach for composition when reuse, not
substitutability, is what is being asked for.

### Orthogonality

Independent parts of a system should stay independent: changing one
should never force an unrelated change somewhere else because of a hidden
side effect.

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

Tension: pursuing orthogonality past the point of diminishing returns
produces parts so decoupled that a legitimate cross-cutting concern,
tracing one request across label submission and logging, needs its own
plumbing just to reconnect them.

## Coupling and cohesion

### Minimise Coupling

Fewer, narrower dependencies between classes mean a change in one ripples
into fewer places. Coupling cannot reach zero; two collaborating classes
must agree on something. The question is whether that agreement is a
narrow, published interface or a wide, accidental one.

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

Tension: minimizing coupling by passing primitive values instead of a
small, named type ("stringly typed" calls) trades one kind of coupling, to
a type, for a worse kind, to an unenforced convention every call site has
to get right on its own.

### Maximise Cohesion

Group data with the operations that act only on that data, so a reader
finds the logic next to the fields it touches instead of scattered across
the codebase.

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

Tension: pulling every method that merely accepts a `Shipment` parameter
onto the record itself, including logic that also depends heavily on
unrelated types, produces a god object; keep on the type only the logic
that depends solely on that type's own fields.

### Law of Demeter

A method should talk to itself, its parameters, objects it creates, and
its own direct components, not chain through a stranger's stranger to
reach a fact three hops away.

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

Tension: wrapping every possible chain in a delegating method bloats the
near type with pass-through methods that exist for no reason but to
satisfy the rule; apply it to chains an outside caller walks, not to every
accessor that could theoretically be chained.

### Command-Query Separation

A method should either do something or answer something, never both, so a
caller can read a query call without worrying it also changed state.

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

Tension: strict separation sometimes costs a real feature, an atomic
pop-and-return off a queue shared across threads being the common case;
some call sites accept a documented command that also returns a value
rather than pay for two round trips under one lock.

### Inversion of Control

Let a framework, a feed, or a caller decide when your code runs, instead
of your code deciding when to go ask. This is the general shape;
Dependency Inversion, recapped above, is the specific SOLID claim about
which abstraction the dependency points at once control is inverted.

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

Tension: inverting control is not the same as inverting a dependency. The
method reference above can still name a concrete `RateFeed` type directly
and violate Dependency Inversion even while correctly inverting who calls
whom.

## Interoperability and resilience

### Robustness Principle (Postel's Law)

Be conservative in what you send, liberal in what you accept. At a
boundary where another system's payload is outside your control, a
carrier's tracking webhook being the common case, tolerate variation
defensively on the way in, and never introduce variation of your own on
the way out.

```java
// Violates the Robustness Principle: throws on any field the schema does
// not explicitly list, so a carrier adding an undocumented field breaks
// every shipment update until the parser is patched.
TrackingUpdate parseWebhook(JsonValue body) {
    for (String key : body.fieldNames()) {
        if (!isKnownField(key)) {
            throw new IllegalStateException("unexpected field: " + key);
        }
    }
    return new TrackingUpdate(body.stringValue("status"), body.stringValue("timestamp"));
}
```

```java
// Honors the Robustness Principle: unknown fields are ignored, and a
// missing optional field falls back to a documented default.
TrackingUpdate parseWebhook(JsonValue body) {
    return new TrackingUpdate(
            body.stringValue("status", "unknown"),
            body.stringValue("timestamp", currentTimestampIso8601()));
}
```

Tension: "liberal in what you accept" taken past its limit can mask a real
producer bug, silently absorbing a field that should have raised an
alert; log what was ignored even while tolerating it, or a real data
problem becomes indistinguishable from a clean parse.

## Principles for teams and process

These five are not code-expressible; they describe how people and
schedules behave around code, not a shape a compiler checks.

**Boy-Scout Rule.** Leave the code you touch a little cleaner than you
found it: rename the field in `CarrierGateway` whose name no longer
matches what it holds while you are already in that file for a bug fix,
delete the stale comment next to the rate calculation you just corrected.
The rule compounds a codebase's quality over many small, incidental
improvements instead of waiting for a dedicated cleanup project that
rarely gets scheduled on its own. The tension: folding a boy-scout cleanup
into an unrelated bug-fix pull request makes the diff harder to review and
can bury the actual fix; keep the cleanup small and visible, in its own
commit, so a reviewer can separate the fix from the tidying.

**Code For The Maintainer.** Write code assuming the next reader does not
carry the context sitting in your head right now, and remember that the
next reader is often you, six months later, having forgotten exactly this
detail. In a shipping system that means naming a variable for the
quantity it represents, `outstandingBalanceCents`, rather than its type,
and writing a comment that explains why a check against a carrier's rate
limit exists rather than restating what the line already says. The
tension: optimizing purely for an inattentive reader produces comments
that repeat the code and defensive checks for conditions that structurally
cannot occur, burying the real logic in noise; write for a competent
engineer unfamiliar with this specific change, not for a careless one.

**Murphy's Law.** Whatever can go wrong will go wrong, and it tends to
happen under load, at the worst time. For a shipping system that means a
carrier's rate API will time out during a peak shipping day, a tracking
webhook will arrive with a field a contract promised would always be
present but is empty this one time, and a network partition will land
during the one deploy that mattered. The practical response is defensive
design concentrated at a boundary, a retrying carrier gateway, a
validating webhook intake, rather than trusting that a failure mode has
not happened yet as evidence it never will. The tension: treating every
internal call site as if the worst case applies duplicates defensive code
everywhere; put the defenses at the boundary where the real uncertainty
lives, not on every call downstream of it.

**Brooks's Law.** Adding people to a software project that is already
late makes it later, an observation Fred Brooks drew directly from
managing IBM's OS/360 project. New engineers draw ramp-up time from people
already stretched thin, and the number of communication paths across a
team grows faster than the team itself. A tracking-system migration
running two weeks behind a deadline does not get rescued by doubling the
engineers assigned to it mid-sprint. The tension: the law describes a late
project specifically, not team growth in general; a team given slack and
brought on well ahead of a deadline, rather than in a crunch, faces a
different situation the law does not cover.

**Linus's Law.** Given enough eyeballs, all bugs are shallow, a maxim from
open-source review culture associated with Linus Torvalds, claiming that
more independent reviewers surface more of a change's defects than one
reviewer working alone. Code review on a carrier-integration pull request
applies this principle directly, a second reviewer catching a retry-logic
edge case the author missed because they were too close to it. The
tension: the law assumes reviewers bring independent context; several
reviewers skimming the same invoice-calculation diff without understanding
the billing domain do not multiply scrutiny, they multiply rubber stamps.
The quality of a review matters more than the headcount of reviewers.

## For deeper coverage

- David Thomas, Andrew Hunt, [*The Pragmatic Programmer: Your Journey to Mastery*](https://pragprog.com/titles/tpp20/the-pragmatic-programmer-20th-anniversary-edition/) (20th Anniversary Edition, Addison-Wesley / Pragmatic Bookshelf): the source for DRY, orthogonality, and the Robustness Principle reviewed in this appendix.
- Robert C. Martin, [*Clean Code: A Handbook of Agile Software Craftsmanship*](https://www.pearson.com/en-us/subject-catalog/p/clean-code-a-handbook-of-agile-software-craftsmanship/P200000009044/9780132350884) (Prentice Hall): the source for the Boy-Scout Rule, Code For The Maintainer, and the do-one-thing discipline behind Curly's Law.
- Frederick P. Brooks Jr., [*The Mythical Man-Month: Essays on Software Engineering*](https://www.pearson.com/en-us/subject-catalog/p/Brooks-Mythical-Man-Month-Anniversary-Edition-The-Essays-On-Software-Engineering-2nd-Edition/P200000000149/9780132119160) (Anniversary Edition, Addison-Wesley): the source for Brooks's Law.
- Robert C. Martin, [*Agile Software Development: Principles, Patterns, and Practices*](https://www.pearson.com/en-gb/subject-catalog/p/agile-software-development-principles-patterns-and-practices-pearson-new-international-edition/P200000004932/9781292038360) (Prentice Hall): the original statement of the five SOLID principles the first family above recaps, taught in full in the SOLID chapters.
- Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480/9780321700698) (Addison-Wesley): the Gang of Four's own advice to favor composition over inheritance and to program to an interface, the root of several principles in the structure-and-change family.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
