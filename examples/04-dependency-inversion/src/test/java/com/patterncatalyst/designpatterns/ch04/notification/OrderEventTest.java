package com.patterncatalyst.designpatterns.ch04.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OrderEventTest {

    @Test
    void rejectsBlankOrderId() {
        assertThatThrownBy(() -> new OrderEvent(" ", "sam@example.com", "Your order has shipped."))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orderId");
    }

    @Test
    void rejectsBlankMessageBody() {
        assertThatThrownBy(() -> new OrderEvent("ORD-9001", "sam@example.com", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messageBody");
    }

    @Test
    void allowsBlankCustomerContactAsAValidDomainState() {
        OrderEvent event = new OrderEvent("ORD-9002", "", "Your order has shipped.");

        assertThat(event.customerContact()).isEmpty();
    }

    @Test
    void withMessageBodyReturnsACopyWithTheSameIdentityFields() {
        OrderEvent original = new OrderEvent("ORD-9003", "sam@example.com", "original body");

        OrderEvent rendered = original.withMessageBody("rendered body");

        assertThat(rendered.orderId()).isEqualTo(original.orderId());
        assertThat(rendered.customerContact()).isEqualTo(original.customerContact());
        assertThat(rendered.messageBody()).isEqualTo("rendered body");
    }
}
