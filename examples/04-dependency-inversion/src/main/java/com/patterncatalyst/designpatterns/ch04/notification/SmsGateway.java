package com.patterncatalyst.designpatterns.ch04.notification;

/**
 * An SMS channel. A carrier API sends a short plain-text message and
 * reports nothing else back: no template rendering, no delivery-status
 * lookup. {@code SmsGateway} implements exactly {@link MessageSender} and
 * nothing more, so there is no {@code render} or {@code statusOf} method to
 * stub, fake, or silently no-op.
 */
public final class SmsGateway implements MessageSender {

    @Override
    public DispatchResult send(OrderEvent event) {
        String dispatchId = "sms-" + event.orderId();
        return new DispatchResult(DispatchStatus.SENT, dispatchId);
    }
}
