package com.patterncatalyst.designpatterns.ch16.shipping;

/**
 * The single contract every Decorator layer and both Proxy expressions in
 * this chapter implement. A caller holding a {@code RateQuoter} cannot tell,
 * from the interface alone, whether it holds the plain core, a stack of
 * decorators three layers deep, or a proxy standing in front of either one.
 */
public interface RateQuoter {
    RateQuote quote(Shipment shipment);
}
