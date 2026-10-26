package com.patterncatalyst.designpatterns.ch20.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SurchargeExprTest {

    @Test
    void literalEvaluatesToItsOwnAmountRegardlessOfWeight() {
        SurchargeExpr expr = new Literal(750);

        assertThat(SurchargeExprs.evaluateCents(expr, 1.0)).isEqualTo(750);
        assertThat(SurchargeExprs.evaluateCents(expr, 500.0)).isEqualTo(750);
    }

    @Test
    void addSumsBothOperands() {
        SurchargeExpr expr = new Add(new Literal(300), new Literal(450));

        assertThat(SurchargeExprs.evaluateCents(expr, 1.0)).isEqualTo(750);
    }

    @Test
    void multiplyScalesByADimensionlessFactorAndRoundsToTheNearestCent() {
        SurchargeExpr expr = new Multiply(new Literal(333), 1.1);

        assertThat(SurchargeExprs.evaluateCents(expr, 1.0)).isEqualTo(366);
    }

    @Test
    void ifWeightAboveTakesTheAboveBranchWhenWeightExceedsTheThreshold() {
        SurchargeExpr expr = new IfWeightAbove(20.0, new Literal(1_000), new Literal(0));

        assertThat(SurchargeExprs.evaluateCents(expr, 25.0)).isEqualTo(1_000);
    }

    @Test
    void ifWeightAboveTakesTheAtOrBelowBranchAtExactlyTheThreshold() {
        SurchargeExpr expr = new IfWeightAbove(20.0, new Literal(1_000), new Literal(0));

        assertThat(SurchargeExprs.evaluateCents(expr, 20.0)).isEqualTo(0);
    }

    @Test
    void ifWeightAboveTakesTheAtOrBelowBranchBelowTheThreshold() {
        SurchargeExpr expr = new IfWeightAbove(20.0, new Literal(1_000), new Literal(0));

        assertThat(SurchargeExprs.evaluateCents(expr, 5.0)).isEqualTo(0);
    }

    @Test
    void nestingTheIdenticalThreePiecesDifferentlyChangesTheEvaluatedResult() {
        SurchargeExpr fuelOnHandling = new Multiply(
                new Add(new Literal(500), new IfWeightAbove(20.0, new Literal(1_000), new Literal(0))),
                1.08);
        SurchargeExpr handlingOnFuel = new Add(
                new Literal(500),
                new IfWeightAbove(20.0, new Multiply(new Literal(1_000), 1.08), new Literal(0)));

        long fuelOnHandlingResult = SurchargeExprs.evaluateCents(fuelOnHandling, 22.0);
        long handlingOnFuelResult = SurchargeExprs.evaluateCents(handlingOnFuel, 22.0);

        assertThat(fuelOnHandlingResult).isEqualTo(1_620);
        assertThat(handlingOnFuelResult).isEqualTo(1_580);
        assertThat(fuelOnHandlingResult).isNotEqualTo(handlingOnFuelResult);
    }

    @Test
    void describeRendersNestingAsParenthesizedText() {
        SurchargeExpr expr = new Multiply(new Add(new Literal(500), new Literal(1_000)), 1.08);

        assertThat(SurchargeExprs.describe(expr)).isEqualTo("(($5.00 + $10.00) * 1.08)");
    }
}
