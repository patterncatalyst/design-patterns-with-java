---
title: "Visitor, Memento, and Interpreter"
order: 20
layout: tutorial
sectionid: docs
part: "coordinating-traversing"
description: "Three idiomatic Java 25 patterns for a fixed shipping-manifest hierarchy: Visitor expressed as a sealed interface and an exhaustive pattern-matching switch that adds operations with no accept() method anywhere, Memento as a package-private snapshot record that lets a draft manifest roll back to an earlier state in one call, and Interpreter as a small sealed surcharge-expression grammar whose nesting decides the evaluated result."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Name the force Visitor answers: a fixed set of types that rarely grows a
  new member, paired with a growing set of operations that each need to
  touch every member, without putting every operation inside the types
  themselves.
- Build Visitor as a sealed interface and an exhaustive pattern-matching
  `switch`, and show that a third operation ships with no change to any
  type the switch dispatches over.
- Build a Memento whose snapshot type is visible only inside its own
  package, so a caretaker can hold what it returns without being able to
  read a field out of it, and restore a whole object's state in one call.
- Build an Interpreter as a small sealed expression grammar evaluated by a
  record-pattern `switch`, and show that how its pieces are nested decides
  the evaluated result, not just which pieces are present.
- Compare a type-pattern `switch` against a record-pattern `switch` over
  two different sealed hierarchies in this chapter, and say when
  deconstructing a record's components directly reads better than binding
  the whole record and calling an accessor.
- Recognize when any of the three patterns is answering a force the
  surrounding code does not have, and say what to write instead.

## Why this matters

An export manifest that leaves a warehouse carries a handful of parcel
kinds: a plain domestic parcel, one crossing a border with a customs code
attached, one flagged hazardous with a placard requirement, one too large
for a standard belt and routed through a freight desk instead. That set of
kinds barely changes. A new carrier contract does not usually invent a
fifth kind of parcel; it adds a new question to ask about the four kinds
that already exist. First it was "how much does this cost to ship." Then a
compliance team asked for "what does a customs desk need to know about
this." Then a merchant's draft screen asked for "what is wrong with this
entry, all at once, before it gets submitted." Each question is a new
operation over the same fixed structure, and the Gang of Four's Visitor
pattern names exactly this force: a structure that stays closed, and an
open-ended list of operations that each need to see every member of it,
written so a new operation never has to reopen the types it operates over.

A merchant editing a draft manifest in an admin screen runs into a
different-shaped problem. Building a manifest takes several steps: name the
batch, add an entry, add another, maybe drop one that turned out wrong,
flip a submitted flag when it is ready to go. If step four was a mistake,
undoing it by editing fields back to where they were is slower and more
error-prone than discarding everything since step three and starting that
step over. That is Memento's force: capture an object's entire state in a
form a caller can hold and hand back later, without exposing what that
state actually contains, so a whole batch of changes can be thrown away in
one call instead of reversed one edit at a time.

Interpreter answers a third, related question over a smaller and more
arithmetic grammar: given a handling fee, an overweight surcharge gated on
a threshold, and a fuel factor, what does the whole surcharge expression
evaluate to, once those pieces are nested together. Pairing Visitor,
Memento, and Interpreter in one chapter is not an arbitrary grouping of
leftover Gang of Four entries. Visitor and Interpreter both walk a sealed
hierarchy with a pattern-matching switch, close enough in mechanism that
telling them apart by what each one is *for*, adding an operation over a
structure, versus evaluating a grammar that structure already represents,
is itself the discipline this chapter trains. Memento sits beside them as a
different force, snapshotting state rather than walking it, that this same
export-manifest domain also needs.

{% include excalidraw.html name="20-visitor-switch-operations" caption="Figure 20.1: ManifestEntry's four fixed kinds on the left, three independent operations on the right, each one its own exhaustive switch with no accept() method anywhere on the node types." %}

## The domain: a fixed export-manifest hierarchy

The example behind this chapter lives in `examples/20-visitor-memento-interpreter`.
`ManifestEntry` is a sealed interface over exactly four record kinds:
`StandardParcel`, `InternationalParcel`, `HazardousParcel`, and
`OversizedParcel`. Every kind carries an `id`, a `declaredValue`, and a
`weightKg`; the other three carry whatever extra data their own handling
needs, a customs code and an origin country, a hazard class and a UN
number, or three dimensions:

```java
public sealed interface ManifestEntry
        permits StandardParcel, InternationalParcel, HazardousParcel, OversizedParcel {

    String id();

    Money declaredValue();

    double weightKg();
}
```

