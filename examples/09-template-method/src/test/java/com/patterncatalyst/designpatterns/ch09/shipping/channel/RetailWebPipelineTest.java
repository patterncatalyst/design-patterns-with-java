package com.patterncatalyst.designpatterns.ch09.shipping.channel;

import com.patterncatalyst.designpatterns.ch09.shipping.Order;
import com.patterncatalyst.designpatterns.ch09.shipping.OrderLine;
import com.patterncatalyst.designpatterns.ch09.shipping.ProcessingResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RetailWebPipelineTest {

    private final RetailWebPipeline pipeline = new RetailWebPipeline();

    @Test
    void pricesAtListPriceWithNoDiscountOrCommission() {
        Order order = new Order(
                "ORD-1",
                "Avery Whitfield",
                List.of(
                        new OrderLine("SKU-100", 2, 2_500),
                        new OrderLine("SKU-205", 1, 4_000)),
                "");

        ProcessingResult result = pipeline.process(order);

        assertThat(result.channel()).isEqualTo("retail-web");
        assertThat(result.totalCents()).isEqualTo(9_000L);
        assertThat(result.lineCount()).isEqualTo(3);
        assertThat(result.ledgerNote()).isEqualTo("retail-web:ORD-1");
    }

    @Test
    void rejectsAnOrderOverTheLineCap() {
        List<OrderLine> tooManyLines = new ArrayList<>();
        for (int i = 0; i < 41; i++) {
            tooManyLines.add(new OrderLine("SKU-" + i, 1, 100));
        }
        Order order = new Order("ORD-2", "Customer", tooManyLines, "");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> pipeline.process(order))
                .withMessageContaining("40");
    }
}
