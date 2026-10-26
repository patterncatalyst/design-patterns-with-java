package com.patterncatalyst.designpatterns.ch23.shipping;

import java.time.Duration;

/**
 * Turns a {@link ShipmentEvent} into customer-facing text and into a
 * notification decision, both by pattern matching directly on the sealed
 * hierarchy's shape. Neither method has a {@code default} arm. The
 * compiler proves every {@code permits} case is covered, the same
 * completeness guarantee a Visitor's double dispatch or an
 * {@code instanceof}-cast chain never gave an author for free.
 */
public final class ShipmentEventNarrator {

    private ShipmentEventNarrator() {}

    public static String describe(ShipmentEvent event) {
        return switch (event) {
            case ShipmentEvent.PickedUp(var at, var facility) ->
                    "picked up at " + facility + " on " + at;
            case ShipmentEvent.InTransit(var at, var facility) ->
                    "in transit, last seen at " + facility + " on " + at;
            case ShipmentEvent.Delayed(var at, var reason, var extra) ->
                    "delayed (" + reason + "), expect an extra "
                            + extra.toMinutes() + " minutes, reported " + at;
            case ShipmentEvent.Delivered(var at, var signedBy) ->
                    "delivered on " + at + ", signed by " + signedBy;
            case ShipmentEvent.Lost(var at, var facility) ->
                    "lost, last known location " + facility + " as of " + at;
        };
    }

    /**
     * A customer gets proactively notified for a delay longer than two
     * hours, or once the shipment is delivered or lost. The guarded
     * {@code Delayed} pattern reads the threshold directly out of the
     * record's own component, no separate getter call needed to reach it.
     */
    public static boolean requiresCustomerNotification(ShipmentEvent event) {
        return switch (event) {
            case ShipmentEvent.PickedUp ignored -> false;
            case ShipmentEvent.InTransit ignored -> false;
            case ShipmentEvent.Delayed(var at, var reason, var extra)
                    when extra.compareTo(Duration.ofHours(2)) > 0 -> true;
            case ShipmentEvent.Delayed ignored -> false;
            case ShipmentEvent.Delivered ignored -> true;
            case ShipmentEvent.Lost ignored -> true;
        };
    }
}
