package com.patterncatalyst.designpatterns.ch33.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentBuilderTest {

    @Test
    void resolvesAKnownCarriersStrategyAndComputesAnOpeningQuote() {
        Shipment shipment = new ShipmentBuilder()
                .id("SHIP-1")
                .carrier("GROUND")
                .destinationZone(DestinationZone.REGIONAL)
                .weightKg(8.0)
                .build();

        assertThat(shipment.carrierCode()).isEqualTo("GROUND");
        assertThat(shipment.weightKg()).isEqualTo(8.0);
        assertThat(shipment.quotedRate()).isEqualTo(Money.ofCents(1_940));
    }

    @Test
    void flatCarrierIgnoresWeightWhenQuoting() {
        Shipment shipment = new ShipmentBuilder()
                .id("SHIP-2")
                .carrier("AIR")
                .destinationZone(DestinationZone.NATIONAL)
                .weightKg(50.0)
                .build();

        assertThat(shipment.quotedRate()).isEqualTo(Money.ofCents(1_999));
    }

    @Test
    void customRateStrategyOverridesTheKnownCarrierLookup() {
        RateStrategy contractRate = s -> Money.ofCents(777);

        Shipment shipment = new ShipmentBuilder()
                .id("SHIP-3")
                .carrier("GROUND")
                .destinationZone(DestinationZone.LOCAL)
                .weightKg(2.0)
                .rateStrategy(contractRate)
                .build();

        assertThat(shipment.quotedRate()).isEqualTo(Money.ofCents(777));
        assertThat(shipment.rateStrategy()).isSameAs(contractRate);
    }

    @Test
    void unknownCarrierWithNoOverrideStrategyIsRejected() {
        ShipmentBuilder builder = new ShipmentBuilder()
                .id("SHIP-4")
                .carrier("SPACEX")
                .destinationZone(DestinationZone.INTERNATIONAL)
                .weightKg(1.0);

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SPACEX");
    }

    @Test
    void missingRequiredFieldIsRejected() {
        ShipmentBuilder builder = new ShipmentBuilder()
                .carrier("GROUND")
                .destinationZone(DestinationZone.LOCAL)
                .weightKg(1.0);

        assertThatThrownBy(builder::build).isInstanceOf(NullPointerException.class);
    }
}
