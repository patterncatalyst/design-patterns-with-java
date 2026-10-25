package com.patterncatalyst.designpatterns.ch04.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NotificationOutcomeTest {

    @Test
    void notifiedCarriesTheDispatchResult() {
        DispatchResult result = new DispatchResult(DispatchStatus.SENT, "email-ORD-6001");

        NotificationOutcome outcome = new NotificationOutcome.Notified(result);

        String described = describe(outcome);

        assertThat(described).isEqualTo("Notified: " + result);
    }

    @Test
    void rejectedCarriesTheReason() {
        NotificationOutcome outcome = new NotificationOutcome.Rejected("order event has no customerContact to notify");

        String described = describe(outcome);

        assertThat(described).isEqualTo("Rejected: order event has no customerContact to notify");
    }

    /** Mirrors the exhaustive pattern-matching switch used at the call site in {@code Main}. */
    private static String describe(NotificationOutcome outcome) {
        return switch (outcome) {
            case NotificationOutcome.Notified notified -> "Notified: " + notified.result();
            case NotificationOutcome.Rejected rejected -> "Rejected: " + rejected.reason();
        };
    }
}
