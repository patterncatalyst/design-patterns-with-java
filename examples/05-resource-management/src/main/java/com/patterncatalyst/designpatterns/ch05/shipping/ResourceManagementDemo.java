package com.patterncatalyst.designpatterns.ch05.shipping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Exercises every resource-management idiom in this chapter against one
 * shipping scenario: try-with-resources on both the normal and the
 * exception path, the Execute-Around Method idiom around a ledger write,
 * and a pooled resource whose lifetime outlives any single call.
 */
public final class ResourceManagementDemo {

    private ResourceManagementDemo() {
    }

    public static void main(String[] args) throws IOException {
        System.out.println("== Java 25: try-with-resources, normal path ==");
        CarrierConnection connection = CarrierConnection.open("fedex-booking.example");
        try (connection) {
            ManifestReceipt receipt = connection.submitManifest(new Manifest("PKG-1002", 3.1));
            System.out.println("Receipt: " + receipt);
        } catch (ManifestRejectedException e) {
            System.out.println("Rejected: " + e.getMessage());
        }
        System.out.println("Connection open after the block? " + connection.isOpen());

        System.out.println();
        System.out.println("== Java 25: try-with-resources, exception path ==");
        CarrierConnection overweightConnection = CarrierConnection.open("ups-booking.example");
        try (overweightConnection) {
            overweightConnection.submitManifest(new Manifest("PKG-1003", 95.0));
        } catch (ManifestRejectedException e) {
            System.out.println("Rejected: " + e.getMessage());
        }
        System.out.println("Connection open after the exception unwound the block? " + overweightConnection.isOpen());

        System.out.println();
        System.out.println("== Java 25: Execute-Around Method writes and commits a ledger batch ==");
        Path ledgerPath = Files.createTempFile("ledger-demo", ".csv");
        try {
            ShipmentBatch batch = new ShipmentBatch("BATCH-77", 12, 48.6);
            LedgerOperations.withLedger(ledgerPath, writer -> {
                writer.writeEntry(batch);
                return null;
            });
            System.out.println("Ledger committed: " + Files.readString(ledgerPath).strip());

            System.out.println();
            System.out.println("== Java 25: Execute-Around Method still closes when the action throws ==");
            try {
                LedgerOperations.withLedger(ledgerPath, writer -> {
                    writer.writeEntry(batch);
                    throw new IOException("carrier portal rejected the batch upload");
                });
            } catch (LedgerWriteException e) {
                System.out.println("Ledger write failed: " + e.getCause().getMessage());
            }
        } finally {
            Files.deleteIfExists(ledgerPath);
        }

        System.out.println();
        System.out.println("== A pooled resource that outlives any one call: TrackingFeedHandle ==");
        try (TrackingFeedRegistry registry = new TrackingFeedRegistry()) {
            TrackingUpdate first = registry.handleFor("fedex-booking.example").poll("PKG-1001");
            TrackingUpdate second = registry.handleFor("fedex-booking.example").poll("PKG-1002");
            System.out.println("First poll:  " + first);
            System.out.println("Second poll: " + second);
            System.out.println("Open handles while the registry is in use: " + registry.openHandleCount());
        }
        System.out.println("Registry closed; every pooled handle released at once, not per call.");
    }
}
