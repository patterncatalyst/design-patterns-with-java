package com.patterncatalyst.designpatterns.ch04.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link DeliveryAuditor} depends only on {@link DeliveryStatusProvider}.
 * This suite mocks that single role and never constructs an {@link
 * EmailGateway}, and it checks the {@code switch} expression's description
 * for every {@link DeliveryState}.
 */
@ExtendWith(MockitoExtension.class)
class DeliveryAuditorTest {

    @Mock
    private DeliveryStatusProvider statusProvider;

    @ParameterizedTest
    @EnumSource(DeliveryState.class)
    void auditDeliveryDescribesEveryDeliveryState(DeliveryState state) {
        when(statusProvider.statusOf("dispatch-1")).thenReturn(state);
        DeliveryAuditor auditor = new DeliveryAuditor(statusProvider);

        String described = auditor.auditDelivery("dispatch-1");

        String expected = switch (state) {
            case PENDING -> "still in flight";
            case DELIVERED -> "confirmed delivered";
            case FAILED -> "confirmed failed";
            case UNKNOWN -> "no record of this dispatch";
        };
        assertThat(described).isEqualTo(expected);
        verify(statusProvider).statusOf("dispatch-1");
    }
}
