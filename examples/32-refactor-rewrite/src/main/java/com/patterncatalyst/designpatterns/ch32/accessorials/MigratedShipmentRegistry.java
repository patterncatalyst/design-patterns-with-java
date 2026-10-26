package com.patterncatalyst.designpatterns.ch32.accessorials;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The cutover policy: an explicit, queryable record of which shipment IDs
 * have moved to the modern calculator. {@link StranglerFeeCalculatorRouter}
 * consults this on every call instead of guessing, which is what lets the
 * migration move one shipment, one customer, or one carrier at a time
 * rather than all at once.
 */
public final class MigratedShipmentRegistry {

    private final Set<String> migratedShipmentIds = new HashSet<>();

    public void migrate(String shipmentId) {
        migratedShipmentIds.add(Objects.requireNonNull(shipmentId, "shipmentId"));
    }

    public boolean isMigrated(String shipmentId) {
        return migratedShipmentIds.contains(shipmentId);
    }

    public int migratedCount() {
        return migratedShipmentIds.size();
    }
}
