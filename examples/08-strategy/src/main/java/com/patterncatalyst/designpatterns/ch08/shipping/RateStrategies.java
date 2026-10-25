package com.patterncatalyst.designpatterns.ch08.shipping;

import java.util.Objects;

/**
 * The same three pricing algorithms as the {@code named} package, each
 * collapsed from a named class to a one-expression lambda assigned straight
 * to a {@link RateStrategy} variable. {@link #withInsuranceSurcharge} is the
 * capability a lambda value gets for free that a class hierarchy does not:
 * wrapping one strategy around another without introducing a decorator
 * class.
 */
public final class RateStrategies {

    private RateStrategies() {
    }

    public static RateStrategy flat(String carrierName, long flatCents) {
        Objects.requireNonNull(carrierName, "carrierName");
        return shipment -> new RateQuote(carrierName, Money.ofCents(flatCents));
    }

    public static RateStrategy weightTiered(String carrierName, long baseCents, long perKgCents) {
        Objects.requireNonNull(carrierName, "carrierName");
        return shipment -> new RateQuote(
                carrierName,
                Money.ofCents(ShippingMath.weightTieredCents(baseCents, perKgCents, shipment.weightKg())));
    }

    public static RateStrategy distanceZone(String carrierName) {
        Objects.requireNonNull(carrierName, "carrierName");
        return shipment -> new RateQuote(
                carrierName,
                Money.ofCents(ShippingMath.distanceZoneCents(shipment.zone(), shipment.weightKg())));
    }

    /**
     * Wraps an existing strategy in an insurance surcharge proportional to
     * the shipment's declared value, returning a new {@link RateStrategy}
     * rather than mutating the one passed in. Doing the same thing to a
     * named-class concrete implementation would mean writing a decorator
     * class that also implements {@code RateStrategy} and forwards to a
     * wrapped instance; a lambda closes over {@code base} and {@code
     * centsPerDeclaredDollar} with no new type at all.
     */
    public static RateStrategy withInsuranceSurcharge(RateStrategy base, double centsPerDeclaredDollar) {
        Objects.requireNonNull(base, "base");
        return shipment -> {
            RateQuote baseQuote = base.quote(shipment);
            long surchargeCents = ShippingMath.insuranceSurchargeCents(
                    shipment.declaredValueCents(), centsPerDeclaredDollar);
            return new RateQuote(baseQuote.quotedBy(), baseQuote.amount().plus(Money.ofCents(surchargeCents)));
        };
    }
}
