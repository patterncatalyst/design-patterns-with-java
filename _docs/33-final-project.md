---
title: "Final project"
order: 33
layout: tutorial
sectionid: docs
part: "putting-it-together"
description: "The book's final project: a shipment dispatch center that composes Builder, State, Command, Observer, and Strategy into one running system, taught against the seams where the patterns meet rather than re-teaching any one of them from scratch."
duration: "35 min"
---

## Learning objectives

By the end of this chapter you can:

- Compose Builder, State, Command, Observer, and Strategy into one running
  system instead of five unrelated demonstrations, and name the object that
  owns each pattern's role.
- Identify the seams where two patterns meet: where a Builder resolves a
  Strategy, where a Command drives a State machine, where a Command invokes
  a Strategy, where a Command swaps a Strategy outright, and where a State
  transition fires an Observer event.
- Design a single composition root that wires every pattern together
  without any collaborator needing to know about the others.
- Write an integration test that proves the composed system works end to
  end: a full lifecycle run with a resolved rate, fired notifications, a
  reversed correction, and a swapped pricing rule, rather than five isolated
  unit tests that never touch each other.
- Recognize when composing this many patterns into one system is
  over-engineering for a smaller problem, and say what a smaller system
  should do instead.

{% include excalidraw.html name="33-final-project-composition" caption="Figure 33.1: ShipmentDispatchCenter as the composition root, with ShipmentBuilder resolving a RateStrategy, three Commands acting on the shared Shipment, and Shipment notifying every subscribed ShipmentStatusListener." %}

## Why this matters

Every pattern in this book up to this point has been taught the same way:
one force, one worked example, one focused test suite. That is the right way
to learn what Strategy buys you, or what Command's undo costs, in isolation.
It is also not how any of these patterns show up in a real system. A real
shipment dispatch center does not have a construction step and a status
machine and a notification pipeline running side by side with nothing
connecting them. It has one `Shipment` whose lifecycle fires events that
something else reacts to, one rate calculation that something else invokes
on demand, and one carrier-selection step that hands the right pieces to
everything downstream of it. The patterns meet at seams, and a team that has
only ever seen each pattern taught alone is unprepared for the moment two of
them have to cooperate inside the same method.

This final project builds that system. It composes a `ShipmentBuilder`
(Builder) that resolves a carrier's rate rule and produces an opening quote,
a `Shipment` that is simultaneously a State machine's context and an
Observer's subject, and three `ShipmentCommand` implementations (Command)
that each wrap one reversible operation a dispatch operator needs: advancing
the lifecycle, correcting a weight, and swapping the pricing rule itself.
The worked example in this chapter does not re-explain what Strategy is or
why State rejects an illegal transition; it assumes a reader already knows
that, by name, and spends its words instead on the five places these
patterns touch. Those five seams, not the five patterns individually, are
what this chapter is about.

## The domain: a shipment dispatch center

The system lives in `examples/33-final-project`. A `Shipment` carries an id,
a carrier code, a destination zone, a weight in kilograms, and a quoted rate
in cents, expressed as a `Money` record so a test assertion compares two
quotes for exact equality with no rounding error to argue about. It also
carries a status, one of `CREATED`, `PICKED_UP`, `IN_TRANSIT`, `DELIVERED`,
or `CANCELLED`. Four events, `PICK_UP`, `DISPATCH`, `DELIVER`, and `CANCEL`,
move it forward, with `CANCEL` legal from either of the two earliest
statuses and nowhere else. `Shipment.advance()` enforces that order directly
against a transition table keyed by the current status:

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

That is the whole State component: a context that owns its current status,
rejects an event that does not apply, and only notifies once a transition
succeeds. `Shipment` is also the Observer component's subject. It holds a
map of subscribed `ShipmentStatusListener` callbacks and fans a transition
out to every one of them the moment `advance()` accepts it. Putting both
roles on one type is not an accident of a small example. It is the first
seam this chapter cares about: the State machine's success condition and the
Observer's trigger condition are the same event, with nothing in between
them to get out of sync.

