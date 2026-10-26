---
title: "xUnit test patterns"
order: 38
layout: tutorial
sectionid: docs
part: appendices
appendix: "D"
description: "Gerard Meszaros's xUnit test patterns, the four-phase test, the test-double taxonomy, fixture strategies, and the common test smells, mapped onto this book's JUnit 5, Mockito, and AssertJ stack."
duration: "20 min"
---

## What this appendix covers

Gerard Meszaros's *xUnit Test Patterns* gave the testing world a shared
vocabulary: four-phase test, test double, fixture, test smell. Every test
suite in this book already uses that vocabulary, just without naming it.
This appendix names it, and maps each term onto the JUnit 5, Mockito, and
AssertJ stack the whole book builds on, against the same collaborators
already seen elsewhere: a shipment booking service, a rate ledger, a
carrier gateway, a clock.

## The four-phase test

Meszaros splits every test into four phases, whether the test author labels
them or not:

1. **Setup** (arrange): build the object under test and its collaborators.
2. **Exercise** (act): call the one behavior under test.
3. **Verify** (assert): check the outcome.
4. **Teardown**: release anything setup acquired, reset anything shared.

JUnit 5's lifecycle annotations pull phases one and four out of the test
body entirely. `@BeforeEach` runs setup before every `@Test`; `@AfterEach`
runs teardown after every `@Test`, pass or fail. A `@Test` method then holds
only exercise and verify:

```java
@ExtendWith(MockitoExtension.class)
class ShipmentBookingServiceTest {

    private final ShipmentValidator validator = new ShipmentValidator();
    private final FreightCostCalculator costCalculator = new FreightCostCalculator();
    private final BookingIdGenerator idGenerator = () -> "BK-0001";

    @Mock
    private RateLedger ledger;

    private ShipmentBookingService service;

    @BeforeEach
    void setUp() {
        lenient().when(ledger.capacityRemaining()).thenReturn(10);   // setup
        service = new ShipmentBookingService(validator, costCalculator, ledger, idGenerator);
    }

    @Test
    void recordsThePricedBookingOnTheLedger() {
        Shipment shipment = new Shipment(3.5, CargoClass.STANDARD, "SEA", 12_000L);

        BookingRecord bookingRecord = service.book(shipment);          // exercise

        ArgumentCaptor<BookingRecord> captor = ArgumentCaptor.forClass(BookingRecord.class);
        verify(ledger).record(captor.capture());                       // verify
        assertThat(captor.getValue()).isEqualTo(bookingRecord);        // verify
    }
}
```

The `lenient().when(...)` line inside `@BeforeEach` is still setup, even
though it reads like a declaration about the future: it tells the mock what
to answer when asked, before any call happens. The field declarations above
`@BeforeEach`, `validator`, `costCalculator`, and `idGenerator`, are setup
too, re-created fresh for every test because JUnit 5 constructs a new test
instance per `@Test` method by default (the `PER_METHOD` lifecycle). This
example carries no explicit `@AfterEach`, because Mockito's own extension
resets each `@Mock` field between tests; a plain collaborator, a counter or
a file handle, is where teardown earns an explicit method.

## The test-double taxonomy

Meszaros names five kinds of test double. This book's stack has a concrete
answer for each. Mockito's own `spy()` method is not the same concept as
Meszaros's Spy pattern, even though they share a word.

| Double | What it does | In this stack |
|---|---|---|
| **Dummy** | A placeholder that satisfies a signature but is never exercised | A `BookingIdGenerator` lambda passed into `ShipmentBookingService` for a test whose shipment fails validation before id generation ever runs |
| **Stub** | Returns canned values on request, no verification of how it was called | `Clock.fixed(overdueAt, zone)`, pinning the instant `ShipmentEscalationService` reads when it checks a delivery deadline |
| **Spy** | Records what was called, inspected after the fact with no expectation declared up front | A hand-written `RecordingCarrierGateway` that appends every `notifyCarrier` call to a list a test reads after exercise, no `verify` involved |
| **Mock** | Verifies an expected interaction as part of the test, failing if it was not met | A Mockito `@Mock RateLedger`, checked with `verify(ledger).record(...)` |
| **Fake** | A working, lightweight implementation that behaves correctly but is unsuited to production | A hand-written `FakeCarrierGateway` that records and can answer queries about what it "sent," entirely in memory, with no SMTP connection |

**Dummy.** `ShipmentBookingService`'s constructor takes a `BookingIdGenerator`
along with a validator, a cost calculator, and a ledger. The test that proves
a malformed shipment never reaches the ledger hands the service the same
`() -> "BK-0001"` lambda every other test in the class uses, but that lambda
is never consulted in this one case:

```java
@Test
void neverTouchesTheLedger() {
    Shipment malformed = new Shipment(-1.0, CargoClass.STANDARD, "SEA", 12_000L);

    assertThatThrownBy(() -> service.book(malformed)).isInstanceOf(InvalidShipmentException.class);

    verify(ledger, never()).record(any());
}
```

