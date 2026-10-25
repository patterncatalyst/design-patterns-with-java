package com.patterncatalyst.designpatterns.ch03.billing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class LineItemTest {

    @Test
    void extendedPriceMultipliesQuantityByUnitPrice() {
        LineItem item = new LineItem("Freight handling", 3, 82_500);

        assertThat(item.extendedPriceCents()).isEqualTo(247_500);
    }

    @Test
    void rejectsNonPositiveQuantity() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LineItem("Freight handling", 0, 82_500))
                .withMessageContaining("quantity");
    }

    @Test
    void rejectsNegativeUnitPrice() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LineItem("Freight handling", 1, -1))
                .withMessageContaining("unitPriceCents");
    }

    @Test
    void rejectsBlankDescription() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LineItem("   ", 1, 100))
                .withMessageContaining("description");
    }

    @Test
    void rejectsNullDescription() {
        assertThatNullPointerException().isThrownBy(() -> new LineItem(null, 1, 100));
    }
}
