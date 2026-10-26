package com.patterncatalyst.designpatterns.ch20.shipping;

import java.util.Objects;

/**
 * An expression scaled by a dimensionless factor, such as a 1.08 fuel
 * surcharge. Multiply takes one SurchargeExpr and a plain factor rather
 * than two expressions, because multiplying two cents amounts together
 * produces a unit (cents squared) nothing in this domain means.
 */
public record Multiply(SurchargeExpr expr, double factor) implements SurchargeExpr {

    public Multiply {
        Objects.requireNonNull(expr, "expr");
    }
}
