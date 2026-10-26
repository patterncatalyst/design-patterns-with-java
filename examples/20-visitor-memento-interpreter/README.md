# 20 - Visitor, Memento, Interpreter: an export manifest

An export manifest holds a fixed set of parcel kinds, and the operations a
checkout, a customs desk, and a compliance screen each run over that set
keep growing. This example builds that fixed hierarchy once and adds three
operations over it with no change to any of its four record types, then
pairs it with a draft manifest that can roll back a batch of edits in one
call and a tiny expression grammar whose nesting decides a surcharge.

## The domain

- `ManifestEntry`: a sealed interface over exactly four record kinds,
  `StandardParcel`, `InternationalParcel`, `HazardousParcel`, and
  `OversizedParcel`. Each carries an `id`, a `declaredValue`, and a
  `weightKg`; the international, hazardous, and oversized kinds carry
  whatever extra data their own handling requires.
- `Money` tracks cents as a `long`, never a `double`, so a quote is exact.

## Visitor: three operations, no accept() anywhere

`ManifestOperations` holds three static methods, each dispatching on an
exhaustive pattern-matching `switch` over `ManifestEntry`:

1. `shippingCost(ManifestEntry)` prices a base per-kilogram rate plus
   whatever surcharge each kind adds: a flat customs brokerage fee, a
   per-kilogram hazmat handling fee, or a volumetric dimensional surcharge.
2. `customsSummary(ManifestEntry)` renders what a customs or routing desk
   needs to know about one entry.
3. `validate(ManifestEntry)` collects every reason an entry cannot be
   submitted, in one list, rather than throwing on the first one found.

None of the four `ManifestEntry` records implements a visitor interface or
declares an `accept()` method. Adding `validate()` after `shippingCost()`
and `customsSummary()` already shipped touched exactly one file,
`ManifestOperations.java`; every switch has no `default` branch, so a fifth
parcel kind added to `ManifestEntry`'s `permits` clause fails every switch
in this file at compile time until each one accounts for it.

## Memento: a draft manifest that rolls back in one call

`ManifestDraft` is the Originator: a draft name, a growing list of
`ManifestEntry` line items, and a `submitted` flag. Its nested `Snapshot`
is a package-private record, declared with no access modifier, so its
canonical constructor and accessors are visible only inside this package.
`save()` and `restore(Snapshot)` are the only supported way to produce or
apply one. A caller outside this package can still hold what `save()`
returns, through `var`, without ever being able to name the type
`ManifestDraft.Snapshot` or read a field off it directly.

`restore()` replaces the whole draft, `draftName`, every `ManifestEntry`,
and `submitted`, in one call, regardless of how many `rename()`,
`addEntry()`, `removeLastEntry()`, or `submit()` calls happened since
`save()` produced the snapshot it is given.

## Interpreter: a surcharge expression whose nesting decides the answer

`SurchargeExpr` is a sealed interface over four kinds: `Literal`, `Add`,
`Multiply` (an expression scaled by a dimensionless factor), and
`IfWeightAbove` (a weight-conditional choice between two expressions).
`SurchargeExprs.evaluateCents()` and `SurchargeExprs.describe()` walk that
grammar with record-pattern switches, deconstructing each case directly
into its components rather than binding the whole record and calling an
accessor.

The same three pieces, a $5.00 handling fee, a $10.00 overweight fee gated
on a 20kg threshold, and a 1.08 fuel factor, nested two different ways
evaluate to two different amounts for the identical 22kg shipment: $16.20
when the fuel factor scales the whole sum, $15.80 when it scales only the
overweight fee.

`ShippingDemo.java` exercises all three patterns against one manifest.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

This example is standalone-buildable from this directory:

```sh
cd examples/20-visitor-memento-interpreter
mvn test
```

It is also picked up by the repo root's aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 25 JUnit 5 cases (AssertJ assertions), including
a case that evaluates two differently-nested `SurchargeExpr` trees built
from identical pieces and asserts they produce different results, not just
that each one evaluates without throwing.

To see all three patterns run against one manifest rather than just reading
an assertion that says they work, run the demo:

```sh
./demo.sh
```

Expected output:

```
== Visitor: three operations over a fixed ManifestEntry hierarchy ==
PCL-100: cost=$1.60, customs=PCL-100: domestic parcel, no customs filing required
PCL-200: cost=$18.00, customs=PCL-200: customs code 8471.30, origin DE, brokerage required
PCL-300: cost=$23.75, customs=PCL-300: hazmat class Class 9 (UN3481), placard required, domestic handling only
PCL-400: cost=$14.40, customs=PCL-400: oversized parcel, no customs filing, freight-desk routing required

== Visitor: validation collects every issue, not just the first ==
Issues for PCL-999: [PCL-999: weight must be positive, was 0.00kg, PCL-999: customs code is required, PCL-999: origin country is required]

== Memento: editing a draft, then restoring a prior snapshot ==
Before edits:  name=Q4-export-batch entries=1 submitted=false
After edits:   name=DRAFT-EDIT entries=2 submitted=true
After restore: name=Q4-export-batch entries=1 submitted=false

== Interpreter: nesting changes the evaluated surcharge ==
(($5.00 + (if weight > 20.0kg then $10.00 else $0.00)) * 1.08) -> $16.20
($5.00 + (if weight > 20.0kg then ($10.00 * 1.08) else $0.00)) -> $15.80
```

The final block's two different totals, $16.20 against $15.80, computed
from the identical three pieces nested two different ways, is nesting
deciding an interpreted grammar's answer, visible at the terminal rather
than only in an assertion.

## Files

```
examples/20-visitor-memento-interpreter/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run ShippingDemo
  src/main/java/com/patterncatalyst/designpatterns/ch20/shipping/
    Money.java                    # exact integer cents
    ManifestEntry.java              # sealed interface, the fixed structure
    StandardParcel.java              # Visitor node kind one
    InternationalParcel.java          # Visitor node kind two
    HazardousParcel.java               # Visitor node kind three
    OversizedParcel.java                # Visitor node kind four
    ManifestOperations.java              # three operations, exhaustive switches
    ManifestDraft.java                    # Memento Originator + Snapshot
    SurchargeExpr.java                     # sealed interface, the grammar
    Literal.java                            # grammar leaf
    Add.java                                 # grammar combinator
    Multiply.java                             # grammar combinator
    IfWeightAbove.java                         # grammar combinator
    SurchargeExprs.java                         # evaluate + describe
    ShippingDemo.java                            # exercises all three patterns
  src/test/java/com/patterncatalyst/designpatterns/ch20/shipping/
    ManifestOperationsTest.java    # every operation, every entry kind
    ManifestDraftTest.java          # save/restore round trips, immutability
    SurchargeExprTest.java           # evaluation, nesting, description
```