## Seam one: Builder resolves Strategy

`ShipmentBuilder` is a fluent Builder with one method per field a `Shipment`
needs: `id()`, `carrier()`, `destinationZone()`, `weightKg()`. What makes it
more than ceremony around a constructor is what `build()` does with the
carrier code before it ever calls `new Shipment(...)`:

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

`resolveStrategy()` looks a carrier code up against a small, known table,
`GROUND` maps to a weight-tiered rule, `AIR` maps to a flat rate, `FREIGHT`
maps to a bulkier weight-tiered rule with a higher base and a lower per
kilogram charge, and hands back whichever `RateStrategy` matches. Nothing
calling `ShipmentBuilder` ever names `WeightTieredRateStrategy` or
`FlatRateStrategy` directly, and neither does the `Shipment` constructor;
both receive a `RateStrategy` reference and call `quote()` on it without
caring which concrete type answered. `rateStrategy()` is the builder's
escape hatch: a caller negotiating a one-off contract with a carrier this
table does not know about yet can hand in a `RateStrategy` directly, and
`build()` skips the lookup entirely. This is the seam where Builder and
Strategy meet. Builder's job up to this point has been pure construction,
assembling the fields a `Shipment` needs; the moment it also decides which
algorithm prices that shipment, it has taken on a second responsibility. A
growing carrier list is a clear signal to split that lookup into its own
small type later. For three carriers, keeping the lookup inside the builder
that already needs the carrier code is the simpler choice.

## Seam two: Command drives State

`AdvanceShipmentCommand` wraps exactly one call to `Shipment.advance()`.
Its `execute()` records the status before the call so its `undo()` has
something to restore:

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

It is tempting to assume Command's undo for a State transition should call
some reverse event, an "un-pick-up," to walk the machine backward. Real
carriers do not support that operation, and neither does this one.
`restoreStatusForUndo()` is a separate, narrower entry point that bypasses
the transition table and sets the status back to whatever it was. That is
the same technique Command's own material uses for any action with no clean
arithmetic inverse: capture a snapshot before the change, restore the
snapshot instead of computing an inverse operation. The snapshot still
notifies every subscribed listener, because an operator correcting a
mis-scanned pickup is itself a real event a dashboard should see, not a
rewind no one hears about. `ShipmentDispatchCenter.advance()` is where this
seam becomes visible at the call site: an operator console only reaches
`shipment.advance()` through a command the center builds and runs through
its `CommandHistory`, which is what makes every lifecycle change undoable by
construction, rather than undoable only for the operations someone
remembered to wrap.

## Seam three: Command invokes Strategy

A warehouse rescale corrects a shipment's recorded weight after it was
already quoted. `AdjustWeightCommand` is where Command and Strategy meet
directly: its `execute()` does not just set a new number, it asks the same
`RateStrategy` the shipment was opened with for a fresh quote at the
corrected weight.

```java
@Override
public void execute() {
    previousWeightKg = shipment.weightKg();
    previousQuotedRate = shipment.quotedRate();
    shipment.setWeightKg(newWeightKg);
    shipment.setQuotedRate(shipment.requote());
}
```

`undo()` does not call the strategy a second time. It restores the exact
`previousWeightKg` and `previousQuotedRate` captured before the correction
ran, which is both cheaper and more correct than recomputing: recomputing
assumes the strategy is a pure function of the shipment's current fields,
and while that happens to be true for every `RateStrategy` in this example,
a snapshot does not depend on that assumption holding for every future one
someone adds. The test suite behind this chapter's example proves the
distinction directly, with a `RateStrategy` that counts how many times
`quote()` is called: `execute()` calls it once, and `undo()` calls it zero
times, confirming the restore is a snapshot and not a second computation
that happens to land on the same number.

## Seam four: Command swaps Strategy

The two commands above both use the `RateStrategy` a shipment already has.
`SwitchRateStrategyCommand` is the one place in this system that replaces it
outright, for example an expedite surcharge contract negotiated after a
shipment was already opened:

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

