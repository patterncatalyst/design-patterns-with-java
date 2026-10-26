package com.patterncatalyst.designpatterns.ch33.shipping;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The one type this final project asks to play two roles at once: the State
 * machine's context, and the Observer's subject. Both roles share the same
 * success condition, a status change {@link #advance(ShipmentEvent)}
 * accepts, with nothing in between them to get out of sync. A
 * {@link Shipment} also holds the {@link RateStrategy} it was opened with,
 * which is the seam {@link AdjustWeightCommand} and
 * {@link SwitchRateStrategyCommand} both use.
 *
 * <p>Only {@link ShipmentBuilder} constructs a {@code Shipment}; only the
 * command types in this package call {@link #restoreStatusForUndo},
 * {@link #setWeightKg}, {@link #setQuotedRate}, and
 * {@link #setRateStrategy}. Every other caller reaches this class through
 * its public, read-only accessors and {@link #advance(ShipmentEvent)}.
 */
public final class Shipment {

    private static final Map<ShipmentStatus, Map<ShipmentEvent, ShipmentStatus>> TRANSITIONS = buildTransitions();

    private final String id;
    private final String carrierCode;
    private final DestinationZone destinationZone;
    private final Map<Long, ShipmentStatusListener> listeners = new LinkedHashMap<>();
    private final AtomicLong nextSubscriptionId = new AtomicLong();

    private RateStrategy rateStrategy;
    private double weightKg;
    private Money quotedRate;
    private ShipmentStatus status = ShipmentStatus.CREATED;

    Shipment(String id, String carrierCode, DestinationZone destinationZone, double weightKg,
            RateStrategy rateStrategy) {
        this.id = Objects.requireNonNull(id, "id");
        this.carrierCode = Objects.requireNonNull(carrierCode, "carrierCode");
        this.destinationZone = Objects.requireNonNull(destinationZone, "destinationZone");
        this.rateStrategy = Objects.requireNonNull(rateStrategy, "rateStrategy");
        if (weightKg < 0.0) {
            throw new IllegalArgumentException("weightKg cannot be negative: " + weightKg);
        }
        this.weightKg = weightKg;
        this.quotedRate = rateStrategy.quote(this);
    }

    public String id() {
        return id;
    }

    public String carrierCode() {
        return carrierCode;
    }

    public DestinationZone destinationZone() {
        return destinationZone;
    }

    public double weightKg() {
        return weightKg;
    }

    public Money quotedRate() {
        return quotedRate;
    }

    public RateStrategy rateStrategy() {
        return rateStrategy;
    }

    public ShipmentStatus status() {
        return status;
    }

    /** Recomputes a quote against the current weight through the current strategy. */
    public Money requote() {
        return rateStrategy.quote(this);
    }

    public Subscription subscribe(ShipmentStatusListener listener) {
        Objects.requireNonNull(listener, "listener");
        long subscriptionId = nextSubscriptionId.incrementAndGet();
        listeners.put(subscriptionId, listener);
        return () -> listeners.remove(subscriptionId);
    }

    /**
     * Applies an event through the transition table. A legal move changes
     * {@link #status()} and notifies every subscribed listener; an illegal
     * one throws and leaves the shipment exactly where it was.
     */
    public void advance(ShipmentEvent event) {
        Map<ShipmentEvent, ShipmentStatus> legalFromHere = TRANSITIONS.getOrDefault(status, Map.of());
        ShipmentStatus next = legalFromHere.get(event);
        if (next == null) {
            throw new IllegalShipmentTransitionException(status, event);
        }
        ShipmentStatus previous = status;
        status = next;
        notifyListeners(previous, status);
    }

    /**
     * Sets the status directly, bypassing the transition table, and still
     * notifies every listener. {@link AdvanceShipmentCommand#undo()} is the
     * only caller: an operator correcting a mis-scanned event is itself a
     * real event a dashboard should see, not a silent rewind.
     */
    void restoreStatusForUndo(ShipmentStatus status) {
        ShipmentStatus previous = this.status;
        this.status = Objects.requireNonNull(status, "status");
        notifyListeners(previous, this.status);
    }

    void setWeightKg(double weightKg) {
        if (weightKg < 0.0) {
            throw new IllegalArgumentException("weightKg cannot be negative: " + weightKg);
        }
        this.weightKg = weightKg;
    }

    void setQuotedRate(Money quotedRate) {
        this.quotedRate = Objects.requireNonNull(quotedRate, "quotedRate");
    }

    void setRateStrategy(RateStrategy rateStrategy) {
        this.rateStrategy = Objects.requireNonNull(rateStrategy, "rateStrategy");
    }

    private void notifyListeners(ShipmentStatus from, ShipmentStatus to) {
        List<ShipmentStatusListener> snapshot = new ArrayList<>(listeners.values());
        for (ShipmentStatusListener listener : snapshot) {
            listener.onStatusChanged(id, from, to);
        }
    }

    private static Map<ShipmentStatus, Map<ShipmentEvent, ShipmentStatus>> buildTransitions() {
        Map<ShipmentStatus, Map<ShipmentEvent, ShipmentStatus>> table = new EnumMap<>(ShipmentStatus.class);
        table.put(ShipmentStatus.CREATED, Map.of(
                ShipmentEvent.PICK_UP, ShipmentStatus.PICKED_UP,
                ShipmentEvent.CANCEL, ShipmentStatus.CANCELLED));
        table.put(ShipmentStatus.PICKED_UP, Map.of(
                ShipmentEvent.DISPATCH, ShipmentStatus.IN_TRANSIT,
                ShipmentEvent.CANCEL, ShipmentStatus.CANCELLED));
        table.put(ShipmentStatus.IN_TRANSIT, Map.of(
                ShipmentEvent.DELIVER, ShipmentStatus.DELIVERED));
        return Map.copyOf(table);
    }
}
