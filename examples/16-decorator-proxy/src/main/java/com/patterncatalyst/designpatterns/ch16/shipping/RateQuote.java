package com.patterncatalyst.designpatterns.ch16.shipping;

/** A priced quote: who quoted it, and how much. */
public record RateQuote(String quotedBy, Money amount) {
}
