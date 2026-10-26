package com.patterncatalyst.designpatterns.ch18.shipping.tracking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An observer that accumulates a list another component reads later, the
 * same long-lived-collaborator shape that earns {@link DashboardView} its
 * own class. {@code AlertingService} only reacts to one specific
 * transition, a status flipping into {@link ShipmentStatus#EXCEPTION}; every
 * other status change it is notified of is silently ignored.
 */
public final class AlertingService implements ShipmentStatusListener {

    private final List<String> alerts = new ArrayList<>();

    @Override
    public void onStatusChanged(String trackingId, ShipmentStatus oldStatus, ShipmentStatus newStatus) {
        if (newStatus == ShipmentStatus.EXCEPTION) {
            alerts.add(trackingId + " flagged: " + oldStatus + " -> " + newStatus);
        }
    }

    public List<String> alerts() {
        return Collections.unmodifiableList(alerts);
    }
}
