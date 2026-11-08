---
title: "Testing with JUnit 5, Mockito, and AssertJ"
order: 7
layout: tutorial
sectionid: docs
part: "java-substrate"
description: "The testing stack every example in this book runs behind the scenes: JUnit 5's lifecycle and parameterized tests, Mockito's mocks and spies, and AssertJ's fluent assertions, taught against a shipment-booking service with one collaborator mocked and two called directly."
duration: "35 min"
---

## Learning objectives

By the end of this chapter you can:

- Choose between a `@BeforeEach` fixture and state built inside a test, and check a post-condition in `@AfterEach`.
- Replace near-identical `@Test` methods with one `@ParameterizedTest` fed by `@ValueSource`, `@EnumSource` or a named `@MethodSource` table, and pick the source that fits the input.
- Group related cases under `@Nested` classes with `@DisplayName`, so a report reads as a specification.
- Assert that a call throws the right exception with the right message, with `Assertions.assertThrows` and with AssertJ's `assertThatThrownBy`.
- Choose a Mockito mock, a spy or the real collaborator for a dependency, and verify an interaction with `verify` and an `ArgumentCaptor`.
- Recognize a test that restates the code instead of checking its behavior, and say what to write instead.

## Why this matters

Every example makes a checkable claim: a Strategy prices a shipment identically whichever mechanism quotes it, a segregated interface leaves an SMS gateway nothing to stub, a sealed outcome keeps a rejection reason `Optional` would lose. A claim is worth something only once code runs and checks it, which is why every example module ends in `src/test`.

The stack has three parts because each answers a different question:

| Library | Answers |
| --- | --- |
| JUnit 5 | what runs before and after a test, how a family of cases is written once, how cases are grouped in a report |
| Mockito | did the component talk to its collaborator correctly, not only return the right value |
| AssertJ | nothing new; it makes checks read as a sentence and report which fact failed |

The component here, a shipment booking service, needs all three. It validates a shipment, checks a ledger's remaining capacity, prices the shipment and records the booking. Two collaborators are deterministic with no side effects; one writes to storage. Telling those apart comes before choosing a tool.

{% include excalidraw.html name="07-test-anatomy" caption="Figure 7.1: One JUnit 5 test method, arranged with a mock, acting on a real call into one real and one mocked (or spied) collaborator, and asserted with AssertJ and Mockito.verify answering two different questions." %}

## The domain: booking a shipment

A `Shipment` carries a weight, a `CargoClass` (`STANDARD`, `FRAGILE`, `HAZMAT`, `OVERSIZED`), a destination code and a declared value in cents:

```java
public record Shipment(double weightKg, CargoClass cargoClass, String destinationCode, long declaredValueCents) {

    public Shipment {
        Objects.requireNonNull(cargoClass, "cargoClass");
        if (destinationCode == null || destinationCode.isBlank()) {
            throw new IllegalArgumentException("destinationCode must not be blank");
        }
    }
}
```

The compact constructor enforces only structure: a cargo class and a destination. A non-positive weight or declared value constructs fine, because it breaks a business rule, not the shape. Checking it here would make it impossible to build the well-formed but invalid `Shipment` that the validator's tests need.

`ShipmentValidator` owns the business rules: four checks, each throwing `InvalidShipmentException` with its own reason, first failure wins:

```java
    public void validate(Shipment shipment) {
        if (shipment.weightKg() <= 0.0) {
            throw new InvalidShipmentException("weight must be positive, got " + shipment.weightKg() + "kg");
        }
        if (shipment.declaredValueCents() <= 0L) {
            throw new InvalidShipmentException("declared value must be positive");
        }
        if (shipment.declaredValueCents() > MAX_INSURABLE_DECLARED_VALUE_CENTS) {
            throw new InvalidShipmentException("declared value exceeds the insurable limit");
        }
        if (shipment.cargoClass() == CargoClass.HAZMAT && shipment.weightKg() > HAZMAT_WEIGHT_CEILING_KG) {
            throw new InvalidShipmentException(
                    "hazmat shipments over " + HAZMAT_WEIGHT_CEILING_KG + "kg require freight approval");
        }
    }
```

`ShipmentBookingService` orchestrates validate, check capacity, price and record:

