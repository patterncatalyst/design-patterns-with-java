---
title: "Shortcut patterns: dispatch tables and maps"
order: 26
layout: tutorial
sectionid: docs
part: "concurrency-patterns-in-the-wild"
description: "Two idiomatic Java 25 shortcuts for routing a key to a short handler without a full Strategy or Command hierarchy, taught against a shipping event feed: an EnumMap built from an exhaustive switch expression, and a Map<String, handler> dispatch table that registers a handler at runtime and falls back on an unrecognized key."
duration: "25 min"
---

## Learning objectives

By the end of this chapter you can:

- Name the force that makes a dispatch table or map the right-sized tool: a
  key selects one short handler, and a full Strategy or Command hierarchy
  would be ceremony with nothing underneath it.
- Build an `EnumMap<K, handler>` from an exhaustive switch expression over a
  closed enum, and explain what guarantee the switch's missing `default`
  branch gives you that a hand-built array or if-chain does not.
- Build a `Map<K, handler>` dispatch table for an open, runtime-extensible
  key set, register a new handler into it after construction, and route an
  unrecognized key to a defined fallback instead of throwing.
- Compare constant-specific enum method bodies against an externally built
  `EnumMap`, and say which one keeps the enum as a plain key and which one
  makes the enum carry its own behavior.
- Recognize when a dispatch table has outgrown itself, when a handler needs
  memory across calls, needs more context than the key itself carries, or
  needs an undo path, and say what to reach for instead.
- Name the one real cost an open dispatch map pays that a closed,
  enum-keyed table does not.

## Why this matters

A shipping ingestion service sits behind two different sources of truth
about what just happened to a parcel. Its own domain code raises lifecycle
events it fully controls: a shipment was created, picked up, moved in
transit, put out for delivery, delivered, or flagged as an exception.
Separately, third-party carriers deliver webhooks with their own wire
codes, strings this codebase never declared and cannot enumerate in
advance. Both sources answer the same question for a different key shape:
given this event, which short block of code should run? A chain of
`if (type == ShipmentEventType.CREATED) { ... } else if (...)` answers that
question the least disciplined way, growing sideways with every event kind
and asking a reviewer to read a function that does several unrelated
things to confirm any one of them is correct.

The Gang of Four's answer to "behavior that varies with a selector" is a
full pattern: Strategy for an algorithm a caller picks and can swap at
runtime, Command for a request turned into an object with its own execution
and undo. Those patterns earn their cost when the behavior behind each
branch is substantial, holds state across calls, or needs to be reversed.
A handler that writes one line to a ledger and returns is a smaller
problem, and building a named class with a constructor and an interface
override for each one is over-engineering in the other direction: six
classes, a factory to construct the right one, and a test suite that mostly
asserts each class delegates to the same one-line body.

This chapter builds the lightweight answer to that smaller problem two
idiomatic ways, then names exactly where each one stops being enough. An
`EnumMap` built from an exhaustive switch expression resolves a known event
type to its handler in one map lookup, with the compiler itself refusing to
let a new event type go unhandled. A `Map<String, handler>` dispatch table
does the same job for a set of keys this codebase does not fully control,
letting a new carrier's wire code register a handler while the program is
already running, no recompile required.

{% include excalidraw.html name="26-shortcut-dispatch" caption="Figure 26.1: a closed enum key and an open string key both resolve through a small dispatch table to a short handler; an unmatched open key still has a fallback route defined." %}

## The domain: routing a shipping event feed

The example behind this chapter lives in `examples/26-shortcut-patterns`. A
`ShipmentEvent` pairs a closed `ShipmentEventType` enum, six constants this
codebase compiles against directly, with a tracking ID and a detail
string:

```java
public enum ShipmentEventType {
    CREATED,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    EXCEPTION
}

public record ShipmentEvent(ShipmentEventType type, String trackingId, String detail) {

    public ShipmentEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(trackingId, "trackingId");
        Objects.requireNonNull(detail, "detail");
    }
}
```

`CarrierWebhookEvent` carries a `code`, a plain string standing in for
whatever a third-party carrier's webhook actually sends on the wire, a
value this codebase has no control over and cannot enumerate ahead of
time:

```java
public record CarrierWebhookEvent(String code, String trackingId, String detail) {

    public CarrierWebhookEvent {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(trackingId, "trackingId");
        Objects.requireNonNull(detail, "detail");
    }
}
```

Both shapes resolve to short handler methods living in one place,
`ShipmentHandlers`: `handleCreated`, `handlePickedUp`, and so on through
`handleException`, each writing a single note into a shared `ShipmentLedger`.
Keeping the handler bodies in one shared class, rather than duplicating them
inside each dispatch mechanism, is deliberate: if the routing mechanism
changes, the behavior behind each route should not, and the only way to
make that true by construction rather than by care is to have both
mechanisms call the same methods.

