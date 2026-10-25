package com.patterncatalyst.designpatterns.ch05.shipping;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Appends shipment-batch records to a ledger file on disk. The open file
 * handle is the resource this class guards: every {@link #open(Path)} must
 * be matched by exactly one {@link #close()}, or the handle leaks for as
 * long as the JVM keeps running.
 */
public final class BatchLedgerWriter implements AutoCloseable {

    private final Path ledgerPath;
    private final BufferedWriter writer;
    private boolean committed;

    private BatchLedgerWriter(Path ledgerPath, BufferedWriter writer) {
        this.ledgerPath = ledgerPath;
        this.writer = writer;
    }

    public static BatchLedgerWriter open(Path ledgerPath) throws IOException {
        Objects.requireNonNull(ledgerPath, "ledgerPath");
        BufferedWriter writer = Files.newBufferedWriter(
                ledgerPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        return new BatchLedgerWriter(ledgerPath, writer);
    }

    public void writeEntry(ShipmentBatch batch) throws IOException {
        Objects.requireNonNull(batch, "batch");
        writer.write(batch.batchId() + "," + batch.shipmentCount() + "," + batch.totalWeightKg());
        writer.newLine();
    }

    public void commit() throws IOException {
        writer.flush();
        committed = true;
    }

    public boolean isCommitted() {
        return committed;
    }

    public Path ledgerPath() {
        return ledgerPath;
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
