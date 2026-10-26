package com.patterncatalyst.designpatterns.ch28.shipping;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

/**
 * Overrides {@code shipping.rate.strategy} to {@code flat} for
 * {@link RateQuoterProducerFlatProfileTest}, proving the producer's choice
 * of {@link RateQuoter} implementation is driven entirely by configuration
 * with no change to {@link RateQuoterProducer} itself.
 */
public class FlatRateStrategyProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("shipping.rate.strategy", "flat");
    }
}
