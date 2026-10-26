package com.patterncatalyst.designpatterns.ch15.shipping;

import com.patterncatalyst.designpatterns.ch15.shipping.globalcourier.GlobalCourierRateClient;

import java.util.Objects;

/**
 * The Adapter. It implements {@link CarrierGateway} by holding a
 * {@link GlobalCourierRateClient} as a field and translating on every call:
 * kilograms to grams, {@link DestinationZone} to GlobalCourier's own zone
 * code, and a raw {@code long} back into this app's {@link Money}.
 *
 * <p>This is an object adapter: composition, not inheritance. GlobalCourier's
 * client is {@code final}, so Java would not even let this class extend it,
 * but the choice would be the same either way. A class adapter ties this
 * translation to exactly one legacy type for its whole lifetime; composition
 * keeps {@code GlobalCourierGatewayAdapter} free to wrap a different vendor's
 * client, or two of them, without touching its own type hierarchy.
 */
public final class GlobalCourierGatewayAdapter implements CarrierGateway {

    private final GlobalCourierRateClient legacyClient;

    public GlobalCourierGatewayAdapter(GlobalCourierRateClient legacyClient) {
        this.legacyClient = Objects.requireNonNull(legacyClient, "legacyClient");
    }

    @Override
    public Money quote(Shipment shipment) {
        long weightGrams = Math.round(shipment.weightKg() * 1000.0);
        int zoneCode = zoneToLegacyCode(shipment.zone());
        return Money.ofCents(legacyClient.rateCentsForParcel(weightGrams, zoneCode));
    }

    private static int zoneToLegacyCode(DestinationZone zone) {
        return switch (zone) {
            case LOCAL -> 0;
            case REGIONAL -> 1;
            case NATIONAL -> 2;
            case INTERNATIONAL -> 3;
        };
    }
}
