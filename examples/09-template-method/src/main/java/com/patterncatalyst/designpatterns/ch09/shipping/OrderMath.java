package com.patterncatalyst.designpatterns.ch09.shipping;

/**
 * The pricing and quantity math shared by every channel pipeline, in both
 * the abstract-method and functional expressions of Template Method in this
 * example, including the invariant precondition {@link
 * OrderProcessingPipeline} and {@link FunctionalOrderPipeline} both run
 * before any varying step. Centralizing it here means an identical {@link
 * Order} totals identically no matter which pipeline, or which form of
 * pipeline, processed it.
 */
public final class OrderMath {

    private OrderMath() {
    }

    public static int totalQuantity(Order order) {
        int total = 0;
        for (OrderLine line : order.lines()) {
            total += line.quantity();
        }
        return total;
    }

    public static long listPriceCents(Order order) {
        long total = 0L;
        for (OrderLine line : order.lines()) {
            total += (long) line.quantity() * line.unitPriceCents();
        }
        return total;
    }

    public static long applyPercentageReduction(long cents, double reductionRate) {
        if (reductionRate < 0.0 || reductionRate >= 1.0) {
            throw new IllegalArgumentException("reductionRate must be in [0, 1): " + reductionRate);
        }
        return Math.round(cents * (1.0 - reductionRate));
    }

    public static void requireAtLeastOneLine(Order order) {
        if (order.lines().isEmpty()) {
            throw new IllegalArgumentException("order " + order.orderId() + " has no line items");
        }
    }
}
