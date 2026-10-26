package com.patterncatalyst.designpatterns.ch24.shipping;

import java.util.Comparator;
import java.util.List;

/**
 * Exercises an unbounded and a bounded carrier fan-out against the same
 * shipment and reports elapsed time, so the concurrency virtual threads buy
 * you shows up as an observable fact, not just a passing assertion.
 */
public final class ShippingDemo {

    private ShippingDemo() {
    }

    public static void main(String[] args) throws InterruptedException {
        Shipment shipment = new Shipment("SHIP-4821", 6.4);
        List<String> carrierIds = List.of(
                "acme", "bolt", "crate", "dash", "ember", "flux", "granite", "harbor");

        CarrierGateway gateway = new SimulatedCarrierGateway();
        RateQuoteFanOutService fanOutService = new RateQuoteFanOutService(gateway);
        CarrierRateCache cache = new CarrierRateCache();

        System.out.println("== Fan-out across " + carrierIds.size()
                + " carriers, one virtual thread per carrier call ==");
        long started = System.nanoTime();
        List<RateQuote> quotes = fanOutService.fanOut(shipment, carrierIds);
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;
        quotes.forEach(cache::recordIfCheaper);

        quotes.stream()
                .sorted(Comparator.comparing(RateQuote::carrierId))
                .forEach(q -> System.out.println("  " + q.carrierId() + ": " + q.quotedCents() + " cents"));
        System.out.println("Elapsed: " + elapsedMs + " ms for " + carrierIds.size()
                + " blocking calls (each sleeps 120-300 ms; a sequential loop would take "
                + "roughly " + (carrierIds.size() * 200) + " ms)");
        System.out.println("Cheapest overall: " + cache.cheapestOverall().orElseThrow());

        System.out.println();
        System.out.println("== Bounded fan-out: at most 3 carrier calls in flight at once ==");
        long boundedStarted = System.nanoTime();
        List<RateQuote> boundedQuotes = fanOutService.fanOutBounded(shipment, carrierIds, 3);
        long boundedElapsedMs = (System.nanoTime() - boundedStarted) / 1_000_000;
        System.out.println("Received " + boundedQuotes.size() + " quotes in " + boundedElapsedMs
                + " ms with concurrency capped at 3 (still one virtual thread per carrier; "
                + "only the number of carrier calls allowed to block at once is limited)");
    }
}
