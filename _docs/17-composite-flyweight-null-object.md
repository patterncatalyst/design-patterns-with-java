---
title: "Composite, Flyweight, Null Object"
order: 17
layout: tutorial
sectionid: docs
part: "composing-adapting-structure"
description: "Three idiomatic Java 25 ways to keep a recursive shipment tree simple: a sealed ShipmentNode interface that treats one parcel and an entire pallet identically, a FlyweightPool that interns shared PackagingSpec and Country instances across thousands of parcels, and a Null Object that lets a weight-limit check skip every null branch, contrasted with Optional resolved at a construction boundary."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Name the force Composite answers: client code that needs to treat a
  single object and an arbitrarily deep tree of objects through the same
  interface, with no branch telling a leaf and a branch apart.
- Build a Composite as a sealed interface with record leaves and branches,
  and dispatch over it with an exhaustive pattern-matching `switch`.
- Name the force Flyweight answers: a program holding so many copies of the
  same immutable data that the copies, not the data's logic, become the
  cost.
- Build a generic Flyweight pool that returns one shared instance per key
  and prove the sharing with a reference-identity check rather than an
  equality check.
- Build a Null Object as an enum singleton and explain why it removes a
  null check from every call site, not just the one that constructs it.
- Decide when `Optional` removes the need for a Null Object outright, and
  when a real Null Object still reads better.
- Recognize when any of the three is solving a problem a plain class, a
  `List`, or a null check already solves, and say what to write instead.

{% include excalidraw.html name="17-composite-flyweight-tree-and-pool" caption="Figure 17.1: a composite shipment tree sharing one interface on the left; a flyweight pool of PackagingSpec instances shared by many parcels on the right." %}

## Why this matters

A pallet ready to leave a warehouse is a tree. The pallet holds cartons, a
carton can hold more cartons before it holds a single parcel, and the
question a shipping system asks at every level is the same one: how much
does this weigh, and how many parcels does it hold. A naive answer writes
one method for "weigh a parcel" and a nearly identical method for "weigh a
carton," and a third for "weigh a pallet," each one aware of what kind of
thing it is summing. That duplication is not a style complaint. The moment
a carton gets nested inside another carton, which happens the first time a
warehouse consolidates two partial shipments into one box, the
carton-weighing method has to learn to call itself, and now two of the
three methods are doing the same recursive work under different names.
Composite is the Gang of Four's name for refusing that duplication
outright: one interface, satisfied by both the leaf and the thing that
holds leaves, so "weigh this" means the same call no matter how deep the
tree goes.

A second, unrelated pressure shows up once that pallet ships. A
single packaging code, a particular corrugated box or a particular lithium
pack, might appear on hundreds of parcels across a day's shipments, and
every one of those parcels needs the same facts about it: its tare weight,
its material, whether it is hazardous to ship. Storing that packaging
description inside every parcel that references it is correct and
wasteful in the same breath; the facts never change between one parcel and
the next, only the destination and the declared value do. Flyweight is the
Gang of Four's name for separating those two kinds of state and sharing
the part that never changes, so a thousand parcels referencing one
packaging code share one descriptor object instead of carrying a thousand
copies of it.

A third pressure is smaller but no less common: a carton's weight-limit
check needs somewhere to report a violation, and most of the time nobody
has wired up anything to receive that report yet. The naive fix guards
every call site with a null check: "if there is a handler, tell it." That
guard is cheap to write once and expensive to keep correct everywhere it
gets copied, because every new call site that forgets the check is a crash
waiting on a shipment nobody flagged as overweight. Null Object removes the
check instead of remembering to write it: a do-nothing implementation
stands in for "no handler," so every call site can call the handler
unconditionally.

None of the three forces above are the same force, and the Gang of Four
did not group them in one chapter because they resemble each other
structurally. They are grouped here because one shipping domain needs all
three at once, and seeing them solve three different problems in the same
code makes each one's boundary sharper than seeing it alone would.

## The domain: a shipment container tree

The example behind this chapter lives in
`examples/17-composite-flyweight-null`. A warehouse packs a pallet from
cartons, a carton from parcels or from more cartons, and each parcel
references a `PackagingSpec` and a `Country` that describe facts about it
that never vary between one parcel and the next:

