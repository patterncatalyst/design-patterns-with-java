package com.patterncatalyst.designpatterns.ch15.shipping;

public final class EmailDeliveryChannel implements DeliveryChannel {

    @Override
    public String deliver(String recipient, String message) {
        return "EMAIL to %s: %s".formatted(recipient, message);
    }
}
