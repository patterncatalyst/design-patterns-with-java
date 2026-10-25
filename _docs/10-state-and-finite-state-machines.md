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

- Name the force that makes State the right candidate pattern: behavior
  tied to a current status, with illegal status changes that must be
  refused rather than silently accepted.
- Build a finite state machine as a plain enum plus a transition table, and
  explain why a reviewer can check a legal move by reading one entry rather
  than tracing a branch.
- Build the identical lifecycle as a sealed interface of per-status records,
  dispatched through an exhaustive pattern-matching `switch`, and say what
  the compiler guarantees that the enum form does not.
- Decide which of the two fits a given status set, based on whether
  different statuses need to carry different data and whether the set is
  closed today.
- Build a probability-driven state machine whose transitions are chosen by
  a weighted draw, and make that machine's behavior reproducible in a test
  by injecting a seeded random generator rather than owning one internally.
- Recognize when a status change is solving a problem a single `boolean`
  field already solves, and say what to write instead.

## Why this matters

A shipment moves through a small, fixed set of statuses on its way from
checkout to a customer's door: it is created, packed, handed to a carrier,
and delivered, with a cancellation possible early and a return to sender
possible once it has shipped. The question this chapter trains for is never
"which pricing rule applies," the way the Strategy chapter trained; it is
"which events are legal right now, and what happens next." That is the
State pattern's whole claim, the Gang of Four's answer to an object whose
behavior depends on which of a known, finite set of conditions it currently
holds, and it is also the shape every finite state machine shares whether
or not a class hierarchy sits underneath it.

What earns this force a full chapter is that Java 25 gives you two
different idiomatic ways to express the same lifecycle, and the difference
is not cosmetic. An enum plus a transition table puts every
legal move in one literal structure, a value a reviewer reads as data; a
sealed interface of per-status records puts the same rules inside the type
system, where each status can also carry data of its own and the compiler
refuses to compile a `switch` over the set that forgets a case. A third
expression breaks from both: a shipment that is in transit does not move to
its next status because a caller asked it to, it moves because a weighted
draw, backed by a real probability distribution, decided the outcome.

{% include excalidraw.html name="10-shipment-state-transitions" caption="Figure 10.1: A shipment's legal states and transitions, plus the stochastic outlet the probabilistic machine draws from out of IN_TRANSIT." %}

## The domain: a shipment's lifecycle

The example behind this chapter lives in `examples/10-state-machines`. A
shipment carries one `ShipmentStatus` at a time out of seven: `CREATED`,
`PACKED`, `IN_TRANSIT`, `DELIVERED`, `CANCELLED`, `RETURNED`, and `LOST`.
The first six are the legal lifecycle either deterministic expression below
can reach by calling an event; `LOST` exists only as an outcome the
probabilistic machine can draw, the same stochastic outlet the diagram
above calls out on `IN_TRANSIT`. Five events move a shipment between
statuses: `PACK`, `SHIP`, `DELIVER`, `CANCEL`, and `RETURN_TO_SENDER`.
`ShipmentEvent` also declares an `ADVANCE` constant that is not a
caller-issued event at all; it exists only so the probabilistic machine's
`advance()` can report a rejection through the same channel every other
event uses:

```java
public enum ShipmentStatus {
    CREATED, PACKED, IN_TRANSIT, DELIVERED, CANCELLED, RETURNED, LOST,
}

public enum ShipmentEvent {
    PACK, SHIP, DELIVER, CANCEL, RETURN_TO_SENDER, ADVANCE,
}
```

`IllegalTransitionException` is the one exception type every expression in
this chapter throws the same way, carrying both the status an event was
rejected from and the event itself:

```java
public final class IllegalTransitionException extends RuntimeException {

    public IllegalTransitionException(ShipmentStatus from, ShipmentEvent event) {
        super("cannot apply event '%s' to a shipment in status '%s'".formatted(event, from));
    }
}
```

That shared vocabulary is the only thing the two deterministic expressions
below have in common. The enum machine and the sealed-interface machine
each encode the legal moves independently, in their own idiom, which is
the thing under test: a cross-check asserting the two agree only means
something if each one states the rules itself rather than delegating to a
third, canonical function.

