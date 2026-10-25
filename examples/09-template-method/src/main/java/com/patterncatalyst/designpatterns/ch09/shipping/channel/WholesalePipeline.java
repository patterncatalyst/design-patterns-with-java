package com.patterncatalyst.designpatterns.ch09.shipping.channel;

import com.patterncatalyst.designpatterns.ch09.shipping.Order;
import com.patterncatalyst.designpatterns.ch09.shipping.OrderMath;
import com.patterncatalyst.designpatterns.ch09.shipping.OrderProcessingPipeline;

/**
 * The wholesale channel: a contracted volume discount, gated behind a
 * minimum order quantity a one-off retail order will never reach.
 */
public final class WholesalePipeline extends OrderProcessingPipeline {

    private static final int MINIMUM_UNITS = 50;
    private static final double VOLUME_DISCOUNT = 0.12;

    @Override
    protected String channelName() {
        return "wholesale";
    }

    @Override
    protected void validateChannelRules(Order order) {
        int totalUnits = OrderMath.totalQuantity(order);
        if (totalUnits < MINIMUM_UNITS) {
            throw new IllegalArgumentException(
                    "wholesale orders require at least " + MINIMUM_UNITS + " units, got " + totalUnits);
        }
    }

    @Override
    protected long priceOrder(Order order) {
        return OrderMath.applyPercentageReduction(OrderMath.listPriceCents(order), VOLUME_DISCOUNT);
    }

    @Override
    protected String recordToLedger(Order order, long totalCents) {
        return "wholesale:" + order.orderId() + ":discount=" + VOLUME_DISCOUNT;
    }
}
