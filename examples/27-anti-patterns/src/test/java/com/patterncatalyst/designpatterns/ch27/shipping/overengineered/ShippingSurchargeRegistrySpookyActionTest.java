package com.patterncatalyst.designpatterns.ch27.shipping.overengineered;

import com.patterncatalyst.designpatterns.ch27.shipping.Money;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Forces the cost of {@link ShippingSurchargeRegistry} into the open: a
 * singleton configuration slot cannot hold two independently valid
 * surcharge rates at once, because there is only one slot.
 */
class ShippingSurchargeRegistrySpookyActionTest {

    @AfterEach
    void resetRegistry() {
        ShippingSurchargeRegistry.INSTANCE.setSurchargeMultiplier(1.0);
    }

    @Test
    void regionACalculatorChangesWhenRegionBConfiguresTheSharedRegistry() {
        RateCalculatorFactory factory = RateCalculatorFactoryProvider.INSTANCE.factory();

        ShippingSurchargeRegistry.INSTANCE.setSurchargeMultiplier(1.10);
        RateCalculator regionA = factory.createCalculator();
        assertThat(regionA.calculate(Money.ofCents(10_000))).isEqualTo(Money.ofCents(11_000));

        ShippingSurchargeRegistry.INSTANCE.setSurchargeMultiplier(1.30);
        RateCalculator regionB = factory.createCalculator();
        assertThat(regionB.calculate(Money.ofCents(10_000))).isEqualTo(Money.ofCents(13_000));

        // regionA was never touched directly, yet it now reports region B's
        // rate: spooky action at a distance through the shared registry slot.
        assertThat(regionA.calculate(Money.ofCents(10_000))).isEqualTo(Money.ofCents(13_000));
    }
}
