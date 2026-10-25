package com.patterncatalyst.designpatterns.ch05.shipping;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BatchLedgerWriterTest {

    @Test
    void writesAndCommitsAnEntryToDisk(@TempDir Path tempDir) throws IOException {
        Path ledgerPath = tempDir.resolve("ledger.csv");
        ShipmentBatch batch = new ShipmentBatch("BATCH-1", 6, 20.0);

        try (BatchLedgerWriter writer = BatchLedgerWriter.open(ledgerPath)) {
            writer.writeEntry(batch);
            writer.commit();

            assertThat(writer.isCommitted()).isTrue();
        }

        assertThat(Files.readString(ledgerPath)).contains("BATCH-1,6,20.0");
    }

    @Test
    void appendsAcrossSeparateOpenCloseCycles(@TempDir Path tempDir) throws IOException {
        Path ledgerPath = tempDir.resolve("ledger.csv");

        try (BatchLedgerWriter writer = BatchLedgerWriter.open(ledgerPath)) {
            writer.writeEntry(new ShipmentBatch("BATCH-1", 6, 20.0));
            writer.commit();
        }
        try (BatchLedgerWriter writer = BatchLedgerWriter.open(ledgerPath)) {
            writer.writeEntry(new ShipmentBatch("BATCH-2", 3, 9.5));
            writer.commit();
        }

        String contents = Files.readString(ledgerPath);
        assertThat(contents).contains("BATCH-1,6,20.0").contains("BATCH-2,3,9.5");
    }
}
