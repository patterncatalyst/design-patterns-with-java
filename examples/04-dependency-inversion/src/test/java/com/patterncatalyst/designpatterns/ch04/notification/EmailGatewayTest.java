package com.patterncatalyst.designpatterns.ch04.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailGatewayTest {

    @Test
    void sendAcceptsAnEventWithAContactAndRecordsItAsPending() {
        EmailGateway gateway = new EmailGateway();
        OrderEvent event = new OrderEvent("ORD-4001", "sam@example.com", "Your order has shipped.");

        DispatchResult result = gateway.send(event);

        assertThat(result.status()).isEqualTo(DispatchStatus.SENT);
        assertThat(result.dispatchId()).isEqualTo("email-ORD-4001");
        assertThat(gateway.statusOf(result.dispatchId())).isEqualTo(DeliveryState.PENDING);
    }

    @Test
    void sendFailsAnEventWithNoContactOnFile() {
        EmailGateway gateway = new EmailGateway();
        OrderEvent event = new OrderEvent("ORD-4002", "", "Your order has shipped.");

        DispatchResult result = gateway.send(event);

        assertThat(result.status()).isEqualTo(DispatchStatus.FAILED);
        assertThat(gateway.statusOf(result.dispatchId())).isEqualTo(DeliveryState.FAILED);
    }

    @Test
    void markDeliveredMovesAPendingDispatchToDelivered() {
        EmailGateway gateway = new EmailGateway();
        OrderEvent event = new OrderEvent("ORD-4003", "sam@example.com", "Your order has shipped.");
        DispatchResult result = gateway.send(event);

        gateway.markDelivered(result.dispatchId());

        assertThat(gateway.statusOf(result.dispatchId())).isEqualTo(DeliveryState.DELIVERED);
    }

    @Test
    void statusOfAnUnknownDispatchIdIsUnknown() {
        EmailGateway gateway = new EmailGateway();

        assertThat(gateway.statusOf("email-NEVER-SENT")).isEqualTo(DeliveryState.UNKNOWN);
    }

    @Test
    void renderUsesTheNamedTemplate() {
        EmailGateway gateway = new EmailGateway();
        OrderEvent event = new OrderEvent("ORD-4004", "sam@example.com", "it left the warehouse this morning");

        String rendered = gateway.render("order-delayed", event);

        assertThat(rendered).isEqualTo(
                "[Order ORD-4004] We're sorry, your order is running late: it left the warehouse this morning");
    }

    @Test
    void renderFallsBackToAGenericTemplateForAnUnknownTemplateId() {
        EmailGateway gateway = new EmailGateway();
        OrderEvent event = new OrderEvent("ORD-4005", "sam@example.com", "a custom note");

        String rendered = gateway.render("no-such-template", event);

        assertThat(rendered).isEqualTo("[Order ORD-4005] a custom note");
    }
}