`ShipmentValidator.validate` throws before `book` ever calls
`idGenerator.nextId()`, which makes `idGenerator` a dummy for this one test
even though the identical object is a real, consulted collaborator in every
accepted-booking test in the same class. Dummy status is a property of what
one test exercises, not a property of the object itself.

**Stub.** A `Clock` stub pins "now" to one fixed instant so a delivery-window
check becomes deterministic:

```java
Instant overdueAt = Instant.parse("2026-01-10T12:00:00Z");
ShipmentEscalationService service =
        new ShipmentEscalationService(Clock.fixed(overdueAt, ZoneOffset.UTC), gateway);
```

No `verify` call follows that construction. The test does not care how many
times `clock.instant()` is called inside `escalate`, only what each call
answers.

**Spy.** A hand-written `RecordingCarrierGateway` records every notification
instead of returning a scripted answer or declaring an expectation before
the fact:

```java
final class RecordingCarrierGateway implements CarrierGateway {
    private final List<String> notified = new ArrayList<>();

    @Override
    public void notifyCarrier(Shipment shipment, String reason) {
        notified.add(shipment.id() + ": " + reason);
    }

    List<String> notified() {
        return List.copyOf(notified);
    }
}
```

The test exercises `escalate`, then asserts against `notified()` directly:

```java
assertThat(recordingGateway.notified())
        .anyMatch(entry -> entry.startsWith("SHM-2101"));
```

This is the exact pattern Meszaros calls a Spy, and it is hand-written
rather than built with Mockito's `spy()` method. Mockito's `spy()` wraps a
real object and lets specific methods be overridden while every other
method keeps the real object's behavior, which answers a different
question than this pattern does; calling both of them a "spy" is a
vocabulary collision between the library and the pattern name, not a sign
that they solve the same problem.

**Mock.** Mockito's `@Mock` plus `verify` makes the interaction itself the
assertion:

```java
@Mock
private RateLedger ledger;

// ...
verify(ledger).record(captor.capture());
```

The test fails if `record` was never called at all, which is what separates
a Mock from a Stub: a Stub is judged only by what it returns, a Mock is
judged by whether it was called the way the test expected.

**Fake.** A `FakeCarrierGateway` is a working implementation, not a script:
it keeps a real, queryable list of what was sent, and correctly reflects
every call made to it, but it is never wired into production because it has
no SMTP connection behind it:

```java
final class FakeCarrierGateway implements CarrierGateway {
    private final Map<String, String> dispatched = new LinkedHashMap<>();

    @Override
    public void notifyCarrier(Shipment shipment, String reason) {
        dispatched.put(shipment.id(), reason);
    }

    Optional<String> reasonSentFor(String shipmentId) {
        return Optional.ofNullable(dispatched.get(shipmentId));
    }
}
```

A Fake earns its cost exactly when a Stub's "return one canned value" is too
thin to drive the logic under test, for example an end-to-end escalation
test that books, escalates, and then queries what was sent for a specific
shipment, something a one-shot stub answer cannot model.

Mockito's `spy()` sits closest to a Fake-plus-Mock hybrid in practice: a
real `InMemoryRateLedger` wrapped with `spy()`, then forced on exactly one
method, keeps every other method's real behavior while still letting
`verify` check an interaction on the wrapped object:

```java
InMemoryRateLedger realLedger = new InMemoryRateLedger(1);
RateLedger ledgerSpy = spy(realLedger);
doReturn(0).when(ledgerSpy).capacityRemaining();

// ...
verify(ledgerSpy, never()).record(any());
assertThat(realLedger.recordedBookings()).isEmpty();
```

Neither `realLedger` nor `ledgerSpy` is a Meszaros Spy here. `realLedger` is
the real collaborator under a forced edge case, and the `verify` call makes
the check a Mock-style interaction assertion, not a pattern this book treats
as a Fake plus a Spy and leaves at that.

## Fixture strategies

**Fresh fixture.** JUnit 5's default lifecycle, `PER_METHOD`, constructs a
new test instance for every single `@Test`, so field initializers and
`@BeforeEach` both run fresh every time. `ShipmentBookingServiceTest` above
is a fresh fixture twice over: the test instance itself is new per method,
and `@BeforeEach` additionally rebuilds `service` from scratch, so nothing
one test does to `ledger` is visible to any other test in the class.

