package com.patterncatalyst.designpatterns.ch08.shipping.classic;

import com.patterncatalyst.designpatterns.ch08.shipping.Money;
import com.patterncatalyst.designpatterns.ch08.shipping.RateQuote;
import com.patterncatalyst.designpatterns.ch08.shipping.RateStrategy;
import com.patterncatalyst.designpatterns.ch08.shipping.Shipment;
import com.patterncatalyst.designpatterns.ch08.shipping.ShippingMath;
import java.util.Objects;

/**
 * Prices a shipment from a base charge plus a per-kilogram rate that varies
 * by {@link com.patterncatalyst.designpatterns.ch08.shipping.DestinationZone}.
 * A freight carrier with a published zone table is the real-world shape
 * behind this strategy: the farther the zone, the higher the per-kilogram
 * rate, with the zone-to-rate table itself living in
 * {@link ShippingMath#distanceZoneCents(com.patterncatalyst.designpatterns.ch08.shipping.DestinationZone, double)}.
 */
public final class DistanceRateStrategy implements RateStrategy {

    private final String carrierName;

    public DistanceRateStrategy(String carrierName) {
        this.carrierName = Objects.requireNonNull(carrierName, "carrierName");
    }

    @Override
    public RateQuote quote(Shipment shipment) {
        long cents = ShippingMath.distanceZoneCents(shipment.zone(), shipment.weightKg());
        return new RateQuote(carrierName, Money.ofCents(cents));
    }
}
