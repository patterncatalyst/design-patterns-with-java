# 33 - Final project: a shipment dispatch center

The final project composes five patterns this book taught separately into
one running system: a `ShipmentBuilder` (Builder) that resolves a carrier's
`RateStrategy` (Strategy) and produces an opening quote, a `Shipment` that is
both the State machine's context and the Observer's subject, three
`ShipmentCommand` implementations that each wrap one reversible operation,
and a `ShipmentDispatchCenter` composition root that is the only type in the
system that knows about all five.

## The domain

- `Money`, `DestinationZone`, `ShipmentStatus`, `ShipmentEvent`: small value
  types shared across the whole example.
- `Shipment`: holds the current status, the current weight, the current
  quote, and the `RateStrategy` it was opened with. It enforces its own
  transition table and fans a legal transition out to every subscribed
  `ShipmentStatusListener`.
- `ShipmentBuilder`: a fluent Builder. `build()` looks up a `RateStrategy`
  from a small table of known carriers (`GROUND`, `AIR`, `FREIGHT`) unless a
  caller overrides it with `rateStrategy(...)`, then returns a `Shipment`
  that already carries its opening quote.
- `RateStrategy`, `FlatRateStrategy`, `WeightTieredRateStrategy`: the
  Strategy contract and two interchangeable implementations.

## The three commands

1. **`AdvanceShipmentCommand`** (Command drives State): applies an event
   through `Shipment.advance()`; `undo()` restores the previous status
   through `restoreStatusForUndo()`, not by walking the transition table
   backward, because a real carrier has no "un-pick-up" event.
2. **`AdjustWeightCommand`** (Command invokes Strategy): sets a corrected
   weight and asks the shipment's own `RateStrategy` for a fresh quote;
   `undo()` restores the exact captured weight and quote rather than
   recomputing.
3. **`SwitchRateStrategyCommand`** (Command swaps Strategy): replaces the
   shipment's `RateStrategy` and requotes against the new one; `undo()`
   restores both the previous strategy reference and its last quote.

All three implement `ShipmentCommand` (`execute()`/`undo()`) and run through
`CommandHistory`, a two-`Deque` undo/redo stack.

## The composition root

`ShipmentDispatchCenter` is the one type that knows about all five patterns.
`subscribe()` delegates to the shipment's Observer registration; `advance()`,
`adjustWeight()`, and `switchRateStrategy()` each build the matching command
and run it through `apply()`; `undoLast()`/`redoLast()` delegate to
`CommandHistory`. No command implementation references another, and no
`ShipmentStatusListener` knows a command was ever involved.

`ShipmentDispatchDemo.java` runs one shipment through the whole system end
to end and prints every step.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

This example is standalone-buildable from this directory:

```sh
cd examples/33-final-project
mvn test
```

It is also picked up by the repo root's aggregator:

```sh
mvn -f examples/pom.xml test
```

To see the composition play out rather than read about it, run the demo:

```sh
./demo.sh
```

Expected output:

```
== 1. open(): ShipmentBuilder resolves a RateStrategy ==
carrier: GROUND
opening quote at 8kg: $19.40

== 2-3. Observer: lifecycle events fan out as they happen ==
  [observer] SHIP-9100: CREATED -> PICKED_UP
  [observer] SHIP-9100: PICKED_UP -> IN_TRANSIT

== 4. Command invokes Strategy: a warehouse rescale to 15kg ==
weight now: 15.0kg, recomputed quote: $32.00

== 5. The rescale turns out to be a scanner glitch; undo it ==
weight restored to: 8.0kg, quote restored to: $19.40

== Bonus: swapping the rate strategy mid-flight, then undoing it ==
expedited quote at 8kg: $29.80
strategy swap undone, quote restored to: $19.40

== 6. Delivering the shipment ==
  [observer] SHIP-9100: IN_TRANSIT -> DELIVERED

== Operator mis-scanned PICK_UP again by habit; the console rejects it ==
rejected: cannot apply event 'PICK_UP' to a shipment in status 'DELIVERED'
```

## Files

```
examples/33-final-project/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run ShipmentDispatchDemo
  src/main/java/com/patterncatalyst/designpatterns/ch33/shipping/
    Money.java                          # value type: exact cents
    DestinationZone.java                # enum: LOCAL..INTERNATIONAL
    ShipmentStatus.java                 # enum: the lifecycle's five statuses
    ShipmentEvent.java                  # enum: PICK_UP, DISPATCH, DELIVER, CANCEL
    IllegalShipmentTransitionException.java
    RateStrategy.java                   # Strategy contract
    FlatRateStrategy.java               # record: fixed price
    WeightTieredRateStrategy.java       # record: base + per-kg price
    ShipmentStatusListener.java         # Observer contract
    Subscription.java                   # AutoCloseable unsubscribe handle
    Shipment.java                       # State context + Observer subject
    ShipmentBuilder.java                # Builder: resolves a RateStrategy
    ShipmentCommand.java                # Command contract: execute()/undo()
    AdvanceShipmentCommand.java         # Command drives State
    AdjustWeightCommand.java            # Command invokes Strategy
    SwitchRateStrategyCommand.java      # Command swaps Strategy
    CommandHistory.java                 # undo/redo stack (two Deques)
    ShipmentDispatchCenter.java         # the composition root
    ShipmentDispatchDemo.java           # exercises everything end to end
  src/test/java/com/patterncatalyst/designpatterns/ch33/shipping/
    ShipmentBuilderTest.java            # carrier lookup, overrides, rejections
    ShipmentLifecycleTest.java          # legal and illegal transitions
    ShipmentObserverTest.java           # fan-out, unsubscribe, self-unsubscribe
    AdvanceShipmentCommandTest.java     # execute/undo on the lifecycle
    AdjustWeightCommandTest.java        # execute/undo on weight + quote
    SwitchRateStrategyCommandTest.java  # execute/undo on the strategy itself
    CommandHistoryTest.java             # LIFO undo/redo, redo-branch clearing
    RateStrategyContractTest.java       # parameterized: strategies are interchangeable
    ShipmentDispatchCenterIntegrationTest.java  # the composed system, end to end
```
