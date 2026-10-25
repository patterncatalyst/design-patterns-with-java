package com.patterncatalyst.designpatterns.ch10.shipment.state;

import com.patterncatalyst.designpatterns.ch10.shipment.IllegalTransitionException;
import com.patterncatalyst.designpatterns.ch10.shipment.ShipmentEvent;

/**
 * The same lifecycle as {@code EnumShipmentMachine}, expressed as a sealed
 * {@link ShipmentState} dispatched through a pattern-matching {@code switch}.
 * Every event method switches over all seven permitted types with no
 * {@code default} branch, so the compiler refuses to build this file the
 * moment an eighth state type is added to {@link ShipmentState}'s
 * {@code permits} clause without a matching case here.
 */
public final class SealedShipmentMachine {

    private ShipmentState state;

    public SealedShipmentMachine() {
        this.state = new Created();
    }

    public ShipmentState state() {
        return state;
    }

    public void pack() {
        state = switch (state) {
            case Created c -> new Packed();
            case Packed p -> throw reject(ShipmentEvent.PACK);
            case InTransit t -> throw reject(ShipmentEvent.PACK);
            case Delivered d -> throw reject(ShipmentEvent.PACK);
            case Cancelled c -> throw reject(ShipmentEvent.PACK);
            case Returned r -> throw reject(ShipmentEvent.PACK);
            case Lost l -> throw reject(ShipmentEvent.PACK);
        };
    }

    public void ship(String carrierTrackingNumber) {
        state = switch (state) {
            case Created c -> throw reject(ShipmentEvent.SHIP);
            case Packed p -> new InTransit(carrierTrackingNumber);
            case InTransit t -> throw reject(ShipmentEvent.SHIP);
            case Delivered d -> throw reject(ShipmentEvent.SHIP);
            case Cancelled c -> throw reject(ShipmentEvent.SHIP);
            case Returned r -> throw reject(ShipmentEvent.SHIP);
            case Lost l -> throw reject(ShipmentEvent.SHIP);
        };
    }

    public void deliver() {
        state = switch (state) {
            case Created c -> throw reject(ShipmentEvent.DELIVER);
            case Packed p -> throw reject(ShipmentEvent.DELIVER);
            case InTransit t -> new Delivered();
            case Delivered d -> throw reject(ShipmentEvent.DELIVER);
            case Cancelled c -> throw reject(ShipmentEvent.DELIVER);
            case Returned r -> throw reject(ShipmentEvent.DELIVER);
            case Lost l -> throw reject(ShipmentEvent.DELIVER);
        };
    }

    public void cancel(String reason) {
        state = switch (state) {
            case Created c -> new Cancelled(reason);
            case Packed p -> new Cancelled(reason);
            case InTransit t -> throw reject(ShipmentEvent.CANCEL);
            case Delivered d -> throw reject(ShipmentEvent.CANCEL);
            case Cancelled c -> throw reject(ShipmentEvent.CANCEL);
            case Returned r -> throw reject(ShipmentEvent.CANCEL);
            case Lost l -> throw reject(ShipmentEvent.CANCEL);
        };
    }

    public void returnToSender(String reason) {
        state = switch (state) {
            case Created c -> throw reject(ShipmentEvent.RETURN_TO_SENDER);
            case Packed p -> throw reject(ShipmentEvent.RETURN_TO_SENDER);
            case InTransit t -> new Returned(reason);
            case Delivered d -> throw reject(ShipmentEvent.RETURN_TO_SENDER);
            case Cancelled c -> throw reject(ShipmentEvent.RETURN_TO_SENDER);
            case Returned r -> throw reject(ShipmentEvent.RETURN_TO_SENDER);
            case Lost l -> throw reject(ShipmentEvent.RETURN_TO_SENDER);
        };
    }

    /**
     * Exhaustive over every permitted type, with no {@code default}: the
     * four terminal statuses return {@code true}, the three that can still
     * accept an event return {@code false}. Adding a new {@code ShipmentState}
     * without extending this method is a compile error, not a silent
     * fall-through.
     */
    public boolean isTerminal() {
        return switch (state) {
            case Delivered d -> true;
            case Cancelled c -> true;
            case Returned r -> true;
            case Lost l -> true;
            case Created c -> false;
            case Packed p -> false;
            case InTransit t -> false;
        };
    }

    private IllegalTransitionException reject(ShipmentEvent event) {
        return new IllegalTransitionException(state.status(), event);
    }
}
