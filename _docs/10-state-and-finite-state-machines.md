---
title: "State and finite state machines"
order: 10
layout: tutorial
sectionid: docs
part: "selecting-varying-behavior"
description: "A shipment's lifecycle modeled two idiomatic Java 25 ways, an enum-plus-transition-table machine and a sealed-interface state dispatched through an exhaustive pattern-matching switch, plus a probabilistic machine whose weighted transitions are made reproducible by injecting a seeded RandomGenerator."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Say when status-dependent behavior with illegal moves calls for a state machine.
- Build a finite state machine as an enum plus a transition table that a reviewer reads as data.
- Build the same lifecycle as a sealed interface of per-status records, dispatched through an exhaustive pattern-matching `switch`.
- Choose between the two by whether statuses carry their own data and how often the set changes.
- Build a machine whose transitions come from a weighted draw, and make it reproducible by injecting a seeded `RandomGenerator`.
- Recognize when a `boolean` is enough.

## Why this matters

A shipment moves through a small, fixed set of statuses from checkout to the customer's door: created, packed, handed to a carrier and delivered, with cancellation possible early and a return to sender possible once it has shipped. The question at each step is which events are legal now and what comes next. Java offers two idiomatic answers that differ in more than syntax, and a third for a transition whose outcome is uncertain.

{% include excalidraw.html name="10-shipment-state-transitions" caption="Figure 10.1: A shipment's legal states and transitions, plus the stochastic outlet the probabilistic machine draws from out of IN_TRANSIT." %}

## Pattern: State

### The problem

A shipment holds one `ShipmentStatus` of seven. Five events move it: `PACK`, `SHIP`, `DELIVER`, `CANCEL` and `RETURN_TO_SENDER`. The first six statuses are reachable by events; `LOST` is only an outcome the probabilistic machine can draw. `ADVANCE` isn't a caller event; it lets the probabilistic machine's `advance()` report a rejection the same way the others do.

```java
public enum ShipmentStatus {
    CREATED, PACKED, IN_TRANSIT, DELIVERED, CANCELLED, RETURNED, LOST,
}

public enum ShipmentEvent {
    PACK, SHIP, DELIVER, CANCEL, RETURN_TO_SENDER, ADVANCE,
}
```

A `String status` checked by `if` statements scattered through the code has no single place that says which moves are legal, so a delivery event on a cancelled shipment goes through.

### The solution

A **State** machine ties an object's behavior to its current status and refuses any event the status doesn't allow.

Every machine throws the same exception, naming the status and the rejected event:

```java
public final class IllegalTransitionException extends RuntimeException {

    public IllegalTransitionException(ShipmentStatus from, ShipmentEvent event) {
        super("cannot apply event '%s' to a shipment in status '%s'".formatted(event, from));
    }
}
```

The enum machine and the sealed machine each state the rules themselves, with no shared helper, so a check that they agree on every step of the same event sequence means something.

| Form | Rules live in | Per-status data | Missing case caught |
| --- | --- | --- | --- |
| Enum and transition table | One literal map | None | Never: a missing entry reads as "no legal move" |
| Sealed interface and `switch` | One `switch` arm per status per event | Each record carries its own | At compile time |
| Weighted draw over an injected generator | A table of outcomes and weights | None | Not applicable |

### How Java models it

`EnumShipmentMachine` keeps the status in one field and looks every move up in a nested table:

