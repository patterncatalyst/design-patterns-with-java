---
title: "Seams and the test safety net"
order: 29
layout: tutorial
sectionid: docs
part: "changing-inherited-code"
description: "Getting a hard-wired shipment escalation service and a risk scorer under test before changing them, taught against inherited Java that reads the wall clock directly and reaches into a global calendar singleton. Characterization tests pin the current behavior, an object seam and an extract-interface seam make the escalation service's collaborators substitutable with Mockito, and a subclass-and-override seam opens the risk scorer with no constructor change at all."
duration: "35 min"
---

## Learning objectives

By the end of this chapter you can:

- Write a characterization test that pins what a piece of inherited code
  actually does right now, including a margin or a gap that only exists
  because of a hard dependency, as distinct from a specification test that
  asserts what the code should do.
- Recognize a hard dependency, a class that constructs or reaches into a
  concrete collaborator it cannot be told to substitute, and name the
  specific symptom it produces: a test that cannot verify an interaction, or
  cannot reach an exact boundary.
- Open an object seam by taking a collaborator through the constructor
  instead of constructing it inline, using `java.time.Clock` as the
  injected dependency.
- Open an extract-interface seam by giving a hard-wired concrete
  collaborator an interface a test can substitute, and use Mockito to supply
  a mock through it.
- Open a subclass-and-override seam for a dependency with no constructor to
  change, by moving the one hard call into a protected method a test
  subclass overrides.
- Apply the safety-net discipline in order: characterize first, open a seam
  second, change behavior third, with the test suite staying green the
  entire time.

## Why this matters

Code you inherit was written to satisfy whatever requirement was in front
of its author at the time, under whatever deadline was in front of them,
and it got checked in once it worked. Nobody asked whether a future
maintainer would need to substitute one of its collaborators for a test
double, because the change that will need the substitution had not been
imagined yet. By the time that change arrives, the code has usually been
running for months or years, with real shipments depending on it continuing
to do exactly what it already does. Changing it with no test watching is a
bet that you understood every consequence of the change before you made it,
a bet a test exists specifically to let you stop making.

Michael Feathers calls code with no tests covering it legacy code,
independent of how old it is or how well it was written, because the
absence of a test is what makes a change risky, not the code's age. A
useful test needs two things inherited code often refuses to give it: a way
to observe the result, and a way to control the inputs. A seam is Feathers'
name for a place where you can get one of those two things without editing
the logic the test is trying to pin: a place where behavior can change
without the surrounding code changing with it.

The shipment escalation service worked through below has exactly the shape
that makes this hard. It decides whether a shipment has missed its promised
delivery window, and the inherited version answers that question by asking
the operating system what time it is, directly inside the method that makes
the decision, and then by building its own notification client right there
too. Neither choice is unusual; it is an ordinary way to write this kind of
check. It is also the exact pair of decisions that make the check's
boundary behavior impossible to pin down and its side effect impossible to
verify, until something about the code's structure changes. The job ahead
is not to rewrite the check. It is to make it observable and controllable
first, prove nothing moved while doing that, and only then touch the one
behavior that was wrong.

{% include excalidraw.html name="29-seams-and-substitution" caption="Figure 29.1: the same inherited dependencies made substitutable two ways, an object seam for an injected Clock and an extract-interface seam for a CarrierGateway, with a third seam, subclass-and-override, noted for a dependency with no constructor to change." %}

## Characterization tests: pinning what the code actually does

A characterization test is not an assertion about what a piece of code
should do. It is a recorded observation of what it does do, written by
running the code and copying down the answer, so a later change can be
checked against that recorded answer instead of against memory. This
distinction matters because inherited code frequently contains behavior
nobody would sign off on if they were designing it fresh, a boundary that
leans the wrong way, a silent fallback that hides a failure. A
characterization test pins that behavior exactly as found, questionable
parts included, because the goal at this stage is not correctness. The goal
is a safety net: a way to know, after any later edit, whether that edit
changed behavior that was not supposed to change.

`LegacyShipmentEscalationService` in this chapter's example is exactly this
kind of inherited code. Its `escalate` method takes a `Shipment`, a
tracking ID paired with a promised delivery instant, and decides whether
that shipment has missed its delivery window by more than a two-hour grace
period.

