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

- Name the force that makes a dispatch table the right size: a key selects one short handler, and a Strategy or Command hierarchy would be ceremony.
- Build an `EnumMap<K, handler>` from an exhaustive switch expression, and say what its missing `default` buys over an array or an if-chain.
- Build a `Map<K, handler>` for an open key set, register a handler after construction, and route an unknown key to a fallback.
- Compare constant-specific enum bodies with an external `EnumMap`, and say where each puts the behavior.
- Recognize a table that has outgrown itself: a handler that needs memory, more context than the key, or an undo.
- Name the cost an open map pays that an enum-keyed table doesn't.

## Why this matters

A shipping ingestion service hears about parcels from two sources. Its own domain code raises lifecycle events it controls: created, picked up, in transit, out for delivery, delivered, exception. Third-party carriers send webhooks with their own wire codes, strings the codebase never declared and can't enumerate. Both ask the same question with a different key: which short block of code runs for this event? An `if (type == ShipmentEventType.CREATED) { ... } else if (...)` chain answers it worst, growing with every event kind into one function that does several unrelated things.

The GoF answer to behavior that varies by selector is Strategy, for an algorithm a caller picks and can swap, or Command, for a request object with its own execution and undo. They earn their cost when each branch is substantial, holds state or must be reversed. A handler that writes one ledger line doesn't: six classes, a factory and tests that each check a one-line delegation is over-engineering.

Two lighter forms fit. An `EnumMap` built from an exhaustive switch expression resolves a known event type in one lookup, and the compiler refuses an unhandled type. A `Map<String, handler>` does the same for keys the codebase doesn't control, and accepts a new carrier code while the program runs.

{% include excalidraw.html name="26-shortcut-dispatch" caption="Figure 26.1: a closed enum key and an open string key both resolve through a small dispatch table to a short handler; an unmatched open key still has a fallback route defined." %}

## The domain: routing a shipping event feed

The code lives in `examples/26-shortcut-patterns`. A `ShipmentEvent` pairs a closed `ShipmentEventType` enum of six constants with a tracking ID and a detail string:

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

`CarrierWebhookEvent` carries a `code`, a plain string standing in for whatever a carrier's webhook sends:

```java
public record CarrierWebhookEvent(String code, String trackingId, String detail) {

    public CarrierWebhookEvent {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(trackingId, "trackingId");
        Objects.requireNonNull(detail, "detail");
    }
}
```

Each handler writes one note into a shared `ShipmentLedger`. The enum's handlers are static methods on `ShipmentHandlers`, from `handleCreated` through `handleException`; the webhook router's built-in handlers are lambdas in its constructor.

## Pattern: Enum dispatch table

### The problem

Six internal event types each need a one-line handler. An if-chain or a `switch` statement with a throwing `default` finds a missing type only at runtime, on the first event of that type. A Strategy class per type is six classes for six lines.

### The solution

**An enum dispatch table maps each constant of a closed enum to a short handler in an `EnumMap`, built from a switch expression the compiler checks for completeness.**

### How Java models it

`EnumDispatchRouter` builds the map once in its constructor from `handlerFor`, which has one case per constant and no `default`:

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

`route()` is one map lookup and one call. `EnumMap` stores entries in an array ordered by declaration, so the lookup is close to array indexing, without `HashMap`'s hashing. That's why it beats a `HashMap<ShipmentEventType, handler>` for a small, closed, ordered key type.

The guarantee comes from `handlerFor`. Add a seventh constant, `RETURNED_TO_SENDER`, without a case and the build fails with `the switch expression does not cover all possible input values`. A `switch` statement with a throwing `default`, or an array indexed by `ordinal()`, finds the gap only at runtime. The enum's constants and the switch's cases are two pieces of source that must agree, and the `default`-free switch expression makes the compiler keep them in step.

### Constant-specific bodies or a built dispatch map

An enum can also carry the behavior itself, each constant overriding an abstract method, the shape a rate-per-carrier Strategy enum uses. Both are current Java:

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

Both get the same compile-time check: a constant with no body fails to compile just as a missing case does. They differ in where the behavior lives:

| Form | The enum is | Fits when |
| --- | --- | --- |
| Constant-specific body | An identifier that also knows what to do | The enum exists only to carry that one behavior |
| `EnumMap` + switch | A plain value to name, serialize or compare | The handlers in `ShipmentHandlers` should be callable without going through the enum, by a second dispatch path or a reporting job |

`examples/26-shortcut-patterns` uses the second form, keeping `ShipmentEventType` a plain key.

## Pattern: Open dispatch map

### The problem

