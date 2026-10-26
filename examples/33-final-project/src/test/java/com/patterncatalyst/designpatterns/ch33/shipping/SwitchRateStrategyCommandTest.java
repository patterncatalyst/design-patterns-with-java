package com.patterncatalyst.designpatterns.ch33.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SwitchRateStrategyCommandTest {

    private Shipment newShipment() {
        return new ShipmentBuilder()
                .id("SHIP-SWAP")
                .carrier("GROUND")
                .destinationZone(DestinationZone.REGIONAL)
                .weightKg(8.0)
                .build();
    }

    @Test
    void executeReplacesTheStrategyAndRequotesAgainstIt() {
        Shipment shipment = newShipment();
        Money groundQuote = shipment.quotedRate();
        RateStrategy expedited = new WeightTieredRateStrategy("ground-expedited", 900, 260);

        new SwitchRateStrategyCommand(shipment, expedited).execute();

        assertThat(shipment.rateStrategy()).isSameAs(expedited);
        assertThat(shipment.quotedRate()).isNotEqualTo(groundQuote);
        assertThat(shipment.quotedRate()).isEqualTo(expedited.quote(shipment));
    }

    @Test
    void undoRestoresThePreviousStrategyAndItsLastQuoteExactly() {
        Shipment shipment = newShipment();
        RateStrategy originalStrategy = shipment.rateStrategy();
        Money originalQuote = shipment.quotedRate();
        RateStrategy expedited = new WeightTieredRateStrategy("ground-expedited", 900, 260);

        SwitchRateStrategyCommand command = new SwitchRateStrategyCommand(shipment, expedited);
        command.execute();
        command.undo();

        assertThat(shipment.rateStrategy()).isSameAs(originalStrategy);
        assertThat(shipment.quotedRate()).isEqualTo(originalQuote);
    }

    @Test
    void aFlatAndAWeightTieredStrategyAreInterchangeableBehindTheSameCommand() {
        Shipment shipment = newShipment();
        RateStrategy flat = new FlatRateStrategy("courier-x", 2_500);

        new SwitchRateStrategyCommand(shipment, flat).execute();

        assertThat(shipment.quotedRate()).isEqualTo(Money.ofCents(2_500));
    }
}
