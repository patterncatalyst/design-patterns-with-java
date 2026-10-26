package com.patterncatalyst.designpatterns.ch28.shipping;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The binding annotation that attaches {@link OperationLoggingInterceptor}
 * to a method or a bean class. A caller holding a {@code @LoggedOperation}
 * method behind an injected reference cannot tell, from the call site
 * alone, that a logging layer sits in front of it: the same transparency a
 * hand-written Decorator or Proxy wrapping the identical interface has to
 * preserve by hand, here enforced by the container instead.
 */
@InterceptorBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LoggedOperation {
}
