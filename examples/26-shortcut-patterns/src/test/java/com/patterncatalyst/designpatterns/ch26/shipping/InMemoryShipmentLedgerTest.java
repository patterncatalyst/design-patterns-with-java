package com.patterncatalyst.designpatterns.ch26.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class InMemoryShipmentLedgerTest {

    @Test
    void recordsEntriesInOrderAsTrackingIdColonNote() {
        InMemoryShipmentLedger ledger = new InMemoryShipmentLedger();

        ledger.record("SH-1", "created: placed");
        ledger.record("SH-1", "picked-up: scanned");

        assertThat(ledger.entries()).containsExactly(
                "SH-1: created: placed",
                "SH-1: picked-up: scanned");
    }

    @Test
    void entriesViewIsUnmodifiable() {
        InMemoryShipmentLedger ledger = new InMemoryShipmentLedger();
        ledger.record("SH-1", "created: placed");

        assertThatThrownBy(() -> ledger.entries().add("SH-2: tampered"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
