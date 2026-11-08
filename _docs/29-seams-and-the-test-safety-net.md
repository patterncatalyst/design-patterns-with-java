---
title: "Seams and the test safety net"
order: 29
layout: tutorial
sectionid: docs
part: "changing-inherited-code"
description: "Getting a hard-wired shipment escalation service and a risk scorer under test before changing them. Characterization tests pin the current behavior, an object seam and an extract-interface seam make the escalation service's clock and gateway substitutable, and a subclass-and-override seam opens the risk scorer with no constructor change."
duration: "35 min"
---

## Learning objectives

By the end of this chapter you can:

- Write a characterization test that pins what inherited code does now, gaps included, as distinct from a test of what it should do.
- Name the symptom a hard dependency produces: an interaction you can't verify, or a boundary you can't reach.
- Open an object seam by taking `java.time.Clock` through the constructor.
- Open an extract-interface seam so Mockito can stand in for a hard-wired collaborator.
- Open a subclass-and-override seam when there's no constructor worth changing.
- Work in order: characterize, open a seam, then change behavior, with every test green throughout.

## Why this matters

Inherited code was written for the requirement in front of its author, and nobody asked whether a later maintainer would need to substitute a collaborator. By the time that change arrives, real shipments depend on the code doing exactly what it does now. Changing it with no test watching is a bet that you've understood every consequence.

Michael Feathers calls code without tests legacy code, whatever its age, because the missing test is what makes a change risky. A useful test needs a way to observe the result and a way to control the inputs, and inherited code often offers neither. A **seam** is Feathers's name for a place where behavior can change without editing the code around it, which is where a test gets that control.

The shipment escalation service here decides whether a shipment has missed its promised delivery window. It asks the operating system for the time inside the decision method, and it builds its own notification client there too. Both are ordinary choices, and together they make the boundary impossible to pin and the side effect impossible to verify. The job isn't to rewrite the check: make it observable and controllable, prove nothing moved, and only then fix the one rule that was wrong.

{% include excalidraw.html name="29-seams-and-substitution" caption="Figure 29.1: the same inherited dependencies made substitutable two ways, an object seam for an injected Clock and an extract-interface seam for a CarrierGateway, with a third seam, subclass-and-override, noted for a dependency with no constructor to change." %}

## Characterization tests: pinning what the code actually does

A **characterization test** records what code does, written by running it and copying down the answer, so a later change is checked against that answer instead of memory. Inherited code often holds behavior nobody would approve today, such as a boundary that leans the wrong way or a fallback that hides a failure. A characterization test pins it as found, because at this stage the goal is a safety net, not correctness.

`LegacyShipmentEscalationService.escalate` takes a `Shipment`, a tracking ID with a promised delivery instant, and escalates it if the window was missed by more than a two-hour grace period:

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

A shipment promised a day ago is escalated; one promised a day from now isn't. Both pin easily, with a full day of margin:

```java
@Test
void aShipmentPromisedADayAgoIsEscalated() {
    Shipment overdue = new Shipment("SHM-1001", Instant.now().minus(Duration.ofDays(1)), 10.0);

    EscalationResult result = legacy.escalate(overdue);

    assertThat(result.escalated()).isTrue();
    assertThat(result.reason()).contains("promised delivery window missed");
}
```

The margin isn't caution. `escalate` reads `Instant.now()` again, a few microseconds after the caller read it to build the shipment, so a small margin would test scheduling jitter instead of the service. A shipment whose grace period ends at exactly this instant can't be written at all: two separate `Instant.now()` calls never land on the same nanosecond. That's the cost of the hard dependency. The behavior at the service's most important boundary can't even be observed.

The second symptom is the gateway. `escalate` builds its own `SmtpCarrierGateway`, so nothing outside can hold a reference to it. The reason string on the result is the only evidence a notification happened. How many times `notifyCarrier` ran, with what arguments, or whether it was skipped on a path that needed it, are all invisible. A return value and an interaction are different facts, and a class that constructs its own collaborator hides the second.

## The object seam: java.time.Clock

An **object seam** gives a class its dependency through the constructor instead of letting it build or fetch one. `ShipmentEscalationService` is the legacy service refactored through that seam, and the dependency is the JDK's own abstraction for the current instant:

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

`Clock` is a seam the JDK ships ready-made. Production wires `Clock.systemUTC()`, which reads the wall clock; a test passes `Clock.fixed(instant, zone)`, which returns the same instant on every call. No custom interface is needed.

The seam alone should change nothing observable. Built with `Clock.systemUTC()`, the new service gives the same answers as the legacy one for the overdue and on-time wide-margin shipments, so introducing it was behavior-preserving. What changed is what a test can do to it.

## The extract-interface seam: a gateway a mock can stand in for

The second seam takes the `CarrierGateway` interface, extracted from the concrete class that used to be the only option:

```java
public interface CarrierGateway {
    void notifyCarrier(Shipment shipment, String reason);
}
```

A Mockito mock passed through it makes the interaction checkable. With `Clock.fixed`, the boundary the legacy tests couldn't reach becomes three instant, deterministic cases: exactly at the grace-period boundary, one nanosecond before and one nanosecond after.

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

No sleeping and no margin. `verify` checks that `notifyCarrier` ran, not just that the result looked right. One nanosecond before the boundary, the result isn't escalated and `verify(gateway, never()).notifyCarrier(any(), any())` holds. Checking only the return value would miss a bug that notified the carrier before the overdue check.

