package com.patterncatalyst.designpatterns.ch04.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Proves Interface Segregation as an observable fact about the gateways
 * rather than an assertion in prose. A Java compiler already refuses
 * {@code TemplateRenderer renderer = new SmsGateway();}, since {@code
 * SmsGateway} never declares that interface; these tests confirm the same
 * boundary at runtime, the way a reviewer skimming the class declarations
 * would.
 */
class GatewayRoleSeparationTest {

    @Test
    void smsGatewayImplementsOnlyMessageSender() {
        SmsGateway smsGateway = new SmsGateway();

        assertThat(smsGateway).isInstanceOf(MessageSender.class);
        assertThat(smsGateway).isNotInstanceOf(TemplateRenderer.class);
        assertThat(smsGateway).isNotInstanceOf(DeliveryStatusProvider.class);
    }

    @Test
    void emailGatewayImplementsAllThreeRoles() {
        EmailGateway emailGateway = new EmailGateway();

        assertThat(emailGateway).isInstanceOf(MessageSender.class);
        assertThat(emailGateway).isInstanceOf(TemplateRenderer.class);
        assertThat(emailGateway).isInstanceOf(DeliveryStatusProvider.class);
    }
}
