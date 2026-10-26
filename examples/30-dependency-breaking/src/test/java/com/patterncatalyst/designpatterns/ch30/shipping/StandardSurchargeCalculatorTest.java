package com.patterncatalyst.designpatterns.ch30.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StandardSurchargeCalculatorTest {

    private final StandardSurchargeCalculator calculator = new StandardSurchargeCalculator();

    @Test
    void appliesAnEightPercentSurcharge() {
        assertThat(calculator.surchargeCents(10_000)).isEqualTo(800);
    }

    @Test
    void roundsToTheNearestCent() {
        assertThat(calculator.surchargeCents(125)).isEqualTo(10);
    }

    @Test
    void returnsZeroForAZeroBaseCost() {
        assertThat(calculator.surchargeCents(0)).isZero();
    }
}
