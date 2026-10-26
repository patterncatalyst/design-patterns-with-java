package com.patterncatalyst.designpatterns.ch28.shipping;

import io.smallrye.config.ConfigMapping;

/**
 * Typed configuration for which rate strategy the checkout pipeline uses,
 * bound from {@code shipping.rate.*} in {@code application.properties}
 * with no parsing code anywhere. This is this chapter's externalized
 * Strategy selection: the set of algorithms is the same two classes either
 * way, and {@code strategy()} is the only thing that decides which one a
 * deployment runs.
 */
@ConfigMapping(prefix = "shipping.rate")
public interface RateConfig {

    String strategy();

    long baseCents();

    long perKgCents();
}
