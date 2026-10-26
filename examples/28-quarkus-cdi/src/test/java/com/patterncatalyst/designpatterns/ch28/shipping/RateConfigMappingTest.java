package com.patterncatalyst.designpatterns.ch28.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RateConfigMappingTest {

    @Inject
    RateConfig rateConfig;

    @Test
    void configMappingBindsApplicationPropertiesToATypedInterfaceWithNoParsingCode() {
        assertThat(rateConfig.strategy()).isEqualTo("weight-tiered");
        assertThat(rateConfig.baseCents()).isEqualTo(500L);
        assertThat(rateConfig.perKgCents()).isEqualTo(180L);
    }
}
