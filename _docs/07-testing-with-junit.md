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

- Choose between a `@BeforeEach` fixture and state built inline in a test
  method, and reset shared state in `@AfterEach` when a side effect would
  otherwise leak from one test into the next.
- Replace a handful of near-identical `@Test` methods with one
  `@ParameterizedTest`, driven by `@ValueSource`, `@EnumSource`, or a named
  `@MethodSource` table, and say which source fits which shape of input.
- Group related cases under `@Nested` classes with `@DisplayName`, so a test
  report reads as a specification of the component's behavior rather than a
  flat list of method names.
- Assert that a call throws the right exception with the right message,
  using both `Assertions.assertThrows` and AssertJ's `assertThatThrownBy`.
- Decide between a Mockito mock, a Mockito spy, and a real collaborator for
  a given dependency, and verify an interaction with `verify` and an
  `ArgumentCaptor` rather than checking only a return value.
- Recognize a test that restates the code it exercises instead of asserting
  its behavior, including one an AI pairing partner is likely to hand you,
  and say what to write instead.

## Why this matters

Every chapter in this book makes a specific, checkable claim: a Strategy
prices a shipment identically no matter which of three mechanisms quotes
it, a segregated interface leaves an SMS gateway with no `render` method to
stub, a sealed outcome keeps a rejection reason a plain `Optional` would
throw away. None of those claims is worth anything until something runs the
code and checks it, which is why every example module in this book ends in
`src/test`, not in `src/main`. This chapter is where that testing stack
gets its own, deliberate treatment, so every chapter after it can spend its
test suite proving the chapter's claim instead of re-explaining how a
fixture or a mock works.

The stack has three parts because no single library answers every question
a test needs to ask. JUnit 5 owns the test lifecycle: what runs before and
after a test, how a family of near-identical cases is expressed once, and
how related cases are grouped so a failure report reads like a
specification. Mockito answers a question JUnit cannot: did a component
talk to its collaborator correctly, not only did it return the right value.
AssertJ does not add a new category of check; it makes the checks from
either library read as a sentence a reviewer can parse without tracing
argument order, which matters more than it sounds once a test suite is
large enough that someone other than its author has to read a failure and
act on it quickly.

The component this chapter builds, a shipment booking service, is sized to
need all three. It validates a shipment against business rules, prices it
with a pure calculation, checks a ledger's remaining capacity, and records
the booking if every step clears. Two of those collaborators are
deterministic and side-effect free; the third writes to storage. That split
is not an accident of this example's design. It is the fact every
real test suite in this book's domain has to recognize before choosing a
tool, and this chapter trains the decision on a case where getting it wrong
is cheap to notice.

{% include excalidraw.html name="07-test-anatomy" caption="Figure 7.1: One JUnit 5 test method, arranged with a mock, acting on a real call into one real and one mocked (or spied) collaborator, and asserted with AssertJ and Mockito.verify answering two different questions." %}

## The domain: booking a shipment

The example behind this chapter lives in `examples/07-testing-with-junit`.
A `Shipment` is a record carrying a weight, a `CargoClass`
(`STANDARD`, `FRAGILE`, `HAZMAT`, `OVERSIZED`), a destination code, and a
declared value in cents:

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

The compact constructor enforces only the structural invariants, a cargo
class and a destination. A non-positive weight or a non-positive declared
value passes through construction untouched: whether those are acceptable
is a business rule, not a structural one, and checking it here instead
would make it impossible for a test to build a well-formed `Shipment` that
still breaks the rule, which is exactly the record this chapter's validator
tests need.

`ShipmentValidator` owns that business rule, and it checks three
independent conditions, throwing `InvalidShipmentException` with a specific
reason on the first one it finds broken:

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

`FreightCostCalculator` prices an accepted shipment with a pure function:
a base rate plus a per-kilogram rate, both varying by cargo class, with no
collaborator and no side effect. `RateLedger` is the opposite kind of
component: an interface with a `record` method that writes a booking
somewhere durable, and a `capacityRemaining` method a caller checks before
committing to a booking. `ShipmentBookingService` orchestrates all three:
validate, check capacity, price, record.

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

