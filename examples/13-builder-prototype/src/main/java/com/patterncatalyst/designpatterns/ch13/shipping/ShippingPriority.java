package com.patterncatalyst.designpatterns.ch13.shipping;

/**
 * A shipment's priority tier. {@link #STANDARD} is the default a
 * {@link Shipment.Builder} applies when a caller never sets this field.
 */
public enum ShippingPriority {
    STANDARD,
    EXPEDITED,
    OVERNIGHT
}
