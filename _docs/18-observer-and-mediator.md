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

- Build an Observer subject that notifies listeners through one functional interface, and choose between a named listener class and a lambda.
- Return an `AutoCloseable` subscription handle so try-with-resources unsubscribes, and notify over a snapshot so a listener can unsubscribe mid-notification.
- Know when `java.util.concurrent.Flow` replaces a listener list.
- Build a Mediator that routes events between a warehouse, a carrier and a notification station that never reference each other.
- Tell the two apart: one-to-many fan-out against many-to-many routing through a hub.
- Recognize when a direct call is better than either.

## Why this matters

A shipment's status changes many times between packing and delivery, and several parts of the system care. A support dashboard shows the latest status. An alerting service wants only the move into an exception state. A billing dispute months later wants every transition. Each reaction is added on its own schedule, and the tracker shouldn't have to know any of them exist. That's Observer.

A dispatch desk has a different shape. The warehouse stages an order, the carrier picks it up and reports progress, and a notification service turns every event into a customer message. If each component called the next directly, adding a returns stage that hears about delivery exceptions would mean editing every class it hears from, and the number of direct wires grows faster than the number of components. That's Mediator: each component talks only to one coordinator, the one place that knows who everyone is.

Observer frees a subject from knowing its listeners. Mediator frees peers from knowing each other, and pays with a hub that knows all of them.

{% include excalidraw.html name="18-observer-mediator" caption="Figure 18.1: A ShipmentTracker subject fanning a status change out to independent listeners, next to a DispatchCoordinator mediator routing between three collaborators that never call each other directly." %}

Both halves of `examples/18-observer-mediator` share one enum, and share nothing else:

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

## Pattern: Observer

### The problem

A tracker that calls the dashboard, the alerting service and the audit log by name has to change every time a reaction is added or removed, and a forgotten unsubscribe leaves a dead listener running.

### The solution

An **Observer** lets a subject notify an open set of subscribed listeners when its state changes, without knowing who they are.

Use an Observer when:

- more than one independent reaction follows the same change;
- reactions are added and removed separately from the subject.

### How Java models it

A listener is a single-method functional interface:

```java
@FunctionalInterface
public interface ShipmentStatusListener {
    void onStatusChanged(String trackingId, ShipmentStatus oldStatus, ShipmentStatus newStatus);
}
```

`ShipmentTracker.subscribe(ShipmentStatusListener)` stores a listener and returns a `Subscription`. `advanceStatus()` moves the status and calls every subscribed listener in subscription order with the tracking ID, the old status and the new one. Advancing past `DELIVERED` or `EXCEPTION` throws `IllegalStateException`. There's no base class to extend: a named class, a method reference or a lambda all qualify.

### Named class or lambda

`DashboardView` and `AlertingService` are named classes because other code holds and queries them: `currentStatusLine()` and `alerts()`.

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

The audit log only reacts, so it's a lambda capturing the list it appends to:

```java
List<String> auditEntries = new ArrayList<>();
Subscription auditSubscription = tracker.subscribe(
        (trackingId, oldStatus, newStatus) ->
                auditEntries.add(trackingId + ": " + oldStatus + " -> " + newStatus));
```

| Form | Use it when |
| --- | --- |
| Lambda or method reference | The listener only reacts and keeps no identity of its own. |
| Named class | Other code holds the listener, queries its state, or reuses it across subscriptions. |

The GoF class diagram assumes every listener is a class. A functional interface removes that constraint for any one-method contract.

### Unsubscribe with AutoCloseable

`Subscription` narrows `close()` so it declares no checked exception, since closing a subscription can't fail:

```java
public interface Subscription extends AutoCloseable {
    @Override
    void close();
}
```

`close()` removes the listener from the tracker, so try-with-resources ends a subscription on every exit path, including an exception:

```java
AtomicInteger callCount = new AtomicInteger();
try (Subscription ignored = thirdTracker.subscribe(
        (trackingId, oldStatus, newStatus) -> callCount.incrementAndGet())) {
    thirdTracker.advanceStatus(ShipmentStatus.PICKED_UP);
}
thirdTracker.advanceStatus(ShipmentStatus.IN_TRANSIT);
```

The listener counts one change. The second `advanceStatus()` runs after the block has closed the subscription.

### Notify over a snapshot

A listener may unsubscribe itself, or another listener, from inside its callback. The tracker keeps listeners in a `LinkedHashMap`, so notification iterates a copy:

```java
List<ShipmentStatusListener> snapshot = new ArrayList<>(listeners.values());
for (ShipmentStatusListener listener : snapshot) {
    listener.onStatusChanged(trackingId, oldStatus, newStatus);
}
```

Iterating the live map instead throws `ConcurrentModificationException` the moment a callback unsubscribes. Copying a few references per change is a fixed cost against a failure that appears only when a reaction edits the list it's called from.

### When to reach for Flow

A listener list calls each listener synchronously on the thread that raised the change. `java.util.concurrent.Flow` (`Publisher`, `Subscriber`, `Subscription`) adds asynchronous delivery and backpressure: a subscriber requests only as many items as it can handle.

