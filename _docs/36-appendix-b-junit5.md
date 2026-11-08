---
title: "JUnit 5 reference"
order: 36
layout: tutorial
sectionid: docs
part: appendices
appendix: "B"
description: "Reference for the JUnit 5 annotations, lifecycle, assertions, parameterized and dynamic tests the examples use, and how Maven Surefire runs them."
duration: "15 min"
---

## Learning objectives

By the end of this appendix you can:

- Pick the JUnit 5 annotation for a case, a fixture, a table of inputs or a grouping.
- Say what each lifecycle method guarantees and when it runs.
- Choose a parameter source for a list, an enum, a CSV row or an object row.
- Run one class, one tag or the whole aggregator through Surefire.

## Why this matters

Every example's tests run on JUnit 5 (Jupiter), with AssertJ for assertions and Mockito for collaborators. Writing tests against a shipment booking service is covered in [Testing with JUnit 5, Mockito, and AssertJ]({{ '/docs/07-testing-with-junit/' | relative_url }}); Mockito has its own [reference]({{ '/docs/37-appendix-c-mockito/' | relative_url }}). This page is the lookup table. The listings come from the booking example: a `ShipmentValidator`, a `FreightCostCalculator` and an `InMemoryRateLedger`.

## Annotations at a glance

| Annotation | Guarantee | Use it when |
|---|---|---|
| `@Test` | A no-argument method runs as one case, on a fresh instance of the class | Every single case |
| `@BeforeEach` / `@AfterEach` | Runs before and after every test; `@AfterEach` runs even when a test fails | A fixture rebuilt per test, or a postcondition every test must hold |
| `@BeforeAll` / `@AfterAll` | Runs once per class, on a `static` method (unless the class is `@TestInstance(Lifecycle.PER_CLASS)`) | Expensive setup that every test only reads |
| `@ParameterizedTest` | One method runs once per row a source supplies | Several inputs share one assertion |
| `@Nested` | An inner class groups tests and sees the outer instance's fields | Cases that share a precondition |
| `@DisplayName` | Replaces the method or class name in reports | Reports that read as a specification |
| `@Tag` | Labels a test or class for include or exclude | Splitting slow tests from fast ones |
| `@TestFactory` | Returns `DynamicTest` instances built at run time | The set of cases is computed, not known up front |

## `@Test` and a fresh instance

Jupiter creates a new instance of the class for every `@Test` method, so a field initialized inline never carries state from one test to the next, and no test depends on discovery order:

```java
@DisplayName("FreightCostCalculator")
class FreightCostCalculatorTest {

    private final FreightCostCalculator calculator = new FreightCostCalculator();

    @Test
    @DisplayName("prices a standard shipment as base plus per-kilogram rate")
    void pricesAStandardShipment() {
        Shipment shipment = new Shipment(3.5, CargoClass.STANDARD, "SEA", 10_000L);

        Money amount = calculator.cost(shipment);

        assertThat(amount)
                .isEqualTo(Money.ofCents(150L + Math.round(120L * 3.5)))
                .isEqualTo(Money.ofCents(570L));
    }
}
```

## Lifecycle

`@BeforeEach` rebuilds the fixture before each test. `@AfterEach` runs after each one whether it passed or failed, which makes it the place for an invariant every test must leave intact. A check at the bottom of a test body is skipped when an earlier assertion fails:

```java
@DisplayName("InMemoryRateLedger")
class InMemoryRateLedgerTest {

    private static final int CAPACITY = 2;

    private InMemoryRateLedger ledger;

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

    @Test
    @DisplayName("reports full capacity remaining when empty")
    void reportsFullCapacityWhenEmpty() {
        assertThat(ledger.capacityRemaining()).isEqualTo(CAPACITY);
    }
}
```

Use `@BeforeAll` only for setup that is expensive and read-only for the whole class, such as loading a reference table from a file. A field any test mutates belongs in `@BeforeEach`, so one test's mutation can't reach the next. No example needs `@BeforeAll`; each fixture is cheap to rebuild.

## Assertions

| Method | Checks |
|---|---|
| `assertEquals(expected, actual)` | equality |
| `assertTrue(condition)` / `assertFalse(condition)` | a boolean condition |
| `assertNotNull(value)` / `assertNull(value)` | nullity |
| `assertThrows(type, executable)` | the call throws `type` or a subtype, and returns the exception |
| `assertAll(executables...)` | runs every executable and reports every failure together |

`assertThrows` returns what it caught, so the next line can check the message without a try/catch:

```java
@Test
@DisplayName("rejects a null cargo class")
void rejectsNullCargoClass() {
    NullPointerException thrown = assertThrows(
            NullPointerException.class, () -> new Shipment(3.5, null, "SEA", 10_000L));

    assertThat(thrown.getMessage()).contains("cargoClass");
}
```

A run of plain `assertEquals` calls stops at the first failure; `assertAll` reports all of them. AssertJ's `assertThat(...).extracting(...)` and chained assertions cover the same need for facts read off one object, and the examples use AssertJ for it. Either beats one `assertTrue` over several `&&` conditions, which reports only `false`.

## Parameterized tests

A `@ParameterizedTest` runs once per row, so a new case is a new row, never a copy of the method body. The `name` attribute puts the row's values in the report.

