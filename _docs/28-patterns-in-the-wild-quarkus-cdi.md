---
title: "Patterns in a real framework: Quarkus and CDI"
order: 28
layout: tutorial
sectionid: docs
part: "concurrency-patterns-in-the-wild"
description: "How Quarkus's CDI container, Arc, answers the same forces this book built by hand: constructor injection into a container-managed bean for Singleton and dependency injection, a producer method for Factory, an interceptor binding for Decorator and Proxy, a CDI event for Observer, and typed configuration for Strategy, taught against a shipping checkout service."
duration: "35 min"
---

## Learning objectives

By the end of this chapter you can:

- Explain why a dependency-injection container is not a different idea from
  the patterns this book built by hand, but the same forces answered by a
  framework instead of a class a programmer writes.
- Build a CDI bean that receives its collaborators through constructor
  injection, and say which earlier pattern, Singleton plus dependency
  injection, that bean's container-managed lifecycle replaces.
- Write a producer method that chooses between two implementations of an
  interface, and say which earlier pattern it stands in for.
- Bind a custom interceptor to a method with an interceptor binding
  annotation, and say which earlier pattern an interceptor-wrapped call
  resembles from the outside.
- Fire a CDI event from one bean and observe it from two unrelated beans,
  and say which earlier pattern a CDI event replaces.
- Bind a typed configuration interface to application properties, and
  explain what that interface buys over reading a raw string by hand.
- Judge when reaching for a full framework to get these five capabilities is
  worth its weight, and when the plain Java version this book already
  taught is the stronger choice.

## Why this matters

Every pattern in this book answers a force: an algorithm that varies
independently of its caller, an object that needs exactly one instance, a
construction step that should not leak into the rest of a program, a
component that needs to react to another component's state without naming
it directly. Plain Java answers each of those forces with a class a
programmer writes by hand: an interface and a factory, an enum singleton, a
decorator that wraps an interface, a listener list a subject calls into.
That code is correct, and it is also exactly the kind of code a framework
exists to take off a programmer's hands once a codebase grows past a
handful of classes that need it.

Quarkus is built on CDI, Contexts and Dependency Injection, and its
implementation, Arc, is a dependency-injection container: a piece of
infrastructure whose entire job is resolving which object satisfies which
dependency, constructing it, and handing it to whatever asked. That sentence
describes the exact job the earlier chapters on Singleton, Factory, and
dependency injection did by hand with holder classes and constructor
parameters. Nothing about the underlying force changed. A checkout
service still needs exactly one rate-quoting collaborator for the life of
the process; something still has to decide which concrete implementation to
build. What changed is who writes the code that answers those forces. A
programmer writing `CarrierRateTable.INSTANCE` by hand becomes an
`@ApplicationScoped` annotation the container reads. A `RateQuoterFactory`
class with a `create()` method becomes a `@Produces` method the container
calls on a reader's behalf. The forces this book spent twenty-seven
chapters teaching are the same forces; Quarkus just answers four or five of
them at once, consistently, across an entire application, instead of one
hand-rolled class at a time.

This chapter is not an argument that a framework is always the right
answer. It is a translation exercise: for each pattern already in this
book's catalogue, see the framework feature that exists because the
pattern's force shows up often enough in real applications to be worth
baking into infrastructure, and see what that feature looks like applied to
the same kind of shipping-checkout problem. The closing section argues the
other direction, for when the plain Java version remains the better call.

{% include excalidraw.html name="28-quarkus-cdi-composition-root" caption="Figure 28.1: The Quarkus/Arc container as composition root. Each box is a CDI mechanism; the label names the plain-Java pattern it answers: @ConfigMapping for Strategy, @Produces for Factory, @ApplicationScoped plus constructor @Inject for Singleton and dependency injection, an @AroundInvoke interceptor for Decorator and Proxy, and a CDI event with @Observes for Observer." %}

## The domain: a checkout service with nothing wired by hand

