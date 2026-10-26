---
title: "Observer and Mediator"
order: 18
layout: tutorial
sectionid: docs
part: "coordinating-traversing"
description: "Two coordination patterns for a many-party system, taught against a shipment tracker and a warehouse dispatch desk: Observer fans a status change out to independently subscribed listeners behind one functional interface, and Mediator centralizes a many-to-many conversation behind a single hub so a warehouse, a carrier, and a notification service never reference each other directly."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Name the force each pattern answers: Observer frees a subject from
  knowing who is listening to it; Mediator frees a set of peers from
  knowing about each other at all.
- Build an Observer subject that fans a status change out to independently
  subscribed listeners through one functional interface, from a named class
  down to a bare lambda, and say when the lambda form is enough on its own.
- Manage a subscription with an `AutoCloseable` handle so try-with-resources
  closes it, and explain why a snapshot copy of the listener list matters
  during notification.
- Build a Mediator that routes events between a warehouse, a carrier, and a
  customer-notification station, none of which ever references another
  directly.
- Tell the two patterns apart by the shape of the communication each one
  centralizes: Observer's one-to-many fan-out versus Mediator's
  many-to-many routing through a single hub.
- Recognize when either pattern is coordinating collaborators that have no
  real reason to avoid knowing about each other, and say what to write
  instead.

## Why this matters

A shipment's status changes many times between the moment a warehouse packs
it and the moment it lands on a doorstep, and more than one part of a
shipping system cares every time it does. A support dashboard wants the
latest status to show up the instant it changes. An alerting service wants
to know the moment a shipment flips into an exception state, and nothing
before that. A billing dispute, months later, wants a durable record of
every transition a specific tracking number went through. None of those
three concerns is the status change itself; each one is something that
happens in response to it, added independently of the others and on its own
schedule. Observer is the Gang of Four's name for exactly this force: a
subject broadcasts one fact, that its state changed, to an open set of
listeners it never has to enumerate by name, import, or even know exist.

A warehouse dispatch desk has a related problem with a different shape. A
warehouse stages an order and hands it to a carrier; the carrier picks it
up, moves it through delivery, and occasionally reports a problem; a
customer-notification service has to hear about every one of those events
and turn it into a message a customer reads. If the warehouse called the
carrier's method directly, and the carrier called the notification
service's method directly, adding a fourth collaborator, a
returns-processing stage that needs to hear about delivery exceptions too,
would mean editing every existing class that stage needs to hear from. The
number of possible direct wires between components grows far faster than
the number of components. Mediator is the Gang of Four's name for breaking
that mesh: no warehouse, carrier, or notification component ever calls
another one's method, and every one of them talks only to a single
coordinator standing between them. The coordinator is the one place in the
system allowed to know who everyone is and what each of them is for.

Both patterns loosen a connection that would otherwise bind two parts of a
system directly to each other, but they loosen it along different axes.
Observer frees a subject from needing to know who is listening to it, and
nothing about a listener's identity ever reaches the subject's own code.
Mediator frees a set of peers from knowing about each other at all, and
pays for that with a hub that does know about every one of them. That
distinction, not how similar their code looks, is what separates the two
patterns.

{% include excalidraw.html name="18-observer-mediator" caption="Figure 18.1: A ShipmentTracker subject fanning a status change out to independent listeners, next to a DispatchCoordinator mediator routing between three collaborators that never call each other directly." %}

## The domain: a shipment tracker and a dispatch desk

The example behind this chapter lives in `examples/18-observer-mediator`;
its `README.md` covers what each class does, and `demo.sh` builds and runs
it. Both halves of the example share one small enum, a shipment's lifecycle
from creation to delivery:

```java
public enum ShipmentStatus {
    CREATED,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    EXCEPTION;

    public boolean isTerminal() {
        return this == DELIVERED || this == EXCEPTION;
    }
}
```

