package com.patterncatalyst.designpatterns.ch11.warehouse;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AllocateStockCommandTest {

    @Test
    void executeAddsTheQuantityToTheBin() {
        WarehouseLedger ledger = new WarehouseLedger();
        AllocateStockCommand command = new AllocateStockCommand(ledger, "A1", "WIDGET-100", 50);

        command.execute();

        assertThat(ledger.quantityOf("A1", "WIDGET-100")).isEqualTo(50);
    }

    @Test
    void undoRestoresThePriorQuantity() {
        WarehouseLedger ledger = new WarehouseLedger();
        ledger.adjustStock("A1", "WIDGET-100", 10);
        AllocateStockCommand command = new AllocateStockCommand(ledger, "A1", "WIDGET-100", 50);

        command.execute();
        command.undo();

        assertThat(ledger.quantityOf("A1", "WIDGET-100")).isEqualTo(10);
    }

    @Test
    void undoOnlyReversesItsOwnShareWhenStackedWithOtherAllocations() {
        WarehouseLedger ledger = new WarehouseLedger();
        AllocateStockCommand first = new AllocateStockCommand(ledger, "A1", "WIDGET-100", 50);
        AllocateStockCommand second = new AllocateStockCommand(ledger, "A1", "WIDGET-100", 20);

        first.execute();
        second.execute();
        second.undo();

        assertThat(ledger.quantityOf("A1", "WIDGET-100")).isEqualTo(50);
    }
}