```java
public enum HazmatClass {
    NONE,
    FLAMMABLE,
    CORROSIVE,
    LITHIUM
}

public record PackagingSpec(String code, String material, int tareWeightGrams, HazmatClass hazmatClass) {
    public PackagingSpec {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(hazmatClass, "hazmatClass");
        if (tareWeightGrams < 0) {
            throw new IllegalArgumentException("tareWeightGrams cannot be negative: " + tareWeightGrams);
        }
    }
}
```

Nothing in `PackagingSpec` varies per parcel. A `STD-BOX` weighs the same
180 grams of tare whether it is parcel one on pallet nine or parcel sixty
on pallet twelve. That is the fact Flyweight below leans on: `PackagingSpec`
is safe to share because nothing about it is specific to any one parcel.
`HazmatClass` needs no sharing mechanism of its own; it is a plain enum,
and an enum constant is already a Flyweight the language gives you for
free, one JVM instance no matter how many `PackagingSpec` records
reference it.

## Composite: one sealed interface, leaf or branch

`ShipmentNode` is the interface both a single parcel and an entire group of
parcels satisfy. Sealing it to exactly two permitted types is what lets the
compiler check dispatch over it for completeness:

```java
public sealed interface ShipmentNode permits Parcel, ShipmentBox {
}
```

`Parcel` is the leaf, a record holding a shared `PackagingSpec` and
`Country` plus the two facts that are its own: how much it weighs and what
it is declared worth.

```java
public record Parcel(String id, int weightGrams, long valueCents, PackagingSpec packagingSpec, Country destination)
        implements ShipmentNode {

    public Parcel {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(packagingSpec, "packagingSpec");
        Objects.requireNonNull(destination, "destination");
        if (weightGrams < 0) {
            throw new IllegalArgumentException("weightGrams cannot be negative: " + weightGrams);
        }
        if (valueCents < 0) {
            throw new IllegalArgumentException("valueCents cannot be negative: " + valueCents);
        }
    }
}
```

`ShipmentBox` is the branch, and it is one record, not a separate `Carton`
type and a separate `Pallet` type. A carton and a pallet are the same thing
from `ShipmentNode`'s point of view: a collection of children, queried the
same way a leaf is queried.

```java
public record ShipmentBox(String label, List<ShipmentNode> contents, long weightLimitGrams,
                           ShipmentAlertHandler alertHandler) implements ShipmentNode {

    public ShipmentBox {
        Objects.requireNonNull(label, "label");
        contents = List.copyOf(contents);
        Objects.requireNonNull(alertHandler, "alertHandler");
        if (weightLimitGrams < 0) {
            throw new IllegalArgumentException("weightLimitGrams cannot be negative: " + weightLimitGrams);
        }
    }
}
```

`contents` is copied defensively on the way in, so a caller's own mutable
list cannot reach back into the tree after construction. A `ShipmentBox`
representing a carton can hold `Parcel`s, and a `ShipmentBox` representing
a pallet can hold other `ShipmentBox`es, with no type distinguishing the
two roles anywhere in the code.

The recursion lives in exactly one utility class, `ShipmentNodes`, and
every method there is a pattern-matching `switch` over the sealed
hierarchy:

```java
public static long totalWeightGrams(ShipmentNode node) {
    return switch (node) {
        case Parcel parcel -> parcel.weightGrams();
        case ShipmentBox box -> box.contents().stream().mapToLong(ShipmentNodes::totalWeightGrams).sum();
    };
}
```

Neither branch of that switch asks what kind of child it is looking at
beyond the one pattern match the switch itself performs, and the compiler
rejects the method outright if `ShipmentNode` ever grows a third permitted
type without a case here. `totalValueCents` and `itemCount` are the
identical shape with a different field or a constant `1` in place of
`weightGrams()`. Running the example's demo against a pallet with a carton
that itself holds a nested carton shows the arithmetic working without any
special case: a pallet holding a flat carton and a carton that wraps a
third, deeper carton reports the sum of both branches correctly, because
every branch answered the same three questions the same way.

This is also where Composite pays for itself over the plainer alternative
of a single `List<Parcel>` with no nesting at all. A flat list answers "how
much does this pallet weigh" just as easily as a tree does, right up until
a warehouse needs to consolidate two partial cartons into one without
re-flattening every parcel inside them, or needs to move a whole carton
from one pallet to another as a single unit, or needs the weight of one
carton without summing parcels it never holds. A flat structure answers
the whole pallet's question but not a sub-tree's question, and this
chapter's domain needs the intermediate-level answer just as much as the
top-level one.

## Flyweight: an interning cache for state that never changes

