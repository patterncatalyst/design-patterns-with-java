package com.patterncatalyst.designpatterns.ch24.shipping;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Proves the fan-out runs every carrier call concurrently, on its own
 * virtual thread, rather than one at a time. Every carrier call rendezvouses
 * at a {@link CyclicBarrier} sized to the carrier count: the barrier can
 * only release once every call has arrived. If the fan-out serialized the
 * calls instead of handing each one its own virtual thread, the barrier
 * would never fill and the test would hang rather than quietly pass.
 * {@link org.junit.jupiter.api.Assertions#assertTimeoutPreemptively} is a
 * deadlock guard here, not an assertion about how fast the pattern runs.
 */
class RateQuoteFanOutServiceConcurrencyTest {

    @Test
    void everyCarrierCallRunsConcurrentlyOnItsOwnVirtualThread() {
        List<String> carrierIds = IntStream.rangeClosed(1, 24)
                .mapToObj(i -> "carrier-" + i)
                .toList();
        CyclicBarrier allCarriersArrived = new CyclicBarrier(carrierIds.size());
        Set<Thread> threadsUsed = ConcurrentHashMap.newKeySet();

        CarrierGateway gateway = (shipment, carrierId) -> {
            threadsUsed.add(Thread.currentThread());
            try {
                allCarriersArrived.await();
            } catch (BrokenBarrierException e) {
                throw new IllegalStateException(e);
            }
            return new RateQuote(carrierId, carrierId.length() * 100L);
        };

        RateQuoteFanOutService service = new RateQuoteFanOutService(gateway);
        Shipment shipment = new Shipment("SHIP-100", 3.0);

        List<RateQuote> quotes = assertTimeoutPreemptively(Duration.ofSeconds(10),
                () -> service.fanOut(shipment, carrierIds));

        assertThat(quotes).hasSize(carrierIds.size());
        assertThat(quotes).extracting(RateQuote::carrierId)
                .containsExactlyInAnyOrderElementsOf(carrierIds);
        assertThat(threadsUsed).hasSize(carrierIds.size());
        assertThat(threadsUsed).allMatch(Thread::isVirtual);
    }
}
