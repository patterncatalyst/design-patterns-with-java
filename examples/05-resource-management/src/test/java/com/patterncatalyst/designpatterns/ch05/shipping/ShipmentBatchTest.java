package com.patterncatalyst.designpatterns.ch05.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentBatchTest {

    @Test
    void storesBatchFields() {
        ShipmentBatch batch = new ShipmentBatch("BATCH-20", 10, 33.5);

        assertThat(batch.batchId()).isEqualTo("BATCH-20");
        assertThat(batch.shipmentCount()).isEqualTo(10);
        assertThat(batch.totalWeightKg()).isEqualTo(33.5);
    }

    @Test
    void rejectsNonPositiveShipmentCount() {
        assertThatThrownBy(() -> new ShipmentBatch("BATCH-21", 0, 33.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("shipmentCount");
    }

    @Test
    void rejectsBlankBatchId() {
        assertThatThrownBy(() -> new ShipmentBatch(" ", 1, 1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("batchId");
    }
}
