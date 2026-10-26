package com.patterncatalyst.designpatterns.ch32.accessorials;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * StranglerFeeCalculatorRouter is the routing seam: every caller depends on
 * AccessorialFeeCalculator, never on a concrete legacy or modern
 * implementation, and the router decides per shipment which one answers.
 * Both collaborators are mocked here so each assertion checks an
 * interaction, which shipment was sent to which calculator, not only a
 * value either mock was told to return.
 */
@ExtendWith(MockitoExtension.class)
class StranglerFeeCalculatorRouterTest {

    @Mock
    private AccessorialFeeCalculator legacy;
    @Mock
    private AccessorialFeeCalculator modern;

    private MigratedShipmentRegistry registry;
    private StranglerFeeCalculatorRouter router;

    private final ShipmentManifest migratedShipment =
            new ShipmentManifest("SHIP-2001", "AIR", 60_000L, 12.0, false, false, false);
    private final ShipmentManifest unmigratedShipment =
            new ShipmentManifest("SHIP-2002", "GROUND", 30_000L, 8.0, false, false, false);

    @BeforeEach
    void setUp() {
        registry = new MigratedShipmentRegistry();
        registry.migrate(migratedShipment.shipmentId());
        router = new StranglerFeeCalculatorRouter(legacy, modern, registry);
    }

    @Nested
    @DisplayName("while only one shipment has been migrated")
    class WhilePartiallyMigrated {

        @Test
        @DisplayName("routes a migrated shipment to the modern calculator, never the legacy one")
        void routesMigratedShipmentToModern() {
            AccessorialFees modernFees = new AccessorialFees(1L, 2L, 3L, 4L);
            when(modern.calculateFees(migratedShipment)).thenReturn(modernFees);

            AccessorialFees result = router.calculateFees(migratedShipment);

            assertThat(result).isEqualTo(modernFees);
            verify(modern).calculateFees(migratedShipment);
            verify(legacy, never()).calculateFees(any());
        }

        @Test
        @DisplayName("routes an unmigrated shipment to the legacy calculator, never the modern one")
        void routesUnmigratedShipmentToLegacy() {
            AccessorialFees legacyFees = new AccessorialFees(5L, 6L, 7L, 8L);
            when(legacy.calculateFees(unmigratedShipment)).thenReturn(legacyFees);

            AccessorialFees result = router.calculateFees(unmigratedShipment);

            assertThat(result).isEqualTo(legacyFees);
            verify(legacy).calculateFees(unmigratedShipment);
            verify(modern, never()).calculateFees(any());
        }
    }

    @Nested
    @DisplayName("once every shipment has migrated")
    class WhenFullyMigrated {

        @Test
        @DisplayName("sends every shipment to the modern calculator and never touches legacy again")
        void sendsEveryShipmentToModern() {
            registry.migrate(unmigratedShipment.shipmentId());
            when(modern.calculateFees(any())).thenReturn(new AccessorialFees(0L, 0L, 0L, 0L));

            router.calculateFees(migratedShipment);
            router.calculateFees(unmigratedShipment);

            verify(modern, times(2)).calculateFees(any());
            verifyNoInteractions(legacy);
        }
    }
}
