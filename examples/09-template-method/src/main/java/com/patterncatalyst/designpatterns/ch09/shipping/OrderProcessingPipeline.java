package com.patterncatalyst.designpatterns.ch09.shipping;

/**
 * The fixed sequence every channel pipeline runs: an invariant precondition
 * no channel can skip, then three hooks a channel subclass fills in.
 * {@link #process(Order)} is {@code final}, so no subclass can reorder the
 * steps, skip the precondition, or call a hook twice; the hooks themselves
 * are {@code protected abstract}, so a subclass supplies their content but
 * has no entry point of its own into the sequence.
 */
public abstract class OrderProcessingPipeline {

    public final ProcessingResult process(Order order) {
        OrderMath.requireAtLeastOneLine(order);
        validateChannelRules(order);
        long totalCents = priceOrder(order);
        String ledgerNote = recordToLedger(order, totalCents);
        return new ProcessingResult(
                order.orderId(), channelName(), totalCents, OrderMath.totalQuantity(order), ledgerNote);
    }

    protected abstract String channelName();

    protected abstract void validateChannelRules(Order order);

    protected abstract long priceOrder(Order order);

    protected abstract String recordToLedger(Order order, long totalCents);
}
