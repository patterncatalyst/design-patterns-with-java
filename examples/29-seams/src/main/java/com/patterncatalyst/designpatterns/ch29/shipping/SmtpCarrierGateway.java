package com.patterncatalyst.designpatterns.ch29.shipping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The only concrete {@link CarrierGateway} production code ever constructs.
 * A real implementation would hand a message to an SMTP relay or a carrier
 * API; this one records what it was asked to send, which is enough to
 * demonstrate the seam without reaching out to a real network dependency
 * anywhere in this book's build.
 */
public final class SmtpCarrierGateway implements CarrierGateway {

    private final List<String> sentNotifications = new ArrayList<>();

    @Override
    public void notifyCarrier(Shipment shipment, String reason) {
        sentNotifications.add(shipment.id() + ": " + reason);
    }

    public List<String> sentNotifications() {
        return Collections.unmodifiableList(sentNotifications);
    }
}
