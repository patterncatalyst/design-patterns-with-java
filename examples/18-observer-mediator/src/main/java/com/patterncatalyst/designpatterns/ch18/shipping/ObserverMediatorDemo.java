package com.patterncatalyst.designpatterns.ch18.shipping;

import com.patterncatalyst.designpatterns.ch18.shipping.dispatch.CarrierStation;
import com.patterncatalyst.designpatterns.ch18.shipping.dispatch.CustomerNotificationStation;
import com.patterncatalyst.designpatterns.ch18.shipping.dispatch.DispatchCoordinator;
import com.patterncatalyst.designpatterns.ch18.shipping.dispatch.WarehouseStation;
import com.patterncatalyst.designpatterns.ch18.shipping.tracking.AlertingService;
import com.patterncatalyst.designpatterns.ch18.shipping.tracking.DashboardView;
import com.patterncatalyst.designpatterns.ch18.shipping.tracking.ShipmentStatus;
import com.patterncatalyst.designpatterns.ch18.shipping.tracking.ShipmentTracker;
import com.patterncatalyst.designpatterns.ch18.shipping.tracking.Subscription;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Exercises both halves of this chapter's example against real output. */
public final class ObserverMediatorDemo {

    private ObserverMediatorDemo() {
    }

    public static void main(String[] args) {
        System.out.println("== Observer: named listeners fan out on a status change ==");
        ShipmentTracker tracker = new ShipmentTracker("TRK-7001");
        DashboardView dashboard = new DashboardView();
        AlertingService alerting = new AlertingService();
        tracker.subscribe(dashboard);
        tracker.subscribe(alerting);

        tracker.advanceStatus(ShipmentStatus.PICKED_UP);
        tracker.advanceStatus(ShipmentStatus.IN_TRANSIT);
        System.out.println("Dashboard now reads: " + dashboard.currentStatusLine());
        tracker.advanceStatus(ShipmentStatus.EXCEPTION);
        System.out.println("Dashboard now reads: " + dashboard.currentStatusLine());
        System.out.println("Alerts raised: " + alerting.alerts().size());
        alerting.alerts().forEach(alert -> System.out.println("  " + alert));

        System.out.println();
        System.out.println("== Observer: a lambda needs no listener class at all ==");
        ShipmentTracker secondTracker = new ShipmentTracker("TRK-7002");
        List<String> auditEntries = new ArrayList<>();
        Subscription auditSubscription = secondTracker.subscribe(
                (trackingId, oldStatus, newStatus) ->
                        auditEntries.add(trackingId + ": " + oldStatus + " -> " + newStatus));
        secondTracker.advanceStatus(ShipmentStatus.PICKED_UP);
        secondTracker.advanceStatus(ShipmentStatus.IN_TRANSIT);
        secondTracker.advanceStatus(ShipmentStatus.DELIVERED);
        auditEntries.forEach(System.out::println);
        auditSubscription.close();

        System.out.println();
        System.out.println("== Observer: closing a subscription stops notification mid-session ==");
        ShipmentTracker thirdTracker = new ShipmentTracker("TRK-7003");
        AtomicInteger callCount = new AtomicInteger();
        try (Subscription ignored = thirdTracker.subscribe(
                (trackingId, oldStatus, newStatus) -> callCount.incrementAndGet())) {
            thirdTracker.advanceStatus(ShipmentStatus.PICKED_UP);
        }
        thirdTracker.advanceStatus(ShipmentStatus.IN_TRANSIT);
        System.out.println("Callback ran " + callCount.get() + " time(s); subscriber_count() is now "
                + thirdTracker.subscriberCount());

        System.out.println();
        System.out.println("== Mediator: a warehouse event cascades through the coordinator ==");
        WarehouseStation warehouse = new WarehouseStation();
        CarrierStation carrier = new CarrierStation();
        CustomerNotificationStation notifier = new CustomerNotificationStation();
        new DispatchCoordinator(warehouse, carrier, notifier);

        warehouse.markOrderReady("ORD-5001");
        carrier.markOutForDelivery("ORD-5001");
        carrier.reportDeliveryException("ORD-5001");

        System.out.println("Carrier's own record of pickups: " + carrier.pickupCount());
        System.out.println("Messages the coordinator routed to the customer:");
        notifier.sentMessages().forEach(message -> System.out.println("  " + message));
    }
}
