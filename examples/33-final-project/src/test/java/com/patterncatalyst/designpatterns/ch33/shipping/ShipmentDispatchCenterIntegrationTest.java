package com.patterncatalyst.designpatterns.ch33.shipping;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * The example's actual point. Every other test file in this module checks
 * one pattern's own contract in isolation; this one drives the whole
 * {@link ShipmentDispatchCenter} through a single shipment's run and
 * confirms the five composed patterns cooperate correctly, not just that
 * each one works alone.
 */
@ExtendWith(MockitoExtension.class)
class ShipmentDispatchCenterIntegrationTest {

    @Mock
    private ShipmentStatusListener listener;

    @Test
    void aShipmentFlowsThroughBuilderStateCommandObserverAndStrategyAsOneSystem() {
        // Builder resolves a RateStrategy and produces an opening quote.
        Shipment shipment = new ShipmentBuilder()
                .id("SHIP-7001")
                .carrier("GROUND")
                .destinationZone(DestinationZone.REGIONAL)
                .weightKg(8.0)
                .build();
        Money openingQuote = shipment.quotedRate();
        assertThat(openingQuote).isEqualTo(shipment.rateStrategy().quote(shipment));

        ShipmentDispatchCenter center = new ShipmentDispatchCenter(shipment);
        try (Subscription ignored = center.subscribe(listener)) {

            // Command drives State; State fires Observer on each legal move.
            center.advance(ShipmentEvent.PICK_UP);
            center.advance(ShipmentEvent.DISPATCH);
            assertThat(shipment.status()).isEqualTo(ShipmentStatus.IN_TRANSIT);

            // Command invokes Strategy: a weight correction recomputes the quote.
            center.adjustWeight(15.0);
            Money correctedQuote = shipment.quotedRate();
            assertThat(shipment.weightKg()).isEqualTo(15.0);
            assertThat(correctedQuote).isNotEqualTo(openingQuote);

            // Undo restores the exact pre-correction weight and quote.
            assertThat(center.undoLast()).isTrue();
            assertThat(shipment.weightKg()).isEqualTo(8.0);
            assertThat(shipment.quotedRate()).isEqualTo(openingQuote);

            // The rate Strategy is swappable with the same result contract.
            RateStrategy premiumStrategy = new FlatRateStrategy("premium-ground", 5_000);
            center.switchRateStrategy(premiumStrategy);
            assertThat(shipment.rateStrategy()).isSameAs(premiumStrategy);
            assertThat(shipment.quotedRate()).isEqualTo(Money.ofCents(5_000));

            assertThat(center.undoLast()).isTrue();
            assertThat(shipment.quotedRate()).isEqualTo(openingQuote);

            // Deliver, then confirm State rejects a repeated event afterward.
            center.advance(ShipmentEvent.DELIVER);
            assertThat(shipment.status()).isEqualTo(ShipmentStatus.DELIVERED);

            assertThatThrownBy(() -> center.advance(ShipmentEvent.PICK_UP))
                    .isInstanceOf(IllegalShipmentTransitionException.class);
        }

        InOrder order = inOrder(listener);
        order.verify(listener).onStatusChanged("SHIP-7001", ShipmentStatus.CREATED, ShipmentStatus.PICKED_UP);
        order.verify(listener).onStatusChanged("SHIP-7001", ShipmentStatus.PICKED_UP, ShipmentStatus.IN_TRANSIT);
        order.verify(listener).onStatusChanged("SHIP-7001", ShipmentStatus.IN_TRANSIT, ShipmentStatus.DELIVERED);
        verify(listener, times(3)).onStatusChanged(anyString(), any(), any());
        verifyNoMoreInteractions(listener);
    }

    @Test
    void undoAndRedoStepThroughTwoLifecycleAdvancesInOppositeOrders() {
        Shipment shipment = new ShipmentBuilder()
                .id("SHIP-7002")
                .carrier("AIR")
                .destinationZone(DestinationZone.INTERNATIONAL)
                .weightKg(2.0)
                .build();
        ShipmentDispatchCenter center = new ShipmentDispatchCenter(shipment);

        center.advance(ShipmentEvent.PICK_UP);
        center.advance(ShipmentEvent.DISPATCH);
        assertThat(shipment.status()).isEqualTo(ShipmentStatus.IN_TRANSIT);

        assertThat(center.undoLast()).isTrue();
        assertThat(shipment.status()).isEqualTo(ShipmentStatus.PICKED_UP);

        assertThat(center.undoLast()).isTrue();
        assertThat(shipment.status()).isEqualTo(ShipmentStatus.CREATED);

        assertThat(center.redoLast()).isTrue();
        assertThat(shipment.status()).isEqualTo(ShipmentStatus.PICKED_UP);

        assertThat(center.redoLast()).isTrue();
        assertThat(shipment.status()).isEqualTo(ShipmentStatus.IN_TRANSIT);
    }
}