Four collaborators, two shapes: `validator` and `costCalculator` are plain
and deterministic, `ledger` has a side effect, and `idGenerator` is a
single-method contract plain enough to satisfy with a lambda. Every tool
this chapter trains answers one of those two shapes, never both at once.

## The test lifecycle: fixtures, grouping, and reporting

`InMemoryRateLedgerTest` is where `@BeforeEach` and `@AfterEach` earn their
keep. Every test in the class needs a fresh ledger, and one more thing, a
capacity invariant, deserves checking after every test regardless of what
that test itself asserted:

```java
@BeforeEach
void setUp() {
    ledger = new InMemoryRateLedger(CAPACITY);
}

@AfterEach
void checkCapacityInvariantHeld() {
    assertThat(ledger.recordedBookings()).hasSizeLessThanOrEqualTo(CAPACITY);
}
```

`@BeforeEach` runs before every `@Test` in the class, so no test can start
from a ledger another test already filled. `@AfterEach` runs after every
test, pass or fail, which is what makes it the right place for a
post-condition like this one: a test that tries to overfill the ledger and
expects an exception still leaves the ledger in a state worth re-checking,
and putting the check only at the bottom of each test body would mean
skipping it on exactly the test where an early assertion failure already
signaled trouble.

`ShipmentValidatorTest` reaches for the other lifecycle tool, `@Nested`,
because its cases split cleanly into two groups that deserve separate
names in a test report:

```java
@Nested
@DisplayName("when the shipment is well-formed")
class WhenWellFormed {
    // ...
}

@Nested
@DisplayName("when the shipment breaks a business rule")
class WhenMalformed {
    // ...
}
```

A flat list of fifteen `@Test` methods named `rejectsNegativeWeight`,
`rejectsZeroWeight`, `acceptsStandardCargo`, and so on would carry exactly
the same assertions as these two nested classes, but a reader scanning a
failure report has to parse each method name individually to reconstruct
the grouping that `@Nested` states directly. `@DisplayName` on both the
nested classes and the methods inside them turns a test run's output into
something closer to a specification: "ShipmentValidator, when the shipment
breaks a business rule, rejects a non-positive weight" reads as a sentence
about the component's contract, not as a Java identifier a human reverse-
engineers into English.

## One rule, many inputs: parameterized tests

`ShipmentValidator`'s three rules need checking against more than one
input apiece, and JUnit 5 gives this chapter three different sources for
that, each suited to a different shape of case. `@ValueSource` fits a short
list of primitive values checked against the same assertion:

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

`@EnumSource` fits a closed set of constants checked the same way, one run
per value with no list to maintain by hand:

```java
@ParameterizedTest(name = "validates a {0} shipment")
@EnumSource(CargoClass.class)
@DisplayName("accepts every cargo class at a moderate weight")
void validatesEveryCargoClass(CargoClass cargoClass) {
    Shipment shipment = new Shipment(10.0, cargoClass, "SEA", 10_000L);

    assertThatCode(() -> validator.validate(shipment)).doesNotThrowAnyException();
}
```

Adding a fifth `CargoClass` constant later extends this test automatically,
with no list of enum values to remember to update; the same cannot be said
of a `@ValueSource` list, which is exactly why the weight cases above use
the narrower tool and the cargo-class case uses this one. `@MethodSource`
is the third shape, for cases where each input needs more than one
primitive value and a name:

```java
@ParameterizedTest(name = "{0}")
@MethodSource("...#invalidShipments")
@DisplayName("rejects each case with the matching reason")
void rejectsWithExpectedReason(String caseName, Shipment shipment, String expectedReasonFragment) {
    assertThatThrownBy(() -> validator.validate(shipment))
            .isInstanceOf(InvalidShipmentException.class)
            .hasMessageContaining(expectedReasonFragment);
}

static Stream<Arguments> invalidShipments() {
    return Stream.of(
            Arguments.of("declared value over the insurable limit",
                    new Shipment(5.0, CargoClass.STANDARD, "SEA", 10_000_000L), "insurable limit"),
            Arguments.of("hazmat over the unescorted weight ceiling",
                    new Shipment(75.0, CargoClass.HAZMAT, "SEA", 10_000L), "freight approval"),
            Arguments.of("a negative declared value",
                    new Shipment(5.0, CargoClass.STANDARD, "SEA", -500L), "declared value"));
}
```

