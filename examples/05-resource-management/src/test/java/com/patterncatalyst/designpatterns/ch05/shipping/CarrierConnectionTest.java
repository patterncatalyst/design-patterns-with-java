package com.patterncatalyst.designpatterns.ch05.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CarrierConnectionTest {

    @Test
    void opensInTheOpenState() {
        CarrierConnection connection = CarrierConnection.open("fedex-booking.example");

        assertThat(connection.isOpen()).isTrue();
        assertThat(connection.carrierEndpoint()).isEqualTo("fedex-booking.example");
    }

    @Test
    void submitsAManifestUnderTheWeightLimit() throws ManifestRejectedException {
        CarrierConnection connection = CarrierConnection.open("fedex-booking.example");

        ManifestReceipt receipt = connection.submitManifest(new Manifest("PKG-3001", 10.0));

        assertThat(receipt.trackingId()).isEqualTo("PKG-3001");
        assertThat(receipt.carrierEndpoint()).isEqualTo("fedex-booking.example");
    }

    @Test
    void rejectsAManifestOverTheWeightLimit() {
        CarrierConnection connection = CarrierConnection.open("fedex-booking.example");

        assertThatThrownBy(() -> connection.submitManifest(new Manifest("PKG-3002", 95.0)))
                .isInstanceOf(ManifestRejectedException.class)
                .hasMessageContaining("PKG-3002");
    }

    @Test
    void rejectsBlankEndpoint() {
        assertThatThrownBy(() -> CarrierConnection.open(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refusesToSubmitAfterClose() {
        CarrierConnection connection = CarrierConnection.open("fedex-booking.example");
        connection.close();

        assertThatThrownBy(() -> connection.submitManifest(new Manifest("PKG-3003", 5.0)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void closeIsIdempotent() {
        CarrierConnection connection = CarrierConnection.open("fedex-booking.example");

        connection.close();
        connection.close();

        assertThat(connection.isOpen()).isFalse();
    }
}
