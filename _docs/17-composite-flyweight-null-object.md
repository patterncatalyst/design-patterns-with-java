---
title: "Composite, Flyweight, Null Object"
order: 17
layout: tutorial
sectionid: docs
part: "composing-adapting-structure"
description: "Three idiomatic Java ways to keep a recursive shipment tree simple: a sealed ShipmentNode interface that treats one parcel and an entire pallet identically, a FlyweightPool that interns shared PackagingSpec and Country instances across thousands of parcels, and a Null Object that lets a weight-limit check skip every null branch, contrasted with Optional resolved at a construction boundary."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Build a Composite as a sealed interface with record leaves and branches, and recurse over it with an exhaustive pattern-matching `switch`.
- Build a generic Flyweight pool that returns one shared instance per key, and check the sharing by reference identity.
- Build a Null Object as an enum singleton so no call site checks for null.
- Decide when `Optional` at a boundary replaces a Null Object and when a Null Object still reads better.
- Recognize when a `List`, a plain field or an exception is the better answer.

{% include excalidraw.html name="17-composite-flyweight-tree-and-pool" caption="Figure 17.1: a composite shipment tree sharing one interface on the left; a flyweight pool of PackagingSpec instances shared by many parcels on the right." %}

## Why this matters

A pallet is a tree: it holds cartons, and a carton holds parcels or more cartons. The question at every level is the same: how much does this weigh, and how many parcels does it hold. Write one method per level and, the first time a warehouse consolidates two partial cartons into one box, the carton method has to call itself and duplicates the pallet's recursion.

A packaging code such as `STD-BOX` appears on hundreds of parcels a day, and every parcel needs the same tare weight, material and hazmat class. Copying those facts into every parcel is correct and wasteful.

A carton's weight-limit check needs somewhere to report a violation, and usually nothing is wired up to receive it. A null check at every call site works until one call site forgets it.

Composite, Flyweight and Null Object answer those three problems in one shipping domain.

The parcels in `examples/17-composite-flyweight-null` reference a `PackagingSpec`, whose fields never vary between parcels:

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

A `STD-BOX` has 180 grams of tare on every parcel that uses it, which is what makes it safe to share. `HazmatClass` needs no sharing mechanism: an enum constant is already one instance per JVM.

## Pattern: Composite

### The problem

Weighing a parcel, a carton and a pallet with three methods means each one knows what kind of thing it's summing, and nesting a carton in a carton forces recursion into more than one of them.

### The solution

A **Composite** lets a leaf and a group of leaves implement the same interface, so client code treats a single object and a tree of any depth through one call.

Use a Composite when:

- the structure nests to a depth you don't know in advance;
- callers need answers for a sub-tree as well as for the whole.

### How Java models it

`ShipmentNode` is sealed to exactly two types, so the compiler can check dispatch over it for completeness:

```java
public sealed interface ShipmentNode permits Parcel, ShipmentBox {
}
```

`Parcel` is the leaf. It holds the shared `PackagingSpec` and `Country` plus its own weight and declared value:

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

`ShipmentBox` is the branch, and it's one record for both a carton and a pallet. `List.copyOf` keeps a caller's mutable list from reaching into the tree after construction:

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

The recursion lives in one utility class, `ShipmentNodes`, as a pattern-matching `switch` over the sealed hierarchy:

```java
public static long totalWeightGrams(ShipmentNode node) {
    return switch (node) {
        case Parcel parcel -> parcel.weightGrams();
        case ShipmentBox box -> box.contents().stream().mapToLong(ShipmentNodes::totalWeightGrams).sum();
    };
}
```

If `ShipmentNode` gains a third permitted type, this method stops compiling until it has a case for it. `totalValueCents` and `itemCount` have the same shape with a different field, or a constant `1`.

### Why a tree and not a list

A flat `List<Parcel>` answers "what does the pallet weigh" as easily. It can't answer for a sub-tree: the weight of one carton, or moving a whole carton between pallets as a unit, or consolidating two partial cartons without re-flattening them. In the demo, carton `CTN-B` reports exactly the totals of the carton `CTN-C` it wraps, and pallet `PAL-9` reports `CTN-A` plus `CTN-B`.

## Pattern: Flyweight

### The problem

Thousands of parcels each carry a copy of the same immutable packaging description. The copies, not the logic, become the memory cost.

### The solution

A **Flyweight** shares one instance of the intrinsic state that never varies, and leaves the extrinsic state, such as weight and destination, on each object that uses it.

Use a Flyweight when:

- many objects reference the same immutable value;
- reference counts per shared value run into the hundreds or thousands.

### How Java models it

`FlyweightPool<K, V>` keeps two maps: a registry of every value the program can describe, and a pool of the values materialized so far:

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

A packaging catalog may list dozens of codes that never touch a given pallet, so `get()` moves a value into the pool only on first request. `ConcurrentHashMap.computeIfAbsent` applies its function at most once per key, so two threads racing on the same code both get the same instance without a separate lock.

`PackagingSpecCatalog` and `CountryCatalog` each wrap one pool:

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

`materializedCount()` counts distinct shared codes, not parcels. Sixty more lookups across the same two codes leave it at two.

### Check identity, not equality

Two `get("STD-BOX")` calls return references that are `==`. A pool that built a fresh `PackagingSpec` from the same fields on every call would pass an equality check and still allocate one object per parcel. Assert with AssertJ's `isSameAs`, not `isEqualTo`.

