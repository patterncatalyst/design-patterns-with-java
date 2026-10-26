package com.patterncatalyst.designpatterns.ch20.shipping;

/**
 * Walks a SurchargeExpr tree two ways: evaluateCents() computes the amount
 * it denotes for a given shipment weight, and describe() renders it as
 * text for an audit log. Both switches use record patterns to deconstruct
 * each case directly into its components, rather than binding the whole
 * record and calling an accessor, the idiom this chapter pairs against
 * ManifestOperations's plain type patterns.
 */
public final class SurchargeExprs {

    private SurchargeExprs() {
    }

    public static long evaluateCents(SurchargeExpr expr, double weightKg) {
        return switch (expr) {
            case Literal(long amountCents) -> amountCents;
            case Add(var left, var right) -> evaluateCents(left, weightKg) + evaluateCents(right, weightKg);
            case Multiply(var inner, var factor) -> Math.round(evaluateCents(inner, weightKg) * factor);
            case IfWeightAbove(var threshold, var whenAbove, var whenAtOrBelow) ->
                    weightKg > threshold ? evaluateCents(whenAbove, weightKg) : evaluateCents(whenAtOrBelow, weightKg);
        };
    }

    public static String describe(SurchargeExpr expr) {
        return switch (expr) {
            case Literal(long amountCents) -> Money.ofCents(amountCents).toString();
            case Add(var left, var right) -> "(%s + %s)".formatted(describe(left), describe(right));
            case Multiply(var inner, var factor) -> "(%s * %.2f)".formatted(describe(inner), factor);
            case IfWeightAbove(var threshold, var whenAbove, var whenAtOrBelow) ->
                    "(if weight > %.1fkg then %s else %s)"
                            .formatted(threshold, describe(whenAbove), describe(whenAtOrBelow));
        };
    }
}
