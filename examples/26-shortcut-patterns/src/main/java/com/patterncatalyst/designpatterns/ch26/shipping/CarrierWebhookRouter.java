package com.patterncatalyst.designpatterns.ch26.shipping;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Routes a {@link CarrierWebhookEvent} to its handler through a
 * {@code Map<String, handler>}, the Java analog of a C-style
 * function-pointer jump table built over an open key instead of a closed
 * one. A carrier's wire code is not one of six values this codebase
 * enumerated ahead of time; {@link #register(String, BiConsumer)} lets a
 * new carrier's code be added while the program is already running, with
 * no recompile.
 */
public final class CarrierWebhookRouter {

    public static final String UNRECOGNIZED_CODE_NOTE = "unrecognized webhook code, routed to fallback";

    private final Map<String, BiConsumer<CarrierWebhookEvent, ShipmentLedger>> handlers = new HashMap<>();

    public CarrierWebhookRouter() {
        handlers.put("created", (event, ledger) -> ledger.record(event.trackingId(), "created: " + event.detail()));
        handlers.put("picked_up", (event, ledger) -> ledger.record(event.trackingId(), "picked-up: " + event.detail()));
        handlers.put("in_transit", (event, ledger) -> ledger.record(event.trackingId(), "in-transit: " + event.detail()));
        handlers.put("delivered", (event, ledger) -> ledger.record(event.trackingId(), "delivered: " + event.detail()));
    }

    public void register(String code, BiConsumer<CarrierWebhookEvent, ShipmentLedger> handler) {
        handlers.put(Objects.requireNonNull(code, "code"), Objects.requireNonNull(handler, "handler"));
    }

    public int registeredCodeCount() {
        return handlers.size();
    }

    public void route(CarrierWebhookEvent event, ShipmentLedger ledger) {
        handlers.getOrDefault(event.code(), CarrierWebhookRouter::handleUnrecognizedCode)
                .accept(event, ledger);
    }

    private static void handleUnrecognizedCode(CarrierWebhookEvent event, ShipmentLedger ledger) {
        ledger.record(event.trackingId(), UNRECOGNIZED_CODE_NOTE + ": " + event.code());
    }
}
