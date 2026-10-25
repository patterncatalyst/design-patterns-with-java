package com.patterncatalyst.designpatterns.ch10.shipment;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * A data-driven finite state machine: every legal (status, event) pair is a
 * single entry in {@link #TRANSITIONS}, a plain value a reviewer reads as
 * data with no class to open and no branch to trace. {@link #apply} does
 * nothing but look up the current status and event and throw
 * {@link IllegalTransitionException} when the lookup misses.
 */
public final class EnumShipmentMachine {

    private static final Map<ShipmentStatus, Map<ShipmentEvent, ShipmentStatus>> TRANSITIONS = buildTransitions();

    private ShipmentStatus status;

    public EnumShipmentMachine() {
        this(ShipmentStatus.CREATED);
    }

    public EnumShipmentMachine(ShipmentStatus initial) {
        this.status = Objects.requireNonNull(initial, "initial");
    }

    public ShipmentStatus status() {
        return status;
    }

    public void apply(ShipmentEvent event) {
        Map<ShipmentEvent, ShipmentStatus> legalFromHere = TRANSITIONS.getOrDefault(status, Map.of());
        ShipmentStatus next = legalFromHere.get(event);
        if (next == null) {
            throw new IllegalTransitionException(status, event);
        }
        status = next;
    }

    private static Map<ShipmentStatus, Map<ShipmentEvent, ShipmentStatus>> buildTransitions() {
        Map<ShipmentStatus, Map<ShipmentEvent, ShipmentStatus>> table = new EnumMap<>(ShipmentStatus.class);
        table.put(ShipmentStatus.CREATED, Map.of(
                ShipmentEvent.PACK, ShipmentStatus.PACKED,
                ShipmentEvent.CANCEL, ShipmentStatus.CANCELLED));
        table.put(ShipmentStatus.PACKED, Map.of(
                ShipmentEvent.SHIP, ShipmentStatus.IN_TRANSIT,
                ShipmentEvent.CANCEL, ShipmentStatus.CANCELLED));
        table.put(ShipmentStatus.IN_TRANSIT, Map.of(
                ShipmentEvent.DELIVER, ShipmentStatus.DELIVERED,
                ShipmentEvent.RETURN_TO_SENDER, ShipmentStatus.RETURNED));
        return Map.copyOf(table);
    }
}
