package com.patterncatalyst.designpatterns.ch16.shipping.decorator;

import com.patterncatalyst.designpatterns.ch16.shipping.RateQuote;
import com.patterncatalyst.designpatterns.ch16.shipping.RateQuoter;
import com.patterncatalyst.designpatterns.ch16.shipping.Shipment;

import java.util.List;
import java.util.Objects;

/**
 * Appends one line per quote to a shared call log, then forwards unchanged.
 * Wraps whatever {@link RateQuoter} is handed to it and adds no behavior a
 * caller can observe in the returned {@link RateQuote} itself.
 */
public final class LoggingRateQuoter implements RateQuoter {

    private final RateQuoter inner;
    private final List<String> callLog;

    public LoggingRateQuoter(RateQuoter inner, List<String> callLog) {
        this.inner = Objects.requireNonNull(inner, "inner");
        this.callLog = Objects.requireNonNull(callLog, "callLog");
    }

    @Override
    public RateQuote quote(Shipment shipment) {
        RateQuote result = inner.quote(shipment);
        callLog.add("quoted " + shipment.zone() + " shipment at " + result.amount());
        return result;
    }
}
