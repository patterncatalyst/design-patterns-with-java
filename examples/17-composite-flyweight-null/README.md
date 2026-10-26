# 17 - Composite, Flyweight, Null Object: a shipment container tree

A warehouse packs a pallet from cartons, a carton from parcels or from more
cartons, and shares the packaging and destination facts that never change
between one parcel and the next. This example builds that domain three
idiomatic Java 25 ways: a sealed Composite interface that treats a leaf and
a branch identically, a Flyweight pool that interns shared descriptors, and
a Null Object that removes a null check from a weight-limit call site,
contrasted with Optional resolved at a construction boundary.

## The domain

- `ShipmentNode` is a sealed interface, `permits Parcel, ShipmentBox`.
  `Parcel` is the leaf; `ShipmentBox` is the branch, holding any mix of
  parcels and nested boxes.
- `ShipmentNodes` sums weight, value, and item count with a
  pattern-matching `switch` over the sealed hierarchy. The compiler checks
  the switch for exhaustiveness against the two permitted types, so a third
  subtype added later without a case for it fails to compile.
- `PackagingSpec` (packaging code, material, tare weight, `HazmatClass`)
  and `Country` (ISO code, name, customs flag) are intrinsic state shared
  by every parcel packed or shipped the same way. `FlyweightPool<K, V>` is
  a generic interning cache; `PackagingSpecCatalog` and `CountryCatalog`
  both wrap one.
- `HazmatClass` is a plain enum. An enum constant is already a Flyweight
  the language gives you for free, which is why it needs no catalog at all,
  unlike `PackagingSpec` and `Country`, whose sets are too large to
  hand-enumerate as constants.
- `ShipmentAlertHandler` is the collaborator a box tells about an
  overweight condition. `NullShipmentAlertHandler` is a one-constant enum
  implementing it as a do-nothing Null Object.

## Composite, Flyweight, and Null Object in this example

1. **Composite** (`ShipmentNode.java`, `Parcel.java`, `ShipmentBox.java`,
   `ShipmentNodes.java`): a pallet holding a flat carton and a carton that
   wraps a deeper nested carton reports correct totals at every level, with
   `ShipmentNodes`'s three static methods never asking what kind of node
   they are standing in before recursing.
2. **Flyweight** (`FlyweightPool.java`, `PackagingSpecCatalog.java`,
   `CountryCatalog.java`): two packaging codes and two destination
   countries, referenced by dozens of parcels, materialize as exactly two
   `PackagingSpec` instances and two `Country` instances. Two separate
   `get("STD-BOX")` calls return the identical reference, proven with a
   reference-identity assertion, not field equality.
3. **Null Object** (`ShipmentAlertHandler.java`,
   `NullShipmentAlertHandler.java`, `ShipmentBox.java`):
   `ShipmentBox.checkWeightLimit()` calls `alertHandler.onOverweight(...)`
   unconditionally. The three-argument constructor binds the Null Object by
   default; `withAlertHandler(...)` accepts an `Optional<ShipmentAlertHandler>`
   at the construction boundary and resolves it immediately, so the
   `alertHandler` field itself is never Optional and the call site still
   has nothing to branch on.

`ShipmentDemo.java` exercises all three against one small shipment and
prints each result.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

This example is standalone-buildable from this directory:

```sh
cd examples/17-composite-flyweight-null
mvn test
```

It is also picked up by the repo root's aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 20 JUnit 5 cases (AssertJ assertions, Mockito test
doubles for the alert handler), including a suite that asserts the
composite sums a two-level nested tree correctly, a suite that asserts the
flyweight pool returns the identical instance by reference, and a suite
that asserts the Null Object is a safe no-op while a real, injected handler
is still reached unconditionally.

To see the three patterns produce real output rather than just reading an
assertion that says they work, run the demo:

```sh
./demo.sh
```

Expected output:

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
mixed tree correctly, observable at the terminal rather than only inside a
passing assertion.

## Files

```
examples/17-composite-flyweight-null/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run ShipmentDemo
  src/main/java/com/patterncatalyst/designpatterns/ch17/shipping/
    HazmatClass.java              # a closed set; already a Flyweight for free
    Country.java                   # intrinsic destination state (record)
    PackagingSpec.java              # intrinsic packaging state (record)
    FlyweightPool.java                # generic interning cache
    PackagingSpecCatalog.java         # Flyweight catalog over PackagingSpec
    CountryCatalog.java                # Flyweight catalog over Country
    ShipmentNode.java                   # sealed Composite interface
    Parcel.java                          # Composite leaf
    ShipmentBox.java                      # Composite branch + Null Object binding
    ShipmentNodes.java                     # recursive switch: weight/value/count
    ShipmentAlertHandler.java               # the Null Object's contract
    NullShipmentAlertHandler.java            # enum singleton, do-nothing
    ShipmentDemo.java                         # exercises all three patterns
  src/test/java/com/patterncatalyst/designpatterns/ch17/shipping/
    ShipmentNodesCompositeTest.java  # leaf, nested branch, mixed tree totals
    FlyweightPoolTest.java            # identity, pool growth, unregistered key
    PackagingSpecCatalogTest.java      # catalog-level sharing
    CountryCatalogTest.java             # catalog-level sharing
    ShipmentBoxNullObjectTest.java       # default no-op vs. a real handler
    ShipmentBoxOptionalBoundaryTest.java  # Optional resolved at construction
    ParcelTest.java                        # leaf validation edge cases
    ShipmentBoxContentsTest.java            # defensive copy, limit validation
```
