package com.patterncatalyst.designpatterns.ch11.warehouse;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * The receiver every command in this example mutates. It tracks on-hand
 * quantity per bin/SKU pair and the order lines already picked against that
 * stock, and exposes exactly the primitives a command needs: an adjustment
 * by a signed delta, a quantity lookup, and order-line read/write. Nothing
 * about a command, an undo, or a queue appears here; this class only knows
 * how to change warehouse state.
 */
public final class WarehouseLedger {

    private record BinSku(String bin, String sku) {
    }

    private final Map<BinSku, Integer> onHand = new HashMap<>();
    private final Map<String, OrderLine> orderLines = new HashMap<>();

    /**
     * Adjusts the on-hand quantity for a bin/SKU pair by a signed delta. A
     * single adjustment primitive, rather than separate add/remove methods,
     * is what lets every command below express its undo as the identical
     * call with the sign flipped.
     */
    public void adjustStock(String bin, String sku, int delta) {
        onHand.merge(new BinSku(bin, sku), delta, Integer::sum);
    }

    public int quantityOf(String bin, String sku) {
        return onHand.getOrDefault(new BinSku(bin, sku), 0);
    }

    public void addOrderLine(String lineId, OrderLine line) {
        orderLines.put(lineId, Objects.requireNonNull(line, "line"));
    }

    public OrderLine orderLine(String lineId) {
        OrderLine line = orderLines.get(lineId);
        if (line == null) {
            throw new NoSuchElementException("no order line: " + lineId);
        }
        return line;
    }

    public void setLineState(String lineId, int quantity, boolean cancelled) {
        OrderLine current = orderLine(lineId);
        orderLines.put(lineId, new OrderLine(current.sku(), current.bin(), quantity, cancelled));
    }
}
