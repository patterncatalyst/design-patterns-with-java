package com.patterncatalyst.designpatterns.ch24.shipping;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

/**
 * Fans one {@link Shipment} out to many carriers at once and gathers every
 * quote. Each carrier call is a blocking operation; this class runs each one
 * on its own virtual thread rather than a thread drawn from a bounded pool,
 * so a fan-out of hundreds of carriers costs hundreds of cheap virtual
 * threads, not hundreds of expensive platform threads sitting idle on a
 * socket read.
 */
public final class RateQuoteFanOutService {

    private final CarrierGateway gateway;

    public RateQuoteFanOutService(CarrierGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    /**
     * Requests a quote from every carrier in {@code carrierIds}, one virtual
     * thread per carrier, and returns every result. A single carrier failure
     * fails the whole fan-out with a {@link CarrierQuoteException}, the
     * simplest contract for a caller that needs every quote to make a
     * decision.
     */
    public List<RateQuote> fanOut(Shipment shipment, List<String> carrierIds) throws InterruptedException {
        Objects.requireNonNull(shipment, "shipment");
        Objects.requireNonNull(carrierIds, "carrierIds");
        List<Callable<RateQuote>> tasks = carrierIds.stream()
                .map(carrierId -> (Callable<RateQuote>) () -> gateway.fetchQuote(shipment, carrierId))
                .toList();
        return runAll(tasks);
    }

    /**
     * The same fan-out, except at most {@code maxConcurrentCalls} carrier
     * calls are ever in flight at once. This still launches one virtual
     * thread per carrier; a virtual thread denied a permit blocks on the
     * {@link Semaphore}, not on the carrier's network call, so the thread
     * count stays one-per-task even while the carrier-call concurrency is
     * capped. Reach for this when a carrier's rate API enforces its own
     * concurrent-connection limit.
     */
    public List<RateQuote> fanOutBounded(Shipment shipment, List<String> carrierIds, int maxConcurrentCalls)
            throws InterruptedException {
        Objects.requireNonNull(shipment, "shipment");
        Objects.requireNonNull(carrierIds, "carrierIds");
        if (maxConcurrentCalls < 1) {
            throw new IllegalArgumentException("maxConcurrentCalls must be at least 1: " + maxConcurrentCalls);
        }
        Semaphore inFlight = new Semaphore(maxConcurrentCalls);
        List<Callable<RateQuote>> tasks = carrierIds.stream()
                .map(carrierId -> (Callable<RateQuote>) () -> {
                    inFlight.acquire();
                    try {
                        return gateway.fetchQuote(shipment, carrierId);
                    } finally {
                        inFlight.release();
                    }
                })
                .toList();
        return runAll(tasks);
    }

    private List<RateQuote> runAll(List<Callable<RateQuote>> tasks) throws InterruptedException {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<RateQuote>> futures = executor.invokeAll(tasks);
            List<RateQuote> quotes = new ArrayList<>(futures.size());
            for (Future<RateQuote> future : futures) {
                quotes.add(resultOf(future));
            }
            return List.copyOf(quotes);
        }
    }

    private static RateQuote resultOf(Future<RateQuote> future) {
        try {
            return future.get();
        } catch (ExecutionException e) {
            throw new CarrierQuoteException("carrier quote failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CarrierQuoteException("interrupted while awaiting a carrier quote", e);
        }
    }
}