A carrier's `code` isn't one of six known values. A carrier integrated next quarter sends codes nobody wrote a case for, and onboarding it shouldn't need a recompile. An unknown code must not crash ingestion or vanish.

### The solution

**An open dispatch map keys short handlers by a string, accepts new entries at runtime, and routes any key it doesn't hold to a defined fallback.**

### How Java models it

`CarrierWebhookRouter` uses a plain `Map<String, handler>`:

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

`register()` is the reason to choose a map: a caller adds a code after construction and the next `route()` dispatches to it, with no change to `CarrierWebhookRouter`. A handler registered for `"customs_hold"`, which none of the four built-in entries know, routes on the very next call.

### The fallback is a first-class branch

`route()` uses `getOrDefault()` rather than `containsKey()` then `get()`, so the lookup and the fallback are one expression and can't drift apart as the method grows. `handleUnrecognizedCode` records the raw code in the ledger instead of throwing or dropping the event. A `ShipmentEventType` can only be one of six checked constants; a `String` has no such limit, so every open table needs a defined answer for a key it has never seen, from the start.

## When a table outgrows itself

Both forms answer one question: which short function runs for this key. When the answer needs more, go back to the full pattern:

| Signal | Example | Reach for |
| --- | --- | --- |
| A handler needs memory across calls, or a caller swaps it at runtime | A second `DELIVERED` notice for one tracking ID should count as a carrier error | Strategy: an object the caller owns and configures |
| An action must be undone, queued apart from its event, or replayed | An operator flags an exception in error and rolls the ledger entry back | Command: an object with apply and undo |
| The key encodes more than identity | A wire code that packs in a severity or retry count | A small parser or value type in front of the dispatch |

> **🤖 Explore with your agent**
>
> "In `examples/26-shortcut-patterns`, add a `RETURNED_TO_SENDER` constant to `ShipmentEventType`, a `handleReturnedToSender` static method on `ShipmentHandlers` that records `returned-to-sender: ` plus the detail, and one `case` for it in `EnumDispatchRouter.handlerFor`. Keep `handlerFor` a switch expression with no `default`, keep the enum a plain key with no method bodies, and don't add a `HashMap` or an if-chain. Separately, register a `returned` webhook code with `CarrierWebhookRouter.register` from the caller, without editing the router's constructor, and leave `getOrDefault` and the fallback as they are."
>
> Check: `mvn -q -f examples/26-shortcut-patterns/pom.xml test -Dtest=EnumDispatchRouterTest,CarrierWebhookRouterTest`; removing the new `case` must break the build.

## When not to use it

- **Two or three branches.** A table pays off when an if-chain on one key would pass three or four cases, because then a table reads as data scanned in one pass. Below that, a map field, a seeding constructor and a fallback cost more than the chain. `CarrierWebhookRouter` for two codes solves a problem that hasn't arrived.
- **A disguised object.** When a handler needs a constructor argument, holds a collaborator or needs mocking in isolation, the map hides a family of classes. Name it as a Strategy or Command.
- **A closed key set in an open map.** The open map can't tell a caller at compile time whether a code exists, so every call pays the "never registered" branch. The enum form pays that once, where an untrusted value is parsed into a `ShipmentEventType`. Use the open map when a third party controls part of the key set or runtime registration is a requirement, not because `Map` is familiar.

## Build, run, observe

From the repository root:

```sh
mvn -f examples/pom.xml test
```

Or standalone:

```sh
cd examples/26-shortcut-patterns
mvn test
```

All 20 tests pass, including every `ShipmentEventType` constant, every built-in webhook code, the fallback and a code registered after construction. The demo routes events through both forms:

```sh
examples/26-shortcut-patterns/demo.sh
```

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

The count rises from 4 to 5 when `"customs_hold"` is registered, and the next `route()` reaches it with no restart. One experiment, in a scratch copy:

- **Add an unhandled constant.** Add `RETURNED_TO_SENDER` to `ShipmentEventType`. Compilation fails in `EnumDispatchRouter` with `the switch expression does not cover all possible input values`.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design
  Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480)
  (Addison-Wesley): the Strategy and Command entries these dispatch
  shortcuts stand in for at a smaller scale, and the source to reach for
  once a handler needs more than a function call.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991),
  third edition (Addison-Wesley): Item 34 covers enum constants with
  constant-specific method bodies, the alternative compared here with a
  separately built `EnumMap`.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/)
  and [AssertJ documentation](https://assertj.github.io/doc/): the testing
  idiom behind `examples/26-shortcut-patterns`.
- The full reading list lives on the
  [bibliography page]({{ '/bibliography/' | relative_url }}).
