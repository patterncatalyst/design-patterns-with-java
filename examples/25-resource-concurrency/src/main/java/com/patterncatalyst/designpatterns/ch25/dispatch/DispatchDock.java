package com.patterncatalyst.designpatterns.ch25.dispatch;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A loading-dock bay shared by every warehouse scanner staging parcels for
 * the next outbound truck.
 *
 * <p>{@code DispatchDock} is a monitor object: it owns its own
 * {@link ReentrantLock} and takes it at the top of every public method, so
 * no caller ever sees the lock, names it, or can forget to take it before
 * touching the staged list. A plain class that exposed its lock, or
 * exposed nothing and trusted every call site to synchronize on its own,
 * would push that discipline out to callers, where one missed lock corrupts
 * the bay's count under load. Collapsing the lock into the method body
 * makes that bug impossible to write here, not merely unlikely.
 */
public final class DispatchDock {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition bayFull = lock.newCondition();
    private final List<ParcelScan> staged = new ArrayList<>();
    private final int bayCapacity;

    public DispatchDock(int bayCapacity) {
        if (bayCapacity <= 0) {
            throw new IllegalArgumentException("bayCapacity must be positive: " + bayCapacity);
        }
        this.bayCapacity = bayCapacity;
    }

    public void stageParcel(ParcelScan scan) {
        Objects.requireNonNull(scan, "scan");
        lock.lock();
        try {
            staged.add(scan);
            if (staged.size() >= bayCapacity) {
                bayFull.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Blocks the calling thread, the dispatcher, until the bay holds
     * {@code bayCapacity} parcels or {@code timeout} elapses. Returns an
     * immutable snapshot of the staged parcels and empties the bay for the
     * next truck; the snapshot is a defensive copy, so the caller can read
     * it after the lock is released without a second thread mutating it
     * underneath.
     */
    public List<ParcelScan> awaitFullBay(Duration timeout) throws InterruptedException {
        Objects.requireNonNull(timeout, "timeout");
        lock.lock();
        try {
            long nanosRemaining = timeout.toNanos();
            while (staged.size() < bayCapacity) {
                if (nanosRemaining <= 0L) {
                    throw new IllegalStateException(
                            "bay not full within " + timeout + " (staged=" + staged.size() + " of " + bayCapacity
                                    + ")");
                }
                nanosRemaining = bayFull.awaitNanos(nanosRemaining);
            }
            List<ParcelScan> manifest = List.copyOf(staged);
            staged.clear();
            return manifest;
        } finally {
            lock.unlock();
        }
    }

    public int stagedCount() {
        lock.lock();
        try {
            return staged.size();
        } finally {
            lock.unlock();
        }
    }
}
