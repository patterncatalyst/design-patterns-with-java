package com.patterncatalyst.designpatterns.ch30.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmtpNotificationGatewayTest {

    @Test
    void rejectsANullHost() {
        assertThatThrownBy(() -> new SmtpNotificationGateway(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void sendingDoesNotThrow() {
        SmtpNotificationGateway gateway = new SmtpNotificationGateway("mail.internal.example");

        assertThatCode(() -> gateway.sendDispatchConfirmation("ops@example.com", "test message"))
                .doesNotThrowAnyException();
    }
}
