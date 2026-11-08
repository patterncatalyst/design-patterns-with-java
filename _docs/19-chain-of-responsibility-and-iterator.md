---
title: "Chain of Responsibility and Iterator"
order: 19
layout: tutorial
sectionid: docs
part: "coordinating-traversing"
description: "Two patterns for coordinating a collection of objects rather than one: Chain of Responsibility routes a request through a sequence of handlers until one of them acts, built as an object chain and as a Stream-driven functional composition, and Iterator provides sequential access to an aggregate without exposing its internal structure, built by hand against a zone-bucketed shipment manifest and then exposed as a Stream with no extra code, through Iterable's default spliterator() method."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Build an object chain where each handler decides a request or passes it on, ending in a fallback that always decides.
- Build the same rules as a list of functions run through a Stream, and say what that form gives up.
- Tell a chain, which stops at the first handler that acts, from a decorator stack, which runs every layer.
- Write an `Iterable` and `Iterator` pair that hides how an aggregate stores its data.
- Get a Stream over the same aggregate through `Iterable`'s default `spliterator()`, and say why a hand-written `Iterator` is rare.
- Recognize when an `if`/`else` or a `for`-each over a `List` is enough.

## Why this matters

A logistics platform approves a shipment only after several checks: the weight limit, hazmat clearance for the destination, and the embargo list, with anything left over going to a human reviewer. Operations owns the weight limit, safety owns hazmat and compliance owns embargoes, and each changes on its own schedule. The code that submits a shipment shouldn't know how many checks exist, their order, or which one decided. Chain of Responsibility passes the request down a sequence of handlers so the sender holds only the first.

A warehouse sorts arriving packages into one lane per destination zone, so its manifest holds four buckets, not one list. A caller walking every package in zone order shouldn't know about the buckets. Iterator gives a uniform sequential view over the real structure, and once the manifest implements `Iterable`, Java turns it into a Stream with no further traversal code.

A chain coordinates a sequence of decisions; an iterator coordinates a sequence of elements.

{% include excalidraw.html name="19-chain-iterator" caption="Figure 19.1: An ApprovalRequest flowing down a chain of handlers until one of them rejects it or the chain falls back to manual review, next to a ShipmentManifest traversed by a hand-written Iterator and by a Stream built on the same Iterable." %}

## Pattern: Chain of Responsibility

### The problem

Three automatic checks owned by three teams, plus a fallback. Put them in one method and every new check edits that method, with no clear answer to whether it runs before or after the others or stops processing when it fires.

### The solution

A **Chain of Responsibility** passes a request along a sequence of handlers until one of them acts on it, so the sender is coupled only to the first handler.

Use a chain when:

- the set of checks is open-ended or owned by more than one team;
- checks must be added, reordered or swapped per deployment without touching a shared method.

### How Java models it

The request is a record, and the outcome is sealed to two cases. Nothing is approved automatically: a request fails a check or lands with a human:

```java
public record ApprovalRequest(double weightKg, boolean hazmat, String destinationCountry) {

    public ApprovalRequest {
        Objects.requireNonNull(destinationCountry, "destinationCountry");
        if (weightKg < 0.0) {
            throw new IllegalArgumentException("weightKg cannot be negative: " + weightKg);
        }
    }
}
```

```java
public sealed interface ApprovalOutcome permits ApprovalOutcome.Rejected, ApprovalOutcome.NeedsManualReview {
    String decidedBy();

    record Rejected(String decidedBy, String reason) implements ApprovalOutcome {}

    record NeedsManualReview(String decidedBy, String reason) implements ApprovalOutcome {}
}
```

Because `ApprovalOutcome` is sealed, a switch over the result is exhaustive with no `default`.

`ApprovalHandler` is the base class. Its `handle()` is `final`, so every handler records that it was consulted, asks its own `attempt()` for a decision, and otherwise delegates to the next, and no subclass can skip a step:

```java
public abstract class ApprovalHandler {
    private final String name;
    private final ApprovalHandler next;

    protected ApprovalHandler(String name, ApprovalHandler next) {
        this.name = name;
        this.next = next;
    }

    public final ApprovalOutcome handle(ApprovalRequest request, List<String> callLog) {
        callLog.add(name);
        Optional<ApprovalOutcome> decision = attempt(request);
        if (decision.isPresent()) {
            return decision.get();
        }
        if (next != null) {
            return next.handle(request, callLog);
        }
        return new ApprovalOutcome.NeedsManualReview(name, "no handler in the chain reached a decision");
    }

    protected abstract Optional<ApprovalOutcome> attempt(ApprovalRequest request);
}
```