```java
public final class EnumShipmentMachine {

    private static final Map<ShipmentStatus, Map<ShipmentEvent, ShipmentStatus>> TRANSITIONS = buildTransitions();

    private ShipmentStatus status = ShipmentStatus.CREATED;

    public ShipmentStatus status() {
        return status;
    }

    public void apply(ShipmentEvent event) {
        Map<ShipmentEvent, ShipmentStatus> legalFromHere = TRANSITIONS.getOrDefault(status, Map.of());
        ShipmentStatus next = legalFromHere.get(event);
        if (next == null) {
            throw new IllegalTransitionException(status, event);
        }
        status = next;
    }

    private static Map<ShipmentStatus, Map<ShipmentEvent, ShipmentStatus>> buildTransitions() {
        Map<ShipmentStatus, Map<ShipmentEvent, ShipmentStatus>> table = new EnumMap<>(ShipmentStatus.class);
        table.put(ShipmentStatus.CREATED, Map.of(
                ShipmentEvent.PACK, ShipmentStatus.PACKED,
                ShipmentEvent.CANCEL, ShipmentStatus.CANCELLED));
        table.put(ShipmentStatus.PACKED, Map.of(
                ShipmentEvent.SHIP, ShipmentStatus.IN_TRANSIT,
                ShipmentEvent.CANCEL, ShipmentStatus.CANCELLED));
        table.put(ShipmentStatus.IN_TRANSIT, Map.of(
                ShipmentEvent.DELIVER, ShipmentStatus.DELIVERED,
                ShipmentEvent.RETURN_TO_SENDER, ShipmentStatus.RETURNED));
        return Map.copyOf(table);
    }
}
```

The table is filled in an `EnumMap` and frozen with `Map.copyOf`, which returns an unmodifiable map; the inner maps come from `Map.of`. `apply()` is a lookup and a null check. A status with no row falls back to an empty map, and `get()` on it returns `null` like any miss, so an unknown status needs no special case.

The case for a table is who can read it. "Can a cancelled shipment be delivered?" is answered by seeing that `CANCELLED` isn't a key, without tracing any method. A new move is one entry in `buildTransitions()`. The cost: a table has nowhere to put behavior beyond the next status, such as stamping a delivery time.

### A sealed interface and an exhaustive switch

`ShipmentState` is a sealed interface over seven records, one per status:

```java
public sealed interface ShipmentState
        permits Created, Packed, InTransit, Delivered, Cancelled, Returned, Lost {

    ShipmentStatus status();
}
```

A record can carry data for its status. `InTransit` carries the carrier's tracking number, and `Cancelled` and `Returned` carry a reason; `ShipmentStatus.IN_TRANSIT` is the same bare constant whichever carrier has the parcel.

```java
public record InTransit(String carrierTrackingNumber) implements ShipmentState {

    public InTransit {
        Objects.requireNonNull(carrierTrackingNumber, "carrierTrackingNumber");
    }

    @Override
    public ShipmentStatus status() {
        return ShipmentStatus.IN_TRANSIT;
    }
}
```

`SealedShipmentMachine` dispatches each event through a pattern-matching `switch` on the current state:

```java
public void ship(String carrierTrackingNumber) {
    state = switch (state) {
        case Created c -> throw reject(ShipmentEvent.SHIP);
        case Packed p -> new InTransit(carrierTrackingNumber);
        case InTransit t -> throw reject(ShipmentEvent.SHIP);
        case Delivered d -> throw reject(ShipmentEvent.SHIP);
        case Cancelled c -> throw reject(ShipmentEvent.SHIP);
        case Returned r -> throw reject(ShipmentEvent.SHIP);
        case Lost l -> throw reject(ShipmentEvent.SHIP);
    };
}
```

There's no `default`. Because `ShipmentState` is sealed, the compiler checks that every permitted type has a case. Add an eighth record to the `permits` clause and every `switch` in `SealedShipmentMachine` stops compiling with `the switch expression does not cover all possible input values`. An `if`/`else if` chain of `instanceof` checks, or a `switch` with a `default`, leaves a forgotten status to be found by the first shipment that reaches it.

`isTerminal()` is the same guarantee for a question rather than an event:

```java
public boolean isTerminal() {
    return switch (state) {
        case Delivered d -> true;
        case Cancelled c -> true;
        case Returned r -> true;
        case Lost l -> true;
        case Created c -> false;
        case Packed p -> false;
        case InTransit t -> false;
    };
}
```

### The same rule in both idioms

{% include codetabs.html langs="Enum + transition table|Sealed interface + switch" %}