## Form one: the EnumMap, checked complete by the compiler

`EnumDispatchRouter` resolves a `ShipmentEventType` to its handler through
an `EnumMap`, built once in the constructor from `handlerFor`, a switch
expression with one case per enum constant and no `default` branch:

```java
public final class EnumDispatchRouter {

    private final Map<ShipmentEventType, BiConsumer<ShipmentEvent, ShipmentLedger>> handlers;

    public EnumDispatchRouter() {
        this.handlers = Arrays.stream(ShipmentEventType.values())
                .collect(Collectors.toMap(
                        type -> type,
                        EnumDispatchRouter::handlerFor,
                        (first, second) -> first,
                        () -> new EnumMap<>(ShipmentEventType.class)));
    }

    public void route(ShipmentEvent event, ShipmentLedger ledger) {
        handlers.get(event.type()).accept(event, ledger);
    }

    private static BiConsumer<ShipmentEvent, ShipmentLedger> handlerFor(ShipmentEventType type) {
        return switch (type) {
            case CREATED -> ShipmentHandlers::handleCreated;
            case PICKED_UP -> ShipmentHandlers::handlePickedUp;
            case IN_TRANSIT -> ShipmentHandlers::handleInTransit;
            case OUT_FOR_DELIVERY -> ShipmentHandlers::handleOutForDelivery;
            case DELIVERED -> ShipmentHandlers::handleDelivered;
            case EXCEPTION -> ShipmentHandlers::handleException;
        };
    }
}
```

`route()` itself costs one map lookup and one call through a method
reference, no branching once the key is in hand. `EnumMap` stores its
entries in an array ordered by the enum's own declaration, so that lookup
is as close to array-indexed as a `Map` interface gets, with none of
`HashMap`'s hashing overhead. That is the case for reaching for `EnumMap`
specifically over a plain `HashMap<ShipmentEventType, handler>`: the key
type is already a small, ordered, closed set, and `EnumMap` is built for
exactly that shape.

The real payoff is in how `handlers` gets built. `handlerFor`'s switch
expression has no `default` branch, which means the compiler itself checks
that every declared `ShipmentEventType` constant has a case. Add a seventh
constant, `RETURNED_TO_SENDER`, to the enum without adding a matching case
to this switch, and the build fails at the switch expression, naming the
exact problem, before any shipment reaches the missing case in production.
That is a meaningfully stronger guarantee than a `switch` statement with a
`default` branch that throws `IllegalStateException`, or a hand-built array
indexed by `ordinal()`, both of which only discover a missing case at
runtime, on whichever event happens to exercise it first. The enum's
constants and the switch's cases are two separate pieces of source text
that have to agree, and only one of those two shapes, the `default`-free
switch expression, makes the compiler responsible for keeping them in sync
rather than a reviewer's memory.

### Two modern idioms: constant-specific bodies or a built dispatch map

A Java 25 codebase has a second idiomatic way to get constant-specific
behavior out of an enum: let each constant override an abstract method with
its own body, the same shape `Carrier` used for shipping-rate Strategy
implementations. Both idioms are current Java, and the choice between them
is a real design decision, not a style preference:

{% include codetabs.html langs="Constant-specific body|EnumMap + switch" %}

```java
// Constant-specific body: the enum itself implements the handler contract.
// Each constant supplies its own route() override, so the behavior lives
// inside the enum declaration and travels with it wherever the enum goes.
public enum ShipmentEventType implements ShipmentEventHandler {
    CREATED {
        @Override
        public void route(ShipmentEvent event, ShipmentLedger ledger) {
            ledger.record(event.trackingId(), "created: " + event.detail());
        }
    },
    PICKED_UP {
        @Override
        public void route(ShipmentEvent event, ShipmentLedger ledger) {
            ledger.record(event.trackingId(), "picked-up: " + event.detail());
        }
    }
    // ... one body per remaining constant
}
```
```java
// EnumMap + switch: the enum stays a plain key with no behavior of its
// own. A separate exhaustive switch builds the table that maps each key to
// a handler method defined elsewhere, and that handler is reusable by code
// that has nothing to do with ShipmentEventType at all.
private static BiConsumer<ShipmentEvent, ShipmentLedger> handlerFor(ShipmentEventType type) {
    return switch (type) {
        case CREATED -> ShipmentHandlers::handleCreated;
        case PICKED_UP -> ShipmentHandlers::handlePickedUp;
        // ... one line per remaining constant
    };
}
```

