package com.patterncatalyst.designpatterns.ch12.shipping;

/**
 * The regional hub a shipment departs from. Each region runs its own
 * compliance scheme: its own label format, its own rate table, and its own
 * customs paperwork. A shipment's region decides which {@link ShippingToolkit}
 * must build every one of those three documents, because mixing paperwork
 * across regions produces a shipment customs will reject.
 */
public enum Region {
    NORTH_AMERICA,
    EUROPEAN_UNION,
    ASIA_PACIFIC
}
