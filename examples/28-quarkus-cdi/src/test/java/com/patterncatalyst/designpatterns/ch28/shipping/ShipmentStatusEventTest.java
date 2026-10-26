package com.patterncatalyst.designpatterns.ch28.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ShipmentStatusEventTest {

    @Inject
    CheckoutService checkoutService;

    @Inject
    DashboardNotifier dashboardNotifier;

    @Inject
    DeliveryAuditLog deliveryAuditLog;

    @Test
    void firingOneEventReachesEveryIndependentObserver() {
        checkoutService.dispatch("TRK-3001");

        assertThat(dashboardNotifier.currentStatusLine()).isEqualTo("TRK-3001: CREATED -> DISPATCHED");
        assertThat(deliveryAuditLog.entries())
                .extracting(ShipmentStatusChanged::trackingId)
                .contains("TRK-3001");
    }
}
