package com.patterncatalyst.designpatterns.ch09.shipping;

/**
 * The same three channel configurations {@code channel.RetailWebPipeline},
 * {@code channel.WholesalePipeline}, and {@code channel.MarketplacePipeline}
 * express as subclasses, built instead as {@link FunctionalOrderPipeline}
 * values from lambdas assigned directly to this example's functional
 * interfaces. The pricing and quantity math is the same {@link OrderMath}
 * the subclass form calls, so the two forms stay provably identical.
 */
public final class ChannelPipelines {

    private static final int RETAIL_MAX_LINES_PER_ORDER = 40;
    private static final int WHOLESALE_MINIMUM_UNITS = 50;
    private static final double WHOLESALE_VOLUME_DISCOUNT = 0.12;
    private static final double MARKETPLACE_COMMISSION_RATE = 0.15;

    private ChannelPipelines() {
    }

    public static FunctionalOrderPipeline retailWeb() {
        return new FunctionalOrderPipeline(
                "retail-web",
                order -> {
                    if (order.lines().size() > RETAIL_MAX_LINES_PER_ORDER) {
                        throw new IllegalArgumentException(
                                "retail-web orders are capped at " + RETAIL_MAX_LINES_PER_ORDER + " line items");
                    }
                },
                OrderMath::listPriceCents,
                (order, totalCents) -> "retail-web:" + order.orderId());
    }

    public static FunctionalOrderPipeline wholesale() {
        return new FunctionalOrderPipeline(
                "wholesale",
                order -> {
                    int totalUnits = OrderMath.totalQuantity(order);
                    if (totalUnits < WHOLESALE_MINIMUM_UNITS) {
                        throw new IllegalArgumentException(
                                "wholesale orders require at least " + WHOLESALE_MINIMUM_UNITS
                                        + " units, got " + totalUnits);
                    }
                },
                order -> OrderMath.applyPercentageReduction(
                        OrderMath.listPriceCents(order), WHOLESALE_VOLUME_DISCOUNT),
                (order, totalCents) -> "wholesale:" + order.orderId() + ":discount=" + WHOLESALE_VOLUME_DISCOUNT);
    }

    public static FunctionalOrderPipeline marketplace() {
        return new FunctionalOrderPipeline(
                "marketplace",
                order -> {
                    if (order.marketplaceReference().isBlank()) {
                        throw new IllegalArgumentException("marketplace orders require a marketplaceReference");
                    }
                },
                order -> OrderMath.applyPercentageReduction(
                        OrderMath.listPriceCents(order), MARKETPLACE_COMMISSION_RATE),
                (order, totalCents) -> "marketplace:" + order.orderId() + ":ref=" + order.marketplaceReference()
                        + ":commission=" + MARKETPLACE_COMMISSION_RATE);
    }
}