```java
public EscalationResult escalate(Shipment shipment) {
    Instant now = Instant.now();
    Instant overdueAt = shipment.promisedDelivery().plus(OVERDUE_GRACE);
    if (now.isAfter(overdueAt)) {
        CarrierGateway gateway = new SmtpCarrierGateway();
        String reason = "promised delivery window missed by more than "
                + OVERDUE_GRACE.toHours() + "h";
        gateway.notifyCarrier(shipment, reason);
        return EscalationResult.escalated(shipment, reason);
    }
    return EscalationResult.notOverdue(shipment);
}
```

Writing a characterization test for this method means constructing a
`Shipment` with a promised delivery instant, calling `escalate`, and
recording what comes back. For a shipment promised a day ago, the answer is
an escalated result. For a shipment promised a day from now, the answer is
a non-escalated one. Both are straightforward to pin, with a wide margin: a
full day, not a few seconds.

```java
@Test
void aShipmentPromisedADayAgoIsEscalated() {
    Shipment overdue = new Shipment("SHM-1001", Instant.now().minus(Duration.ofDays(1)), 10.0);

    EscalationResult result = legacy.escalate(overdue);

    assertThat(result.escalated()).isTrue();
    assertThat(result.reason()).contains("promised delivery window missed");
}
```

That margin is not stylistic caution. `escalate` calls `Instant.now()` a
second time, inside its own body, a few microseconds after this test
computed its own `now` to build the promised delivery instant. A margin
small enough for those two readings to land close together would make the
test exercise scheduling jitter instead of the service's logic. The case
this test file cannot write at all is a shipment whose grace period expires
at exactly this instant. There is no way to make two separate calls to
`Instant.now()`, one inside the test and one inside `escalate`, land on the
identical nanosecond. That is the concrete cost of the hard dependency: the
service's exact behavior at its own most important boundary, the instant
the grace period lapses, cannot be observed at all, let alone pinned.

A second, independent symptom shows up in the same class. `escalate`
constructs its own `SmtpCarrierGateway` inline, so no test can hold a
reference to the gateway that actually ran and ask it what it did.

```java
@Test
void anEscalationCarriesAReasonButTheNotificationItselfIsUnobservable() {
    Shipment overdue = new Shipment("SHM-1003", Instant.now().minus(Duration.ofDays(1)), 10.0);

    EscalationResult result = legacy.escalate(overdue);

    assertThat(result.reason()).isNotBlank();
}
```

The reason string is the only proxy this test has for "a notification
happened." It cannot check how many times `notifyCarrier` was called, what
arguments it received, or whether it was skipped on a path that should have
called it. A return value and an observed interaction are two different
facts, and a class that constructs its own collaborator forecloses checking
the second one entirely.

## The object seam: java.time.Clock

Feathers' term for a change point like this is a seam: a place in the code
where behavior can be altered without editing the logic it lives in. An
object seam is the most common kind: give the class a way to receive its
dependency from outside, through a constructor parameter, instead of
reaching out and constructing it itself. `ShipmentEscalationService` is
`LegacyShipmentEscalationService` refactored through exactly this seam, and
the dependency it takes is the JDK's own abstraction for the current
instant, `java.time.Clock`.

```java
public final class ShipmentEscalationService {

    private final Clock clock;
    private final CarrierGateway gateway;

    public ShipmentEscalationService(Clock clock, CarrierGateway gateway) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    public EscalationResult escalate(Shipment shipment) {
        Instant now = clock.instant();
        Instant overdueAt = shipment.promisedDelivery().plus(OVERDUE_GRACE);
        if (!now.isBefore(overdueAt)) {
            String reason = "promised delivery window missed by more than "
                    + OVERDUE_GRACE.toHours() + "h";
            gateway.notifyCarrier(shipment, reason);
            return EscalationResult.escalated(shipment, reason);
        }
        return EscalationResult.notOverdue(shipment);
    }
}
```

`Clock` is a seam the JDK ships ready-made: `Clock.systemUTC()` reads the real wall clock, the only
instance production code ever wires in, and `Clock.fixed(instant, zone)`
returns a fixed instant on every call, which is exactly what a test needs.
No custom interface, no hand-rolled abstraction, just a constructor
parameter typed to a class that already has both a production
implementation and a test-friendly one built in.

