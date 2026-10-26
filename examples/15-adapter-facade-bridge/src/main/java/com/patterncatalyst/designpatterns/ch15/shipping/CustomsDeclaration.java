package com.patterncatalyst.designpatterns.ch15.shipping;

import java.util.Objects;

/**
 * A customs declaration attached to an international shipment: the declared
 * value and a note describing the contents.
 *
 * @param declaredValue the declared value, exact to the cent
 * @param note a short description a customs form can print
 */
public record CustomsDeclaration(Money declaredValue, String note) {

    public CustomsDeclaration {
        Objects.requireNonNull(declaredValue, "declaredValue");
        Objects.requireNonNull(note, "note");
    }
}