Both forms get the same compile-time completeness guarantee: a constant
with no method body fails to compile under the first form exactly as a
switch expression missing a case fails to compile under the second. The
difference is where the behavior lives. Constant-specific bodies couple the
enum to its handling logic directly; `ShipmentEventType` becomes something
that both identifies an event and knows what to do about it, which reads
well when the enum exists for exactly one purpose. The `EnumMap`-and-switch
form keeps `ShipmentEventType` a plain value, usable anywhere an event type
needs to be named, serialized, or compared, with the handler logic sitting
in `ShipmentHandlers` where a second dispatch path, a test, or an unrelated
reporting job can call the same methods directly without going through the
enum at all. `examples/26-shortcut-patterns` uses the second form because
`ShipmentHandlers`'s methods are also called directly from the test suite
and the demo independent of either router; reach for constant-specific
bodies instead when the enum's only job in the codebase is to carry that
one behavior.

## Form two: the open dispatch map, registered at runtime

A carrier's `code` is not one of six values this codebase enumerated ahead
of time. It is a string, and a new carrier integrated next quarter sends
codes nobody wrote a case for. `CarrierWebhookRouter` answers that with a
plain `Map<String, handler>` instead of an `EnumMap`:

```java
public final class CarrierWebhookRouter {

    public static final String UNRECOGNIZED_CODE_NOTE = "unrecognized webhook code, routed to fallback";

    private final Map<String, BiConsumer<CarrierWebhookEvent, ShipmentLedger>> handlers = new HashMap<>();

    public CarrierWebhookRouter() {
        handlers.put("created", (event, ledger) -> ledger.record(event.trackingId(), "created: " + event.detail()));
        handlers.put("picked_up", (event, ledger) -> ledger.record(event.trackingId(), "picked-up: " + event.detail()));
        handlers.put("in_transit", (event, ledger) -> ledger.record(event.trackingId(), "in-transit: " + event.detail()));
        handlers.put("delivered", (event, ledger) -> ledger.record(event.trackingId(), "delivered: " + event.detail()));
    }

    public void register(String code, BiConsumer<CarrierWebhookEvent, ShipmentLedger> handler) {
        handlers.put(Objects.requireNonNull(code, "code"), Objects.requireNonNull(handler, "handler"));
    }

    public void route(CarrierWebhookEvent event, ShipmentLedger ledger) {
        handlers.getOrDefault(event.code(), CarrierWebhookRouter::handleUnrecognizedCode)
                .accept(event, ledger);
    }

    private static void handleUnrecognizedCode(CarrierWebhookEvent event, ShipmentLedger ledger) {
        ledger.record(event.trackingId(), UNRECOGNIZED_CODE_NOTE + ": " + event.code());
    }
}
```

`register()` is the whole point of choosing a map over an enum-keyed
table: a caller reaches it after construction, at any point while the
program is already running, and the dispatch table's behavior changes with
no recompile and no change to `CarrierWebhookRouter`'s own source. The
example's demo registers a handler for `"customs_hold"`, a code none of the
four built-in entries above know about, after `CarrierWebhookRouter` is
already constructed, and the very next `route()` call dispatches to it
correctly.

`route()` leans on `getOrDefault()` rather than a manual `containsKey()`
check followed by a `get()`, which matters for more than brevity: a
two-step check-then-fetch invites a bug where the check and the fetch drift
apart as the method grows, while `getOrDefault()` makes the fallback part of
the same expression that does the lookup. `handleUnrecognizedCode` records
the raw code for whoever reviews the ledger later, rather than throwing or
silently dropping the event. That fallback matters more here than it would
for the closed enum form. A `ShipmentEventType` value can only ever be one
of six compiler-checked constants; a `String` key has no such guarantee at
all, so every open dispatch table needs a defined answer for "I have never
seen this key," not as an edge case patched on later but as a first-class
branch of `route()` from the start.

## When a table outgrows itself

Both forms in this chapter answer exactly one question: which short
function runs for this key. The moment the answer needs more than a
function call, a table is the wrong tool, and the full pattern it was
standing in for is the right one.

Strategy becomes the right tool again the moment a handler needs to hold
state across calls or needs to be swapped by a caller at runtime rather
than selected by an incoming key. This chapter's handlers are stateless
methods chosen once per call by a key that arrives with the event. If
handling a `DELIVERED` webhook needed to remember how many times a given
tracking ID had already been marked delivered, so a second delivery
notice for the same shipment is treated as a carrier error rather than a
routine update, that memory belongs in an object a caller owns and
configures, not in a static table entry.

Command becomes the right tool again when a dispatched action needs to be
undone, queued independently of the event that triggered it, or replayed
later. This chapter's handlers run once and return nothing a caller can
later reverse. If recording an exception needed an undo path, because an
operator sometimes flags an exception in error and needs to roll the
ledger entry back, that need is an object with its own apply and undo, not
a method reference sitting in a map.

A third signal, specific to the open form, is a key space that starts
encoding more than an identity. If a carrier's wire code began carrying a
severity level or a retry count packed into the string itself, parsing
that out of the key before dispatch is a sign the key has stopped being a
simple lookup token and the routing decision needs a small parser or a
dedicated value type in front of it, not a bigger map with more encoded
string keys.

