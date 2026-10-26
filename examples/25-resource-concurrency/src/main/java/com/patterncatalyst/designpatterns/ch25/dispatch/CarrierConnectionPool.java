package com.patterncatalyst.designpatterns.ch25.dispatch;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A bounded pool of {@link CarrierApiConnection} instances.
 *
 * <p>A {@link Semaphore} sized to the pool's capacity is the bound: no
 * caller can ever hold more connections at once than the pool was built
 * with, no matter how many callers contend for one. An
 * {@link ArrayBlockingQueue} holds the connections that are currently idle
 * and ready for reuse; a new connection is built only the first time demand
 * exceeds what is already idle, up to the capacity.
 *
 * <p>Pooling the connection, not the thread that uses it, is the point. A
 * virtual thread is cheap enough to create one per tracking request; the
 * connection that request needs on the other end is not. Pooling virtual
 * threads themselves would defeat the reason they exist.
 */
public final class CarrierConnectionPool {

    private final int capacity;
    private final CarrierApiConnectionFactory factory;
    private final Semaphore permits;
    private final BlockingQueue<CarrierApiConnection> idle;
    private final AtomicInteger created = new AtomicInteger();

    public CarrierConnectionPool(int capacity, CarrierApiConnectionFactory factory) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive: " + capacity);
        }
        this.capacity = capacity;
        this.factory = Objects.requireNonNull(factory, "factory");
        this.permits = new Semaphore(capacity, true);
        this.idle = new ArrayBlockingQueue<>(capacity);
    }

    /**
     * Blocks the caller until a connection is available or {@code timeout}
     * elapses. A caller that never releases its lease starves every other
     * caller waiting here, which is the bound working as designed, not a
     * bug: the pool exists specifically to cap how many connections can be
     * outstanding at once.
     */
    public Lease acquire(Duration timeout) throws InterruptedException, TimeoutException {
        Objects.requireNonNull(timeout, "timeout");
        if (!permits.tryAcquire(timeout.toNanos(), TimeUnit.NANOSECONDS)) {
            throw new TimeoutException(
                    "no carrier connection available within " + timeout + " (capacity=" + capacity + ")");
        }
        CarrierApiConnection connection = idle.poll();
        if (connection == null) {
            connection = factory.create();
            created.incrementAndGet();
        }
        return new Lease(connection);
    }

    public int connectionsCreated() {
        return created.get();
    }

    public int capacity() {
        return capacity;
    }

    /**
     * A borrowed connection. {@code close()} is the only path back into the
     * idle queue; there is no separate {@code release(connection)} method
     * for a caller to forget on an exception path. Use a {@code Lease} in
     * try-with-resources.
     */
    public final class Lease implements AutoCloseable {

        private final CarrierApiConnection connection;
        private volatile boolean closed;

        private Lease(CarrierApiConnection connection) {
            this.connection = connection;
        }

        public CarrierApiConnection connection() {
            if (closed) {
                throw new IllegalStateException("lease already closed");
            }
            return connection;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            idle.offer(connection);
            permits.release();
        }
    }
}
