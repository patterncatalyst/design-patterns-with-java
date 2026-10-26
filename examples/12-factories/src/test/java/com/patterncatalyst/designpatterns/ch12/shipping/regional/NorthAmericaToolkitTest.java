package com.patterncatalyst.designpatterns.ch12.shipping.regional;

import com.patterncatalyst.designpatterns.ch12.shipping.Money;
import com.patterncatalyst.designpatterns.ch12.shipping.Region;
import com.patterncatalyst.designpatterns.ch12.shipping.Shipment;
import com.patterncatalyst.designpatterns.ch12.shipping.ShippingToolkit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies North America's family stays internally consistent: the same hub
 * code shows up in the label and the customs form, and the rate math matches
 * the flat-base-plus-per-kilogram rule.
 */
class NorthAmericaToolkitTest {

    private final ShippingToolkit toolkit = new NorthAmericaToolkit("hub-na-01");
    private final Shipment shipment = new Shipment("Canada", 4.2, 18_000, Region.NORTH_AMERICA);

    @Test
    void labelCarriesTheHubCode() {
        assertThat(toolkit.labelFormatter().formatLabel(shipment))
                .isEqualTo("NA-LABEL hub=hub-na-01 dest=Canada wt=4.2kg");
    }

    @Test
    void customsFormCarriesTheSameHubCode() {
        assertThat(toolkit.customsForm().declare(shipment))
                .isEqualTo("USMCA-CERT hub=hub-na-01 dest=Canada declaredValue=18000");
    }

    @Test
    void ratePricesByBasePlusPerKilogram() {
        assertThat(toolkit.rateTable().quote(shipment)).isEqualTo(Money.ofCents(1532));
    }
}
