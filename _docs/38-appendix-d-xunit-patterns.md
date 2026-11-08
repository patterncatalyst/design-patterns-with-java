---
title: "xUnit test patterns"
order: 38
layout: tutorial
sectionid: docs
part: appendices
appendix: "D"
description: "Gerard Meszaros's xUnit test patterns, the four-phase test, the test-double taxonomy, fixture strategies, and the common test smells, mapped onto JUnit 5, Mockito, and AssertJ."
duration: "20 min"
---

## Learning objectives

By the end of this appendix you can:

- Name the four phases of a test and say where JUnit 5 puts each one.
- Tell a dummy, stub, spy, mock and fake apart, and build each with JUnit 5, Mockito or plain Java.
- Choose a fresh, shared or persistent fixture and say what each risks.
- Recognize five common test smells and the fix for each.
- Write the verify phase with AssertJ chains and soft assertions.

## Why this matters

Gerard Meszaros's *xUnit Test Patterns* named the parts of a test: four-phase test, test double, fixture, test smell. With the names, a review comment can say "that's a shared fixture" instead of a paragraph. The tables and listings below map each name onto JUnit 5, Mockito and AssertJ, using the shipment booking service, rate ledger, carrier gateway and clock from the examples.

## The four-phase test

| Phase | Also called | Does | In JUnit 5 |
|---|---|---|---|
| Setup | arrange | Builds the object under test and its collaborators | Field initializers and `@BeforeEach` |
| Exercise | act | Calls the one behavior under test | The `@Test` body |
| Verify | assert | Checks the outcome | The `@Test` body: AssertJ, `verify` |
| Teardown | | Releases what setup acquired, resets what's shared | `@AfterEach`, which runs pass or fail |

With setup and teardown in lifecycle methods, a `@Test` holds only exercise and verify:

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

Stubbing in `@BeforeEach` is setup: it tells the mock what to answer before any call. The fields are setup too, rebuilt for every test because JUnit 5 creates a new instance per `@Test` method (the `PER_METHOD` lifecycle). No `@AfterEach` is needed, because `MockitoExtension` creates new mocks for each test. Teardown earns an explicit method when a test holds a counter, a file handle or other shared state.

## The test-double taxonomy

Meszaros names five doubles. Mockito's `spy()` isn't Meszaros's Spy, despite the shared word.

| Double | What it does | In this stack |
|---|---|---|
| **Dummy** | Fills a parameter and is never used | The `BookingIdGenerator` lambda in a test whose shipment fails validation before an id is generated |
| **Stub** | Returns canned answers; nobody checks how it was called | `Clock.fixed(overdueAt, zone)`, pinning the instant `ShipmentEscalationService` reads |
| **Spy** | Records calls to inspect afterward, with no expectation declared up front | A hand-written `RecordingCarrierGateway` that appends every `notifyCarrier` call to a list |
| **Mock** | Fails if the expected interaction didn't happen | A Mockito `@Mock RateLedger`, checked with `verify(ledger).record(...)` |
| **Fake** | A working, lightweight implementation unfit for production | A `FakeCarrierGateway` that keeps what it "sent" in memory, with no SMTP connection |

### Dummy

The same `() -> "BK-0001"` lambda is a real collaborator in every accepted-booking test and a dummy here, because `ShipmentValidator.validate` throws before `book` calls `idGenerator.nextId()`. Being a dummy depends on what one test exercises, not on the object:

```java
@Test
void neverTouchesTheLedger() {
    Shipment malformed = new Shipment(-1.0, CargoClass.STANDARD, "SEA", 12_000L);

    assertThatThrownBy(() -> service.book(malformed)).isInstanceOf(InvalidShipmentException.class);

    verify(ledger, never()).record(any());
}
```

### Stub

A `Clock` stub pins "now" to one instant, so a delivery-window check is deterministic. No `verify` follows; it doesn't matter how often `escalate` reads the clock, only what it answers:

```java
Instant overdueAt = Instant.parse("2026-01-10T12:00:00Z");
ShipmentEscalationService service =
        new ShipmentEscalationService(Clock.fixed(overdueAt, ZoneOffset.UTC), gateway);
```

### Spy

A hand-written Spy records each notification, read back after exercising `escalate`:

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

```java
assertThat(recordingGateway.notified())
        .anyMatch(entry -> entry.startsWith("SHM-2101"));
```

Mockito's `spy()` answers a different question: it wraps a real object and overrides chosen methods while the rest keep their real behavior.

### Mock

`@Mock` plus `verify` makes the interaction the assertion. Verification fails if `record` was never called. A stub is judged by what it returns; a mock by whether it was called as expected:

```java
@Mock
private RateLedger ledger;

// ...
verify(ledger).record(captor.capture());
```

### Fake

A Fake works: it keeps a queryable record of every call and answers questions about it. It's never wired into production, because nothing real sits behind it:

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

Use a fake when one canned answer is too thin to drive the logic, such as an end-to-end test that books, escalates and then asks what was sent for one shipment.

