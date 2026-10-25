package com.patterncatalyst.designpatterns.ch04.notification;

import java.util.Objects;
import java.util.Optional;

/**
 * The high-level policy that decides an order event is worth telling a
 * customer about. It depends on {@link MessageSender}, the abstraction,
 * never on {@link EmailGateway} or {@link SmsGateway} directly: no field,
 * import, or {@code new} anywhere in this class names a concrete channel.
 *
 * <p>{@code tryNotifyCustomerOptional} and {@code tryNotifyCustomer} answer
 * the same question, whether a customer with no contact on file should be
 * notified, two different idiomatic ways. The first is the shape available
 * before sealed interfaces and pattern matching: {@code Optional} says
 * whether the event was accepted but has nothing to say about why it
 * was not. The second carries the rejection reason in the type itself.
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
     * Classic Java: {@code Optional<DispatchResult>} tells a caller whether
     * the event was sent, nothing more. A rejected event and a rejected
     * event for a different reason look identical from the outside.
     */
    public Optional<DispatchResult> tryNotifyCustomerOptional(OrderEvent event) {
        if (event.customerContact().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(sender.send(event));
    }

    /**
     * Java 25: a sealed {@link NotificationOutcome} keeps the rejection
     * reason in the value a caller already has to inspect, with no second
     * call or logging side channel required to recover it.
     */
    public NotificationOutcome tryNotifyCustomer(OrderEvent event) {
        if (event.customerContact().isBlank()) {
            return new NotificationOutcome.Rejected("order event has no customerContact to notify");
        }
        return new NotificationOutcome.Notified(sender.send(event));
    }
}
