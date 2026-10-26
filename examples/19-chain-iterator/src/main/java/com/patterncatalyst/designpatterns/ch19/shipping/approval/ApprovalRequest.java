package com.patterncatalyst.designpatterns.ch19.shipping.approval;

import java.util.Objects;

/**
 * A shipment awaiting approval: its weight, whether it carries hazardous
 * material, and the country it is headed to. Every handler in the chain,
 * object or functional, reads one or more of these three fields and nothing
 * else.
 */
public record ApprovalRequest(double weightKg, boolean hazmat, String destinationCountry) {

    public ApprovalRequest {
        Objects.requireNonNull(destinationCountry, "destinationCountry");
        if (weightKg < 0.0) {
            throw new IllegalArgumentException("weightKg cannot be negative: " + weightKg);
        }
    }
}