`Money` tracks cents as a `long`, never a `double`, the same discipline
every shipping-domain example in this book keeps, so a shipping cost
compares exactly in a test assertion with no epsilon anywhere. None of the
four implementing records validates `weightKg` or a business rule like a
blank customs code in its compact constructor. That omission is
deliberate: a draft entry can legitimately start with a placeholder weight
before a scale reads it, and the validation this chapter's Visitor
operation performs needs real invalid states to find, not states a
constructor already refuses to let exist.

## Visitor: operations as an exhaustive switch, not an accept() method

The Gang of Four's original Visitor pairs a visitor interface with an
`accept()` method on every node, so a first virtual call resolves the
node's exact type and a second, already-overloaded call resolves the
operation's behavior for that type, a mechanism usually called double
dispatch. Java 25 code answers the identical force without either piece.
`ManifestOperations` is a utility class holding three static methods, each
one dispatching on an exhaustive pattern-matching `switch` over
`ManifestEntry`, with no visitor interface declared anywhere and no
`accept()` method on any of the four record types:

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

`switch` resolves which arm runs by checking `entry`'s runtime type against
each `case` pattern directly; there is no `visit()` overload set and no
intermediate call that needs a node to know how to dispatch itself. The
compiler is doing the same completeness work a Gang of Four visitor
interface does by declaring every `visit()` overload abstract: because
`ManifestEntry`'s `permits` clause names exactly four types and this
`switch` has no `default` branch, `javac` refuses to compile unless all
four are covered. Add a fifth record to `permits` later and every switch in
`ManifestOperations.java` fails to compile until it grows a matching case,
the same guarantee a Gang of Four `RuleVisitor` interface gets from
declaring its `visit()` methods pure virtual, with no interface, no
`accept()`, and no second class hierarchy to maintain alongside the first.

`customsSummary()` is the second operation, built the same way, with one
new static method and zero changes to `StandardParcel`,
`InternationalParcel`, `HazardousParcel`, or `OversizedParcel`:

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

`validate()` is the third operation, and it does more than restate
`shippingCost()` and `customsSummary()` with different logic, because of
what it returns.
A compact record constructor that rejected a blank `customsCode` the
moment one was supplied could only ever report the first problem it found.
A merchant staring at a rejected manifest wants every reason it was
rejected at once, so `validate()` collects issues into a list instead of
throwing on the first one, checking the shared `weightKg` rule before a
second `switch` checks whatever is specific to each kind:

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

An `InternationalParcel` built with a zero weight and both required fields
blank reports all three problems from one call, in the order a merchant
would want to fix them, not whichever one a constructor happened to notice
first. Three operations now exist over `ManifestEntry`, written across
three separate commits in three separate contexts, and `StandardParcel`
through `OversizedParcel` have not changed since the day they were first
declared.

> **AI sidebar.** Ask an LLM pairing partner to "add an operation that
> works over every kind in this hierarchy" and a common first answer,
> reached for by reflex because it is the shape that appears in the most
> textbooks, is the Gang of Four's classic Visitor: a new `ManifestVisitor`
> interface, an `accept(ManifestVisitor visitor)` method added to
> `ManifestEntry` and every record that implements it, and a `visit()`
> overload per concrete type. That shape earns its cost when the set of
> node kinds stays open, for instance a plugin system where a third
> party can register a node kind this codebase has never seen, because an
> interface-based visitor needs no `permits` clause and no recompile to
> accept one. It is the wrong shape for a fixed, known set of four parcel
> kinds that changes through a deliberate code review, not through runtime
> registration. Before accepting a generated `accept()`-and-`visit()`
> scaffold, check whether `ManifestEntry` could be declared `sealed` at
> all; if the full set of implementors is known today and changes rarely,
> a pattern-matching `switch` gets the identical completeness guarantee
> with no second class hierarchy, no `accept()` boilerplate on types that
> otherwise have none, and one fewer file per operation. A generated answer
> that reaches for `accept()` over a `sealed` set with no plugin
> requirement anywhere in the ticket is the clearest sign the model matched
> the pattern's name to its most common textbook shape instead of reading
> the forces first.

## Interpreter: a surcharge grammar whose nesting decides the answer

Interpreter is not a third way to write Visitor's operations; it names a
different force. Visitor's question is "what operations can be added over
this fixed structure." Interpreter's question is "given that a structure
already represents a small grammar, what does walking it compute."
`SurchargeExpr` is a sealed interface over four kinds: a fixed `Literal`
amount in cents, an `Add` of two expressions, a `Multiply` that scales one
expression by a dimensionless factor such as a fuel surcharge, and an
`IfWeightAbove` that picks one of two expressions based on a weight
threshold:

```java
public sealed interface SurchargeExpr permits Literal, Add, Multiply, IfWeightAbove {
}
```

`Multiply` takes one `SurchargeExpr` and a plain `double` factor rather
than two expressions, because multiplying two cents amounts together
produces a unit, cents squared, nothing in this domain means; a surcharge
expression can only ever be scaled by a dimensionless number, never
multiplied by another amount of money. `SurchargeExprs.evaluateCents()`
walks the grammar with a `switch` that deconstructs each case's components
directly through a record pattern, rather than binding the whole record
and calling an accessor on it:

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

`Literal(long amountCents)` pulls the record's single component straight
into a local variable named in the `case` itself; there is no
`expr.amountCents()` call anywhere in this method. A record pattern is one
more idiomatic way to write an exhaustive `switch` over a sealed
hierarchy, next to the plain type pattern `ManifestOperations` already
used. Reading the two side by side against the identical kind of problem,
a sealed-hierarchy `switch`, shows what each one buys:

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

Both arms are equally exhaustive and equally compiler-checked. The type
pattern reads better when an arm uses most of a record's components,
`p.id()`, `p.hazardClass()`, and `p.unNumber()` all read naturally off one
bound name. The record pattern reads better once an arm immediately
destructures every component anyway, especially a recursive one like
`Add(var left, var right)`, where naming `left` and `right` directly in the
`case` line removes two accessor calls from every branch beneath it.

What makes Interpreter worth its own section rather than a note on Visitor
is nesting. `fuelOnHandling` and `handlingOnFuel` below use the identical
three pieces, a five-dollar handling fee, a ten-dollar overweight fee
gated at twenty kilograms, and a 1.08 fuel factor, nested two different
ways:

```java
SurchargeExpr fuelOnHandling = new Multiply(
        new Add(new Literal(500), new IfWeightAbove(20.0, new Literal(1_000), new Literal(0))),
        1.08);
SurchargeExpr handlingOnFuel = new Add(
        new Literal(500),
        new IfWeightAbove(20.0, new Multiply(new Literal(1_000), 1.08), new Literal(0)));
```

Evaluated against a 22 kilogram shipment, `fuelOnHandling` scales the whole
fifteen-dollar sum by the fuel factor and comes to $16.20; `handlingOnFuel`
scales only the ten-dollar overweight fee and comes to $15.80. Three
identical leaf values, two different trees, two different totals for the
identical shipment. That difference is precedence, expressed as tree shape
rather than as an operator-binding rule a parser would otherwise apply;
building the tree directly the way this example does sidesteps writing a
parser at all while keeping the exact behavior a parsed grammar would have.

## Memento: a draft manifest that rolls back in one call

`ManifestDraft` is this chapter's Originator: a draft name, a growing list
of `ManifestEntry` line items, and a `submitted` flag flipped once at the
end of an editing session. Its nested `Snapshot` is the Memento, declared
as a record with no access modifier at all:

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

No access modifier on a nested type means package-private: `Snapshot`'s
canonical constructor and its three accessors are visible only to code
inside `ManifestDraft`'s own package. A caller in another package can still
hold whatever `save()` returns, assigned to a local declared with `var`
rather than a named type, and pass that same value straight into
`restore()`, but it can never write out the type `ManifestDraft.Snapshot`
in its own source, and it has no way to call `.entries()` on a value whose
type it cannot name. That is the real content of Memento's classic
contract, capture an object's state in a form a caretaker can store and
return without exposing what that state contains, done here with Java's
own package boundary instead of the friend declarations a language without
package-level access would need. Every caller in this example happens to
live in the same package as `ManifestDraft` itself, so nothing here forces
the discipline on this one file, but a real admin-screen controller written
in its own package gets the same hiding the moment it reaches for `var`
instead of spelling out a type it was never meant to see.

`save()` does one thing beyond copying two scalar fields: it copies
`entries` with `List.copyOf()` rather than handing back the same list
object. Skipping that copy would let a `Snapshot` alias the live draft's
list, so a later `addEntry()` call on the draft would silently change what
an already-saved snapshot appears to hold. No deeper cloning is needed
beyond that one copy, because every `ManifestEntry` is already an immutable
record; nothing inside one can change after it is built, so sharing the
same `StandardParcel` instance between a draft and ten different snapshots
of it is always safe. `restore()` is the mirror operation, replacing
`draftName`, `entries`, and `submitted` wholesale in a single call,
regardless of how many `rename()`, `addEntry()`, `removeLastEntry()`, or
`submit()` calls happened in between. Nothing about `restore()` knows or
needs to know how many edits it is discarding; a caretaker that calls
`save()` once and `restore()` once later undoes an entire editing session,
whether that session held two edits or twenty.