The payoff is the same one a table-driven test gives in any language: a
fourth rule means a fourth row in `invalidShipments`, not a fourth copy of
the assertion logic, and the `{0}` in the display name means a failing case
reports its own name, "hazmat over the unescorted weight ceiling," not an
index a reader has to count rows to decode.

```java
// @ParameterizedTest surfaces each row of the table as its own named test.
// A fourth case is a fourth row in invalidShipments(); the method body
// never changes, and a failure reports its case name directly, not a loop
// index a reader has to trace back to a row.
@ParameterizedTest(name = "{0}")
@MethodSource("invalidShipments")
void rejectsWithExpectedReason(String caseName, Shipment shipment, String expectedReasonFragment) {
    assertThatThrownBy(() -> validator.validate(shipment))
            .isInstanceOf(InvalidShipmentException.class)
            .hasMessageContaining(expectedReasonFragment);
}
```

`@ParameterizedTest` turns the table into as many independently reported,
independently named test results as there are rows. A failure on the second
row reports that row's own case name, not a shared pass or fail line a
reader has to trace back to a specific row by hand.

## Proving a rejection: exception assertions, two ways

`ShipmentTest` and `ShipmentValidatorTest` both check that a call throws,
and both idioms for that appear in this suite rather than picking one and
hiding the other. JUnit 5's own `Assertions.assertThrows` returns the
caught exception, which a second assertion can then inspect:

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

AssertJ's `assertThatThrownBy` folds the same two facts, that a call throws
and what it throws, into one fluent chain, which is the shape this
chapter's other exception checks use:

```java
assertThatThrownBy(() -> validator.validate(shipment))
        .isInstanceOf(InvalidShipmentException.class)
        .hasMessageContaining("weight must be positive");
```

Both forms check an identical fact. The difference AssertJ buys is legibility
once more than one property of the result needs checking, which is the same
argument for AssertJ generally: a bare JUnit assertion checks one fact per
line, and checking several facts about the same value means either several
separate `assertEquals` calls or a wall of `&&` inside one `assertTrue`,
neither of which reports which specific fact failed.

```java
// One AssertJ chain reads as a single sentence about the same object, and
// extracting() lets several related properties sit in one readable
// assertion instead of three unrelated-looking lines.
assertThat(bookingRecord)
        .extracting(BookingRecord::bookingId, br -> br.amount(), br -> br.shipment().cargoClass())
        .containsExactly("BK-0001", Money.ofCents(570L), CargoClass.STANDARD);
```

`ShipmentValidatorTest` still reaches for a bare `Assertions.assertThrows`
in exactly one place to keep that idiom visible in a suite that otherwise
favors AssertJ. AssertJ wins by default in this book's examples because of
what a failure message looks like: AssertJ's `containsExactly` reports
which of the three extracted values did not match and what it held
instead, while a failed `assertTrue` on a combined boolean reports only
`false`, leaving the reader to re-derive which of three conditions was the
one that broke.

## Mockito: choosing a mock, a spy, or the real thing

`ShipmentBookingServiceTest` is where the decision this chapter trains
becomes concrete, because its one component has four collaborators split
across both answers. `validator` and `costCalculator` are constructed as
the real classes and called directly in every test:

```java
private final ShipmentValidator validator = new ShipmentValidator();
private final FreightCostCalculator costCalculator = new FreightCostCalculator();
private final BookingIdGenerator idGenerator = () -> "BK-0001";
```

Neither is mocked, because mocking either one would make the test faster to
write and worse at its job. `validator.validate(shipment)` is deterministic
and has no side effect; a mock that always returns without throwing would
make every "accepted shipment" test pass regardless of whether
`ShipmentBookingService` actually calls `validate` at the right point, which
is precisely the fact worth proving. `idGenerator` needs even less: a
single-method contract is satisfied with a plain lambda, no Mockito
involved, because there is no interaction with it worth verifying beyond
the value it returns.

