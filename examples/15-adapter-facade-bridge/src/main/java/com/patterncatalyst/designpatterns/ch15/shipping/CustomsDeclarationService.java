package com.patterncatalyst.designpatterns.ch15.shipping;

import java.util.Optional;

/**
 * Decides whether a shipment needs a customs declaration and builds one when
 * it does. Only {@link DestinationZone#INTERNATIONAL} shipments need one;
 * every other zone gets {@link Optional#empty()}, which is what lets
 * {@link ShippingFacade} treat "no customs step for this shipment" as a plain
 * value instead of a null check scattered across callers.
 */
public final class CustomsDeclarationService {

    public Optional<CustomsDeclaration> declareIfNeeded(Shipment shipment) {
        if (shipment.zone() != DestinationZone.INTERNATIONAL) {
            return Optional.empty();
        }
        Money declaredValue = Money.ofCents(shipment.declaredValueCents());
        String note = "General merchandise, declared value " + declaredValue;
        return Optional.of(new CustomsDeclaration(declaredValue, note));
    }
}
