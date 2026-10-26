package com.patterncatalyst.designpatterns.ch18.shipping.tracking;

/**
 * A handle returned by {@link ShipmentTracker#subscribe}. {@code close()}
 * removes the subscription, which makes a subscription a natural fit for
 * try-with-resources: a listener's lifetime and its subscription's lifetime
 * stay tied together without a caller having to remember a separate
 * unsubscribe call on every exit path, including an exception.
 *
 * <p>This interface narrows {@link AutoCloseable#close()} to declare no
 * checked exception, the same idiom {@code java.io.Closeable} itself does
 * not follow but plenty of narrower resource handles do: closing a
 * subscription cannot fail, so forcing every caller to catch or declare
 * {@code Exception} would be ceremony with nothing behind it.
 */
public interface Subscription extends AutoCloseable {

    @Override
    void close();
}