```java
    public BookingRecord book(Shipment shipment) {
        validator.validate(shipment);
        if (ledger.capacityRemaining() <= 0) {
            throw new IllegalStateException("rate ledger is at capacity; booking rejected");
        }
        Money amount = costCalculator.cost(shipment);
        BookingRecord bookingRecord = new BookingRecord(idGenerator.nextId(), shipment, amount);
        ledger.record(bookingRecord);
        return bookingRecord;
    }
```

| Collaborator | Shape | In tests |
| --- | --- | --- |
| `ShipmentValidator` | deterministic, no side effect | the real class |
| `FreightCostCalculator` | pure: base rate plus per-kilogram rate by cargo class | the real class |
| `BookingIdGenerator` | one-method interface | a lambda |
| `RateLedger` | writes a booking somewhere durable, reports remaining capacity | a Mockito mock, or a spy over `InMemoryRateLedger` |

## Test lifecycle: fixtures, grouping, and reporting

The ledger's tests each need a fresh ledger, and one invariant deserves checking after every test, whatever each one asserted:

```java
    @BeforeEach
    void setUp() {
        // Fresh ledger before every test, so no test can see another
        // test's bookings. A field assigned here, not in a constructor,
        // is what lets every test method start from the same known state
        // without hand-copying the same three lines into each one.
        ledger = new InMemoryRateLedger(CAPACITY);
    }

    @AfterEach
    void checkCapacityInvariantHeld() {
        // A post-condition check that runs after every test in this class,
        // independent of what that test itself asserted: this ledger must
        // never hold more bookings than its declared capacity, whether the
        // test above filled it, left it empty, or tried to overfill it.
        assertThat(ledger.recordedBookings()).hasSizeLessThanOrEqualTo(CAPACITY);
    }
```

`@BeforeEach` runs before every `@Test`, so no test starts from a ledger another filled. `@AfterEach` runs after every test, pass or fail. That's the place for a post-condition: written at the bottom of each test body, it would be skipped in exactly the case where an earlier assertion already failed.

The validator's tests split into two groups, so they use `@Nested`:

```java
    @Nested
    @DisplayName("when the shipment is well-formed")
    class WhenWellFormed {
```

```java
    @Nested
    @DisplayName("when the shipment breaks a business rule")
    class WhenMalformed {
```

A flat list of methods carries the same assertions, but a reader rebuilds the grouping from method names. With `@DisplayName` on the classes and methods, a report reads as a sentence about the contract: "ShipmentValidator, when the shipment breaks a business rule, rejects a non-positive weight."

## One rule, many inputs: parameterized tests

JUnit 5 offers three argument sources, each for a different shape of input:

| Source | Fits | Grows by |
| --- | --- | --- |
| `@ValueSource` | a short list of primitives against one assertion | editing the list |
| `@EnumSource` | every constant of an enum | itself: a new constant is a new run |
| `@MethodSource` | cases needing several values and a name | a new row in the table |

`@ValueSource` for weights:

```java
        @ParameterizedTest(name = "rejects a weight of {0}kg")
        @ValueSource(doubles = {0.0, -1.0, -42.5})
        @DisplayName("rejects a non-positive weight")
        void rejectsNonPositiveWeight(double weightKg) {
            Shipment shipment = new Shipment(weightKg, CargoClass.STANDARD, "SEA", 10_000L);

            assertThatThrownBy(() -> validator.validate(shipment))
                    .isInstanceOf(InvalidShipmentException.class)
                    .hasMessageContaining("weight must be positive");
        }
```

`@EnumSource` for cargo classes, so a fifth `CargoClass` is covered with no list to update:

```java
        @ParameterizedTest(name = "validates a {0} shipment")
        @EnumSource(CargoClass.class)
        @DisplayName("accepts every cargo class at a moderate weight")
        void validatesEveryCargoClass(CargoClass cargoClass) {
            Shipment shipment = new Shipment(10.0, cargoClass, "SEA", 10_000L);

            assertThatCode(() -> validator.validate(shipment)).doesNotThrowAnyException();
        }
```

`@MethodSource` for rule violations, each row with a name, a shipment and the expected reason:

```java
        @ParameterizedTest(name = "{0}")
        @MethodSource("com.patterncatalyst.designpatterns.ch07.booking.ShipmentValidatorTest#invalidShipments")
        @DisplayName("rejects each case with the matching reason")
        void rejectsWithExpectedReason(String caseName, Shipment shipment, String expectedReasonFragment) {
            assertThatThrownBy(() -> validator.validate(shipment))
                    .isInstanceOf(InvalidShipmentException.class)
                    .hasMessageContaining(expectedReasonFragment);
        }
    }

    static Stream<Arguments> invalidShipments() {
        return Stream.of(
                Arguments.of(
                        "declared value over the insurable limit",
                        new Shipment(5.0, CargoClass.STANDARD, "SEA", 10_000_000L),
                        "insurable limit"),
                Arguments.of(
                        "hazmat over the unescorted weight ceiling",
                        new Shipment(75.0, CargoClass.HAZMAT, "SEA", 10_000L),
                        "freight approval"),
                Arguments.of(
                        "a negative declared value",
                        new Shipment(5.0, CargoClass.STANDARD, "SEA", -500L),
                        "declared value"));
    }
```

A new rule is a new row, not a new copy of the assertion. Each row is reported as its own test, and `name = "{0}"` makes a failure report "hazmat over the unescorted weight ceiling" rather than an index to count.

## Proving a rejection: exception assertions, two ways

JUnit's `Assertions.assertThrows` returns the exception for a second assertion to inspect:

```java
        @Test
        @DisplayName("rejects a zero declared value, checked with Assertions.assertThrows")
        void rejectsZeroDeclaredValueUsingAssertThrows() {
            Shipment shipment = new Shipment(5.0, CargoClass.STANDARD, "SEA", 0L);

            InvalidShipmentException thrown =
                    assertThrows(InvalidShipmentException.class, () -> validator.validate(shipment));

            assertThat(thrown.getMessage()).contains("declared value");
        }
```

AssertJ's `assertThatThrownBy`, used in the weight and table cases above, folds "it throws" and "what it throws" into one chain. Both check the same fact. AssertJ is the default because of what a failure says once several properties of one value need checking: a chained AssertJ assertion names the property that failed and what it held, while a bare `assertTrue` over a combined condition reports only `false`, and several `assertEquals` calls spread one fact across lines.

## Mockito: choosing a mock, a spy, or the real thing

The booking service's tests build the deterministic collaborators as real objects:

```java
    private final ShipmentValidator validator = new ShipmentValidator();
    private final FreightCostCalculator costCalculator = new FreightCostCalculator();
    private final BookingIdGenerator idGenerator = () -> "BK-0001";
```

Mocking the validator would make the tests easier to write and worse at their job. A mock that never throws makes every accepted-shipment test pass whether or not `book` calls `validate` at the right point, which is the fact worth proving. The id generator needs only a lambda: there's no interaction with it to verify beyond its return value.

The ledger, the one collaborator with a side effect, is a `@Mock`:

```java
    @Mock
    private RateLedger ledger;
```

For an accepted shipment, an `ArgumentCaptor` captures what was recorded, to compare with what `book` returned and with the real calculator's price:

```java
        @Test
        @DisplayName("records the priced booking on the ledger")
        void recordsThePricedBookingOnTheLedger() {
            BookingRecord bookingRecord = service.book(shipment);

            ArgumentCaptor<BookingRecord> captor = ArgumentCaptor.forClass(BookingRecord.class);
            verify(ledger).record(captor.capture());

            assertThat(captor.getValue()).isEqualTo(bookingRecord);
            assertThat(captor.getValue().amount()).isEqualTo(costCalculator.cost(shipment));
        }
```

For a shipment that fails validation, the absence of a call is the assertion:

```java
            assertThatThrownBy(() -> service.book(malformed)).isInstanceOf(InvalidShipmentException.class);

            verify(ledger, never()).record(any());
```

Checking only the exception would pass even if `book` recorded the booking first and validated afterward. The interaction check catches that; a return-value check can't.

A ledger with no capacity left calls for a spy. Producing one without stubbing means booking it full first, which hides the one fact being checked. `spy()` wraps a real `InMemoryRateLedger`, and `doReturn` stubs only `capacityRemaining`; `record`, if called, would run the real code:

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

The last line is a fact about real state that a mock can't give: the ledger under the spy holds no bookings.

| Use | When |
| --- | --- |
| the real collaborator | it's deterministic and has no side effect |
| a mock | it has a side effect and none of its real behavior needs to run |
| a spy | most of its real behavior should run and one hard-to-produce condition needs forcing |

The fixture's capacity stub is marked `lenient()`:

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