Introducing the seam alone should change nothing observable. A
`ShipmentEscalationService` built with `Clock.systemUTC()` has to answer
every wide-margin question the legacy service already answered, the same
way, because it is reading the same clock through one extra method call.
That claim is provable directly, by running the identical wide-margin
scenarios against both services and checking the answers agree.

```java
@Test
void wideMarginCasesAgreeWithTheLegacyService() {
    ShipmentEscalationService seamed = new ShipmentEscalationService(Clock.systemUTC(), gateway);
    Shipment overdue = new Shipment("SHM-2001", Instant.now().minus(Duration.ofDays(1)), 10.0);
    Shipment onTime = new Shipment("SHM-2002", Instant.now().plus(Duration.ofDays(1)), 10.0);

    assertThat(seamed.escalate(overdue).escalated())
            .isEqualTo(legacy.escalate(overdue).escalated());
    assertThat(seamed.escalate(onTime).escalated())
            .isEqualTo(legacy.escalate(onTime).escalated());
}
```

That test passing is the entire argument that introducing the seam was
behavior-preserving on its own. Nothing interesting has happened to the
service's answer yet. What has changed is what a test can now do to it.

## The extract-interface seam: a gateway a mock can stand in for

`CarrierGateway` is the second seam `ShipmentEscalationService` opens, and
it answers the problem the earlier unobservable-notification test ran into.
Instead of constructing a concrete `SmtpCarrierGateway` inline, the
constructor takes a `CarrierGateway` interface, extracted from the concrete
class that used to be the only option.

```java
public interface CarrierGateway {
    void notifyCarrier(Shipment shipment, String reason);
}
```

A Mockito mock substituted through this interface turns an unobservable
interaction into a direct assertion. Combined with
`Clock.fixed`, the exact boundary this chapter's characterization tests
could not reach becomes three ordinary, instant, flake-proof tests: a
deadline exactly at the grace-period boundary, one nanosecond before it,
and one nanosecond after it.

```java
@Test
void aDeadlineExactlyAtTheGracePeriodBoundaryIsEscalated() {
    Instant overdueAt = Instant.parse("2026-01-10T12:00:00Z");
    Instant promised = overdueAt.minus(OVERDUE_GRACE);
    ShipmentEscalationService service =
            new ShipmentEscalationService(Clock.fixed(overdueAt, ZoneOffset.UTC), gateway);
    Shipment shipment = new Shipment("SHM-2101", promised, 10.0);

    EscalationResult result = service.escalate(shipment);

    assertThat(result.escalated()).isTrue();
    verify(gateway).notifyCarrier(eq(shipment), any());
}
```

No sleeping, no margin, no dependence on how fast the test machine happens
to run that day. `Clock.fixed` tells the service exactly what instant
`clock.instant()` returns every time it is called, and `verify` checks that
`notifyCarrier` was actually invoked, not just that the returned
`EscalationResult` looked right. The one-nanosecond-before case flips the
assertion both ways at once: `result.escalated()` is false, and
`verify(gateway, never()).notifyCarrier(any(), any())` confirms the
notification never fired either. A test that only checked the return value
would pass even if a bug called `notifyCarrier` before the overdue check
ran; the interaction assertion catches that bug directly.

This is also where a deliberate behavior change becomes safe to make. The
legacy comparison, `now.isAfter(overdueAt)`, treats a deadline exactly equal
to the current instant as still on time, a boundary nobody could even test
before the seam existed. `ShipmentEscalationService` corrects it directly:
`!now.isBefore(overdueAt)`. The wide-margin equivalence test from the
previous section proves the far-from-the-boundary behavior never moved; the
exact-boundary tests pin the corrected rule precisely. The legacy service
is never edited to match. It stays in the codebase exactly as found, a
frozen reference a reviewer can compare against the one line that changed.