The example behind this chapter lives in `examples/28-quarkus-cdi`. Its
domain is the same shipping checkout this book has used throughout: a
`Shipment` record carrying a tracking ID, a weight, and a declared value; a
`RateQuote` record pairing a carrier label with a `Money` amount; and a
`RateQuoter` interface with two implementations, `FlatRateQuoter` and
`WeightTieredRateQuoter`. What is different from every earlier chapter is
that no class in this example constructs a `RateQuoter`, registers a
listener, or calls `getInstance()` anywhere. Every object that needs a
collaborator declares it as a constructor parameter or an `@Observes`
method parameter, and Quarkus's Arc container is the one thing in the whole
module that reads an annotation and decides what goes where. The container
is the composition root this book's dependency-injection work always
pointed toward: a single place, owned by the framework instead of a
programmer-written `main` method, where every wiring decision happens.

## Constructor injection into a managed bean: Singleton and DI, provided

`CheckoutService` is an `@ApplicationScoped` bean whose constructor takes a
`RateQuoter` and an `Event<ShipmentStatusChanged>`:

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

`@ApplicationScoped` is the annotation that tells Arc to construct exactly
one `CheckoutService` for the life of the application and hand that same
instance to every injection point that asks for one. That guarantee is the
Singleton pattern's intent statement, restated as an annotation instead of
an enum constant or a holder class's static field. The dependency-injection
half of the same claim is in the constructor: `CheckoutService` never calls
`new` on a concrete `RateQuoter` implementation and never reaches for a
static accessor. It declares what it needs and lets something else supply
it, the identical inversion this book's dependency-injection chapter built
by passing a `RateLookup` through a constructor parameter instead of
reaching into a global. Arc fills that role here instead of a
hand-written `main` method deciding which concrete class to pass at
startup. `CheckoutServiceInjectionTest` proves both halves of this claim
directly: injecting `CheckoutService` and calling `quote()` confirms the
container resolved a working `RateQuoter` with no factory code written
anywhere in the test, and a second test asks for the bean twice through an
`Instance<CheckoutService>` and asserts both calls return the exact same
object reference, the observable fact behind `@ApplicationScoped`.

## A `@Produces` method: Factory, as a container-managed method

Something still has to decide whether `CheckoutService` gets a
`FlatRateQuoter` or a `WeightTieredRateQuoter`. `RateQuoterProducer` is that
decision, written as a CDI producer method:

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

`@Produces` tells Arc that this method, not a constructor, is how to build a
`RateQuoter`. Every bean in the module that asks for a `RateQuoter`,
`CheckoutService` among them, receives whatever this one method returns,
and nothing outside this class ever names `FlatRateQuoter` or
`WeightTieredRateQuoter` directly. That is the Factory pattern's job
description word for word: centralize the decision of which concrete class
to construct behind one method, so every caller depends on the interface
alone. The earlier Factory chapter wrote that method by hand, as a static
method or a small factory class a caller invoked explicitly. Here the
method still exists and still makes the same decision; a caller never calls
it. Arc calls it once, caches the result because the producer itself is
annotated `@ApplicationScoped`, and injects the cached answer everywhere a
`RateQuoter` is needed. `RateQuoterProducerTest` confirms the quoter the
container built behaves like a weight-tiered quoter for the default
configuration, and `RateQuoterProducerFlatProfileTest` reruns the identical
assertion under a Quarkus test profile that overrides `shipping.rate.strategy`
to `flat`, proving the producer's choice tracks configuration with no
change to `RateQuoterProducer` itself.

## An `@AroundInvoke` interceptor: Decorator and Proxy, applied by annotation

`CheckoutService.quote()` carries one more annotation:

```java
@LoggedOperation
public RateQuote quote(Shipment shipment) {
    return rateQuoter.quote(shipment);
}
```

`@LoggedOperation` is a custom interceptor binding, declared once as its own
annotation and retained at runtime:

```java
@InterceptorBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LoggedOperation {
}
```

`OperationLoggingInterceptor` is the class bound to it, with an
`@AroundInvoke` method that runs before and after the annotated call:

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

