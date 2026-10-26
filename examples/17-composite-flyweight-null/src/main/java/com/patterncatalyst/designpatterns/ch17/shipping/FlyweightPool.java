package com.patterncatalyst.designpatterns.ch17.shipping;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A generic interning cache. {@code registry} holds every known intrinsic
 * value a caller might ask for, built once at construction; {@code pool}
 * only ever holds the values something has actually requested, so a large
 * registry that nothing uses yet materializes nothing. Two {@link #get}
 * calls for the same key return the identical instance from {@code
 * registry}, which is what lets a caller prove the sharing with a
 * reference-identity check instead of trusting field equality.
 *
 * @param <K> the key a caller looks a flyweight up by
 * @param <V> the immutable, shareable value type
 */
public final class FlyweightPool<K, V> {

    private final Map<K, V> registry;
    private final Map<K, V> pool = new ConcurrentHashMap<>();

    public FlyweightPool(Map<K, V> registry) {
        this.registry = Map.copyOf(registry);
    }

    public V get(K key) {
        V registered = registry.get(key);
        if (registered == null) {
            throw new NoSuchElementException("unregistered key: " + key);
        }
        return pool.computeIfAbsent(key, ignoredKey -> registered);
    }

    public int size() {
        return pool.size();
    }
}
