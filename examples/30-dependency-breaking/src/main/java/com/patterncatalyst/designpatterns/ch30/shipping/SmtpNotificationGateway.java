package com.patterncatalyst.designpatterns.ch30.shipping;

import java.util.Objects;

/**
 * The production implementation of {@link NotificationGateway}: the exact
 * concrete type {@code LegacyShipmentProcessor} used to construct inline.
 * A real deployment would open an SMTP connection to {@code smtpHost}; this
 * example only formats and prints the message it would send, so the demo
 * and the aggregator build never touch a network.
 */
public final class SmtpNotificationGateway implements NotificationGateway {

    private final String smtpHost;

    public SmtpNotificationGateway(String smtpHost) {
        this.smtpHost = Objects.requireNonNull(smtpHost, "smtpHost");
    }

    @Override
    public void sendDispatchConfirmation(String operationsEmail, String message) {
        System.out.printf("[smtp:%s] to=%s \"%s\"%n", smtpHost, operationsEmail, message);
    }
}
