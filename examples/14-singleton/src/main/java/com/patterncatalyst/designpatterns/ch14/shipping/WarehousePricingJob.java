package com.patterncatalyst.designpatterns.ch14.shipping;

/**
 * A warehouse batch job that prices every outbound shipment at the end of a
 * shift. It reaches directly into the global {@link CarrierRateTable}
 * rather than taking a {@link RateLookup} through its constructor, which is
 * exactly the hidden-dependency shape this chapter argues against: nothing
 * in {@link #quoteBaseRate(String)}'s signature, or in this class's
 * constructor, tells a reader that pricing depends on
 * {@code CarrierRateTable.INSTANCE} at all. A reader has to open the method
 * body to find out, and a test has no way to substitute a different rate
 * table without changing production code, because there is no seam left
 * for a test to use.
 */
public final class WarehousePricingJob {

    public Money quoteBaseRate(String carrierCode) {
        return CarrierRateTable.INSTANCE.baseRateFor(carrierCode);
    }
}
