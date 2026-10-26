package com.patterncatalyst.designpatterns.ch12.shipping;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The checklist a hazardous-materials shipment runs: a placard check and a
 * segregation-group check that a standard shipment never needs, plus an
 * embargo check that a standard shipment's checklist has no reason to run.
 */
public final class HazmatInspectionChecklist implements InspectionChecklist {
    private static final Set<String> EMBARGOED_DESTINATIONS = Set.of("North Korea");

    @Override
    public List<String> run(Shipment shipment) {
        List<String> log = new ArrayList<>();
        log.add("verified hazardous materials placard is attached and legible");
        log.add("confirmed segregation group compatibility for a "
                + shipment.weightKg() + "kg hazardous parcel");
        if (EMBARGOED_DESTINATIONS.contains(shipment.destinationCountry())) {
            log.add("FAILED: destination embargoes hazardous imports");
        } else {
            log.add("confirmed destination accepts hazardous imports");
        }
        return log;
    }
}
