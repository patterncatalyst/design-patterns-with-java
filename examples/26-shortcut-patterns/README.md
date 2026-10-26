# 26 - Shortcut patterns: dispatch tables and maps

A shipping ingestion service routes inbound events to a short handler by a
key: a lifecycle event type it raised itself, or a raw webhook code a
carrier sent on the wire. Neither side needs a full Strategy or Command
class hierarchy; each handler is a few lines, and a full class for each one
would be ceremony with nothing under it. This example builds the two
shortcut mechanisms that fit that smaller problem.

## The domain

- `ShipmentEventType` is a closed enum this codebase compiles against
  directly: `CREATED`, `PICKED_UP`, `IN_TRANSIT`, `OUT_FOR_DELIVERY`,
  `DELIVERED`, `EXCEPTION`.
- `ShipmentEvent` is a record pairing a `ShipmentEventType` with a tracking
  ID and a detail string.
- `CarrierWebhookEvent` is a record pairing a raw string `code`, standing in
  for whatever a third-party carrier's webhook actually sends, with the same
  tracking ID and detail shape.
- `ShipmentHandlers` holds the short handler methods both dispatch forms
  route to (`handleCreated`, `handlePickedUp`, and so on). Keeping the
  handler bodies shared means a routing change never changes what gets
  recorded, only how the handler was found.
- `ShipmentLedger` / `InMemoryShipmentLedger` is the one thing every handler
  writes a note to.

## The two dispatch shortcuts

1. **`EnumDispatchRouter`**: an `EnumMap<ShipmentEventType, handler>` built
   once from `handlerFor(ShipmentEventType)`, an exhaustive switch
   expression with no `default` branch. Add a seventh `ShipmentEventType`
   constant without a matching case in that switch, and the build fails
   before any shipment reaches the missing case. `route()` is one map
   lookup plus one call.
2. **`CarrierWebhookRouter`**: a `Map<String, handler>` dispatch table for
   an open set of carrier wire codes. `register()` lets a new carrier's code
   be added while the program is already running, with no recompile.
   `route()` uses `getOrDefault()` with `handleUnrecognizedCode` as the
   fallback, so an unseen code is recorded rather than thrown.

`ShortcutDispatchDemo.java` exercises both routers against a short sequence
of events and prints every routed note.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

This example is standalone-buildable from this directory:

```sh
cd examples/26-shortcut-patterns
mvn test
```

It is also picked up by the repo root's aggregator once registered there:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 20 JUnit 5 cases (AssertJ assertions): a
parameterized suite that routes every `ShipmentEventType` constant and every
built-in webhook code to its own handler, a case proving an unrecognized
webhook code falls back instead of throwing, and a case proving
`register()` makes a new code routable on the very next call.

To see both forms route real events side by side rather than just reading an
assertion that says they work, run the demo:

```sh
./demo.sh
```

Expected output:

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

The webhook code count climbing from 4 to 5 between the two "Form two"
lines is `register("customs_hold", ...)` taking effect: the very next
`route()` call after registration routes to it correctly, with no restart
and no recompile.

## Files

```
examples/26-shortcut-patterns/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run ShortcutDispatchDemo
  src/main/java/com/patterncatalyst/designpatterns/ch26/shipping/
    ShipmentEventType.java        # closed enum: the six lifecycle events
    ShipmentEvent.java             # record: type, trackingId, detail
    CarrierWebhookEvent.java        # record: code, trackingId, detail
    ShipmentLedger.java              # the shared recording contract
    InMemoryShipmentLedger.java       # in-memory ledger for demo + tests
    ShipmentHandlers.java              # the short handlers both forms call
    EnumDispatchRouter.java             # form one: EnumMap + exhaustive switch
    CarrierWebhookRouter.java            # form two: Map<String, handler> + fallback
    ShortcutDispatchDemo.java             # exercises both forms
  src/test/java/com/patterncatalyst/designpatterns/ch26/shipping/
    EnumDispatchRouterTest.java    # every closed-set key routes correctly
    CarrierWebhookRouterTest.java   # every open-set code, fallback, register()
    InMemoryShipmentLedgerTest.java  # recording order and unmodifiable view
    ShipmentEventTest.java            # value-type validation edge cases
```
