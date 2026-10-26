package com.patterncatalyst.designpatterns.ch28.shipping;

/** The CDI event {@link CheckoutService#dispatch(String)} fires. */
public record ShipmentStatusChanged(String trackingId, String previousStatus, String newStatus) {
}
