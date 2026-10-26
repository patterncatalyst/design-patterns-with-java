package com.patterncatalyst.designpatterns.ch18.shipping.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AlertingServiceTest {

    @Test
    void onlyRecordsTransitionsIntoException() {
        ShipmentTracker tracker = new ShipmentTracker("TRK-3001");
        AlertingService alerting = new AlertingService();
        tracker.subscribe(alerting);

        tracker.advanceStatus(ShipmentStatus.PICKED_UP);
        tracker.advanceStatus(ShipmentStatus.IN_TRANSIT);
        tracker.advanceStatus(ShipmentStatus.EXCEPTION);

        assertThat(alerting.alerts()).containsExactly("TRK-3001 flagged: IN_TRANSIT -> EXCEPTION");
    }
}
