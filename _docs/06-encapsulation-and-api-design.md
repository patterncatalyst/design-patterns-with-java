---
title: "Encapsulation and API design"
order: 6
layout: tutorial
sectionid: docs
part: "java-substrate"
description: "A shipping-tracking gateway built behind a public interface with a package-private implementation reached through a static factory, and a Shipment/Manifest/TrackingHistory value model built from records whose compact constructors defend every collection field with List.copyOf."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Hide an implementation class behind a public interface and a static
  factory method, the way `List.of` and `Optional.of` hide theirs, and
  explain what the package boundary buys you that a public class with a
  public constructor does not.
- Use a static factory method to cache a shared instance, return a type the
  caller never names, or swap an implementation without touching a single
  caller, and say which of those a plain public constructor cannot do.
- Build a value type as a `record` with a validating compact constructor,
  and explain why an immutable field like `Instant` needs no defensive copy
  where a mutable field like `Date` does.
- Defend a collection-typed field two ways, `List.copyOf` and
  `Collections.unmodifiableList`, and say what each one guarantees about a
  caller's original list and the list a caller receives back.
- Recognize when a record is the wrong shape for a type, and when a factory
  behind an interface with a single implementation is solving a problem
  nobody has yet.

## Why this matters

A shipping company's tracking system has to answer one question correctly:
where is this shipment right now, and what happened to it before this
moment? Answering that means a caller elsewhere in the system, a checkout
flow, a customer support tool, a billing job, can read a shipment's state
but cannot rewrite its history. If any caller holding a reference to a
tracking record can mutate it, two callers racing to update the same
shipment produce a result nobody asked for, and a bug report six weeks
later has no way to establish what the record said at the time the dispute
started. Encapsulation is how a type protects that record: it decides what
a caller can see, what a caller can change, and what stays hidden well
enough that changing it later does not break every caller that already
depends on the type.

Java gives you four distinct tools for this, and they compose. A public
interface with a package-private implementation is Java's answer to the
PImpl idiom other languages reach for with a pointer to a hidden struct:
the caller depends on a contract, and the class that satisfies it can
change shape entirely without recompiling a single caller. A static
factory method is how a caller reaches that hidden implementation in the
first place, and it can do things a public constructor flatly cannot: cache
and return the same instance every time, return a subtype the caller never
has to name, or swap which concrete class it hands back based on an
argument, a configuration value, or nothing at all. Records give a value
type its equality, its hash code, and its accessors with no extra code to
write, and because a record's fields are implicitly final, half of what a
classic value class had to get right by hand is correct the moment the
type compiles. Defensive copying closes the one hole records do not close
on their own: a mutable object passed into a constructor, or handed back
out of a getter, is still a live reference to something the type does not
own unless the type copies it.

None of these four tools is optional ornamentation on top of "just write a
class." Each one answers a specific way an object's internal state can leak
past the boundary a type is supposed to draw around it, and a shipping
domain, where a tracking record moves through dozens of hands before a
package ever arrives, is a system where a leak like that costs real money
to track down.

{% include excalidraw.html name="06-encapsulation-boundary" caption="Figure 6.1: a caller depends only on the ShippingGateway interface and its static factory. The concrete implementation and the representation it owns live entirely behind the package boundary, free to change without the caller noticing." %}

## The domain: tracking a shipment without exposing how

The example behind this chapter lives in `examples/06-encapsulation`. A
`TrackingEvent` is one scan of a shipment: when it happened, where, and
what state the shipment was in. A `TrackingHistory` is the ordered list of
every scan recorded so far. A `Manifest` is the bill of lading, a list of
`ManifestLineItem` values plus when it was prepared. A `Shipment` ties all
of this together: a tracking id, an origin, a destination, a manifest, and
a history. Every one of these five types is a record, and every one of
them is immutable: nothing in this example ever mutates a `Shipment` in
place. `Shipment.withEvent(event)` returns a brand-new `Shipment` with an
updated history, and the `Shipment` it was called on is left exactly as it
was.

The gateway that creates and tracks shipments, `ShippingGateway`, is a
public interface with exactly one method a caller calls to get one: the
static factory `ShippingGateway.inMemory()`. The class behind
that factory, `InMemoryShippingGateway`, carries no access modifier at
all, which in Java means package-private: nothing outside
`com.patterncatalyst.designpatterns.ch06.shipping` can write its name,
construct it, or cast a `ShippingGateway` reference to it. A caller that
wants to create a shipment, look one up, or record a new event depends on
`ShippingGateway` and nothing else.

