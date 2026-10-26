package com.patterncatalyst.designpatterns.ch12.shipping;

/**
 * Prices a shipment under one region's own rate rule. A product role in the
 * {@link ShippingToolkit} family.
 */
public interface RateTable {
    Money quote(Shipment shipment);
}
