package com.patterncatalyst.designpatterns.ch15.shipping;

import java.util.Objects;

/**
 * The abstraction side of the Bridge. A {@code Notification} holds a
 * {@link DeliveryChannel} by composition, not by inheriting from one, and its
 * {@link #send(String)} method is the bridge itself: it asks the concrete
 * notification for its message text, then hands that text to whichever
 * channel it was built with. A subclass refines what the message says; it
 * never touches how the message gets delivered.
 */
public abstract class Notification {

    private final DeliveryChannel channel;

    protected Notification(DeliveryChannel channel) {
        this.channel = Objects.requireNonNull(channel, "channel");
    }

    public final String send(String recipient) {
        return channel.deliver(recipient, formatMessage());
    }

    protected abstract String formatMessage();
}
