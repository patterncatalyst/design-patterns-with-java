package com.patterncatalyst.designpatterns.ch25.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/**
 * {@link DispatchDock} must keep an exact count under concurrent scanner
 * threads (the monitor's invariant), and {@code awaitFullBay} must actually
 * block the dispatcher until the bay reaches capacity rather than returning
 * early or on a fixed schedule.
 */
class DispatchDockTest {

    @Test
    void concurrentScansAreAllSerializedWithoutLoss() throws Exception {
        int threadCount = 8;
        int scansPerThread = 250;
        DispatchDock dock = new DispatchDock(threadCount * scansPerThread);
        CountDownLatch startGate = new CountDownLatch(1);

        List<Callable<Void>> tasks = new java.util.ArrayList<>();
        for (int t = 0; t < threadCount; t++) {
            int threadIndex = t;
            tasks.add(() -> {
                startGate.await();
                for (int i = 0; i < scansPerThread; i++) {
                    dock.stageParcel(new ParcelScan("SHM-" + threadIndex + "-" + i, "scanned"));
                }
                return null;
            });
        }

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Void>> futures = new java.util.ArrayList<>();
            for (Callable<Void> task : tasks) {
                futures.add(executor.submit(task));
            }
            startGate.countDown();
            for (Future<Void> future : futures) {
                future.get(10, java.util.concurrent.TimeUnit.SECONDS);
            }
        }

        assertThat(dock.stagedCount()).isEqualTo(threadCount * scansPerThread);
    }

    @Test
    void awaitFullBayBlocksUntilCapacityThenReturnsAnImmutableSnapshot() throws Exception {
        DispatchDock dock = new DispatchDock(3);

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<List<ParcelScan>> dispatcherFuture =
                    executor.submit(() -> dock.awaitFullBay(Duration.ofSeconds(2)));

            assertThat(dispatcherFuture.isDone()).isFalse();

            dock.stageParcel(new ParcelScan("SHM-8001", "scanned"));
            dock.stageParcel(new ParcelScan("SHM-8002", "scanned"));
            dock.stageParcel(new ParcelScan("SHM-8003", "scanned"));

            List<ParcelScan> manifest = dispatcherFuture.get(2, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(manifest).hasSize(3);
            assertThat(dock.stagedCount()).isZero();
            assertThatThrownBy(() -> manifest.add(new ParcelScan("SHM-8004", "scanned")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Test
    void awaitFullBayTimesOutWhenTheBayNeverFills() {
        DispatchDock dock = new DispatchDock(5);
        dock.stageParcel(new ParcelScan("SHM-8001", "scanned"));

        assertThatThrownBy(() -> dock.awaitFullBay(Duration.ofMillis(50)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsANonPositiveBayCapacity() {
        assertThatThrownBy(() -> new DispatchDock(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
