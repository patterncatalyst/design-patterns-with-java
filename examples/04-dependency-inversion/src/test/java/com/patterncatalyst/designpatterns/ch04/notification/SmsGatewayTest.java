package com.patterncatalyst.designpatterns.ch04.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SmsGatewayTest {

    @Test
    void sendAlwaysReturnsSentWithAChannelPrefixedDispatchId() {
        SmsGateway gateway = new SmsGateway();
        OrderEvent event = new OrderEvent("ORD-5001", "+1-555-0100", "Your order has shipped.");

        DispatchResult result = gateway.send(event);

        assertThat(result.status()).isEqualTo(DispatchStatus.SENT);
        assertThat(result.dispatchId()).isEqualTo("sms-ORD-5001");
    }

    @Test
    void dispatchIdsAreUniquePerOrder() {
        SmsGateway gateway = new SmsGateway();

        DispatchResult first = gateway.send(new OrderEvent("ORD-5002", "+1-555-0100", "Your order has shipped."));
        DispatchResult second = gateway.send(new OrderEvent("ORD-5003", "+1-555-0100", "Your order has shipped."));

        assertThat(first.dispatchId()).isNotEqualTo(second.dispatchId());
    }
}
