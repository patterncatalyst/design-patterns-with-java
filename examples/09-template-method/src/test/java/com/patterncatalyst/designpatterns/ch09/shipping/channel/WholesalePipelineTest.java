package com.patterncatalyst.designpatterns.ch09.shipping.channel;

import com.patterncatalyst.designpatterns.ch09.shipping.Order;
import com.patterncatalyst.designpatterns.ch09.shipping.OrderLine;
import com.patterncatalyst.designpatterns.ch09.shipping.ProcessingResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class WholesalePipelineTest {

    private final WholesalePipeline pipeline = new WholesalePipeline();

    @Test
    void appliesTheVolumeDiscountWhenTheMinimumIsMet() {
        Order order = new Order("ORD-1", "Harborline Hardware Co.", List.of(new OrderLine("SKU-100", 60, 2_500)), "");

        ProcessingResult result = pipeline.process(order);

        // list price 150_000, 12% discount -> 132_000
        assertThat(result.channel()).isEqualTo("wholesale");
        assertThat(result.totalCents()).isEqualTo(132_000L);
        assertThat(result.ledgerNote()).contains("wholesale:ORD-1").contains("discount=0.12");
    }

    @Test
    void rejectsAnOrderBelowTheMinimumUnits() {
        Order order = new Order("ORD-2", "Customer", List.of(new OrderLine("SKU-100", 10, 2_500)), "");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> pipeline.process(order))
                .withMessageContaining("50");
    }
}
