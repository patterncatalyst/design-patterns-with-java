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

- Name the force both patterns answer: an object wrapped behind the exact
  interface it already implements, so a caller holding that interface
  cannot tell how many wrappers, if any, sit between it and the real thing.
- Build a Decorator stack where each layer adds one responsibility, and
  show that the order the layers wrap in changes the result, not just the
  order a log prints lines in.
- Write the same decorator as a named class and as a lambda assigned
  directly to the shared interface, and say when each form is the better
  fit.
- Build a hand-written Proxy that defers constructing an expensive real
  subject until the first call reaches it, and a dynamic proxy built with
  `java.lang.reflect.Proxy` and an `InvocationHandler` that gates access on
  a policy check.
- Tell Decorator and Proxy apart by intent even though both wrap the same
  interface they stand in front of: one adds behavior and is meant to
  stack, the other controls access or timing and is meant to be
  transparent.
- Recognize when either pattern is wrapping an object that has nothing to
  wrap it for, and say what to write instead.

## Why this matters

A checkout pipeline that quotes a shipping rate rarely wants exactly one
thing to happen when it asks for a quote. It wants the quote computed, and
it wants a surcharge added for high-value shipments, and it wants the
computation cached so a cart priced three times in one session does not
hit the tariff lookup three times, and it wants every quote logged for the
next billing dispute. None of those four concerns is the quote itself.
Each one is something that happens around the quote, and each one is the
kind of thing a team ships on its own schedule: the surcharge rule changes
when finance renegotiates a contract, the cache gets added once load
testing shows the tariff lookup is hot, and the logging gets added when
support asks for an audit trail. Decorator is the Gang of Four's name for
building a pipeline like that out of layers that each implement the same
interface the thing they wrap implements, so any combination of them, in
any order a caller chooses to assemble, still looks like one quote source
to whatever code asks it for a price.

A warehouse scanning app has a different problem with the same shape. It
needs to look up a carrier's rate through a gateway that is expensive to
open, a real session against a live tariff API, before the first lookup
can even run. The app also has more than one kind of caller hitting that
lookup, and a guest account has no business reaching a gateway that a
warehouse worker's badge unlocks. Neither problem is about the gateway's
own pricing logic. One is about when the real gateway gets built, and the
other is about whether a particular caller is allowed to reach it at all.
Proxy is the Gang of Four's name for resolving that force: a surrogate
that implements the exact interface the real subject implements, so a
caller cannot distinguish it from the real thing except by the one
behavior the proxy exists to change, construction timing or access
control, never what a successful call returns.

Both patterns wrap an object behind its own interface. That similarity is
exactly why this chapter pairs them: the test for telling them apart is
not what either one looks like in code, which can be nearly identical, but
what each one is for.

{% include excalidraw.html name="16-decorator-proxy" caption="Figure 16.1: a Decorator stack (surcharge, caching, logging) wrapping a core LiveCarrierGateway, next to a Proxy (RateQuoterAccessControl) standing in front of the same kind of real subject. Both wrap the identical RateQuoter interface; only the intent differs." %}

## The domain: a shared RateQuoter contract

The example behind this chapter lives in `examples/16-decorator-proxy`, and
every class in it implements one interface:

```java
public interface RateQuoter {
    RateQuote quote(Shipment shipment);
}
```

`LiveCarrierGateway` is the core component at the bottom of every decorator
stack in this example, and it is also the real subject both proxies stand
in front of. It computes a plain per-zone, per-kilogram rate with no
decoration and no access control of its own:

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

`instancesCreated()` stands in for the cost of opening a real carrier
session: in production, constructing this object means a network round
trip against a live tariff API, not a cheap field assignment. Tracking it
as a counter is what lets this chapter's test suite prove a proxy
deferred that cost rather than just asserting a lookup returned the right
number. A caller holding a `RateQuoter` reference, whether it points at
this core directly, at a stack of decorators three layers deep, or at
either proxy in this chapter, calls `quote()` the same way and cannot tell
from the interface alone which one it is talking to.

## Decorator: three layers, one interface, order that matters

Three decorators wrap a `RateQuoter` in this example, and each one adds
exactly one responsibility. `LoggingRateQuoter` appends a line to a shared
call log every time `quote()` runs, then forwards unchanged:

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

