package com.patterncatalyst.designpatterns.ch18.shipping.dispatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The station a customer-facing message ultimately reaches. Nothing routes
 * a call to {@link #send} except {@link DispatchCoordinator}; the warehouse
 * and the carrier have no reference to this class at all.
 */
public final class CustomerNotificationStation extends DispatchColleague {

    private final List<String> sentMessages = new ArrayList<>();

    public void send(String orderId, String message) {
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(message, "message");
        sentMessages.add(orderId + ": " + message);
    }

    public List<String> sentMessages() {
        return Collections.unmodifiableList(sentMessages);
    }
}
