package com.patterncatalyst.designpatterns.ch05.shipping;

/**
 * A batch of shipments ready to record in the carrier ledger: a batch id,
 * how many shipments it contains, and their combined weight.
 */
public record ShipmentBatch(String batchId, int shipmentCount, double totalWeightKg) {

    public ShipmentBatch {
        if (batchId == null || batchId.isBlank()) {
            throw new IllegalArgumentException("batchId must not be blank");
        }
        if (shipmentCount <= 0) {
            throw new IllegalArgumentException("shipmentCount must be positive: " + shipmentCount);
        }
        if (totalWeightKg <= 0.0) {
            throw new IllegalArgumentException("totalWeightKg must be positive: " + totalWeightKg);
        }
    }
}
