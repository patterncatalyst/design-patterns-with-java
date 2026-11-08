---
title: "Visitor, Memento, and Interpreter"
order: 20
layout: tutorial
sectionid: docs
part: "coordinating-traversing"
description: "Three idiomatic Java patterns for a fixed shipping-manifest hierarchy: Visitor expressed as a sealed interface and an exhaustive pattern-matching switch that adds operations with no accept() method anywhere, Memento as a package-private snapshot record that lets a draft manifest roll back to an earlier state in one call, and Interpreter as a small sealed surcharge-expression grammar whose nesting decides the evaluated result."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Write Visitor as a sealed interface and an exhaustive pattern-matching `switch`, and add an operation without touching the types it dispatches over.
- Build a Memento whose snapshot type is package-private, so a caretaker can hold and return it but not read it.
- Build an Interpreter as a small sealed expression grammar evaluated by a record-pattern `switch`, and show that nesting decides the result.
- Choose between a type pattern and a record pattern in a `switch` arm.
- Recognize when the surrounding code has none of these problems.

## Why this matters

An export manifest carries four parcel kinds: domestic, international with a customs code, hazardous with a placard, and oversized for the freight desk. That set barely changes. What keeps arriving is new questions about the same four kinds: what does it cost to ship, what does customs need to know, what's wrong with this entry before it's submitted. Visitor adds each operation without reopening the types.

A merchant editing a draft manifest names the batch, adds entries, drops one, flips it to submitted. If the last few edits were wrong, discarding everything since a known point is faster and safer than reversing each edit. Memento captures the whole state in a form the caller can hold but not read.

A surcharge is a handling fee, an overweight fee gated on a threshold and a fuel factor, nested together. Interpreter evaluates that small grammar.

Visitor and Interpreter both walk a sealed hierarchy with a pattern-matching switch. Visitor adds operations over a structure; Interpreter computes what a structure already means. Memento snapshots state instead of walking it.

{% include excalidraw.html name="20-visitor-switch-operations" caption="Figure 20.1: ManifestEntry's four fixed kinds on the left, three independent operations on the right, each one its own exhaustive switch with no accept() method anywhere on the node types." %}

## Pattern: Visitor

### The problem

`ManifestEntry` in `examples/20-visitor-memento-interpreter` is sealed over four records: `StandardParcel`, `InternationalParcel`, `HazardousParcel` and `OversizedParcel`. Each has an `id`, a `declaredValue` and a `weightKg`; the others add a customs code and origin country, a hazard class and UN number, or three dimensions:

```java
public sealed interface ManifestEntry
        permits StandardParcel, InternationalParcel, HazardousParcel, OversizedParcel {

    String id();

    Money declaredValue();

    double weightKg();
}
```

`Money` holds cents as a `long`, so costs compare exactly. The records don't validate weight or blank fields in their constructors, because a draft entry may start with a placeholder weight, and validation is itself one of the operations. Putting cost, customs and validation methods on every record means editing all four for each new question.

### The solution

A **Visitor** represents an operation over the elements of a structure as a separate unit, so new operations are added without changing the element types.

Use a Visitor when:

- the set of types is fixed and known;
- operations over those types keep multiplying.

### How Java models it

The GoF Visitor puts an `accept()` method on every node and a `visit()` overload per type, so two virtual calls (double dispatch) select the behavior. A sealed interface and an exhaustive `switch` replace both. `ManifestOperations` holds one static method per operation, with no visitor interface and no `accept()`:

```java
public static Money shippingCost(ManifestEntry entry) {
    return switch (entry) {
        case StandardParcel p -> baseRate(p.weightKg());
        case InternationalParcel p -> baseRate(p.weightKg()).plus(Money.ofCents(CUSTOMS_BROKERAGE_FLAT_CENTS));
        case HazardousParcel p ->
                baseRate(p.weightKg()).plus(Money.ofCents(Math.round(p.weightKg() * HAZMAT_HANDLING_CENTS_PER_KG)));
        case OversizedParcel p -> baseRate(p.weightKg()).plus(dimensionalSurcharge(p));
    };
}
```

The `switch` dispatches on the runtime type directly. With no `default` branch and a `permits` clause naming four types, `javac` refuses to compile unless all four are covered. Add a fifth record to `permits` and every switch in `ManifestOperations` stops compiling until it has a case: the completeness check a visitor interface gets from abstract `visit()` methods, without the second hierarchy.

The second operation is one new method and no change to any record:

```java
public static String customsSummary(ManifestEntry entry) {
    return switch (entry) {
        case StandardParcel p -> "%s: domestic parcel, no customs filing required".formatted(p.id());
        case InternationalParcel p -> "%s: customs code %s, origin %s, brokerage required"
                .formatted(p.id(), p.customsCode(), p.originCountry());
        case HazardousParcel p -> "%s: hazmat class %s (UN%s), placard required, domestic handling only"
                .formatted(p.id(), p.hazardClass(), p.unNumber());
        case OversizedParcel p -> "%s: oversized parcel, no customs filing, freight-desk routing required"
                .formatted(p.id());
    };
}
```