The Observer half is `ShipmentTracker`, the subject that owns a shipment's
current status and notifies whoever has subscribed every time that status
moves. The Mediator half is `DispatchCoordinator`, which sits between three
independent stations, `WarehouseStation`, `CarrierStation`, and
`CustomerNotificationStation`, and decides which one responds to which
event. Nothing in either half of the example reuses a type from the other;
they are two separate forces, taught against the same shipping operation
because both show up in the same system at once, not because one calls into
the other.

## Observer: a subject that does not know who is listening

`ShipmentTracker` holds one piece of state, a `ShipmentStatus`, and a
collection of subscribed listeners. What a listener is, underneath, is a
single-method functional interface:

```java
@FunctionalInterface
public interface ShipmentStatusListener {
    void onStatusChanged(String trackingId, ShipmentStatus oldStatus, ShipmentStatus newStatus);
}
```

`subscribe(ShipmentStatusListener)` stores one of these and returns a
`Subscription`; `advanceStatus()` moves the tracked status forward and calls
every currently subscribed listener with the tracking ID, the old status,
and the new one, in subscription order. A subject built this way has no
class hierarchy to maintain and no base type a listener must extend to
participate. Anything satisfying the one-method shape, a named class, a
method reference, or a lambda with captured state, is a valid listener.

`DashboardView` and `AlertingService` are named classes that implement
`ShipmentStatusListener`. Each earns the ceremony of a class because each
one is a long-lived collaborator something else holds onto and queries
later: `DashboardView.currentStatusLine()` reads the latest status a
different part of the program displays, and `AlertingService.alerts()`
accumulates a list another component reads after the fact. Neither is a
one-shot reaction; both are collaborators with state and an identity of
their own, which is exactly the case a named class is for.

```java
public final class DashboardView implements ShipmentStatusListener {
    private volatile String currentStatusLine = "no shipment tracked yet";

    @Override
    public void onStatusChanged(String trackingId, ShipmentStatus oldStatus, ShipmentStatus newStatus) {
        currentStatusLine = trackingId + ": " + newStatus;
    }

    public String currentStatusLine() {
        return currentStatusLine;
    }
}
```

## When a lambda is the whole story

A caller with nothing but a reaction and no state to expose does not need a
named class at all. This example's audit log subscribes directly, with no
class anywhere:

```java
List<String> auditEntries = new ArrayList<>();
Subscription auditSubscription = tracker.subscribe(
        (trackingId, oldStatus, newStatus) ->
                auditEntries.add(trackingId + ": " + oldStatus + " -> " + newStatus));
```

That lambda is the listener the task of logging every transition needs:
one closure, capturing the list it appends to, with no class file and
nothing for a reader to look up in a separate package. A language without
first-class functions forces every listener through an interface
implementation no matter how small its job is, which is why the textbook
Observer pattern is drawn as a class diagram in the first place. Java's
functional interfaces remove that constraint for any contract with exactly
one abstract method, so reach for a named class like `DashboardView` only
when a listener earns a name of its own: a long-lived object a different
part of the program holds onto, queries, or reuses across more than one
subscription. A lambda that only ever runs in reaction to a notification
and keeps no identity of its own, like the audit log, does not need that
ceremony.

## Closing the dangling-listener gap with `AutoCloseable`

The classic hazard in every Observer implementation is a listener that
stays subscribed after whatever it belonged to has stopped caring, quietly
running reactions nobody reads anymore. This example closes that hole with
a `Subscription` handle rather than a separate `unsubscribe()` call a
caller has to remember to make on every exit path, including an exception:

```java
public interface Subscription extends AutoCloseable {
    @Override
    void close();
}
```

`ShipmentTracker.subscribe()` returns a `Subscription` whose `close()`
removes the listener from the tracker's map, which makes a subscription a
natural fit for try-with-resources:

