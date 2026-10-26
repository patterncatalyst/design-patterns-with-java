package com.patterncatalyst.designpatterns.ch22.fulfillment;

/**
 * The warehouse's three packaging tiers, each implementing
 * {@link SurchargeRule} through its own constant-specific method body. This
 * is a closed set: a fourth tier means a new constant with its own body
 * added here, not a configuration value read at startup, so the compiler
 * guarantees every tier this warehouse ever hands a package to also knows
 * how to price its own surcharge.
 */
public enum PackagingTier implements SurchargeRule {

    STANDARD {
        @Override
        public long surchargeCents(Shipment shipment) {
            return 0L;
        }
    },

    FRAGILE {
        @Override
        public long surchargeCents(Shipment shipment) {
            return 350L + Math.round(shipment.weightKg() * 40);
        }
    },

    OVERSIZED {
        @Override
        public long surchargeCents(Shipment shipment) {
            return 900L + Math.round(shipment.weightKg() * 75);
        }
    }
}
