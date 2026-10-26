package com.patterncatalyst.designpatterns.ch32.accessorials;

/**
 * The stable interface every caller depends on. Neither
 * {@link LegacyAccessorialFeeCalculator}, {@link ModernAccessorialFeeCalculator},
 * nor {@link StranglerFeeCalculatorRouter} is visible to a caller; only this
 * contract is, which is what lets the implementation behind it change
 * without touching a single call site.
 */
public interface AccessorialFeeCalculator {

    AccessorialFees calculateFees(ShipmentManifest manifest);
}
