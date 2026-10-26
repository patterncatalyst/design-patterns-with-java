package com.patterncatalyst.designpatterns.ch12.shipping;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Verifies the guarantee {@code RegionalShippingDesk} exists to provide: all
 * three products come from exactly one {@link ShippingToolkit} call, made
 * once in the constructor, never per shipment processed afterward.
 */
@ExtendWith(MockitoExtension.class)
class RegionalShippingDeskTest {

    @Mock
    private ShippingToolkit toolkit;

    @Mock
    private LabelFormatter labelFormatter;

    @Mock
    private RateTable rateTable;

    @Mock
    private CustomsForm customsForm;

    @Test
    void buildsAllThreeProductsFromOneFactoryCallInTheConstructor() {
        when(toolkit.labelFormatter()).thenReturn(labelFormatter);
        when(toolkit.rateTable()).thenReturn(rateTable);
        when(toolkit.customsForm()).thenReturn(customsForm);

        new RegionalShippingDesk(toolkit);

        verify(toolkit).labelFormatter();
        verify(toolkit).rateTable();
        verify(toolkit).customsForm();
        verifyNoMoreInteractions(toolkit);
    }

    @Test
    void processDelegatesToTheProductsBuiltAtConstructionWithoutCallingTheFactoryAgain() {
        when(toolkit.labelFormatter()).thenReturn(labelFormatter);
        when(toolkit.rateTable()).thenReturn(rateTable);
        when(toolkit.customsForm()).thenReturn(customsForm);

        Shipment shipment = new Shipment("Canada", 4.2, 18_000, Region.NORTH_AMERICA);
        when(labelFormatter.formatLabel(shipment)).thenReturn("STUB-LABEL");
        when(rateTable.quote(shipment)).thenReturn(Money.ofCents(1234));
        when(customsForm.declare(shipment)).thenReturn("STUB-CUSTOMS");

        RegionalShippingDesk desk = new RegionalShippingDesk(toolkit);
        ProcessedShipment first = desk.process(shipment);
        ProcessedShipment second = desk.process(shipment);

        assertThat(first).isEqualTo(new ProcessedShipment("STUB-LABEL", Money.ofCents(1234), "STUB-CUSTOMS"));
        assertThat(second).isEqualTo(first);

        // Exactly one call each, from the constructor; process() never asks
        // the toolkit for a product a second time.
        verify(toolkit).labelFormatter();
        verify(toolkit).rateTable();
        verify(toolkit).customsForm();
    }
}
