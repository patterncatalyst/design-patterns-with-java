package com.patterncatalyst.designpatterns.ch04.notification;

import java.util.Objects;

/**
 * An order event worth telling a customer about: an order left the
 * warehouse, is running late, or arrived.
 *
 * <p>{@code customerContact} is allowed to be blank. An order
 * placed with no contact on file is a real state this domain has to carry,
 * not an invariant the type should reject at construction. {@link
 * OrderNotificationPolicy#tryNotifyCustomer(OrderEvent)} is where that
 * missing contact gets turned into a decision.
 */
public record OrderEvent(String orderId, String customerContact, String messageBody) {

    public OrderEvent {
        Objects.requireNonNull(customerContact, "customerContact");
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId must not be blank");
        }
        if (messageBody == null || messageBody.isBlank()) {
            throw new IllegalArgumentException("messageBody must not be blank");
        }
    }

    /**
     * Returns a copy of this event with a different message body, used to
     * carry a rendered template back into the dispatch path without
     * mutating the original event.
     */
    public OrderEvent withMessageBody(String renderedBody) {
        return new OrderEvent(orderId, customerContact, renderedBody);
    }
}
