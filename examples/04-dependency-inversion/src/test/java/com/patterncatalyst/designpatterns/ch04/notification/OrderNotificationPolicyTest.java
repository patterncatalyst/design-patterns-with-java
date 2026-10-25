package com.patterncatalyst.designpatterns.ch04.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link OrderNotificationPolicy} depends only on {@link MessageSender},
 * which is the whole point of Dependency Inversion: this suite substitutes
 * a Mockito test double for the abstraction and never links against {@link
 * EmailGateway} or {@link SmsGateway} to exercise the policy's decisions.
 */
@ExtendWith(MockitoExtension.class)
class OrderNotificationPolicyTest {

    @Mock
    private MessageSender sender;

    @Test
    void notifyCustomerDelegatesToTheInjectedSender() {
        OrderEvent event = new OrderEvent("ORD-2001", "sam@example.com", "Your order has shipped.");
        DispatchResult expected = new DispatchResult(DispatchStatus.SENT, "email-ORD-2001");
        when(sender.send(event)).thenReturn(expected);
        OrderNotificationPolicy policy = new OrderNotificationPolicy(sender);

        DispatchResult actual = policy.notifyCustomer(event);

        assertThat(actual).isEqualTo(expected);
        verify(sender).send(event);
    }

    @Test
    void tryNotifyCustomerRejectsABlankContactWithoutCallingTheSender() {
        OrderEvent event = new OrderEvent("ORD-2002", "", "Your order is running late.");
        OrderNotificationPolicy policy = new OrderNotificationPolicy(sender);

        NotificationOutcome outcome = policy.tryNotifyCustomer(event);

        assertThat(outcome).isInstanceOf(NotificationOutcome.Rejected.class);
        assertThat(((NotificationOutcome.Rejected) outcome).reason()).contains("customerContact");
        verify(sender, never()).send(any());
    }

    @Test
    void tryNotifyCustomerNotifiesAndReturnsTheSenderResultWhenContactIsPresent() {
        OrderEvent event = new OrderEvent("ORD-2003", "sam@example.com", "Your order is running late.");
        DispatchResult expected = new DispatchResult(DispatchStatus.SENT, "email-ORD-2003");
        when(sender.send(event)).thenReturn(expected);
        OrderNotificationPolicy policy = new OrderNotificationPolicy(sender);

        NotificationOutcome outcome = policy.tryNotifyCustomer(event);

        assertThat(outcome).isEqualTo(new NotificationOutcome.Notified(expected));
    }
}
