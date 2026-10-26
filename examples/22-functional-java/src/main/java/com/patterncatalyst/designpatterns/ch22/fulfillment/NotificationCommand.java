package com.patterncatalyst.designpatterns.ch22.fulfillment;

/**
 * A request to notify someone about a {@link Shipment}, encapsulated as an
 * object instead of invoked directly, the Command pattern's whole claim.
 * {@link NotificationLog#recordEmail} and {@link NotificationLog#recordSms}
 * satisfy this contract as method references, with no class written
 * anywhere named {@code EmailNotificationCommand} or
 * {@code SmsNotificationCommand}.
 */
@FunctionalInterface
public interface NotificationCommand {
    void dispatch(Shipment shipment);
}
