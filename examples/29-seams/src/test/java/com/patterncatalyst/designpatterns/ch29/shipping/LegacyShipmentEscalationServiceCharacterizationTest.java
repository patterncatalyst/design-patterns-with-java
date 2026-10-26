package com.patterncatalyst.designpatterns.ch29.shipping;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Characterization tests: recorded observations of what
 * LegacyShipmentEscalationService does right now, not assertions about what
 * it should do. Both cases use a wide margin, a full day, because
 * escalate() reads Instant.now() a second time inside its own body, a few
 * microseconds after this test computes its own Instant.now() to build the
 * promised-delivery timestamp. A margin that small would make the test
 * exercise scheduling jitter instead of the service's own logic.
 */
class LegacyShipmentEscalationServiceCharacterizationTest {

    private final LegacyShipmentEscalationService legacy = new LegacyShipmentEscalationService();

    @Test
    void aShipmentPromisedADayAgoIsEscalated() {
        Shipment overdue = new Shipment("SHM-1001", Instant.now().minus(Duration.ofDays(1)), 10.0);

        EscalationResult result = legacy.escalate(overdue);

        assertThat(result.escalated()).isTrue();
        assertThat(result.reason()).contains("promised delivery window missed");
    }

    @Test
    void aShipmentPromisedADayFromNowIsNotEscalated() {
        Shipment onTime = new Shipment("SHM-1002", Instant.now().plus(Duration.ofDays(1)), 10.0);

        EscalationResult result = legacy.escalate(onTime);

        assertThat(result.escalated()).isFalse();
    }

    /**
     * This is as far as a characterization test can reach without a seam.
     * escalate() constructs its own SmtpCarrierGateway inside the method, so
     * there is no gateway reference this test can hold onto and verify an
     * interaction against. The EscalationResult's reason string is the only
     * observable proxy for "a notification happened"; ShipmentEscalationServiceTest
     * is where the opened seam lets a test verify the interaction directly.
     */
    @Test
    void anEscalationCarriesAReasonButTheNotificationItselfIsUnobservable() {
        Shipment overdue = new Shipment("SHM-1003", Instant.now().minus(Duration.ofDays(1)), 10.0);

        EscalationResult result = legacy.escalate(overdue);

        assertThat(result.reason()).isNotBlank();
    }
}