## Expression one: an enum and a transition table

`EnumShipmentMachine` keeps the current status in a single field and looks
up every transition in a nested `EnumMap`, a literal value where the rule
"a packed shipment can be shipped or cancelled" is one entry rather than a
line of code inside a class:

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

The outer map is keyed by `ShipmentStatus`, the inner by `ShipmentEvent`,
both enums, so an `EnumMap` is the idiomatic container on both levels: it
stores entries in constant-declaration order in a flat array under the
hood, instead of hashing a general key the way a `HashMap` keyed on a
combined status-and-event pair would. `apply()` is nothing but a lookup and
a null check; a status with no entry in the outer map, or an event missing
from the inner one, both fall through to `Map.of()`'s empty map, and
`get()` on an empty map returns `null` exactly like a miss on a populated
one, so the rejection path needs no special case for "a status the table
never heard of."

The real argument for this shape over a class hierarchy is who can read
it. A product manager asking "what happens when a cancelled shipment gets a
delivery event" answers the question by scanning the table's six entries
and finding `CANCELLED` absent as a key entirely, rather than tracing a
method call through whichever class happens to represent that status.
Adding a new status means adding one entry to `buildTransitions()` and
nothing else; the cost is that the table has no natural place to attach
behavior beyond "what status comes next." A method like `deliver()` could,
in principle, also stamp a delivery timestamp or fire a notification; a
table FSM has nowhere to put that without growing into something that is
not a table anymore. A table is the right shape when the only thing that
varies by status is which transitions are legal; a richer type per status
earns its ceremony when different statuses need meaningfully different
behavior, or different data, beyond that.

## Expression two: a sealed interface and an exhaustive pattern-matching switch

`ShipmentState` is a sealed interface over exactly seven record types, one
per status, declared in its `permits` clause:

```java
public sealed interface ShipmentState
        permits Created, Packed, InTransit, Delivered, Cancelled, Returned, Lost {

    ShipmentStatus status();
}
```

Unlike `ShipmentStatus`, a value here can carry data specific to its
status. `InTransit` carries the carrier's tracking number; `Cancelled` and
`Returned` carry a reason string. The enum form has nowhere to put that:
`ShipmentStatus.IN_TRANSIT` is a bare constant, the same value no matter
which carrier is handling the parcel, while an `InTransit` record is a
value a caller can read a tracking number back out of:

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

`SealedShipmentMachine` dispatches each event through a pattern-matching
`switch` over the current `ShipmentState`. `ship()` is representative:

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

Every case in that `switch` corresponds to one of the seven types in
`ShipmentState`'s `permits` clause, and there is no `default` branch. That
absence is the entire point: `ShipmentState` is a closed, sealed set, so
the compiler can verify this `switch` covers every possible subtype at
compile time, and if an eighth record is ever added to the `permits`
clause without a matching `case` here, the file stops compiling. A
`switch` over an open type, or an `if`/`else if` chain checked with
`instanceof`, gives you no such guarantee; a forgotten branch there is a
bug that waits for whichever shipment reaches it first in production, not
a build failure a reviewer sees before merging.

`isTerminal()` makes the same guarantee visible in miniature, with every
branch returning a plain `boolean` instead of throwing:

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

Reading this method answers "which statuses are terminal" completely, by
construction; there is no seventh case anyone could add to
`ShipmentState` that this `switch` would silently mishandle, because
leaving one out is not a runtime behavior to discover, it is a compiler
error to fix.

### Comparing the same transition in both idioms

The `(PACKED, SHIP) -> IN_TRANSIT` rule looks different depending on which
idiom states it, even though both enforce the identical move:

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

The table version states every legal move as data and has no code path
named after a single event; the switch version states every legal move as
a dedicated method and has no single place where all the rules sit side by
side as a list. Neither reads the other's rules at all: that separation,
not a shared helper, is what a cross-check test in this example's suite
verifies by driving an identical event sequence through both machines and
asserting they land on the same status after every step.

## When each idiom fits

