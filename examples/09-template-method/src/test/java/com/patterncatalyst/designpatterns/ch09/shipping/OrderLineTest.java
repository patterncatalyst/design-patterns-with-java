package com.patterncatalyst.designpatterns.ch09.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class OrderLineTest {

    @Test
    void rejectsABlankSku() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OrderLine(" ", 1, 1_000))
                .withMessageContaining("sku");
    }

    @Test
    void rejectsANonPositiveQuantity() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OrderLine("SKU-1", 0, 1_000))
                .withMessageContaining("quantity");
    }

    @Test
    void rejectsANegativeUnitPrice() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OrderLine("SKU-1", 1, -1))
                .withMessageContaining("unitPriceCents");
    }

    @Test
    void acceptsAValidLine() {
        OrderLine line = new OrderLine("SKU-1", 3, 2_500);

        assertThat(line.sku()).isEqualTo("SKU-1");
        assertThat(line.quantity()).isEqualTo(3);
        assertThat(line.unitPriceCents()).isEqualTo(2_500L);
    }
}