With `MockitoExtension`, strict stubbing fails a test with `UnnecessaryStubbingException` when a stub set up for it goes unused. Accepted bookings reach the capacity check; a shipment that fails validation never does, and the at-capacity test builds its own spy and never touches the mock. Without `lenient()`, both of those fail on the unused stub instead of on their own assertions. Use `lenient()` for a shared stub that some tests legitimately skip, not to silence strict stubbing in general.

> **🤖 Explore with your agent**
>
> "In `examples/07-testing-with-junit`, add a test to `ShipmentBookingServiceTest` for a `HAZMAT` shipment of 75.0 kg. Use the real `ShipmentValidator` and `FreightCostCalculator`, never mocks of them, and the existing `@Mock RateLedger`. Assert with AssertJ's `assertThatThrownBy` that `book` throws `InvalidShipmentException` with a message containing 'freight approval', and verify with `verify(ledger, never()).record(any())` that nothing was recorded. Put it in the nested class for validation failures, give it a `@DisplayName`, and don't stub anything new."
>
> Check: `mvn -q -f examples/07-testing-with-junit/pom.xml test -Dtest=ShipmentBookingServiceTest` passes; then move `validator.validate(shipment)` below `ledger.record(bookingRecord)` in a scratch copy, and the new test must fail.

## Build, run, observe

From the repository root, through the aggregator, or standalone:

```sh
mvn -f examples/pom.xml test
```

```sh
cd examples/07-testing-with-junit
mvn test
```

All 28 tests pass, covering the lifecycle, parameterized and nested styles and the mock, spy and captor styles. Run the demo to see the service accept two bookings and refuse two:

```sh
examples/07-testing-with-junit/demo.sh
```

It books two well-formed shipments into a two-slot ledger, then shows both rejection paths:

```
== Booking two well-formed shipments ==
BK-0001: $5.70 (STANDARD, 3.5kg)
BK-0002: $6.16 (FRAGILE, 1.2kg)

== Rejected before pricing: a validation failure ==
Rejected: hazmat shipments over 50.0kg require freight approval

== Rejected after validation: the ledger is at capacity ==
Rejected: rate ledger is at capacity; booking rejected

Ledger now holds 2 bookings.
```

A booking can pass validation and still be refused, for a reason unrelated to the shipment. Two experiments, each in a scratch copy:

- **Record before validating.** In `book`, move `validator.validate(shipment)` below `ledger.record(bookingRecord)`. The validation-failure test fails: its `never()` verification sees a `record` call.
- **Drop `lenient()`.** Stub `capacityRemaining` with a plain `when(...)` in the fixture. Two tests error with `UnnecessaryStubbingException`: the validation failure and the at-capacity case, neither of which reads the shared stub.

## When not to use these styles

- **`@Nested` around one test** organizes for its own sake. Grouping pays once two or three related cases share a name.
- **`@ParameterizedTest` with one row** solves a problem that doesn't exist yet. A plain `@Test` is shorter until a second case arrives.
- **`@EnumSource` when some constants need special handling** forces branching inside one assertion body. A `@ValueSource` list or a `@MethodSource` table avoids it.
- **Mocking a pure function** adds setup and removes confidence. `FreightCostCalculator` and `ShipmentValidator` never sit behind a mock, because calling the real method is simpler and closer to what callers of `ShipmentBookingService` depend on.
- **Stubbing several methods on a spy** turns a mostly-real collaborator into a hybrid that's harder to reason about than a mock or the real object.
- **Asserting on internals** couples a test to details a correct refactor may change. Reading a private field by reflection, or the size of `InMemoryRateLedger`'s backing list instead of `capacityRemaining()`, breaks on a refactor that changes nothing callers see. The ledger's tests check `recordedBookings()` and `capacityRemaining()`, both public behavior.

## For deeper coverage

- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/): the full lifecycle, extension model, and parameterized-test source vocabulary beyond the handful this chapter used.
- The official [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html): the complete mock, spy, matcher, and verification API, including strict stubbing and the `lenient()` escape hatch this chapter's fixture relies on.
- The official [AssertJ documentation](https://assertj.github.io/doc/): the fluent assertion vocabulary across every type this chapter's suite touches, records, exceptions, and collections alike.
- Gerard Meszaros, [*xUnit Test Patterns: Refactoring Test Code*](https://www.informit.com/store/xunit-test-patterns-refactoring-test-code-9780131495050) (Addison-Wesley): the four-phase test structure and test-double taxonomy, mock, stub, spy, and fake, that this chapter's mock-versus-spy decision draws its vocabulary from.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
