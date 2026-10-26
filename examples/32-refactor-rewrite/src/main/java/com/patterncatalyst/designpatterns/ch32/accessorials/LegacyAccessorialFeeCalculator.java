package com.patterncatalyst.designpatterns.ch32.accessorials;

/**
 * The component nobody wants to touch. It has priced every accessorial fee
 * in production for years, and three of its four branches encode a rule
 * nobody currently on the team can trace to a specification, only to the
 * comments left behind by whoever found it the hard way. This class is
 * deliberately left in its inherited shape: a long method, string-equality
 * carrier checks, and magic numbers, so the chapter has something real to
 * pin with a characterization test before touching it.
 */
public class LegacyAccessorialFeeCalculator implements AccessorialFeeCalculator {

    @Override
    public AccessorialFees calculateFees(ShipmentManifest manifest) {
        String carrier = manifest.carrierCode();

        long fuelSurcharge;
        if (carrier.equals("OCEAN")) {
            // bumped from 0.12 to 0.15 during the 2019 contract renegotiation;
            // nobody who signed that contract still works here
            fuelSurcharge = Math.round(manifest.declaredValueCents() * 0.15);
        } else if (carrier.equals("AIR")) {
            fuelSurcharge = Math.round(manifest.declaredValueCents() * 0.22);
        } else if (carrier.equals("GROUND")) {
            fuelSurcharge = Math.round(manifest.declaredValueCents() * 0.08);
        } else {
            fuelSurcharge = Math.round(manifest.declaredValueCents() * 0.10);
        }

        long residentialFee = 0L;
        if (manifest.residentialDelivery()) {
            // packages at or under 20kg were exempted from the residential
            // fee at some point; the ticket that explains why is long gone
            if (manifest.weightKg() > 20.0) {
                residentialFee = 450L;
            }
        }

        long liftgateFee = 0L;
        if (manifest.liftgateRequired()) {
            liftgateFee = 350L;
            if (manifest.oversized()) {
                liftgateFee = liftgateFee + 100L;
            }
        }

        long oversizeFee = 0L;
        if (manifest.oversized()) {
            oversizeFee = 900L;
            if (carrier.equals("AIR")) {
                // air freight doubles the oversize penalty; found by an
                // auditor comparing invoices against the carrier contract,
                // never written down anywhere this code can point to
                oversizeFee = oversizeFee * 2L;
            }
        }

        return new AccessorialFees(fuelSurcharge, residentialFee, liftgateFee, oversizeFee);
    }
}
