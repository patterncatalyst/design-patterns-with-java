package com.patterncatalyst.designpatterns.ch05.shipping;

/**
 * One status reading pulled from a carrier's live tracking feed.
 */
public record TrackingUpdate(String trackingId, String status) {
}
