package com.patterncatalyst.designpatterns.ch12.shipping;

/**
 * A static factory method that decides which {@link ShipmentInspector}
 * subclass a shipment category needs. The switch expression is exhaustive
 * over {@link ShipmentCategory}: a third category added without a matching
 * branch here fails to compile, rather than falling through to a
 * {@code default} nobody wrote.
 */
public final class ShipmentInspectors {

    private ShipmentInspectors() {
    }

    public static ShipmentInspector forCategory(ShipmentCategory category) {
        return switch (category) {
            case STANDARD -> new StandardShipmentInspector();
            case HAZMAT -> new HazmatShipmentInspector();
        };
    }
}
