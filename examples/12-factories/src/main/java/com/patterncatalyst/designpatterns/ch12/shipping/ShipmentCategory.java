package com.patterncatalyst.designpatterns.ch12.shipping;

/**
 * Which inspection sequence a shipment needs before it leaves a hub. An
 * onboarding workflow reads this off the shipment manifest and hands it to
 * {@link ShipmentInspectors#forCategory(ShipmentCategory)}.
 */
public enum ShipmentCategory {
    STANDARD,
    HAZMAT
}
