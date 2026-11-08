---
title: "Final project"
order: 33
layout: tutorial
sectionid: docs
part: "putting-it-together"
description: "A shipment dispatch center that composes Builder, State, Command, Observer and Strategy into one running system, taught at the seams where the patterns meet."
duration: "35 min"
---

## Learning objectives

By the end of this chapter you can:

- Compose Builder, State, Command, Observer and Strategy into one running system, and name the object that owns each role.
- Identify the five seams where two patterns meet: Builder resolving Strategy, Command driving State, Command invoking Strategy, Command swapping Strategy, and State firing Observer.
- Design one composition root that wires the patterns together without any collaborator knowing about the others.
- Prove the composed system end to end with an integration test: a full lifecycle with a resolved rate, fired notifications, a reversed correction and a swapped pricing rule.
- Recognize when composing this many patterns is over-engineering.

{% include excalidraw.html name="33-final-project-composition" caption="Figure 33.1: ShipmentDispatchCenter as the composition root, with ShipmentBuilder resolving a RateStrategy, three Commands acting on the shared Shipment, and Shipment notifying every subscribed ShipmentStatusListener." %}

## Why this matters

Each pattern so far came with one force, one example and its own tests. That's how to learn what Strategy buys or what Command's undo costs, and it isn't how the patterns appear in a real system. A dispatch center doesn't run a construction step, a status machine and a notification pipeline side by side with nothing connecting them. It has one `Shipment` whose lifecycle fires events that something reacts to, one rate calculation invoked on demand, and one carrier-selection step that hands the right pieces downstream. The patterns meet at seams, and that's where two of them have to cooperate inside one method.

The system composes a `ShipmentBuilder` (Builder) that resolves a carrier's rate rule and produces an opening quote; a `Shipment` that is both a State machine's context and an Observer's subject; and three `ShipmentCommand` implementations (Command) for the reversible operations a dispatcher needs: advancing the lifecycle, correcting a weight and swapping the pricing rule. It doesn't re-teach any pattern. It's about the five seams.

## The domain: a shipment dispatch center

The code is in `examples/33-final-project`. A `Shipment` carries an id, a carrier code, a destination zone, a weight in kilograms and a quoted rate as a `Money` record, so two quotes compare exactly. Its status is one of `CREATED`, `PICKED_UP`, `IN_TRANSIT`, `DELIVERED` or `CANCELLED`. Four events, `PICK_UP`, `DISPATCH`, `DELIVER` and `CANCEL`, move it, with `CANCEL` legal only from the two earliest statuses. `Shipment.advance()` checks a transition table keyed by the current status:

```java
public void advance(ShipmentEvent event) {
    Map<ShipmentEvent, ShipmentStatus> legalFromHere = TRANSITIONS.getOrDefault(status, Map.of());
    ShipmentStatus next = legalFromHere.get(event);
    if (next == null) {
        throw new IllegalShipmentTransitionException(status, event);
    }
    ShipmentStatus previous = status;
    status = next;
    notifyListeners(previous, status);
}
```

That's the whole State component: a context that owns its status, rejects an event that doesn't apply, and notifies only after a transition succeeds. `Shipment` is also the Observer subject, holding a map of subscribed `ShipmentStatusListener` callbacks. Putting both roles on one type is the first seam: the State machine's success and the Observer's trigger are the same moment, with nothing between them to drift out of sync.

## Seam one: Builder resolves Strategy

`ShipmentBuilder` is a fluent Builder with `id()`, `carrier()`, `destinationZone()` and `weightKg()`. What makes it more than ceremony is what `build()` does with the carrier code:

```java
public Shipment build() {
    Objects.requireNonNull(id, "id must be set before build()");
    Objects.requireNonNull(carrierCode, "carrier must be set before build()");
    Objects.requireNonNull(destinationZone, "destinationZone must be set before build()");
    Objects.requireNonNull(weightKg, "weightKg must be set before build()");

    RateStrategy resolvedStrategy = rateStrategy != null ? rateStrategy : resolveStrategy(carrierCode);
    return new Shipment(id, carrierCode, destinationZone, weightKg, resolvedStrategy);
}
```

`resolveStrategy()` maps a carrier code through a small table:

| Carrier | `RateStrategy` |
| --- | --- |
| `GROUND` | Weight-tiered |
| `AIR` | Flat rate |
| `FREIGHT` | Weight-tiered with a higher base and a lower per-kilogram charge |

