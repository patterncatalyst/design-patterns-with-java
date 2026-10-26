package com.patterncatalyst.designpatterns.ch12.shipping;

import java.util.Objects;

/**
 * The client of the {@link ShippingToolkit} family. Built from one toolkit,
 * once, in its constructor, so every document it produces afterward comes
 * from that same region's compliance scheme. Nothing in this class names
 * {@code NorthAmericaToolkit}, {@code EuropeanUnionToolkit}, or
 * {@code AsiaPacificToolkit}.
 */
public final class RegionalShippingDesk {
    private final LabelFormatter labelFormatter;
    private final RateTable rateTable;
    private final CustomsForm customsForm;

    public RegionalShippingDesk(ShippingToolkit toolkit) {
        Objects.requireNonNull(toolkit, "toolkit");
        this.labelFormatter = toolkit.labelFormatter();
        this.rateTable = toolkit.rateTable();
        this.customsForm = toolkit.customsForm();
    }

    public ProcessedShipment process(Shipment shipment) {
        Objects.requireNonNull(shipment, "shipment");
        return new ProcessedShipment(
                labelFormatter.formatLabel(shipment),
                rateTable.quote(shipment),
                customsForm.declare(shipment));
    }
}
