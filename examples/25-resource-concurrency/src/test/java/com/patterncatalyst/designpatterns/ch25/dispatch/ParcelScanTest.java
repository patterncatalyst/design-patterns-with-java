package com.patterncatalyst.designpatterns.ch25.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ParcelScanTest {

    @Test
    void rejectsABlankParcelId() {
        assertThatThrownBy(() -> new ParcelScan("  ", "scanned"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullFields() {
        assertThatThrownBy(() -> new ParcelScan(null, "scanned")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ParcelScan("SHM-7001", null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void acceptsAValidScan() {
        ParcelScan scan = new ParcelScan("SHM-7001", "scanned");

        assertThat(scan.parcelId()).isEqualTo("SHM-7001");
        assertThat(scan.event()).isEqualTo("scanned");
    }
}
