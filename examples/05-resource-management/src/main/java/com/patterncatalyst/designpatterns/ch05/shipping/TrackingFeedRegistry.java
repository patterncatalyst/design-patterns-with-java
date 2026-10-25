package com.patterncatalyst.designpatterns.ch05.shipping;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns one {@link TrackingFeedHandle} per carrier endpoint for the life of
 * the application, not the life of any one request. A request that needs a
 * tracking update asks the registry for a handle and never closes it;
 * {@link #close()} releases every handle at once, when the application
 * itself shuts down.
 */
public final class TrackingFeedRegistry implements AutoCloseable {

    private final Map<String, TrackingFeedHandle> handles = new ConcurrentHashMap<>();

    public TrackingFeedHandle handleFor(String carrierEndpoint) {
        Objects.requireNonNull(carrierEndpoint, "carrierEndpoint");
        return handles.computeIfAbsent(carrierEndpoint, TrackingFeedHandle::subscribe);
    }

    public int openHandleCount() {
        return handles.size();
    }

    @Override
    public void close() {
        handles.values().forEach(TrackingFeedHandle::close);
        handles.clear();
    }
}
