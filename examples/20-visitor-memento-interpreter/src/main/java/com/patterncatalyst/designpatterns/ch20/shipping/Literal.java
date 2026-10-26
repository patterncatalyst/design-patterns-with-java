package com.patterncatalyst.designpatterns.ch20.shipping;

/** A fixed amount, in cents, independent of the shipment being priced. */
public record Literal(long amountCents) implements SurchargeExpr {
}