Each concrete handler owns one rule. `attempt()` returns a decision when its rule fires and `Optional.empty()` to pass:

```java
public final class HazmatClearanceHandler extends ApprovalHandler {
    private final Set<String> clearedCountries;

    public HazmatClearanceHandler(Set<String> clearedCountries, ApprovalHandler next) {
        super("HazmatClearanceHandler", next);
        this.clearedCountries = Set.copyOf(clearedCountries);
    }

    @Override
    protected Optional<ApprovalOutcome> attempt(ApprovalRequest request) {
        if (!request.hazmat()) {
            return Optional.empty();
        }
        if (!clearedCountries.contains(request.destinationCountry())) {
            return Optional.of(new ApprovalOutcome.Rejected(name(),
                    "hazmat shipments are not cleared for " + request.destinationCountry()));
        }
        return Optional.empty();
    }
}
```

`ApprovalChains.defaultChain()` wires the handlers, innermost first:

```java
public static ApprovalHandler defaultChain() {
    ApprovalHandler fallback = new ManualReviewHandler();
    ApprovalHandler embargo = new EmbargoHandler(Set.of("KP", "IR"), fallback);
    ApprovalHandler hazmat = new HazmatClearanceHandler(Set.of("US", "CA"), embargo);
    return new WeightLimitHandler(40.0, hazmat);
}
```

A 55 kg shipment logs only `WeightLimitHandler`; the hazmat and embargo handlers never see it.

### A fallback that always decides

`ManualReviewHandler` ends the chain and never returns `Optional.empty()`:

```java
public final class ManualReviewHandler extends ApprovalHandler {
    public ManualReviewHandler() {
        super("ManualReviewHandler", null);
    }

    @Override
    protected Optional<ApprovalOutcome> attempt(ApprovalRequest request) {
        return Optional.of(new ApprovalOutcome.NeedsManualReview(name(), "routed to the manual review queue"));
    }
}
```

Nothing in a hand-strung chain's type guarantees some handler decides. A fallback whose `attempt()` is unconditional turns "falls off the end" into a named outcome, and `handle()`'s final `return` reports the same case explicitly if the fallback is ever missing. A shipment to Germany that's under the limit, not hazmat and not embargoed reaches `ManualReviewHandler` and goes to a person.

### Chain or decorator stack

Both let a caller hold only the outermost object. They answer opposite questions:

| | Chain of Responsibility | Decorator stack |
| --- | --- | --- |
| Layers run per request | Until one acts | All of them |
| Downstream sees the request | Only if every earlier handler passed | Always |
| Call log for a rejection | Short | The full stack |

Before building either, settle whether every layer must run.

### The same rules as functions

`attempt()` is the only abstract method, so a rule is also a value: `Function<ApprovalRequest, Optional<ApprovalOutcome>>`. `ApprovalChecks` rebuilds the rules with no class and no `next` field:

```java
public static Function<ApprovalRequest, Optional<ApprovalOutcome>> embargo(Set<String> embargoedCountries) {
    Set<String> embargoed = Set.copyOf(embargoedCountries);
    return request -> embargoed.contains(request.destinationCountry())
            ? Optional.of(new ApprovalOutcome.Rejected("embargo",
                    "destination " + request.destinationCountry() + " is under embargo"))
            : Optional.empty();
}
```

`FunctionalApprovalChain` runs the list through a Stream and stops at the first decision:

```java
public ApprovalOutcome decide(ApprovalRequest request) {
    return checks.stream()
            .map(check -> check.apply(request))
            .flatMap(Optional::stream)
            .findFirst()
            .orElseGet(() -> new ApprovalOutcome.NeedsManualReview("functionalChain", "no check reached a decision"));
}
```

`Optional::stream` gives a zero-or-one-element Stream, so `flatMap` keeps only the decisions made, in order, and `findFirst()` takes the first. A hazmat shipment to France is rejected for the same reason by both forms.

The functional form loses identity. A `Function` has no name, so `embargo()` hardcodes `"embargo"` into its `Rejected`, and there's no call log of which checks ran.

| Form | Use it when |
| --- | --- |
| Functions in a list | Rules are stateless checks assembled once at a call site. |
| Handler objects | A rule has state or identity worth a class, or callers need to inspect which handlers ran and in what order. |

