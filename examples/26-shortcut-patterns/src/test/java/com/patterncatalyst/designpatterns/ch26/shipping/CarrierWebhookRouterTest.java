package com.patterncatalyst.designpatterns.ch26.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CarrierWebhookRouterTest {

    private final CarrierWebhookRouter router = new CarrierWebhookRouter();

    @ParameterizedTest
    @ValueSource(strings = {"created", "picked_up", "in_transit", "delivered"})
    void routesEveryBuiltInCodeToItsOwnHandler(String code) {
        InMemoryShipmentLedger ledger = new InMemoryShipmentLedger();

        router.route(new CarrierWebhookEvent(code, "SH-2", "detail-" + code), ledger);

        assertThat(ledger.entries()).hasSize(1);
        assertThat(ledger.entries().getFirst())
                .startsWith("SH-2: ")
                .contains("detail-" + code)
                .doesNotContain(CarrierWebhookRouter.UNRECOGNIZED_CODE_NOTE);
    }

    @Test
    void unrecognizedCodeFallsBackInsteadOfThrowing() {
        InMemoryShipmentLedger ledger = new InMemoryShipmentLedger();

        router.route(new CarrierWebhookEvent("UNKNOWN_EVENT", "SH-3", "raw message ignored"), ledger);

        assertThat(ledger.entries()).containsExactly(
                "SH-3: " + CarrierWebhookRouter.UNRECOGNIZED_CODE_NOTE + ": UNKNOWN_EVENT");
    }

    @Test
    void registerAddsANewCodeThatRoutesImmediately() {
        InMemoryShipmentLedger ledger = new InMemoryShipmentLedger();
        int before = router.registeredCodeCount();

        router.register("customs_hold", (event, destination) ->
                destination.record(event.trackingId(), "customs-hold: " + event.detail()));
        router.route(new CarrierWebhookEvent("customs_hold", "SH-4", "awaiting duty payment"), ledger);

        assertThat(router.registeredCodeCount()).isEqualTo(before + 1);
        assertThat(ledger.entries()).containsExactly("SH-4: customs-hold: awaiting duty payment");
    }

    @Test
    void registeringOverAnExistingCodeReplacesItsHandler() {
        InMemoryShipmentLedger ledger = new InMemoryShipmentLedger();

        router.register("delivered", (event, destination) ->
                destination.record(event.trackingId(), "delivered-v2: " + event.detail()));
        router.route(new CarrierWebhookEvent("delivered", "SH-5", "signed for"), ledger);

        assertThat(ledger.entries()).containsExactly("SH-5: delivered-v2: signed for");
    }
}
