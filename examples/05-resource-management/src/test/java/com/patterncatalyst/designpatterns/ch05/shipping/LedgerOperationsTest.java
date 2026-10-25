package com.patterncatalyst.designpatterns.ch05.shipping;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LedgerOperationsTest {

    @Test
    void withLedgerWritesAndCommitsOnTheNormalPath(@TempDir Path tempDir) throws IOException {
        Path ledgerPath = tempDir.resolve("ledger.csv");
        ShipmentBatch batch = new ShipmentBatch("BATCH-10", 4, 12.0);

        Integer entryCount = LedgerOperations.withLedger(ledgerPath, writer -> {
            writer.writeEntry(batch);
            return 1;
        });

        assertThat(entryCount).isEqualTo(1);
        assertThat(Files.readString(ledgerPath)).contains("BATCH-10,4,12.0");
    }

    @Test
    void withLedgerClosesExactlyOnceOnTheNormalPath(@TempDir Path tempDir) throws IOException {
        BatchLedgerWriter writer = spy(BatchLedgerWriter.open(tempDir.resolve("ledger.csv")));

        LedgerOperations.withLedgerWriter(() -> writer, w -> {
            w.writeEntry(new ShipmentBatch("BATCH-11", 2, 5.0));
            return null;
        });

        verify(writer, times(1)).close();
        assertThat(writer.isCommitted()).isTrue();
    }

    @Test
    void withLedgerClosesExactlyOnceWhenTheActionThrows(@TempDir Path tempDir) throws IOException {
        BatchLedgerWriter writer = spy(BatchLedgerWriter.open(tempDir.resolve("ledger.csv")));

        assertThatThrownBy(() -> LedgerOperations.withLedgerWriter(() -> writer, w -> {
            w.writeEntry(new ShipmentBatch("BATCH-12", 1, 2.0));
            throw new IOException("carrier portal rejected the batch upload");
        })).isInstanceOf(LedgerWriteException.class)
                .hasCauseInstanceOf(IOException.class);

        verify(writer, times(1)).close();
        assertThat(writer.isCommitted()).isFalse();
    }
}
