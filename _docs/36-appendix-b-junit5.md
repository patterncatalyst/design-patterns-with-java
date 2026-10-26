---
title: "JUnit 5 reference"
order: 36
layout: tutorial
sectionid: docs
part: appendices
appendix: "B"
description: "A reference for the JUnit 5 annotations, lifecycle, assertions, and parameterized/dynamic test styles this book's examples use, and how Maven Surefire runs them."
duration: "15 min"
---

## What this appendix covers

Every example's test suite in this book runs on JUnit 5 (Jupiter). The
testing chapter builds that foundation narratively, against a shipment
booking service with a validator, a cost calculator, and a rate ledger.
This appendix is the lookup table: the annotations, what each one
guarantees, and when to reach for which one. Mocking collaborators with
Mockito has its own appendix; nothing here covers `@Mock` or `verify`
beyond acknowledging where a test needs one.

## `@Test`: registering a case

`@Test` marks a no-argument instance method as a test case. Jupiter
discovers every `@Test` method on the class, creates a fresh instance of
the class for each one, and runs it independently of every other test:

```java
class FreightCostCalculatorTest {

    @Test
    void chargesTheBaseRatePlusAPerKilogramFee() {
        FreightCostCalculator calculator = new FreightCostCalculator();
        Shipment shipment = new Shipment(3.5, CargoClass.STANDARD, "SEA", 10_000L);

        Money cost = calculator.cost(shipment);

        assertThat(cost).isEqualTo(Money.ofCents(570));
    }
}
```

A fresh instance per test method means fields initialized inline, or set
in `@BeforeEach`, never leak state from one test to the next through a
shared instance. No test method depends on another method having run
first, or on the order Jupiter happens to discover them in.

## Lifecycle: `@BeforeEach`, `@AfterEach`, `@BeforeAll`, `@AfterAll`

`@BeforeEach` runs before every `@Test` in the class; `@AfterEach` runs
after every one, whether it passed or failed. Together they give a class
of related tests a fixture that is rebuilt from scratch each time:

```java
class InMemoryRateLedgerTest {

    private static final int CAPACITY = 5;
    private InMemoryRateLedger ledger;

    @BeforeEach
    void setUp() {
        ledger = new InMemoryRateLedger(CAPACITY);
    }

    @AfterEach
    void checkCapacityInvariantHeld() {
        assertThat(ledger.recordedBookings()).hasSizeLessThanOrEqualTo(CAPACITY);
    }

    @Test
    void startsWithFullCapacityAvailable() {
        assertThat(ledger.capacityRemaining()).isEqualTo(CAPACITY);
    }
}
```

`@AfterEach` running regardless of pass or fail is what makes it the right
place for a postcondition like the capacity check above: a test that tries
to overfill the ledger and expects an exception still leaves the ledger in
a state worth re-checking, and a check placed only at the bottom of each
test body would be skipped on exactly the test where an earlier assertion
already failed.

`@BeforeAll` and `@AfterAll` run once per test class instead of once per
test, on static methods, for setup that is expensive and safe to share
read-only across every test in the class:

```java
class CarrierRateTableTest {

    private static CarrierRateTable rateTable;

    @BeforeAll
    static void loadReferenceRates() {
        rateTable = CarrierRateTable.loadFromReferenceFile();
    }

    @Test
    void groundRateForRegionalZoneMatchesThePublishedTariff() {
        assertThat(rateTable.rateFor(Carrier.GROUND, DestinationZone.REGIONAL))
                .isEqualTo(Money.ofCents(480));
    }
}
```

Reach for `@BeforeAll` only when the setup is read-only for the whole
class and expensive enough to matter, loading a reference rate table from
a file once instead of once per test. A field any test mutates belongs in
`@BeforeEach` instead, so a mutation in one test cannot bleed into the
next. `@BeforeAll`/`@AfterAll` methods must be `static` unless the class
carries `@TestInstance(Lifecycle.PER_CLASS)`, which this book's examples
do not use.

## Assertions, `assertThrows`, and `assertAll`

Jupiter's `Assertions` class covers the common checks directly, with
AssertJ layered on top in this book's examples for anything that benefits
from a fluent chain:

| Method | Checks |
|---|---|
| `assertEquals(expected, actual)` | equality |
| `assertTrue(condition)` / `assertFalse(condition)` | a boolean condition |
| `assertNotNull(value)` / `assertNull(value)` | nullity |
| `assertThrows(type, executable)` | the call throws exactly `type`, and returns the caught exception |
| `assertAll(executables...)` | every executable runs and reports, even if more than one fails |

`assertThrows` returns the exception it caught, so a second line can
inspect the message without a separate try/catch:

```java
@Test
void rejectsAShipmentOverTheInsurableLimit() {
    ShipmentValidator validator = new ShipmentValidator();
    Shipment shipment = new Shipment(5.0, CargoClass.STANDARD, "SEA", 10_000_000L);

    InvalidShipmentException thrown =
            assertThrows(InvalidShipmentException.class, () -> validator.validate(shipment));

    assertThat(thrown.getMessage()).contains("insurable limit");
}
```

`assertAll` groups independent checks on the same result so a failure in
one does not hide a failure in another. A bare sequence of `assertEquals`
calls stops reporting at the first failure; `assertAll` runs every
supplied executable and reports every failure together:

```java
@Test
void recordsEveryFieldOfTheBooking() {
    BookingRecord booking = service.book(shipment);

    assertAll(
            () -> assertEquals("BK-0001", booking.bookingId()),
            () -> assertEquals(CargoClass.STANDARD, booking.shipment().cargoClass()),
            () -> assertEquals(Money.ofCents(570), booking.amount()));
}
```

A regression that corrupts the booking ID while leaving the amount
correct shows up as exactly one failing line inside the `assertAll`
report, not a test that stops after the first problem and hides the
second. This book's examples reach for AssertJ's `assertThat(...).extracting(...)`
for the same multi-fact shape when the values come off one object; both
express the same idea, and either is a better choice than an
`assertTrue` wrapping several `&&`-joined conditions, which reports only
`false` with no indication of which condition broke.

## `@ParameterizedTest`: one method, many rows

A `@ParameterizedTest` method runs once per value or row a source
supplies, instead of one `@Test` method per case. Four source annotations
cover most shapes a test table needs.

`@ValueSource` supplies a short list of primitive values, each checked by
the same assertion:

```java
@ParameterizedTest(name = "rejects a weight of {0}kg")
@ValueSource(doubles = {0.0, -1.0, -42.5})
void rejectsNonPositiveWeight(double weightKg) {
    ShipmentValidator validator = new ShipmentValidator();
    Shipment shipment = new Shipment(weightKg, CargoClass.STANDARD, "SEA", 10_000L);

    assertThrows(InvalidShipmentException.class, () -> validator.validate(shipment));
}
```

`@EnumSource` runs the test once per constant of an enum, with no list of
values to maintain by hand:

```java
@ParameterizedTest(name = "accepts a moderate {0} shipment")
@EnumSource(CargoClass.class)
void acceptsEveryCargoClassAtAModerateWeight(CargoClass cargoClass) {
    ShipmentValidator validator = new ShipmentValidator();
    Shipment shipment = new Shipment(10.0, cargoClass, "SEA", 10_000L);

    assertDoesNotThrow(() -> validator.validate(shipment));
}
```

Adding a fifth `CargoClass` constant later extends this test
automatically. The same is not true of a `@ValueSource` list, which is
exactly why the weight cases above enumerate values by hand and the
cargo-class case does not.

`@CsvSource` fits a row that needs more than one primitive column, read
straight off a comma-separated line:

```java
@ParameterizedTest(name = "{0} at {1}kg costs {2} cents")
@CsvSource({
    "LOCAL, 2.0, 400",
    "REGIONAL, 2.0, 480",
    "NATIONAL, 2.0, 650"
})
void groundRateMatchesTheZoneTariff(DestinationZone zone, double weightKg, long expectedCents) {
    CarrierRateTable rateTable = CarrierRateTable.loadFromReferenceFile();

    assertThat(rateTable.rateFor(Carrier.GROUND, zone).cents()).isEqualTo(expectedCents);
}
```

Jupiter converts each column to the matching parameter type automatically,
including an enum constant from its name, so `DestinationZone zone` reads
`"LOCAL"` as `DestinationZone.LOCAL` with no explicit conversion code.

`@MethodSource` is the widest shape, for cases where each row needs more
than primitives and a name, such as a whole `Shipment` and an expected
reason:

```java
@ParameterizedTest(name = "{0}")
@MethodSource("invalidShipments")
void rejectsWithExpectedReason(String caseName, Shipment shipment, String expectedReasonFragment) {
    ShipmentValidator validator = new ShipmentValidator();

    InvalidShipmentException thrown =
            assertThrows(InvalidShipmentException.class, () -> validator.validate(shipment));

    assertThat(thrown.getMessage()).contains(expectedReasonFragment);
}

static Stream<Arguments> invalidShipments() {
    return Stream.of(
            Arguments.of("declared value over the insurable limit",
                    new Shipment(5.0, CargoClass.STANDARD, "SEA", 10_000_000L), "insurable limit"),
            Arguments.of("hazmat over the unescorted weight ceiling",
                    new Shipment(75.0, CargoClass.HAZMAT, "SEA", 10_000L), "freight approval"));
}
```

The `{0}` in the `name` attribute names each case from its own first
argument, so a failure reports "hazmat over the unescorted weight ceiling"
directly, not an index a reader has to count rows to decode. The payoff
every one of these four sources shares is the same: a fourth case is a
fourth row, never a fourth copy of the assertion body.