```java
// Enum + transition table: the rule is a value, one line in buildTransitions().
table.put(ShipmentStatus.PACKED, Map.of(
        ShipmentEvent.SHIP, ShipmentStatus.IN_TRANSIT,
        ShipmentEvent.CANCEL, ShipmentStatus.CANCELLED));
// apply() looks it up generically for every status; this file has no method
// named ship() anywhere.
```
```java
// Sealed interface + switch: the rule is one arm of a dedicated ship()
// method, and the compiler enforces that every other status either
// rejects SHIP explicitly or that rejection is a deliberate case here.
public void ship(String carrierTrackingNumber) {
    state = switch (state) {
        case Packed p -> new InTransit(carrierTrackingNumber);
        // ...the other six cases reject SHIP, as shown above.
    };
}
```

The table lists every legal move side by side and has no method per event. The `switch` has a method per event and no single list of rules.

{% include excalidraw.html name="10-table-vs-switch-dispatch" caption="Figure 10.2: the identical (PACKED, SHIP) → IN_TRANSIT rule stated two ways. The table only lists the three statuses that have any legal move at all; the switch lists all seven, with no default, so a missing case is a compile error rather than a silent miss." %}

### When each idiom fits

| Use | When |
| --- | --- |
| Enum and table | Statuses differ only in which events they accept; the set is stable; non-Java readers, such as a compliance reviewer, audit the rules. |
| Sealed interface | Statuses carry their own data, like `InTransit`'s tracking number; a status has behavior beyond the next status; new statuses arrive and someone must decide how every operation treats each one. |

A table silently treats a forgotten status as having no legal moves, which looks the same as a status designed to be terminal. The sealed form refuses to build until every `switch` handles the new status.

### A probabilistic state machine

`StochasticShipmentMachine` accepts `pack()`, `ship()` and `cancel()` like the others, but once a shipment is `IN_TRANSIT` nothing calls `deliver()`. `advance()` draws a weighted outcome from an injected generator:

```java
public final class StochasticShipmentMachine {

    private static final List<WeightedOutcome> IN_TRANSIT_OUTCOMES = List.of(
            new WeightedOutcome(ShipmentStatus.DELIVERED, 0.85),
            new WeightedOutcome(ShipmentStatus.IN_TRANSIT, 0.10),
            new WeightedOutcome(ShipmentStatus.LOST, 0.05));

    private final RandomGenerator rng;
    private ShipmentStatus status;

    public StochasticShipmentMachine(RandomGenerator rng, ShipmentStatus initial) {
        this.rng = Objects.requireNonNull(rng, "rng");
        this.status = Objects.requireNonNull(initial, "initial");
    }

    public ShipmentStatus advance() {
        if (status != ShipmentStatus.IN_TRANSIT) {
            throw new IllegalTransitionException(status, ShipmentEvent.ADVANCE);
        }
        double draw = rng.nextDouble();
        double cumulative = 0.0;
        for (WeightedOutcome candidate : IN_TRANSIT_OUTCOMES) {
            cumulative += candidate.weight();
            if (draw < cumulative) {
                status = candidate.outcome();
                return status;
            }
        }
        status = IN_TRANSIT_OUTCOMES.getLast().outcome();
        return status;
    }
}
```

| Draw lands on | Weight | Models |
| --- | --- | --- |
| `DELIVERED` | 0.85 | Delivery, a terminal status |
| `IN_TRANSIT` | 0.10 | A carrier delay, no change |
| `LOST` | 0.05 | A lost parcel, which no event can produce |

`runToTerminal(maxSteps)` calls `advance()` until the status leaves `IN_TRANSIT` or the step limit is reached, and returns every status it saw, so a shipment can be delayed several times before it resolves.

The generator is a constructor parameter, a `java.util.random.RandomGenerator`, not a field the class creates. `java.util.Random` has implemented `RandomGenerator` since JDK 17, so production passes `new Random()` and a test passes a fixed seed such as `new Random(2026)`. `Random` is a deterministic algorithm: the same seed gives the same draws. The class needs no test-only branch, mock or reflection; it depends on an interface rather than owning its source of entropy, the same inversion Strategy uses.

