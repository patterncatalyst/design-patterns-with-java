package com.patterncatalyst.designpatterns.ch09.shipping.channel;

import com.patterncatalyst.designpatterns.ch09.shipping.Order;
import com.patterncatalyst.designpatterns.ch09.shipping.OrderMath;
import com.patterncatalyst.designpatterns.ch09.shipping.OrderProcessingPipeline;

/**
 * The web storefront channel: list price straight through, no volume
 * minimum, no commission. Its only channel rule is a per-order line cap a
 * single fulfillment center can pick without a pallet.
 */
public final class RetailWebPipeline extends OrderProcessingPipeline {

    private static final int MAX_LINES_PER_ORDER = 40;

    @Override
    protected String channelName() {
        return "retail-web";
    }

    @Override
    protected void validateChannelRules(Order order) {
        if (order.lines().size() > MAX_LINES_PER_ORDER) {
            throw new IllegalArgumentException(
                    "retail-web orders are capped at " + MAX_LINES_PER_ORDER + " line items");
        }
    }

    @Override
    protected long priceOrder(Order order) {
        return OrderMath.listPriceCents(order);
    }

    @Override
    protected String recordToLedger(Order order, long totalCents) {
        return "retail-web:" + order.orderId();
    }
}
