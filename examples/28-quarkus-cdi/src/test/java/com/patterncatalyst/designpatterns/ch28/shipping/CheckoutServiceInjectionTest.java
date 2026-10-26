package com.patterncatalyst.designpatterns.ch28.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CheckoutServiceInjectionTest {

    @Inject
    CheckoutService checkoutService;

    @Inject
    Instance<CheckoutService> checkoutServiceInstance;

    @Test
    void containerResolvesCheckoutServiceWithNoFactoryCodeWritten() {
        assertThat(checkoutService).isNotNull();

        RateQuote quote = checkoutService.quote(new Shipment("TRK-9001", 3.0, 12_000));

        assertThat(quote.quotedBy()).isEqualTo("weight-tiered");
    }

    @Test
    void applicationScopedBeanIsTheSameInstanceOnEveryResolution() {
        CheckoutService first = checkoutServiceInstance.get();
        CheckoutService second = checkoutServiceInstance.get();

        assertThat(first).isSameAs(second);
    }
}
