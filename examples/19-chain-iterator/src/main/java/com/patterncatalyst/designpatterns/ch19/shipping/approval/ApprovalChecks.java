package com.patterncatalyst.designpatterns.ch19.shipping.approval;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * The same three rules as the object-chain handlers, each expressed as a
 * plain {@code Function<ApprovalRequest, Optional<ApprovalOutcome>>}
 * instead of a named class. A check that declines to decide returns
 * {@link Optional#empty()}, the same signal the object chain uses.
 */
public final class ApprovalChecks {

    private ApprovalChecks() {}

    public static Function<ApprovalRequest, Optional<ApprovalOutcome>> weightLimit(double limitKg) {
        return request -> request.weightKg() > limitKg
                ? Optional.of(new ApprovalOutcome.Rejected("weightLimit",
                        "shipment weight %.1f kg exceeds the %.1f kg limit".formatted(request.weightKg(), limitKg)))
                : Optional.empty();
    }

    public static Function<ApprovalRequest, Optional<ApprovalOutcome>> hazmatClearance(Set<String> clearedCountries) {
        Set<String> cleared = Set.copyOf(clearedCountries);
        return request -> {
            if (!request.hazmat() || cleared.contains(request.destinationCountry())) {
                return Optional.empty();
            }
            return Optional.of(new ApprovalOutcome.Rejected("hazmatClearance",
                    "hazmat shipments are not cleared for " + request.destinationCountry()));
        };
    }

    public static Function<ApprovalRequest, Optional<ApprovalOutcome>> embargo(Set<String> embargoedCountries) {
        Set<String> embargoed = Set.copyOf(embargoedCountries);
        return request -> embargoed.contains(request.destinationCountry())
                ? Optional.of(new ApprovalOutcome.Rejected("embargo",
                        "destination " + request.destinationCountry() + " is under embargo"))
                : Optional.empty();
    }
}
