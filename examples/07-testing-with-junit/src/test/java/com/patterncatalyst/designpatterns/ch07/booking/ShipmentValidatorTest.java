package com.patterncatalyst.designpatterns.ch07.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("ShipmentValidator")
class ShipmentValidatorTest {

    private ShipmentValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ShipmentValidator();
    }

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

        @Test
        @DisplayName("rejects a zero declared value, checked with Assertions.assertThrows")
        void rejectsZeroDeclaredValueUsingAssertThrows() {
            Shipment shipment = new Shipment(5.0, CargoClass.STANDARD, "SEA", 0L);

            InvalidShipmentException thrown =
                    assertThrows(InvalidShipmentException.class, () -> validator.validate(shipment));

            assertThat(thrown.getMessage()).contains("declared value");
        }

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
}