## Pattern: Null Object

### The problem

`ShipmentBox` can report an overweight condition, but most boxes have no handler. The obvious fix guards the call:

```java
if (alertHandler != null && total > weightLimitGrams) {
    alertHandler.onOverweight(label, total - weightLimitGrams);
}
```

That line is correct, and nothing stops the next method that touches `alertHandler` from forgetting it.

### The solution

A **Null Object** is a do-nothing implementation of a collaborator's interface that stands in for "no collaborator", so every call site calls it unconditionally.

Use a Null Object when:

- a missing collaborator is a normal, expected state;
- the collaborator is called from more than one place.

### How Java models it

An enum with one constant gives the language's own singleton guarantee, with no private constructor or static field to write:

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

A secondary constructor on `ShipmentBox` binds the default:

```java
public ShipmentBox(String label, List<ShipmentNode> contents, long weightLimitGrams) {
    this(label, contents, weightLimitGrams, NullShipmentAlertHandler.INSTANCE);
}
```

`checkWeightLimit()` calls `alertHandler.onOverweight(...)` with no `if` about the handler. Whether the call does work depends only on which handler was bound at construction. With the default, the check runs with no effect; with a real handler, it receives one call with the box label and the overage.

### Optional at the boundary, Null Object inside

`Optional` replaces a Null Object for a value a caller reads once: a method returning `Optional<Customer>` forces each caller to handle absence. It doesn't work for a field that gets called. An `Optional<ShipmentAlertHandler>` field puts `ifPresent(h -> h.onOverweight(...))` at every call site, the branch Null Object removes, and a collaborator with several methods repeats it for each one.

Accept `Optional` at the boundary and resolve it once. `ShipmentBox.withAlertHandler` does that:

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

`orElse(NullShipmentAlertHandler.INSTANCE)` runs once, at construction, and nothing downstream calls `isPresent()`. Bloch's *Effective Java* (Item 55) recommends `Optional` mainly as a return type and treats an optional field as a design to reconsider.

| Absence is | Use |
| --- | --- |
| One fact a caller reacts to once | `Optional` return value |
| A collaborator called repeatedly, from several places, or with several methods | Null Object bound at construction |
| A caller-supplied maybe | `Optional` parameter resolved with `orElse` to a Null Object |

## When not to use it

- **Composite.** A parcel list that never nests needs no `ShipmentNode`; a `List<Parcel>` and a loop that sums `weightGrams()` says the same with one interface fewer.
- **Flyweight.** Two packaging codes across a dozen parcels save nothing worth the indirection; keep the fields on `Parcel`. The pool pays off once reference counts per value reach the hundreds and the copies show up on a profiler.
- **Null Object.** Don't use one for a collaborator that must never be absent, such as the payment gateway checkout can't run without. A silent no-op replaces a loud early failure; throw at construction instead. Ask whether "absent" is a valid state or a bug.

> **🤖 Explore with your agent**
>
> "In `examples/17-composite-flyweight-null`, keep `ShipmentBox.alertHandler` typed `ShipmentAlertHandler`, never `Optional<ShipmentAlertHandler>`. Default it to `NullShipmentAlertHandler.INSTANCE`, an enum singleton whose `onOverweight` does nothing. Accept `Optional<ShipmentAlertHandler>` only in the static factory `withAlertHandler` and resolve it there with `orElse(NullShipmentAlertHandler.INSTANCE)`. `checkWeightLimit()` must call `alertHandler.onOverweight(label, total - weightLimitGrams)` with no null or presence check."
>
> Check: `mvn -q -f examples/17-composite-flyweight-null/pom.xml test -Dtest='ShipmentBoxNullObjectTest,ShipmentBoxOptionalBoundaryTest'`; reject any `ifPresent` or `!= null` on the handler.

## Build, run, observe

From the repository root, the aggregator builds every example:

```sh
mvn -f examples/pom.xml test
```

Or build this one on its own:

```sh
cd examples/17-composite-flyweight-null
mvn test
```

All 20 tests pass. The demo builds a pallet with a nested carton, reports totals at every level, looks up the same two packaging codes sixty more times, and checks one carton's weight limit with the default Null Object, an empty `Optional` and a logging handler:

```sh
examples/17-composite-flyweight-null/demo.sh
```

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

`CTN-B` carries `CTN-C`'s total up one level unchanged, and `PAL-9` is `CTN-A` plus `CTN-B`.

- **Copy instead of share.** In a scratch copy, make `PackagingSpecCatalog.get` return a new `PackagingSpec` built from the pooled one's fields. The sharing test fails on identity, though every field is equal.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design
  Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480)
  (Addison-Wesley): the original Composite and Flyweight entries, including
  the intrinsic-versus-extrinsic state distinction `PackagingSpec` and
  `Parcel` build on. Null Object is not one of the original 23 patterns; it
  was named and popularized afterward as an answer to the
  null-check-at-every-call-site problem `ShipmentAlertHandler` solves.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991),
  third edition (Addison-Wesley): Item 55 on returning optionals judiciously,
  the source for resolving an `Optional` at a boundary rather than storing
  it as a repeatedly called field.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/)
  and [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html):
  the testing idiom behind `examples/17-composite-flyweight-null`, which
  asserts reference identity for the flyweight pool and verifies a mocked
  collaborator's exact invocation for the Null Object.
- The full reading list lives on the
  [bibliography page]({{ '/bibliography/' | relative_url }}).