## Pattern: Iterator

### The problem

`ShipmentManifest` stores shipments in an `EnumMap<DestinationZone, List<Shipment>>`, one list per zone, and `add()` routes each shipment to its bucket:

```java
public void add(Shipment shipment) {
    Objects.requireNonNull(shipment, "shipment");
    byZone.get(shipment.zone()).add(shipment);
}
```

Exposing the map makes every caller walk four lists and know the zone order.

### The solution

An **Iterator** provides sequential access to the elements of an aggregate without exposing how the aggregate stores them.

Use a hand-written iterator when:

- the storage has structure a caller shouldn't see, such as buckets;
- no standard collection's own iterator already gives the order you need.

### How Java models it

`ShipmentManifest` implements `Iterable<Shipment>`, and its private `ZoneOrderIterator` flattens the buckets:

```java
private final class ZoneOrderIterator implements Iterator<Shipment> {
    private final Iterator<DestinationZone> zones = List.of(DestinationZone.values()).iterator();
    private Iterator<Shipment> current = Collections.emptyIterator();

    private ZoneOrderIterator() {
        advanceToNextNonEmptyBucket();
    }

    @Override
    public boolean hasNext() {
        return current.hasNext();
    }

    @Override
    public Shipment next() {
        if (!current.hasNext()) {
            throw new NoSuchElementException();
        }
        Shipment shipment = current.next();
        advanceToNextNonEmptyBucket();
        return shipment;
    }

    private void advanceToNextNonEmptyBucket() {
        while (!current.hasNext() && zones.hasNext()) {
            current = byZone.get(zones.next()).iterator();
        }
    }
}
```

`advanceToNextNonEmptyBucket()` moves `current` to the next zone's list whenever the current one runs dry. The constructor and `next()` both call it, so an empty first bucket never produces an iterator with nothing in `current`, and an emptied bucket is skipped as soon as its last element is returned. Callers see only `hasNext()` and `next()`:

```java
for (Shipment shipment : manifest) {
    System.out.println(shipment.trackingNumber());
}
```

Shipments inserted International, Local, National, National, Regional come back Local, Regional, National, National, International.

### The same traversal as a Stream

`Iterable` has a default `spliterator()` built from `iterator()`, and `StreamSupport.stream()` turns a spliterator into a Stream:

```java
public Stream<Shipment> stream() {
    return StreamSupport.stream(spliterator(), false);
}
```

The traversal still lives in `ZoneOrderIterator`; `stream()` only wraps it. Both forms below give the same list:

{% include codetabs.html langs="For-each over the Iterator|Stream pipeline" %}
```java
// Imperative: walk the iterator directly, filter and collect by hand.
List<Double> nationalWeights = new ArrayList<>();
for (Shipment shipment : manifest) {
    if (shipment.zone() == DestinationZone.NATIONAL) {
        nationalWeights.add(shipment.weightKg());
    }
}
```
```java
// Declarative: the same result, expressed as what to keep and what to
// extract rather than how to loop.
List<Double> nationalWeights = manifest.stream()
        .filter(shipment -> shipment.zone() == DestinationZone.NATIONAL)
        .map(Shipment::weightKg)
        .collect(Collectors.toList());
```

| Traversal | Use |
| --- | --- |
| One pass, no filtering or transformation | `for`-each |
| Filter, map, sort or collect into another shape | Stream pipeline, one more call per step instead of an accumulator and an `if` |

### Why a hand-written Iterator is rare

`ZoneOrderIterator` is worth writing because the buckets shouldn't be visible. If the manifest held one `ArrayList<Shipment>`, a custom iterator would only forward `hasNext()` and `next()` to the list's own. The standard collections supply an iterator for every common shape, and a Stream answers "which of these, transformed how" over any `Iterable`. Write a custom `Iterator` only when storage needs hiding.

## When not to use it

**Chain of Responsibility.** A fixed pair of checks owned by one team, never reordered or disabled independently, belongs in one method read top to bottom:

```java
public ApprovalOutcome approve(ApprovalRequest request) {
    if (request.weightKg() > 40.0) {
        return new ApprovalOutcome.Rejected("checkout", "shipment is over the weight limit");
    }
    if (request.hazmat() && !request.destinationCountry().equals("US")) {
        return new ApprovalOutcome.Rejected("checkout", "hazmat shipment is not cleared for this destination");
    }
    return new ApprovalOutcome.NeedsManualReview("checkout", "routed to the manual review queue");
}
```

