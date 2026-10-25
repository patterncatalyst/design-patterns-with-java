package com.patterncatalyst.designpatterns.ch11.warehouse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class WarehouseLedgerTest {

    @Test
    void quantityOfDefaultsToZeroForAnUntouchedBinAndSku() {
        WarehouseLedger ledger = new WarehouseLedger();

        assertThat(ledger.quantityOf("A1", "WIDGET-100")).isZero();
    }

    @Test
    void adjustStockAccumulatesAcrossMultipleCalls() {
        WarehouseLedger ledger = new WarehouseLedger();

        ledger.adjustStock("A1", "WIDGET-100", 50);
        ledger.adjustStock("A1", "WIDGET-100", -20);
        ledger.adjustStock("A1", "WIDGET-100", 5);

        assertThat(ledger.quantityOf("A1", "WIDGET-100")).isEqualTo(35);
    }

    @Test
    void adjustStockKeepsDifferentBinSkuPairsIndependent() {
        WarehouseLedger ledger = new WarehouseLedger();

        ledger.adjustStock("A1", "WIDGET-100", 50);
        ledger.adjustStock("B2", "WIDGET-100", 10);
        ledger.adjustStock("A1", "GASKET-7", 3);

        assertThat(ledger.quantityOf("A1", "WIDGET-100")).isEqualTo(50);
        assertThat(ledger.quantityOf("B2", "WIDGET-100")).isEqualTo(10);
        assertThat(ledger.quantityOf("A1", "GASKET-7")).isEqualTo(3);
    }

    @Test
    void addOrderLineAndOrderLineRoundTrip() {
        WarehouseLedger ledger = new WarehouseLedger();
        OrderLine line = new OrderLine("WIDGET-100", "PICK-FACE-3", 5, false);

        ledger.addOrderLine("LINE-9001", line);

        assertThat(ledger.orderLine("LINE-9001")).isEqualTo(line);
    }

    @Test
    void orderLineThrowsForAnUnknownLineId() {
        WarehouseLedger ledger = new WarehouseLedger();

        assertThatThrownBy(() -> ledger.orderLine("LINE-404")).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void setLineStateUpdatesQuantityAndCancelledWithoutDisturbingSkuOrBin() {
        WarehouseLedger ledger = new WarehouseLedger();
        ledger.addOrderLine("LINE-9001", new OrderLine("WIDGET-100", "PICK-FACE-3", 5, false));

        ledger.setLineState("LINE-9001", 0, true);

        OrderLine updated = ledger.orderLine("LINE-9001");
        assertThat(updated.sku()).isEqualTo("WIDGET-100");
        assertThat(updated.bin()).isEqualTo("PICK-FACE-3");
        assertThat(updated.quantity()).isZero();
        assertThat(updated.cancelled()).isTrue();
    }
}
