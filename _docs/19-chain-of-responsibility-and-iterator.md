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

- Name the force Chain of Responsibility answers: a request that must be
  checked against a sequence of rules owned by different concerns, where
  the sender should not need to know which rule, if any, ends up acting on
  it.
- Build an object chain where each handler either decides a request or
  passes it along, with a fallback that guarantees the chain always
  terminates in a decision.
- Build the identical set of rules as a functional composition of checks
  over a Stream, and say what that form buys and gives up against the
  object chain.
- Build a hand-written `Iterable`/`Iterator` pair that provides sequential
  access to an aggregate without exposing how it stores its data
  internally.
- Get a Stream over that same aggregate with no additional traversal code,
  through `Iterable`'s default `spliterator()` method, and say why you
  rarely hand-write an `Iterator` today.
- Recognize when either pattern is solving a problem a plain `if`/`else` or
  a plain `for`-each loop over a `List` already solves, and say what to
  write instead.

## Why this matters

A logistics platform rarely approves a shipment with one check. It has to
confirm the parcel is not over the carrier's weight limit, confirm a
hazardous-materials parcel is cleared for its destination country, confirm
the destination is not under an active trade embargo, and only when none of
that rejects the request outright, route anything left over to a human
reviewer. Each of those checks is owned by a different concern: compliance
owns the embargo list, safety owns the hazmat clearance list, and
operations owns the weight limit, and each one changes on its own schedule,
independent of the others. The code that submits a shipment for approval
should not have to know how many checks exist today, in what order they
run, or which team owns the rule that eventually decides a given request.
Chain of Responsibility is the Gang of Four's name for exactly that force: a
request passed down a sequence of handlers, each of which either acts on it
or hands it to the next, so the sender holds nothing more than a reference
to the first handler, or the first check, in the sequence.

A warehouse scanning app that lists packages has a different, unrelated
problem with a similar shape. The packages on a manifest are not
necessarily stored as one flat list. In this chapter's example a warehouse
sorts arriving packages into one lane per destination zone as they come in,
so the manifest holds four separate buckets internally, not a single
`List`. A caller that wants to walk every package in zone order, regardless
of the order packages arrived, should not have to know the manifest is
bucketed at all. Iterator is the Gang of Four's name for that force: a
small object that knows how to traverse an aggregate's actual structure and
presents a uniform, sequential view of it to anything that asks for one.
Java gives this pattern a second, complementary expression: once an
aggregate implements `Iterable`, the standard library already knows how to
turn it into a Stream, with no further traversal code of its own required.

This chapter pairs the two patterns because they sit next to each other in
this book's catalogue of coordinating structures, not because one grows out
of the other. Chain of Responsibility coordinates a sequence of
*decisions*; Iterator coordinates a sequence of *elements*. Reading them
together also reinforces a habit this book has trained from its opening
chapters: naming the actual force at work, a request that needs routing
versus a structure that needs walking, before reaching for either pattern's
shape, and each pattern gets exactly two idiomatic expressions here because
that is how many Java gives you a real reason to choose between.

{% include excalidraw.html name="19-chain-iterator" caption="Figure 19.1: An ApprovalRequest flowing down a chain of handlers until one of them rejects it or the chain falls back to manual review, next to a ShipmentManifest traversed by a hand-written Iterator and by a Stream built on the same Iterable." %}

## The domain: a shipment-approval chain and a zone-bucketed manifest

The example behind this chapter lives in `examples/19-chain-iterator` and
splits into the two halves the diagram shows. The Chain of Responsibility
half works against a small request type:

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

A decision comes back as an `ApprovalOutcome`, a sealed interface with
exactly two implementations. Nothing in this domain is approved
automatically; a request either fails a check outright or clears every
automatic check and lands with a human:

```java
public sealed interface ApprovalOutcome permits ApprovalOutcome.Rejected, ApprovalOutcome.NeedsManualReview {
    String decidedBy();

    record Rejected(String decidedBy, String reason) implements ApprovalOutcome {}

    record NeedsManualReview(String decidedBy, String reason) implements ApprovalOutcome {}
}
```