`CachingRateQuoter` remembers a quote by `Shipment` and skips the wrapped
`RateQuoter` entirely on a repeat lookup for an identical shipment, using
`Shipment`'s generated record equality as the cache key with no extra code
anywhere. `SurchargeRateQuoter` adds a markup proportional to declared
value on top of whatever the wrapped quoter returns, and its rate is
mutable through a setter so a merchant can adjust it without rebuilding
the pipeline. All three constructors take a `RateQuoter` and hold it in a
`final` field, the same way `ShippingRateCalculator` held a single
`RateStrategy`; here, each decorator holds exactly the one layer directly
beneath it.

Composing three decorators around a core looks like nested construction,
innermost first:

```java
LoggingRateQuoter logging = new LoggingRateQuoter(new LiveCarrierGateway("ground"), callLog);
CachingRateQuoter caching = new CachingRateQuoter(logging);
SurchargeRateQuoter surcharge = new SurchargeRateQuoter(caching, 1.5);
```

Reading that from the inside out names the stack: a `LiveCarrierGateway`
wrapped in a `LoggingRateQuoter`, wrapped in a `CachingRateQuoter`, wrapped
in a `SurchargeRateQuoter`. `surcharge.quote(shipment)` is the only call a
checkout flow makes, and it cannot tell from that call alone that three
layers sit underneath it rather than zero.

## Order is part of the design, not an implementation detail

The stack above puts caching inside the surcharge: `SurchargeRateQuoter`
wraps `CachingRateQuoter`, which wraps `LoggingRateQuoter`, which wraps the
core. That ordering has a real consequence. A cache hit inside
`CachingRateQuoter` returns a memoized base price without ever calling the
`LoggingRateQuoter` beneath it, so a repeat lookup for the same shipment
produces no new log line. The surcharge, sitting outside the cache, still
runs on every single call, because `SurchargeRateQuoter.quote()` always
calls its wrapped `RateQuoter` and adds a fresh markup on top of whatever
comes back. Raise the surcharge rate between two calls for the identical
shipment and the second call's total changes immediately, even though the
base rate underneath it only computed once.

Swap the two decorators' positions, caching outside the surcharge instead
of inside it, and the same rate change behaves differently. Now the cache
memoizes the already-surcharged total, not the base price, so a later
change to the surcharge rate has no effect on a repeat lookup until the
shipment itself changes. Nothing else in the stack moved. The two
decorators involved are the identical classes, constructed with the
identical arguments; only the order they wrap in changed, and that order
alone decided whether a live rate change shows up on the next quote or
goes unnoticed until a different shipment comes through.
`DecoratorOrderingTest` in this chapter's example asserts both outcomes
side by side from one shipment, confirming the order dependency is a
property of the design and not an artifact of one particular test run.

This is the fact about Decorator worth carrying forward past this
chapter's specific rate pipeline: stacking order changes what each layer
sees, not merely what order some log lines print in. A layer only
observes what happens on the side of the stack closer to the core than it
is; anything further out is invisible to it. Deciding where a decorator
goes in a stack is deciding what information reaches it, which is a
design decision with observable consequences, not a stylistic choice a
reviewer can wave past.

{% include excalidraw.html name="16-decorator-stacking-order" caption="Figure 16.2: the same two decorator classes, nested in opposite order around the identical core. Surcharge outside Caching recomputes on every call, so raising the rate between two lookups changes the second total; Caching outside Surcharge memoizes the already-surcharged amount, so the same rate change has no effect until the shipment itself changes." %}

## Java 25: the same decorators without a class file

`RateQuoter` declares exactly one abstract method, which makes it a
functional interface whether or not anyone marks it with the annotation.
`RateQuoters` satisfies the identical surcharge and logging contracts with
lambdas instead of named classes, and nothing about the `RateQuoter` type
a caller depends on changes:

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

`RateQuotersFunctionalTest` proves the two columns produce byte-for-byte
identical `RateQuote` values for the same `Shipment`, rather than assuming
it. Reach for the lambda form when a decorator is built, used once, and
discarded at a call site that owns the values it closes over. The named
class fits better when a decorator carries enough internal state that
giving it a name and a file clarifies the code, the way
`CachingRateQuoter` exposes `hitCount()` and `missCount()` for a caller
that wants to inspect the cache's behavior, something a lambda has no
place to hang a method off of.

