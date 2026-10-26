package com.patterncatalyst.designpatterns.ch28.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OperationLoggingInterceptorTest {

    @Inject
    CheckoutService checkoutService;

    @Inject
    InvocationLog invocationLog;

    @Test
    void interceptorWrapsTheBoundMethodWithoutCheckoutServiceKnowingItExists() {
        checkoutService.quote(new Shipment("TRK-2001", 2.0, 0));

        assertThat(invocationLog.entries())
                .anyMatch(entry -> entry.startsWith("quote returned") && entry.endsWith("us"));
    }
}
