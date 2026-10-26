package com.patterncatalyst.designpatterns.ch12.shipping;

/**
 * The three documents a shipment needs before it leaves a hub, all built by
 * the same {@link ShippingToolkit} call.
 */
public record ProcessedShipment(String label, Money rate, String customsDeclaration) {
}
