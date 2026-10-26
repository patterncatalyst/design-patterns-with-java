package com.patterncatalyst.designpatterns.ch17.shipping;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The Composite branch: a box holding any mix of {@link Parcel} leaves and
 * nested {@code ShipmentBox} branches. {@code alertHandler} is always bound,
 * real or a {@link NullShipmentAlertHandler}, so there is no third state,
 * "handler not yet decided," for a bug to hide in.
 */
public record ShipmentBox(String label, List<ShipmentNode> contents, long weightLimitGrams,
                           ShipmentAlertHandler alertHandler) implements ShipmentNode {

    public ShipmentBox {
        Objects.requireNonNull(label, "label");
        contents = List.copyOf(contents);
        Objects.requireNonNull(alertHandler, "alertHandler");
        if (weightLimitGrams < 0) {
            throw new IllegalArgumentException("weightLimitGrams cannot be negative: " + weightLimitGrams);
        }
    }

    /**
     * Most boxes in this domain never get a real alert handler wired up;
     * this overload binds the Null Object so every other constructor and
     * every call site can ignore the distinction entirely.
     */
    public ShipmentBox(String label, List<ShipmentNode> contents, long weightLimitGrams) {
        this(label, contents, weightLimitGrams, NullShipmentAlertHandler.INSTANCE);
    }

    /**
     * An Optional accepted at a construction boundary and resolved
     * immediately into a concrete {@link ShipmentAlertHandler}. This is
     * where Optional earns its keep: once resolved, {@code alertHandler} is
     * never Optional, so {@link #checkWeightLimit()} still has no branch to
     * get wrong.
     */
    public static ShipmentBox withAlertHandler(String label, List<ShipmentNode> contents, long weightLimitGrams,
                                                Optional<ShipmentAlertHandler> alertHandler) {
        return new ShipmentBox(label, contents, weightLimitGrams, alertHandler.orElse(NullShipmentAlertHandler.INSTANCE));
    }

    public void checkWeightLimit() {
        long total = ShipmentNodes.totalWeightGrams(this);
        if (total > weightLimitGrams) {
            alertHandler.onOverweight(label, total - weightLimitGrams);
        }
    }
}