This is the seam that makes Strategy's swappability a tested, reversible
fact rather than a claim about the interface. Nothing about
`AdjustWeightCommand`, `ShipmentBuilder`, or `ShipmentDispatchCenter` changes
to admit a new concrete `RateStrategy`; a caller hands one to this command,
`execute()` requotes against it, and `undo()` returns the shipment to
exactly the strategy and quote it had a moment before, including the
strategy reference itself, not just the numbers it produced. A test in this
chapter's example builds a `Shipment` with a `WeightTieredRateStrategy`,
swaps in a `FlatRateStrategy`, and confirms both the new quote and the
restored quote are exactly what each strategy would compute on its own,
proving the same result contract holds no matter which concrete
implementation is behind the field at a given moment.

## Seam five: State fires Observer

The fifth seam already appeared in `Shipment.advance()` above: a legal
transition calls `notifyListeners()`, and that method walks a snapshot copy
of every subscribed listener and calls `onStatusChanged()` on each one.

```java
private void notifyListeners(ShipmentStatus from, ShipmentStatus to) {
    List<ShipmentStatusListener> snapshot = new ArrayList<>(listeners.values());
    for (ShipmentStatusListener listener : snapshot) {
        listener.onStatusChanged(id, from, to);
    }
}
```

The demo program's console listener prints a line to the terminal. A real
deployment might wire the same functional interface to a customer
notification service, a tracking dashboard, and a billing ledger, three
subscribers that never reference each other and never reference the
`AdvanceShipmentCommand` that triggered the event several calls up the
stack. The observer does not know, and should not need to know, that a
command was involved at all; it only knows a shipment changed status. Taking
a snapshot before iterating matters here for a reason specific to a composed
system: a listener is free to unsubscribe itself, or something else, from
inside its own callback, and iterating the live collection while a callback
mutates it would raise a `ConcurrentModificationException` at exactly the
moment two patterns are interacting.

## The composition root

`ShipmentDispatchCenter` is the one type in this final project that knows
about all five patterns at once. Every other component only ever sees the
interface directly above or below it: `ShipmentBuilder` does not know about
`ShipmentDispatchCenter`, `AdjustWeightCommand` does not know about
`ShipmentBuilder`, and a `ShipmentStatusListener` implementation does not
know `ShipmentCommand` exists. `ShipmentDispatchCenter` is where
`subscribe()` delegates to the shipment's Observer registration, where
`advance()`, `adjustWeight()`, and `switchRateStrategy()` each build the
matching command and run it through `apply()`, and where `undoLast()` and
`redoLast()` delegate to a `CommandHistory`, the same undo/redo stack built
from two `Deque`s that Command's own material introduced.

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

Reading those three methods side by side is the whole argument for a
composition root. Each one is a single line that names which command this
operation needs and hands it to the same `apply()` method, so every
operation an operator console exposes is undoable by construction, not
undoable only for the ones somebody remembered to route through history.

> **AI sidebar.** Ask an LLM pairing partner to "add retry logic around the
> carrier lookup" or "cache the rate strategy for performance" on a system
> like this one, and a common result touches `ShipmentBuilder`,
> `ShipmentDispatchCenter`, and `AdjustWeightCommand` all at once, because
> from the model's point of view they are just three nearby files in the
> same package. What a generated patch will not do on its own is notice
> that `AdjustWeightCommand` and `SwitchRateStrategyCommand` both hold the
> `RateStrategy` reference live on the shipment, so a cached or swapped
> strategy stays correct for a command that might still be sitting on the
> undo stack, or that `ShipmentBuilder` handing back a fresh `Shipment` by
> value, rather than a shared handle, is what keeps one shipment's pricing
> rule from leaking into another's test. Before accepting a generated change
> to a composed system like this one, trace which seam the change crosses. A
> change that looks local to one file can break an invariant two patterns
> away from it, and the five seams named above are the places to check
> first.

## When not to over-compose

Composing five patterns into one system is a cost, not a default, and the
same forces that justify any one of these patterns on its own are what
decide whether composing several of them here is worth paying for.

