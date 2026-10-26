package com.patterncatalyst.designpatterns.ch16.shipping.proxy;

import com.patterncatalyst.designpatterns.ch16.shipping.LiveCarrierGateway;
import com.patterncatalyst.designpatterns.ch16.shipping.RateQuote;
import com.patterncatalyst.designpatterns.ch16.shipping.RateQuoter;
import com.patterncatalyst.designpatterns.ch16.shipping.Shipment;

import java.util.Objects;

/**
 * A hand-written Proxy implementing the exact interface its real subject
 * implements. It defers constructing the {@link LiveCarrierGateway} until
 * the first call that actually needs a quote, then forwards every call
 * after that to the same instance. A caller holding a {@link RateQuoter}
 * cannot tell this proxy apart from a real gateway that happened to
 * construct instantly; that is the point, not a side effect.
 */
public final class LazyCarrierGatewayProxy implements RateQuoter {

    private final String carrierName;
    private volatile LiveCarrierGateway real;

    public LazyCarrierGatewayProxy(String carrierName) {
        this.carrierName = Objects.requireNonNull(carrierName, "carrierName");
    }

    public boolean isLoaded() {
        return real != null;
    }

    @Override
    public RateQuote quote(Shipment shipment) {
        if (real == null) {
            synchronized (this) {
                if (real == null) {
                    real = new LiveCarrierGateway(carrierName);
                }
            }
        }
        return real.quote(shipment);
    }
}
