package com.patterncatalyst.designpatterns.ch22.fulfillment;

import java.util.List;
import java.util.Objects;

/**
 * Holds a queue of {@link NotificationCommand} values and runs every one of
 * them against a shipment, without knowing or caring whether a given command
 * is a method reference, a lambda, or a named class. The queue itself is the
 * only state this type owns; the behavior that runs lives entirely in the
 * commands it was handed.
 */
public final class FulfillmentNotifier {

    private final List<NotificationCommand> commands;

    public FulfillmentNotifier(List<NotificationCommand> commands) {
        this.commands = List.copyOf(Objects.requireNonNull(commands, "commands"));
    }

    public void dispatchAll(Shipment shipment) {
        Objects.requireNonNull(shipment, "shipment");
        for (NotificationCommand command : commands) {
            command.dispatch(shipment);
        }
    }

    public int commandCount() {
        return commands.size();
    }
}
