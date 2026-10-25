package com.patterncatalyst.designpatterns.ch09.shipping.channel;

import com.patterncatalyst.designpatterns.ch09.shipping.Order;
import com.patterncatalyst.designpatterns.ch09.shipping.OrderLine;
import com.patterncatalyst.designpatterns.ch09.shipping.ProcessingResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class MarketplacePipelineTest {

    private final MarketplacePipeline pipeline = new MarketplacePipeline();

    @Test
    void deductsTheCommissionWhenAReferenceIsPresent() {
        Order order = new Order(
                "ORD-1", "Dana Oyelaran", List.of(new OrderLine("SKU-205", 3, 4_000)), "MKT-998877");

        ProcessingResult result = pipeline.process(order);

        // list price 12_000, 15% commission -> 10_200
        assertThat(result.channel()).isEqualTo("marketplace");
        assertThat(result.totalCents()).isEqualTo(10_200L);
        assertThat(result.ledgerNote())
                .contains("marketplace:ORD-1")
                .contains("ref=MKT-998877")
                .contains("commission=0.15");
    }

    @Test
    void rejectsAnOrderWithNoMarketplaceReference() {
        Order order = new Order("ORD-2", "Customer", List.of(new OrderLine("SKU-205", 1, 4_000)), "");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> pipeline.process(order))
                .withMessageContaining("marketplaceReference");
    }
}