## When not to use Visitor, Memento, or Interpreter

All three patterns in this chapter earn their cost only when the force
behind them is real, not merely anticipated.

Visitor pays for itself when a structure stays fixed and the operations
over it keep multiplying. A hierarchy with exactly one operation checked
in exactly one place needs neither a sealed interface's `permits` clause
nor a dedicated utility class; it needs the handful of `if`/`else if`
branches a single method can hold, read start to finish by one reviewer.
The harder trap runs the other way: a set of kinds that stays open,
extended by a plugin or a third-party integration this
codebase will never recompile against, fights a sealed `switch` instead of
fitting it, because `permits` demands every implementor be known and
compiled together. An open type set that keeps growing from outside the
project is a sign to reach for ordinary polymorphism, an interface with no
`permits` clause and a `default` method or two, not a sealed hierarchy a
new, unanticipated implementor can never join.

Interpreter pays for itself when a grammar is small, stable, and built from
a handful of call sites that construct expressions directly, the way this
chapter's `fuelOnHandling` and `handlingOnFuel` do. A grammar that needs to
accept untrusted text, a merchant typing a surcharge formula into a text
box rather than a developer composing `SurchargeExpr` values in code, is a
different and much larger problem: tokenizing, parsing precedence and
associativity correctly, and reporting a syntax error at the right column
are all real work that a hand-rolled recursive-descent parser gets wrong in
ways that are easy to miss and expensive to find later. Reach for an
established parsing library or parser generator once a grammar needs to
read text a user supplies, rather than growing a Interpreter example's
tree-building helpers into a parser nobody intended to write.

Memento pays for itself when a whole-state rollback is something a caller
actually asks for, not a feature added because the pattern was available.
An object with no multi-step editing workflow and no "discard everything
since this point" requirement does not need a `Snapshot` type and a
`save()`/`restore()` pair sitting unused beside it. The sharper trap is
reaching for Memento when a single, cheap, targeted undo is what the
situation calls for: cloning an entire manifest on every keystroke to
support undoing one field edit spends far more than the problem is worth,
when a plain setter that remembers the one previous value would do the
same job. Name which rollback shape a caller actually needs, one field
reverted or one whole batch discarded, before reaching for either answer.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this
loop. From the repository root, the aggregator picks up
`examples/20-visitor-memento-interpreter` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/20-visitor-memento-interpreter
mvn test
```

Either path runs the same 25 JUnit 5 cases, asserted with AssertJ,
including a case that builds `fuelOnHandling` and `handlingOnFuel` from
identical pieces and asserts they evaluate to different totals, not just
that each one evaluates without throwing. Observed locally on Fedora with
Temurin 25.0.2:

```
Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see all three patterns run against one manifest rather than just reading
an assertion that says they work, run the example's own demo script:

```sh
examples/20-visitor-memento-interpreter/demo.sh
```

It prices and summarizes a four-entry manifest with the Visitor operations,
collects every validation issue on an entry missing required fields, edits
and restores a `ManifestDraft` through Memento, and evaluates the two
differently-nested surcharge expressions through Interpreter:

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

The validation block's three-item list, printed from one rejected entry, is
`validate()` collecting every problem in one pass rather than stopping at
the first one, the observable version of the test suite's equivalent
assertion. The memento block's final line, `entries=1` and `submitted=false`
again after `entries=2` and `submitted=true` mid-edit, is a whole editing
session discarded in one `restore()` call, not reversed field by field. The
interpreter block's two totals, $16.20 against $15.80 for the identical
three pieces nested two different ways, is nesting deciding an interpreted
result, visible at the terminal rather than only in an assertion.

## For deeper coverage

Visitor, Interpreter, and Memento are three of the last four patterns in
the Gang of Four's original catalogue.

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480/9780321700698) (Addison-Wesley): the original Visitor, Interpreter, and Memento entries, including the "represent an operation to be performed on the elements of an object structure," "given a language, define a representation for its grammar," and "capture and externalize an object's internal state" intent statements this chapter's exhaustive switches, expression grammar, and package-private snapshot each build on.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 17 on minimizing mutability is the idiom behind every `ManifestEntry` and `SurchargeExpr` record in this chapter, and the reason `ManifestDraft.save()` copies its entry list with `List.copyOf()` rather than aliasing it.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/20-visitor-memento-interpreter`'s suite, which asserts that two independently nested expressions evaluate to different totals and that a restored snapshot reproduces exact prior state, rather than only checking that each class constructs without error.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
