package com.patterncatalyst.designpatterns.ch17.shipping;

import java.util.Map;

/**
 * The factory and pool for {@link PackagingSpec} flyweights, keyed by
 * packaging code. {@link #get(String)} always returns the one shared
 * instance for a given code; {@link #materializedCount()} reports how many
 * distinct codes something has actually asked for, not how many codes the
 * catalog knows about.
 */
public final class PackagingSpecCatalog {

    private final FlyweightPool<String, PackagingSpec> pool;

    public PackagingSpecCatalog(Map<String, PackagingSpec> registry) {
        this.pool = new FlyweightPool<>(registry);
    }

    public PackagingSpec get(String code) {
        return pool.get(code);
    }

    public int materializedCount() {
        return pool.size();
    }
}