> **AI sidebar.** Ask an LLM pairing partner to "make this testable" and a
> common first move is reaching for Mockito's static-mocking support,
> `mockStatic`, to intercept a call like `Instant.now()` or
> `EnterpriseCalendar.isPeakSeason()` without touching the class's shape at
> all. That compiles, and it avoids editing the class under test, which
> looks like the smaller diff. Check what it actually bought you: the
> interception only exists inside the test, built on bytecode rewriting of
> a class the production code never asked to be substitutable. Nothing
> about the design changed, the hidden dependency is still hidden from
> every other caller, and the next person who wants a different `Clock` in
> production, a time zone override for one region, say, is back to square
> one. An object seam costs one constructor parameter, visible in the
> production code, not only in test scaffolding, and it pays that cost back
> the first time any other caller needs a different collaborator too.
> Prefer the seam that changes the design over the one that only changes
> what the test process can see.

## The subclass-and-override seam: a dependency with no constructor to change

An object seam and an extract-interface seam both need a constructor a test
can call with a different argument. Not every hard dependency offers one
cheaply. `LegacyShipmentRiskScorer`'s `score` method reaches directly into
`EnterpriseCalendar.isPeakSeason()`, a process-wide, legacy calendar
singleton several shipment-side services read from and no single team owns.

```java
public int score(Shipment shipment) {
    int score = baseRiskScore(shipment);
    if (EnterpriseCalendar.isPeakSeason()) {
        score += PEAK_SEASON_SURCHARGE;
    }
    return score;
}
```

Parameterizing this call, giving `score` a boolean argument for whether it
is peak season, is the correct long-term fix, and it is exactly the kind of
change Feathers calls parameterizing a dependency once a direct edit is
cheap enough to make. It stops being cheap the moment `isHighRiskWindow`'s
equivalent call shows up at a dozen other sites across a scoring pipeline
you do not want to touch more than this one change requires; rewriting
every call site today, in code you are not yet confident you understand, is
a bigger and riskier edit than the test you are trying to add.

A subclass-and-override seam sidesteps that cost. `ShipmentRiskScorer`
moves the one call into its own protected method, which production code
never overrides, and a test substitutes an answer by extending the class
and overriding that one method instead.

```java
public class ShipmentRiskScorer {

    public int score(Shipment shipment) {
        int score = baseRiskScore(shipment);
        if (isHighRiskWindow()) {
            score += PEAK_SEASON_SURCHARGE;
        }
        return score;
    }

    protected boolean isHighRiskWindow() {
        return EnterpriseCalendar.isPeakSeason();
    }
}
```

```java
private static final class TestableShipmentRiskScorer extends ShipmentRiskScorer {
    private final boolean peakSeason;

    TestableShipmentRiskScorer(boolean peakSeason) {
        this.peakSeason = peakSeason;
    }

    @Override
    protected boolean isHighRiskWindow() {
        return peakSeason;
    }
}
```

Both branches of `isHighRiskWindow` become reachable on demand, with
neither the constructor nor the public `score` contract touched at all.

```java
@Test
void appliesTheSurchargeDuringPeakSeason() {
    ShipmentRiskScorer scorer = new TestableShipmentRiskScorer(true);

    assertThat(scorer.score(LIGHT)).isEqualTo(20);
    assertThat(scorer.score(HEAVY)).isEqualTo(55);
}
```

Compare this to what the legacy class's own characterization test had to
settle for. With no seam, the only way to know what `score` should return
is to call `EnterpriseCalendar.isPeakSeason()` a second time, from inside
the test, and compute the expected value from whatever answer the
production code's own global dependency happens to give today.

```java
int expected = EnterpriseCalendar.isPeakSeason() ? 5 + 15 : 5;

assertThat(legacy.score(light)).isEqualTo(expected);
```

That test does pass, every day of the year, but it proves only that the
scorer agrees with the calendar, never that the surcharge logic is correct
in the branch the calendar is not currently returning. A subclass-and-override
seam is the smaller cost here than rewriting `EnterpriseCalendar` or every
one of its other callers: one class, one overridden method, and both
branches of a previously half-tested rule become ordinary, deterministic
assertions.

## The safety-net discipline, in order

The sequence this chapter worked through is the discipline itself, and the
order is not optional. Characterize first: observe and pin what the
inherited code does right now, wide margins and unreachable boundaries
included, before touching anything. Open a seam second, whichever kind fits
the shape of the dependency, an object seam when a constructor parameter is
cheap, an extract-interface seam when the hard-wired collaborator has a
side effect worth verifying, a subclass-and-override seam when neither
constructor change is available yet, and prove with the existing
characterization tests that the seam alone changed nothing. Change behavior
third, and only third: once the seam exists and the pre-existing tests are
green against it, make the deliberate change, add a test that pins the
corrected behavior at the exact point it lives, and watch every other test
stay green while it does.

