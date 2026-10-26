package com.patterncatalyst.designpatterns.ch12.shipping;

import java.util.List;

/**
 * The outcome of one {@link ShipmentInspector#inspect(Shipment)} run.
 */
public record InspectionResult(boolean passed, List<String> log) {
}
