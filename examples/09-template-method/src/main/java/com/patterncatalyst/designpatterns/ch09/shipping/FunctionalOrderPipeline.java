package com.patterncatalyst.designpatterns.ch09.shipping;

import java.util.Objects;

/**
 * The identical fixed sequence {@link OrderProcessingPipeline} enforces
 * through inheritance, written once as a value instead. The invariant
 * precondition is still non-negotiable: {@link
 * OrderMath#requireAtLeastOneLine(Order)} runs before any supplied step runs.
 * The three varying steps arrive as functional-interface arguments rather
 * than as overridden methods, so a new channel is a new value built from
 * three lambdas, never a new class.
 */
public record FunctionalOrderPipeline(
        String channelName, ChannelValidator validator, ChannelPricer pricer, ChannelRecorder recorder) {

    public FunctionalOrderPipeline {
        if (channelName == null || channelName.isBlank()) {
            throw new IllegalArgumentException("channelName must not be blank");
        }
        Objects.requireNonNull(validator, "validator");
        Objects.requireNonNull(pricer, "pricer");
        Objects.requireNonNull(recorder, "recorder");
    }

    public ProcessingResult process(Order order) {
        OrderMath.requireAtLeastOneLine(order);
        validator.validate(order);
        long totalCents = pricer.price(order);
        String ledgerNote = recorder.record(order, totalCents);
        return new ProcessingResult(
                order.orderId(), channelName, totalCents, OrderMath.totalQuantity(order), ledgerNote);
    }
}
