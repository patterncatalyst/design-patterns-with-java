package com.patterncatalyst.designpatterns.ch09.shipping;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ChannelPipelinesTest {

    @Test
    void retailWebPricesAtListPrice() {
        Order order = new Order("ORD-1", "Customer", List.of(new OrderLine("SKU-1", 2, 2_500)), "");

        ProcessingResult result = ChannelPipelines.retailWeb().process(order);

        assertThat(result.totalCents()).isEqualTo(5_000L);
    }

    @Test
    void wholesaleRejectsAnOrderBelowTheMinimumUnits() {
        Order order = new Order("ORD-2", "Customer", List.of(new OrderLine("SKU-1", 5, 1_000)), "");

        assertThatIllegalArgumentException().isThrownBy(() -> ChannelPipelines.wholesale().process(order));
    }

    @Test
    void marketplaceRejectsAnOrderWithNoReference() {
        Order order = new Order("ORD-3", "Customer", List.of(new OrderLine("SKU-1", 1, 1_000)), "");

        assertThatIllegalArgumentException().isThrownBy(() -> ChannelPipelines.marketplace().process(order));
    }
}
