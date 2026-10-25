package com.patterncatalyst.designpatterns.ch09.shipping;

import com.patterncatalyst.designpatterns.ch09.shipping.channel.MarketplacePipeline;
import com.patterncatalyst.designpatterns.ch09.shipping.channel.RetailWebPipeline;
import com.patterncatalyst.designpatterns.ch09.shipping.channel.WholesalePipeline;

import java.util.List;

/**
 * Runs one order through each channel using both the abstract-method and
 * functional forms, so the agreement this example's tests check for is also
 * something you can watch happen at the terminal.
 */
public final class OrderProcessingDemo {

    private OrderProcessingDemo() {
    }

    public static void main(String[] args) {
        Order retailOrder = new Order(
                "ORD-4001",
                "Avery Whitfield",
                List.of(
                        new OrderLine("SKU-100", 2, 2_500),
                        new OrderLine("SKU-205", 1, 4_000)),
                "");

        System.out.println("== Retail web channel ==");
        System.out.println("RetailWebPipeline:             " + new RetailWebPipeline().process(retailOrder));
        System.out.println("ChannelPipelines.retailWeb():  " + ChannelPipelines.retailWeb().process(retailOrder));

        Order wholesaleOrder = new Order(
                "ORD-4002",
                "Harborline Hardware Co.",
                List.of(new OrderLine("SKU-100", 60, 2_500)),
                "");
        System.out.println();
        System.out.println("== Wholesale channel ==");
        System.out.println("WholesalePipeline:             " + new WholesalePipeline().process(wholesaleOrder));
        System.out.println("ChannelPipelines.wholesale():  " + ChannelPipelines.wholesale().process(wholesaleOrder));

        Order marketplaceOrder = new Order(
                "ORD-4003",
                "Dana Oyelaran",
                List.of(new OrderLine("SKU-205", 3, 4_000)),
                "MKT-998877");
        System.out.println();
        System.out.println("== Marketplace channel ==");
        System.out.println("MarketplacePipeline:           " + new MarketplacePipeline().process(marketplaceOrder));
        System.out.println(
                "ChannelPipelines.marketplace():" + ChannelPipelines.marketplace().process(marketplaceOrder));

        System.out.println();
        System.out.println("Notice each abstract-method pipeline and its functional counterpart agree");
        System.out.println("on every field for the same order: one fixed sequence, two ways to supply");
        System.out.println("the steps that vary.");
    }
}