```java
AtomicInteger callCount = new AtomicInteger();
try (Subscription ignored = thirdTracker.subscribe(
        (trackingId, oldStatus, newStatus) -> callCount.incrementAndGet())) {
    thirdTracker.advanceStatus(ShipmentStatus.PICKED_UP);
}
thirdTracker.advanceStatus(ShipmentStatus.IN_TRANSIT);
```

The listener only counts the one status change raised inside the block;
the second `advanceStatus()` call runs after the try-with-resources block
already closed the subscription, so nothing is listening to hear about it.
`Subscription` narrows `AutoCloseable#close()` to declare no checked
exception, the same idiom a handful of narrower resource types in the JDK
follow: closing a subscription cannot fail, so forcing every caller to
catch or declare `Exception` would be ceremony with nothing behind it.

`advanceStatus()` guards against one more hazard. A listener is free to
unsubscribe itself, or trigger an unsubscribe of a different listener, from
inside its own callback, so notification runs over a snapshot copy of the
listener list rather than the live one:

```java
List<ShipmentStatusListener> snapshot = new ArrayList<>(listeners.values());
for (ShipmentStatusListener listener : snapshot) {
    listener.onStatusChanged(trackingId, oldStatus, newStatus);
}
```

Without that copy, a listener unsubscribing mid-notification would mutate
the very map this loop is iterating and raise a
`ConcurrentModificationException`. Copying a handful of listener references
once per status change is a small, fixed cost against a failure that is
hard to reproduce, showing up only when a listener's own reaction changes
the subscription list it is being called from.

## Reaching for `java.util.concurrent.Flow` instead

A listener list is the right shape when a subject calls its listeners
directly, synchronously, on whatever thread raised the change, and expects
each one to finish quickly. Once a shipment tracker has to support backpressure,
asynchronous delivery, or a publisher feeding more than one kind of
subscriber at scale, `java.util.concurrent.Flow`'s `Publisher` and
`Subscriber` interfaces are the standard library's answer to the same
force, with a subscriber able to request only as many events as it can
currently process instead of being called whether it is ready or not. The
tradeoff runs the other way too: `Flow` brings a request-based protocol and
subscription lifecycle a reader has to learn, which is pure overhead for a
subject with a handful of synchronous, fast listeners like `DashboardView`
and `AlertingService`. Reach for `ShipmentStatusListener`'s plain list
first, and move to `Flow` only once a listener's own processing time, or
its volume, makes a direct, unbounded call from the subject a real problem.

## Mediator: centralizing a many-to-many conversation

`DispatchColleague` is the shared base every station in the dispatch desk
extends. It holds no reference to `WarehouseStation`, no reference to
`CarrierStation`, and no reference to `CustomerNotificationStation`: the
only thing a colleague can reach is its own mediator.

```java
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
```

`WarehouseStation.markOrderReady()` calls
`raise(DispatchEvent.ORDER_READY_FOR_PICKUP, orderId)`, and that call has no
idea which station, if any, responds. Wiring a colleague to a mediator
happens once, in `DispatchCoordinator`'s constructor:

```java
public DispatchCoordinator(WarehouseStation warehouse, CarrierStation carrier,
        CustomerNotificationStation notifier) {
    this.warehouse = warehouse;
    this.carrier = carrier;
    this.notifier = notifier;
    warehouse.bindMediator(this);
    carrier.bindMediator(this);
    notifier.bindMediator(this);
}
```

That constructor order matters for a reason specific to Mediator: every
colleague needs a mediator reference, but the coordinator needs live
colleague objects to construct itself from, which is a circular requirement
if both sides try to hold the other from the start. Binding after
construction, through a method only the coordinator is meant to call,
breaks the cycle: a `WarehouseStation` can exist with no mediator at all
until something wires it to one.

