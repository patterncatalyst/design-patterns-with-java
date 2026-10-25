package com.patterncatalyst.designpatterns.ch07.booking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// No @Mock anywhere in this file. FreightCostCalculator takes no
// collaborator and has no side effect, so there is nothing here for
// Mockito to stand in for; calling the real method and checking what it
// returns is both the simplest test and the one closest to what a caller
// actually depends on.
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

    @Test
    @DisplayName("prices a hazmat shipment higher than a standard one of the same weight")
    void hazmatCostsMoreThanStandardAtTheSameWeight() {
        Shipment standard = new Shipment(5.0, CargoClass.STANDARD, "SEA", 10_000L);
        Shipment hazmat = new Shipment(5.0, CargoClass.HAZMAT, "SEA", 10_000L);

        assertThat(calculator.cost(hazmat).cents()).isGreaterThan(calculator.cost(standard).cents());
    }

    @ParameterizedTest(name = "a heavier {0} shipment never costs less than a lighter one")
    @EnumSource(CargoClass.class)
    @DisplayName("cost rises monotonically with weight, for every cargo class")
    void costRisesWithWeight(CargoClass cargoClass) {
        Shipment light = new Shipment(1.0, cargoClass, "SEA", 10_000L);
        Shipment heavy = new Shipment(20.0, cargoClass, "SEA", 10_000L);

        assertThat(calculator.cost(heavy).cents()).isGreaterThan(calculator.cost(light).cents());
    }

    @Test
    @DisplayName("a zero-weight shipment costs exactly the base rate")
    void zeroWeightCostsExactlyTheBaseRate() {
        Shipment shipment = new Shipment(0.0, CargoClass.OVERSIZED, "SEA", 10_000L);

        assertThat(calculator.cost(shipment)).isEqualTo(Money.ofCents(650L));
    }
}
