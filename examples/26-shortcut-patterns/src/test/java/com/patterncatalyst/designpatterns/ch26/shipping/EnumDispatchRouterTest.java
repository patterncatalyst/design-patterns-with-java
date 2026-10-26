package com.patterncatalyst.designpatterns.ch26.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EnumDispatchRouterTest {

    private final EnumDispatchRouter router = new EnumDispatchRouter();

    @ParameterizedTest
    @EnumSource(ShipmentEventType.class)
    void routesEveryClosedSetKeyToItsOwnHandler(ShipmentEventType type) {
        InMemoryShipmentLedger ledger = new InMemoryShipmentLedger();

        router.route(new ShipmentEvent(type, "SH-1", "detail-" + type), ledger);

        assertThat(ledger.entries()).hasSize(1);
        assertThat(ledger.entries().getFirst()).startsWith("SH-1: ").contains("detail-" + type);
    }

    @Test
    void differentEventTypesProduceDifferentlyPrefixedNotes() {
        InMemoryShipmentLedger ledger = new InMemoryShipmentLedger();

        router.route(new ShipmentEvent(ShipmentEventType.CREATED, "SH-1", "placed"), ledger);
        router.route(new ShipmentEvent(ShipmentEventType.DELIVERED, "SH-1", "left on porch"), ledger);

        assertThat(ledger.entries()).containsExactly(
                "SH-1: created: placed",
                "SH-1: delivered: left on porch");
    }

    @Test
    void exceptionEventRoutesToTheExceptionHandler() {
        InMemoryShipmentLedger ledger = new InMemoryShipmentLedger();

        router.route(new ShipmentEvent(ShipmentEventType.EXCEPTION, "SH-9", "recipient refused delivery"), ledger);

        assertThat(ledger.entries()).containsExactly("SH-9: exception: recipient refused delivery");
    }
}