## Proxy: the same interface, a different intent

`LazyCarrierGatewayProxy` implements `RateQuoter` without constructing a
`LiveCarrierGateway` up front:

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

Its `quote()` constructs the real gateway on the first call it ever
receives, then forwards every call after that to the same instance. A
checkout flow holding a `RateQuoter` cannot tell the difference between
this proxy and a real gateway that happened to construct instantly, which
is the entire point: the proxy's one job is to make the expensive
construction invisible to the caller, deferring it until a lookup demands
it rather than paying for it whether a lookup ever happens or not.

## A dynamic proxy with java.lang.reflect.Proxy

A hand-written proxy class works, but Java can generate one at runtime
from the interface alone. `RateQuoterAccessControl` builds a `RateQuoter`
proxy with `Proxy.newProxyInstance` and an `InvocationHandler` that gates
every call on a caller's clearance before forwarding it:

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

`Proxy.newProxyInstance` takes a class loader, the array of interfaces the
generated proxy must implement, and the `InvocationHandler` that receives
every method call made on it. There is no `RateQuoter` implementation
written anywhere in this file; the JVM generates one at class-load time,
and every call a caller makes against it routes through `invoke()`
instead of a hand-written method body. `RateQuoterAccessControlTest`
verifies this proxy behaves exactly like `LazyCarrierGatewayProxy` in one
respect and differently in another: a caller with sufficient clearance
gets back precisely what the wrapped `RateQuoter` would have returned on
its own, and an under-cleared caller's call never reaches the real
`quote()` method at all, confirmed with a mock that Mockito's `verify`
checks was never invoked. The handler's own `AtomicInteger` counts every
call it intercepts regardless of outcome, which is the kind of
cross-cutting bookkeeping a dynamic proxy centralizes in one place rather
than scattering across however many interfaces a hand-written proxy would
need to implement by hand.

The hand-written `LazyCarrierGatewayProxy` and the dynamic
`RateQuoterAccessControl` solve different problems behind the identical
interface, and neither one reformats a `RateQuote`, adds a field to it, or
changes what a successful call returns in any way. A caller with
sufficient clearance gets back exactly what the wrapped `RateQuoter` would
have returned on its own. That is the line between this chapter's two
patterns, drawn as precisely as the forces allow: a Decorator adds new
behavior to what a call returns and is designed to be stacked, several of
them, in whatever order a caller assembles. A Proxy controls whether a
call reaches its subject at all, construction deferred or access denied,
and is designed to be transparent, leaving a successful call's result
untouched. `CachingRateQuoter` from the Decorator half of this chapter
looks deceptively close to a caching Proxy by name alone, but by that same
test it is a Decorator: it changes what a caller experiences, specifically
how fast a repeat call returns and whether the call reaches the layer
underneath, in a way meant to compose with other layers, not to gate a
single access decision.

> **AI sidebar.** Ask an LLM pairing partner to "add caching" to an object
> behind an interface, and a common result either wraps the object in a
> new class implementing that same interface, this chapter's Decorator
> shape, or adds a cache field and an early-return branch directly inside
> the object's own method body. Both compile and both can pass a quick
> test. The difference shows up the moment a second concern needs to be
> layered on, a surcharge, a logging line, an access check: the wrapped
> version composes by constructing one more layer with no change to any
> existing class, while the branch-inside-the-method version means editing
> that one method again, and again, every time a new concern needs to sit
> somewhere in the sequence. Check which shape a generated change took
> before accepting it. A cache field growing inside `LiveCarrierGateway`
> itself is the sign the model solved today's request without leaving room
> for tomorrow's. The same scrutiny applies to a generated
> `java.lang.reflect.Proxy`: a model asked for "access control" sometimes
> reaches for an `if` statement bolted onto the real class instead of a
> separate `InvocationHandler`, which works until a second kind of access
> rule needs to apply to a different interface and there is nowhere to put
> it that does not touch the real subject's own code.

## When not to use Decorator or Proxy

Both patterns earn their wrapper when there is a real reason to keep a
responsibility separate from the object it attaches to, or a real reason a
caller must not reach a subject directly. Outside that case, each one adds
an interface implementation with nothing behind it to justify the cost.

