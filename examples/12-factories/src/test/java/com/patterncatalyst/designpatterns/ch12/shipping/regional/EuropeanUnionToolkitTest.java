package com.patterncatalyst.designpatterns.ch12.shipping.regional;

import com.patterncatalyst.designpatterns.ch12.shipping.Money;
import com.patterncatalyst.designpatterns.ch12.shipping.Region;
import com.patterncatalyst.designpatterns.ch12.shipping.Shipment;
import com.patterncatalyst.designpatterns.ch12.shipping.ShippingToolkit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the European Union's family stays internally consistent and that
 * its rate folds in the VAT handling fee on top of the weight-tiered base.
 */
class EuropeanUnionToolkitTest {

    private final ShippingToolkit toolkit = new EuropeanUnionToolkit("hub-eu-07");
    private final Shipment shipment = new Shipment("Germany", 4.2, 18_000, Region.EUROPEAN_UNION);

    @Test
    void labelCarriesTheHubCode() {
        assertThat(toolkit.labelFormatter().formatLabel(shipment))
                .isEqualTo("EU-LABEL hub:hub-eu-07 to:Germany weight:4.2");
    }

    @Test
    void customsFormCarriesTheSameHubCode() {
        assertThat(toolkit.customsForm().declare(shipment))
                .isEqualTo("EORI-DECLARATION hub:hub-eu-07 dest:Germany value:18000");
    }

    @Test
    void rateFoldsInTheVatHandlingFee() {
        // tiered: 480 + round(260 * 4.2) = 480 + 1092 = 1572
        // vat handling: round(18_000 * 0.02) = 360
        assertThat(toolkit.rateTable().quote(shipment)).isEqualTo(Money.ofCents(1932));
    }
}
