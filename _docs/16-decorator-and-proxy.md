---
title: "Decorator and Proxy"
order: 16
layout: tutorial
sectionid: docs
part: "composing-adapting-structure"
description: "Two structural patterns that wrap an object behind its own interface, taught against a shipping-rate quoting pipeline: Decorator stacks layers that each add one responsibility in an order that changes the result, and Proxy stands a single surrogate in front of an expensive or access-controlled subject without adding any new capability."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Wrap an object behind the interface it already implements, so callers can't tell how many wrappers sit in front of it.
- Build a Decorator stack with one responsibility per layer, and predict how the wrapping order changes the result.
- Write a decorator as a named class or as a lambda, and choose between them.
- Build a lazy-initialization Proxy by hand, and an access-control Proxy with `java.lang.reflect.Proxy` and an `InvocationHandler`.
- Tell Decorator from Proxy by intent when the code looks the same.
- Recognize when neither wrapper has anything to do.

## Why this matters

A checkout pipeline asks for a shipping quote, and it wants more than the quote: a surcharge on high-value shipments, a cache so a cart priced three times doesn't hit the tariff lookup three times, and a log line for the next billing dispute. Each concern changes on its own schedule. Finance renegotiates the surcharge, load testing adds the cache, support asks for the audit trail. Decorator builds that pipeline from layers that all implement the quote interface.

A warehouse app has a related problem. Opening a session against the live tariff API is expensive, so it should wait until the first lookup. And a guest account shouldn't reach a gateway that a warehouse badge unlocks. Proxy solves both: a surrogate with the same interface that changes when, or whether, a call reaches the real subject, never what a successful call returns.

Both patterns wrap an object behind its own interface. They differ in what the wrapper is for.

{% include excalidraw.html name="16-decorator-proxy" caption="Figure 16.1: a Decorator stack (surcharge, caching, logging) wrapping a core LiveCarrierGateway, next to a Proxy (RateQuoterAccessControl) standing in front of the same kind of real subject. Both wrap the identical RateQuoter interface; only the intent differs." %}

Every class in `examples/16-decorator-proxy` implements one interface:

```java
public interface RateQuoter {
    RateQuote quote(Shipment shipment);
}
```

`LiveCarrierGateway` is the core at the bottom of every decorator stack and the real subject behind both proxies. It computes a per-zone, per-kilogram rate. Its instance counter stands in for the cost of opening a carrier session:

```java
public final class LiveCarrierGateway implements RateQuoter {
    private static final AtomicInteger INSTANCES_CREATED = new AtomicInteger(0);
    private final String carrierName;

    public LiveCarrierGateway(String carrierName) {
        this.carrierName = Objects.requireNonNull(carrierName, "carrierName");
        INSTANCES_CREATED.incrementAndGet();
    }

    public static int instancesCreated() {
        return INSTANCES_CREATED.get();
    }

    @Override
    public RateQuote quote(Shipment shipment) {
        long cents = ShippingMath.baseRateCents(shipment.zone(), shipment.weightKg());
        return new RateQuote(carrierName, Money.ofCents(cents));
    }
}
```

## Pattern: Decorator

### The problem

Surcharge, caching and logging all happen around the quote. Put them inside `LiveCarrierGateway` and every new concern means editing the same method again, with no way to reorder or drop one per caller.

### The solution

A **Decorator** attaches a responsibility to an object by wrapping it in another object with the same interface, so wrappers can be stacked in any combination.

Use a Decorator when:

- several optional concerns wrap the same call;
- callers need different combinations or orders of those concerns;
- a concern changes on a different schedule from the core.

### How Java models it

Each decorator takes a `RateQuoter` in its constructor, holds it in a `final` field and adds one responsibility. `LoggingRateQuoter` appends to a call log and forwards:

```java
public final class LoggingRateQuoter implements RateQuoter {
    private final RateQuoter inner;
    private final List<String> callLog;

    public LoggingRateQuoter(RateQuoter inner, List<String> callLog) {
        this.inner = Objects.requireNonNull(inner, "inner");
        this.callLog = Objects.requireNonNull(callLog, "callLog");
    }

    @Override
    public RateQuote quote(Shipment shipment) {
        RateQuote result = inner.quote(shipment);
        callLog.add("quoted " + shipment.zone() + " shipment at " + result.amount());
        return result;
    }
}
```

`CachingRateQuoter` keeps a `ConcurrentHashMap<Shipment, RateQuote>` and skips the wrapped quoter on a repeat shipment. `Shipment` is a record, so its generated `equals` and `hashCode` make it the cache key with no extra code. `SurchargeRateQuoter` adds a markup proportional to declared value, with a setter for the rate.

A stack is nested construction, innermost first:

```java
LoggingRateQuoter logging = new LoggingRateQuoter(new LiveCarrierGateway("ground"), callLog);
CachingRateQuoter caching = new CachingRateQuoter(logging);
SurchargeRateQuoter surcharge = new SurchargeRateQuoter(caching, 1.5);
```

Checkout calls `surcharge.quote(shipment)` and sees one `RateQuoter`.

### Stacking order changes the result

