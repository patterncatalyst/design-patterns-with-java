package com.patterncatalyst.designpatterns.ch25.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * {@link CarrierConnectionPool} must never let more connections exist
 * outstanding at once than its capacity, must reuse a released connection
 * rather than build a new one, and must block, not fail instantly, when a
 * caller asks for a connection past capacity.
 */
class CarrierConnectionPoolTest {

    @Test
    void reusesAReleasedConnectionInsteadOfBuildingANewOne() throws Exception {
        CarrierConnectionPool pool = new CarrierConnectionPool(1, CarrierApiConnection::new);

        int firstId;
        try (CarrierConnectionPool.Lease lease = pool.acquire(Duration.ofSeconds(1))) {
            firstId = lease.connection().id();
        }

        try (CarrierConnectionPool.Lease lease = pool.acquire(Duration.ofSeconds(1))) {
            assertThat(lease.connection().id()).isEqualTo(firstId);
        }

        assertThat(pool.connectionsCreated()).isEqualTo(1);
    }

    @Test
    void acquireBlocksPastCapacityAndTimesOutRatherThanFailingInstantly() throws Exception {
        CarrierConnectionPool pool = new CarrierConnectionPool(1, CarrierApiConnection::new);
        CarrierConnectionPool.Lease held = pool.acquire(Duration.ofSeconds(1));

        Duration timeout = Duration.ofMillis(80);
        long startNanos = System.nanoTime();
        assertThatThrownBy(() -> pool.acquire(timeout)).isInstanceOf(TimeoutException.class);
        long elapsedNanos = System.nanoTime() - startNanos;

        assertThat(elapsedNanos).isGreaterThanOrEqualTo(timeout.toNanos());
        held.close();
    }

    @Test
    void neverLetsMoreConnectionsExistConcurrentlyThanItsCapacity() throws Exception {
        int capacity = 3;
        CarrierConnectionPool pool = new CarrierConnectionPool(capacity, CarrierApiConnection::new);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();

        List<Callable<Void>> tasks = new java.util.ArrayList<>();
        for (int i = 0; i < 30; i++) {
            tasks.add(() -> {
                try (CarrierConnectionPool.Lease lease = pool.acquire(Duration.ofSeconds(2))) {
                    int now = active.incrementAndGet();
                    maxActive.updateAndGet(previous -> Math.max(previous, now));
                    assertThat(lease.connection()).isNotNull();
                    active.decrementAndGet();
                }
                return null;
            });
        }

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Void>> futures = executor.invokeAll(tasks);
            for (Future<Void> future : futures) {
                future.get();
            }
        }

        assertThat(maxActive.get()).isLessThanOrEqualTo(capacity);
        assertThat(pool.connectionsCreated()).isLessThanOrEqualTo(capacity);
    }

    @Test
    void rejectsANullFactoryOrANonPositiveCapacity() {
        assertThatThrownBy(() -> new CarrierConnectionPool(1, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CarrierConnectionPool(0, CarrierApiConnection::new))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aClosedLeaseRejectsFurtherUse() throws Exception {
        CarrierConnectionPool pool = new CarrierConnectionPool(1, CarrierApiConnection::new);
        CarrierConnectionPool.Lease lease = pool.acquire(Duration.ofSeconds(1));
        lease.close();

        assertThatThrownBy(lease::connection).isInstanceOf(IllegalStateException.class);
    }
}