Skipping the first step means changing code you have not actually
observed, trusting memory instead of a recorded answer. Skipping the second
means changing behavior in code that still has no seam, the same bet this
chapter opened with: confidence substituting for a mechanical check. Doing
all three in order converts "I am fairly sure this is still correct" into
"the suite that passed before this change still passes after it, plus one
new test for exactly what I meant to change."

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this
loop. From the repository root, the aggregator picks up
`examples/29-seams` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/29-seams
mvn test
```

Either path runs the same 16 JUnit 5 cases, covering the legacy
characterization tests for both the escalation service and the risk scorer,
the object and extract-interface seam tests with Mockito, and the
subclass-and-override seam tests. Observed locally on Fedora with Temurin
25:

```
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see the legacy and seamed escalation services agree on the same two
shipments, and the gateway log the legacy service never exposes, run the
example's own demo script:

```sh
examples/29-seams/demo.sh
```

```
== Legacy service: hard-wired clock and gateway, no seam ==
legacy SHM-7001: EscalationResult[..., escalated=true, reason=promised delivery window missed by more than 2h]
legacy SHM-7002: EscalationResult[..., escalated=false, reason=within the promised delivery window]

== Seamed service: Clock and CarrierGateway injected through the constructor ==
seamed SHM-7001: EscalationResult[..., escalated=true, reason=promised delivery window missed by more than 2h]
seamed SHM-7002: EscalationResult[..., escalated=false, reason=within the promised delivery window]
gateway log: [SHM-7001: promised delivery window missed by more than 2h]
```

The legacy and seamed services agree on both shipments, the same fact the
wide-margin equivalence test proves, now visible at the terminal. The
gateway log is new: nothing outside the legacy service can ever see it,
because `escalate` never hands out the collaborator it built.

## When not to use it

A seam is not free. An object seam adds a constructor parameter where a
concrete type and a direct construction used to be, and a reader now has to
check which `Clock` or which `CarrierGateway` a given service is actually
running against. A subclass-and-override seam adds a second class, visible
only in test code, that a future maintainer has to find before they
understand why the production class's protected method exists at all.
Neither cost is large, but neither is free, and paying it needs a real
force behind it.

Skip the seam entirely when the unit under suspicion is small enough, and
understood well enough, to rewrite outright, checked against a handful of
example-based tests written against the new code's intended contract, not
characterization tests pinning the old code's accidents. A ten-line helper
with one call site and an obvious intended behavior does not need the
characterize-seam-change sequence; rewriting it correctly once and
confirming the one call site still produces sensible results costs less
than characterizing, seaming, and then changing it. The sequence pays off
on code whose current behavior other systems depend on in ways nobody can
fully enumerate from reading it, code with enough callers, enough history,
or enough subtlety that an outright rewrite is a bet nobody can fully
evaluate in advance.

A seam is also the wrong tool when the dependency is not actually hard. A
value read once at startup and never touched again has no real
substitution need, and wrapping it behind a `Clock`-style abstraction on
principle is speculative generality borrowing this chapter's vocabulary
instead of solving the problem it exists to solve. Reach for an object
seam, an extract-interface seam, or a subclass-and-override seam when a
test has no other way to observe or control a dependency, not as a default
shape applied to every inherited method on sight.

## For deeper coverage

- Michael Feathers, [*Working Effectively with Legacy Code*](https://www.oreilly.com/library/view/working-effectively-with/0131177052/) (Prentice Hall): the original definition of a seam, the full catalogue of dependency-breaking techniques this chapter draws its object, extract-interface, and subclass-and-override seams from, and the fuller discussion of characterization tests as a discipline distinct from specification testing.
- The official [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html): the mock, `verify`, and argument-matcher vocabulary this chapter's `CarrierGateway` tests use, including the static-mocking support the AI sidebar warns against reaching for by default.
- The official [`java.time.Clock` documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/time/Clock.html): the built-in object seam this chapter's escalation service is built around, including `Clock.fixed` and the other test-friendly factory methods.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
