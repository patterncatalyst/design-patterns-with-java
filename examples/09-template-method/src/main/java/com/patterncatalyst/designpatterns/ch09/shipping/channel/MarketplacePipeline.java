package com.patterncatalyst.designpatterns.ch09.shipping.channel;

import com.patterncatalyst.designpatterns.ch09.shipping.Order;
import com.patterncatalyst.designpatterns.ch09.shipping.OrderMath;
import com.patterncatalyst.designpatterns.ch09.shipping.OrderProcessingPipeline;

/**
 * The third-party marketplace channel: every order must carry the
 * marketplace's own reference number, and pricing deducts the marketplace's
 * commission before the order reaches the ledger.
 */
public final class MarketplacePipeline extends OrderProcessingPipeline {

    private static final double COMMISSION_RATE = 0.15;

    @Override
    protected String channelName() {
        return "marketplace";
    }

    @Override
    protected void validateChannelRules(Order order) {
        if (order.marketplaceReference().isBlank()) {
            throw new IllegalArgumentException("marketplace orders require a marketplaceReference");
        }
    }

    @Override
    protected long priceOrder(Order order) {
        return OrderMath.applyPercentageReduction(OrderMath.listPriceCents(order), COMMISSION_RATE);
    }

    @Override
    protected String recordToLedger(Order order, long totalCents) {
        return "marketplace:" + order.orderId() + ":ref=" + order.marketplaceReference()
                + ":commission=" + COMMISSION_RATE;
    }
}
