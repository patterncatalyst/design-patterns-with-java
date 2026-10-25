package com.patterncatalyst.designpatterns.ch08.shipping;

import java.util.Objects;

/**
 * The result of asking a {@link RateStrategy} to price a {@link Shipment}:
 * who quoted it, and how much. Keeping the quoted-by name on the result,
 * rather than only on the strategy object, is what lets a test compare two
 * quotes produced by two different dispatch mechanisms for equality without
 * reaching back into either strategy.
 *
 * @param quotedBy the carrier or rate plan name attached to this quote
 * @param amount   the priced amount
 */
public record RateQuote(String quotedBy, Money amount) {

    public RateQuote {
        Objects.requireNonNull(quotedBy, "quotedBy");
        Objects.requireNonNull(amount, "amount");
    }
}
