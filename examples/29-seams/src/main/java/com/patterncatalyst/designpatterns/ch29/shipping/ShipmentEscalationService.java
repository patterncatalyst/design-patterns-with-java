package com.patterncatalyst.designpatterns.ch29.shipping;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * {@link LegacyShipmentEscalationService} refactored through two seams. The
 * wall-clock read becomes a {@link Clock} the constructor takes, the object
 * seam; and the carrier notifier becomes a {@link CarrierGateway} the
 * constructor also takes instead of constructing one inline, the
 * extract-interface seam. Production code wires in {@code Clock.systemUTC()}
 * and {@link SmtpCarrierGateway}; a test wires in {@code Clock.fixed(...)}
 * and a Mockito mock.
 *
 * <p>The comparison also changed, from {@code now.isAfter(overdueAt)} to
 * {@code !now.isBefore(overdueAt)}: the legacy service treated the exact
 * instant a grace period elapses as still on time, a boundary nobody could
 * even test before this seam existed. The wide-margin cases this class
 * agrees with the legacy service on prove the seam itself changed nothing;
 * the exact-boundary tests pin the one behavior corrected once the boundary
 * became reachable.
 */
public final class ShipmentEscalationService {

    private static final Duration OVERDUE_GRACE = Duration.ofHours(2);

    private final Clock clock;
    private final CarrierGateway gateway;

    public ShipmentEscalationService(Clock clock, CarrierGateway gateway) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    public EscalationResult escalate(Shipment shipment) {
        Instant now = clock.instant();
        Instant overdueAt = shipment.promisedDelivery().plus(OVERDUE_GRACE);
        if (!now.isBefore(overdueAt)) {
            String reason = "promised delivery window missed by more than "
                    + OVERDUE_GRACE.toHours() + "h";
            gateway.notifyCarrier(shipment, reason);
            return EscalationResult.escalated(shipment, reason);
        }
        return EscalationResult.notOverdue(shipment);
    }
}
