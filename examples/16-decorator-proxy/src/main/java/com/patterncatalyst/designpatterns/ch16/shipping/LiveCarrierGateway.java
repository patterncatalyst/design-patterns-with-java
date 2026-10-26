package com.patterncatalyst.designpatterns.ch16.shipping;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The real rate-lookup subject every decorator stack wraps and every proxy
 * in this chapter stands in front of. In production this would open a
 * session against a carrier's live tariff API before the first lookup can
 * run; here, {@link #instancesCreated()} stands in for that construction
 * cost so a test can prove a proxy deferred it rather than just asserting
 * that a lookup returned the right number.
 */
public final class LiveCarrierGateway implements RateQuoter {

    private static final AtomicInteger INSTANCES_CREATED = new AtomicInteger(0);

    private final String carrierName;

    public LiveCarrierGateway(String carrierName) {
        this.carrierName = Objects.requireNonNull(carrierName, "carrierName");
        INSTANCES_CREATED.incrementAndGet();
    }

    public static int instancesCreated() {
        return INSTANCES_CREATED.get();
    }

    @Override
    public RateQuote quote(Shipment shipment) {
        long cents = ShippingMath.baseRateCents(shipment.zone(), shipment.weightKg());
        return new RateQuote(carrierName, Money.ofCents(cents));
    }
}
