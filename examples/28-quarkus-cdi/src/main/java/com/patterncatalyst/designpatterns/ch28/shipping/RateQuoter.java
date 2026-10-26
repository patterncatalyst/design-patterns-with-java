package com.patterncatalyst.designpatterns.ch28.shipping;

/**
 * The contract both rate algorithms satisfy. Neither implementation is a
 * CDI bean in its own right; {@link RateQuoterProducer} is the only thing
 * that constructs one, which is what makes this interface this chapter's
 * Strategy-selected-by-configuration contract.
 */
public interface RateQuoter {
    RateQuote quote(Shipment shipment);
}