## `@Nested` and `@DisplayName`

`@Nested` groups related test methods inside an inner class, which turns
a flat list of test names into a structure a report can show as a
specification:

```java
class ShipmentValidatorTest {

    private final ShipmentValidator validator = new ShipmentValidator();

    @Nested
    @DisplayName("when the shipment is well-formed")
    class WhenWellFormed {

        @Test
        @DisplayName("accepts a standard shipment at a moderate weight")
        void acceptsAStandardShipment() {
            Shipment shipment = new Shipment(10.0, CargoClass.STANDARD, "SEA", 10_000L);

            assertDoesNotThrow(() -> validator.validate(shipment));
        }
    }

    @Nested
    @DisplayName("when the shipment breaks a business rule")
    class WhenMalformed {

        @Test
        @DisplayName("rejects a non-positive weight")
        void rejectsNonPositiveWeight() {
            Shipment shipment = new Shipment(0.0, CargoClass.STANDARD, "SEA", 10_000L);

            assertThrows(InvalidShipmentException.class, () -> validator.validate(shipment));
        }
    }
}
```

A non-static inner class carries an implicit reference to its enclosing
test instance, which is how `WhenWellFormed` and `WhenMalformed` both see
the same `validator` field without redeclaring it. `@DisplayName` on the
nested classes and the methods inside them turns a test run's output into
"ShipmentValidator, when the shipment breaks a business rule, rejects a
non-positive weight," a sentence about the component's contract rather
than a Java identifier a reader reverse-engineers into English.

## `@Tag`: grouping tests across classes

`@Tag` marks a test or a whole class with a label Surefire can include or
exclude by name, independent of which class or package the test lives in:

```java
@Tag("slow")
class CarrierRateTableIntegrationTest {

    @Test
    void loadsTheFullTariffTableFromTheReferenceFile() {
        CarrierRateTable rateTable = CarrierRateTable.loadFromReferenceFile();

        assertThat(rateTable.rateFor(Carrier.FREIGHT_ZONE, DestinationZone.INTERNATIONAL))
                .isNotNull();
    }
}
```

A tag groups tests by a concern the class hierarchy does not already
express, such as slow against fast, so a build can run a focused subset
(`mvn test -Dgroups=slow`) without touching the test source. This book's suites run untagged by default; a tag is worth adding
only once a suite is large enough that running every test on every change
stops being the right default.

## Dynamic tests: `@TestFactory`

A `@TestFactory` method returns a collection of `DynamicTest` instances
built at runtime, for cases where the set of tests is itself computed, not
known until the method runs:

```java
@TestFactory
Stream<DynamicTest> everyCarrierQuotesAPositiveAmount() {
    Shipment shipment = new Shipment(2.0, CargoClass.STANDARD, "SEA", 5_000L);

    return Stream.of(Carrier.values())
            .map(carrier -> DynamicTest.dynamicTest(
                    "quote from " + carrier,
                    () -> assertThat(carrier.quote(shipment).amount().cents()).isPositive()));
}
```

`@ParameterizedTest` is the right tool whenever the input values are known
up front, as a `@ValueSource` list, an enum, or a `@MethodSource` table.
`@TestFactory` only earns its extra ceremony, no declarative source
annotation, no automatic name templating, when the test count or the test
bodies themselves depend on something computed at runtime, such as a
carrier list read from a configuration file a `@TestFactory` method loads
before generating one `DynamicTest` per entry.

## Running through Maven Surefire

Every example's `pom.xml` depends on the JUnit Jupiter BOM, and the Maven
Surefire plugin runs the Jupiter engine during `mvn test`:

```sh
mvn -f examples/pom.xml test
```

Surefire discovers every class under `src/test/java` matching its default
naming convention (`*Test`, in this book's examples), hands each one to
the Jupiter engine, and reports one line per test method, not one line per
class, so a failure in `ShipmentValidatorTest.rejectsNonPositiveWeight`
is distinguishable in the console output from every other test in the
same class. An example is also runnable standalone from its own
directory with the same command run without the `-f` flag, from inside
`examples/<NN-pattern>`.

## For deeper coverage

- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/):
  the full lifecycle, extension model, and parameterized-test source
  vocabulary beyond the handful covered here.
- Gerard Meszaros, [*xUnit Test Patterns: Refactoring Test Code*](https://www.informit.com/store/xunit-test-patterns-refactoring-test-code-9780132800051)
  (Addison-Wesley): the four-phase test structure this book's test methods
  follow, setup, exercise, verify, and teardown, even where `@BeforeEach`
  and `@AfterEach` carry the setup and teardown phases implicitly.
- The full reading list behind this book's opinions lives on the
  [bibliography page]({{ '/bibliography/' | relative_url }}); each
  citation there is a pointer to the full book, not a substitute for
  reading it.
