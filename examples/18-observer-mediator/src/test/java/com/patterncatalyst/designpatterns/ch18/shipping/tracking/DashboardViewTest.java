package com.patterncatalyst.designpatterns.ch18.shipping.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DashboardViewTest {

    @Test
    void reflectsTheLatestNotificationRatherThanAHistory() {
        ShipmentTracker tracker = new ShipmentTracker("TRK-2001");
        DashboardView dashboard = new DashboardView();
        tracker.subscribe(dashboard);

        tracker.advanceStatus(ShipmentStatus.PICKED_UP);
        tracker.advanceStatus(ShipmentStatus.IN_TRANSIT);

        assertThat(dashboard.currentStatusLine()).isEqualTo("TRK-2001: IN_TRANSIT");
    }
}
