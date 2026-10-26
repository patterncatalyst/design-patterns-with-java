package com.patterncatalyst.designpatterns.ch31.shipping;

/**
 * Replaces the legacy calculator's two parallel if/else chains, one for
 * transit days and one for the base rate, both keyed on the same carrier
 * name, with one object per carrier that owns both facts together. Adding a
 * fourth carrier later means one new implementation and one new branch in
 * {@link #forCode(String)}, not a new branch in two separate methods that
 * have to be kept in sync by hand.
 */
public sealed interface CarrierProfile permits GroundCarrierProfile, AirCarrierProfile, ExpressCarrierProfile {

    int transitDays(String originZone, String destinationZone);

    long baseCents();

    static CarrierProfile forCode(String carrierCode) {
        return switch (carrierCode) {
            case "ground" -> new GroundCarrierProfile();
            case "air" -> new AirCarrierProfile();
            case "express" -> new ExpressCarrierProfile();
            default -> throw new IllegalArgumentException("unknown carrier: " + carrierCode);
        };
    }
}