Now the deliberate change is safe. The legacy `now.isAfter(overdueAt)` treats a deadline equal to the current instant as on time, a boundary nobody could test before the seam. `ShipmentEscalationService` corrects it to `!now.isBefore(overdueAt)`. The wide-margin cases show the behavior away from the boundary didn't move, and the exact-boundary cases pin the corrected rule. The legacy service stays as found, a frozen reference a reviewer can compare against the one line that changed.

> **🤖 Explore with your agent**
>
> "In `examples/29-seams`, leave `LegacyShipmentEscalationService` and `LegacyShipmentRiskScorer` unchanged as references. In `ShipmentEscalationService`, take a `java.time.Clock` and a `CarrierGateway` through the constructor; production uses `Clock.systemUTC()`, and tests use `Clock.fixed` with a Mockito mock of `CarrierGateway`. Escalate when `!now.isBefore(overdueAt)`. In `ShipmentRiskScorer`, move `EnterpriseCalendar.isPeakSeason()` into a protected `isHighRiskWindow()` and override it in a test subclass. Don't use `mockStatic`."
>
> Check: `mvn -q -f examples/29-seams/pom.xml test -Dtest='ShipmentEscalationServiceTest*,ShipmentRiskScorerTest'`, and no `MockedStatic` import anywhere.

## The subclass-and-override seam: a dependency with no constructor to change

Both seams so far need a constructor a test can call with a different argument. `LegacyShipmentRiskScorer.score` reaches into `EnterpriseCalendar.isPeakSeason()`, a process-wide legacy calendar singleton that several shipment services read and no single team owns:

```java
public int score(Shipment shipment) {
    int score = baseRiskScore(shipment);
    if (EnterpriseCalendar.isPeakSeason()) {
        score += PEAK_SEASON_SURCHARGE;
    }
    return score;
}
```

Passing peak season in as a parameter is the right long-term fix; Feathers calls it Parameterize Method. It stops being cheap when the change ripples through callers you don't yet understand. A **subclass-and-override seam** moves the one call into a protected method that production never overrides, and a test overrides it:

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

Both branches become reachable with the constructor and the public `score` contract untouched. Outside peak season a light shipment scores 5 and a heavy one 40; during it, 20 and 55.

Without the seam, the legacy characterization test can only compute its expectation from the same global the code reads:

```java
int expected = EnterpriseCalendar.isPeakSeason() ? 5 + 15 : 5;

assertThat(legacy.score(light)).isEqualTo(expected);
```

That passes every day of the year and proves only that the scorer agrees with the calendar, never that the branch the calendar isn't returning today is right. One subclass with one overridden method costs less than rewriting `EnterpriseCalendar` or its other callers.

## The safety-net discipline, in order

1. **Characterize.** Pin what the code does now, wide margins and unreachable boundaries included, before touching anything.
2. **Open a seam** that fits the dependency, and show the existing characterization tests still pass against it.
3. **Change behavior,** and only now. Add a test that pins the corrected rule where it lives, and watch everything else stay green.

| Dependency | Seam |
| --- | --- |
| A constructor parameter is cheap | Object seam |
| A hard-wired collaborator has a side effect to verify | Extract-interface seam |
| No constructor change is available yet | Subclass-and-override seam |

Skipping the first step means changing code you haven't observed. Skipping the second means changing behavior with no seam, which is the original bet. In order, the three steps turn "I'm fairly sure this still works" into "everything that passed before still passes, plus one new test for what I meant to change."

## Build, run, observe

From the repository root:

```sh
mvn -f examples/pom.xml test
```

Or standalone:

```sh
cd examples/29-seams
mvn test
```

All 16 cases pass. To see the legacy and seamed services side by side:

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

The two services agree on both shipments. The gateway log is new: the legacy service never hands out the gateway it built, so nothing can read it. The demo ends with the production risk score for a heavy shipment, which depends on today's calendar.

- **Restore the legacy comparison.** In a scratch copy, change `ShipmentEscalationService` back to `now.isAfter(overdueAt)`. The exactly-at-the-boundary case fails, and the wide-margin agreement still passes.

## When not to use it

A seam has a cost. An object seam adds a constructor parameter, and a reader now has to check which `Clock` or `CarrierGateway` a service runs with. A subclass-and-override seam adds a test-only class a maintainer has to find before the protected method makes sense.

- **Skip the sequence** when the code is small and understood well enough to rewrite outright against tests of its intended contract. A ten-line helper with one call site costs less to rewrite than to characterize, seam and change. The sequence pays on code whose current behavior other systems depend on in ways nobody can list by reading it.
- **Skip the seam** when the dependency isn't hard. A value read once at startup has no substitution need, and wrapping it in a `Clock`-style abstraction on principle is speculative generality. Open a seam when a test has no other way to observe or control a dependency.

## For deeper coverage

- Michael Feathers, [*Working Effectively with Legacy Code*](https://www.oreilly.com/library/view/working-effectively-with/0131177052/) (Prentice Hall): the original definition of a seam, the catalogue of dependency-breaking techniques the object, extract-interface and subclass-and-override seams come from, and characterization tests as a discipline distinct from specification testing.
- The official [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html): mocks, `verify` and argument matchers for the `CarrierGateway` tests, and the static-mocking support the AI sidebar rules out.
- The official [`java.time.Clock` documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/time/Clock.html): the built-in object seam, including `Clock.fixed` and the other test-friendly factory methods.
- The [bibliography page]({{ '/bibliography/' | relative_url }}) lists the full reading list.
