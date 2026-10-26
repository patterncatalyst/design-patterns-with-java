package com.patterncatalyst.designpatterns.ch32.accessorials;

/**
 * The replacement for {@link LegacyAccessorialFeeCalculator}. Every rate,
 * threshold, and quirk the legacy calculator enforces is preserved here,
 * including the two undocumented carrier-specific rules: the fuel
 * surcharge rates and the air-freight oversize penalty. This rewrite
 * restructures how those rules are expressed, a named switch expression
 * instead of a string-equality if/else chain, named constants instead of
 * inline magic numbers. It does not second-guess what the rules say. It
 * earns the right to take traffic by passing the same characterization
 * table the legacy calculator is pinned against, not by looking cleaner.
 */
public final class ModernAccessorialFeeCalculator implements AccessorialFeeCalculator {

    private static final double DEFAULT_FUEL_SURCHARGE_RATE = 0.10;

    private static final double RESIDENTIAL_FEE_WEIGHT_WAIVER_KG = 20.0;
    private static final long RESIDENTIAL_FEE_CENTS = 450L;

    private static final long LIFTGATE_FEE_CENTS = 350L;
    private static final long LIFTGATE_OVERSIZE_SURCHARGE_CENTS = 100L;

    private static final long OVERSIZE_FEE_CENTS = 900L;
    private static final long AIR_OVERSIZE_MULTIPLIER = 2L;

    @Override
    public AccessorialFees calculateFees(ShipmentManifest manifest) {
        return new AccessorialFees(
                fuelSurchargeCents(manifest),
                residentialFeeCents(manifest),
                liftgateFeeCents(manifest),
                oversizeFeeCents(manifest));
    }

    private long fuelSurchargeCents(ShipmentManifest manifest) {
        double rate = fuelSurchargeRate(manifest.carrierCode());
        return Math.round(manifest.declaredValueCents() * rate);
    }

    // A switch expression retires the legacy if/else chain of string
    // equality checks: every carrier's rate is one named branch instead of
    // a condition and an assignment split across two lines.
    private double fuelSurchargeRate(String carrierCode) {
        return switch (carrierCode) {
            case "OCEAN" -> 0.15;
            case "AIR" -> 0.22;
            case "GROUND" -> 0.08;
            default -> DEFAULT_FUEL_SURCHARGE_RATE;
        };
    }

    private long residentialFeeCents(ShipmentManifest manifest) {
        if (!manifest.residentialDelivery() || manifest.weightKg() <= RESIDENTIAL_FEE_WEIGHT_WAIVER_KG) {
            return 0L;
        }
        return RESIDENTIAL_FEE_CENTS;
    }

    private long liftgateFeeCents(ShipmentManifest manifest) {
        if (!manifest.liftgateRequired()) {
            return 0L;
        }
        long fee = LIFTGATE_FEE_CENTS;
        if (manifest.oversized()) {
            fee += LIFTGATE_OVERSIZE_SURCHARGE_CENTS;
        }
        return fee;
    }

    private long oversizeFeeCents(ShipmentManifest manifest) {
        if (!manifest.oversized()) {
            return 0L;
        }
        long fee = OVERSIZE_FEE_CENTS;
        if ("AIR".equals(manifest.carrierCode())) {
            fee *= AIR_OVERSIZE_MULTIPLIER;
        }
        return fee;
    }
}
