package com.patterncatalyst.designpatterns.ch22.fulfillment;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Declarative stream pipelines over a nightly batch of shipments, each
 * replacing a hand-written loop that would otherwise declare an
 * accumulator, iterate the list, and mutate the accumulator once per
 * element. {@link #totalSurchargeCents} and {@link #surchargeByTier} read as
 * what they compute, a sum and a grouped sum, rather than as how a loop
 * would compute them. The grouping collectors build an {@link EnumMap}
 * rather than the default {@code HashMap}, so a tier's position in its own
 * {@code enum} declaration, not its hash code, decides the iteration order
 * a caller sees.
 */
public final class FulfillmentBatch {

    private FulfillmentBatch() {
    }

    public static long totalSurchargeCents(List<Shipment> shipments) {
        Objects.requireNonNull(shipments, "shipments");
        return shipments.stream()
                .mapToLong(shipment -> shipment.tier().surchargeCents(shipment))
                .sum();
    }

    public static List<Shipment> needingExtraHandling(List<Shipment> shipments) {
        Objects.requireNonNull(shipments, "shipments");
        return shipments.stream()
                .filter(shipment -> shipment.tier() != PackagingTier.STANDARD)
                .toList();
    }

    public static Map<PackagingTier, Long> surchargeByTier(List<Shipment> shipments) {
        Objects.requireNonNull(shipments, "shipments");
        return shipments.stream()
                .collect(Collectors.groupingBy(
                        Shipment::tier,
                        () -> new EnumMap<>(PackagingTier.class),
                        Collectors.summingLong(shipment -> shipment.tier().surchargeCents(shipment))));
    }

    public static Map<PackagingTier, Long> countByTier(List<Shipment> shipments) {
        Objects.requireNonNull(shipments, "shipments");
        return shipments.stream()
                .collect(Collectors.groupingBy(
                        Shipment::tier, () -> new EnumMap<>(PackagingTier.class), Collectors.counting()));
    }
}
