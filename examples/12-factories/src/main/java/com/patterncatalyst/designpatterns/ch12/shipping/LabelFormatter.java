package com.patterncatalyst.designpatterns.ch12.shipping;

/**
 * Formats a shipping label in one region's own format. A product role in the
 * {@link ShippingToolkit} family: {@code RegionalShippingDesk} depends on
 * this interface alone, never on which region produced an implementation.
 */
public interface LabelFormatter {
    String formatLabel(Shipment shipment);
}
