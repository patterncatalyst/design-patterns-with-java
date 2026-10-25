package com.patterncatalyst.designpatterns.ch09.shipping;

import com.patterncatalyst.designpatterns.ch09.shipping.channel.MarketplacePipeline;
import com.patterncatalyst.designpatterns.ch09.shipping.channel.RetailWebPipeline;
import com.patterncatalyst.designpatterns.ch09.shipping.channel.WholesalePipeline;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The claim this chapter makes about the two forms: the abstract-method
 * skeleton and the functional skeleton run the identical sequence, so an
 * identical {@link Order} produces an identical {@link ProcessingResult} no
 * matter which form processed it.
 */
class AbstractMethodAndFunctionalEquivalenceTest {

    @Test
    void retailWebAgreesAcrossBothForms() {
        Order order = new Order(
                "ORD-4001",
                "Avery Whitfield",
                List.of(
                        new OrderLine("SKU-100", 2, 2_500),
                        new OrderLine("SKU-205", 1, 4_000)),
                "");

        ProcessingResult fromSubclass = new RetailWebPipeline().process(order);
        ProcessingResult fromLambdas = ChannelPipelines.retailWeb().process(order);

        assertThat(fromSubclass).isEqualTo(fromLambdas);
    }

    @Test
    void wholesaleAgreesAcrossBothForms() {
        Order order = new Order(
                "ORD-4002", "Harborline Hardware Co.", List.of(new OrderLine("SKU-100", 60, 2_500)), "");

        ProcessingResult fromSubclass = new WholesalePipeline().process(order);
        ProcessingResult fromLambdas = ChannelPipelines.wholesale().process(order);

        assertThat(fromSubclass).isEqualTo(fromLambdas);
    }

    @Test
    void marketplaceAgreesAcrossBothForms() {
        Order order = new Order(
                "ORD-4003", "Dana Oyelaran", List.of(new OrderLine("SKU-205", 3, 4_000)), "MKT-998877");

        ProcessingResult fromSubclass = new MarketplacePipeline().process(order);
        ProcessingResult fromLambdas = ChannelPipelines.marketplace().process(order);

        assertThat(fromSubclass).isEqualTo(fromLambdas);
    }
}