Sealing `ApprovalOutcome` to those two cases means any code that switches
on the result, this chapter's demo included, does so exhaustively, with no
`default` branch standing in for a case nobody got around to handling.

The Iterator half works against a different type, `ShipmentManifest`, which
does not store its `Shipment` values as one flat list. It stores four
buckets, one per `DestinationZone` (`LOCAL`, `REGIONAL`, `NATIONAL`,
`INTERNATIONAL`), because that is how a warehouse sorts inbound packages.
`ShipmentManifest`'s hand-written iterator is the object whose entire job
is hiding that bucketing from anything that walks the manifest.

## An object chain: each handler decides or passes

`ApprovalHandler` is the base class every concrete handler extends. Its
`handle()` method is `final`, which matters: every handler gets the same
three steps, record that it was consulted, ask its own `attempt()` for a
decision, and otherwise delegate to the next handler, with no handler
subclass able to skip the logging or the delegation by accident.

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

Three concrete handlers each own exactly one check. `WeightLimitHandler`
rejects a shipment over its configured limit; `HazmatClearanceHandler`
rejects a hazardous shipment headed to a country not on its cleared list;
`EmbargoHandler` rejects a shipment to an embargoed destination. Each one's
`attempt()` returns a decision only when its own rule fires, and
`Optional.empty()` otherwise, which is the signal `handle()` reads as "pass
this to the next handler":

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

`ApprovalChains.defaultChain()` wires the four handlers together, innermost
construction first:

```java
public static ApprovalHandler defaultChain() {
    ApprovalHandler fallback = new ManualReviewHandler();
    ApprovalHandler embargo = new EmbargoHandler(Set.of("KP", "IR"), fallback);
    ApprovalHandler hazmat = new HazmatClearanceHandler(Set.of("US", "CA"), embargo);
    return new WeightLimitHandler(40.0, hazmat);
}
```

A caller holding only the `WeightLimitHandler` returned from
`defaultChain()` cannot tell, from one `handle()` call, whether a request
stopped at the first handler or traveled through all four.
`ApprovalChainTest` makes that observable directly by asserting the full
`callLog` alongside the final decision: a 55 kg shipment produces a
`callLog` of exactly `["WeightLimitHandler"]`, confirming that
`HazmatClearanceHandler` and `EmbargoHandler` are never consulted once
`WeightLimitHandler` has already rejected the request.

## The fallback guarantees the chain terminates

`ManualReviewHandler` sits at the end of the chain with no next handler,
and its `attempt()` never returns `Optional.empty()`:

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

Without the fallback, nothing in the chain's own type guarantees a request
ever reaches a decision. A chain built by hand, by stringing constructors
together, has no compiler-level check that some handler eventually decides
a request; if every concrete handler returned `Optional.empty()` and the
chain ran out of handlers, `handle()` would fall through its `next != null`
branch into the final `return` statement, which reports that case as an
explicit `NeedsManualReview` rather than leaving the caller to guess.
Designing one handler, the fallback, whose `attempt()` is unconditional
turns "falls off the end unhandled" into "always lands in a known, named
outcome." A shipment to Germany, not overweight, not hazmat, not
embargoed, clears all three automatic checks and reaches
`ManualReviewHandler`, which routes it to a human rather than approving it
automatically.

Both this chapter's chain and a Decorator stack wrap one object's interface
around another and let a caller hold only the outermost layer, but the two
patterns answer opposite questions about how many layers run. A decorator
stack always calls every layer on every request; a surcharge decorator
always calls down into whatever it wraps and adds its markup on top, no
matter what the wrapped object returns. A Chain of Responsibility stops at
the first handler that acts, and nothing downstream of that handler ever
sees the request at all. `ApprovalChainTest`'s `callLog` assertions exist
specifically to make that difference observable: a chain's log for a
rejected request is short, a decorator stack's call sequence for any
request is always the full stack. If a reviewer cannot immediately say
whether a given layer in a new design always runs or only runs until one
layer claims the request, that question, not the two patterns' superficially
similar constructor shapes, is the one to answer first.

