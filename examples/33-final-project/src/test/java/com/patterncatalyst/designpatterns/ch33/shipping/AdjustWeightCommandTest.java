package com.patterncatalyst.designpatterns.ch33.shipping;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdjustWeightCommandTest {

    private Shipment newShipment() {
        return new ShipmentBuilder()
                .id("SHIP-WEIGHT")
                .carrier("GROUND")
                .destinationZone(DestinationZone.REGIONAL)
                .weightKg(8.0)
                .build();
    }

    @Test
    void executeSetsTheNewWeightAndRequotesThroughTheShipmentsStrategy() {
        Shipment shipment = newShipment();
        Money openingQuote = shipment.quotedRate();

        new AdjustWeightCommand(shipment, 15.0).execute();

        assertThat(shipment.weightKg()).isEqualTo(15.0);
        assertThat(shipment.quotedRate()).isNotEqualTo(openingQuote);
        assertThat(shipment.quotedRate()).isEqualTo(shipment.rateStrategy().quote(shipment));
    }

    @Test
    void undoRestoresTheExactCapturedWeightAndRateWithoutRecomputing() {
        AtomicInteger quoteCalls = new AtomicInteger();
        RateStrategy countingStrategy = s -> {
            quoteCalls.incrementAndGet();
            return Money.ofCents(1_000 + (long) (s.weightKg() * 10));
        };
        Shipment shipment = new ShipmentBuilder()
                .id("SHIP-COUNT")
                .carrier("GROUND")
                .destinationZone(DestinationZone.REGIONAL)
                .weightKg(8.0)
                .rateStrategy(countingStrategy)
                .build();
        double openingWeight = shipment.weightKg();
        Money openingQuote = shipment.quotedRate();
        quoteCalls.set(0);

        AdjustWeightCommand command = new AdjustWeightCommand(shipment, 15.0);
        command.execute();
        assertThat(quoteCalls.get()).isEqualTo(1);

        command.undo();
        assertThat(quoteCalls.get()).isEqualTo(1);
        assertThat(shipment.weightKg()).isEqualTo(openingWeight);
        assertThat(shipment.quotedRate()).isEqualTo(openingQuote);
    }

    @Test
    void executeThenUndoRoundTripsBackToTheOriginalState() {
        Shipment shipment = newShipment();
        double originalWeight = shipment.weightKg();
        Money originalQuote = shipment.quotedRate();

        AdjustWeightCommand command = new AdjustWeightCommand(shipment, 22.5);
        command.execute();
        command.undo();

        assertThat(shipment.weightKg()).isEqualTo(originalWeight);
        assertThat(shipment.quotedRate()).isEqualTo(originalQuote);
    }
}
