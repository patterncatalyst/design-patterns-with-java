package com.patterncatalyst.designpatterns.ch08.shipping.named;

import com.patterncatalyst.designpatterns.ch08.shipping.Money;
import com.patterncatalyst.designpatterns.ch08.shipping.RateQuote;
import com.patterncatalyst.designpatterns.ch08.shipping.RateStrategy;
import com.patterncatalyst.designpatterns.ch08.shipping.Shipment;
import com.patterncatalyst.designpatterns.ch08.shipping.ShippingMath;
import java.util.Objects;

/**
 * Prices a shipment as a base charge plus a per-kilogram rate. This is the
 * named-class carrier of the three the book compares, holding its own
 * {@code baseCents} and {@code perKgCents}, next to the identical math
 * expressed as a one-line lambda in {@code RateStrategies} and as a
 * {@link com.patterncatalyst.designpatterns.ch08.shipping.Carrier}
 * enum constant.
 */
public final class WeightTieredRateStrategy implements RateStrategy {

    private final String carrierName;
    private final long baseCents;
    private final long perKgCents;

    public WeightTieredRateStrategy(String carrierName, long baseCents, long perKgCents) {
        this.carrierName = Objects.requireNonNull(carrierName, "carrierName");
        this.baseCents = baseCents;
        this.perKgCents = perKgCents;
    }

    @Override
    public RateQuote quote(Shipment shipment) {
        long cents = ShippingMath.weightTieredCents(baseCents, perKgCents, shipment.weightKg());
        return new RateQuote(carrierName, Money.ofCents(cents));
    }
}