Switch to a chain when a second team needs to add a check without touching the first team's code, or a check varies per deployment region.
**Iterator.** Over a flat `List<Shipment>`, a `for`-each is the whole story:

```java
for (Shipment shipment : shipments) {
    System.out.println(shipment.trackingNumber());
}
```

A private inner class, two overrides and a position field around an already flat aggregate hide nothing. A Stream pipeline doesn't change that: use it for a subset-and-transform view, not to dress up a single pass.

> **🤖 Explore with your agent**
>
> "In `examples/19-chain-iterator`, add an empty-parcel check that rejects a request whose `weightKg()` is zero. Add it twice: as an `ApprovalHandler` subclass that overrides only `attempt()`, returns `Optional.empty()` on every path that should pass, and is inserted between `HazmatClearanceHandler` and `EmbargoHandler` in `ApprovalChains.defaultChain()`; and as a function in `ApprovalChecks` added to `FunctionalApprovalChain` in the same position. Don't add an `if` to `ApprovalHandler.handle()` or to a caller of `decide()`, keep `ManualReviewHandler` last, and update the expected call logs in `ApprovalChainTest` for the new position."
>
> Check: `mvn -q -f examples/19-chain-iterator/pom.xml test -Dtest='ApprovalChainTest,FunctionalApprovalChainTest'`, and confirm the demo's `chain visited` lines show the new handler in its position.

## Build, run, observe

From the repository root, the aggregator builds every example:

```sh
mvn -f examples/pom.xml test
```

Or build this one on its own:

```sh
cd examples/19-chain-iterator
mvn test
```

All 15 tests pass. The demo routes four requests through the object chain, printing the decider and every handler consulted, then through the functional chain, then walks a bucketed manifest with the iterator and a Stream:

```sh
examples/19-chain-iterator/demo.sh
```

```
== Chain of Responsibility: the object chain ==
US: Rejected by WeightLimitHandler -- shipment weight 55.0 kg exceeds the 40.0 kg limit
  chain visited: WeightLimitHandler
FR: Rejected by HazmatClearanceHandler -- hazmat shipments are not cleared for FR
  chain visited: WeightLimitHandler -> HazmatClearanceHandler
KP: Rejected by EmbargoHandler -- destination KP is under embargo
  chain visited: WeightLimitHandler -> HazmatClearanceHandler -> EmbargoHandler
DE: Needs manual review by ManualReviewHandler -- routed to the manual review queue
  chain visited: WeightLimitHandler -> HazmatClearanceHandler -> EmbargoHandler -> ManualReviewHandler

== Chain of Responsibility: the functional composition ==
US: Rejected by weightLimit -- shipment weight 55.0 kg exceeds the 40.0 kg limit
FR: Rejected by hazmatClearance -- hazmat shipments are not cleared for FR
KP: Rejected by embargo -- destination KP is under embargo
DE: Needs manual review by functionalChain -- no check reached a decision

== Iterator: a hand-written Iterator over a zone-bucketed manifest ==
Inserted INTL, LOC, NAT, NAT, REG order; the Iterator still visits zone order:
  TRK-LOC-1
  TRK-REG-1
  TRK-NAT-1
  TRK-NAT-2
  TRK-INTL-1

== Iterator: the same data through a Stream pipeline ==
National-zone weights (kg): [5.0, 7.5]
```

The `chain visited` line grows by one handler per request, from one for the overweight US shipment to all four for Germany. The functional chain reaches the same decisions with no log.

- **Skip the first advance.** In a scratch copy, remove the `advanceToNextNonEmptyBucket()` call from the `ZoneOrderIterator` constructor. The iterator reports no elements, and three manifest tests fail, including both Stream tests.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original Chain of Responsibility and Iterator entries, including the "avoid coupling the sender of a request to its receiver" and "provide a way to access elements sequentially without exposing its underlying representation" intent statements the approval chain and shipment manifest build on.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 47 covers preferring a `Collection`-returning or `Iterable`-returning API over a `Stream`-returning one so a caller can choose between a `for`-each loop and a Stream, the decision behind `ShipmentManifest` offering both.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/19-chain-iterator`, which asserts the full chain of handlers consulted for a request, not only the final decision.
- The full reading list lives on the [bibliography page]({{ '/bibliography/' | relative_url }}).