> **AI sidebar.** Ask an LLM pairing partner to "add dispatch for shipment
> events" and a common result reaches for a `Map<String, Runnable>` or
> `Map<Enum, Consumer>` even when the key set is small, fixed, and known at
> the call site, which is exactly the case an `EnumMap` built from an
> exhaustive switch handles with a compile-time completeness guarantee a
> generated `HashMap.put()` call list does not. The generated code also
> tends to skip the fallback branch on the open form entirely, assuming
> every webhook code the map will ever see was already written into the
> constructor. Read a generated dispatch table the way this chapter trained:
> ask whether the key set is open or closed before accepting the container
> choice, and ask what happens, today, if a key arrives that nobody wrote a
> row for. If nothing defined answers that question, the fix is a fallback
> branch or an exhaustive switch, not a bigger map.

## When not to use it

A dispatch table or map is worth the extra code the moment a chain of
`if`/`else if` branches on the same key would otherwise grow past three or
four cases, because at that size a table reads as data a reviewer scans in
one pass, while an `if` chain reads as code traced branch by branch. Below
that size, the `if` chain is not a problem to fix. Two or three short
branches, each self-contained, cost a reader less than a map field, a
constructor that seeds it, and a fallback method would. Introducing
`CarrierWebhookRouter` for two webhook codes solves a problem of scale that
has not arrived yet.

The enum-keyed form specifically should not be reached for when the
"handler" behind each key is not actually a short, stateless function but a
disguised object: a value that needs a constructor argument, holds a
collaborator, or needs to be mocked independently in a test. At that point
the map is hiding a family of classes behind a lookup, and naming that
family directly as a Strategy or Command is the clearer design.

The open map's flexibility is a cost as well as a feature. It cannot tell a
caller, at compile time, whether a code it is about to dispatch exists;
every single call pays the branch for "what if this key was never
registered," a branch the enum-keyed form only pays once, at whatever
boundary parses an untrusted value into a `ShipmentEventType` in the first
place. Reach for an open map specifically when the key set is actually
open, a third party controls part of it, or a registration step at runtime
is a real requirement, not merely because `Map` is a familiar, flexible
container. And reach past both forms in this chapter, back to Strategy or
Command, the moment a handler needs memory across calls, needs more
context than the key itself provides, or needs an undo path; this
chapter's tables answer "which function runs," not "what does an evolving,
stateful collaborator do."

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this
loop. From the repository root, the aggregator picks up
`examples/26-shortcut-patterns` once it is registered there:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/26-shortcut-patterns
mvn test
```

Either path runs the same 20 JUnit 5 cases (AssertJ assertions), including
a parameterized suite over every `ShipmentEventType` constant and every
built-in webhook code, a case proving an unrecognized webhook code falls
back instead of throwing, and a case proving `register()` makes a new code
routable on the very next call. Observed locally on Fedora with Temurin
25.0.2:

```
Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see both forms route real events side by side rather than just reading
an assertion that says they work, run the example's own demo script:

```sh
examples/26-shortcut-patterns/demo.sh
```

It routes a short sequence of events through the `EnumMap` form, then the
open webhook map, including a code registered after construction:

```
== Form one: EnumMap built from an exhaustive switch ==
  SH-1001: created: order placed with FastFreight
  SH-1001: picked-up: scanned at origin depot
  SH-1001: delivered: left on porch

== Form two: Map<String, handler>, open and extensible ==
Built-in webhook codes registered: 4
Webhook codes registered after onboarding: 5
  SH-2002: created: order placed with RegionalParcel
  SH-2002: in-transit: departed regional hub
  SH-2002: customs-hold: awaiting duty payment
  SH-2002: unrecognized webhook code, routed to fallback: unknown_event
```

Notice the webhook code count climbing from 4 to 5 between the two "Form
two" lines: `"customs_hold"` was never one of the four built-in entries,
and the very next `route()` call after `register()` dispatches to it
correctly, with no restart and no recompile.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design
  Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480)
  (Addison-Wesley): the Strategy and Command entries this chapter's two
  dispatch shortcuts stand in for at a smaller scale, and the source to
  reach for the moment a handler needs more than a function call.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991),
  third edition (Addison-Wesley): Item 34 covers enum constants with
  constant-specific method bodies as the alternative this chapter compares
  directly against a separately built `EnumMap`.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/)
  and [AssertJ documentation](https://assertj.github.io/doc/): the testing
  idiom behind `examples/26-shortcut-patterns`'s suite, which asserts that
  every key, closed and open, routes to its own handler and that an
  unregistered key falls back rather than throwing.
- The full reading list behind this book's opinions lives on the
  [bibliography page]({{ '/bibliography/' | relative_url }}); each citation
  there is a pointer to the full book, not a substitute for reading it.