## The same rules as a functional composition

`ApprovalHandler` declares exactly one abstract method, `attempt()`, which
means the same decision a handler makes can be expressed as a plain value:
`Function<ApprovalRequest, Optional<ApprovalOutcome>>`. `ApprovalChecks`
rebuilds the identical three rules this way, with no class, no
constructor, and no `next` field anywhere:

```java
public static Function<ApprovalRequest, Optional<ApprovalOutcome>> embargo(Set<String> embargoedCountries) {
    Set<String> embargoed = Set.copyOf(embargoedCountries);
    return request -> embargoed.contains(request.destinationCountry())
            ? Optional.of(new ApprovalOutcome.Rejected("embargo",
                    "destination " + request.destinationCountry() + " is under embargo"))
            : Optional.empty();
}
```

`FunctionalApprovalChain` holds a list of these functions and stops at the
first one that returns a decision, the same stopping rule the object
chain's `handle()` enforces, expressed as a Stream pipeline instead of a
chain of method calls:

```java
public ApprovalOutcome decide(ApprovalRequest request) {
    return checks.stream()
            .map(check -> check.apply(request))
            .flatMap(Optional::stream)
            .findFirst()
            .orElseGet(() -> new ApprovalOutcome.NeedsManualReview("functionalChain", "no check reached a decision"));
}
```

`Optional::stream`, a zero-or-one-element Stream over an `Optional`'s
contents, is what lets `flatMap` turn a list of `Optional<ApprovalOutcome>`
results into a single Stream of only the decisions that were actually made,
in order; `findFirst()` then picks the first one, which is exactly "the
first check that fired." `FunctionalApprovalChainTest` confirms both forms
agree: given the same hazmat shipment to France, the object chain and the
functional chain each reject it, for the identical reason, by different
mechanisms.

What the functional form gives up is almost everything that made the
object chain's `callLog` possible to assert directly. A plain `Function`
has no name of its own; `ApprovalChecks.embargo()` has to hardcode the
string `"embargo"` into the `Rejected` it returns, because nothing about a
lambda or a method reference can be inspected to recover which check it
is. The object chain gets that identity, and the call-logging it enables,
directly from each handler's constructor argument. Reach for the
functional form when the rules are plain, stateless checks assembled once
at a call site, an order of magnitude less ceremony than a dedicated class
per rule. Reach for the object form when a rule needs enough internal
state or identity that naming it, as a class with its own constructor and
fields, clarifies the code, or when the chain's own shape, which handler
ran and in what order, is something calling code needs to inspect or test
directly.

## Iterator: hiding the manifest's bucketed storage

`ShipmentManifest` stores its shipments in an `EnumMap<DestinationZone,
List<Shipment>>`, one list per zone, and `add()` routes each new shipment
into the bucket matching its zone:

```java
public void add(Shipment shipment) {
    Objects.requireNonNull(shipment, "shipment");
    byZone.get(shipment.zone()).add(shipment);
}
```

A caller that wants to list every shipment in the manifest, in zone order,
should not have to know buckets exist. `ShipmentManifest` implements
`Iterable<Shipment>`, and its private `ZoneOrderIterator` is a
hand-written forward iterator that flattens the four buckets into one
traversal:

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

`advanceToNextNonEmptyBucket()` moves `current` to the next zone's
iterator whenever the one it is holding runs dry, continuing until it
finds a bucket with an element in it or runs out of zones entirely. Both
the constructor and `next()` call it, so an empty first bucket never
produces an iterator that claims to have a next element when it does not,
and a bucket that empties out mid-traversal is skipped the moment its last
element is returned. A caller holding a `ShipmentManifest`'s iterator has
no way to observe that the manifest underneath it is four lists rather than
one; `hasNext()` and `next()` are the entire contract, and a plain `for`
loop never once mentions buckets:

