package com.patterncatalyst.designpatterns.ch12.shipping.regional;

import com.patterncatalyst.designpatterns.ch12.shipping.Money;
import com.patterncatalyst.designpatterns.ch12.shipping.Region;
import com.patterncatalyst.designpatterns.ch12.shipping.Shipment;
import com.patterncatalyst.designpatterns.ch12.shipping.ShippingToolkit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Asia-Pacific's family stays internally consistent and that its
 * rate steps up past the declared-value threshold.
 */
class AsiaPacificToolkitTest {

    private final ShippingToolkit toolkit = new AsiaPacificToolkit("hub-apac-14");

    @Test
    void labelCarriesTheHubCode() {
        Shipment shipment = new Shipment("Japan", 12.0, 92_000, Region.ASIA_PACIFIC);

        assertThat(toolkit.labelFormatter().formatLabel(shipment))
                .isEqualTo("APAC-MANIFEST hub=hub-apac-14 dest=Japan wt=12.0kg");
    }

    @Test
    void customsFormCarriesTheSameHubCode() {
        Shipment shipment = new Shipment("Japan", 12.0, 92_000, Region.ASIA_PACIFIC);

        assertThat(toolkit.customsForm().declare(shipment))
                .isEqualTo("HS-TARIFF-DECLARATION hub=hub-apac-14 dest=Japan value=92000");
    }

    @Test
    void ratesBelowTheDeclaredValueThresholdAtTheLowBase() {
        Shipment lowValueShipment = new Shipment("Japan", 2.0, 10_000, Region.ASIA_PACIFIC);

        // 420 + round(190 * 2.0) = 420 + 380 = 800
        assertThat(toolkit.rateTable().quote(lowValueShipment)).isEqualTo(Money.ofCents(800));
    }

    @Test
    void ratesAtOrAboveTheDeclaredValueThresholdAtTheHighBase() {
        Shipment highValueShipment = new Shipment("Japan", 12.0, 92_000, Region.ASIA_PACIFIC);

        // 710 + round(190 * 12.0) = 710 + 2280 = 2990
        assertThat(toolkit.rateTable().quote(highValueShipment)).isEqualTo(Money.ofCents(2990));
    }
}