| Listeners are | Use |
| --- | --- |
| Few, fast and synchronous, like `DashboardView` and `AlertingService` | A plain listener list |
| Slow, high-volume, or need to throttle the publisher | `Flow` |

`Flow`'s request protocol and lifecycle are overhead until a listener's processing time or volume makes a direct call from the subject a problem.

## Pattern: Mediator

### The problem

Three stations react to each other's events. Direct references between them mean every new collaborator adds a field and a call to each peer that cares, and the wiring becomes a mesh.

### The solution

A **Mediator** encapsulates how a set of objects interact, so each object talks only to the mediator and never to its peers.

Use a Mediator when:

- several collaborators react to each other's events;
- the routing rule changes independently of any one collaborator;
- different teams own different collaborators.

### How Java models it

Every station extends `DispatchColleague`, which can reach only its mediator:

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

`WarehouseStation.markOrderReady()` raises `ORDER_READY_FOR_PICKUP` without knowing who responds. `DispatchCoordinator` wires the colleagues in its constructor:

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

Colleagues need the mediator and the mediator needs live colleagues. Binding after construction breaks that cycle: a station exists with no mediator until the coordinator wires it.

`bindMediator()` is package-private. Java has no `friend` declaration, and package-private access is the idiomatic substitute: code outside the `dispatch` package sees no way to rewire a colleague's mediator.

### Routing in one switch

All routing is one exhaustive switch expression over `DispatchEvent`:

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

`warehouse.markOrderReady("ORD-5001")` cascades through two hops the warehouse never sees: the coordinator calls `CarrierStation.confirmPickup()`, which raises `PICKED_UP_BY_CARRIER`, which the coordinator routes to `CustomerNotificationStation.send()`. No station holds a reference to another. A fifth `DispatchEvent` constant without a case is a compile error.

## When not to use it

- **Observer.** A status change with exactly one reaction needs a direct method call, not a `Subscription` to manage and a functional interface to trace through. Add the subscription machinery when a second listener is in hand or clearly coming.
- **Mediator, too early.** Three stations that won't gain a fourth need no hub. A visible sequence of calls, read top to bottom, is easier to review than a coordinator.
- **Mediator, too late.** A coordinator absorbs every new rule because one more `case` always looks cheapest. Once `DispatchCoordinator` holds validation, retry policy and station-specific business rules, it's a god object. Keep its job to deciding which station hears which event; what the station does belongs in that station, as `CarrierStation.confirmPickup()` does.

> **🤖 Explore with your agent**
>
> "In `examples/18-observer-mediator`, add a `ReturnsStation` that hears about `DELIVERY_EXCEPTION`. Make it extend `DispatchColleague`, give it no field referring to `WarehouseStation`, `CarrierStation` or `CustomerNotificationStation`, pass it to the `DispatchCoordinator` constructor and bind it there with the package-private `bindMediator`, and add the call to the `DELIVERY_EXCEPTION` case of `notify`. Update `DispatchCoordinatorTest` for the new constructor and add a case that a delivery exception reaches `ReturnsStation`. Keep routing out of the stations and station logic out of the coordinator."
>
> Check: `mvn -q -f examples/18-observer-mediator/pom.xml test -Dtest=DispatchCoordinatorTest`, and confirm no station class gains a field typed as another station.

## Build, run, observe

From the repository root, the aggregator builds every example:

```sh
mvn -f examples/pom.xml test
```

Or build this one on its own:

```sh
cd examples/18-observer-mediator
mvn test
```

All 11 tests pass. The demo subscribes a dashboard and an alerting service to one shipment and advances it into an exception, subscribes a lambda audit log to a second shipment, closes a subscription mid-session, then runs an order through the dispatch coordinator:

```sh
examples/18-observer-mediator/demo.sh
```

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
Callback ran 1 time(s); subscriberCount() is now 0

== Mediator: a warehouse event cascades through the coordinator ==
Carrier's own record of pickups: 1
Messages the coordinator routed to the customer:
  ORD-5001: Your package has been picked up and is on its way.
  ORD-5001: Your package is out for delivery today.
  ORD-5001: There is a delivery exception on your package; our team is investigating.
```

The single alert is the `IN_TRANSIT -> EXCEPTION` transition; earlier changes didn't trigger it. The three customer messages come from one `markOrderReady()` call and two `CarrierStation` calls, with no station referencing another.

- **Drop the snapshot.** In a scratch copy, make `advanceStatus()` iterate `listeners.values()` directly. The self-unsubscribe test errors with `ConcurrentModificationException`.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design
  Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480)
  (Addison-Wesley): the original Observer and Mediator entries, including
  the "one-to-many dependency" and "encapsulate how a set of objects
  interact" intent statements the tracker and dispatch desk build on.
- The official [`java.util.concurrent.Flow`
  documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/Flow.html):
  the `Publisher`/`Subscriber`/`Subscription` protocol the listener list is
  contrasted against, including the request-based backpressure a plain
  listener list does not provide.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/)
  and [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html):
  the testing idiom behind `examples/18-observer-mediator`, which asserts
  fan-out order, post-unsubscribe silence, and event routing with a
  verified mock.
- The full reading list lives on the
  [bibliography page]({{ '/bibliography/' | relative_url }}).
