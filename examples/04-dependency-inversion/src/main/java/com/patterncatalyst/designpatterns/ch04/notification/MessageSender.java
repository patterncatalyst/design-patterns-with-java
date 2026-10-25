package com.patterncatalyst.designpatterns.ch04.notification;

/**
 * The one capability every notification channel has in common: accept an
 * {@link OrderEvent} and attempt to deliver it. This is the only
 * method in this interface, so a channel that can send but cannot render a
 * template or report delivery status, such as {@link SmsGateway}, implements
 * exactly this and nothing else.
 */
public interface MessageSender {
    DispatchResult send(OrderEvent event);
}
