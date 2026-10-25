package com.patterncatalyst.designpatterns.ch04.notification;

import java.util.Objects;

/** What a {@link MessageSender} hands back after accepting or rejecting a send. */
public record DispatchResult(DispatchStatus status, String dispatchId) {

    public DispatchResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(dispatchId, "dispatchId");
    }
}
