package com.patterncatalyst.designpatterns.ch28.shipping;

/** A carrier's quoted price for a {@link Shipment}. */
public record RateQuote(String quotedBy, Money amount) {
}
