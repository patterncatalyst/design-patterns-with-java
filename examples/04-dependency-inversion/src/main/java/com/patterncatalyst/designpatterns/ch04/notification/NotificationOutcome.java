package com.patterncatalyst.designpatterns.ch04.notification;

/**
 * A sealed, exhaustive answer from {@link
 * OrderNotificationPolicy#tryNotifyCustomer(OrderEvent)}: either the event
 * was handed to the channel, or it was rejected before that, with the
 * reason attached.
 *
 * <p>Compare this with {@link
 * OrderNotificationPolicy#tryNotifyCustomerOptional(OrderEvent)}, which
 * answers the identical question with {@code Optional<DispatchResult>} and
 * throws the rejection reason away in the process.
 */
public sealed interface NotificationOutcome permits NotificationOutcome.Notified, NotificationOutcome.Rejected {

    record Notified(DispatchResult result) implements NotificationOutcome {}

    record Rejected(String reason) implements NotificationOutcome {}
}
