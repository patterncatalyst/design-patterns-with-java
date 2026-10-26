package com.patterncatalyst.designpatterns.ch26.shipping;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Routes a {@link ShipmentEvent} to its handler through an {@link EnumMap}
 * keyed on the closed {@link ShipmentEventType}. The map itself is built
 * once, in the constructor, from {@link #handlerFor(ShipmentEventType)}, an
 * exhaustive switch expression with no {@code default} branch. A seventh
 * {@code ShipmentEventType} constant with no matching case in that switch
 * fails to compile; nothing here depends on a reviewer noticing a missing
 * row the way a hand-rolled array would.
 */
public final class EnumDispatchRouter {

    private final Map<ShipmentEventType, BiConsumer<ShipmentEvent, ShipmentLedger>> handlers;

    public EnumDispatchRouter() {
        this.handlers = Arrays.stream(ShipmentEventType.values())
                .collect(Collectors.toMap(
                        type -> type,
                        EnumDispatchRouter::handlerFor,
                        (first, second) -> first,
                        () -> new EnumMap<>(ShipmentEventType.class)));
    }

    public void route(ShipmentEvent event, ShipmentLedger ledger) {
        handlers.get(event.type()).accept(event, ledger);
    }

    private static BiConsumer<ShipmentEvent, ShipmentLedger> handlerFor(ShipmentEventType type) {
        return switch (type) {
            case CREATED -> ShipmentHandlers::handleCreated;
            case PICKED_UP -> ShipmentHandlers::handlePickedUp;
            case IN_TRANSIT -> ShipmentHandlers::handleInTransit;
            case OUT_FOR_DELIVERY -> ShipmentHandlers::handleOutForDelivery;
            case DELIVERED -> ShipmentHandlers::handleDelivered;
            case EXCEPTION -> ShipmentHandlers::handleException;
        };
    }
}
