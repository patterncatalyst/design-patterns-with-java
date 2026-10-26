package com.patterncatalyst.designpatterns.ch16.shipping;

import java.util.List;
import java.util.Objects;

/**
 * The same two decorators as {@code decorator.SurchargeRateQuoter} and
 * {@code decorator.LoggingRateQuoter}, expressed as lambdas assigned
 * directly to {@link RateQuoter} instead of as named classes. {@code
 * RateQuoter} declares exactly one abstract method, so it is a functional
 * interface whether or not anyone marks it {@code @FunctionalInterface},
 * and a caller holding a {@code RateQuoter} field cannot tell whether the
 * value it holds came from {@code new SurchargeRateQuoter(...)} or from a
 * lambda that never declared a class at all.
 */
public final class RateQuoters {

    private RateQuoters() {
    }

    public static RateQuoter surcharging(RateQuoter inner, double centsPerDeclaredDollar) {
        Objects.requireNonNull(inner, "inner");
        return shipment -> {
            RateQuote base = inner.quote(shipment);
            long surchargeCents = ShippingMath.insuranceSurchargeCents(shipment.declaredValueCents(), centsPerDeclaredDollar);
            return new RateQuote(base.quotedBy(), base.amount().plus(Money.ofCents(surchargeCents)));
        };
    }

    public static RateQuoter logging(RateQuoter inner, List<String> callLog) {
        Objects.requireNonNull(inner, "inner");
        Objects.requireNonNull(callLog, "callLog");
        return shipment -> {
            RateQuote result = inner.quote(shipment);
            callLog.add("quoted " + shipment.zone() + " shipment at " + result.amount());
            return result;
        };
    }
}
