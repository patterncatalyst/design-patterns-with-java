package com.patterncatalyst.designpatterns.ch12.shipping;

/**
 * Plugs {@link HazmatInspectionChecklist} into {@link ShipmentInspector}'s
 * fixed sequence. The sequence itself is inherited unchanged; only which
 * checklist runs differs from {@link StandardShipmentInspector}.
 */
public final class HazmatShipmentInspector extends ShipmentInspector {
    @Override
    protected InspectionChecklist createChecklist() {
        return new HazmatInspectionChecklist();
    }
}
