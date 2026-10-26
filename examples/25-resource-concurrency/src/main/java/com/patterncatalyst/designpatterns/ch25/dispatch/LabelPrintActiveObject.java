package com.patterncatalyst.designpatterns.ch25.dispatch;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Decouples "print this label" from whatever thread eventually runs it.
 *
 * <p>{@code submitPrint()} wraps the request in a task, pushes it onto a
 * private {@link BlockingQueue}, and returns a {@link CompletableFuture}
 * immediately, without running anything on the calling thread. One
 * dedicated worker thread, started once in the constructor, drains that
 * queue and runs each request in submission order, one at a time, matching
 * a physical label printer that can only execute one job at a time
 * regardless of how many callers want one.
 *
 * <p>This is a different question than {@link CarrierConnectionPool}'s. The
 * pool answers "how many of these may exist at once"; the active object
 * answers "what happens when a caller must never block on submission, and
 * the receiving side needs one serialized timeline." A single long-lived
 * worker gains nothing from being a virtual thread, there is only one of
 * it, so this worker is an ordinary platform thread.
 */
public final class LabelPrintActiveObject {

    private static final Runnable SHUTDOWN_SIGNAL = () -> { };

    private final BlockingQueue<Runnable> requests = new LinkedBlockingQueue<>();
    private final AtomicLong sequence = new AtomicLong();
    private final Thread worker;

    public LabelPrintActiveObject() {
        this.worker = Thread.ofPlatform().name("label-print-worker").start(this::drainLoop);
    }

    public CompletableFuture<PrintReceipt> submitPrint(ShippingLabelRequest request) {
        CompletableFuture<PrintReceipt> result = new CompletableFuture<>();
        requests.add(() -> {
            try {
                PrintReceipt receipt = new PrintReceipt(
                        request.parcelId(), sequence.incrementAndGet(), Thread.currentThread().getName());
                result.complete(receipt);
            } catch (RuntimeException e) {
                result.completeExceptionally(e);
            }
        });
        return result;
    }

    /** Stops the worker after it drains every request already queued. */
    public void shutdown() throws InterruptedException {
        requests.add(SHUTDOWN_SIGNAL);
        worker.join();
    }

    private void drainLoop() {
        while (true) {
            Runnable task;
            try {
                task = requests.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (task == SHUTDOWN_SIGNAL) {
                return;
            }
            task.run();
        }
    }
}
