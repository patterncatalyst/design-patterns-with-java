package com.patterncatalyst.designpatterns.ch16.shipping.proxy;

import com.patterncatalyst.designpatterns.ch16.shipping.ClearanceLevel;
import com.patterncatalyst.designpatterns.ch16.shipping.RateQuoter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builds a {@link RateQuoter} access-control proxy with
 * {@link Proxy#newProxyInstance} instead of a hand-written wrapper class.
 * The dynamic proxy implements the identical {@code RateQuoter} interface a
 * hand-written proxy would, generated at runtime from the interface alone,
 * and gates every call on the caller's {@link ClearanceLevel} before
 * forwarding to the real subject. It changes nothing about what a permitted
 * call returns; a caller with sufficient clearance gets back exactly what
 * the wrapped {@code RateQuoter} would have returned on its own.
 */
public final class RateQuoterAccessControl {

    private RateQuoterAccessControl() {
    }

    public static RateQuoter guarded(RateQuoter real, ClearanceLevel callerLevel, ClearanceLevel requiredLevel) {
        Objects.requireNonNull(real, "real");
        Objects.requireNonNull(callerLevel, "callerLevel");
        Objects.requireNonNull(requiredLevel, "requiredLevel");
        GuardedInvocationHandler handler = new GuardedInvocationHandler(real, callerLevel, requiredLevel);
        return (RateQuoter) Proxy.newProxyInstance(
                RateQuoterAccessControl.class.getClassLoader(),
                new Class<?>[] {RateQuoter.class},
                handler);
    }

    /** Number of calls the dynamic proxy backing {@code quoter} has intercepted. */
    public static int callCount(RateQuoter quoter) {
        InvocationHandler handler = Proxy.getInvocationHandler(quoter);
        if (handler instanceof GuardedInvocationHandler guarded) {
            return guarded.callCount();
        }
        throw new IllegalArgumentException("not a RateQuoterAccessControl proxy: " + quoter);
    }

    private static final class GuardedInvocationHandler implements InvocationHandler {
        private final RateQuoter real;
        private final ClearanceLevel callerLevel;
        private final ClearanceLevel requiredLevel;
        private final AtomicInteger calls = new AtomicInteger();

        GuardedInvocationHandler(RateQuoter real, ClearanceLevel callerLevel, ClearanceLevel requiredLevel) {
            this.real = real;
            this.callerLevel = callerLevel;
            this.requiredLevel = requiredLevel;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("quote".equals(method.getName())) {
                calls.incrementAndGet();
                if (callerLevel.ordinal() < requiredLevel.ordinal()) {
                    throw new SecurityException(
                            "access denied: rate lookup requires " + requiredLevel
                                    + " clearance, caller has " + callerLevel);
                }
            }
            return method.invoke(real, args);
        }

        int callCount() {
            return calls.get();
        }
    }
}
