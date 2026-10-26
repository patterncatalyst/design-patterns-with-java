package com.patterncatalyst.designpatterns.ch30.shipping;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Subclass and Override Method: the inherited eligibility check read
 * {@code LocalDate.now()} inline, so no test could ever pin an exact
 * answer. Overriding {@code today()} in a small test-only subclass gives a
 * known date with no change to the weekday rule itself.
 */
@ExtendWith(MockitoExtension.class)
class LegacyShipmentProcessorExpressLaneTest {

    @Mock
    private NotificationGateway notificationGateway;

    private static final class FixedDateShipmentProcessor extends LegacyShipmentProcessor {
        private final LocalDate fixedToday;

        FixedDateShipmentProcessor(NotificationGateway notificationGateway, LocalDate fixedToday) {
            super("ops-supervisor@example.com", notificationGateway);
            this.fixedToday = fixedToday;
        }

        @Override
        protected LocalDate today() {
            return fixedToday;
        }
    }

    @Test
    void aTuesdayDispatchIsExpressLaneEligible() {
        LegacyShipmentProcessor processor = new FixedDateShipmentProcessor(
                notificationGateway, LocalDate.of(2026, Month.OCTOBER, 20));

        ShipmentDispatchResult result = processor.dispatch(new Shipment("SH-8001", 4_000));

        assertThat(result.expressLaneEligible()).isTrue();
    }

    @Test
    void aSaturdayDispatchIsNotExpressLaneEligible() {
        LegacyShipmentProcessor processor = new FixedDateShipmentProcessor(
                notificationGateway, LocalDate.of(2026, Month.OCTOBER, 24));

        ShipmentDispatchResult result = processor.dispatch(new Shipment("SH-8002", 4_000));

        assertThat(result.expressLaneEligible()).isFalse();
    }

    @Test
    void aSundayDispatchIsNotExpressLaneEligible() {
        LegacyShipmentProcessor processor = new FixedDateShipmentProcessor(
                notificationGateway, LocalDate.of(2026, Month.OCTOBER, 25));

        ShipmentDispatchResult result = processor.dispatch(new Shipment("SH-8003", 4_000));

        assertThat(result.expressLaneEligible()).isFalse();
    }
}
