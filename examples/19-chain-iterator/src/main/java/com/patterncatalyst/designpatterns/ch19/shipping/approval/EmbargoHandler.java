package com.patterncatalyst.designpatterns.ch19.shipping.approval;

import java.util.Optional;
import java.util.Set;

/** Rejects a shipment addressed to a country under an active trade embargo. */
public final class EmbargoHandler extends ApprovalHandler {

    private final Set<String> embargoedCountries;

    public EmbargoHandler(Set<String> embargoedCountries, ApprovalHandler next) {
        super("EmbargoHandler", next);
        this.embargoedCountries = Set.copyOf(embargoedCountries);
    }

    @Override
    protected Optional<ApprovalOutcome> attempt(ApprovalRequest request) {
        if (embargoedCountries.contains(request.destinationCountry())) {
            return Optional.of(new ApprovalOutcome.Rejected(name(),
                    "destination " + request.destinationCountry() + " is under embargo"));
        }
        return Optional.empty();
    }
}
