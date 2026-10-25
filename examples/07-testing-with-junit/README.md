# 07 - Testing with JUnit 5, Mockito, and AssertJ

Every example in this book is tested the same way: JUnit 5 drives the test
lifecycle, Mockito stands in for the one collaborator with a side effect
worth watching, and AssertJ reads the assertion back in plain English. This
example builds a shipment-booking component specifically to carry that test
loop, so the suite itself is the worked example, not an afterthought bolted
onto a finished feature.

## The domain

- `Shipment`: a `record` carrying a weight, a `CargoClass`, a destination
  code, and a declared value in cents. The compact constructor enforces only
  structural invariants (non-null cargo class, non-blank destination); a
  non-positive weight or declared value is allowed through on purpose, so
  `ShipmentValidator` has something real to reject.
- `ShipmentValidator`: three independent business rules (positive weight,
  declared value within the insurable limit, hazmat under its weight
  ceiling), each throwing `InvalidShipmentException` with a specific reason.
- `FreightCostCalculator`: a pure function pricing a shipment by cargo
  class. No collaborator, no side effect, no mock anywhere in its tests.
- `RateLedger` / `InMemoryRateLedger`: the one collaborator with a side
  effect, recording a booking and reporting remaining capacity.
- `ShipmentBookingService`: validates, prices, checks capacity, and records
  a booking, in that order.

## JUnit 5: lifecycle, nesting, and parameterized tests

- `InMemoryRateLedgerTest` pairs `@BeforeEach` (a fresh ledger before every
  test) with `@AfterEach` (a capacity invariant checked after every test,
  independent of what that test itself asserted).
- `ShipmentValidatorTest` groups its cases with `@Nested` classes
  (`WhenWellFormed` / `WhenMalformed`) and drives them with all three
  parameterized sources: `@EnumSource` over every `CargoClass`,
  `@ValueSource` over a short list of bad weights, and `@MethodSource` over
  a named table of malformed shipments and the reason each one should fail
  with.
- `ShipmentTest` and `ShipmentValidatorTest` both use
  `org.junit.jupiter.api.Assertions.assertThrows` directly at least once,
  alongside AssertJ's `assertThatThrownBy`, so both idioms for exception
  testing appear in this suite.

## Mockito: mock, spy, verify, and ArgumentCaptor

`ShipmentBookingServiceTest` is the Mockito-focused suite:

- `ShipmentValidator` and `FreightCostCalculator` are used for real in every
  test; neither is mocked, because neither has a side effect worth
  intercepting.
- `RateLedger` is mocked (`@Mock`). `WhenAccepted` verifies a booking was
  recorded with an `ArgumentCaptor`, and separately with an argument matcher
  (`argThat`); `WhenValidationFails` verifies the ledger was `never()`
  touched when validation rejects the shipment first.
- `WhenLedgerIsAtCapacity` spies on a real `InMemoryRateLedger` instead of
  mocking the interface, stubbing only `capacityRemaining()` to force an
  edge condition a real run would take many bookings to reach, while
  `record()` would still run the real implementation if it were ever called.

## AssertJ: fluent assertions over bare asserts

`FreightCostCalculatorTest` chains AssertJ assertions
(`assertThat(...).isEqualTo(...).isEqualTo(...)`, `isGreaterThan`,
`hasSizeLessThanOrEqualTo`) instead of a sequence of bare `assertEquals`
calls, and `assertThatThrownBy(...).isInstanceOf(...).hasMessageContaining(...)`
reads as one sentence about what should have failed and why.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

Standalone, from this directory:

```sh
cd examples/07-testing-with-junit
mvn test
```

Also picked up by the repository root's aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 28 JUnit 5 cases. Observed locally on Fedora with
Temurin 25:

```
Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see `ShipmentBookingService` accept, reject, and reject again rather
than just reading an assertion that says it does, run the demo:

```sh
./demo.sh
```

Expected output:

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

## Files

```
examples/07-testing-with-junit/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run Main
  src/main/java/com/patterncatalyst/designpatterns/ch07/booking/
    CargoClass.java                # STANDARD / FRAGILE / HAZMAT / OVERSIZED
    Shipment.java                   # record: weightKg, cargoClass, destinationCode, declaredValueCents
    InvalidShipmentException.java    # thrown by ShipmentValidator
    ShipmentValidator.java            # three business rules, one validate() method
    Money.java                         # record: integer cents
    FreightCostCalculator.java          # pure pricing function
    BookingIdGenerator.java              # single-method; satisfied with a lambda in tests
    SequentialBookingIdGenerator.java     # production BookingIdGenerator
    BookingRecord.java                     # record: bookingId, shipment, amount
    RateLedger.java                         # the one collaborator with a side effect
    InMemoryRateLedger.java                  # real RateLedger, spied on in tests
    ShipmentBookingService.java               # validate -> capacity check -> price -> record
    Main.java                                  # demo entry point
  src/test/java/com/patterncatalyst/designpatterns/ch07/booking/
    ShipmentTest.java                 # record invariants, assertThrows
    ShipmentValidatorTest.java         # @Nested, @ValueSource/@EnumSource/@MethodSource
    FreightCostCalculatorTest.java      # AssertJ fluent chains, zero mocks
    InMemoryRateLedgerTest.java          # @BeforeEach/@AfterEach
    ShipmentBookingServiceTest.java       # @Mock, verify, ArgumentCaptor, spy, @Nested
```