### Validation as an operation

A compact constructor that rejected a blank `customsCode` could report only the first problem. A merchant wants every reason at once, so `validate()` collects issues into a list, checking the shared weight rule before a `switch` checks each kind:

```java
public static List<String> validate(ManifestEntry entry) {
    List<String> issues = new ArrayList<>();
    if (entry.weightKg() <= 0.0) {
        issues.add("%s: weight must be positive, was %.2fkg".formatted(entry.id(), entry.weightKg()));
    }
    switch (entry) {
        case StandardParcel ignored -> { }
        case InternationalParcel p -> {
            if (p.customsCode().isBlank()) {
                issues.add("%s: customs code is required".formatted(p.id()));
            }
            if (p.originCountry().isBlank()) {
                issues.add("%s: origin country is required".formatted(p.id()));
            }
        }
        case HazardousParcel p -> { /* checks hazardClass and unNumber the same way */ }
        case OversizedParcel p -> { /* checks all three dimensions are positive */ }
    }
    return List.copyOf(issues);
}
```

An `InternationalParcel` with zero weight and both required fields blank reports three issues from one call. The four records haven't changed since they were declared.

## Pattern: Interpreter

### The problem

A surcharge combines fixed fees, a weight threshold and a multiplier. The same three pieces combined differently give different totals, and the code has to compute the right one for each combination.

### The solution

An **Interpreter** defines a representation for a small grammar and evaluates sentences in it by walking that representation.

Use an Interpreter when:

- the grammar is small and stable;
- expressions are built in code at a few call sites, not parsed from user text.

### How Java models it

`SurchargeExpr` is sealed over four records: `Literal` (cents), `Add` of two expressions, `Multiply` scaling one expression by a dimensionless factor, and `IfWeightAbove`, which picks one of two expressions by a threshold:

```java
public sealed interface SurchargeExpr permits Literal, Add, Multiply, IfWeightAbove {
}
```

`Multiply` takes a `double`, not a second expression, because cents times cents is a unit nothing in the domain means. `SurchargeExprs.evaluateCents()` walks the tree with record patterns that deconstruct each case's components in the `case` line:

```java
public static long evaluateCents(SurchargeExpr expr, double weightKg) {
    return switch (expr) {
        case Literal(long amountCents) -> amountCents;
        case Add(var left, var right) -> evaluateCents(left, weightKg) + evaluateCents(right, weightKg);
        case Multiply(var inner, var factor) -> Math.round(evaluateCents(inner, weightKg) * factor);
        case IfWeightAbove(var threshold, var whenAbove, var whenAtOrBelow) ->
                weightKg > threshold ? evaluateCents(whenAbove, weightKg) : evaluateCents(whenAtOrBelow, weightKg);
    };
}
```

### Type pattern or record pattern

Both forms are exhaustive and compiler-checked:

{% include codetabs.html langs="Type pattern|Record pattern" %}

```java
// Type pattern: binds the whole matched value to one name, then reads
// whichever components the arm needs through an accessor call.
case HazardousParcel p -> "%s: hazmat class %s (UN%s), placard required, domestic handling only"
        .formatted(p.id(), p.hazardClass(), p.unNumber());
```
```java
// Record pattern: deconstructs the matched value's components directly
// into named locals; the arm never calls an accessor at all.
case Multiply(var inner, var factor) -> Math.round(evaluateCents(inner, weightKg) * factor);
```

| Pattern | Reads better when |
| --- | --- |
| Type pattern (`case HazardousParcel p`) | The arm reads several components off one bound name, as `p.id()`, `p.hazardClass()` and `p.unNumber()` do. |
| Record pattern (`case Add(var left, var right)`) | The arm uses every component at once, especially in a recursive case. |

### Nesting decides the result

The same three pieces, a $5.00 handling fee, a $10.00 overweight fee above 20 kg and a 1.08 fuel factor, nested two ways:

```java
SurchargeExpr fuelOnHandling = new Multiply(
        new Add(new Literal(500), new IfWeightAbove(20.0, new Literal(1_000), new Literal(0))),
        1.08);
SurchargeExpr handlingOnFuel = new Add(
        new Literal(500),
        new IfWeightAbove(20.0, new Multiply(new Literal(1_000), 1.08), new Literal(0)));
```

For a 22 kg shipment, `fuelOnHandling` scales the whole $15.00 and gives $16.20; `handlingOnFuel` scales only the overweight fee and gives $15.80. Precedence is the tree's shape, so building the tree directly needs no parser.

## Pattern: Memento

### The problem

Undoing a batch of edits to a `ManifestDraft` field by field is slow and error-prone, and exposing the draft's internals to an undo stack lets that stack depend on them.

### The solution

A **Memento** captures an object's internal state in an opaque value that a caretaker stores and later hands back to restore it, without seeing what it contains.

