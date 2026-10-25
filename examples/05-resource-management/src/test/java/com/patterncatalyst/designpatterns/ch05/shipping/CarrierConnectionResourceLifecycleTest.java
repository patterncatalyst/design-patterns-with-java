package com.patterncatalyst.designpatterns.ch05.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Proves the try-with-resources guarantee directly: close() runs exactly
 * once on the normal path and exactly once on a path a checked exception
 * unwinds, with nothing at the call site asking for either.
 */
class CarrierConnectionResourceLifecycleTest {

    @Test
    void closesExactlyOnceOnTheNormalPath() throws ManifestRejectedException {
        CarrierConnection connection = spy(CarrierConnection.open("fedex-booking.example"));

        try (connection) {
            connection.submitManifest(new Manifest("PKG-4001", 8.0));
        }

        verify(connection, times(1)).close();
        assertThat(connection.isOpen()).isFalse();
    }

    @Test
    void closesExactlyOnceOnTheExceptionPath() {
        CarrierConnection connection = spy(CarrierConnection.open("ups-booking.example"));

        assertThatThrownBy(() -> {
            try (connection) {
                connection.submitManifest(new Manifest("PKG-4002", 95.0));
            }
        }).isInstanceOf(ManifestRejectedException.class);

        verify(connection, times(1)).close();
        assertThat(connection.isOpen()).isFalse();
    }
}
