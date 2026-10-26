package com.patterncatalyst.designpatterns.ch19.shipping.approval;

import java.util.Optional;
import java.util.Set;

/**
 * Rejects a hazardous shipment headed to a country not on its cleared list.
 * A non-hazardous shipment always passes, regardless of destination.
 */
public final class HazmatClearanceHandler extends ApprovalHandler {

    private final Set<String> clearedCountries;

    public HazmatClearanceHandler(Set<String> clearedCountries, ApprovalHandler next) {
        super("HazmatClearanceHandler", next);
        this.clearedCountries = Set.copyOf(clearedCountries);
    }

    @Override
    protected Optional<ApprovalOutcome> attempt(ApprovalRequest request) {
        if (!request.hazmat()) {
            return Optional.empty();
        }
        if (!clearedCountries.contains(request.destinationCountry())) {
            return Optional.of(new ApprovalOutcome.Rejected(name(),
                    "hazmat shipments are not cleared for " + request.destinationCountry()));
        }
        return Optional.empty();
    }
}
