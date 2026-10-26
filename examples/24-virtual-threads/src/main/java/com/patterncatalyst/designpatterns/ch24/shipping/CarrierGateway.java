package com.patterncatalyst.designpatterns.ch24.shipping;

/**
 * A single carrier's rate API. A real implementation makes a blocking network
 * call; {@link RateQuoteFanOutService} runs every call on its own virtual
 * thread so that block never ties up a platform thread.
 */
@FunctionalInterface
public interface CarrierGateway {

    RateQuote fetchQuote(Shipment shipment, String carrierId) throws InterruptedException;
}