## Public interface, package-private implementation: Java's PImpl

A C++ type hides its representation behind a pointer to an incomplete
type, the PImpl idiom covered elsewhere in this book's domain for that
language. Java's version of the same idea needs no pointer indirection at
all, because Java already separates a type's contract from its
implementation at the language level: an interface declares what a caller
can do, and any class, in any package, can satisfy it, but only classes in
the same package can see an implementation that chooses not to be public.

```java
public interface ShippingGateway {

    Shipment createShipment(String origin, String destination, Manifest manifest);

    Optional<Shipment> find(String trackingId);

    Shipment recordEvent(String trackingId, TrackingEvent event);

    static ShippingGateway inMemory() {
        return new InMemoryShippingGateway();
    }
}
```

```java
final class InMemoryShippingGateway implements ShippingGateway {

    private final Map<String, Shipment> shipmentsByTrackingId = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public Shipment createShipment(String origin, String destination, Manifest manifest) {
        String trackingId = "SHIP-" + sequence.incrementAndGet();
        Shipment shipment = Shipment.created(trackingId, origin, destination, manifest);
        shipmentsByTrackingId.put(trackingId, shipment);
        return shipment;
    }

    // find(...) and recordEvent(...) omitted
}
```

`InMemoryShippingGateway` has no `public` keyword in front of `final class`,
and that omission is the whole mechanism. A test file in the same package
can still construct it directly, which is exactly how this chapter's own
`ShippingGatewayTest` confirms `ShippingGateway.inMemory()` returns an
instance of that specific class. A caller in a different package cannot do
the same thing even if it wanted to; the compiler rejects
`new InMemoryShippingGateway()` from outside the package with an error, not
a warning. That is a guarantee a code reviewer does not have to enforce by
reading every file that touches shipping, because nothing compiles that
would violate it. `ShippingGatewayContractTest` in this chapter's example
proves the other half of the same idea: a second implementation,
written entirely in the test file and never referencing
`InMemoryShippingGateway`, satisfies the exact same contract and passes
the exact same assertions. The interface, not the concrete class, is what
every caller and every alternate implementation agrees on.

## Static factory methods: the List.of and Optional.of idiom

`ShippingGateway.inMemory()` is a static factory method, and the pattern
is older than this chapter's example: `List.of(...)`, `Optional.of(...)`,
and `Optional.empty()` are all static factories on types whose public
constructors you have likely never called directly. A static factory can
do four things a public constructor cannot. It can have a name, so
`inMemory()` says what it returns in a way `new ShippingGatewayImpl()`
does not. It can return a subtype of its declared return type, which is
exactly how it hands back a package-private class through a public
interface. It can return the same cached instance every time instead of
allocating a new one, the way `TrackingHistory.empty()` always returns the
one shared empty history rather than a fresh allocation per call. And it
can choose which concrete implementation to return based on an argument or
nothing at all, without a caller's code changing when that choice changes.

```java
public record TrackingHistory(List<TrackingEvent> events) {

    private static final TrackingHistory EMPTY = new TrackingHistory(List.of());

    public TrackingHistory {
        events = List.copyOf(events);
    }

    public static TrackingHistory empty() {
        return EMPTY;
    }
    // withEvent(...) and latest() omitted
}
```

`TrackingHistory.empty() == TrackingHistory.empty()` is `true`, proven
directly in `ShippingDemo`'s output: both calls return the identical
object, not two separate empty lists that happen to be equal. A public
constructor cannot do this on its own; `new TrackingHistory(List.of())`
always allocates, no matter how many times it runs.

```java
public interface ShippingGateway {
    static ShippingGateway inMemory() {
        return new InMemoryShippingGateway();
    }
}

ShippingGateway gateway = ShippingGateway.inMemory();
```

The caller's variable is typed `ShippingGateway`, and the concrete
`InMemoryShippingGateway` class is never named outside its own package.
That changes what a caller's source code is allowed to know. A caller
holding a reference typed to a single concrete gateway class is coupled to
that exact class forever; swapping the storage strategy to a
database-backed implementation later means either rewriting that class in
place or changing every caller's declared type. A caller holding a
`ShippingGateway` reference built through `inMemory()` is coupled to an
interface with three methods; swapping `InMemoryShippingGateway` for a
`JdbcShippingGateway` behind a new static factory, or even behind the same
one, changes zero lines in any caller.

