package com.patterncatalyst.designpatterns.ch24.shipping;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Proves {@code fanOutBounded} never lets more than the configured limit of
 * carrier calls block at once, and that it actually reaches that limit
 * rather than serializing by accident. A {@link CyclicBarrier} sized to the
 * limit can only release once exactly that many calls are concurrently past
 * the semaphore gate; with only {@code limit} permits available in the
 * production code under test, that is also the most that can ever be in
 * flight, so the observed maximum both proves the ceiling is reached and
 * proves it is never exceeded.
 */
class RateQuoteFanOutServiceBoundedConcurrencyTest {

    @Test
    void boundedFanOutCapsConcurrentCarrierCallsAtTheConfiguredLimit() {
        int limit = 3;
        List<String> carrierIds = IntStream.rangeClosed(1, 12)
                .mapToObj(i -> "carrier-" + i)
                .toList();

        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger observedMax = new AtomicInteger();
        CyclicBarrier exactlyLimitConcurrently = new CyclicBarrier(limit);

        CarrierGateway gateway = (shipment, carrierId) -> {
            int now = inFlight.incrementAndGet();
            observedMax.accumulateAndGet(now, Math::max);
            try {
                exactlyLimitConcurrently.await();
            } catch (BrokenBarrierException e) {
                throw new IllegalStateException(e);
            } finally {
                inFlight.decrementAndGet();
            }
            return new RateQuote(carrierId, 100L);
        };

        RateQuoteFanOutService service = new RateQuoteFanOutService(gateway);
        Shipment shipment = new Shipment("SHIP-200", 2.0);

        List<RateQuote> quotes = assertTimeoutPreemptively(Duration.ofSeconds(10),
                () -> service.fanOutBounded(shipment, carrierIds, limit));

        assertThat(quotes).hasSize(carrierIds.size());
        assertThat(observedMax.get()).isEqualTo(limit);
    }
}
