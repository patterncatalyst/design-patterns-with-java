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
 * EmailGateway}, and it checks that the Classic Java {@code switch}
 * statement and the Java 25 {@code switch} expression on {@link
 * DeliveryAuditor} agree on every {@link DeliveryState}, the way {@code
 * StrategyEquivalenceTest} checks three Strategy expressions against each
 * other rather than against three separately hand-written expectations.
 */
@ExtendWith(MockitoExtension.class)
class DeliveryAuditorTest {

    @Mock
    private DeliveryStatusProvider statusProvider;

    @ParameterizedTest
    @EnumSource(DeliveryState.class)
    void auditDeliveryMatchesBothSwitchExpressionsForEveryState(DeliveryState state) {
        when(statusProvider.statusOf("dispatch-1")).thenReturn(state);
        DeliveryAuditor auditor = new DeliveryAuditor(statusProvider);

        String described = auditor.auditDelivery("dispatch-1");

        assertThat(described).isEqualTo(DeliveryAuditor.describeStateClassic(state));
        assertThat(described).isEqualTo(DeliveryAuditor.describeState(state));
        verify(statusProvider).statusOf("dispatch-1");
    }
}
