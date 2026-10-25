# 10 - State and finite state machines: a shipment lifecycle

A shipment moves through a small, fixed set of statuses: created, packed,
in transit, delivered, with cancellation and return-to-sender as exceptions
along the way. This example models that lifecycle three idiomatic Java
ways, so you can compare the mechanism each one uses to refuse an illegal
transition without changing the lifecycle rules themselves.

## The domain

- `ShipmentStatus`: the seven statuses a shipment can carry (`CREATED`,
  `PACKED`, `IN_TRANSIT`, `DELIVERED`, `CANCELLED`, `RETURNED`, `LOST`).
  `LOST` is reachable only through the stochastic machine below.
- `ShipmentEvent`: the events a caller can issue (`PACK`, `SHIP`, `DELIVER`,
  `CANCEL`, `RETURN_TO_SENDER`), plus `ADVANCE`, used only to report a
  rejection from the stochastic machine's `advance()`.
- `IllegalTransitionException`: the one exception type every machine below
  throws the same way when an event does not apply to the current status.

## The three expressions

1. **Enum + transition table** (`EnumShipmentMachine.java`): every legal
   `(status, event)` pair is a single entry in a nested `EnumMap`, a literal
   value a reviewer reads as data with no class to open. `apply()` looks up
   the pair and throws `IllegalTransitionException` on a miss.
2. **Sealed interface + exhaustive pattern-matching switch**
   (`state/ShipmentState.java` and its seven records, dispatched by
   `state/SealedShipmentMachine.java`): each status is its own record, and
   `InTransit`, `Cancelled`, and `Returned` carry data the enum form has
   nowhere to put (a carrier tracking number, a cancellation reason). Every
   event method switches over all seven permitted types with no `default`,
   so the compiler refuses to build the file if an eighth state type is
   added to `ShipmentState`'s `permits` clause without a matching case.
3. **Stochastic FSM** (`StochasticShipmentMachine.java`): once a shipment is
   `IN_TRANSIT`, nothing calls `deliver()` on it. `advance()` draws a
   weighted outcome (85% delivered, 10% delayed, 5% lost) from an injected
   `java.util.random.RandomGenerator`, so production code passes a real
   generator while a test passes a seeded one and gets an exact,
   reproducible sequence back.

`MachineEquivalenceTest` drives the same event sequence through the enum
machine and the sealed machine side by side and asserts they land on the
same status after every step. `ShipmentDemo.java` exercises all three
expressions and prints each one's outcome.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

This example is standalone-buildable from this directory:

```sh
cd examples/10-state-machines
mvn test
```

It is also picked up by the repo root's aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 22 JUnit 5 cases (AssertJ assertions), including
a cross-check that the enum and sealed machines agree, illegal-transition
refusals for both deterministic machines, a seeded-reproducibility check on
the stochastic machine, and a distribution check across 20,000 draws.

To see all three expressions walk the same lifecycle rather than just
reading an assertion that says they agree, run the demo:

```sh
./demo.sh
```

Expected output:

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

The enum machine and the sealed machine both reach `DELIVERED` for the
identical pack/ship/deliver sequence, the observable version of
`MachineEquivalenceTest`. The stochastic block shows this particular seed
taking two delays before resolving; a different seed can resolve
immediately, take more delays, or land on `LOST`.

## Files

```
examples/10-state-machines/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run ShipmentDemo
  src/main/java/com/patterncatalyst/designpatterns/ch10/shipment/
    ShipmentStatus.java          # the seven statuses
    ShipmentEvent.java            # the caller-issued events, plus ADVANCE
    IllegalTransitionException.java # shared rejection type
    EnumShipmentMachine.java         # expression 1: enum + transition table
    WeightedOutcome.java               # outcome/weight pair for expression 3
    StochasticShipmentMachine.java       # expression 3: probabilistic FSM
    ShipmentDemo.java                      # exercises all three
    state/
      ShipmentState.java                    # sealed interface
      Created.java                           # expression 2: per-status records
      Packed.java
      InTransit.java                          # carries carrierTrackingNumber
      Delivered.java
      Cancelled.java                           # carries reason
      Returned.java                             # carries reason
      Lost.java
      SealedShipmentMachine.java                 # exhaustive pattern-matching switch
  src/test/java/com/patterncatalyst/designpatterns/ch10/shipment/
    EnumShipmentMachineTest.java      # legal/illegal transitions
    MachineEquivalenceTest.java        # enum vs. sealed cross-check
    StochasticShipmentMachineTest.java   # seeded reproducibility + distribution
    state/
      SealedShipmentMachineTest.java      # legal/illegal transitions, carried data
```
