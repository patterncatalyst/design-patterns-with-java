package com.patterncatalyst.designpatterns.ch17.shipping;

/**
 * The collaborator a {@link ShipmentBox} tells about an overweight
 * condition. Most boxes never wire up a real one, which is exactly the case
 * {@link NullShipmentAlertHandler} exists to make unremarkable.
 */
public interface ShipmentAlertHandler {
    void onOverweight(String boxLabel, long gramsOverLimit);
}
