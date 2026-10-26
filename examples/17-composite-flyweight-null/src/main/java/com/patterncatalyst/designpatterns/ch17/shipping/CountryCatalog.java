package com.patterncatalyst.designpatterns.ch17.shipping;

import java.util.Map;

/**
 * The factory and pool for {@link Country} flyweights, keyed by ISO code.
 * Shares the same {@link FlyweightPool} machinery {@link PackagingSpecCatalog}
 * uses, parameterized over a different intrinsic value type.
 */
public final class CountryCatalog {

    private final FlyweightPool<String, Country> pool;

    public CountryCatalog(Map<String, Country> registry) {
        this.pool = new FlyweightPool<>(registry);
    }

    public Country get(String isoCode) {
        return pool.get(isoCode);
    }

    public int materializedCount() {
        return pool.size();
    }
}
