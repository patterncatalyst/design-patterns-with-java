package com.patterncatalyst.designpatterns.ch23.shipping;

import java.util.Locale;
import java.util.Objects;

/**
 * Decides which carrier handles a customer's shipment. The method body
 * never checks for {@code null} and never asks whether the thing it got
 * back is a real preference or a stand-in; it reads as a straight line
 * because {@link PreferredCarrierDirectory#preferredCarrierFor(String)}
 * already carries "might not be there" in its return type.
 */
public final class ShipmentRouter {

    private final PreferredCarrierDirectory directory;

    public ShipmentRouter(PreferredCarrierDirectory directory) {
        this.directory = Objects.requireNonNull(directory, "directory");
    }

    public String carrierToUse(String customerId, String defaultCarrier) {
        return directory.preferredCarrierFor(customerId)
                .map(carrier -> carrier.toUpperCase(Locale.ROOT))
                .orElse(defaultCarrier.toUpperCase(Locale.ROOT));
    }
}
