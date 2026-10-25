package com.patterncatalyst.designpatterns.ch05.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManifestTest {

    @Test
    void storesTrackingIdAndWeight() {
        Manifest manifest = new Manifest("PKG-2001", 5.5);

        assertThat(manifest.trackingId()).isEqualTo("PKG-2001");
        assertThat(manifest.weightKg()).isEqualTo(5.5);
    }

    @Test
    void rejectsBlankTrackingId() {
        assertThatThrownBy(() -> new Manifest("  ", 5.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trackingId");
    }

    @Test
    void rejectsNonPositiveWeight() {
        assertThatThrownBy(() -> new Manifest("PKG-2002", 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weightKg");
    }
}
