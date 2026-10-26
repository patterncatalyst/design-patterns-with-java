package com.patterncatalyst.designpatterns.ch27.shipping.overengineered;

import com.patterncatalyst.designpatterns.ch27.shipping.Money;

/**
 * OVER-ENGINEERED EXHIBIT. The value {@link ShipmentOperationsManager}
 * formats and returns for every shipment it prices.
 *
 * @param orderId the shipment's order identifier
 * @param totalCost the shipment's priced total
 * @param formattedReceipt a display-ready receipt line for this shipment
 */
public record PricedShipmentReceipt(String orderId, Money totalCost, String formattedReceipt) {
}
