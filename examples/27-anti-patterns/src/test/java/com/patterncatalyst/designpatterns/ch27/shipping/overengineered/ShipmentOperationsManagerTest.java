package com.patterncatalyst.designpatterns.ch27.shipping.overengineered;

import com.patterncatalyst.designpatterns.ch27.shipping.Money;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentOperationsManagerTest {

    @AfterEach
    void resetRegistry() {
        ShippingSurchargeRegistry.INSTANCE.setSurchargeMultiplier(1.0);
    }

    @Test
    void pricingAShipmentWritesAnAuditLogEntryUnrelatedToPricing() {
        ShipmentOperationsManager manager = new ShipmentOperationsManager();
        assertThat(manager.operationLog()).isEmpty();

        manager.priceShipment("ORD-3001", Money.ofCents(5_000));

        assertThat(manager.operationLog()).hasSize(1);
    }

    @Test
    void wasLastCalculationSurchargeAppliedAnswersForWhicheverShipmentWasPricedMostRecently() {
        ShipmentOperationsManager manager = new ShipmentOperationsManager();

        manager.priceShipment("ORD-3001", Money.ofCents(5_000));
        assertThat(manager.wasLastCalculationSurchargeApplied()).isFalse();

        ShippingSurchargeRegistry.INSTANCE.setSurchargeMultiplier(1.15);
        manager.priceShipment("ORD-3002", Money.ofCents(5_000));

        // Correct for ORD-3002, the shipment just priced, but a caller still
        // holding ORD-3001's receipt and asking this method is told
        // ORD-3002's answer: the flag is ordered state tied to the last
        // call, not a fact attached to ORD-3001's own receipt.
        assertThat(manager.wasLastCalculationSurchargeApplied()).isTrue();
    }

    @Test
    void priceShipmentRejectsANonPositiveBaseCost() {
        ShipmentOperationsManager manager = new ShipmentOperationsManager();

        assertThatThrownBy(() -> manager.priceShipment("ORD-9000", Money.ofCents(0)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
