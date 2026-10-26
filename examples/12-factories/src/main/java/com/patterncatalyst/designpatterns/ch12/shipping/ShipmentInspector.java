package com.patterncatalyst.designpatterns.ch12.shipping;

import java.util.List;
import java.util.Objects;

/**
 * A fixed inspection sequence with exactly one varying step. {@link #inspect}
 * is {@code final}: every subclass validates, runs a checklist, and judges
 * pass or fail the same way. {@link #createChecklist()} is the Factory
 * Method, a hook each subclass overrides to decide the one product,
 * {@link InspectionChecklist}, that the sequence runs.
 */
public abstract class ShipmentInspector {

    public final InspectionResult inspect(Shipment shipment) {
        Objects.requireNonNull(shipment, "shipment");

        InspectionChecklist checklist = createChecklist();
        List<String> log = checklist.run(shipment);
        boolean passed = log.stream().noneMatch(line -> line.contains("FAILED"));

        return new InspectionResult(passed, List.copyOf(log));
    }

    protected abstract InspectionChecklist createChecklist();
}
