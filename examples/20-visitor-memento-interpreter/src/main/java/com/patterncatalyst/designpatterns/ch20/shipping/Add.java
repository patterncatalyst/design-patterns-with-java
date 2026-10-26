package com.patterncatalyst.designpatterns.ch20.shipping;

import java.util.Objects;

/** The sum of two surcharge expressions. */
public record Add(SurchargeExpr left, SurchargeExpr right) implements SurchargeExpr {

    public Add {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
    }
}