Use a Memento when:

- a caller needs to discard a whole batch of changes since a known point;
- the state shouldn't be readable by whoever holds the snapshot.

### How Java models it

`ManifestDraft` is the originator: a name, a list of entries and a `submitted` flag. Its nested `Snapshot` is the memento, declared with no access modifier:

```java
public final class ManifestDraft {

    /* ... draftName, entries, submitted, rename(), addEntry(), removeLastEntry(), submit() ... */

    record Snapshot(String draftName, List<ManifestEntry> entries, boolean submitted) {
    }

    public Snapshot save() {
        return new Snapshot(draftName, List.copyOf(entries), submitted);
    }

    public void restore(Snapshot snapshot) {
        this.draftName = snapshot.draftName();
        this.entries.clear();
        this.entries.addAll(snapshot.entries());
        this.submitted = snapshot.submitted();
    }
}
```

A nested type with no modifier is package-private. Code in another package can still write `var s = draft.save();` and pass `s` to `restore()`, which compiles, but it can't write the type `ManifestDraft.Snapshot`, and calling `s.entries()` fails with "is defined in an inaccessible class or interface". Java's package boundary does the hiding that a friend declaration does elsewhere. All of the demo's callers share `ManifestDraft`'s package, so the hiding matters for code outside it, such as an admin-screen controller.

`save()` copies `entries` with `List.copyOf()`. Without the copy, the snapshot would alias the live list, and a later `addEntry()` would change what a saved snapshot holds. No deeper copy is needed, because every `ManifestEntry` is an immutable record. `restore()` replaces the name, the entries and the flag in one call, however many `rename()`, `addEntry()`, `removeLastEntry()` or `submit()` calls happened since `save()`.

## When not to use it

- **Visitor.** One operation in one place needs a few `if`/`else if` branches, not a sealed hierarchy and a utility class. The opposite trap is an open set of kinds, extended by plugins or third parties you never recompile against: `permits` requires every implementor to be compiled together, so use an ordinary interface without `permits`. A classic `accept()`/`visit()` visitor fits that open case.
- **Interpreter.** A grammar read from untrusted text, such as a surcharge formula a merchant types, needs tokenizing, precedence, associativity and error positions. Use an established parsing library or parser generator rather than growing the tree-building helpers into a parser.
- **Memento.** An object with no multi-step editing workflow needs no `Snapshot`. Cloning a whole manifest on every keystroke to undo one field costs far more than a setter that remembers the previous value. Decide whether the caller reverts one field or discards a batch.

> **🤖 Explore with your agent**
>
> "In `examples/20-visitor-memento-interpreter`, add a fourth operation over `ManifestEntry`, `insuranceRequired(ManifestEntry)`, that returns true when `declaredValue` exceeds $1,000.00 or the entry is a `HazardousParcel`. Write it as a static method on `ManifestOperations` with an exhaustive pattern-matching `switch` and no `default` branch. Don't add a visitor interface or an `accept()` method, and don't change any of the four records. Add tests for it in `ManifestOperationsTest`, one per parcel kind."
>
> Check: `mvn -q -f examples/20-visitor-memento-interpreter/pom.xml test -Dtest=ManifestOperationsTest`, and confirm none of the four record files changed.

## Build, run, observe

From the repository root, the aggregator builds every example:

```sh
mvn -f examples/pom.xml test
```

Or build this one on its own:

```sh
cd examples/20-visitor-memento-interpreter
mvn test
```

All 25 tests pass. The demo prices and summarizes a four-entry manifest, validates an entry missing required fields, edits and restores a `ManifestDraft`, and evaluates the two surcharge trees:

```sh
examples/20-visitor-memento-interpreter/demo.sh
```

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

One rejected entry lists all three issues. The draft returns to `entries=1` and `submitted=false` in one `restore()` call. The same three pieces give $16.20 and $15.80.

- **Alias instead of copy.** In a scratch copy, pass `entries` to the `Snapshot` in `save()` without `List.copyOf()`. Three snapshot tests fail, because later edits to the draft now show through a held snapshot.

## For deeper coverage

Visitor, Interpreter, and Memento are three of the last four patterns in
the Gang of Four's original catalogue.

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480/9780321700698) (Addison-Wesley): the original Visitor, Interpreter, and Memento entries, including the "represent an operation to be performed on the elements of an object structure," "given a language, define a representation for its grammar," and "capture and externalize an object's internal state" intent statements the exhaustive switches, expression grammar, and package-private snapshot build on.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 17 on minimizing mutability is the idiom behind every `ManifestEntry` and `SurchargeExpr` record, and the reason `ManifestDraft.save()` copies its entry list with `List.copyOf()` rather than aliasing it.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/20-visitor-memento-interpreter`, which asserts that two differently nested expressions evaluate to different totals and that a restored snapshot reproduces exact prior state.
- The full reading list lives on the [bibliography page]({{ '/bibliography/' | relative_url }}).