### Mockito's `spy()` in the taxonomy

A real `InMemoryRateLedger` under `spy()`, forced on one method, keeps its real behavior everywhere else and still accepts `verify`:

```java
InMemoryRateLedger realLedger = new InMemoryRateLedger(1);
RateLedger ledgerSpy = spy(realLedger);
doReturn(0).when(ledgerSpy).capacityRemaining();

// ...
verify(ledgerSpy, never()).record(any());
assertThat(realLedger.recordedBookings()).isEmpty();
```

Neither object is a Meszaros Spy. `realLedger` is the real collaborator under a forced edge case, and `verify` makes the check a mock-style interaction assertion.

## Fixture strategies

| Strategy | How JUnit 5 builds it | Risk |
|---|---|---|
| **Fresh** | `PER_METHOD` lifecycle: a new instance per `@Test`, with field initializers and `@BeforeEach` run each time | None from shared state; the cost is rebuilding it |
| **Shared** | `@BeforeAll`, once per class; `static`, or non-static with `@TestInstance(TestInstance.Lifecycle.PER_CLASS)` | A mutable shared object leaks state between tests, which then depend on run order |
| **Transient** | Created in setup and discarded afterward, like an `InMemoryRateLedger` built in `@BeforeEach` | None |
| **Persistent** | Survives across tests by design | Each test sees what the last one left; needs a reset in `@BeforeEach` and `@AfterEach` |

`ShipmentBookingServiceTest` is fresh twice over: the instance is new per method, and `@BeforeEach` rebuilds `service`, so nothing one test does to `ledger` reaches another.

Share a fixture only when it's expensive and read-only for the whole class, such as a rate table loaded once from a file. `SequentialBookingIdGenerator` shows the persistent-fixture hazard. It wraps an `AtomicLong`, so one instance shared across a class makes ids count on from wherever the previous test stopped. A persistent fixture needs a reset in both `@BeforeEach` and `@AfterEach`: `@AfterEach` runs even when the test body throws, so a failed test still leaves a known state.

## Test smells

| Smell | Symptom | Fix in this stack |
|---|---|---|
| **Fragile test** | A refactor that changes no behavior breaks tests | Depend on the collaborator's interface (`RateLedger`, `CarrierGateway`, `Clock`), never a concrete class's internals |
| **Erratic (flaky) test** | Result depends on run order or the wall clock | Inject a `Clock` instead of calling `Instant.now()`; keep counters like a sequential id generator transient |
| **Obscure test** | A failure doesn't say which behavior broke | One behavior per `@Test`, a `@DisplayName` naming it, `@Nested` classes for scenarios (accepted, validation failure, at capacity) |
| **Conditional test logic** | The method body branches or loops over cases | `@ParameterizedTest` with `@MethodSource`, so no case is skipped silently |
| **Slow test** | A run that should take milliseconds touches a real store or network | An in-memory fake, or a spied real object with the expensive call forced; tag I/O-bound tests `@Tag("integration")` and exclude them from the default run |

## AssertJ and the verify phase

An AssertJ chain reads as one sentence. On failure, `extracting` names which value diverged and what it held:

```java
assertThat(bookingRecord)
        .extracting(BookingRecord::bookingId, br -> br.amount(), br -> br.shipment().cargoClass())
        .containsExactly("BK-0001", Money.ofCents(570L), CargoClass.STANDARD);
```

Assertions fail fast: the first failure stops the method, and later facts go unchecked. `assertSoftly` collects every failure in the block and reports them together:

```java
assertSoftly(softly -> {
    softly.assertThat(bookingRecord.amount()).isEqualTo(Money.ofCents(570L));
    softly.assertThat(bookingRecord.shipment().cargoClass()).isEqualTo(CargoClass.STANDARD);
});
```

A regression that breaks the amount and the cargo class reports two failures, not one hiding the other. Use `assertSoftly` when one exercise produces several independent facts, not as the default.

A guard assertion, such as `isPresent()` on an `Optional` before reading its value, is still a separate first assertion: it turns a `NoSuchElementException` into a failure that says what was missing.

## For deeper coverage

- Gerard Meszaros, [*xUnit Test Patterns: Refactoring Test Code*](https://www.informit.com/store/xunit-test-patterns-refactoring-test-code-9780132800051)
  (Addison-Wesley): the source of every pattern name here, the
  four-phase test, the double taxonomy, fixture strategy, and the test
  smell catalog.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/):
  the lifecycle and extension model behind `@BeforeEach`, `@AfterEach`,
  `@BeforeAll`, and `@TestInstance`.
- The official [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html):
  the mock, spy, and verification API, including the `spy()` method whose
  name collides with Meszaros's Spy.
- The official [AssertJ documentation](https://assertj.github.io/doc/):
  the fluent assertion vocabulary, including `SoftAssertions`.
- The full reading list is on the
  [bibliography page]({{ '/bibliography/' | relative_url }}).