No caller of the builder names `WeightTieredRateStrategy` or `FlatRateStrategy`, and neither does the `Shipment` constructor; both hold a `RateStrategy` and call `quote()`. `rateStrategy()` is the escape hatch: a one-off contract with a carrier the table doesn't know passes its own strategy, and `build()` skips the lookup.

Here the builder takes on a second job, choosing the pricing algorithm on top of assembling fields. A growing carrier list is the signal to split the lookup into its own type. For three carriers, keeping it in the builder that already has the carrier code is simpler.

## Seam two: Command drives State

`AdvanceShipmentCommand` wraps one call to `Shipment.advance()` and records the status first, so `undo()` has something to restore:

```java
@Override
public void execute() {
    previousStatus = shipment.status();
    shipment.advance(event);
}

@Override
public void undo() {
    shipment.restoreStatusForUndo(previousStatus);
}
```

Undo doesn't fire a reverse event such as an "un-pick-up"; carriers don't support one, and neither does this machine. `restoreStatusForUndo()` is a narrower entry point that bypasses the transition table and puts the old status back. It's the snapshot technique Command uses for any action without a clean inverse. The restore still notifies every listener, because an operator correcting a mis-scanned pickup is an event a dashboard should see.

`ShipmentDispatchCenter.advance()` is the only path from an operator console to `shipment.advance()`, through a command run by its `CommandHistory`. That makes every lifecycle change undoable by construction, not only the ones someone remembered to wrap.

## Seam three: Command invokes Strategy

A warehouse rescale corrects a weight after the shipment was quoted. `AdjustWeightCommand.execute()` sets the new weight and asks the shipment's existing `RateStrategy` for a fresh quote:

```java
@Override
public void execute() {
    previousWeightKg = shipment.weightKg();
    previousQuotedRate = shipment.quotedRate();
    shipment.setWeightKg(newWeightKg);
    shipment.setQuotedRate(shipment.requote());
}
```

`undo()` doesn't call the strategy again. It restores the captured `previousWeightKg` and `previousQuotedRate`, which is cheaper and safer than recomputing: recomputing assumes the strategy is a pure function of the shipment's fields. That holds for every strategy here, but a snapshot doesn't depend on it holding for the next one someone adds. With a counting strategy, `execute()` calls `quote()` once and `undo()` calls it zero times.

## Seam four: Command swaps Strategy

`SwitchRateStrategyCommand` is the one place that replaces the strategy, for example when an expedite contract is negotiated after the shipment was opened:

```java
@Override
public void execute() {
    previousStrategy = shipment.rateStrategy();
    previousQuotedRate = shipment.quotedRate();
    shipment.setRateStrategy(newStrategy);
    shipment.setQuotedRate(shipment.requote());
}

@Override
public void undo() {
    shipment.setRateStrategy(previousStrategy);
    shipment.setQuotedRate(previousQuotedRate);
}
```

This seam makes Strategy's swappability a reversible operation rather than a claim about an interface. `AdjustWeightCommand`, `ShipmentBuilder` and `ShipmentDispatchCenter` don't change to admit a new strategy. `execute()` requotes against it, and `undo()` restores the previous strategy reference and quote, not just the numbers. Swapping a `WeightTieredRateStrategy` for a `FlatRateStrategy` gives exactly the quote the flat strategy computes alone, and the undo gives exactly the weight-tiered quote.

## Seam five: State fires Observer

A legal transition in `Shipment.advance()` calls `notifyListeners()`, which walks a snapshot of the subscribed listeners:

```java
private void notifyListeners(ShipmentStatus from, ShipmentStatus to) {
    List<ShipmentStatusListener> snapshot = new ArrayList<>(listeners.values());
    for (ShipmentStatusListener listener : snapshot) {
        listener.onStatusChanged(id, from, to);
    }
}
```

The demo's console listener prints a line. A deployment might wire the same functional interface to a customer notification service, a tracking dashboard and a billing ledger, none of which references the others or the `AdvanceShipmentCommand` several calls up the stack. A listener only knows a shipment changed status.

The snapshot matters in a composed system. A listener may unsubscribe itself or another listener from inside its callback, and iterating the live `LinkedHashMap` values while that happens throws `ConcurrentModificationException` at exactly the point where two patterns interact.

## The composition root

