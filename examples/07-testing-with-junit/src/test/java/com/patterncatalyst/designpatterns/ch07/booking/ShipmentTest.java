package com.patterncatalyst.designpatterns.ch07.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Shipment")
class ShipmentTest {

    @Test
    @DisplayName("rejects a null cargo class")
    void rejectsNullCargoClass() {
        NullPointerException thrown = assertThrows(
                NullPointerException.class, () -> new Shipment(3.5, null, "SEA", 10_000L));

        assertThat(thrown.getMessage()).contains("cargoClass");
    }

    @Test
    @DisplayName("rejects a blank destination code")
    void rejectsBlankDestinationCode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Shipment(3.5, CargoClass.STANDARD, "  ", 10_000L));
    }

    @Test
    @DisplayName("allows a non-positive weight through construction, on purpose")
    void allowsNonPositiveWeightThroughConstruction() {
        // ShipmentValidator, not the record, owns the business rule that a
        // weight must be positive. Without this permissiveness there would
        // be no way to build the malformed-but-structurally-valid shipment
        // ShipmentValidatorTest needs to prove the rule is actually checked.
        Shipment shipment = new Shipment(-1.0, CargoClass.STANDARD, "SEA", 10_000L);

        assertThat(shipment.weightKg()).isNegative();
    }
}
