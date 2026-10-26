package com.patterncatalyst.designpatterns.ch28.shipping;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * This chapter's Decorator/Proxy, built as a CDI interceptor instead of a
 * hand-written wrapper class. Any bean method annotated
 * {@link LoggedOperation} runs through {@link #logInvocation} first; Arc
 * generates the client proxy that routes the call here and back, so
 * neither the annotated bean nor its callers write a single line of
 * wrapping code. The {@code @Priority} annotation is what Quarkus reads to
 * enable this interceptor; plain CDI would need the same binding declared
 * in {@code beans.xml} instead.
 */
@LoggedOperation
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class OperationLoggingInterceptor {

    private final InvocationLog invocationLog;

    @Inject
    public OperationLoggingInterceptor(InvocationLog invocationLog) {
        this.invocationLog = invocationLog;
    }

    @AroundInvoke
    public Object logInvocation(InvocationContext context) throws Exception {
        long startNanos = System.nanoTime();
        Object result = context.proceed();
        long elapsedMicros = (System.nanoTime() - startNanos) / 1_000;
        invocationLog.record(context.getMethod().getName() + " returned " + result + " in " + elapsedMicros + "us");
        return result;
    }
}
