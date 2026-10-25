package com.patterncatalyst.designpatterns.ch11.warehouse;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CancelOrderLineCommandTest {

    @Test
    void executeReturnsTheQuantityToTheBinAndMarksTheLineCancelled() {
        WarehouseLedger ledger = new WarehouseLedger();
        ledger.adjustStock("PICK-FACE-3", "WIDGET-100", 12);
        ledger.addOrderLine("LINE-9001", new OrderLine("WIDGET-100", "PICK-FACE-3", 5, false));
        CancelOrderLineCommand command = new CancelOrderLineCommand(ledger, "LINE-9001");

        command.execute();

        assertThat(ledger.quantityOf("PICK-FACE-3", "WIDGET-100")).isEqualTo(17);
        assertThat(ledger.orderLine("LINE-9001").cancelled()).isTrue();
        assertThat(ledger.orderLine("LINE-9001").quantity()).isZero();
    }

    @Test
    void undoReversesTheStockReturnAndRestoresTheLine() {
        WarehouseLedger ledger = new WarehouseLedger();
        ledger.adjustStock("PICK-FACE-3", "WIDGET-100", 12);
        ledger.addOrderLine("LINE-9001", new OrderLine("WIDGET-100", "PICK-FACE-3", 5, false));
        CancelOrderLineCommand command = new CancelOrderLineCommand(ledger, "LINE-9001");

        command.execute();
        command.undo();

        assertThat(ledger.quantityOf("PICK-FACE-3", "WIDGET-100")).isEqualTo(12);
        OrderLine restored = ledger.orderLine("LINE-9001");
        assertThat(restored.cancelled()).isFalse();
        assertThat(restored.quantity()).isEqualTo(5);
    }

    @Test
    void undoingACancellationOfAnAlreadyCancelledLineRestoresItStillCancelled() {
        WarehouseLedger ledger = new WarehouseLedger();
        ledger.adjustStock("PICK-FACE-3", "WIDGET-100", 12);
        // The line arrives already cancelled from an earlier operation.
        ledger.addOrderLine("LINE-9001", new OrderLine("WIDGET-100", "PICK-FACE-3", 5, true));
        CancelOrderLineCommand command = new CancelOrderLineCommand(ledger, "LINE-9001");

        command.execute();
        command.undo();

        // Undo restores the captured snapshot exactly, cancelled flag
        // included, rather than assuming undo always means "not cancelled".
        assertThat(ledger.orderLine("LINE-9001").cancelled()).isTrue();
        assertThat(ledger.orderLine("LINE-9001").quantity()).isEqualTo(5);
    }
}