**Shared (suite) fixture.** `@BeforeAll` runs once for an entire test class
rather than once per test, but a non-static `@BeforeAll` method needs
`@TestInstance(Lifecycle.PER_CLASS)` to opt the class out of the default
per-method instantiation:

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FreightCostCalculatorTest {

    private CarrierRateCard rateCard;

    @BeforeAll
    void loadRateCardOnce() {
        rateCard = CarrierRateCard.loadFromClasspath("rates/standard.csv");
    }
}
```

A shared fixture is tempting when a collaborator is expensive to build, a
rate card parsed from a file being the obvious candidate, but it
reintroduces the exact problem fresh fixtures exist to avoid the moment that
collaborator is mutable: state one test leaves behind becomes visible to the
next test in the same class, and the class's tests stop being safe to run in
a different order. Reach for `@BeforeAll` only when the collaborator is
read-only for the whole class's lifetime, the way a rate card loaded once
from a fixed table is.

**Transient vs. persistent fixture.** A transient fixture starts empty and
is discarded after the test, the default for an `InMemoryRateLedger`
constructed fresh inside `@BeforeEach`. A persistent fixture is carried
across tests by design, which is a real hazard whenever a collaborator holds
process-wide state. `SequentialBookingIdGenerator` wraps an `AtomicLong`
counter; constructing a fresh instance per test, the way `@BeforeEach`
normally would, keeps that counter transient. Sharing one instance across a
class's tests, through a `@BeforeAll`-built field, makes the counter
persistent instead, and every test after the first then sees ids counting
up from wherever the previous test left off. A persistent fixture needs an
explicit reset step for exactly this reason, typically a package-private
reset hook the fixture calls in both `@BeforeEach` and `@AfterEach`, not
only one of the two: a failed assertion partway through a test still has to
leave the shared counter in a known state for the next test, and `@AfterEach`
is the one lifecycle method JUnit 5 guarantees runs even when the test body
throws.

## Test smells

Meszaros catalogs dozens of test smells. These five show up most often in a
suite built on this stack, each with the fix this book's own examples apply.

| Smell | Symptom | Fix in this stack |
|---|---|---|
| **Fragile test** | A refactor that changes no behavior still breaks tests | Depend on the collaborator's interface (`RateLedger`, `CarrierGateway`, `Clock`), never on a concrete implementation's internals |
| **Erratic (flaky) test** | Passes or fails depending on run order or wall-clock timing | Replace `Instant.now()` with an injected `Clock`; keep process-wide state like a sequential id counter transient instead of shared across tests |
| **Obscure test** | A reader cannot tell what behavior a failure indicates | One behavior per `@Test`, a `@DisplayName` naming that behavior, `@Nested` classes grouping related scenarios (accepted, validation failure, at capacity) |
| **Conditional test logic** | A test body branches on `if`/`else` or loops over cases | `@ParameterizedTest` with `@MethodSource` for a table of shipments and expected outcomes, instead of a loop inside one test that can silently skip a case |
| **Slow test** | A suite that should run in milliseconds takes seconds because it touches a real store or a real network endpoint | A `Fake` in-memory collaborator, or a `spy()`-wrapped real one with the expensive call forced, in place of a real connection; tag slow, I/O-bound tests with `@Tag("integration")` and keep them out of the default `mvn test` run |

## AssertJ and the verify phase

AssertJ's fluent chains are where the verify phase reads as a sentence
instead of a scattered set of equality checks:

```java
assertThat(bookingRecord)
        .extracting(BookingRecord::bookingId, br -> br.amount(), br -> br.shipment().cargoClass())
        .containsExactly("BK-0001", Money.ofCents(570L), CargoClass.STANDARD);
```

A failure here names which of the three extracted values diverged and what
it held instead, which is the entire argument for AssertJ over a bare
`assertEquals` call per field.

A guard assertion still exists in this stack, just expressed through
AssertJ's `Optional` support rather than a raw `assertTrue` before a
dereference:

```java
assertThat(result).isPresent();
assertThat(result.get().amount()).isEqualTo(Money.ofCents(570L));
```

JUnit 5's and AssertJ's ordinary assertions both behave like a fail-fast
check: the first one that fails stops the test method immediately, so a
test checking three unrelated facts in sequence only ever reports the
first failure. AssertJ's `SoftAssertions` is the tool for the opposite
behavior, collecting every failure in a block and reporting all of them
together once the block ends:

```java
assertSoftly(softly -> {
    softly.assertThat(bookingRecord.amount()).isEqualTo(Money.ofCents(570L));
    softly.assertThat(bookingRecord.shipment().cargoClass()).isEqualTo(CargoClass.STANDARD);
});
```

A regression that breaks both the amount and the cargo class at once is
reported as two failures, not one failure hiding a second behind it; reach
for `assertSoftly` specifically when a single exercise phase produces
several independent facts worth checking, not as a default replacement for
an ordinary `assertThat` chain.

## For deeper coverage

- Gerard Meszaros, [*xUnit Test Patterns: Refactoring Test Code*](https://www.informit.com/store/xunit-test-patterns-refactoring-test-code-9780132800051)
  (Addison-Wesley): the source for every pattern name in this appendix, the
  four-phase test, the double taxonomy, fixture strategy, and the test
  smell catalog.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/):
  the full lifecycle and extension model this appendix's fixture section
  draws `@BeforeEach`, `@AfterEach`, `@BeforeAll`, and `@TestInstance` from.
- The official [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html):
  the complete mock, spy, and verification API, including the `spy()`
  method whose naming collision with Meszaros's Spy pattern this appendix
  calls out directly.
- The official [AssertJ documentation](https://assertj.github.io/doc/):
  the fluent assertion vocabulary, including `SoftAssertions`, behind this
  appendix's verify-phase section.
- The full reading list behind this book's opinions lives on the
  [bibliography page]({{ '/bibliography/' | relative_url }}); each citation
  there is a pointer to the full book, not a substitute for reading it.