## Records: immutable value types without the ceremony

Before records, building an immutable value type meant writing every
accessor, `equals`, `hashCode`, and `toString` by hand, and getting all
four to agree with each other and with the constructor was tedious work a
reviewer checks line by line rather than trusts on sight. A record retires
that hand-written ceremony by declaring the state once and deriving all of
it from the declaration: a record's fields are implicitly `final`, so a
record can never accidentally grow a mutator its author forgot to leave
out.

```java
public record TrackingEvent(Instant occurredAt, String location, TrackingStatus status) {

    public TrackingEvent {
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(status, "status");
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("location cannot be blank");
        }
    }
}
```

`occurredAt` is an `Instant`, which has no mutating methods at all, so
there is no defensive copy to write anywhere in this record: there is no
way for a caller to reach in and change an `Instant` it already handed
over, the way a caller could reach into a mutable `java.util.Date` through
a getter or a retained constructor argument. Choosing `Instant` over
`Date` is itself an encapsulation decision, made once at the type level,
that removes an entire category of defensive code from every type that
uses it afterward.

## Immutability and defensive copying: the part records do not do for you

A record's compact constructor runs on every value that construction
receives, but it does not automatically protect a collection-typed field
the way it protects the record's own identity. `List.copyOf(items)` in
`Manifest`'s compact constructor does two jobs in one line: it copies
whatever list a caller passed in, so a later mutation of the caller's
original list cannot reach this manifest's state, and it wraps that copy
in an immutable list, so `manifest.items()` cannot be mutated by whoever
calls it either.

```java
public record Manifest(String manifestId, List<ManifestLineItem> items, Instant preparedAt) {

    public Manifest {
        Objects.requireNonNull(manifestId, "manifestId");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("a manifest needs at least one line item");
        }
        items = List.copyOf(items);
        Objects.requireNonNull(preparedAt, "preparedAt");
    }
}
```

`ManifestTest.compactConstructorDefensivelyCopiesTheCallersList` builds a
mutable `ArrayList`, passes it to `Manifest`'s constructor, then adds a
second element to that same list after construction, and asserts the
manifest still reports exactly one item. A second test calls
`manifest.items().add(...)` directly and asserts it throws
`UnsupportedOperationException`. Both halves of defensive copying, the
copy on the way in and the immutability on the way out, are proven as
separate, observable facts rather than assumed from reading the one line
of code that produces both.

The same two guarantees can be built by hand with two separate steps
instead of one call:

```java
this.items = Collections.unmodifiableList(new ArrayList<>(items));
```

`new ArrayList<>(items)` is the copy; `Collections.unmodifiableList(...)`
is the immutable wrap. Both steps matter, and dropping either one breaks a
different half of the guarantee: skip the `ArrayList` copy and wrap the
caller's own list directly, and a mutation of the caller's original list
still reaches this object's state, because `unmodifiableList` only blocks
mutation through the view it returns, not mutation of the list underneath
it. Skip the `unmodifiableList` wrap and keep only the copy, and the copy
protects against the caller's original list but not against a caller who
holds onto whatever `items()` returns and calls `.add()` on it directly.
`List.copyOf` closes both gaps in a single call because the list it
produces is guaranteed immutable, which is why `Manifest`'s compact
constructor uses it instead of the two-step form.

> **AI sidebar.** Ask an LLM pairing partner to "make this class
> immutable" and a common result adds `final` to every field and stops
> there, because that is enough to satisfy a narrow, syntactic reading of
> the word. Check the result the way this chapter trained: a `final` field
> that holds a mutable object, a `List`, a `Date`, a `StringBuilder`, is
> still reachable and mutable through that reference no matter how many
> `final` keywords surround it. The generated code will compile, pass a
> quick smoke test, and still let a caller mutate a "finished" object's
> internal list by holding a reference to it from before construction or
> by calling a getter and mutating what it returns. The fix a model rarely
> volunteers unprompted is exactly this chapter's two-part check: does the
> constructor copy a mutable argument before storing it, and does every
> getter that exposes a collection or a mutable object return a defended
> copy or an immutable view rather than the live field itself. A generated
> `Manifest` with a `final List<ManifestLineItem> items` field and a getter
> that returns `items` directly, with no `List.copyOf` anywhere, has
> `final` in exactly the place that does not protect the guarantee the
> prompt actually asked for.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this
loop. From the repository root, the aggregator picks up
`examples/06-encapsulation` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/06-encapsulation
mvn test
```

Either path runs the same 19 JUnit 5 cases, asserted with AssertJ,
including a parameterized contract test that runs identical assertions
against `ShippingGateway.inMemory()` and a second implementation written
entirely in the test tree. Observed locally on Fedora with Temurin 25.0.2:

```
Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see the encapsulation guarantees hold at the terminal rather than just
reading an assertion that says they do, run the example's own demo script:

