---
title: "Mockito reference"
order: 37
layout: tutorial
sectionid: docs
part: appendices
appendix: "C"
description: "Reference for Mockito: mock() and @Mock, stubbing with when/thenReturn and doReturn/doThrow, spy(), verify() with times, never and InOrder, argument matchers, ArgumentCaptor, and strict stubbing, on shipping collaborators."
duration: "15 min"
---

## Learning objectives

By the end of this appendix you can:

- Choose between a mock, a stub, a spy, a fake and a real object for a collaborator.
- Create mocks with `@Mock` and `MockitoExtension`, and stub value-returning and `void` methods.
- Verify call counts, absent calls and call order.
- Combine matchers correctly and capture an argument for a later assertion.
- Say when strict stubbing fails a test and when `lenient()` is the right answer.

## Why this matters

Every collaborator double in the examples is built with Mockito, from the `mockito-core` and `mockito-junit-jupiter` artifacts the aggregator already supplies, so no module adds a dependency. A mock checks an interaction: that a call happened, how often, with what, and in what order. A test that checks a returned value can't see a call that shouldn't have happened. Mocks and seams in context are covered in [Testing with JUnit 5, Mockito, and AssertJ]({{ '/docs/07-testing-with-junit/' | relative_url }}) and [Seams and the test safety net]({{ '/docs/29-seams-and-the-test-safety-net/' | relative_url }}).

## Choosing a double

| Double | What it does | Use it when the assertion is about | Example |
|---|---|---|---|
| Mock | Records calls; every unstubbed method returns a default | A call: which method, which arguments, how many times | A `CarrierGateway` that must be notified once, or not at all |
| Stub | A mock with `when(...).thenReturn(...)` and no `verify` | Nothing about the collaborator; it only has to answer | A rate client that returns a fixed quote |
| Spy | Wraps a real object; real methods run unless stubbed | Real state, with one hard-to-reach condition forced | A real `InMemoryRateLedger` that reports itself full |
| Fake | A small working implementation | State across several calls | An in-memory ledger: record, then read back |
| Real object | The production type | The result, when the type is fast, deterministic and in-process | `Clock.fixed(...)`, a record, a pure calculator |

The rule of thumb: if "it checks that..." ends with a method and an argument, use a mock. If it ends with a value something should hold afterward, use a fake or a real object. A mock earns its cost when the real collaborator is slow, nondeterministic or leaves the process; mocking a fast, deterministic type adds stubbing and gains nothing.

## Creating a mock

`@Mock` with `MockitoExtension` creates each annotated field before every test and checks framework usage after it:

```java
@ExtendWith(MockitoExtension.class)
class ShipmentEscalationServiceTest {

    private static final Duration OVERDUE_GRACE = Duration.ofHours(2);

    @Mock
    private CarrierGateway gateway;
}
```

`Mockito.mock(CarrierGateway.class)` creates the same mock inline, for a one-off double in one method. Either way, the mock implements the interface and answers every call with a default (`null`, `0`, `false`, an empty `Optional` or an empty collection) until it's stubbed. It never runs a real method.

## Stubbing

| Form | Use it for |
|---|---|
| `when(mock.method(args)).thenReturn(value)` | A method with a return value |
| `when(mock.method(args)).thenThrow(exception)` | Making a value-returning method fail |
| `doThrow(exception).when(mock).method(args)` | Making a `void` method fail; `when(...)` needs a return value to wrap |
| `doReturn(value).when(spy).method(args)` | Stubbing a spy without running the real method |

```java
@Test
void secondLookupForTheSameShipmentNeverReachesTheWrappedQuoter() {
    RateQuote quote = new RateQuote("ground", Money.ofCents(740));
    when(inner.quote(shipment)).thenReturn(quote);
    CachingRateQuoter caching = new CachingRateQuoter(inner);

    RateQuote first = caching.quote(shipment);
    RateQuote second = caching.quote(shipment);

    assertThat(first).isEqualTo(quote);
    assertThat(second).isEqualTo(quote);
    verify(inner, times(1)).quote(shipment);
    assertThat(caching.missCount()).isEqualTo(1);
    assertThat(caching.hitCount()).isEqualTo(1);
}
```

`when(spy.method())` calls the real method once to record which call is being stubbed, with whatever side effects it has. The `do...` forms don't, which is why spies use them.

## `spy()`: a real object with one method forced

A spy runs the real implementation for every call except the ones a test stubs. That lets a test force one condition that's hard to produce, here a full ledger, and still check real state underneath:

```java
InMemoryRateLedger realLedger = new InMemoryRateLedger(1);
RateLedger ledgerSpy = spy(realLedger);
doReturn(0).when(ledgerSpy).capacityRemaining();

ShipmentBookingService atCapacityService =
        new ShipmentBookingService(validator, costCalculator, ledgerSpy, idGenerator);
Shipment shipment = new Shipment(2.0, CargoClass.STANDARD, "SEA", 9_000L);

assertThatThrownBy(() -> atCapacityService.book(shipment))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("capacity");

verify(ledgerSpy, never()).record(any());
assertThat(realLedger.recordedBookings()).isEmpty();
```

Use a spy when most of a collaborator should run unchanged. Use a mock when none of it should: a spy still carries the real object's construction and state.

## A real object instead of a double

`java.time.Clock` is the clearest case. Production code takes a `Clock`; a test passes `Clock.fixed(instant, zone)`, a real `Clock` that always returns one instant. It needs no stubbing, and the JDK already tests its contract:

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