Reach for the enum-and-table form when the only thing that distinguishes
one status from another is which events it accepts, when the status set is
stable enough that a flat table is unlikely to need per-status special
cases, and when the people who need to audit the rules are not necessarily
comfortable reading Java. A compliance reviewer checking "can a delivered
shipment ever be cancelled" can scan `buildTransitions()` and see
`DELIVERED` is not a key in the outer map at all, no Java knowledge
required beyond recognizing a missing entry.

Reach for the sealed-interface form when different statuses carry
different data in their own right, the way `InTransit`'s tracking number and
`Returned`'s reason do here, or when a status's behavior is more than "what
comes next," such as validation specific to one status that a flat
transition value has no natural home for. The real payoff is the
exhaustiveness check itself: a codebase where new statuses arrive
occasionally, each needing a human to decide how every existing operation
should treat it, benefits far more from a compiler that refuses to build
until every `switch` is updated than from a table that silently defaults a
forgotten status to "no legal transitions," which looks identical to a
status that was designed to be terminal.

Both forms share the same `IllegalTransitionException` and the same
`ShipmentStatus` vocabulary for exactly this reason: a caller, or a test,
should not need to know or care which expression is enforcing a given
rule, as long as something is.

## Expression three: a probabilistic state machine

The two expressions above both wait for a caller to supply an event and
either accept or reject it. `StochasticShipmentMachine` breaks that
contract for exactly one transition. Once a shipment is `IN_TRANSIT`,
nothing calls `deliver()` on it; instead, `advance()` draws a weighted
outcome from an injected random generator:

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

Eighty-five times in a hundred, the draw lands on `DELIVERED` and the
shipment reaches a terminal status. Ten times in a hundred, the draw lands
back on `IN_TRANSIT` itself, modeling a carrier delay with no status change
at all. Five times in a hundred, the draw lands on `LOST`, a status neither
deterministic expression above can ever produce, because nothing in their
event vocabulary represents a carrier losing a parcel. `runToTerminal()`
calls `advance()` in a loop until the status stops being `IN_TRANSIT`,
collecting every status observed along the way, which turns a single
weighted draw into a self-looping chain: a shipment can be delayed zero,
one, or several times before it finally resolves.

The detail that makes this machine testable at all is where the random
generator lives. The constructor takes a `java.util.random.RandomGenerator`
as a parameter rather than constructing one internally. `java.util.Random`
has implemented `RandomGenerator` since Java 17, so a production caller can
pass `new Random()`, seeded from system entropy, while this example's test
suite passes `new Random(2026)`, a fixed seed. Seeding the same generator
with the same value twice produces the exact same sequence of draws,
because `Random` is a deterministic pseudo-random algorithm; the
unpredictability a caller sees comes entirely from the seed, and a test
that controls the seed controls the output. Nothing about
`StochasticShipmentMachine` itself changed to make this possible, no
test-only branch, no mock, no reflection; injecting the generator through
the constructor is the whole design decision, and it is the same
dependency-inversion move this book's Strategy and dependency-inversion
chapters both trained: depend on an interface, not on a concrete source of
entropy a class owns for itself.

A seed chosen for illustration rather than for test coverage, applied to
this machine, draws two delays before resolving: `IN_TRANSIT`, `IN_TRANSIT`,
`DELIVERED`. Running the same seed twice reproduces that exact sequence
every time; a different seed might resolve on the first draw, take more
than two delays, or land on `LOST` instead. The test suite behind this
example checks both properties directly: a fixed seed reproduces an exact
sequence across two separate machine instances, and a large batch of
single draws, taken from one shared generator advancing across many
machines, lands within a small tolerance of 85/10/5 rather than merely
completing without error.

