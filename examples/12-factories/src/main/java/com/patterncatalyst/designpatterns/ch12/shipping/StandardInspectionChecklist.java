package com.patterncatalyst.designpatterns.ch12.shipping;

import java.util.ArrayList;
import java.util.List;

/**
 * The checklist a standard, non-hazardous shipment runs: no placard or
 * segregation checks, because there is nothing hazardous to segregate.
 */
public final class StandardInspectionChecklist implements InspectionChecklist {
    @Override
    public List<String> run(Shipment shipment) {
        List<String> log = new ArrayList<>();
        log.add("verified declared value is within the standard handling limit");
        log.add("confirmed packaging integrity for a " + shipment.weightKg() + "kg parcel");
        return log;
    }
}