`FlyweightPool<K, V>` is a generic cache. It keeps two separate maps: a
registry of every intrinsic value a program knows how to describe, built
once at construction, and a pool of values actually materialized so far.

```java
public final class FlyweightPool<K, V> {

    private final Map<K, V> registry;
    private final Map<K, V> pool = new ConcurrentHashMap<>();

    public FlyweightPool(Map<K, V> registry) {
        this.registry = Map.copyOf(registry);
    }

    public V get(K key) {
        V registered = registry.get(key);
        if (registered == null) {
            throw new NoSuchElementException("unregistered key: " + key);
        }
        return pool.computeIfAbsent(key, ignoredKey -> registered);
    }
}
```

The distinction between the two maps is the point. A warehouse's full
packaging catalog might list dozens of codes, most of which never touch
any one pallet; materializing a flyweight for every registered code up
front would waste exactly the memory Flyweight exists to save. `get()`
only ever moves a value from the registry into the pool the first time
something asks for it, and `pool.computeIfAbsent(key, ignoredKey ->
registered)` is what makes a concurrent caller safe without a separate lock:
`ConcurrentHashMap` guarantees the supplied function runs at most once per
key, so two threads racing to materialize the same code both end up
holding the identical instance rather than two private copies that merely
look alike.

`PackagingSpecCatalog` and `CountryCatalog` both wrap one `FlyweightPool`,
parameterized over a different value type:

```java
public final class PackagingSpecCatalog {

    private final FlyweightPool<String, PackagingSpec> pool;

    public PackagingSpecCatalog(Map<String, PackagingSpec> registry) {
        this.pool = new FlyweightPool<>(registry);
    }

    public PackagingSpec get(String code) {
        return pool.get(code);
    }

    public int materializedCount() {
        return pool.size();
    }
}
```

`materializedCount()` measures the number of distinct codes a program has
actually shared, not the number of parcels that reference them. Building
sixty more parcels across the same two packaging codes, which the
example's demo does right after building the first few, leaves
`materializedCount()` unchanged at two; only asking for a third, unused
code would grow it.

Returning `PackagingSpec` by reference rather than by value is what makes
the sharing real and checkable rather than assumed. Two separate
`get("STD-BOX")` calls return references that compare equal with `==`,
proving the second call reused the first call's instance instead of
quietly making a private copy that merely looks identical. A Flyweight
implementation that returned a fresh `PackagingSpec` built from the same
fields on every call would compile, pass a test that checks field
equality, and still allocate one object per parcel, defeating the entire
pattern while looking correct from the outside. Testing identity, not just
equality, is what catches that, which is why this chapter's test suite
asserts `isSameAs`, AssertJ's reference-identity assertion, rather than
`isEqualTo`.

## Null Object: a do-nothing implementation instead of a null check

`ShipmentBox` can report an overweight condition, but most of this
chapter's boxes never wire up anything to receive that report. The naive
fix is a null check at the one place `checkWeightLimit()` would call a
handler:

```java
if (alertHandler != null && total > weightLimitGrams) {
    alertHandler.onOverweight(label, total - weightLimitGrams);
}
```

That line is correct, and it is also a decision a reviewer has to
re-verify at every call site that touches `alertHandler`, because nothing
in the type system stops a second method from forgetting the check. Null
Object removes the decision instead of asking every caller to remember it:

```java
public interface ShipmentAlertHandler {
    void onOverweight(String boxLabel, long gramsOverLimit);
}

public enum NullShipmentAlertHandler implements ShipmentAlertHandler {
    INSTANCE;

    @Override
    public void onOverweight(String boxLabel, long gramsOverLimit) {
        // Intentionally does nothing.
    }
}
```

An enum with one constant is the idiomatic Java 25 shape for a type that
must have exactly one instance; the language's own singleton guarantee
replaces a hand-written private constructor and static field, and nothing
elsewhere in the program can construct a second, inconsistent
`NullShipmentAlertHandler`. `ShipmentBox` binds `alertHandler` at
construction, with a secondary constructor defaulting to
`NullShipmentAlertHandler.INSTANCE`:

```java
public ShipmentBox(String label, List<ShipmentNode> contents, long weightLimitGrams) {
    this(label, contents, weightLimitGrams, NullShipmentAlertHandler.INSTANCE);
}
```

