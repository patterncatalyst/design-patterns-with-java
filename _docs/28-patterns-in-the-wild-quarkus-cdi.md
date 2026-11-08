---
title: "Patterns in a real framework: Quarkus and CDI"
order: 28
layout: tutorial
sectionid: docs
part: "concurrency-patterns-in-the-wild"
description: "Quarkus's CDI container, Arc, answers the forces the hand-written patterns answer: constructor injection into a managed bean for Singleton and dependency injection, a producer method for Factory, an interceptor binding for Decorator and Proxy, a CDI event for Observer, and typed configuration for Strategy, in a shipping checkout service."
duration: "35 min"
---

## Learning objectives

By the end of this chapter you can:

- Map Singleton, Factory, Decorator, Proxy, Observer and Strategy onto the CDI mechanism that replaces each.
- Build a bean that receives its collaborators through constructor injection.
- Write a producer method that chooses between two implementations of an interface.
- Bind an interceptor to a method with a custom interceptor binding.
- Fire a CDI event and observe it from two unrelated beans.
- Bind a typed configuration interface to application properties.
- Judge when a container is worth its weight and when plain Java is the better choice.

## Why this matters

Plain Java answers each pattern's force with a class you write: an enum singleton, a factory method, a wrapper that implements the same interface, a listener list. Once enough classes need that wiring, the code that does it becomes a cost of its own.

Quarkus is built on CDI (Contexts and Dependency Injection), and its implementation, Arc, is a container whose job is to decide which object satisfies which dependency, build it and hand it over. The forces don't change. A checkout service still needs one rate quoter for the life of the process, and something still decides which implementation to build. What changes is who writes that code: `CarrierRateTable.INSTANCE` becomes an `@ApplicationScoped` annotation, and a `RateQuoterFactory.create()` becomes a `@Produces` method the container calls for you.

| Pattern | CDI mechanism |
| --- | --- |
| Singleton + dependency injection | `@ApplicationScoped` bean, constructor `@Inject` |
| Factory | `@Produces` method |
| Decorator, Proxy | `@AroundInvoke` interceptor on an `@InterceptorBinding` |
| Observer | `Event<T>.fire()` and `@Observes` |
| Strategy (selected externally) | `@ConfigMapping` interface |

{% include excalidraw.html name="28-quarkus-cdi-composition-root" caption="Figure 28.1: The Quarkus/Arc container as composition root. Each box is a CDI mechanism; the label names the plain-Java pattern it answers: @ConfigMapping for Strategy, @Produces for Factory, @ApplicationScoped plus constructor @Inject for Singleton and dependency injection, an @AroundInvoke interceptor for Decorator and Proxy, and a CDI event with @Observes for Observer." %}

## The domain: a checkout service with nothing wired by hand

The code is in `examples/28-quarkus-cdi`. A `Shipment` record carries a tracking ID, a weight and a declared value; a `RateQuote` pairs a carrier label with a `Money` amount; and the `RateQuoter` interface has two implementations, `FlatRateQuoter` and `WeightTieredRateQuoter`.

No class constructs a `RateQuoter`, registers a listener or calls `getInstance()`. Every collaborator arrives as a constructor parameter or an `@Observes` method parameter, and Arc is the only thing that reads the annotations and decides what goes where. The container is the composition root: one place, owned by the framework rather than a hand-written `main`, where every wiring decision happens.

## Constructor injection into a managed bean: Singleton and DI, provided

`CheckoutService` is an `@ApplicationScoped` bean whose constructor takes a `RateQuoter` and an `Event<ShipmentStatusChanged>`:

```java
@ApplicationScoped
public class CheckoutService {

    private final RateQuoter rateQuoter;
    private final Event<ShipmentStatusChanged> statusChangedEvent;

    @Inject
    public CheckoutService(RateQuoter rateQuoter, Event<ShipmentStatusChanged> statusChangedEvent) {
        this.rateQuoter = Objects.requireNonNull(rateQuoter, "rateQuoter");
        this.statusChangedEvent = Objects.requireNonNull(statusChangedEvent, "statusChangedEvent");
    }
    // ...
}
```

`@ApplicationScoped` tells Arc to build one `CheckoutService` for the life of the application and give that instance to every injection point. That's Singleton's intent, stated as an annotation instead of an enum constant or a holder field: two lookups through `Instance<CheckoutService>` return the same reference.

The constructor is the dependency-injection half. `CheckoutService` never calls `new` on a concrete quoter and never reaches for a static accessor. It declares what it needs, and Arc supplies a working `RateQuoter` without any factory code.

## A `@Produces` method: Factory, as a container-managed method

Something still decides between `FlatRateQuoter` and `WeightTieredRateQuoter`. `RateQuoterProducer` makes that decision as a producer method:

```java
@ApplicationScoped
public class RateQuoterProducer {

    private final RateConfig rateConfig;

    @Inject
    public RateQuoterProducer(RateConfig rateConfig) {
        this.rateConfig = rateConfig;
    }

    @Produces
    @ApplicationScoped
    public RateQuoter rateQuoter() {
        return switch (rateConfig.strategy()) {
            case "flat" -> new FlatRateQuoter(rateConfig.baseCents());
            case "weight-tiered" -> new WeightTieredRateQuoter(rateConfig.baseCents(), rateConfig.perKgCents());
            default -> throw new IllegalStateException("unknown shipping.rate.strategy: " + rateConfig.strategy());
        };
    }
}
```

`@Produces` tells Arc that this method, not a constructor, builds a `RateQuoter`. Every bean that asks for one receives what this method returns, and nothing outside the class names either implementation. That is Factory's job: one method decides which concrete class to build, and every caller depends on the interface.

The difference from a hand-written factory is that no caller invokes the method. Arc does, and because the producer method is `@ApplicationScoped`, the result is shared everywhere a `RateQuoter` is injected. With the default properties the injected quoter is weight-tiered; a Quarkus test profile that sets `shipping.rate.strategy` to `flat` gets a flat quoter, with no change to `RateQuoterProducer`.

## An `@AroundInvoke` interceptor: Decorator and Proxy, applied by annotation

`CheckoutService.quote()` carries one more annotation:

```java
@LoggedOperation
public RateQuote quote(Shipment shipment) {
    return rateQuoter.quote(shipment);
}
```

`@LoggedOperation` is a custom interceptor binding, declared once and retained at runtime:

```java
@InterceptorBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LoggedOperation {
}
```

`OperationLoggingInterceptor` is bound to it, and its `@AroundInvoke` method runs around the annotated call:

```java
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
```

A caller holding an injected `CheckoutService` can't tell that a logging layer sits between it and the quoting logic. That's the property a hand-written `LoggingRateQuoter` gets by implementing the same interface as the object it wraps. Arc gets it another way: `CheckoutService` is a normal-scoped bean, so Arc generates a client proxy for it, and the proxy checks each method for an interceptor binding before forwarding the call. `context.proceed()` is where the wrapped method runs, the same spot a decorator calls its wrapped field. One call to `quote()` leaves one `quote returned ...` entry in the shared `InvocationLog`.

## A CDI event: Observer, with no listener list anywhere

`CheckoutService.dispatch()` fires an event instead of calling anyone:

```java
public void dispatch(String trackingId) {
    statusChangedEvent.fire(new ShipmentStatusChanged(trackingId, "CREATED", "DISPATCHED"));
}
```

`DashboardNotifier` and `DeliveryAuditLog` each declare a method parameter annotated `@Observes`:

```java
@ApplicationScoped
public class DashboardNotifier {
    public void onStatusChanged(@Observes ShipmentStatusChanged event) {
        currentStatusLine = event.trackingId() + ": " + event.previousStatus() + " -> " + event.newStatus();
    }
}
```

Neither observer calls `subscribe()`, holds a subscription handle or references `CheckoutService`. The `@Observes` parameter is the whole registration: Arc finds every matching observer method at build time and delivers each fired event to all of them. One `dispatch()` reaches both beans, the fan-out a hand-written subject gives its listener list, with the list replaced by the container's event bus.

## `@ConfigMapping`: Strategy, selected from outside the program

`RateConfig` is a typed interface bound to `application.properties`:

```java
@ConfigMapping(prefix = "shipping.rate")
public interface RateConfig {
    String strategy();
    long baseCents();
    long perKgCents();
}
```

Nothing here parses a string. SmallRye Config, the configuration layer under Quarkus, reads `shipping.rate.strategy`, `shipping.rate.base-cents` and `shipping.rate.per-kg-cents`, converts the numbers to `long`, and builds an implementation of `RateConfig` at startup.

This is Strategy with the choice moved out of the program. A hand-written Strategy picks the algorithm at a call site; here a deployment picks it in a properties file, and the producer's `switch` is the small, closed dispatch that value drives. The `strategy()` field is what makes it Strategy rather than plain configuration: it chooses between two formulas, not two numbers for one formula.

> **🤖 Explore with your agent**
>
> "In `examples/28-quarkus-cdi`, keep `CheckoutService` `@ApplicationScoped` with constructor injection only, no `@Inject` fields. Keep the choice between `FlatRateQuoter` and `WeightTieredRateQuoter` in `RateQuoterProducer`'s `@Produces` method, driven by `RateConfig.strategy()` bound with `@ConfigMapping` to the `shipping.rate` prefix. Add timing to `quote()` only through `OperationLoggingInterceptor` and the `@LoggedOperation` binding, never inside the method. Fire `ShipmentStatusChanged` through `Event<ShipmentStatusChanged>` and observe it with `@Observes`."
>
> Check: `mvn -q -f examples/28-quarkus-cdi/pom.xml test -Dtest='CheckoutServiceInjectionTest,OperationLoggingInterceptorTest'`, and no `@Inject` field or `System.out.println` in `CheckoutService`.