`bindMediator()` has no access modifier, which gives it package-private
visibility. Java has no `friend` declaration the way C++ does, but a
package-private method reachable only from classes in the same package, here
`DispatchCoordinator`, is the idiomatic substitute: every caller outside the
`dispatch` package, including the colleague subclasses themselves, sees a
type with no visible way to rewire its own mediator. The access modifier is
doing the encapsulation work a language-level `friend` keyword would do in
C++.

Routing itself is a single method on the coordinator, a switch expression
over the event with no branch that lets a colleague skip the hub:

```java
@Override
public void notify(DispatchColleague sender, DispatchEvent event, String orderId) {
    switch (event) {
        case ORDER_READY_FOR_PICKUP -> carrier.confirmPickup(orderId);
        case PICKED_UP_BY_CARRIER ->
            notifier.send(orderId, "Your package has been picked up and is on its way.");
        case OUT_FOR_DELIVERY ->
            notifier.send(orderId, "Your package is out for delivery today.");
        case DELIVERY_EXCEPTION ->
            notifier.send(orderId, "There is a delivery exception on your package; our team is investigating.");
    }
}
```

Calling `warehouse.markOrderReady("ORD-5001")` cascades through two hops the
warehouse itself never has to know about: `WarehouseStation` raises
`ORDER_READY_FOR_PICKUP`, the coordinator calls
`CarrierStation.confirmPickup()`, and that call itself raises
`PICKED_UP_BY_CARRIER`, which the same coordinator routes to
`CustomerNotificationStation.send()`. Three stations took part in one
customer-visible outcome, and at no point did any one of them hold a
reference to another. The switch expression over `DispatchEvent` is
exhaustive against the enum's four constants; adding a fifth event without a
matching case is a compile error, not a runtime surprise discovered on
whichever order reaches the missing branch first.

> **AI sidebar.** Ask an LLM pairing partner to wire together a handful of
> components that need to react to each other's events, and a common result
> has every component holding a direct field reference to every other
> component it needs to call, because that is the shape requiring the least
> new vocabulary to produce. It compiles, and for three stations it even
> reads clearly. Check what happens to that generated code the moment a
> fourth collaborator needs to hear about the same event: every existing
> class that needs to know about it gets a new field and a new call, and the
> number of wires between stations grows faster than the number of stations
> does. A `DispatchCoordinator`-style hub costs one extra hop per event and
> buys a system where adding a collaborator means teaching the hub about it
> once, not editing every peer that happens to care. A generated class
> holding references to three sibling classes it calls directly, with no
> coordinator anywhere in sight, is the clearest sign the model reached for
> the shape with the fewest moving parts today instead of the one that
> stays flat as the collaborator count grows.

## When not to use Observer or Mediator

Both patterns earn their cost when there is a real reason a subject must
not know who depends on it, or a real reason a set of peers must not know
about each other. Outside those two specific cases, each one adds
indirection with nothing underneath it to justify the cost.

A status change with exactly one piece of code that ever reacts to it does
not need `ShipmentTracker`'s subscription machinery at all; it needs a
direct method call at the point the status changes. Wrapping a single,
fixed reaction in a subscribe-and-notify pipeline before a second listener
is in hand or clearly coming adds a `Subscription` to manage and a reader's
detour through a functional interface to find the one call site that will
ever use it. The plainer alternative is calling the handler directly,
naming the pattern and the plain non-pattern side by side and letting the
forces, not the pattern's familiarity, decide.

Mediator fails a related test in a narrower way, and it fails it in both
directions. Three stations that only ever grow by one and never gain a
fourth collaborator do not need a hub standing between them; a plain,
visible sequence of calls, read start to finish by whoever maintains that
code, is the whole story, and a reviewer can confirm it is correct without
jumping to a coordinator class to see who talks to whom. The point where
that calculus flips is the point this chapter's dispatch desk crossed: more
than a handful of collaborators, different teams owning different ones, or
a routing rule that changes independently of any single collaborator's own
logic.

