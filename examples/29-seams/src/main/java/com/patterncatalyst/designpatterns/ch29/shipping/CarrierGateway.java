package com.patterncatalyst.designpatterns.ch29.shipping;

/**
 * The extract-interface seam: the contract {@link ShipmentEscalationService}
 * depends on instead of constructing a concrete notifier itself. A test
 * substitutes a Mockito mock through this interface; production code wires
 * in {@link SmtpCarrierGateway}.
 */
public interface CarrierGateway {

    void notifyCarrier(Shipment shipment, String reason);
}