## `verify()`

| Form | Checks |
|---|---|
| `verify(mock).method(args)` | Exactly one matching call |
| `verify(mock, times(n))` / `atLeastOnce()` | A specific or minimum count |
| `verify(mock, never()).method(args)` | No matching call |
| `verifyNoInteractions(mocks...)` | No call of any kind on the mocks |
| `inOrder(mocks...).verify(mock).method(args)` | Calls across mocks in sequence |

`never()` makes the absence of a call the assertion. A booking that fails validation must not reach the ledger; checking only the exception would pass even if `record` ran first:

```java
Shipment malformed = new Shipment(-1.0, CargoClass.STANDARD, "SEA", 12_000L);

assertThatThrownBy(() -> service.book(malformed)).isInstanceOf(InvalidShipmentException.class);

verify(ledger, never()).record(any());
```

`verifyNoInteractions` is the stronger form, for a precondition that must stop a pipeline before any step runs:

```java
FunctionalOrderPipeline pipeline = new FunctionalOrderPipeline("test-channel", validator, pricer, recorder);
Order empty = new Order("ORD-2", "Customer", List.of(), "");

assertThatIllegalArgumentException().isThrownBy(() -> pipeline.process(empty));

verifyNoInteractions(validator, pricer, recorder);
```

`InOrder` checks a sequence. A shipping facade has to issue the tracking number before it quotes, renders the label and builds the tracking URL:

```java
InOrder order = inOrder(trackingRegistry, carrierGateway, labelPrinter);
order.verify(trackingRegistry).issueTrackingNumber(1);
order.verify(carrierGateway).quote(shipment);
order.verify(labelPrinter).render(shipment, "TRK-1001");
order.verify(trackingRegistry).trackingUrl("TRK-1001");
```

`InOrder` constrains only the calls it names. Other calls on the same mocks are unconstrained unless another `verify` covers them.

## Argument matchers

A plain argument compares with `equals()`. A matcher replaces it with a rule: `any()` accepts anything, `eq(x)` compares by equality, and `argThat(predicate)` takes a lambda:

```java
verify(ledger).record(argThat(recorded -> recorded.bookingId().equals("BK-0001")));
```

Matchers are all-or-nothing within one call. Once one argument uses a matcher, every argument must. `notifyCarrier(eq(shipment), "promised delivery window missed by more than 2h")` compiles and then fails at run time with `InvalidUseOfMatchersException`. Wrap the literal in `eq(...)`, or use `any()` when the value doesn't matter:

```java
verify(gateway).notifyCarrier(eq(shipment), any());
```

## `ArgumentCaptor`

A matcher decides which call counts; it can't hand the argument back. An `ArgumentCaptor` can, which matters when the method under test builds the object and no test could write it down in advance. `capture()` goes inside `verify`, and `getValue()` (or `getAllValues()` for repeated calls) returns what was passed:

```java
BookingRecord bookingRecord = service.book(shipment);

ArgumentCaptor<BookingRecord> captor = ArgumentCaptor.forClass(BookingRecord.class);
verify(ledger).record(captor.capture());

assertThat(captor.getValue()).isEqualTo(bookingRecord);
assertThat(captor.getValue().amount()).isEqualTo(costCalculator.cost(shipment));
```

`@Captor` is the field form, created by `MockitoExtension` like `@Mock`:

```java
@Captor
private ArgumentCaptor<String> messageCaptor;

@Test
void theNotificationMessageNamesTheShipmentAndTheTotal() {
    LegacyShipmentProcessor processor =
            new LegacyShipmentProcessor("ops-supervisor@example.com", notificationGateway);

    processor.dispatch(new Shipment("SH-5002", 10_000));

    verify(notificationGateway).sendDispatchConfirmation(eq("ops-supervisor@example.com"), messageCaptor.capture());
    assertThat(messageCaptor.getValue())
            .contains("SH-5002")
            .contains("$108.00");
}
```

## Strict stubbing and `lenient()`

`MockitoExtension` uses strict stubs: a test that leaves one of its stubs unused fails with `UnnecessaryStubbingException`. That catches a stub left behind after the code that used it was removed. It also fails a test for the wrong reason when a shared `@BeforeEach` stub is reached by only some tests. The booking service checks ledger capacity only after validation passes, so the rejection tests never call the stub:

```java
@BeforeEach
void setUp() {
    // lenient(): the capacity check this stubs is reached by every
    // accepted booking below, but never by a booking that fails
    // ShipmentValidator's checks first. Mockito's strict stubbing would
    // otherwise fail the validation-rejection tests with an
    // UnnecessaryStubbingException for a stub they never call.
    lenient().when(ledger.capacityRemaining()).thenReturn(10);
    service = new ShipmentBookingService(validator, costCalculator, ledger, idGenerator);
}
```

Mark only the shared stub `lenient()`, never a whole class. Without it, the validation-failure and at-capacity tests both error with `UnnecessaryStubbingException`.

## For deeper coverage

- The official [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html): the complete mock, spy, matcher, `ArgumentCaptor`, and strict-stubbing API.
- Gerard Meszaros, [*xUnit Test Patterns: Refactoring Test Code*](https://www.informit.com/store/xunit-test-patterns-refactoring-test-code-9780132800051) (Addison-Wesley): the mock, stub, spy, and fake taxonomy behind the double table.
- The full reading list is on the [bibliography page]({{ '/bibliography/' | relative_url }}).