```java
for (Shipment shipment : manifest) {
    System.out.println(shipment.trackingNumber());
}
```

`ShipmentManifestTest.iteratorVisitsShipmentsInZoneOrderRegardlessOfInsertionOrder`
inserts shipments in International, Local, National, National, Regional
order and asserts they come back Local, Regional, National, National,
International, the manifest's zone order, not the order they were added.

## The same traversal as a Stream

Implementing `Iterable<Shipment>` buys `ShipmentManifest` something beyond
the `for`-each loop: `Iterable` declares a default `spliterator()` method,
built automatically from whatever `iterator()` returns, and
`StreamSupport.stream()` turns any spliterator into a Stream. `stream()` on
`ShipmentManifest` is one line that calls both:

```java
public Stream<Shipment> stream() {
    return StreamSupport.stream(spliterator(), false);
}
```

Nothing in that method knows about zone buckets. All of the real traversal
work, skipping empty buckets, advancing from one zone's list to the next,
still happens inside `ZoneOrderIterator`; `stream()` only wraps that
traversal in the Stream API's declarative operations.

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

Both columns produce the identical list for the identical manifest, which
`ShipmentManifestTest.streamProducesTheSameOrderAsTheIterator` and
`streamFiltersAndMapsOverTheManifest` check directly rather than assume.
Reach for the `for`-each form when a traversal is a single, simple pass
with no filtering or transformation attached, where a loop reads as
clearly as a pipeline would. Reach for the Stream form the moment a
traversal needs to filter, map, sort, or collect into a different shape,
because chaining those operations onto a hand-rolled loop means a new
mutable accumulator and a new `if` for every step, where a Stream pipeline
adds one more `.filter()` or `.map()` call.

## Why you rarely hand-write an Iterator now

`ShipmentManifest`'s `ZoneOrderIterator` is worth writing by hand because
the manifest's real storage, four zone buckets, is not what a caller should
see. That is a narrow condition. If `ShipmentManifest` stored its shipments
as one flat `ArrayList<Shipment>` instead, writing a custom `Iterator`
around it would add an indirection layer, every `hasNext()` and `next()`
call forwarding to the wrapped list's own iterator, with nothing behind it
to justify the cost; `ArrayList` already implements `Iterable` and already
hides nothing a caller needs hiding. Most Java code never reaches for a
hand-written `Iterator` at all, not because the pattern stopped mattering,
but because the standard collections already supply one for every common
shape of data, and a `List`, `Set`, or `Map`'s own iterator is the right
answer whenever there is no real internal structure like zone buckets to
hide. Streams close the remaining gap: a hand-written `Iterator` answers
"how do I traverse this," one element at a time, while a Stream pipeline
answers "what subset of this, transformed how," declaratively, and does so
over any `Iterable`, custom or not, with no bucket-aware code required.
Writing a custom `Iterator` today is a decision made specifically because
an aggregate's storage needs hiding, not a default step on the way to
making something traversable.

> **AI sidebar.** Ask an LLM pairing partner to "add a check" to an
> existing approval flow, and a common result is a new `if` statement
> inserted directly into whatever method already runs the other checks,
> growing one method instead of adding one handler or one check function.
> That shape works today and breaks the property this chapter's `callLog`
> assertions exist to protect: a reviewer can no longer tell, without
> re-reading the whole method, whether a new check can run before or after
> an existing one, or whether it is supposed to stop processing when it
> fires. The same prompt aimed at this chapter's chain should produce
> either one new `ApprovalHandler` subclass inserted at a named position in
> `ApprovalChains.defaultChain()`, or one new entry in
> `FunctionalApprovalChain.of(...)`'s argument list, with its own
> `Optional.empty()` return on every path that should pass to the next
> check. Check which shape a generated change took before accepting it. A
> new `if` branch growing inside `ApprovalHandler.handle()` itself, or
> inside whatever method calls `decide()`, is the sign the model reached
> for the shortest diff instead of the one this chapter's forces call for.

## When not to use Chain of Responsibility or Iterator

