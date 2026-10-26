package com.patterncatalyst.designpatterns.ch29.shipping;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SmtpCarrierGatewayTest {

    @Test
    void recordsEveryNotificationItSends() {
        SmtpCarrierGateway gateway = new SmtpCarrierGateway();
        Shipment shipment = new Shipment("SHM-6001", Instant.now(), 10.0);

        gateway.notifyCarrier(shipment, "promised delivery window missed by more than 2h");

        assertThat(gateway.sentNotifications())
                .containsExactly("SHM-6001: promised delivery window missed by more than 2h");
    }
}
