package com.patterncatalyst.designpatterns.ch18.shipping.dispatch;

/**
 * The shared base every station in the dispatch desk extends. A colleague
 * holds no reference to any other colleague, only to the one
 * {@link DispatchMediator} it was bound to; {@link #raise} is the only way a
 * subclass can reach outward at all.
 *
 * <p>{@link #bindMediator} is package-private on purpose. Java has no
 * {@code friend} declaration, but a package-private method reachable only
 * from the same package, here {@link DispatchCoordinator}, is the idiomatic
 * substitute: every other caller, including the colleague subclasses
 * themselves, sees a type with no visible way to rewire its mediator.
 */
public abstract class DispatchColleague {

    private DispatchMediator mediator;

    void bindMediator(DispatchMediator mediator) {
        this.mediator = mediator;
    }

    protected void raise(DispatchEvent event, String orderId) {
        if (mediator != null) {
            mediator.notify(this, event, orderId);
        }
    }
}
