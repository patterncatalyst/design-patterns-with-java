package com.patterncatalyst.designpatterns.ch15.shipping;

public final class SmsDeliveryChannel implements DeliveryChannel {

    @Override
    public String deliver(String recipient, String message) {
        return "SMS to %s: %s".formatted(recipient, message);
    }
}