Each layer sees only what happens closer to the core than it is. In the stack above, caching sits inside the surcharge:

| Second quote for the same shipment, surcharge rate raised from 1.5 to 3.0 | Surcharge outside caching | Caching outside surcharge |
| --- | --- | --- |
| What the cache holds | Base price | Surcharged total |
| Second total | `$11.00`, the new rate applies | `$9.20`, the old total is replayed |
| Log lines (logging wraps the core) | 1 | 1 |

The classes and constructor arguments are identical in both columns. Deciding where a decorator goes decides what information reaches it.

{% include excalidraw.html name="16-decorator-stacking-order" caption="Figure 16.2: the same two decorator classes, nested in opposite order around the identical core. Surcharge outside Caching recomputes on every call, so raising the rate between two lookups changes the second total; Caching outside Surcharge memoizes the already-surcharged amount, so the same rate change has no effect until the shipment itself changes." %}

### A decorator without a class file

`RateQuoter` has one abstract method, so it's a functional interface. `RateQuoters` satisfies the same surcharge contract with a lambda, and both forms return equal `RateQuote` values for the same `Shipment`:

{% include codetabs.html langs="Named class|Functional decoration" %}

```java
// Named class: a dedicated file, a constructor, a field, an override,
// before the surcharge math runs.
public final class SurchargeRateQuoter implements RateQuoter {
    private final RateQuoter inner;
    private volatile double centsPerDeclaredDollar;

    public SurchargeRateQuoter(RateQuoter inner, double centsPerDeclaredDollar) {
        this.inner = Objects.requireNonNull(inner, "inner");
        this.centsPerDeclaredDollar = centsPerDeclaredDollar;
    }

    @Override
    public RateQuote quote(Shipment shipment) {
        RateQuote base = inner.quote(shipment);
        long surchargeCents = ShippingMath.insuranceSurchargeCents(
                shipment.declaredValueCents(), centsPerDeclaredDollar);
        return new RateQuote(base.quotedBy(), base.amount().plus(Money.ofCents(surchargeCents)));
    }
}
```
```java
// Functional decoration: the identical contract, satisfied by one
// expression that closes over the wrapped quoter and the surcharge rate.
public static RateQuoter surcharging(RateQuoter inner, double centsPerDeclaredDollar) {
    return shipment -> {
        RateQuote base = inner.quote(shipment);
        long surchargeCents = ShippingMath.insuranceSurchargeCents(
                shipment.declaredValueCents(), centsPerDeclaredDollar);
        return new RateQuote(base.quotedBy(), base.amount().plus(Money.ofCents(surchargeCents)));
    };
}
```

| Form | Use it when |
| --- | --- |
| Lambda | The decorator is built, used and discarded at one call site that owns the values it closes over. |
| Named class | The decorator carries state worth inspecting, as `CachingRateQuoter` exposes `hitCount()` and `missCount()`. |

## Pattern: Proxy

### The problem

Constructing `LiveCarrierGateway` opens a live session, and a caller should pay that cost only if it quotes. Separately, a guest caller must not reach the gateway at all. Neither concern touches the pricing logic, and callers shouldn't change to accommodate either.

### The solution

A **Proxy** is a surrogate with the subject's own interface that controls access to the subject, deferring its creation or refusing the call, while a successful call returns what the subject would.

Use a Proxy when:

- the subject is expensive to create and may never be used;
- some callers must be refused before the call reaches the subject;
- the same check applies across many methods or interfaces.

### How Java models it

`LazyCarrierGatewayProxy` builds the real gateway on the first `quote()` and forwards every call to that one instance. The `volatile` field and double-checked lock make the construction happen once under concurrent first calls:

```java
public final class LazyCarrierGatewayProxy implements RateQuoter {
    private final String carrierName;
    private volatile LiveCarrierGateway real;

    public LazyCarrierGatewayProxy(String carrierName) {
        this.carrierName = Objects.requireNonNull(carrierName, "carrierName");
    }

    public boolean isLoaded() {
        return real != null;
    }

    @Override
    public RateQuote quote(Shipment shipment) {
        if (real == null) {
            synchronized (this) {
                if (real == null) {
                    real = new LiveCarrierGateway(carrierName);
                }
            }
        }
        return real.quote(shipment);
    }
}
```

### A dynamic proxy with java.lang.reflect.Proxy

Java can generate the proxy class at runtime from the interface. `RateQuoterAccessControl.guarded` passes `Proxy.newProxyInstance` a class loader, the interfaces to implement and an `InvocationHandler` that receives every call:

```java
public static RateQuoter guarded(RateQuoter real, ClearanceLevel callerLevel, ClearanceLevel requiredLevel) {
    GuardedInvocationHandler handler = new GuardedInvocationHandler(real, callerLevel, requiredLevel);
    return (RateQuoter) Proxy.newProxyInstance(
            RateQuoterAccessControl.class.getClassLoader(),
            new Class<?>[] {RateQuoter.class},
            handler);
}

private static final class GuardedInvocationHandler implements InvocationHandler {
    private final RateQuoter real;
    private final ClearanceLevel callerLevel;
    private final ClearanceLevel requiredLevel;
    private final AtomicInteger calls = new AtomicInteger();

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
}
```