A caller holding an injected `CheckoutService` reference cannot tell, from
the call site, that a logging layer sits between it and the quoting logic.
That transparency is exactly the property this book's Decorator and Proxy
chapter spent its examples proving by hand, where a `LoggingRateQuoter`
wrapped a `RateQuoter` behind the identical interface so a caller stayed
unaware of the wrapper. Arc gets the same transparency a different way:
because `CheckoutService` is a normal-scoped bean, Arc generates a client
proxy for it, and every call that reaches the proxy checks whether the
target method carries an interceptor binding before forwarding the call.
`context.proceed()` is where the wrapped behavior runs, the same spot a
hand-written decorator's call to its wrapped field would sit.
`OperationLoggingInterceptorTest` calls `checkoutService.quote()` once and
asserts the shared `InvocationLog` recorded an entry for the `quote` method,
confirming the interceptor ran around the call rather than merely compiling
in beside it.

## A CDI event: Observer, with no listener list anywhere

`CheckoutService.dispatch()` fires an event instead of calling a method:

```java
public void dispatch(String trackingId) {
    statusChangedEvent.fire(new ShipmentStatusChanged(trackingId, "CREATED", "DISPATCHED"));
}
```

`DashboardNotifier` and `DeliveryAuditLog` each declare a method parameter
annotated `@Observes`:

```java
@ApplicationScoped
public class DashboardNotifier {
    public void onStatusChanged(@Observes ShipmentStatusChanged event) {
        currentStatusLine = event.trackingId() + ": " + event.previousStatus() + " -> " + event.newStatus();
    }
}
```

Neither observer calls a `subscribe()` method, holds a `Subscription`
handle, or references `CheckoutService` at all. Declaring the `@Observes`
parameter is the entire registration; Arc discovers every bean with a
matching observer method at startup and routes every fired event of that
type to all of them. That is the Observer pattern's fan-out, with the
listener list this book built by hand replaced by the container's own event
bus. `ShipmentStatusEventTest` fires one event through
`checkoutService.dispatch()` and asserts both `DashboardNotifier` and
`DeliveryAuditLog` received it, confirming the one-event-to-many-observers
guarantee a hand-written `ShipmentTracker` subject gave its own listener
list.

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

Nothing in this interface parses a string or validates a property by hand.
SmallRye Config, the configuration layer Quarkus builds on, reads
`shipping.rate.strategy`, `shipping.rate.base-cents`, and
`shipping.rate.per-kg-cents` from `application.properties` and builds an
implementation of `RateConfig` at startup, converting `base-cents` to
`long` along the way. `RateConfigMappingTest` asserts the three accessor
methods return exactly the values the properties file declares. This is
the externalized half of Strategy this book's own Strategy chapter named
but did not build: the earlier chapter picked a rate strategy inside the
program, at a call site a programmer controlled. Here the choice of
algorithm lives in a text file a deployment can change without touching a
line of compiled code, and `RateQuoterProducer`'s `switch` expression is the
small, closed dispatch a configuration value drives. A `RateConfig` that
instead held the numeric parameters for one single formula would be
ordinary configuration. The `strategy()` field choosing between two
different formulas, not just two different numbers plugged into one, is
what makes this Strategy rather than plain configuration.

> **AI sidebar.** Ask an LLM pairing partner to "wire up dependency
> injection in Quarkus" and a common result imports `quarkus-arc`, annotates
> a class `@ApplicationScoped`, and stops there, treating the container as a
> replacement for writing a class rather than a replacement for an entire
> pattern with its own forces. Check what a generated bean depends on before
> accepting it: a service field-injected rather than constructor-injected
> still works, but it reintroduces the hidden-dependency problem the
> plain-Java dependency-injection chapter argued against, because a field
> with no constructor parameter is just as invisible to a reader as a static
> accessor was. The same scrutiny applies
> to a generated interceptor: a model asked for "logging" sometimes adds a
> `System.out.println` directly inside the business method instead of an
> `@AroundInvoke` interceptor, which compiles and passes a quick test, then
> has to be copied into every method that needs the same logging later. A
> generated CDI bean with `@Inject` fields and no constructor, or a
> cross-cutting concern folded into a business method instead of an
> interceptor, is the sign a pairing partner reached for the annotation that
> compiles fastest instead of the shape that matches the pattern it is
> replacing.

## When not to use a framework for this

