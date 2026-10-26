package com.patterncatalyst.designpatterns.ch17.shipping;

/**
 * The Null Object: a do-nothing {@link ShipmentAlertHandler}. {@link
 * ShipmentBox#checkWeightLimit()} calls this unconditionally when no real
 * handler was bound, so no call site anywhere needs an {@code if (handler !=
 * null)} guard. An enum with one constant is the idiomatic Java 25 shape for
 * a type that must have exactly one instance; the language's own singleton
 * guarantee replaces a hand-written private constructor and static field.
 */
public enum NullShipmentAlertHandler implements ShipmentAlertHandler {
    INSTANCE;

    @Override
    public void onOverweight(String boxLabel, long gramsOverLimit) {
        // Intentionally does nothing. No alerting is wired up for this box;
        // the caller never has to know that, because it calls this method
        // exactly the way it would call a real handler.
    }
}