No `RateQuoter` implementation exists in the source; every call routes through `invoke()`. `ClearanceLevel` is ordered `GUEST`, `WAREHOUSE`, `ADMIN`. A cleared caller gets exactly what the wrapped quoter returns, and an under-cleared caller gets a `SecurityException` before the real `quote()` runs. The handler's `AtomicInteger` counts every intercepted call whatever the outcome, bookkeeping that a dynamic proxy keeps in one place for any interface.

### Decorator or Proxy

| | Decorator | Proxy |
| --- | --- | --- |
| Intent | Adds behavior to the call | Controls whether or when the call reaches the subject |
| Stacking | Designed to stack, in an order the caller chooses | Usually one, in front of one subject |
| Successful result | May change (`SurchargeRateQuoter` adds a markup) | Unchanged |
| Classes | `LoggingRateQuoter`, `CachingRateQuoter`, `SurchargeRateQuoter` | `LazyCarrierGatewayProxy`, `RateQuoterAccessControl` |

`CachingRateQuoter` sounds like a caching proxy, but it's a Decorator: it composes with other layers and its position in the stack changes what callers see.

## When not to use it

- **Decorator.** A `RateQuoter` with one concern needs no wrapper; call `LiveCarrierGateway.quote()` directly. Add `SurchargeRateQuoter` when the surcharge rule has to change on its own schedule. A decorator nothing else stacks beside adds an override and a debugger hop for flexibility nobody uses.
- **Proxy.** An object that's cheap to build and open to every caller should be constructed where it's used. Don't add `LazyCarrierGatewayProxy` before construction cost is measured, or `RateQuoterAccessControl` before a second caller with lesser clearance exists.
- **The wrong one.** A cache meant to compose with other concerns belongs in a decorator stack. A wrapper whose only job is to gate whether an expensive call happens is a proxy.

> **🤖 Explore with your agent**
>
> "In `examples/16-decorator-proxy`, add caching as `CachingRateQuoter implements RateQuoter`, wrapping a `RateQuoter` held in a final field and keyed by the `Shipment` record in a `ConcurrentHashMap`. Don't change `LiveCarrierGateway`. Assemble the stack as `SurchargeRateQuoter(CachingRateQuoter(LoggingRateQuoter(LiveCarrierGateway)))` so a change to the surcharge rate applies on the next quote of a cached shipment. Put access control in a separate `InvocationHandler` built with `Proxy.newProxyInstance`, not an `if` inside the real class."
>
> Check: `mvn -q -f examples/16-decorator-proxy/pom.xml test -Dtest='DecoratorOrderingTest,CachingRateQuoterTest,RateQuoterAccessControlTest'`; reject a cache field inside `LiveCarrierGateway`.

## Build, run, observe

From the repository root, the aggregator builds every example:

```sh
mvn -f examples/pom.xml test
```

Or build this one on its own:

```sh
cd examples/16-decorator-proxy
mvn test
```

All 22 tests pass. The demo prices the same 2 kilogram Regional shipment through the stack in both orders, raising the surcharge rate between calls, then runs both proxies:

```sh
examples/16-decorator-proxy/demo.sh
```

```
== Decorator: surcharge wraps caching wraps logging wraps core ==
First quote (rate 1.5 cents/declared dollar): $9.20
Second quote, rate raised to 3.0 (base still cached): $11.00
Cache misses: 1, cache hits: 1, log lines: 1
The surcharge recomputes fresh on every call, so raising the rate changes the total even though the base rate underneath it only computed once.

== Decorator: reordering it to caching wraps surcharge wraps logging wraps core ==
First quote (rate 1.5 cents/declared dollar): $9.20
Second quote, rate raised to 3.0 (total now cached): $9.20
Cache misses: 1, cache hits: 1, log lines: 1
Caching now sits outside the surcharge, so the already-surcharged total is what gets memoized; raising the rate has no effect until the shipment key changes.

== Proxy: lazy initialization defers the real carrier gateway ==
Real gateway constructed before first quote? false
Quote: RateQuote[quotedBy=air-flat, amount=$7.40]
Real gateway constructed after first quote?  true

== Proxy: java.lang.reflect.Proxy gates access by clearance ==
Warehouse caller: RateQuote[quotedBy=freight-zone, amount=$7.40]
Guest caller denied: access denied: rate lookup requires WAREHOUSE clearance, caller has GUEST
Calls intercepted by the warehouse proxy: 1
```

- **Make the lazy proxy eager.** In a scratch copy, construct `real` in the `LazyCarrierGatewayProxy` constructor. One proxy test fails: the real gateway now exists before the first quote.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original Decorator and Proxy entries, including the transparency requirement both Java expressions satisfy.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 18 contrasts inheritance with composition, the structural choice Decorator makes explicit by wrapping rather than extending.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/), [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html), and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/16-decorator-proxy`, which verifies a mocked collaborator is forwarded to, skipped, or never reached.
- The full reading list lives on the [bibliography page]({{ '/bibliography/' | relative_url }}).
