package com.patterncatalyst.designpatterns.ch32.accessorials;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MigratedShipmentRegistry is the cutover policy: a small, explicit record
 * of which shipment IDs have moved to the modern calculator, so the router
 * never has to guess and the migration can be driven one shipment at a
 * time.
 */
class MigratedShipmentRegistryTest {

    private final MigratedShipmentRegistry registry = new MigratedShipmentRegistry();

    @Test
    @DisplayName("reports a shipment as not migrated until it is explicitly migrated")
    void reportsNotMigratedUntilMigrated() {
        assertThat(registry.isMigrated("SHIP-3001")).isFalse();

        registry.migrate("SHIP-3001");

        assertThat(registry.isMigrated("SHIP-3001")).isTrue();
    }

    @Test
    @DisplayName("counts only the shipments that have migrated")
    void countsOnlyMigratedShipments() {
        registry.migrate("SHIP-3001");
        registry.migrate("SHIP-3002");

        assertThat(registry.migratedCount()).isEqualTo(2);
        assertThat(registry.isMigrated("SHIP-3003")).isFalse();
    }

    @Test
    @DisplayName("migrating the same shipment twice does not double count it")
    void migratingTwiceDoesNotDoubleCount() {
        registry.migrate("SHIP-3001");
        registry.migrate("SHIP-3001");

        assertThat(registry.migratedCount()).isEqualTo(1);
    }
}
