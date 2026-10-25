package com.patterncatalyst.designpatterns.ch04.notification;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An email channel. Email has all three capabilities this domain
 * recognizes: it can send, it can render a template before sending, and the
 * provider's webhook eventually reports whether a message was delivered.
 * Implementing all three role interfaces costs nothing extra, because
 * {@code EmailGateway} has all three capabilities; Interface
 * Segregation never forbids a type from satisfying more than one role.
 */
public final class EmailGateway implements MessageSender, TemplateRenderer, DeliveryStatusProvider {

    private final Map<String, DeliveryState> deliveryLog = new ConcurrentHashMap<>();

    @Override
    public DispatchResult send(OrderEvent event) {
        String dispatchId = "email-" + event.orderId();
        if (event.customerContact().isBlank()) {
            deliveryLog.put(dispatchId, DeliveryState.FAILED);
            return new DispatchResult(DispatchStatus.FAILED, dispatchId);
        }
        deliveryLog.put(dispatchId, DeliveryState.PENDING);
        return new DispatchResult(DispatchStatus.SENT, dispatchId);
    }

    @Override
    public String render(String templateId, OrderEvent event) {
        return switch (templateId) {
            case "order-shipped" -> "[Order %s] Your order has shipped.".formatted(event.orderId());
            case "order-delayed" ->
                    "[Order %s] We're sorry, your order is running late: %s"
                            .formatted(event.orderId(), event.messageBody());
            default -> "[Order %s] %s".formatted(event.orderId(), event.messageBody());
        };
    }

    @Override
    public DeliveryState statusOf(String dispatchId) {
        return deliveryLog.getOrDefault(dispatchId, DeliveryState.UNKNOWN);
    }

    /**
     * Simulates the provider's delivery webhook confirming a dispatch
     * landed. A real {@code EmailGateway} would update {@code deliveryLog}
     * from an inbound callback instead of a direct call, but the role this
     * plays for {@link DeliveryAuditor} is identical either way.
     */
    public void markDelivered(String dispatchId) {
        deliveryLog.put(dispatchId, DeliveryState.DELIVERED);
    }
}
