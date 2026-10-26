package com.patterncatalyst.designpatterns.ch12.shipping;

/**
 * Plugs {@link StandardInspectionChecklist} into {@link ShipmentInspector}'s
 * fixed sequence. The sequence itself is inherited unchanged.
 */
public final class StandardShipmentInspector extends ShipmentInspector {
    @Override
    protected InspectionChecklist createChecklist() {
        return new StandardInspectionChecklist();
    }
}
