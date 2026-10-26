package com.patterncatalyst.designpatterns.ch23.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ConfirmationNumberGeneratorTest {

    @Test
    void producesCarrierPrefixedNumbers() {
        String number = ConfirmationNumberGenerator.INSTANCE.nextConfirmationNumber("ground");

        assertThat(number).startsWith("GROUND-");
    }

    @Test
    void neverRepeatsANumberAcrossCalls() {
        String first = ConfirmationNumberGenerator.INSTANCE.nextConfirmationNumber("air");
        String second = ConfirmationNumberGenerator.INSTANCE.nextConfirmationNumber("air");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void staysUniqueUnderConcurrentCallers() throws InterruptedException {
        int threadCount = 32;
        Set<String> observed = ConcurrentHashMap.newKeySet();
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        try {
            for (int i = 0; i < threadCount; i++) {
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    observed.add(ConfirmationNumberGenerator.INSTANCE.nextConfirmationNumber("freight"));
                });
            }
            ready.await();
            start.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        assertThat(observed).hasSize(threadCount);
    }
}