Every mechanism in this chapter earns its cost under the same condition:
enough collaborators, enough cross-cutting concerns, or enough
configuration surface that hand-wiring them one at a time becomes the more
expensive option. A small program with three or four classes and one
obvious way to wire them does not need a dependency-injection container at
all. Writing `new WeightTieredRateQuoter(500, 180)` once, in the one place
that needs it, costs a line. Pulling in `quarkus-arc`, learning
`@ApplicationScoped` versus `@Dependent` versus `@Singleton`, and reading an
annotation to understand what a constructor receives costs considerably
more, for a program small enough that the plain-Java version from earlier
chapters already reads clearly start to finish.

A library is a sharper case than a small program. Code meant to be called
from someone else's codebase, not run as its own application, should not
require a CDI container to construct its objects. A `RateQuoter`
implementation published as a library and built with `new` in the
constructor works in a Quarkus application, a Spring application, or a bare
`main` method with no framework at all; the identical class built only
through a `@Produces` method only works inside a CDI container, which
forecloses every one of those other uses. The dependency-injection pattern
this book taught with a constructor parameter is already framework-neutral;
reach for the container's version only inside the application that owns the
composition root, never inside a library meant to be embedded in one.

The interceptor mechanism carries its own warning. An `@AroundInvoke`
interceptor's value comes from being invisible at the call site, and that
same invisibility is a cost once more than one or two bindings start
stacking on the same method. A method wrapped by three different interceptor
bindings, each contributing logging, retry, and a security check, is
exactly as hard to reason about as the three-deep decorator stack the
Decorator chapter warned against, with the added difficulty that an
interceptor's binding is declared on the method, not visible at the call
site the way `new LoggingRateQuoter(new CachingRateQuoter(...))` is. Keep
each interceptor's job narrow and name it after the one concern it adds, the
same discipline this book asked of a hand-written decorator stack.

Finally, this chapter's five mechanisms are not a reason to reach for a
framework on every project. They are what Quarkus, or any CDI-based
framework, gives an application once that application already needs
dependency injection, factories, cross-cutting method wrapping, fan-out
notifications, and externalized configuration at the same time. A project
that needs only one or two of those five is better served picking up the
one plain-Java pattern it needs from earlier in this book than pulling in a
container to get all five whether the project needs them or
not.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this
loop. This module declares exactly one Quarkus runtime extension,
`quarkus-arc`, so there is no datasource, Kafka, or messaging extension to
trigger Quarkus Dev Services. From the repository root, the aggregator
picks up `examples/28-quarkus-cdi` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/28-quarkus-cdi
mvn test
```

Either path runs the same 7 JUnit 5 cases under `@QuarkusTest`, asserted
with AssertJ: constructor injection resolving a working `CheckoutService`
and that bean staying the same instance across lookups, the producer
building the configured `RateQuoter` and switching implementations under a
test profile's config override, the interceptor's log entry proving it ran
around `quote()`, both observers receiving one fired event, and the typed
config mapping matching `application.properties`. Observed locally with
Temurin 25.0.2 and Quarkus 3.40.1.1:

```
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see Arc boot the container and run the suite rather than just reading an
assertion that says it works, run the example's own demo script:

```sh
examples/28-quarkus-cdi/demo.sh
```

Its startup banner reports `Installed features: [cdi]`, the direct
confirmation that nothing beyond CDI itself loaded for this module, no
datasource, no HTTP layer, no messaging client waiting on a container that
was never started.

## For deeper coverage

- Jakarta EE, [*Contexts and Dependency Injection for Java*](https://jakarta.ee/specifications/cdi/) specification: the formal definition of beans, scopes, producers, interceptor bindings, and events this chapter's example relies on.
- The official [Quarkus CDI reference guide](https://quarkus.io/guides/cdi) and [Quarkus configuration reference guide](https://quarkus.io/guides/config-mappings): Arc's specific behavior for bean discovery, interceptor enablement through `@Priority`, and `@ConfigMapping` binding, including the differences from a full Jakarta EE CDI implementation this chapter's example depends on.
- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original intent statements for Factory, Singleton, Decorator, Proxy, Observer, and Strategy that this chapter maps onto CDI mechanisms one by one.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/28-quarkus-cdi`'s suite, extended here with `@QuarkusTest` and `@TestProfile` to verify container-managed behavior rather than plain object construction.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
