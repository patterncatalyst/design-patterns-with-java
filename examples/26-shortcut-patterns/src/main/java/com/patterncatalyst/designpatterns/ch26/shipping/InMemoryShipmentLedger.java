package com.patterncatalyst.designpatterns.ch26.shipping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A simple in-memory {@link ShipmentLedger} for the demo and the test
 * suite. Each entry is one line, {@code "<trackingId>: <note>"}, in the
 * order it was recorded.
 */
public final class InMemoryShipmentLedger implements ShipmentLedger {

    private final List<String> entries = new ArrayList<>();

    @Override
    public void record(String trackingId, String note) {
        entries.add(trackingId + ": " + note);
    }

    public List<String> entries() {
        return Collections.unmodifiableList(entries);
    }
}
