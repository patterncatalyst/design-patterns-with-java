package com.patterncatalyst.designpatterns.ch32.accessorials;

import java.util.Objects;

/**
 * The routing seam. A caller depends on {@link AccessorialFeeCalculator}
 * exactly as it would depend on either concrete implementation, and this
 * router decides, per shipment, whether {@link LegacyAccessorialFeeCalculator}
 * or {@link ModernAccessorialFeeCalculator} answers. The decision lives in
 * {@link MigratedShipmentRegistry}, not in this class, so the routing policy
 * can change (one shipment, a whole carrier, everyone) without touching the
 * router itself.
 */
public final class StranglerFeeCalculatorRouter implements AccessorialFeeCalculator {

    private final AccessorialFeeCalculator legacy;
    private final AccessorialFeeCalculator modern;
    private final MigratedShipmentRegistry registry;

    public StranglerFeeCalculatorRouter(
            AccessorialFeeCalculator legacy, AccessorialFeeCalculator modern, MigratedShipmentRegistry registry) {
        this.legacy = Objects.requireNonNull(legacy, "legacy");
        this.modern = Objects.requireNonNull(modern, "modern");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public AccessorialFees calculateFees(ShipmentManifest manifest) {
        AccessorialFeeCalculator target = registry.isMigrated(manifest.shipmentId()) ? modern : legacy;
        return target.calculateFees(manifest);
    }
}