A shipment that never changes status, never needs an undo, and prices
against a single carrier does not need a `ShipmentDispatchCenter` at all. A
method that takes a weight and a carrier code and returns a `Money` value
solves that problem completely, and wrapping it in Builder, Strategy, and a
composition root to match is solving a problem the system does not have yet.
A system with exactly one carrier does not need `ShipmentBuilder`'s lookup
table at all; a single concrete `RateStrategy` constructed directly says
everything the lookup would have said, with one less seam to reason about.
Reach for the carrier table when a second carrier, and a second concrete
strategy behind it, is a real, current requirement, not a plausible future
one.

A system where no operation ever needs reversing does not need Command's
`execute()`/`undo()` ceremony. If a weight correction is rare, low-stakes,
and easy to re-enter by hand if someone gets it wrong, a plain method call
does the job a `ShipmentCommand` implementation would, without a
`CommandHistory` to maintain. And a status change with exactly one piece of
code that ever reacts to it does not need `Shipment`'s subscription
machinery; a direct call at the point the status changes is the whole
story.

Composing patterns to demonstrate range is itself a smell. The five patterns
in this chapter are each justified by a
specific, present requirement of a shipment dispatch center: more than one
carrier with a materially different pricing rule, a lifecycle with illegal
transitions worth rejecting, operator actions that need to be reversible,
and more than one subscriber that cares about a status change. A smaller
system that needs only two or three of those things should compose only
those two or three, and a system that needs none of them should stay a
handful of functions. These five seams matter because they mark where real
coupling has to exist between patterns; they are not a checklist to complete
on every project that happens to involve more than one pattern.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this loop.
From the repository root, the aggregator picks up `examples/33-final-project`
automatically:

{% include excalidraw.html name="33-final-project-sequence" caption="Figure 33.2: The time-ordered run of one shipment, with the same RateStrategy instance quoting the opening rate at step 1 and the corrected rate at step 4, restored exactly by the undo at step 5." %}

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/33-final-project
mvn test
```

Either path runs the same 39 JUnit 5 cases, asserted with AssertJ and
Mockito, including a parameterized suite that checks every `RateStrategy`
against the same contract and a cross-check that a swapped strategy and a
restored one each honor exactly what their own `quote()` method promises.
Observed locally on Fedora with Temurin 25.0.2:

```
Tests run: 39, Failures: 0, Errors: 0, Skipped: 0
```

Eight of those nine test files each check one pattern's own contract: the
builder's carrier lookup and overrides, the lifecycle's legal and illegal
transitions, the observer's fan-out and unsubscribe behavior, and each
command's execute and undo. The ninth,
`ShipmentDispatchCenterIntegrationTest`, is this example's actual point. It
never repeats a single-pattern check; it drives the whole dispatch center
through a lifecycle run, confirms a builder-resolved strategy priced the
opening quote, confirms two lifecycle advances fire Observer notifications
in order through a Mockito verification, confirms a weight-correction
command recomputes through that same strategy, confirms undoing that
command restores both the weight and the quote exactly, confirms a
strategy swap and its undo round-trip the same way, and confirms a rejected
event after delivery never reaches a single listener, all inside one running
system.

To see the composition play out rather than read about it, run the
example's own demo script:

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

Every line in that output traces back to one of the five seams above: the
opening quote comes from Builder resolving Strategy, the `[observer]` lines
come from State firing Observer, the corrected and restored quotes come from
Command invoking and then snapshotting around Strategy, the expedited and
restored quotes come from Command swapping Strategy outright, and the final
rejection comes from State's own transition table refusing an event that no
longer applies.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original entries for every pattern this chapter composes, including the forces each one answers in isolation before this chapter puts them together.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): the builder-pattern item behind `ShipmentBuilder`'s fluent, validated construction, and the minimal-mutability reasoning behind which fields on `Shipment` are exposed for mutation only to the command types in its own package.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/), [AssertJ documentation](https://assertj.github.io/doc/), and [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html): the testing idiom behind `examples/33-final-project`'s suite, which asserts that the composed system behaves correctly end to end rather than only checking that each pattern's own unit tests pass in isolation.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
