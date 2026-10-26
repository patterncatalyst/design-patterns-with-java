package com.patterncatalyst.designpatterns.ch18.shipping.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link DispatchCoordinator} is the Mediator: a colleague raises an event
 * and the coordinator alone decides which other colleague responds. These
 * tests substitute Mockito doubles for the downstream colleagues to prove
 * that routing, not a direct reference between stations, is what makes the
 * cascade happen.
 */
@ExtendWith(MockitoExtension.class)
class DispatchCoordinatorTest {

    @Mock
    private CarrierStation carrier;

    @Mock
    private CustomerNotificationStation notifier;

    @Test
    void anOrderReadyEventIsRoutedToTheCarrierAndNowhereElse() {
        WarehouseStation warehouse = new WarehouseStation();
        new DispatchCoordinator(warehouse, carrier, notifier);

        warehouse.markOrderReady("ORD-9001");

        verify(carrier).confirmPickup("ORD-9001");
        verify(notifier, never()).send(anyString(), anyString());
    }

    @Test
    void theWarehouseNeverReceivesACallBackFromTheCoordinator() {
        WarehouseStation warehouse = new WarehouseStation();
        DispatchCoordinator coordinator = new DispatchCoordinator(warehouse, carrier, notifier);

        warehouse.markOrderReady("ORD-9002");

        assertThat(coordinator.warehouse()).isSameAs(warehouse);
        // Only the carrier is ever called in response to a warehouse event;
        // the warehouse itself has no method the coordinator could call back
        // on, by construction (DispatchColleague exposes raise() only).
        verify(carrier).confirmPickup("ORD-9002");
    }

    @Test
    void aColleagueWithNoMediatorBoundYetDoesNotThrow() {
        WarehouseStation standaloneWarehouse = new WarehouseStation();

        standaloneWarehouse.markOrderReady("ORD-9003");
    }
}
