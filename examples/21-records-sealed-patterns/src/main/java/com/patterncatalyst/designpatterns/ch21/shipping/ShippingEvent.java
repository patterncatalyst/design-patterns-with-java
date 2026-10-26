package com.patterncatalyst.designpatterns.ch21.shipping;

/**
 * A closed set of events a shipment can raise on its way from checkout to
 * a customer's door. Every kind carries the ParcelKind it concerns, plus
 * whatever extra data that event needs; the set is sealed so an exhaustive
 * switch over it can drop the default branch every open hierarchy forces.
 */
public sealed interface ShippingEvent
        permits QuoteRequested, LabelPurchased, HubScan, DeliveryException, Delivered {

    ParcelKind parcel();
}
