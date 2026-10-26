package com.patterncatalyst.designpatterns.ch12.shipping;

import java.util.List;

/**
 * One product in a {@link ShipmentInspector}'s inspection sequence: the
 * single varying step, not a family. A line containing {@code "FAILED"}
 * marks the checklist as failed; every other line is an informational
 * record of a check that passed.
 */
public interface InspectionChecklist {
    List<String> run(Shipment shipment);
}