`checkWeightLimit()` now calls `alertHandler.onOverweight(...)`
unconditionally, with no `if` anywhere near the handler's presence.
Whether that call does real work or nothing at all depends entirely on
which handler got bound at construction, never on a branch the method body
has to carry. A Mockito test double built for this example's suite proves
both halves of that claim directly: the default handler lets
`checkWeightLimit()` run to completion with no exception and no visible
effect, and a mocked handler receives exactly one call, with the correct
box label and the correct overage, when a real one is bound instead.

## Optional at the boundary, Null Object on the inside

`Optional` is Java's other tool for an absent value, and it removes the
need for a Null Object in plenty of cases: a method that
returns `Optional<Customer>` instead of a nullable `Customer` forces every
caller to deal with absence at the type level, with no separate
`NullCustomer` class required. The place that distinction breaks down is a
field that gets *called*, not just read. Storing `Optional<ShipmentAlertHandler>`
as a field and writing `alertHandler.ifPresent(h -> h.onOverweight(...))`
at every call site reintroduces exactly the branch Null Object was meant
to delete, just spelled with a method reference instead of an `if`. Worse,
a collaborator interface with more than one method turns into a
`Optional<ShipmentAlertHandler>` that every caller has to unwrap
separately for every method it might call, multiplying the same branch
across the whole interface.

The idiomatic middle ground is accepting `Optional` at a boundary and
resolving it immediately into a concrete value. `ShipmentBox.withAlertHandler`
does exactly that:

{% include codetabs.html langs="Optional at the boundary|Null Object inside" %}

```java
// Optional at the boundary: a caller can say "maybe a handler, maybe not"
// in one expression, read naturally at the call site that builds the box.
public static ShipmentBox withAlertHandler(String label, List<ShipmentNode> contents, long weightLimitGrams,
                                            Optional<ShipmentAlertHandler> alertHandler) {
    return new ShipmentBox(label, contents, weightLimitGrams, alertHandler.orElse(NullShipmentAlertHandler.INSTANCE));
}
```
```java
// Null Object inside: once resolved, checkWeightLimit() never sees an
// Optional. There is exactly one field, always bound, always callable.
public void checkWeightLimit() {
    long total = ShipmentNodes.totalWeightGrams(this);
    if (total > weightLimitGrams) {
        alertHandler.onOverweight(label, total - weightLimitGrams);
    }
}
```

`alertHandler.orElse(NullShipmentAlertHandler.INSTANCE)` runs exactly once,
at construction, and nothing downstream of it ever has to call
`isPresent()` again. This is also the shape Joshua Bloch recommends in
*Effective Java*: `Optional` is for a value a caller inspects once, not for
a field a type holds onto and calls repeatedly. Reach for `Optional` when
the absence is a single fact a caller needs to react to once, at a
boundary. Reach for a real Null Object when the collaborator is called
more than once, called from more than one place, or exposes more than one
method, because a Null Object lets every one of those call sites stay
branch-free permanently, where an `Optional` field would make every one of
them pay the unwrapping cost again.

> **AI sidebar.** Ask an LLM pairing partner to "handle the case where
> there's no handler," and a common result reaches for `Optional<T>` as a
> field type by reflex, because it is the shape most recent Java guidance
> recommends for absence. Check the result against the distinction this
> chapter trained: is the `Optional` unwrapped once, at a boundary, into a
> concrete value a collaborator holds from then on, or does it sit on a
> field that gets called from several places, with an `ifPresent` or
> `map` chain repeated at each one. The second shape compiles, reads as
> "modern Java," and still reintroduces the exact branch Null Object
> exists to delete, just hidden inside a lambda instead of an `if`. A
> generated collaborator field typed `Optional<SomeHandler>` that gets
> called more than once in the same class is the sign to ask the model
> directly whether a Null Object, resolved once at construction, would
> remove the repeated unwrapping instead of just relocating it.

## When not to use any of the three

Each of the three patterns in this chapter answers a specific force, and
each one stops paying for itself the moment that force is absent.

Composite earns its sealed interface when a tree can get deep:
cartons inside cartons, or a pallet holding both loose parcels and
sub-cartons in the same shipment. A flat list of parcels with no nesting
on any roadmap does not need `ShipmentNode`; a plain `List<Parcel>` and a
loop that sums `weightGrams()` directly says the same thing with one
interface fewer. Introducing a sealed Composite interface ahead of any
nesting adds a `switch` and a `List<ShipmentNode>` field with nothing
underneath it to justify either cost.

