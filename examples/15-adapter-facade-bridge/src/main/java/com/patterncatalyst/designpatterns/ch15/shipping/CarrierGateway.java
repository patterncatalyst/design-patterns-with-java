package com.patterncatalyst.designpatterns.ch15.shipping;

/**
 * The target interface: the one shape checkout code, and the
 * {@link ShippingFacade} built on top of it, actually depend on. It says
 * nothing about GlobalCourier, grams, or zone codes, which is what lets a
 * different carrier's adapter stand in without touching a caller.
 */
public interface CarrierGateway {

    Money quote(Shipment shipment);
}
