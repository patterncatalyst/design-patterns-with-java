package com.patterncatalyst.designpatterns.ch11.warehouse;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TransferStockCommandTest {

    @Test
    void executeMovesTheQuantityFromSourceToDestination() {
        WarehouseLedger ledger = new WarehouseLedger();
        ledger.adjustStock("A1", "WIDGET-100", 50);
        TransferStockCommand command = new TransferStockCommand(ledger, "A1", "PICK-FACE-3", "WIDGET-100", 20);

        command.execute();

        assertThat(ledger.quantityOf("A1", "WIDGET-100")).isEqualTo(30);
        assertThat(ledger.quantityOf("PICK-FACE-3", "WIDGET-100")).isEqualTo(20);
    }

    @Test
    void undoMovesTheQuantityBackToTheSource() {
        WarehouseLedger ledger = new WarehouseLedger();
        ledger.adjustStock("A1", "WIDGET-100", 50);
        TransferStockCommand command = new TransferStockCommand(ledger, "A1", "PICK-FACE-3", "WIDGET-100", 20);

        command.execute();
        command.undo();

        assertThat(ledger.quantityOf("A1", "WIDGET-100")).isEqualTo(50);
        assertThat(ledger.quantityOf("PICK-FACE-3", "WIDGET-100")).isZero();
    }

    @Test
    void twoRecordsBuiltFromTheSameValuesAreEqual() {
        WarehouseLedger ledger = new WarehouseLedger();
        TransferStockCommand first = new TransferStockCommand(ledger, "A1", "PICK-FACE-3", "WIDGET-100", 20);
        TransferStockCommand second = new TransferStockCommand(ledger, "A1", "PICK-FACE-3", "WIDGET-100", 20);

        assertThat(first).isEqualTo(second);
    }
}
