package com.patterncatalyst.designpatterns.ch14.shipping;

/**
 * The one capability a shipping-quote caller actually needs: a base,
 * per-kilogram rate for a carrier code. Both singleton forms in this
 * package, {@link CarrierRateTable} and {@link HolderCarrierRateTable},
 * satisfy this contract, and so does any test double a caller injects in
 * place of either one. It declares exactly one abstract method, so it is a
 * functional interface: a lambda satisfies it just as well as an enum
 * constant or a holder class does.
 */
@FunctionalInterface
public interface RateLookup {

    /**
     * Returns the base rate, in cents per kilogram, for the given carrier
     * code.
     *
     * @throws IllegalArgumentException if the carrier code is not in the
     *     rate table
     */
    Money baseRateFor(String carrierCode);
}
