package com.patterncatalyst.designpatterns.ch14.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * {@link WarehousePricingJob} has no constructor parameter for its rate
 * source, so this suite has no way to test it against anything other than
 * the real, production {@link CarrierRateTable}. That absence of a seam is
 * the point: a test that wants to exercise a failure, a changed rate, or an
 * unavailable rate source has nowhere to inject one.
 */
class WarehousePricingJobTest {

    @Test
    void quoteBaseRateAlwaysReadsFromTheProductionGlobalTable() {
        WarehousePricingJob job = new WarehousePricingJob();

        assertThat(job.quoteBaseRate("GROUND")).isEqualTo(CarrierRateTable.INSTANCE.baseRateFor("GROUND"));
    }
}
