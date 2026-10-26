package com.patterncatalyst.designpatterns.ch30.shipping;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Extract and Override Factory Method: a test subclass overrides
 * {@code createSurchargeCalculator()} to return a Mockito double instead of
 * the production {@link StandardSurchargeCalculator}, with no change to
 * {@code dispatch}'s own logic or to the constructor.
 */
@ExtendWith(MockitoExtension.class)
class LegacyShipmentProcessorSurchargeTest {

    @Mock
    private NotificationGateway notificationGateway;

    @Mock
    private SurchargeCalculator surchargeCalculator;

    private static final class OverriddenCalculatorProcessor extends LegacyShipmentProcessor {
        private final SurchargeCalculator calculator;

        OverriddenCalculatorProcessor(NotificationGateway notificationGateway, SurchargeCalculator calculator) {
            super("ops-supervisor@example.com", notificationGateway);
            this.calculator = calculator;
        }

        @Override
        protected SurchargeCalculator createSurchargeCalculator() {
            return calculator;
        }
    }

    @Test
    void dispatchUsesTheOverriddenCalculatorInsteadOfTheStandardOne() {
        when(surchargeCalculator.surchargeCents(10_000)).thenReturn(500L);
        LegacyShipmentProcessor processor =
                new OverriddenCalculatorProcessor(notificationGateway, surchargeCalculator);

        ShipmentDispatchResult result = processor.dispatch(new Shipment("SH-6001", 10_000));

        assertThat(result.totalCostCents()).isEqualTo(10_500);
        verify(surchargeCalculator).surchargeCents(10_000);
    }

    @Test
    void theStandardCalculatorRemainsTheDefaultWhenNoSubclassOverridesIt() {
        LegacyShipmentProcessor processor =
                new LegacyShipmentProcessor("ops-supervisor@example.com", notificationGateway);

        ShipmentDispatchResult result = processor.dispatch(new Shipment("SH-6002", 10_000));

        assertThat(result.totalCostCents()).isEqualTo(10_800);
    }
}
