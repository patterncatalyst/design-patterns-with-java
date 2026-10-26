package com.patterncatalyst.designpatterns.ch30.shipping;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Extract Interface + Parameterize Constructor: proves a test double can
 * stand in for the mail dependency the inherited constructor used to build
 * internally, and that the original single-argument constructor still
 * compiles and runs against the real {@link SmtpNotificationGateway}.
 */
@ExtendWith(MockitoExtension.class)
class LegacyShipmentProcessorNotificationTest {

    @Mock
    private NotificationGateway notificationGateway;

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    @Test
    void dispatchSendsThroughTheInjectedGatewayInsteadOfARealSmtpConnection() {
        LegacyShipmentProcessor processor =
                new LegacyShipmentProcessor("ops-supervisor@example.com", notificationGateway);

        processor.dispatch(new Shipment("SH-5001", 10_000));

        verify(notificationGateway).sendDispatchConfirmation(eq("ops-supervisor@example.com"), any());
    }

    @Test
    void theNotificationMessageNamesTheShipmentAndTheTotal() {
        LegacyShipmentProcessor processor =
                new LegacyShipmentProcessor("ops-supervisor@example.com", notificationGateway);

        processor.dispatch(new Shipment("SH-5002", 10_000));

        verify(notificationGateway).sendDispatchConfirmation(eq("ops-supervisor@example.com"), messageCaptor.capture());
        assertThat(messageCaptor.getValue())
                .contains("SH-5002")
                .contains("$108.00");
    }

    @Test
    void theOriginalSingleArgumentConstructorStillCompilesAndRunsAgainstARealGateway() {
        LegacyShipmentProcessor processor = new LegacyShipmentProcessor("ops-supervisor@example.com");

        assertThatCode(() -> processor.dispatch(new Shipment("SH-5003", 5_000)))
                .doesNotThrowAnyException();
    }
}
