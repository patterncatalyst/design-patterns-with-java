package com.patterncatalyst.designpatterns.ch19.shipping.approval;

import java.util.Optional;

/** Rejects a shipment heavier than the carrier's configured weight limit. */
public final class WeightLimitHandler extends ApprovalHandler {

    private final double limitKg;

    public WeightLimitHandler(double limitKg, ApprovalHandler next) {
        super("WeightLimitHandler", next);
        this.limitKg = limitKg;
    }

    @Override
    protected Optional<ApprovalOutcome> attempt(ApprovalRequest request) {
        if (request.weightKg() > limitKg) {
            return Optional.of(new ApprovalOutcome.Rejected(name(),
                    "shipment weight %.1f kg exceeds the %.1f kg limit".formatted(request.weightKg(), limitKg)));
        }
        return Optional.empty();
    }
}