`ledger`, in contrast, is a `@Mock`:

```java
@Mock
private RateLedger ledger;
```

`ledger` is the one collaborator with a side effect, recording a booking
somewhere a test should never have to provision. The test for an
accepted shipment both checks the returned `BookingRecord` and verifies the
interaction with the mock, using an `ArgumentCaptor` to inspect exactly
what was recorded:

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

A test for a shipment that fails validation turns the same mock around and
asserts the absence of a call, with an argument matcher standing in for
"whatever was passed, it does not matter":

```java
assertThatThrownBy(() -> service.book(malformed)).isInstanceOf(InvalidShipmentException.class);

verify(ledger, never()).record(any());
```

`never()` makes the interaction itself the assertion. A test that only
checked the thrown exception would pass even if `ShipmentBookingService`
called `ledger.record` before validation ran and threw afterward, a bug an
interaction check catches and a return-value check cannot.

The third case calls for a spy instead of a mock. Forcing a
real `RateLedger` implementation to report zero remaining capacity would
otherwise mean booking it full first, which works but obscures the one
fact the test actually cares about. `spy()` wraps a real
`InMemoryRateLedger`, and `doReturn` stubs exactly the one method the test
needs to force, while `record`, if it were ever called, would still run
the real implementation:

```java
InMemoryRateLedger realLedger = new InMemoryRateLedger(1);
RateLedger ledgerSpy = spy(realLedger);
doReturn(0).when(ledgerSpy).capacityRemaining();

ShipmentBookingService atCapacityService =
        new ShipmentBookingService(validator, costCalculator, ledgerSpy, idGenerator);

assertThatThrownBy(() -> atCapacityService.book(shipment))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("capacity");

verify(ledgerSpy, never()).record(any());
assertThat(realLedger.recordedBookings()).isEmpty();
```

The last line is the point a mock could not make: `realLedger`, the actual
object underneath the spy, still has zero recorded bookings, which is a
fact about real state, not about a canned answer a mock was told to give.
Reach for a spy specifically when most of a collaborator's behavior should
run unmodified and only one hard-to-produce condition needs forcing; reach
for a full mock when none of the real implementation needs to run.

One stubbing detail in this suite's `@BeforeEach` deserves a direct
explanation, because it is a common surprise the first time a reader hits
it:

```java
@BeforeEach
void setUp() {
    lenient().when(ledger.capacityRemaining()).thenReturn(10);
    service = new ShipmentBookingService(validator, costCalculator, ledger, idGenerator);
}
```

Mockito's default strict stubbing fails a test with an
`UnnecessaryStubbingException` if a stub configured anywhere in the class is
never used by every test that runs. The capacity stub above is reached by
every accepted-booking test, but a shipment that fails `validate` never
gets as far as checking capacity at all, so that nested test class would
otherwise fail for the wrong reason, an unused stub, rather than pass or
fail on its own assertion. `lenient()` tells Mockito this particular stub
is allowed to go unused in some tests; reach for it exactly in this
situation, not as a blanket way to silence the strict-stubbing check.

