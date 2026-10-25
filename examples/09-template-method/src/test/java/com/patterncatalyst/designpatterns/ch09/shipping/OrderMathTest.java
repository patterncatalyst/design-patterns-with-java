package com.patterncatalyst.designpatterns.ch09.shipping;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class OrderMathTest {

    private static final Order TWO_LINE_ORDER = new Order(
            "ORD-1",
            "Customer",
            List.of(
                    new OrderLine("SKU-1", 2, 2_500),
                    new OrderLine("SKU-2", 3, 1_000)),
            "");

    @Test
    void sumsQuantityAcrossEveryLine() {
        assertThat(OrderMath.totalQuantity(TWO_LINE_ORDER)).isEqualTo(5);
    }

    @Test
    void sumsListPriceAcrossEveryLine() {
        // 2 * 2500 + 3 * 1000 = 8000
        assertThat(OrderMath.listPriceCents(TWO_LINE_ORDER)).isEqualTo(8_000L);
    }

    @Test
    void appliesAPercentageReductionToCents() {
        assertThat(OrderMath.applyPercentageReduction(10_000L, 0.12)).isEqualTo(8_800L);
    }

    @Test
    void rejectsANegativeReductionRate() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> OrderMath.applyPercentageReduction(10_000L, -0.01));
    }

    @Test
    void rejectsAReductionRateOfOneHundredPercentOrMore() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> OrderMath.applyPercentageReduction(10_000L, 1.0));
    }

    @Test
    void requireAtLeastOneLinePassesSilentlyWhenLinesArePresent() {
        OrderMath.requireAtLeastOneLine(TWO_LINE_ORDER);
    }

    @Test
    void requireAtLeastOneLineRejectsAnEmptyOrder() {
        Order empty = new Order("ORD-2", "Customer", List.of(), "");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> OrderMath.requireAtLeastOneLine(empty))
                .withMessageContaining("ORD-2");
    }
}
