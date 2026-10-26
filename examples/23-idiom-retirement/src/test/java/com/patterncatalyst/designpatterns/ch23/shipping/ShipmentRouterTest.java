package com.patterncatalyst.designpatterns.ch23.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ShipmentRouterTest {

    private final ShipmentRouter router =
            new ShipmentRouter(new PreferredCarrierDirectory(Map.of("customer-42", "freight-zone")));

    @Test
    void routesToThePreferredCarrierWhenOneIsOnFile() {
        assertThat(router.carrierToUse("customer-42", "ground")).isEqualTo("FREIGHT-ZONE");
    }

    @Test
    void fallsBackToTheDefaultCarrierWhenNoPreferenceIsOnFile() {
        assertThat(router.carrierToUse("customer-99", "ground")).isEqualTo("GROUND");
    }
}
