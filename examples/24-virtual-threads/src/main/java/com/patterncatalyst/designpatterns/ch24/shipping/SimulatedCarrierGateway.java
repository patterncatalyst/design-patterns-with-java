package com.patterncatalyst.designpatterns.ch24.shipping;

import java.time.Duration;
import java.util.Random;

/**
 * A stand-in for a real HTTP call to a carrier's rate API, used by
 * {@code ShippingDemo}. Every call blocks the calling thread for a short,
 * randomized interval the way a real network round trip would. Running this
 * on a virtual thread rather than a platform thread is the entire point of
 * this chapter's example: the block costs wall-clock time and nothing else,
 * because the JVM unmounts the virtual thread from its carrier thread for
 * the duration of the sleep and mounts another virtual thread in its place.
 */
public final class SimulatedCarrierGateway implements CarrierGateway {

    private final Random random = new Random();

    @Override
    public RateQuote fetchQuote(Shipment shipment, String carrierId) throws InterruptedException {
        Thread.sleep(Duration.ofMillis(120 + random.nextInt(180)));
        long cents = baseCentsFor(carrierId) + Math.round(shipment.weightKg() * 215);
        return new RateQuote(carrierId, cents);
    }

    private static long baseCentsFor(String carrierId) {
        return 400L + (long) carrierId.length() * 37;
    }
}
