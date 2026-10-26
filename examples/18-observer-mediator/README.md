# 18 - Observer and Mediator: a shipment tracker and a dispatch desk

A shipment's status changes many times between a warehouse packing it and a
customer signing for it, and more than one part of a shipping system reacts
to each change independently. A warehouse, a carrier, and a
customer-notification service also have to coordinate without any one of
them calling another's method directly. This example builds both forces
side by side against one shipping-dispatch scenario.

## The domain

- `tracking/` is the Observer half. `ShipmentTracker` owns a shipment's
  `ShipmentStatus` and fans every change out to whoever is currently
  subscribed, through the single-method `ShipmentStatusListener` functional
  interface. `subscribe()` returns a `Subscription`, an `AutoCloseable`
  handle that unsubscribes on `close()`, so a listener's lifetime ties to
  try-with-resources instead of a manual unsubscribe call a caller has to
  remember on every exit path.
- `dispatch/` is the Mediator half. `DispatchColleague` is the shared base
  `WarehouseStation`, `CarrierStation`, and `CustomerNotificationStation` all
  extend; none of them holds a reference to another. `DispatchCoordinator`
  binds all three in its constructor and is the only place that decides
  which event reaches which station.

Nothing in either half reuses a type from the other; they are two separate
forces, taught against the same shipping operation because both show up in
the same system at once.

## Observer: named listeners and a bare lambda

`DashboardView` and `AlertingService` are named classes because each one is
a long-lived collaborator something else holds onto and reads later:
`DashboardView.currentStatusLine()` and `AlertingService.alerts()`.
`ShipmentTracker` never imports either class by name; it depends only on
`ShipmentStatusListener`.

A caller with a one-shot reaction and no state to expose does not need a
named class at all. `ObserverMediatorDemo`'s audit log subscribes directly
with a lambda that appends to a captured list, no new type anywhere.

`ShipmentTracker.advanceStatus()` notifies from a snapshot copy of the
listener list, so a listener that unsubscribes itself, or another listener,
from inside its own callback never corrupts the fan-out it is part of.

## Mediator: a dispatch desk with no colleague-to-colleague calls

`WarehouseStation.markOrderReady()` raises an event; it has no idea whether
anything responds. `DispatchCoordinator`'s constructor binds all three
stations to itself after they already exist, which breaks the circular
requirement every Mediator has to solve: a colleague needs a mediator
reference, and the mediator needs live colleagues to route to.
`DispatchColleague.bindMediator()` is package-private, Java's idiomatic
substitute for C++'s `friend`: only `DispatchCoordinator`, in the same
package, can call it.

Calling `warehouse.markOrderReady("ORD-5001")` cascades through the
coordinator to the carrier, and the carrier's own calls cascade again to the
customer-notification station, all without any station holding a reference
to another.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

This example is standalone-buildable from this directory:

```sh
cd examples/18-observer-mediator
mvn test
```

It is also picked up by the repo root's aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same JUnit 5 suite (AssertJ assertions, Mockito test
doubles), asserting that subscribed listeners are notified, an unsubscribed
listener is not, a self-unsubscribing listener does not break the remaining
fan-out, and that `DispatchCoordinator` routes an event to the right
collaborator and nowhere else.

To see the fan-out and the dispatch cascade produce real output rather than
just reading an assertion that says they work, run the example's own demo
script:

```sh
./demo.sh
```

## Files

```
examples/18-observer-mediator/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run ObserverMediatorDemo
  src/main/java/com/patterncatalyst/designpatterns/ch18/shipping/
    ObserverMediatorDemo.java     # exercises both halves
    tracking/
      ShipmentStatus.java          # CREATED..DELIVERED/EXCEPTION, isTerminal()
      ShipmentStatusListener.java  # the Observer contract (functional interface)
      Subscription.java            # AutoCloseable unsubscribe handle
      ShipmentTracker.java         # the subject
      DashboardView.java           # named observer: a long-lived status line
      AlertingService.java         # named observer: accumulates exception alerts
    dispatch/
      DispatchEvent.java           # the closed set of events a colleague can raise
      DispatchMediator.java        # the hub contract
      DispatchColleague.java       # shared base; only raise() reaches outward
      WarehouseStation.java        # raises ORDER_READY_FOR_PICKUP
      CarrierStation.java          # raises PICKED_UP_BY_CARRIER / OUT_FOR_DELIVERY / DELIVERY_EXCEPTION
      CustomerNotificationStation.java # the terminal station a message reaches
      DispatchCoordinator.java     # binds all three, routes every event
  src/test/java/com/patterncatalyst/designpatterns/ch18/shipping/
    tracking/
      ShipmentTrackerTest.java     # fan-out, unsubscribe, terminal-status rejection
      DashboardViewTest.java       # reflects the latest status, not a history
      AlertingServiceTest.java     # only records transitions into EXCEPTION
    dispatch/
      DispatchCoordinatorTest.java # Mockito-verified routing and decoupling
```