`ShipmentDispatchCenter` is the only type that knows all five patterns. Everything else sees only the interface next to it: `ShipmentBuilder` doesn't know the center, `AdjustWeightCommand` doesn't know the builder, and a listener doesn't know `ShipmentCommand` exists. The center's `subscribe()` delegates to the shipment's registration; `advance()`, `adjustWeight()` and `switchRateStrategy()` build the matching command and run it through `apply()`; `undoLast()` and `redoLast()` delegate to `CommandHistory`, the two-`Deque` undo/redo stack from Command.

```java
public void advance(ShipmentEvent event) {
    apply(new AdvanceShipmentCommand(shipment, event));
}

public void adjustWeight(double newWeightKg) {
    apply(new AdjustWeightCommand(shipment, newWeightKg));
}

public void switchRateStrategy(RateStrategy newStrategy) {
    apply(new SwitchRateStrategyCommand(shipment, newStrategy));
}
```

Each operation is one line naming its command and handing it to the same `apply()`, so everything an operator console exposes is undoable by construction.

> **🤖 Explore with your agent**
>
> "In `examples/33-final-project`, add a `CachingRateStrategy` that wraps a `RateStrategy` and caches `quote(Shipment)` results by carrier code and weight, and have `ShipmentBuilder.resolveStrategy()` wrap each table entry in it. Leave a strategy passed through `rateStrategy()` unwrapped, keep `AdjustWeightCommand` and `SwitchRateStrategyCommand` restoring their captured quote and strategy on `undo()` instead of requoting, and don't change `ShipmentDispatchCenter` or `Shipment`. Before writing code, list which of the five seams the change crosses."
>
> Check: `mvn -q -f examples/33-final-project/pom.xml test -Dtest='ShipmentBuilderTest,AdjustWeightCommandTest,ShipmentDispatchCenterIntegrationTest'`, and the 8kg GROUND quote still $19.40 after a rescale to 15kg and its undo.

## When not to over-compose

Five composed patterns are a cost, justified only by the forces behind each one.

| If the system has | It doesn't need |
| --- | --- |
| One carrier and no status changes | A `ShipmentDispatchCenter`; a method from weight and carrier code to `Money` solves it |
| Exactly one carrier | The builder's lookup table; construct the one `RateStrategy` directly |
| No operation that needs reversing | `execute()`/`undo()` and a `CommandHistory`; a plain method call does the job |
| One reaction to a status change | Subscription machinery; a direct call where the status changes |

Add the carrier table when a second carrier with a different pricing rule is a current requirement, not a plausible future one. Composing patterns to show range is itself a smell. Each of the five here answers a present requirement of a dispatch center: carriers with materially different pricing, a lifecycle with illegal transitions, reversible operator actions, and several subscribers to a status change. A system with two of those needs should compose two patterns, and one with none should stay a handful of functions. The seams mark where coupling between patterns has to exist; they aren't a checklist.

## Build, run, observe

From the repository root:

{% include excalidraw.html name="33-final-project-sequence" caption="Figure 33.2: The time-ordered run of one shipment, with the same RateStrategy instance quoting the opening rate at step 1 and the corrected rate at step 4, restored exactly by the undo at step 5." %}

```sh
mvn -f examples/pom.xml test
```

Or standalone:

```sh
cd examples/33-final-project
mvn test
```

All 39 cases pass, across nine test classes. Eight check one pattern's own contract each, including a parameterized contract run against every `RateStrategy`. The integration class drives the whole center through one lifecycle: a builder-resolved opening quote, two advances notifying in order, a weight correction requoted through the same strategy and undone exactly, a strategy swap and its undo, and an event after delivery that is rejected without reaching any listener.

To watch the composition run:

```sh
examples/33-final-project/demo.sh
```

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

Each block traces to a seam: the opening quote to Builder resolving Strategy, the `[observer]` lines to State firing Observer, the rescale and its undo to Command invoking Strategy, the expedited quote to Command swapping Strategy, and the rejection to State's transition table.

- **Recompute on undo.** In a scratch copy, make `AdjustWeightCommand.undo()` call `shipment.requote()` instead of restoring `previousQuotedRate`. The quote still lands on $19.40, but one case fails: undo now calls the strategy.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original entries for every pattern composed here, with the forces each answers on its own.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): the builder item behind `ShipmentBuilder`'s validated fluent construction, and the minimal-mutability reasoning behind exposing `Shipment`'s setters only to the command types in its package.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/), [AssertJ documentation](https://assertj.github.io/doc/), and [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html): the testing idiom behind `examples/33-final-project`, which checks the composed system end to end.
- The [bibliography page]({{ '/bibliography/' | relative_url }}) lists the full reading list.
