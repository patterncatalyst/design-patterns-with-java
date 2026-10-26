package com.patterncatalyst.designpatterns.ch16.shipping.decorator;

import com.patterncatalyst.designpatterns.ch16.shipping.RateQuote;
import com.patterncatalyst.designpatterns.ch16.shipping.RateQuoter;
import com.patterncatalyst.designpatterns.ch16.shipping.Shipment;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Memoizes a quote by {@link Shipment} and skips the wrapped
 * {@link RateQuoter} entirely on a repeat lookup for an identical shipment.
 * {@code Shipment} is a record, so its generated structural equals/hashCode
 * is exactly what a cache key needs with no extra code here.
 */
public final class CachingRateQuoter implements RateQuoter {

    private final RateQuoter inner;
    private final Map<Shipment, RateQuote> cache = new ConcurrentHashMap<>();
    private final AtomicInteger hits = new AtomicInteger();
    private final AtomicInteger misses = new AtomicInteger();

    public CachingRateQuoter(RateQuoter inner) {
        this.inner = Objects.requireNonNull(inner, "inner");
    }

    @Override
    public RateQuote quote(Shipment shipment) {
        RateQuote cached = cache.get(shipment);
        if (cached != null) {
            hits.incrementAndGet();
            return cached;
        }
        misses.incrementAndGet();
        RateQuote computed = inner.quote(shipment);
        cache.put(shipment, computed);
        return computed;
    }

    public int hitCount() {
        return hits.get();
    }

    public int missCount() {
        return misses.get();
    }
}
