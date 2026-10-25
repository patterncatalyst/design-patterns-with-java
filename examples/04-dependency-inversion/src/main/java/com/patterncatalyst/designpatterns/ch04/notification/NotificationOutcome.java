package com.patterncatalyst.designpatterns.ch04.notification;

/**
 * A sealed, exhaustive answer from {@link
 * OrderNotificationPolicy#tryNotifyCustomer(OrderEvent)}: either the event
 * was handed to the channel, or it was rejected before that, with the
 * reason attached.
 */
public sealed interface NotificationOutcome permits NotificationOutcome.Notified, NotificationOutcome.Rejected {

    record Notified(DispatchResult result) implements NotificationOutcome {}

    record Rejected(String reason) implements NotificationOutcome {}
}
