//usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.quarkus:quarkus-arc:3.40.1.1

// Chapter 28's five CDI mechanisms (constructor injection into an
// @ApplicationScoped bean, a @Produces factory method, an @AroundInvoke
// interceptor bound by an @InterceptorBinding, a CDI event with @Observes,
// and a @ConfigMapping) collapsed into one JBang-runnable file.
//
// This uses the real Quarkus JBang integration, not a hand-rolled
// workaround: JBang detects the io.quarkus:quarkus-arc //DEPS line and
// hands the file to io.quarkus.launcher.JBangIntegration, which runs the
// same build-time augmentation (Jandex indexing, bean discovery, Arc
// bytecode generation) the Maven module's `mvn test` triggers through the
// quarkus-maven-plugin. Plain `Arc.initialize()` over annotated classes
// with no augmentation step would NOT wire these beans, because
// annotation-based Arc beans are processed at build time; this script gets
// that build step from JBang itself, so every annotation below is live,
// not simulated.
//
// Run it:
//   jbang jbang/cdi-patterns.java
//
// Override the Strategy selection Chapter 28 covers under @ConfigMapping
// without touching the file (the -D must come before the script path, so
// jbang forwards it as a JVM system property rather than a program
// argument):
//   jbang -Dshipping.rate.strategy=flat jbang/cdi-patterns.java

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.smallrye.config.ConfigMapping;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

// An exact integer-cents amount; not a bean, just a value type.
record Money(long cents) {
    @Override
    public String toString() {
        return String.format("$%d.%02d", cents / 100, cents % 100);
    }
}

// A shipment to quote; not a bean, just a value type.
record Shipment(String trackingId, double weightKg) {
}

// A carrier's quoted price for a Shipment; not a bean, just a value type.
record RateQuote(String quotedBy, Money amount) {
}

// The event CheckoutService.dispatch() fires; not a bean, just a value type.
record ShipmentStatusChanged(String trackingId, String previousStatus, String newStatus) {
}

// The Strategy contract. Neither implementation below is a CDI bean in its
// own right; RateQuoterProducer is the only thing that builds one.
interface RateQuoter {
    RateQuote quote(Shipment shipment);
}

final class FlatRateQuoter implements RateQuoter {
    private final long flatCents;

    FlatRateQuoter(long flatCents) {
        this.flatCents = flatCents;
    }

    @Override
    public RateQuote quote(Shipment shipment) {
        return new RateQuote("flat-rate", new Money(flatCents));
    }
}

final class WeightTieredRateQuoter implements RateQuoter {
    private final long baseCents;
    private final long perKgCents;

    WeightTieredRateQuoter(long baseCents, long perKgCents) {
        this.baseCents = baseCents;
        this.perKgCents = perKgCents;
    }

    @Override
    public RateQuote quote(Shipment shipment) {
        long cents = baseCents + Math.round(perKgCents * shipment.weightKg());
        return new RateQuote("weight-tiered", new Money(cents));
    }
}

// Typed configuration bound from system properties / application.properties
// under shipping.rate.*, with no parsing code anywhere. This is this
// chapter's externalized Strategy selection.
@ConfigMapping(prefix = "shipping.rate")
interface RateConfig {
    String strategy();

    long baseCents();

    long perKgCents();
}

// This chapter's Factory, as a CDI producer method instead of a
// hand-written factory class. rateQuoter() is the one place that decides
// which RateQuoter implementation this process gets.
@ApplicationScoped
class RateQuoterProducer {
    private final RateConfig rateConfig;

    @Inject
    RateQuoterProducer(RateConfig rateConfig) {
        this.rateConfig = rateConfig;
    }

    @Produces
    @ApplicationScoped
    RateQuoter rateQuoter() {
        return switch (rateConfig.strategy()) {
            case "flat" -> new FlatRateQuoter(rateConfig.baseCents());
            case "weight-tiered" -> new WeightTieredRateQuoter(rateConfig.baseCents(), rateConfig.perKgCents());
            default -> throw new IllegalStateException("unknown shipping.rate.strategy: " + rateConfig.strategy());
        };
    }
}

// A shared, injectable sink for OperationLoggingInterceptor, so the demo
// can print that the interceptor ran without parsing console output.
@ApplicationScoped
class InvocationLog {
    private final List<String> entries = new CopyOnWriteArrayList<>();

    void record(String entry) {
        entries.add(entry);
    }

    List<String> entries() {
        return List.copyOf(entries);
    }
}

// The binding annotation that attaches OperationLoggingInterceptor to a
// method or a bean class.
@InterceptorBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@interface LoggedOperation {
}

// This chapter's Decorator/Proxy, as a CDI interceptor instead of a
// hand-written wrapper class.
@LoggedOperation
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
class OperationLoggingInterceptor {
    private final InvocationLog invocationLog;

    @Inject
    OperationLoggingInterceptor(InvocationLog invocationLog) {
        this.invocationLog = invocationLog;
    }

