package com.patterncatalyst.designpatterns.ch08.shipping;

/**
 * The company's three contracted carriers, each implementing
 * {@link RateStrategy} through its own constant-specific method body. Unlike
 * {@code named.WeightTieredRateStrategy} or
 * {@code RateStrategies.weightTiered(...)}, a {@code Carrier} is not built
 * from configuration and is not swapped in at runtime; the three constants
 * below are the entire, closed set of carriers this checkout ever quotes
 * against. Adding a fourth means editing this file and recompiling, which is
 * exactly the trade a closed set is supposed to make: every call site that
 * switches on or iterates {@code Carrier.values()} is guaranteed, by the
 * compiler, to see every carrier that exists.
 */
public enum Carrier implements RateStrategy {

    GROUND {
        @Override
        public RateQuote quote(Shipment shipment) {
            long cents = ShippingMath.weightTieredCents(500, 180, shipment.weightKg());
            return new RateQuote("ground", Money.ofCents(cents));
        }
    },

    AIR_FLAT {
        @Override
        public RateQuote quote(Shipment shipment) {
            return new RateQuote("air-flat", Money.ofCents(999));
        }
    },

    FREIGHT_ZONE {
        @Override
        public RateQuote quote(Shipment shipment) {
            long cents = ShippingMath.distanceZoneCents(shipment.zone(), shipment.weightKg());
            return new RateQuote("freight-zone", Money.ofCents(cents));
        }
    }
}
