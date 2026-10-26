package com.patterncatalyst.designpatterns.ch30.shipping;

/**
 * The narrow seam Extract Interface pulls out of the one method
 * {@code LegacyShipmentProcessor} actually calls on its mail dependency.
 * The inherited code had no interface here at all: the constructor built a
 * concrete {@link SmtpNotificationGateway} directly, so no test could ever
 * supply anything else.
 */
public interface NotificationGateway {

    void sendDispatchConfirmation(String operationsEmail, String message);
}
