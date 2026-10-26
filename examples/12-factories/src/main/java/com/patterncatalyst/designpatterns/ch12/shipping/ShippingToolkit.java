package com.patterncatalyst.designpatterns.ch12.shipping;

/**
 * The Abstract Factory itself. One implementation is a promise that the
 * {@link LabelFormatter}, {@link RateTable}, and {@link CustomsForm} it
 * hands out all belong to the same region's compliance scheme. Nothing in
 * this interface names {@code NORTH_AMERICA}, {@code EUROPEAN_UNION}, or
 * {@code ASIA_PACIFIC}; a caller that holds a {@code ShippingToolkit} cannot
 * tell which region built it without asking the toolkit to build something.
 */
public interface ShippingToolkit {
    LabelFormatter labelFormatter();

    RateTable rateTable();

    CustomsForm customsForm();
}
