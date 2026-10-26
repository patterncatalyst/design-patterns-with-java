package com.patterncatalyst.designpatterns.ch28.shipping;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * This chapter's Factory, built as a CDI producer method instead of a
 * hand-written factory class. {@link #rateQuoter()} is the one place that
 * decides which {@link RateQuoter} implementation a deployment gets; every
 * other bean in this example depends on the {@code RateQuoter} interface
 * and never names {@link FlatRateQuoter} or {@link WeightTieredRateQuoter}
 * directly.
 */
@ApplicationScoped
public class RateQuoterProducer {

    private final RateConfig rateConfig;

    @Inject
    public RateQuoterProducer(RateConfig rateConfig) {
        this.rateConfig = rateConfig;
    }

    @Produces
    @ApplicationScoped
    public RateQuoter rateQuoter() {
        return switch (rateConfig.strategy()) {
            case "flat" -> new FlatRateQuoter(rateConfig.baseCents());
            case "weight-tiered" -> new WeightTieredRateQuoter(rateConfig.baseCents(), rateConfig.perKgCents());
            default -> throw new IllegalStateException(
                    "unknown shipping.rate.strategy: " + rateConfig.strategy());
        };
    }
}
