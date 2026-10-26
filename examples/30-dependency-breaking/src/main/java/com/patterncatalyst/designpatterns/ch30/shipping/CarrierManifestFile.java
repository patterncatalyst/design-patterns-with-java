package com.patterncatalyst.designpatterns.ch30.shipping;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * The heavyweight legacy type {@code summarizeManifest} used to depend on
 * directly. Opening this constructor means opening and parsing a real file
 * on disk, a cost no unit test should have to pay just to read two values
 * back out. This type is unchanged by this chapter's work; only the
 * dependency on it gets narrowed, via {@link CarrierManifestFileAdapter}.
 *
 * <p>The file format is two lines: a carrier code, then a package count.
 */
public final class CarrierManifestFile {

    private final String carrierCode;
    private final int packageCount;

    public CarrierManifestFile(Path manifestPath) {
        Objects.requireNonNull(manifestPath, "manifestPath");
        List<String> lines;
        try {
            lines = Files.readAllLines(manifestPath);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read carrier manifest: " + manifestPath, e);
        }
        if (lines.size() < 2) {
            throw new IllegalArgumentException("manifest file must have a carrier code and a package count: " + manifestPath);
        }
        this.carrierCode = lines.get(0).strip();
        this.packageCount = Integer.parseInt(lines.get(1).strip());
    }

    public String carrierCode() {
        return carrierCode;
    }

    public int packageCount() {
        return packageCount;
    }
}
