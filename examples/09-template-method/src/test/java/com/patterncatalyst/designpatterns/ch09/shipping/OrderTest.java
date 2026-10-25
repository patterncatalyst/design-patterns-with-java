package com.patterncatalyst.designpatterns.ch09.shipping;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static final List<OrderLine> ONE_LINE = List.of(new OrderLine("SKU-1", 1, 1_000));

    @Test
    void rejectsABlankOrderId() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Order(" ", "Customer", ONE_LINE, ""))
                .withMessageContaining("orderId");
    }

    @Test
    void rejectsABlankCustomerName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Order("ORD-1", "", ONE_LINE, ""))
                .withMessageContaining("customerName");
    }

    @Test
    void rejectsNullLines() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Order("ORD-1", "Customer", null, ""));
    }

    @Test
    void treatsANullMarketplaceReferenceAsBlank() {
        Order order = new Order("ORD-1", "Customer", ONE_LINE, null);

        assertThat(order.marketplaceReference()).isEmpty();
    }

    @Test
    void defensivelyCopiesTheLinesSoACallerCannotMutateAnOrderAfterConstruction() {
        List<OrderLine> mutable = new ArrayList<>(ONE_LINE);
        Order order = new Order("ORD-1", "Customer", mutable, "");
        mutable.add(new OrderLine("SKU-2", 1, 500));

        assertThat(order.lines()).hasSize(1);
        assertThatThrownBy(() -> order.lines().add(new OrderLine("SKU-3", 1, 500)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
