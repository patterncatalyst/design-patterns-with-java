package com.patterncatalyst.designpatterns.ch17.shipping;

/**
 * The recursion Composite exists to write exactly once. Every method here
 * dispatches on {@link ShipmentNode} with a pattern-matching {@code switch}
 * that the compiler checks for exhaustiveness against the sealed {@code
 * permits} list, with neither case asking what kind of tree it is standing
 * in before recursing into it.
 */
public final class ShipmentNodes {

    private ShipmentNodes() {
    }

    public static long totalWeightGrams(ShipmentNode node) {
        return switch (node) {
            case Parcel parcel -> parcel.weightGrams();
            case ShipmentBox box -> box.contents().stream().mapToLong(ShipmentNodes::totalWeightGrams).sum();
        };
    }

    public static long totalValueCents(ShipmentNode node) {
        return switch (node) {
            case Parcel parcel -> parcel.valueCents();
            case ShipmentBox box -> box.contents().stream().mapToLong(ShipmentNodes::totalValueCents).sum();
        };
    }

    public static int itemCount(ShipmentNode node) {
        return switch (node) {
            case Parcel parcel -> 1;
            case ShipmentBox box -> box.contents().stream().mapToInt(ShipmentNodes::itemCount).sum();
        };
    }
}