| Source | Supplies | Example |
|---|---|---|
| `@ValueSource` | A list of literals of one type | Three non-positive weights |
| `@EnumSource` | Every constant of an enum | Every `CargoClass` |
| `@CsvSource` | Rows of several columns, converted to the parameter types | A zone and its legacy code |
| `@MethodSource` | A `static` factory returning `Stream<Arguments>` | A `Shipment` and the expected reason |

`@ValueSource` and `@EnumSource`, inside `@Nested` classes:

```java
@Nested
@DisplayName("when the shipment is well-formed")
class WhenWellFormed {

    @ParameterizedTest(name = "validates a {0} shipment")
    @EnumSource(CargoClass.class)
    @DisplayName("accepts every cargo class at a moderate weight")
    void validatesEveryCargoClass(CargoClass cargoClass) {
        Shipment shipment = new Shipment(10.0, cargoClass, "SEA", 10_000L);

        assertThatCode(() -> validator.validate(shipment)).doesNotThrowAnyException();
    }
}

@Nested
@DisplayName("when the shipment breaks a business rule")
class WhenMalformed {

    @ParameterizedTest(name = "rejects a weight of {0}kg")
    @ValueSource(doubles = {0.0, -1.0, -42.5})
    @DisplayName("rejects a non-positive weight")
    void rejectsNonPositiveWeight(double weightKg) {
        Shipment shipment = new Shipment(weightKg, CargoClass.STANDARD, "SEA", 10_000L);

        assertThatThrownBy(() -> validator.validate(shipment))
                .isInstanceOf(InvalidShipmentException.class)
                .hasMessageContaining("weight must be positive");
    }
}
```

`@EnumSource` grows with the enum: a fifth `CargoClass` constant adds a case with no edit to the method. A `@ValueSource` list doesn't.

`@CsvSource` converts each column to the parameter type, including an enum constant from its name, so `"LOCAL"` arrives as `DestinationZone.LOCAL`:

```java
@ParameterizedTest
@CsvSource({
    "LOCAL, 0",
    "REGIONAL, 1",
    "NATIONAL, 2",
    "INTERNATIONAL, 3",
})
void mapsEveryDestinationZoneToTheLegacyNumericCode(DestinationZone zone, int expectedZoneCode) {
    when(legacyClient.rateCentsForParcel(1_000L, expectedZoneCode)).thenReturn(300L);
    GlobalCourierGatewayAdapter adapter = new GlobalCourierGatewayAdapter(legacyClient);
    Shipment shipment = new Shipment(1.0, zone, 0);

    adapter.quote(shipment);

    verify(legacyClient).rateCentsForParcel(1_000L, expectedZoneCode);
}
```

`@MethodSource` takes rows of any type. With `name = "{0}"`, the first argument names each case, so a failure reports "hazmat over the unescorted weight ceiling" instead of a row index. A test inside a `@Nested` class names a factory in the outer class by its fully qualified `Class#method` form:

```java
@ParameterizedTest(name = "{0}")
@MethodSource("com.patterncatalyst.designpatterns.ch07.booking.ShipmentValidatorTest#invalidShipments")
@DisplayName("rejects each case with the matching reason")
void rejectsWithExpectedReason(String caseName, Shipment shipment, String expectedReasonFragment) {
    assertThatThrownBy(() -> validator.validate(shipment))
            .isInstanceOf(InvalidShipmentException.class)
            .hasMessageContaining(expectedReasonFragment);
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

## `@Nested` and `@DisplayName`

A non-static inner class keeps a reference to its enclosing test instance, so `WhenWellFormed` and `WhenMalformed` above both use the outer `validator` field. `@DisplayName` on the classes and methods makes the report read as a sentence: "ShipmentValidator, when the shipment breaks a business rule, rejects a non-positive weight".

## `@Tag` and `@TestFactory`

| Feature | Form | Use it when |
|---|---|---|
| `@Tag("slow")` on a class or method | Select with `mvn test -Dgroups=slow`, exclude with `-DexcludedGroups=slow` | A suite is large enough that running everything on every change stops being the right default |
| `@TestFactory` returning `Stream<DynamicTest>` | Each `DynamicTest.dynamicTest(name, executable)` is one case | The cases depend on something computed at run time, such as entries read from a file |

The examples run untagged and use no `@TestFactory`: every input set is known up front, which is what `@ParameterizedTest` is for. A `@TestFactory` gives up the declarative source and the name template.

## Running through Surefire

The aggregator imports the JUnit BOM, and Surefire runs the Jupiter engine during `mvn test`. It picks up classes under `src/test/java` whose names match its default patterns (the examples all end in `Test`) and prints a count for each class and a total for the module.

| Run | Command |
|---|---|
| Every module | `mvn -f examples/pom.xml test` |
| One example | `cd examples/07-testing-with-junit && mvn test` |
| One class | `mvn test -Dtest=ShipmentValidatorTest` |
| One tag | `mvn test -Dgroups=slow` |

## For deeper coverage

- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/):
  the full lifecycle, extension model, and parameterized-test sources.
- Gerard Meszaros, [*xUnit Test Patterns: Refactoring Test Code*](https://www.informit.com/store/xunit-test-patterns-refactoring-test-code-9780132800051)
  (Addison-Wesley): the four-phase test (setup, exercise, verify, teardown)
  that every test method follows, with `@BeforeEach` and `@AfterEach`
  carrying setup and teardown when a class shares them.
- The full reading list is on the
  [bibliography page]({{ '/bibliography/' | relative_url }}).
