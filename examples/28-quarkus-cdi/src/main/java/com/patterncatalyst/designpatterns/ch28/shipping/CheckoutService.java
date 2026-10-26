package com.patterncatalyst.designpatterns.ch28.shipping;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import java.util.Objects;

/**
 * The checkout-side collaborator every mechanism in this example meets at.
 * {@code @ApplicationScoped} makes Arc construct exactly one instance for
 * the application's lifetime and hand the same reference to every
 * injection point, the Singleton guarantee the rest of this book built by
 * hand with an enum or a holder class. The constructor never calls
 * {@code new} on a {@link RateQuoter} and never reaches for a static
 * accessor; the container resolves both dependencies and wires them in,
 * which is what makes the container the composition root.
 */
@ApplicationScoped
public class CheckoutService {

    private final RateQuoter rateQuoter;
    private final Event<ShipmentStatusChanged> statusChangedEvent;

    @Inject
    public CheckoutService(RateQuoter rateQuoter, Event<ShipmentStatusChanged> statusChangedEvent) {
        this.rateQuoter = Objects.requireNonNull(rateQuoter, "rateQuoter");
        this.statusChangedEvent = Objects.requireNonNull(statusChangedEvent, "statusChangedEvent");
    }

    @LoggedOperation
    public RateQuote quote(Shipment shipment) {
        return rateQuoter.quote(shipment);
    }

    public void dispatch(String trackingId) {
        statusChangedEvent.fire(new ShipmentStatusChanged(trackingId, "CREATED", "DISPATCHED"));
    }
}
