package com.patterncatalyst.designpatterns.ch04.notification;

import java.util.Objects;

/**
 * The high-level policy that decides an order event is worth telling a
 * customer about. It depends on {@link MessageSender}, the abstraction,
 * never on {@link EmailGateway} or {@link SmsGateway} directly: no field,
 * import, or {@code new} anywhere in this class names a concrete channel.
 */
public final class OrderNotificationPolicy {

    private final MessageSender sender;

    public OrderNotificationPolicy(MessageSender sender) {
        this.sender = Objects.requireNonNull(sender, "sender");
    }

    public DispatchResult notifyCustomer(OrderEvent event) {
        return sender.send(event);
    }

    /**
     * A sealed {@link NotificationOutcome} keeps the rejection reason in
     * the value a caller already has to inspect, with no second call or
     * logging side channel required to recover it.
     */
    public NotificationOutcome tryNotifyCustomer(OrderEvent event) {
        if (event.customerContact().isBlank()) {
            return new NotificationOutcome.Rejected("order event has no customerContact to notify");
        }
        return new NotificationOutcome.Notified(sender.send(event));
    }
}
