package com.patterncatalyst.designpatterns.ch04.notification;

import java.util.Objects;

/**
 * A second high-level policy, independent of {@link
 * OrderNotificationPolicy}, that renders a template before sending. It
 * declares two dependencies, {@link MessageSender} and {@link
 * TemplateRenderer}, because it needs both capabilities. A
 * composition root is free to satisfy both references with the same
 * concrete object, such as {@link EmailGateway}, or with two different
 * ones; {@code TemplatedOrderNotifier} cannot tell the difference and does
 * not need to.
 */
public final class TemplatedOrderNotifier {

    private final MessageSender sender;
    private final TemplateRenderer renderer;
    private final String templateId;

    public TemplatedOrderNotifier(MessageSender sender, TemplateRenderer renderer, String templateId) {
        this.sender = Objects.requireNonNull(sender, "sender");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.templateId = Objects.requireNonNull(templateId, "templateId");
    }

    public DispatchResult notifyCustomer(OrderEvent event) {
        String renderedBody = renderer.render(templateId, event);
        return sender.send(event.withMessageBody(renderedBody));
    }
}
