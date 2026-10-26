package com.patterncatalyst.designpatterns.ch25.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * {@link LabelPrintActiveObject} must decouple submission from execution (the
 * request runs on the dedicated worker, never on the caller's thread), run
 * requests in submission order from a single submitter, and process every
 * request exactly once even when several threads submit concurrently.
 */
class LabelPrintActiveObjectTest {

    private final LabelPrintActiveObject printer = new LabelPrintActiveObject();

    @AfterEach
    void shutdownWorker() throws InterruptedException {
        printer.shutdown();
    }

    @Test
    void submittingRunsOnTheDedicatedWorkerThreadNotTheCaller() throws Exception {
        String callerThreadName = Thread.currentThread().getName();

        PrintReceipt receipt = printer.submitPrint(new ShippingLabelRequest("SHM-9001", "PHX")).get();

        assertThat(receipt.printedByThread()).isNotEqualTo(callerThreadName);
        assertThat(receipt.printedByThread()).isEqualTo("label-print-worker");
    }

    @Test
    void aSingleSubmitterSeesRequestsProcessedInSubmissionOrder() throws Exception {
        List<CompletableFuture<PrintReceipt>> futures = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            futures.add(printer.submitPrint(new ShippingLabelRequest("SHM-" + i, "DEN")));
        }

        for (int i = 0; i < futures.size(); i++) {
            PrintReceipt receipt = futures.get(i).get();
            assertThat(receipt.parcelId()).isEqualTo("SHM-" + i);
            assertThat(receipt.sequence()).isEqualTo(i + 1L);
        }
    }

    @Test
    void concurrentSubmittersAreAllProcessedExactlyOnceWithNoDuplicateSequence() throws Exception {
        int submitterCount = 6;
        int perSubmitter = 25;
        int total = submitterCount * perSubmitter;

        List<CompletableFuture<PrintReceipt>> futures;
        try (ExecutorService submitters = Executors.newFixedThreadPool(submitterCount)) {
            List<java.util.concurrent.Future<List<CompletableFuture<PrintReceipt>>>> submissionFutures =
                    new ArrayList<>();
            for (int s = 0; s < submitterCount; s++) {
                int submitterIndex = s;
                submissionFutures.add(submitters.submit(() -> {
                    List<CompletableFuture<PrintReceipt>> submitted = new ArrayList<>();
                    for (int i = 0; i < perSubmitter; i++) {
                        submitted.add(printer.submitPrint(
                                new ShippingLabelRequest("SHM-" + submitterIndex + "-" + i, "ORD")));
                    }
                    return submitted;
                }));
            }
            futures = new ArrayList<>();
            for (var submissionFuture : submissionFutures) {
                futures.addAll(submissionFuture.get());
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        Set<Long> sequences = new HashSet<>();
        long maxSequence = 0L;
        for (CompletableFuture<PrintReceipt> future : futures) {
            long sequence = future.get().sequence();
            sequences.add(sequence);
            maxSequence = Math.max(maxSequence, sequence);
        }

        assertThat(sequences).hasSize(total);
        assertThat(maxSequence).isEqualTo(total);
    }

    @Test
    void shutdownStopsTheWorkerSoLaterRequestsNeverComplete() throws Exception {
        printer.shutdown();

        CompletableFuture<PrintReceipt> afterShutdown =
                printer.submitPrint(new ShippingLabelRequest("SHM-9999", "LAX"));

        assertThatThrownBy(() -> afterShutdown.get(100, TimeUnit.MILLISECONDS))
                .isInstanceOf(TimeoutException.class);
    }
}
