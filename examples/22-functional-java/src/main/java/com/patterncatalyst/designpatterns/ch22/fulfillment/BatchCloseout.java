package com.patterncatalyst.designpatterns.ch22.fulfillment;

import java.util.Objects;

/**
 * Runs whatever a caller supplies when the nightly batch finishes, using
 * {@link Runnable} itself as the Command: a request with no argument and no
 * return value needs nothing more than the functional interface already
 * built into the platform, with no {@code BatchCloseoutCommand} interface of
 * this example's own to declare first.
 */
public final class BatchCloseout {

    private BatchCloseout() {
    }

    public static void run(Runnable onComplete) {
        Objects.requireNonNull(onComplete, "onComplete");
        onComplete.run();
    }
}
