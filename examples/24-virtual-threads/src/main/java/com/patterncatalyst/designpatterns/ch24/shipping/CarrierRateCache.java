package com.patterncatalyst.designpatterns.ch24.shipping;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Tracks the cheapest quote seen so far per carrier while a fan-out is in
 * flight. Every virtual thread running a carrier call shares this one
 * instance, so every read and write has to be serialized.
 *
 * <p>A {@link ReentrantLock} guards the map here instead of a
 * {@code synchronized} block. Since JDK 24 (JEP 491), a virtual thread
 * blocked inside a synchronized block no longer pins its carrier thread, so
 * avoiding pinning is no longer, by itself, a reason to reach for an
 * explicit lock over {@code synchronized}. {@code ReentrantLock} is still
 * the right choice here, for a capability {@code synchronized} has never had:
 * {@link ReentrantLock#tryLock()} lets {@link #tryRecordIfCheaper} skip an
 * update under contention instead of queuing behind one, which matters when
 * hundreds of virtual threads might report a quote back at nearly the same
 * instant.
 */
public final class CarrierRateCache {

    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, RateQuote> cheapestByCarrier = new HashMap<>();

    /**
     * Records {@code quote} if no quote is on file yet for its carrier, or
     * if it undercuts the one already on file. Blocks until the lock is
     * free.
     */
    public boolean recordIfCheaper(RateQuote quote) {
        Objects.requireNonNull(quote, "quote");
        lock.lock();
        try {
            return recordIfCheaperWhileLocked(quote);
        } finally {
            lock.unlock();
        }
    }

    /**
     * The same update, but gives up immediately instead of waiting for the
     * lock. Returns {@code false} both when the quote was not an
     * improvement and when the lock was already held by another thread; a
     * caller that only wants a best-effort snapshot can poll again rather
     * than block a virtual thread it would rather keep running.
     */
    public boolean tryRecordIfCheaper(RateQuote quote) {
        Objects.requireNonNull(quote, "quote");
        if (!lock.tryLock()) {
            return false;
        }
        try {
            return recordIfCheaperWhileLocked(quote);
        } finally {
            lock.unlock();
        }
    }

    public Optional<RateQuote> cheapestOverall() {
        lock.lock();
        try {
            return cheapestByCarrier.values().stream().min(Comparator.comparingLong(RateQuote::quotedCents));
        } finally {
            lock.unlock();
        }
    }

    public int trackedCarrierCount() {
        lock.lock();
        try {
            return cheapestByCarrier.size();
        } finally {
            lock.unlock();
        }
    }

    private boolean recordIfCheaperWhileLocked(RateQuote quote) {
        RateQuote existing = cheapestByCarrier.get(quote.carrierId());
        if (existing == null || quote.quotedCents() < existing.quotedCents()) {
            cheapestByCarrier.put(quote.carrierId(), quote);
            return true;
        }
        return false;
    }
}