    @AroundInvoke
    Object logInvocation(InvocationContext context) throws Exception {
        long startNanos = System.nanoTime();
        Object result = context.proceed();
        long elapsedMicros = (System.nanoTime() - startNanos) / 1_000;
        invocationLog.record(context.getMethod().getName() + " returned " + result + " in " + elapsedMicros + "us");
        return result;
    }
}

// One of two independent observers of ShipmentStatusChanged. Declaring the
// @Observes parameter is the entire subscription; this bean never
// references CheckoutService.
@ApplicationScoped
class DashboardNotifier {
    private volatile String currentStatusLine = "no shipment dispatched yet";

    void onStatusChanged(@Observes ShipmentStatusChanged event) {
        currentStatusLine = event.trackingId() + ": " + event.previousStatus() + " -> " + event.newStatus();
    }

    String currentStatusLine() {
        return currentStatusLine;
    }
}

// The second, independent observer of ShipmentStatusChanged. Arc delivers
// the identical event object to this bean and to DashboardNotifier in the
// same fire() call; neither observer knows the other exists.
@ApplicationScoped
class DeliveryAuditLog {
    private final List<ShipmentStatusChanged> entries = new CopyOnWriteArrayList<>();

    void onStatusChanged(@Observes ShipmentStatusChanged event) {
        entries.add(event);
    }

    List<ShipmentStatusChanged> entries() {
        return List.copyOf(entries);
    }
}

// The checkout-side collaborator every mechanism above meets at.
// @ApplicationScoped makes Arc construct exactly one instance for the
// process's lifetime, the Singleton guarantee Chapter 28 says the rest of
// this book built by hand. The constructor never calls new on a
// RateQuoter and never reaches for a static accessor; Arc resolves both
// constructor parameters.
@ApplicationScoped
class CheckoutService {
    private final RateQuoter rateQuoter;
    private final Event<ShipmentStatusChanged> statusChangedEvent;

    @Inject
    CheckoutService(RateQuoter rateQuoter, Event<ShipmentStatusChanged> statusChangedEvent) {
        this.rateQuoter = Objects.requireNonNull(rateQuoter, "rateQuoter");
        this.statusChangedEvent = Objects.requireNonNull(statusChangedEvent, "statusChangedEvent");
    }

    @LoggedOperation
    RateQuote quote(Shipment shipment) {
        return rateQuoter.quote(shipment);
    }

    void dispatch(String trackingId) {
        statusChangedEvent.fire(new ShipmentStatusChanged(trackingId, "CREATED", "DISPATCHED"));
    }
}

// Entry point. Field injection is used here only because this class is the
// JBang/Quarkus entry point, not a business bean; CheckoutService and
// RateQuoterProducer above still take their collaborators through the
// constructor, the idiom the rest of this book teaches.
@QuarkusMain
class CdiPatterns implements QuarkusApplication {

    @Inject
    CheckoutService checkoutService;

    @Inject
    InvocationLog invocationLog;

    @Inject
    DashboardNotifier dashboardNotifier;

    @Inject
    DeliveryAuditLog deliveryAuditLog;

    @Inject
    RateConfig rateConfig;

    public static void main(String... args) {
        // Defaults matching ../src/main/resources/application.properties,
        // kept here instead of a companion properties file so this stays a
        // single-file script. Running with
        // -Dshipping.rate.strategy=flat before the script path overrides
        // this default, the same override RateQuoterProducerFlatProfileTest
        // exercises through a test profile in the Maven module.
        System.getProperties().putIfAbsent("shipping.rate.strategy", "weight-tiered");
        System.getProperties().putIfAbsent("shipping.rate.base-cents", "500");
        System.getProperties().putIfAbsent("shipping.rate.per-kg-cents", "180");
        Quarkus.run(CdiPatterns.class, args);
    }

    @Override
    public int run(String... args) {
        System.out.println("--- @ConfigMapping: externalized Strategy selection ---");
        System.out.println("shipping.rate.strategy  = " + rateConfig.strategy());
        System.out.println("shipping.rate.baseCents = " + rateConfig.baseCents());

        System.out.println();
        System.out.println("--- constructor injection + @Produces factory + @AroundInvoke interceptor ---");
        Shipment shipment = new Shipment("TRK-100", 3.5);
        RateQuote quote = checkoutService.quote(shipment);
        System.out.println("quote for " + shipment.trackingId() + " = " + quote.quotedBy() + " " + quote.amount());
        System.out.println("interceptor log: " + invocationLog.entries());

        boolean sameInstance = checkoutService == Arc.container().instance(CheckoutService.class).get();
        System.out.println("CheckoutService is an Arc-managed singleton: " + sameInstance);

        System.out.println();
        System.out.println("--- CDI event fired to two independent @Observes listeners ---");
        checkoutService.dispatch(shipment.trackingId());
        System.out.println("DashboardNotifier saw:  " + dashboardNotifier.currentStatusLine());
        System.out.println("DeliveryAuditLog saw:   " + deliveryAuditLog.entries());

        return 0;
    }
}