```sh
examples/06-encapsulation/demo.sh
```

It creates a shipment through the factory-built gateway, confirms
`TrackingHistory.empty()` returns the same cached instance twice, records
two events and shows the original `Shipment` reference untouched by either
one, then mutates a caller's list after handing it to a `Manifest` and
tries to mutate the manifest's own list directly:

```
== Public interface + package-private implementation ==
Concrete type the caller never names: InMemoryShippingGateway
Created: SHIP-1 (5 units)

== Static factory caching: TrackingHistory.empty() is a shared instance ==
empty() == empty(): true

== Immutability: earlier Shipment references never change ==
original history size:   0
picked-up history size:  1
in-transit history size: 2
latest event status: IN_TRANSIT

== Defensive copying: mutating a caller's list does not reach inside ==
caller's list size now: 2
manifest still reports: 1
manifest.items().add(...) rejected: UnsupportedOperationException
```

The last three lines are the point made visible: a caller's list grows to
two elements after handing it to `Manifest`, the manifest itself still
reports one, and a direct attempt to mutate what the manifest returns is
rejected outright rather than silently succeeding.

## When not to use it

Every tool in this chapter earns its cost only when the force behind it is
present in the code, not merely available in the language.

A record is the wrong shape for a type with identity that outlives its
current field values, or for a type that needs to change state in place. A bank account whose balance updates on every transaction is not
well modeled as a record that gets replaced wholesale on every deposit;
two `Account` records with the same id and a different balance are, by a
record's own `equals`, different values entirely, which is backwards for a
type whose whole point is that the same account persists across changes to
its balance. Reach for a record when a type's identity is its data, the
way a `TrackingEvent` or a `Manifest` is fully described by its fields; keep
a classic mutable class, with a stable identity field and an explicit
`equals` based on that identity rather than on every field, when identity
and state need to vary independently.

Factory indirection behind an interface earns its cost when a second
implementation is real or clearly coming, the same forces-over-reflex test
this book applies to every pattern. `ShippingGateway` with exactly one
implementation and nothing else planned would still compile, still pass
every test in this chapter, and still add a file, an interface a reader
has to open to find the one method that matters, and a factory call
standing between a caller and the only class that will ever satisfy the
contract. `ShippingGatewayContractTest`'s second implementation earns its
place here because the chapter needs to prove the indirection buys
something; a real codebase with no second gateway on its roadmap should
write `InMemoryShippingGateway` as a plain public class with a public
constructor, and introduce the interface the day a second implementation
is needed, not before. An interface with a single implementation and
a static factory that only ever returns that one class is the factory
pattern's cost with none of its benefit, and a reviewer should ask the
same question of it that this chapter asks of every other pattern: what
forces does this abstraction resolve, and would the plainer alternative
resolve them just as well.

## For deeper coverage

- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition
  (Addison-Wesley): Item 1 on static factory methods over constructors,
  Item 17 on minimizing mutability, and Item 50 on making defensive copies,
  the three items this chapter's gateway, records, and `List.copyOf`
  sections draw from directly.
- Brian Goetz et al., [*Java Concurrency in Practice*](https://jcip.net/)
  (Addison-Wesley): the chapter on safe publication and immutability covers
  why an immutable value type, correctly built, needs no synchronization to
  share safely between threads, the property `TrackingHistory` and
  `Manifest` both rely on inside `InMemoryShippingGateway`'s
  `ConcurrentHashMap`.
- The official [Java records documentation](https://docs.oracle.com/en/java/javase/25/language/records.html)
  and [`List.copyOf` Javadoc](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/List.html#copyOf(java.util.Collection)):
  the authoritative source for what a compact constructor can rewrite and
  exactly what guarantee `copyOf` makes about the list it returns.
- The full reading list behind this book's opinions lives on the
  [bibliography page]({{ '/bibliography/' | relative_url }}); each citation
  there is a pointer to the full book, not a substitute for reading it.
