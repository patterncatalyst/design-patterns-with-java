package com.patterncatalyst.designpatterns.ch09.shipping;

/**
 * What {@link OrderProcessingPipeline#process(Order)} and {@link
 * FunctionalOrderPipeline#process(Order)} both return: the fixed sequence's
 * one output, assembled from the invariant precondition and all three
 * varying steps, no matter which form supplied them.
 */
public record ProcessingResult(String orderId, String channel, long totalCents, int lineCount, String ledgerNote) {
}
