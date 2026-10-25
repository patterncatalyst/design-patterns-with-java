package com.patterncatalyst.designpatterns.ch08.shipping.classic;

import com.patterncatalyst.designpatterns.ch08.shipping.Money;
import com.patterncatalyst.designpatterns.ch08.shipping.RateQuote;
import com.patterncatalyst.designpatterns.ch08.shipping.RateStrategy;
import com.patterncatalyst.designpatterns.ch08.shipping.Shipment;
import java.util.Objects;

/**
 * A Classic Java {@link RateStrategy} implementation: one named class, one
 * constructor, one field assignment, one override, before a single line of
 * pricing logic runs. This is the ceremony the Java 25 lambda form in
 * {@code RateStrategies.flat(...)} collapses to one expression.
 */
public final class FlatRateStrategy implements RateStrategy {

    private final String carrierName;
    private final long flatCents;

    public FlatRateStrategy(String carrierName, long flatCents) {
        this.carrierName = Objects.requireNonNull(carrierName, "carrierName");
        if (flatCents < 0) {
            throw new IllegalArgumentException("flatCents cannot be negative: " + flatCents);
        }
        this.flatCents = flatCents;
    }

    @Override
    public RateQuote quote(Shipment shipment) {
        return new RateQuote(carrierName, Money.ofCents(flatCents));
    }
}
