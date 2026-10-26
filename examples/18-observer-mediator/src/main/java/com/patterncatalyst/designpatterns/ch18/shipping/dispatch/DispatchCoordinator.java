package com.patterncatalyst.designpatterns.ch18.shipping.dispatch;

import java.util.Objects;

/**
 * The hub that binds a warehouse, a carrier, and a customer-notification
 * station together without any of the three ever holding a reference to
 * another. Binding happens here, in the constructor, after all three
 * colleagues already exist, which breaks the circular requirement a
 * Mediator always has to solve: every colleague needs a mediator, and the
 * mediator needs live colleagues to route to.
 */
public final class DispatchCoordinator implements DispatchMediator {

    private final WarehouseStation warehouse;
    private final CarrierStation carrier;
    private final CustomerNotificationStation notifier;

    public DispatchCoordinator(WarehouseStation warehouse, CarrierStation carrier,
            CustomerNotificationStation notifier) {
        this.warehouse = Objects.requireNonNull(warehouse, "warehouse");
        this.carrier = Objects.requireNonNull(carrier, "carrier");
        this.notifier = Objects.requireNonNull(notifier, "notifier");
        warehouse.bindMediator(this);
        carrier.bindMediator(this);
        notifier.bindMediator(this);
    }

    @Override
    public void notify(DispatchColleague sender, DispatchEvent event, String orderId) {
        switch (event) {
            case ORDER_READY_FOR_PICKUP -> carrier.confirmPickup(orderId);
            case PICKED_UP_BY_CARRIER ->
                notifier.send(orderId, "Your package has been picked up and is on its way.");
            case OUT_FOR_DELIVERY ->
                notifier.send(orderId, "Your package is out for delivery today.");
            case DELIVERY_EXCEPTION ->
                notifier.send(orderId, "There is a delivery exception on your package; our team is investigating.");
        }
    }

    public WarehouseStation warehouse() {
        return warehouse;
    }

    public CarrierStation carrier() {
        return carrier;
    }

    public CustomerNotificationStation notifier() {
        return notifier;
    }
}