> **AI sidebar.** Ask an LLM pairing partner to "add a status field to this
> shipment class" and the overwhelmingly common answer is a `String status`
> or a loosely-typed flag checked with scattered `if` statements through the
> codebase, with no single place that says which transitions are legal.
> That answer works for exactly as long as nobody ships a status typo or an
> out-of-order update, which is not long. Check what the model hands back
> against the questions this chapter trained: does the status set need to
> carry different data per status, in which case the sealed-interface form
> fits; does an auditor need to read the rules as a flat list independent
> of the Java syntax around them, in which case the transition table fits
> better; and is any part of the transition uncertain rather than
> merely unmodeled, a carrier's delivery outcome instead of a database call
> the code has not written yet? A model asked to "make shipment
> tracking feel more realistic" will sometimes reach for a random delay
> length or a coin flip hidden inside a loop, which produces varying output
> but has no named distribution behind it and no way to make a test
> reproduce a specific case. If the uncertainty needs to be quantified,
> weighted, and reproducible in a test, an injected, seedable
> `RandomGenerator` feeding a small table of weighted outcomes is what to
> ask for instead, and a generated answer that constructs its own `Random`
> as a private field with no constructor parameter for it is the clearest
> sign the code runs but cannot be tested.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this
loop. From the repository root, the aggregator picks up
`examples/10-state-machines` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/10-state-machines
mvn test
```

Either path runs the same 22 JUnit 5 cases, asserted with AssertJ,
including a cross-check that the enum machine and the sealed machine agree
on every step of an identical event sequence, illegal-transition refusals
for both deterministic machines, a seeded-reproducibility check on the
stochastic machine, and a distribution check across 20,000 draws. Observed
locally on Fedora with Temurin 25.0.2:

```
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see all three expressions walk the same lifecycle rather than just
reading an assertion that says they agree, run the example's own demo
script:

```sh
examples/10-state-machines/demo.sh
```

It drives pack/ship/deliver through the enum machine and the sealed
machine, shows an illegal transition being refused, and then runs the
stochastic machine from a fixed seed:

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

Both deterministic expressions land on `DELIVERED` for the
identical pack/ship/deliver sequence, the observable version of this
example's cross-check test. The stochastic block shows this particular
seed taking two delays before resolving; a different seed can resolve
immediately, take more delays, or land on `LOST` instead, and the test
suite's distribution check is what confirms those outcomes land close to
85/10/5 over many draws rather than this single run being representative
on its own.

## When not to use any of these

All three expressions in this chapter earn their cost when a thing has
more than a couple of statuses, when the legal transitions between them
are a real constraint worth enforcing rather than a formality, and when an
illegal event is a case the code must guard against. They stop paying for
themselves well before that.

A status with two values and one transition between them does not need an
enum-and-table machine or a sealed interface; it needs a `boolean`, or at
most a two-value enum checked with a plain `if`. Wrapping a flag that only
ever flips from `false` to `true`, such as whether an email has been sent,
in any of this chapter's machinery adds a type, a file, and a test with
nothing underneath it to justify the ceremony. The same caution applies to
events a caller can never issue illegally: if the only code path that ever
calls `ship()` already checked that the shipment was packed moments
earlier, in the same method, enforcing that rule a second time inside a
state machine is not wrong, but it buys nothing either, and a reviewer who
sees `IllegalTransitionException` guarded on every event may reasonably ask
where the illegal calls are expected to come from.

The probabilistic expression deserves extra scrutiny beyond the other two,
because it is the easiest of the three to reach for out of habit rather
than need. A transition with real uncertainty behind it in the world, a
carrier's delivery outcome, a flaky network call's retry result, a feature
rollout's sampled cohort, is exactly what a weighted draw over an injected
generator is for. A transition that is deterministic but inconvenient to
model precisely, such as "shipment status updates sometimes arrive late
from the carrier's API," is not the same thing, and modeling a data
problem as a probability distribution can hide a fixable integration bug
behind a parameter that looks intentional. Reach for the weighted draw when
the uncertainty is real and worth quantifying; reach for a retry, a
timeout, or a bug report when it is not.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original State pattern entry, including the intent statement both deterministic expressions in this chapter satisfy in different idioms.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 23 on preferring class hierarchies to tagged classes and Item 34 on enum constants with constant-specific behavior, both directly relevant to choosing between this chapter's two deterministic expressions.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/10-state-machines`'s suite, which asserts that two independently-coded expressions agree on every step of a shared event sequence and that a seeded stochastic run reproduces an exact sequence rather than merely completing without error.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
