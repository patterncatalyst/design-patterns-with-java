package com.patterncatalyst.designpatterns.ch16.shipping.decorator;

import com.patterncatalyst.designpatterns.ch16.shipping.Money;
import com.patterncatalyst.designpatterns.ch16.shipping.RateQuote;
import com.patterncatalyst.designpatterns.ch16.shipping.RateQuoter;
import com.patterncatalyst.designpatterns.ch16.shipping.Shipment;
import com.patterncatalyst.designpatterns.ch16.shipping.ShippingMath;

import java.util.Objects;

/**
 * Adds an insurance markup proportional to declared value on top of
 * whatever the wrapped {@link RateQuoter} quotes. The rate is mutable
 * through {@link #setCentsPerDeclaredDollar(double)} so a merchant can
 * adjust it without rebuilding the stack underneath it.
 */
public final class SurchargeRateQuoter implements RateQuoter {

    private final RateQuoter inner;
    private volatile double centsPerDeclaredDollar;

    public SurchargeRateQuoter(RateQuoter inner, double centsPerDeclaredDollar) {
        this.inner = Objects.requireNonNull(inner, "inner");
        this.centsPerDeclaredDollar = centsPerDeclaredDollar;
    }

    public void setCentsPerDeclaredDollar(double centsPerDeclaredDollar) {
        this.centsPerDeclaredDollar = centsPerDeclaredDollar;
    }

    @Override
    public RateQuote quote(Shipment shipment) {
        RateQuote base = inner.quote(shipment);
        long surchargeCents = ShippingMath.insuranceSurchargeCents(shipment.declaredValueCents(), centsPerDeclaredDollar);
        return new RateQuote(base.quotedBy(), base.amount().plus(Money.ofCents(surchargeCents)));
    }
}
