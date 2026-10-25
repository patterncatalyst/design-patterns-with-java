package com.patterncatalyst.designpatterns.ch04.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link TemplatedOrderNotifier} declares two role dependencies, {@link
 * MessageSender} and {@link TemplateRenderer}. Mocking each one
 * independently, rather than one fake implementing a fat interface, is the
 * concrete payoff Interface Segregation buys a test author.
 */
@ExtendWith(MockitoExtension.class)
class TemplatedOrderNotifierTest {

    @Mock
    private MessageSender sender;

    @Mock
    private TemplateRenderer renderer;

    @Test
    void rendersTheTemplateBeforeSendingTheResult() {
        OrderEvent event = new OrderEvent("ORD-3001", "sam@example.com", "original body");
        when(renderer.render("order-delayed", event)).thenReturn("[Order ORD-3001] running late");
        DispatchResult expected = new DispatchResult(DispatchStatus.SENT, "email-ORD-3001");
        when(sender.send(any(OrderEvent.class))).thenReturn(expected);
        TemplatedOrderNotifier notifier = new TemplatedOrderNotifier(sender, renderer, "order-delayed");

        DispatchResult actual = notifier.notifyCustomer(event);

        assertThat(actual).isEqualTo(expected);
        ArgumentCaptor<OrderEvent> sentEvent = ArgumentCaptor.forClass(OrderEvent.class);
        verify(sender).send(sentEvent.capture());
        assertThat(sentEvent.getValue().messageBody()).isEqualTo("[Order ORD-3001] running late");
        assertThat(sentEvent.getValue().orderId()).isEqualTo(event.orderId());
    }

    @Test
    void requestsTheConfiguredTemplateIdFromTheRenderer() {
        OrderEvent event = new OrderEvent("ORD-3002", "sam@example.com", "original body");
        when(renderer.render("order-shipped", event)).thenReturn("[Order ORD-3002] shipped");
        when(sender.send(any(OrderEvent.class)))
                .thenReturn(new DispatchResult(DispatchStatus.SENT, "email-ORD-3002"));
        TemplatedOrderNotifier notifier = new TemplatedOrderNotifier(sender, renderer, "order-shipped");

        notifier.notifyCustomer(event);

        verify(renderer).render("order-shipped", event);
    }
}
