package com.patterncatalyst.designpatterns.ch22.fulfillment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Each method here is a declarative stream pipeline standing in for a
 * hand-written loop: a sum, a filter, and two groupings. These assert the
 * computed values directly, not just that the pipeline runs without
 * throwing.
 */
class FulfillmentBatchTest {

    private static final List<Shipment> BATCH = List.of(
            new Shipment("RETAIL-1001", 2.0, DestinationZone.REGIONAL, PackagingTier.STANDARD, 8_000),
            new Shipment("RETAIL-1002", 1.5, DestinationZone.LOCAL, PackagingTier.FRAGILE, 60_000),
            new Shipment("WHOLESALE-2001", 35.0, DestinationZone.NATIONAL, PackagingTier.OVERSIZED, 450_000),
            new Shipment("WHOLESALE-2002", 5.0, DestinationZone.NATIONAL, PackagingTier.STANDARD, 10_000),
            new Shipment("RETAIL-1003", 0.8, DestinationZone.LOCAL, PackagingTier.STANDARD, 3_000));

    @Test
    void totalSurchargeCentsSumsEveryShipmentsTierSurcharge() {
        // FRAGILE 410 + OVERSIZED 3525 + three STANDARD at 0 each = 3935.
        assertThat(FulfillmentBatch.totalSurchargeCents(BATCH)).isEqualTo(3_935L);
    }

    @Test
    void needingExtraHandlingFiltersOutStandardPackaging() {
        List<Shipment> extraHandling = FulfillmentBatch.needingExtraHandling(BATCH);

        assertThat(extraHandling).extracting(Shipment::orderId)
                .containsExactly("RETAIL-1002", "WHOLESALE-2001");
    }

    @Test
    void surchargeByTierGroupsAndSumsInOnePass() {
        Map<PackagingTier, Long> byTier = FulfillmentBatch.surchargeByTier(BATCH);

        assertThat(byTier).containsExactlyInAnyOrderEntriesOf(Map.of(
                PackagingTier.STANDARD, 0L,
                PackagingTier.FRAGILE, 410L,
                PackagingTier.OVERSIZED, 3_525L));
    }

    @Test
    void countByTierGroupsAndCountsInOnePass() {
        Map<PackagingTier, Long> counts = FulfillmentBatch.countByTier(BATCH);

        assertThat(counts).containsExactlyInAnyOrderEntriesOf(Map.of(
                PackagingTier.STANDARD, 3L,
                PackagingTier.FRAGILE, 1L,
                PackagingTier.OVERSIZED, 1L));
    }

    @Test
    void anEmptyBatchProducesEmptyOrZeroResultsRatherThanThrowing() {
        assertThat(FulfillmentBatch.totalSurchargeCents(List.of())).isZero();
        assertThat(FulfillmentBatch.needingExtraHandling(List.of())).isEmpty();
        assertThat(FulfillmentBatch.surchargeByTier(List.of())).isEmpty();
    }
}