Chain of Responsibility earns its handler objects, or its list of check
functions, when the number of checks is open-ended, owned by more than one
team, or needs to change which checks run and in what order without
touching a shared method. A fixed set of two checks, owned by one team,
that never needs to reorder or disable a check independently of the others
does not need `ApprovalHandler` subclasses or a `FunctionalApprovalChain`;
it needs a plain two-step check inside one method, read start to finish by
whoever maintains it:

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

The moment that calculus flips is the moment a second team needs to add a
check without touching the first team's code, or a check needs to be
swapped out per deployment region at runtime. Until then, the plain method
is the stronger engineering choice, even with this chapter's handler
classes and check functions sitting right there as a template to copy.

Iterator fails a related test in a narrower way. If a manifest stored its
shipments as one flat `List<Shipment>` instead of four zone buckets, a
hand-written `Iterator` around it would have nothing to hide, because
`List<Shipment>` already provides sequential access with no internal
structure to leak in the first place. A plain `for`-each loop over that
`List` is the entire story:

```java
for (Shipment shipment : shipments) {
    System.out.println(shipment.trackingNumber());
}
```

Writing a custom `Iterable` and `Iterator` around an aggregate that is
already flat adds a layer of ceremony, a private inner class, two
overridden methods, a field to track position, with nothing behind it to
justify the cost. The reason this chapter's manifest iterator is worth
writing by hand is specifically that its real storage is not what a caller
should see; reach for a custom iterator only when there is a real structure
like that one to hide, and reach for the aggregate's native `List` or `Set`
iterator directly otherwise. A Stream pipeline does not change that
calculus either: `filter` and `map` are worth composing when a caller needs
a declarative subset-and-transform view over a collection, not as a way to
dress up a single pass that a plain `for`-each loop already expresses
clearly.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this
loop. From the repository root, the aggregator picks up
`examples/19-chain-iterator` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/19-chain-iterator
mvn test
```

Either path runs the same 15 JUnit 5 cases, asserted with AssertJ: the
object chain stops at the correct handler and logs exactly the handlers
consulted, the functional chain agrees with the object chain on identical
requests, and the manifest's iterator and Stream traversal produce the
identical zone-ordered sequence. Observed locally on Fedora with Temurin
25.0.2:

```
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see both chain forms route real shipments and both traversal forms walk
the same manifest, rather than just reading an assertion that says they
agree, run the example's own demo script:

```sh
examples/19-chain-iterator/demo.sh
```

It routes four shipment requests through the object chain, printing which
handler decided each one and the full chain of handlers consulted along
the way, then routes the same four requests through the functional chain,
then walks a zone-bucketed manifest through the hand-written iterator and
the Stream pipeline:

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

Notice the first block's `chain visited` line growing by exactly one
handler per request: one for the overweight US shipment, two for the
hazmat shipment to France, three for the embargoed shipment to North
Korea, and all four only for the shipment to Germany that clears every
automatic check. That growing log is the chain stopping at the first
handler that acts, shown as an observable fact rather than inferred from
the final decision alone, and the functional chain's block right below it
reaches the identical decisions with no log at all, the tradeoff this
chapter's two expressions make explicit. The manifest section shows a
different fact: shipments inserted in International, Local, National,
National, Regional order come back out in zone order, Local first and
International last, because the iterator walks buckets by zone regardless
of insertion order, and the Stream pipeline built on top of that same
iterator agrees with it exactly.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original Chain of Responsibility and Iterator entries, including the "avoid coupling the sender of a request to its receiver" and "provide a way to access elements sequentially without exposing its underlying representation" intent statements this chapter's approval chain and shipment manifest each build on.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 47 covers preferring a `Collection`-returning or `Iterable`-returning API over a `Stream`-returning one specifically so a caller can choose between a `for`-each loop and a Stream, the design decision behind `ShipmentManifest` offering both.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/19-chain-iterator`'s suite, which asserts the full chain of handlers consulted for a request, not only the final decision, and compares a Stream pipeline's output against a plain loop over the same manifest.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