## When not to use a framework for this

Each mechanism pays when there are enough collaborators, cross-cutting concerns or configuration that wiring them by hand costs more. Otherwise:

- **A small program.** Three or four classes with one obvious wiring need no container. `new WeightTieredRateQuoter(500, 180)` in the one place that needs it costs a line; `quarkus-arc`, the difference between `@ApplicationScoped`, `@Dependent` and `@Singleton`, and reading annotations to learn what a constructor receives cost more.
- **A library.** Code called from someone else's application shouldn't need a CDI container to build its objects. A `RateQuoter` built with `new` works in Quarkus, Spring or a bare `main`; one built only through a `@Produces` method works only inside CDI. Use the container's version inside the application that owns the composition root, never inside a library meant to be embedded in one.
- **Stacked interceptors.** An interceptor's value is that it's invisible at the call site, and that becomes a cost once several bindings stack on one method. Logging, retry and a security check as three bindings are as hard to follow as a three-deep decorator stack, and less visible than `new LoggingRateQuoter(new CachingRateQuoter(...))`. Keep each interceptor to one concern and name it after that concern.
- **One or two of the five.** A project that needs only dependency injection, or only fan-out events, is better served by the plain-Java pattern than by a container that brings all five.

## Build, run, observe

The module declares one Quarkus runtime extension, `quarkus-arc`, so there is nothing for Dev Services to start and no container in the loop. From the repository root:

```sh
mvn -f examples/pom.xml test
```

Or standalone:

```sh
cd examples/28-quarkus-cdi
mvn test
```

All 7 `@QuarkusTest` cases pass, and the Quarkus startup line reads `Installed features: [cdi]`: nothing beyond CDI loads. `examples/28-quarkus-cdi/demo.sh` runs the same `mvn test`.

### Running the same patterns with JBang

`examples/28-quarkus-cdi/jbang/cdi-patterns.java` puts all five mechanisms in one file: `CheckoutService` with constructor injection, the `@Produces` factory, `OperationLoggingInterceptor` on `@LoggedOperation`, a `ShipmentStatusChanged` event with two `@Observes` listeners, and `RateConfig` with `@ConfigMapping`.

```sh
jbang examples/28-quarkus-cdi/jbang/cdi-patterns.java
```

It needs no `pom.xml`, project directory or separate compile step. The file pins `//DEPS io.quarkus:quarkus-arc:3.40.1.1`, the same Quarkus version as the Maven module, and JBang's Quarkus integration runs the build-time Arc augmentation that `quarkus-maven-plugin` runs for `mvn test`. A plain `Arc.initialize()` over annotated classes, without that step, wouldn't wire the beans. The script prints the quote, the interceptor's entry, the singleton check and both observers' view of the event:

```
--- @ConfigMapping: externalized Strategy selection ---
shipping.rate.strategy  = weight-tiered
shipping.rate.baseCents = 500

--- constructor injection + @Produces factory + @AroundInvoke interceptor ---
quote for TRK-100 = weight-tiered $11.30
interceptor log: [quote returned RateQuote[quotedBy=weight-tiered, amount=$11.30] in 267us]
CheckoutService is an Arc-managed singleton: true

--- CDI event fired to two independent @Observes listeners ---
DashboardNotifier saw:  TRK-100: CREATED -> DISPATCHED
DeliveryAuditLog saw:   [ShipmentStatusChanged[trackingId=TRK-100, previousStatus=CREATED, newStatus=DISPATCHED]]
```

The timing varies run to run. Pass `-Dshipping.rate.strategy=flat` before the script path and the producer picks `FlatRateQuoter`, which quotes `flat-rate $5.00`, with no change to the file:

```sh
jbang -Dshipping.rate.strategy=flat examples/28-quarkus-cdi/jbang/cdi-patterns.java
```

The JBang file is for prototyping; the Maven module's seven `@QuarkusTest` cases are the record.

## For deeper coverage

- Jakarta EE, [*Contexts and Dependency Injection for Java*](https://jakarta.ee/specifications/cdi/) specification: the formal definition of beans, scopes, producers, interceptor bindings and events.
- The official [Quarkus CDI reference guide](https://quarkus.io/guides/cdi) and [Quarkus configuration reference guide](https://quarkus.io/guides/config-mappings): Arc's bean discovery, interceptor enablement through `@Priority`, `@ConfigMapping` binding, and where Arc differs from a full Jakarta EE CDI implementation.
- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original intent statements for Factory, Singleton, Decorator, Proxy, Observer and Strategy.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/28-quarkus-cdi`, extended with `@QuarkusTest` and `@TestProfile` for container-managed behavior.
- The [bibliography page]({{ '/bibliography/' | relative_url }}) lists the full reading list.
