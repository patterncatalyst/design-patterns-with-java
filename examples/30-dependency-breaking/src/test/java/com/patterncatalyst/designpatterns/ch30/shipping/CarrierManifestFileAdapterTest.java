package com.patterncatalyst.designpatterns.ch30.shipping;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CarrierManifestFileAdapterTest {

    @Test
    void forwardsCarrierCodeAndPackageCountFromTheRealFile(@TempDir Path tempDir) throws IOException {
        Path manifestPath = tempDir.resolve("manifest.txt");
        Files.writeString(manifestPath, "FASTFREIGHT" + System.lineSeparator() + "275" + System.lineSeparator());
        CarrierManifestFile realFile = new CarrierManifestFile(manifestPath);

        CarrierManifestFileAdapter adapter = new CarrierManifestFileAdapter(realFile);

        assertThat(adapter.carrierCode()).isEqualTo("FASTFREIGHT");
        assertThat(adapter.packageCount()).isEqualTo(275);
    }

    @Test
    void rejectsAMalformedManifestFile(@TempDir Path tempDir) throws IOException {
        Path manifestPath = tempDir.resolve("manifest.txt");
        Files.writeString(manifestPath, "ONLY-ONE-LINE");

        assertThatThrownBy(() -> new CarrierManifestFile(manifestPath))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANullManifestFile() {
        assertThatThrownBy(() -> new CarrierManifestFileAdapter(null))
                .isInstanceOf(NullPointerException.class);
    }
}
