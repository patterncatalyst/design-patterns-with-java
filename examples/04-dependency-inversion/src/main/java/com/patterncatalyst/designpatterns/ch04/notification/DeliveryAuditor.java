package com.patterncatalyst.designpatterns.ch04.notification;

import java.util.Objects;

/**
 * A third high-level client that only ever checks whether a dispatch
 * landed. Against a fat {@code NotificationGateway} it would have had to
 * depend on {@code send} and {@code render} too, and its own test would
 * have needed a fake implementing methods this class never calls. Against
 * {@link DeliveryStatusProvider} alone, the constructor signature is an
 * accurate description of what {@code DeliveryAuditor} actually needs.
 */
public final class DeliveryAuditor {

    private final DeliveryStatusProvider statusProvider;

    public DeliveryAuditor(DeliveryStatusProvider statusProvider) {
        this.statusProvider = Objects.requireNonNull(statusProvider, "statusProvider");
    }

    public String auditDelivery(String dispatchId) {
        DeliveryState state = statusProvider.statusOf(dispatchId);
        return describeState(state);
    }

    /**
     * Classic Java: a {@code switch} statement with an explicit {@code
     * break} after every branch and a {@code default} that has to be
     * remembered, not enforced. Adding a fifth {@link DeliveryState}
     * constant compiles cleanly even if nobody updates this method; the gap
     * only shows up when a dispatch in that state reaches the default
     * branch at runtime.
     */
    static String describeStateClassic(DeliveryState state) {
        String description;
        switch (state) {
            case PENDING:
                description = "still in flight";
                break;
            case DELIVERED:
                description = "confirmed delivered";
                break;
            case FAILED:
                description = "confirmed failed";
                break;
            default:
                description = "no record of this dispatch";
                break;
        }
        return description;
    }

    /**
     * Java 25: a {@code switch} expression with no {@code default}. Adding
     * a fifth {@link DeliveryState} constant without a matching case here
     * is a compile error, not a runtime surprise on whichever dispatch
     * reaches the missing case first.
     */
    static String describeState(DeliveryState state) {
        return switch (state) {
            case PENDING -> "still in flight";
            case DELIVERED -> "confirmed delivered";
            case FAILED -> "confirmed failed";
            case UNKNOWN -> "no record of this dispatch";
        };
    }
}