> **AI sidebar.** Ask an LLM pairing partner to "write tests for
> `ShipmentBookingService`" and a common result compiles, runs green, and
> proves almost nothing. The tautological shape shows up first: a test that
> stubs `ledger.record(any())` to return normally and then asserts that
> `book()` returned a value, which only confirms the mock did what it was
> told to do, not that `ShipmentBookingService` validates, prices, or
> records correctly. The over-mocking shape shows up next: a generated
> suite that mocks `ShipmentValidator` and `FreightCostCalculator` even for
> tests that need neither, because mocking every collaborator looks
> thorough regardless of whether any of them has a side effect worth
> isolating. Read every generated test the way this chapter trained: does
> this assertion check a fact a bug could break, or does it only check that
> a stub returned what it was configured to return? A generated test suite
> with no `verify(..., never())`, no `ArgumentCaptor`, and a mock for every
> single collaborator regardless of purity is a strong sign the model
> reached for Mockito's mechanics without reaching for the actual question
> an interaction test exists to answer.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this
loop. From the repository root, the aggregator picks up
`examples/07-testing-with-junit` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/07-testing-with-junit
mvn test
```

Either path runs the same 28 JUnit 5 cases, covering the lifecycle,
parameterized, and nested styles from `ShipmentValidatorTest` and
`InMemoryRateLedgerTest`, and the mock, spy, and captor styles from
`ShipmentBookingServiceTest`. Observed locally on Fedora with Temurin 25:

```
Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see `ShipmentBookingService` accept a booking, reject one on validation,
and reject a third on ledger capacity, rather than just reading an
assertion that says it does, run the example's own demo script:

```sh
examples/07-testing-with-junit/demo.sh
```

It books two well-formed shipments into a two-slot ledger, then shows both
rejection paths:

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

The second rejection is the same fact `WhenLedgerIsAtCapacity` proves with
a spy: a booking can clear validation and still be refused, for a reason
that has nothing to do with the shipment itself.

## When not to use these styles

Every tool in this chapter earns its cost only when the shape it answers
is actually present in the thing under test, and each one has a plainer
alternative that wins everywhere else.

A `@Nested` class with a single test inside it is organizing for its own
sake; grouping pays off once at least two or three related cases share one
name, the way `WhenWellFormed` and `WhenMalformed` do here. A `@ParameterizedTest` with exactly one row in its `@MethodSource` is
solving a problem that does not exist yet; a plain `@Test` is shorter and
just as clear until a second case shows up to justify the table. `@EnumSource`
is the wrong choice the moment a test needs to exclude or specially handle
only some of an enum's constants; forcing every value through one assertion
body produces awkward in-method branching that a `@ValueSource` list or a
`@MethodSource` table would avoid entirely.

Mockito's cost is the easiest one to overspend without noticing, for the
same reason the AI sidebar names: it is easy to mock a collaborator by
reflex rather than because it has a side effect worth isolating.
`FreightCostCalculator` and `ShipmentValidator` never appear behind a mock
anywhere in this chapter's suite, because calling the real method and
checking its real return value is both simpler to write and closer to what
a caller of `ShipmentBookingService` actually depends on. Mocking a pure
function to "isolate" a test from code that has nothing to isolate from
adds setup and removes confidence at the same time. A spy carries a
narrower trap: stubbing more methods on a spy than the one edge case
actually requires turns a mostly-real collaborator into a confusing hybrid,
part real and part scripted, that is harder to reason about than either a
plain mock or the unmodified real thing.

Asserting on a component's internals rather than its behavior fails
quietly rather than loudly. A test that reaches into
`ShipmentBookingService` with reflection to check a private field, or that
asserts `InMemoryRateLedger`'s backing list has a particular size instead
of asserting on `capacityRemaining()`, the method callers actually use, is
coupled to an implementation detail a correct refactor is free to change.
`InMemoryRateLedgerTest` checks `recordedBookings()` and
`capacityRemaining()`, both public, observable facts about the ledger's
behavior, precisely so that a future change to how bookings are stored
internally does not break a test that never needed to know.

## For deeper coverage

- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/): the full lifecycle, extension model, and parameterized-test source vocabulary beyond the handful this chapter used.
- The official [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html): the complete mock, spy, matcher, and verification API, including strict stubbing and the `lenient()` escape hatch this chapter's fixture relies on.
- The official [AssertJ documentation](https://assertj.github.io/doc/): the fluent assertion vocabulary across every type this chapter's suite touches, records, exceptions, and collections alike.
- Gerard Meszaros, [*xUnit Test Patterns: Refactoring Test Code*](https://www.informit.com/store/xunit-test-patterns-refactoring-test-code-9780131495050) (Addison-Wesley): the four-phase test structure and test-double taxonomy, mock, stub, spy, and fake, that this chapter's mock-versus-spy decision draws its vocabulary from.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