Flyweight pays for itself when the same immutable value is referenced many
times over. A catalog of two packaging codes referenced by a dozen parcels
saves nothing worth the extra indirection; `PackagingSpec` fields could
live directly on `Parcel` with no pool at all, and the two representations
would use roughly the same memory. The pattern starts paying for itself
once reference counts climb into the hundreds or thousands per shared
value, the point at which a warehouse running real volume sits,
and where the memory a naive copy-per-reference design would spend becomes
visible on a profiler rather than theoretical.

Null Object pays for itself when a missing collaborator is a normal,
expected case rather than a programming error. A `ShipmentBox` built
without a real alert handler is routine: most cartons are never
overweight, and most code paths that build one do not care about the
handler at all. Reaching for Null Object to paper over a collaborator that
should never legitimately be absent, for instance a missing payment
gateway a checkout flow cannot run without, trades a loud, early failure
for a silent one; that case calls for an exception or a typed error at
construction, not a Null Object quietly doing nothing where a real failure
belongs. The test is whether "absent" is a valid state of the world the
rest of the program should tolerate, or a bug that should be caught as
close to its source as possible.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this
loop. From the repository root, the aggregator picks up
`examples/17-composite-flyweight-null` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/17-composite-flyweight-null
mvn test
```

Either path runs the same 20 JUnit 5 cases, asserted with AssertJ and
Mockito, including a suite that checks the flyweight pool returns the
identical instance by reference rather than by field equality, and a suite
that checks a real alert handler is reached unconditionally while the
default Null Object is a provably safe no-op. Observed locally on Fedora
with Temurin 25:

```
Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see all three patterns produce real output rather than just reading an
assertion that says they work, run the example's own demo script:

```sh
examples/17-composite-flyweight-null/demo.sh
```

It builds a pallet with a nested carton, reports totals at every level,
looks up the same two packaging codes sixty more times, confirms the
flyweight pool stayed at two descriptors, and checks one carton's weight
limit three times: once with the default Null Object, once with an empty
`Optional` resolved to that same Null Object, and once with a real,
logging handler:

```
== Composite: a mixed tree, one interface ==
CTN-A: 6400g, 2 units
CTN-C: 4800g, 2 units
CTN-B: 4800g, 2 units
PAL-9: 11200g, 4 units
(Pallet's totals are Carton CTN-A's and Carton CTN-B's totals added together,
 and Carton CTN-B's total is Carton CTN-C's total, recursed through two levels.)

== Flyweight: a few descriptors, many parcels ==
Distinct packaging codes registered: 2
Flyweights materialized so far: 2
After 60 more lookups across the same 2 codes, materialized: 2 (still 2)
Two separate get("STD-BOX") calls return the same instance: true

== Null Object: a weight-limit check with no branch for "no handler" ==
(no output above: the default NullShipmentAlertHandler ran and did nothing)
(no output above either: Optional.empty() resolved to the same Null Object)
  Box CTN-A (logged) is 5900g over its weight limit
```

Notice Carton CTN-B's total, 4800 grams and 2 units, is exactly Carton
CTN-C's total passed up one level unchanged, and Pallet PAL-9's total,
11200 grams and 4 units, is Carton CTN-A's and Carton CTN-B's totals added
together. That agreement is `ShipmentNodes`'s recursive switch summing a
mixed tree correctly, an observable fact at the terminal, not just a
passing assertion.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design
  Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480)
  (Addison-Wesley): the original Composite and Flyweight entries, including
  the intrinsic-versus-extrinsic state distinction this chapter's
  `PackagingSpec` and `Parcel` build on. Null Object is not one of the
  original 23 patterns; it was named and popularized afterward as a direct
  answer to the same null-check-at-every-call-site problem this chapter's
  `ShipmentAlertHandler` solves.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991),
  third edition (Addison-Wesley): Item 55 on returning optionals judiciously
  is the direct source for this chapter's guidance on resolving an
  `Optional` at a boundary rather than storing it as a repeatedly-called
  field.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/)
  and [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html):
  the testing idiom behind `examples/17-composite-flyweight-null`'s suite,
  which asserts reference identity for the flyweight pool and verifies a
  mocked collaborator's exact invocation for the Null Object, rather than
  only checking that the build exits zero.
- The full reading list behind this book's opinions lives on the
  [bibliography page]({{ '/bibliography/' | relative_url }}); each citation
  there is a pointer to the full book, not a substitute for reading it.
