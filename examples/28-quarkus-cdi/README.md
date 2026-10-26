# 28 - Patterns in a real framework: Quarkus and CDI

A shipping-rate checkout service built on Quarkus/CDI (Arc) instead of by
hand. Every pattern this book built with a hand-written class in earlier
chapters shows up here as a framework feature, because the forces behind
each pattern never changed; only who answers them did.

## The domain

- `Money`, `Shipment`, `RateQuote`: the same kind of exact-cents, real-domain
  records the rest of this book uses.
- `RateQuoter`: the one-method contract `FlatRateQuoter` and
  `WeightTieredRateQuoter` both satisfy. Neither implementation is a CDI
  bean in its own right.

## The five CDI mechanisms, and the pattern each one answers

1. **Constructor `@Inject` into an `@ApplicationScoped` bean = Singleton +
   dependency injection.** `CheckoutService` declares `RateQuoter` and
   `Event<ShipmentStatusChanged>` as constructor parameters. Arc resolves
   both, constructs exactly one `CheckoutService` for the application's
   lifetime, and hands every injection point the same instance.
2. **`@Produces` producer method = Factory.** `RateQuoterProducer.rateQuoter()`
   is the only place that decides between `FlatRateQuoter` and
   `WeightTieredRateQuoter`; nothing else in the module names either class.
3. **`@AroundInvoke` + `@InterceptorBinding` = Decorator/Proxy.**
   `@LoggedOperation` binds `OperationLoggingInterceptor` to
   `CheckoutService.quote()`. Arc generates the client proxy that routes a
   call through the interceptor and back; `CheckoutService` never wraps
   itself in anything.
4. **CDI event + `@Observes` = Observer.** `CheckoutService.dispatch()`
   fires a `ShipmentStatusChanged` event; `DashboardNotifier` and
   `DeliveryAuditLog` each declare an `@Observes` parameter and both receive
   the identical event with no listener list, no `Subscription` handle, and
   no reference to each other or to `CheckoutService`.
5. **`@ConfigMapping` = externalized Strategy selection.** `RateConfig`
   binds `shipping.rate.*` from `application.properties` to a typed
   interface with no parsing code; `RateQuoterProducer` reads it to choose
   which `RateQuoter` a deployment runs.

## No container anywhere in this loop

This module declares exactly one Quarkus runtime extension, `quarkus-arc`.
There is no datasource, no Kafka, no messaging extension, so there is
nothing for Quarkus Dev Services to start. `@QuarkusTest` boots the Arc
container in-process on the local JVM; `mvn test` needs no Docker.

## Build, run, observe

This example is standalone-buildable from this directory:

```sh
cd examples/28-quarkus-cdi
mvn test
```

It is also picked up by the repo root's aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 7 JUnit 5 cases under `@QuarkusTest`, asserted
with AssertJ: constructor injection resolving a working `CheckoutService`
and that bean staying the same instance across lookups, the producer
building the configured `RateQuoter` and switching implementations under a
`@TestProfile` config override, the interceptor's log entry proving it ran
around `quote()`, both observers receiving one fired event, and the typed
config mapping matching `application.properties`. Observed locally with
Temurin 25.0.2 and Quarkus 3.40.1.1:

```
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see the Arc container boot and the suite run rather than just reading an
assertion that says it works, run the example's own demo script:

```sh
./demo.sh
```

## Files

```
examples/28-quarkus-cdi/
  pom.xml                       # parent ../pom.xml for the Java version; imports the Quarkus platform BOM
  demo.sh                       # mvn test; boots Arc in-process, no container
  src/main/resources/
    application.properties        # shipping.rate.strategy/base-cents/per-kg-cents
  src/main/java/com/patterncatalyst/designpatterns/ch28/shipping/
    Money.java                      # record: exact integer cents
    Shipment.java                     # record: trackingId, weightKg, declaredValueCents
    RateQuote.java                      # record: quotedBy, amount
    RateQuoter.java                       # the shared contract
    FlatRateQuoter.java                     # flat-fee implementation
    WeightTieredRateQuoter.java               # base + per-kg implementation
    RateConfig.java                             # @ConfigMapping: Strategy, externalized
    RateQuoterProducer.java                       # @Produces: Factory
    LoggedOperation.java                            # @InterceptorBinding
    InvocationLog.java                                # shared sink the interceptor writes to
    OperationLoggingInterceptor.java                    # @AroundInvoke: Decorator/Proxy
    CheckoutService.java                                  # @ApplicationScoped + constructor @Inject: Singleton/DI
    ShipmentStatusChanged.java                              # the fired event
    DashboardNotifier.java                                    # @Observes: Observer
    DeliveryAuditLog.java                                       # @Observes: Observer
  src/test/java/com/patterncatalyst/designpatterns/ch28/shipping/
    CheckoutServiceInjectionTest.java    # constructor injection + singleton identity
    RateQuoterProducerTest.java            # producer builds the configured quoter
    FlatRateStrategyProfile.java             # @TestProfile config override
    RateQuoterProducerFlatProfileTest.java     # switching config switches the quoter
    OperationLoggingInterceptorTest.java         # interceptor ran around quote()
    ShipmentStatusEventTest.java                   # one event, two independent observers
    RateConfigMappingTest.java                       # typed config matches application.properties
```
