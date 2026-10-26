package com.patterncatalyst.designpatterns.ch25.dispatch;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/**
 * {@link TrackingCodeGenerator} confines a non-thread-safe
 * {@link java.security.MessageDigest} per calling thread with
 * {@link ThreadLocal}. The code it computes for a given parcel id must be
 * identical no matter which thread computes it, and concurrent callers on
 * separate threads must never corrupt each other's digest state.
 */
class TrackingCodeGeneratorTest {

    @Test
    void theSameInputProducesTheSameCodeOnOneThread() {
        TrackingCodeGenerator generator = new TrackingCodeGenerator();

        String first = generator.trackingCodeFor("SHM-7001");
        String second = generator.trackingCodeFor("SHM-7001");

        assertThat(second).isEqualTo(first);
    }

    @Test
    void differentInputsProduceDifferentCodes() {
        TrackingCodeGenerator generator = new TrackingCodeGenerator();

        assertThat(generator.trackingCodeFor("SHM-7001")).isNotEqualTo(generator.trackingCodeFor("SHM-7002"));
    }

    @Test
    void everyThreadComputesTheIdenticalCodeForTheSameInput() throws Exception {
        TrackingCodeGenerator generator = new TrackingCodeGenerator();
        String expected = generator.trackingCodeFor("SHM-7001");

        List<Callable<String>> tasks = new java.util.ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tasks.add(() -> generator.trackingCodeFor("SHM-7001"));
        }

        Set<String> observed = new HashSet<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
            List<Future<String>> futures = executor.invokeAll(tasks);
            for (Future<String> future : futures) {
                observed.add(future.get());
            }
        }

        assertThat(observed).containsExactly(expected);
    }
}