A `RateQuoter` with exactly one concern, no surcharge, no cache, no log,
does not need to be wrapped at all. Calling `LiveCarrierGateway.quote()`
directly from checkout code is the plainer non-pattern, and it stays
correct for as long as nothing needs to vary independently around that one
call. The moment to introduce `SurchargeRateQuoter` is the moment the
surcharge rule itself needs to change on its own schedule, separate from
the tariff lookup, not before. Wrapping a single-concern object in a
decorator that nothing else ever stacks alongside it adds one more
`quote()` override and one more hop a debugger has to step through, for a
flexibility nobody is using yet.

Proxy fails the same test in a narrower way. An object that is cheap to
construct and reachable by every caller with no access distinction between
them does not need a proxy standing in front of it; it needs to be
constructed directly where it is used. Introducing `LazyCarrierGatewayProxy`
ahead of any evidence that `LiveCarrierGateway` construction is expensive
enough to matter defends against a cost that has not been measured, and
introducing `RateQuoterAccessControl` ahead of a real second caller with
lesser clearance defends against a distinction that does not exist in the
system yet. The sharper trap is building a caching Proxy when what the
problem calls for is a caching Decorator, the distinction this chapter
drew directly: a cache that is meant to compose with other concerns on the
same object belongs in a stack a caller assembles, while a cache whose
entire job is gating whether an expensive call happens at all, with no
other concern sharing that same wrapped object, is closer to what Proxy's
intent describes. Name which force is in play, composing behavior or
controlling access, before reaching for either pattern's shape.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this
loop. From the repository root, the aggregator picks up
`examples/16-decorator-proxy` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/16-decorator-proxy
mvn test
```

Either path runs the same 22 JUnit 5 cases, asserted with AssertJ and
Mockito, including `DecoratorOrderingTest`'s pair of cases that reorder the
identical decorators and assert the opposite outcome, and
`RateQuoterAccessControlTest`'s case that asserts a denied caller's call
never reaches the real subject. Observed locally with Temurin 25:

```
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see the decorator stack and both proxies produce real output rather
than just reading an assertion that says they work, run the example's own
demo script:

```sh
examples/16-decorator-proxy/demo.sh
```

It prices the same 2 kilogram, Regional-zone shipment through the
decorator stack twice, once with the surcharge outside the cache and once
with the two swapped, raising the surcharge rate between calls both times
so the difference in behavior is visible in the printed totals, then
exercises both proxies against the same kind of real subject:

```
== Decorator: surcharge wraps caching wraps logging wraps core ==
First quote (rate 1.5 cents/declared dollar): $9.20
Second quote, rate raised to 3.0 (base still cached): $11.00
Cache misses: 1, cache hits: 1, log lines: 1

== Decorator: reordering it to caching wraps surcharge wraps logging wraps core ==
First quote (rate 1.5 cents/declared dollar): $9.20
Second quote, rate raised to 3.0 (total now cached): $9.20
Cache misses: 1, cache hits: 1, log lines: 1

== Proxy: lazy initialization defers the real carrier gateway ==
Real gateway constructed before first quote? false
Quote: RateQuote[quotedBy=air-flat, amount=$7.40]
Real gateway constructed after first quote?  true

== Proxy: java.lang.reflect.Proxy gates access by clearance ==
Warehouse caller: RateQuote[quotedBy=freight-zone, amount=$7.40]
Guest caller denied: access denied: rate lookup requires WAREHOUSE clearance, caller has GUEST
Calls intercepted by the warehouse proxy: 1
```

The first block's second total rises to `$11.00` because the surcharge
sits outside the cache and recomputes on every call; the second block's
second total stays at `$9.20` because the cache now sits outside the
surcharge and memoized the already-surcharged amount. Same two decorator
classes, same constructor arguments, opposite result, decided entirely by
which one wraps which.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original Decorator and Proxy entries, including the transparency requirement both of this chapter's Java expressions satisfy.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 18 contrasts inheritance with composition, the structural choice Decorator makes explicit by wrapping rather than extending.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/), [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html), and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/16-decorator-proxy`'s suite, which verifies a mocked collaborator is forwarded to, skipped, or never reached, not just that the build exits zero.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
