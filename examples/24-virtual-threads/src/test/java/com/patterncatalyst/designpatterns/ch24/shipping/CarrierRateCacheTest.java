package com.patterncatalyst.designpatterns.ch24.shipping;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class CarrierRateCacheTest {

    @Test
    void recordsTheCheaperOfTwoQuotesForTheSameCarrier() {
        CarrierRateCache cache = new CarrierRateCache();

        assertThat(cache.recordIfCheaper(new RateQuote("acme", 500))).isTrue();
        assertThat(cache.recordIfCheaper(new RateQuote("acme", 700))).isFalse();
        assertThat(cache.recordIfCheaper(new RateQuote("acme", 300))).isTrue();

        assertThat(cache.cheapestOverall()).contains(new RateQuote("acme", 300));
    }

    @Test
    void tracksTheCheapestQuoteAcrossDifferentCarriers() {
        CarrierRateCache cache = new CarrierRateCache();
        cache.recordIfCheaper(new RateQuote("acme", 500));
        cache.recordIfCheaper(new RateQuote("bolt", 420));
        cache.recordIfCheaper(new RateQuote("crate", 610));

        assertThat(cache.trackedCarrierCount()).isEqualTo(3);
        assertThat(cache.cheapestOverall()).contains(new RateQuote("bolt", 420));
    }

    @Test
    void tryRecordIfCheaperMatchesRecordIfCheaperWhenTheLockIsFree() {
        CarrierRateCache cache = new CarrierRateCache();
        cache.recordIfCheaper(new RateQuote("acme", 500));

        assertThat(cache.tryRecordIfCheaper(new RateQuote("acme", 700))).isFalse();
        assertThat(cache.tryRecordIfCheaper(new RateQuote("acme", 100))).isTrue();
        assertThat(cache.cheapestOverall()).contains(new RateQuote("acme", 100));
    }

    @Test
    void staysConsistentWhenManyVirtualThreadsRecordQuotesAtOnce() throws Exception {
        CarrierRateCache cache = new CarrierRateCache();
        int writerCount = 200;
        CountDownLatch readyToStart = new CountDownLatch(writerCount);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(writerCount);

        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int i = 0; i < writerCount; i++) {
                    long cents = 1000L - i;
                    executor.submit(() -> {
                        readyToStart.countDown();
                        startSignal.await();
                        cache.recordIfCheaper(new RateQuote("acme", cents));
                        finished.countDown();
                        return null;
                    });
                }
                readyToStart.await();
                startSignal.countDown();
                finished.await();
            }
        });

        assertThat(cache.cheapestOverall()).contains(new RateQuote("acme", 1000L - (writerCount - 1)));
    }
}
