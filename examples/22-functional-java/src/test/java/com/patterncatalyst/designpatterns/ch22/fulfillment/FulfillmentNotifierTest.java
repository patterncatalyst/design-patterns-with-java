package com.patterncatalyst.designpatterns.ch22.fulfillment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Proves method references satisfy {@link NotificationCommand} exactly like
 * a lambda or a named class would: {@link FulfillmentNotifier} runs each
 * queued command without knowing that {@code log::recordEmail} is a bound
 * method reference rather than some other kind of value.
 */
class FulfillmentNotifierTest {

    private static final Shipment SHIPMENT =
            new Shipment("RETAIL-1", 1.0, DestinationZone.LOCAL, PackagingTier.STANDARD, 1_000);

    @Test
    void dispatchAllRunsEveryQueuedCommandInOrder() {
        NotificationLog log = new NotificationLog();
        FulfillmentNotifier notifier = new FulfillmentNotifier(List.of(log::recordEmail, log::recordSms));

        notifier.dispatchAll(SHIPMENT);

        assertThat(log.entries()).containsExactly("email:RETAIL-1", "sms:RETAIL-1");
    }

    @Test
    void anEmptyCommandQueueDispatchesNothing() {
        NotificationLog log = new NotificationLog();
        FulfillmentNotifier notifier = new FulfillmentNotifier(List.of());

        notifier.dispatchAll(SHIPMENT);

        assertThat(log.entries()).isEmpty();
        assertThat(notifier.commandCount()).isZero();
    }

    @Test
    void aLambdaSatisfiesTheSameCommandContractAsAMethodReference() {
        NotificationLog log = new NotificationLog();
        NotificationCommand adHocCommand = shipment -> log.recordEmail(shipment);
        FulfillmentNotifier notifier = new FulfillmentNotifier(List.of(adHocCommand, log::recordSms));

        notifier.dispatchAll(SHIPMENT);

        assertThat(log.entries()).containsExactly("email:RETAIL-1", "sms:RETAIL-1");
    }
}
