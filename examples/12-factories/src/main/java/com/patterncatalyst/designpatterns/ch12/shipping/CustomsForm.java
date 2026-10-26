package com.patterncatalyst.designpatterns.ch12.shipping;

/**
 * Declares a shipment to customs under one region's own compliance scheme. A
 * product role in the {@link ShippingToolkit} family.
 */
public interface CustomsForm {
    String declare(Shipment shipment);
}
