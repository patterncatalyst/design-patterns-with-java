package com.patterncatalyst.designpatterns.ch29.shipping;

import java.time.Duration;
import java.time.Instant;

/**
 * Inherited exactly as found. {@link #escalate(Shipment)} reads
 * {@link Instant#now()} directly, and it constructs its own
 * {@link SmtpCarrierGateway} on every call instead of being handed one.
 * Neither hard dependency has a way in for a test to substitute a double,
 * which is the shape this chapter opens a seam on. This class stays
 * unedited; it is the historical record the characterization tests pin, so
 * a reviewer can compare it against {@link ShipmentEscalationService} and
 * see exactly what changed.
 */
public class LegacyShipmentEscalationService {

    private static final Duration OVERDUE_GRACE = Duration.ofHours(2);

    public EscalationResult escalate(Shipment shipment) {
        Instant now = Instant.now();
        Instant overdueAt = shipment.promisedDelivery().plus(OVERDUE_GRACE);
        if (now.isAfter(overdueAt)) {
            CarrierGateway gateway = new SmtpCarrierGateway();
            String reason = "promised delivery window missed by more than "
                    + OVERDUE_GRACE.toHours() + "h";
            gateway.notifyCarrier(shipment, reason);
            return EscalationResult.escalated(shipment, reason);
        }
        return EscalationResult.notOverdue(shipment);
    }
}
