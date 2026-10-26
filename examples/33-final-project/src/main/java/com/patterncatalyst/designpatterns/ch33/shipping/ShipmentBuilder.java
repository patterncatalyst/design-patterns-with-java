package com.patterncatalyst.designpatterns.ch33.shipping;

import java.util.Map;
import java.util.Objects;

/**
 * Builds a {@link Shipment} step by step and resolves the {@link RateStrategy}
 * it opens with. A caller names a carrier code; {@code build()} looks up the
 * matching strategy from a small, known table and hands back a
 * {@code Shipment} that already carries its opening quote. {@link #rateStrategy}
 * overrides that lookup directly, for a carrier contract this table does not
 * know about yet.
 *
 * <p>This is the only seam in the example where object creation and Strategy
 * selection meet: nothing downstream of {@code build()}, not
 * {@link ShipmentDispatchCenter}, not any {@link ShipmentCommand}, ever
 * names {@link FlatRateStrategy} or {@link WeightTieredRateStrategy}
 * directly.
 */
public final class ShipmentBuilder {

    private static final Map<String, RateStrategy> KNOWN_CARRIERS = Map.of(
            "GROUND", new WeightTieredRateStrategy("ground", 500, 180),
            "AIR", new FlatRateStrategy("air", 1_999),
            "FREIGHT", new WeightTieredRateStrategy("freight", 2_400, 95));

    private String id;
    private String carrierCode;
    private DestinationZone destinationZone;
    private Double weightKg;
    private RateStrategy rateStrategy;

    public ShipmentBuilder id(String id) {
        this.id = id;
        return this;
    }

    public ShipmentBuilder carrier(String carrierCode) {
        this.carrierCode = carrierCode;
        return this;
    }

    public ShipmentBuilder destinationZone(DestinationZone destinationZone) {
        this.destinationZone = destinationZone;
        return this;
    }

    public ShipmentBuilder weightKg(double weightKg) {
        this.weightKg = weightKg;
        return this;
    }

    /** Overrides the known-carrier lookup with a caller-supplied strategy. */
    public ShipmentBuilder rateStrategy(RateStrategy rateStrategy) {
        this.rateStrategy = rateStrategy;
        return this;
    }

    public Shipment build() {
        Objects.requireNonNull(id, "id must be set before build()");
        Objects.requireNonNull(carrierCode, "carrier must be set before build()");
        Objects.requireNonNull(destinationZone, "destinationZone must be set before build()");
        Objects.requireNonNull(weightKg, "weightKg must be set before build()");

        RateStrategy resolvedStrategy = rateStrategy != null ? rateStrategy : resolveStrategy(carrierCode);
        return new Shipment(id, carrierCode, destinationZone, weightKg, resolvedStrategy);
    }

    private static RateStrategy resolveStrategy(String carrierCode) {
        RateStrategy strategy = KNOWN_CARRIERS.get(carrierCode);
        if (strategy == null) {
            throw new IllegalArgumentException(
                    "ShipmentBuilder: unknown carrier code '%s' and no rateStrategy override was given"
                            .formatted(carrierCode));
        }
        return strategy;
    }
}