The other direction is just as real, and it shows up after a Mediator is
already in production rather than before. A coordinator that starts out
routing four event types between three stations can quietly absorb every
new rule the system ever needs, because adding one more `case` to its
switch always looks like the path of least resistance compared to touching
a station's own class. A `DispatchCoordinator` that has grown validation
logic, retry policy, and business rules specific to one station, on top of
its original routing job, has stopped being a hub and become a god object
wearing a Mediator's name. The fix is not to abandon the pattern, it is to
keep the coordinator's job narrow: deciding which station hears about which
event, and nothing about what a station does once it hears about it. A
station's own method, `CarrierStation.confirmPickup()`, is where that
station's logic belongs; the coordinator's `notify()` method is only ever
allowed to call it.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this
loop. From the repository root, the aggregator picks up
`examples/18-observer-mediator` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/18-observer-mediator
mvn test
```

Either path runs the same 11 JUnit 5 cases, asserted with AssertJ and
Mockito, including a suite that checks a listener is notified on a status
change, an unsubscribed listener is not, a self-unsubscribing listener does
not break the remaining fan-out, and `DispatchCoordinator` routes an event
to the correct station and nowhere else. Observed locally with Temurin
25.0.2:

```
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see the fan-out and the dispatch cascade produce real output rather than
just reading an assertion that says they work, run the example's own demo
script:

```sh
examples/18-observer-mediator/demo.sh
```

It subscribes a dashboard and an alerting service to a tracked shipment,
advances its status through an exception, subscribes a plain lambda as an
audit log on a second shipment, shows a subscription closing mid-session,
then runs an order through the dispatch coordinator end to end:

```
== Observer: named listeners fan out on a status change ==
Dashboard now reads: TRK-7001: IN_TRANSIT
Dashboard now reads: TRK-7001: EXCEPTION
Alerts raised: 1
  TRK-7001 flagged: IN_TRANSIT -> EXCEPTION

== Observer: a lambda needs no listener class at all ==
TRK-7002: CREATED -> PICKED_UP
TRK-7002: PICKED_UP -> IN_TRANSIT
TRK-7002: IN_TRANSIT -> DELIVERED

== Observer: closing a subscription stops notification mid-session ==
Callback ran 1 time(s); subscriber_count() is now 0

== Mediator: a warehouse event cascades through the coordinator ==
Carrier's own record of pickups: 1
Messages the coordinator routed to the customer:
  ORD-5001: Your package has been picked up and is on its way.
  ORD-5001: Your package is out for delivery today.
  ORD-5001: There is a delivery exception on your package; our team is investigating.
```

The alert line shows the exact transition that triggered it,
`IN_TRANSIT -> EXCEPTION`, confirming `AlertingService` only reacts to the
one transition it cares about and not to the `PICKED_UP` or `IN_TRANSIT`
changes that preceded it. The mediator block shows three messages reaching
the customer from a single `markOrderReady()` call plus two direct
`CarrierStation` calls, none of which required the warehouse, the carrier,
or the notification station to hold a reference to any of the others.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design
  Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480)
  (Addison-Wesley): the original Observer and Mediator entries, including
  the "one-to-many dependency" and "encapsulate how a set of objects
  interact" intent statements this chapter's tracker and dispatch desk each
  build on.
- The official [`java.util.concurrent.Flow`
  documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/Flow.html):
  the `Publisher`/`Subscriber`/`Subscription` protocol this chapter's
  listener list is contrasted against, including the request-based
  backpressure a plain listener list does not provide.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/)
  and [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html):
  the testing idiom behind `examples/18-observer-mediator`'s suite, which
  asserts fan-out order, post-unsubscribe silence, and correct event
  routing with a verified mock rather than only checking that each class
  constructs without error.
- The full reading list behind this book's opinions lives on the
  [bibliography page]({{ '/bibliography/' | relative_url }}); each citation
  there is a pointer to the full book, not a substitute for reading it.
