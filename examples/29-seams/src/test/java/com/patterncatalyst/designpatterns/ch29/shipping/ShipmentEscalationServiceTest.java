package com.patterncatalyst.designpatterns.ch29.shipping;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ShipmentEscalationServiceTest {

    private static final Duration OVERDUE_GRACE = Duration.ofHours(2);

    @Mock
    private CarrierGateway gateway;

    @Nested
    class SeamAlonePreservesTheLegacyWideMarginBehavior {

        private final LegacyShipmentEscalationService legacy = new LegacyShipmentEscalationService();

        @Test
        void wideMarginCasesAgreeWithTheLegacyService() {
            ShipmentEscalationService seamed = new ShipmentEscalationService(Clock.systemUTC(), gateway);
            Shipment overdue = new Shipment("SHM-2001", Instant.now().minus(Duration.ofDays(1)), 10.0);
            Shipment onTime = new Shipment("SHM-2002", Instant.now().plus(Duration.ofDays(1)), 10.0);

            assertThat(seamed.escalate(overdue).escalated())
                    .isEqualTo(legacy.escalate(overdue).escalated());
            assertThat(seamed.escalate(onTime).escalated())
                    .isEqualTo(legacy.escalate(onTime).escalated());
        }
    }

    /**
     * Clock.fixed makes the exact boundary this chapter's legacy
     * characterization tests could never reach, reachable on demand: a
     * deadline exactly at the grace-period boundary, one nanosecond before
     * it, and one nanosecond after it, each an instant, flake-proof test
     * instead of a case nobody could even write before the seam existed.
     */
    @Nested
    class ExactBoundaryOnceTheClockIsASeam {

        @Test
        void aDeadlineExactlyAtTheGracePeriodBoundaryIsEscalated() {
            Instant overdueAt = Instant.parse("2026-01-10T12:00:00Z");
            Instant promised = overdueAt.minus(OVERDUE_GRACE);
            ShipmentEscalationService service =
                    new ShipmentEscalationService(Clock.fixed(overdueAt, ZoneOffset.UTC), gateway);
            Shipment shipment = new Shipment("SHM-2101", promised, 10.0);

            EscalationResult result = service.escalate(shipment);

            assertThat(result.escalated()).isTrue();
            verify(gateway).notifyCarrier(eq(shipment), any());
        }

        @Test
        void oneNanosecondBeforeTheBoundaryIsNotEscalated() {
            Instant overdueAt = Instant.parse("2026-01-10T12:00:00Z");
            Instant promised = overdueAt.minus(OVERDUE_GRACE);
            ShipmentEscalationService service = new ShipmentEscalationService(
                    Clock.fixed(overdueAt.minusNanos(1), ZoneOffset.UTC), gateway);
            Shipment shipment = new Shipment("SHM-2102", promised, 10.0);

            EscalationResult result = service.escalate(shipment);

            assertThat(result.escalated()).isFalse();
            verify(gateway, never()).notifyCarrier(any(), any());
        }

        @Test
        void oneNanosecondAfterTheBoundaryIsEscalated() {
            Instant overdueAt = Instant.parse("2026-01-10T12:00:00Z");
            Instant promised = overdueAt.minus(OVERDUE_GRACE);
            ShipmentEscalationService service = new ShipmentEscalationService(
                    Clock.fixed(overdueAt.plusNanos(1), ZoneOffset.UTC), gateway);
            Shipment shipment = new Shipment("SHM-2103", promised, 10.0);

            EscalationResult result = service.escalate(shipment);

            assertThat(result.escalated()).isTrue();
            verify(gateway).notifyCarrier(eq(shipment), any());
        }
    }
}
