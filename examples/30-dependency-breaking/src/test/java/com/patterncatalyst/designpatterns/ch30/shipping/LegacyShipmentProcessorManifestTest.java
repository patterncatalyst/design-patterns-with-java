package com.patterncatalyst.designpatterns.ch30.shipping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Adapt Parameter: {@code summarizeManifest} now depends on the narrow
 * {@link ManifestSource}, so a test supplies two values with no filesystem
 * access at all, while a second test confirms the adapter is a faithful
 * stand-in for a real {@link CarrierManifestFile}.
 */
@ExtendWith(MockitoExtension.class)
class LegacyShipmentProcessorManifestTest {

    @Mock
    private NotificationGateway notificationGateway;

    @Mock
    private ManifestSource manifestSource;

    private LegacyShipmentProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new LegacyShipmentProcessor("ops-supervisor@example.com", notificationGateway);
    }

    @Test
    void summarizesAFakeManifestSourceWithNoFileRead() {
        when(manifestSource.carrierCode()).thenReturn("FASTFREIGHT");
        when(manifestSource.packageCount()).thenReturn(275);

        String summary = processor.summarizeManifest(manifestSource);

        assertThat(summary).isEqualTo("FASTFREIGHT: 275 package(s)");
    }

    @Test
    void summarizesARealManifestFileThroughTheAdapter(@TempDir Path tempDir) throws IOException {
        Path manifestPath = tempDir.resolve("manifest.txt");
        Files.writeString(manifestPath, "SLOWHAUL" + System.lineSeparator() + "48" + System.lineSeparator());
        CarrierManifestFile realFile = new CarrierManifestFile(manifestPath);
        CarrierManifestFileAdapter adapter = new CarrierManifestFileAdapter(realFile);

        String summary = processor.summarizeManifest(adapter);

        assertThat(summary).isEqualTo("SLOWHAUL: 48 package(s)");
    }
}