Seeded runs are reproducible across machine instances, and 20,000 single draws from one seeded generator land within a small tolerance of 85/10/5.

## When not to use it

| Situation | Write instead |
| --- | --- |
| Two values and one transition, such as whether an email was sent | A `boolean`, or a two-value enum with an `if`. |
| The only caller already checked the status moments earlier in the same method | The plain call; guarding every event again buys nothing. |
| A deterministic but awkward problem, such as carrier status updates that arrive late | A retry, a timeout or a fix to the integration. Modeling it as a probability hides a fixable bug behind a weight that looks intentional. |

Use the weighted draw only when the uncertainty is real and worth quantifying: a carrier's delivery outcome, a retry's result, a sampled rollout cohort.

> **🤖 Explore with your agent**
>
> "In `examples/10-state-machines`, model the lifecycle with `ShipmentStatus` and `ShipmentEvent`. In `EnumShipmentMachine`, keep every legal move in the `buildTransitions()` table and throw `IllegalTransitionException` on any miss. In `SealedShipmentMachine`, keep `ShipmentState` sealed over one record per status, put status data such as `InTransit.carrierTrackingNumber` on the record, and write every `switch` without a `default`. Both machines must state their own rules and agree on every step of the same event sequence. In `StochasticShipmentMachine`, take the `RandomGenerator` as a constructor parameter, never a private `new Random()`, and draw `IN_TRANSIT` outcomes from the 0.85/0.10/0.05 table."
>
> Check: `mvn -q -f examples/10-state-machines/pom.xml test -Dtest=MachineEquivalenceTest,StochasticShipmentMachineTest`, and look for any `default` branch or a `new Random()` inside the machine; either one misses the prompt even if the tests pass.

## Build, run, observe

From the repository root, or standalone from `examples/10-state-machines`:

```sh
mvn -f examples/pom.xml test
```

```sh
cd examples/10-state-machines
mvn test
```

All 22 tests pass. To watch the three machines:

```sh
examples/10-state-machines/demo.sh
```

```
== Expression one: enum + transition table ==
Start: CREATED
After pack/ship/deliver: DELIVERED

== Expression two: sealed interface + pattern-matching switch ==
Start: Created[]
After pack/ship/deliver: Delivered[]
isTerminal(): true

== Refused illegal transition ==
Rejected: cannot apply event 'SHIP' to a shipment in status 'CREATED'

== Expression three: stochastic FSM (seeded for reproducibility) ==
Seed 6233379223152327722, IN_TRANSIT outcomes: [IN_TRANSIT, IN_TRANSIT, DELIVERED]
Final status: DELIVERED
```

Both deterministic machines reach `DELIVERED` on pack, ship and deliver. This seed draws two delays before delivery; another seed may resolve at once, delay longer or draw `LOST`. Two experiments, each in a scratch copy:

- **Add a status.** Add a record `Held` to the `permits` clause of `ShipmentState`. Compilation fails at all six `switch` expressions in `SealedShipmentMachine`.
- **Shift the weights.** Change `DELIVERED` to 0.80 and `LOST` to 0.10. The 20,000-draw distribution check fails; the other 21 pass.

## What you can do now

- Put legal moves in one transition table, like `buildTransitions()`, when the rules are data.
- Use a sealed interface and a `switch` without `default` when statuses carry data or the set grows.
- Throw one exception type, `IllegalTransitionException`, from every machine.
- Inject a `RandomGenerator` so a weighted machine is reproducible from a seed.
- Use a `boolean` for a two-value status.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original State pattern entry, with the intent statement both deterministic machines satisfy in different idioms.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 23 on preferring class hierarchies to tagged classes and Item 34 on enum constants with constant-specific behavior, both relevant to choosing between the two deterministic machines.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/10-state-machines`, which checks that two independently coded machines agree step by step and that a seeded run reproduces an exact sequence.
- The full reading list lives on the [bibliography page]({{ '/bibliography/' | relative_url }}).
