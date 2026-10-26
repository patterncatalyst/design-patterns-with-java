package com.patterncatalyst.designpatterns.ch20.shipping;

import java.util.Objects;

/** Chooses one of two expressions based on whether a shipment's weight exceeds a threshold. */
public record IfWeightAbove(double thresholdKg, SurchargeExpr whenAbove, SurchargeExpr whenAtOrBelow)
        implements SurchargeExpr {

    public IfWeightAbove {
        Objects.requireNonNull(whenAbove, "whenAbove");
        Objects.requireNonNull(whenAtOrBelow, "whenAtOrBelow");
    }
}
