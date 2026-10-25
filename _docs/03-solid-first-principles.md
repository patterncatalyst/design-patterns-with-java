---
title: "SOLID I: SRP / OCP / LSP"
order: 3
layout: tutorial
sectionid: docs
part: "principles"
description: "A freight-billing engine split along three SOLID principles: one reason to change per type under the Single Responsibility Principle, an InvoiceRule abstraction new rules can satisfy without an edit under the Open/Closed Principle, and a discount rule refined without breaking its parent's contract under Liskov Substitution."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- State the Single Responsibility Principle as "one reason to change," not
  "one method," and split a type along the lines of who asks for the change
  rather than how many lines the type has.
- Build an Open/Closed extension point: an interface a new rule can satisfy
  without editing the code that already calls it, and know when a closed,
  compiler-checked alternative is the better fit instead.
- State the Liskov Substitution Principle as a promise about behavior, not
  a matching method signature, and name what a subtype has to honor to keep
  that promise.
- Enforce an invariant once, in a `final` method a subtype cannot override,
  instead of trusting every subtype to reimplement the same guarantee
  correctly.
- Read a JUnit 5 and Mockito suite that proves OCP and LSP directly: a
  test-defined rule type that needs no production code changes, and a
  function written once against an interface that behaves correctly for
  every concrete rule passed to it, including one that does not.
- Recognize when splitting a responsibility, opening an abstraction, or
  protecting an invariant is solving a problem nobody has yet, and say what
  to write instead.

## Why this matters

A freight-billing engine has to do three unrelated jobs with the same
invoice: total it, show it to the customer who is paying it, and remember it
in case a dispute shows up six weeks later. The first version of that engine
often lands as one `Invoice` type carrying a `calculateTotal` method, a
`format` method, and a `save` method, because the invoice is the object all
three jobs are about, and that reads as the obvious place for them to live.
A sales tax rate changes once or twice a year, set by a finance team that
has never opened this codebase. A receipt's wording changes whenever
customer support wants a clearer line item or a different currency format.
The storage layer changes rarely, maybe once, the day the business outgrows
a flat file and moves to a real database. Three different reasons, three
different schedules, three different people who might ask for each change,
and in the one-type version all three land in the same file because nothing
in the original design said they had to live apart.

This chapter's three principles, Single Responsibility, Open/Closed, and
Liskov Substitution, all answer one question: this will change, and the
rest of the system should not have to change with it. Robert C. Martin
frames every one of the five SOLID principles that way, and a billing engine
meets these three first, almost always in this order. SRP asks which code
changes for which reason, and insists one type answer to exactly one of
those reasons. OCP asks what happens the day a new rule shows up, a new
jurisdiction's tax or a new discount program, and insists the answer is a
new type, not an edited one. LSP asks whether a refined version of an
existing type can stand in for it everywhere that type is expected, and
insists the refinement keep every promise the original type made, not just
its method signatures. Interface Segregation and Dependency Inversion pick
up where this trio stops, once a dependency has to cross a boundary these
three principles do not yet need to cross.

{% include excalidraw.html name="03-open-closed-extension" caption="Figure 3.1: InvoiceCalculator depends on the InvoiceRule abstraction, never on a concrete rule. PercentageTaxRule and PercentageDiscountRule already existed; FlatServiceFeeRule arrived afterward as a new implementation, and InvoiceCalculator did not change." %}

## The domain: billing a freight invoice

The example behind this chapter lives in `examples/03-solid-first-principles`.
`LineItem` carries a description, a quantity, and a per-unit price; `Invoice`
carries an id, a customer name, and a list of line items. Money is tracked
as a `long` of exact cents, never a `double`, so a total is exact and a test
assertion can compare it for equality without an epsilon:

```java
public record LineItem(String description, int quantity, long unitPriceCents) {

    public LineItem {
        Objects.requireNonNull(description, "description");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
        if (unitPriceCents < 0) {
            throw new IllegalArgumentException("unitPriceCents cannot be negative: " + unitPriceCents);
        }
    }

    public long extendedPriceCents() {
        return (long) quantity * unitPriceCents;
    }
}
```

A record is the right shape for `LineItem` because a line item is a value,
not an entity: two line items with the same description, quantity, and
price are interchangeable, and nothing about a line item should ever change
after it is built. Before records, that same value type took a lot more
code to get right.

{% include codetabs.html langs="Classic Java|Java 25" %}
```java
// Classic Java: a hand-written value class. Every field needs a getter, and
// equals/hashCode/toString all have to be written and kept in sync by hand
// with whatever fields the class ends up with.
public final class LineItem {
    private final String description;
    private final int quantity;
    private final long unitPriceCents;

    public LineItem(String description, int quantity, long unitPriceCents) {
        this.description = description;
        this.quantity = quantity;
        this.unitPriceCents = unitPriceCents;
    }

    public String getDescription() { return description; }
    public int getQuantity() { return quantity; }
    public long getUnitPriceCents() { return unitPriceCents; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LineItem that)) return false;
        return quantity == that.quantity
                && unitPriceCents == that.unitPriceCents
                && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, quantity, unitPriceCents);
    }

    @Override
    public String toString() {
        return "LineItem[description=" + description + ", quantity=" + quantity
                + ", unitPriceCents=" + unitPriceCents + "]";
    }
}
```
```java
// Java 25: the identical value contract, declared once. The accessors,
// equals, hashCode, and toString all follow from the state the header
// declares; there is no second copy of the field list to keep in sync.
public record LineItem(String description, int quantity, long unitPriceCents) {
}
```

Both columns give a caller the same three accessors, `description()`,
`quantity()`, and `unitPriceCents()` on the record, `getDescription()` and
friends on the hand-written class, the same value equality, and the same
readable `toString()`. The record version is also the one this example
actually ships, with validation added in the compact constructor shown
above. None of this is about SRP, OCP, or LSP by itself; it is the
foundation the next three sections build their reasoning on, so it earns
its place before the principles do.

## Single Responsibility: splitting by reason to change

Picture the version that has not been split yet: one `Invoice` type with a
`calculateTotal` method, a `format` method, and a `save` method, each
reaching into the same line items and each changing for its own unrelated
reason. That type compiles, runs, and passes a test the day it ships. It
also means a change to the tax rate, a change to the receipt's wording, and
a change to how invoices are stored all touch the same file, and a reviewer
approving a tax fix has to read past formatting code and persistence code to
find the handful of lines that actually matter. SRP's fix is not to make the
type smaller for its own sake; it is to separate three unrelated reasons to
change that were sharing one address, and give each one its own.

This example's split lands on three types, each with exactly one reason to
change. `InvoiceCalculator` turns an `Invoice` and a list of rules into
`Totals`:

```java
public final class InvoiceCalculator {

    private final List<InvoiceRule> rules;

    public InvoiceCalculator(List<InvoiceRule> rules) {
        this.rules = List.copyOf(Objects.requireNonNull(rules, "rules"));
    }

    public Totals totalsFor(Invoice invoice) {
        long subtotalCents = subtotalOf(invoice);
        long totalCents = subtotalCents;
        for (InvoiceRule rule : rules) {
            totalCents += rule.adjustmentCents(subtotalCents);
        }
        return new Totals(subtotalCents, totalCents);
    }
    // ...
}
```

`InvoiceFormatter` turns an `Invoice` and its already-computed `Totals` into
receipt text, and knows nothing about how those totals were derived:

```java
public final class InvoiceFormatter {
    public static String format(Invoice invoice, Totals totals) {
        // ... renders invoice, line items, and totals as receipt text
    }
}
```

`InMemoryInvoiceRepository` remembers invoices by id and hands them back,
and never computes or renders anything:

```java
public final class InMemoryInvoiceRepository implements InvoiceRepository {
    private final Map<String, Invoice> invoicesById = new HashMap<>();

    @Override
    public void save(Invoice invoice) {
        invoicesById.put(invoice.id(), invoice);
    }

    @Override
    public Optional<Invoice> findById(String id) {
        return Optional.ofNullable(invoicesById.get(id));
    }
}
```

None of these three types knows the other two exist. `InvoiceCalculator`
never calls `InvoiceFormatter.format`, and `InMemoryInvoiceRepository` never
sees a `Totals` value at all; `BillingDemo` is the only place that calls all
three in sequence. That separation is what makes the "one reason to change"
test concrete rather than aspirational: changing the tax calculation means
editing `InvoiceCalculator` and nothing else, changing the receipt's wording
means editing `InvoiceFormatter` and nothing else, and a reviewer approving
either change reads exactly the file the change touches. The suite proves
this at the behavior level too: `InvoiceFormatterTest` hands the formatter a
`Totals` value that does not match the invoice's real subtotal, and the
formatter prints it anyway, because it never recomputes anything a caller
already computed for it.

## Open/Closed: a rule abstraction instead of an edited method

A tax rate changing twice a year is a predictable kind of change, and OCP's
claim is specific about how to meet it: a type should be open to having new
behavior added, and closed against having its existing, already-tested code
edited to make room for that behavior. The naive alternative is a method
that grows a new branch every time a new tax jurisdiction or discount
program launches, which means every addition risks breaking a branch that
was already correct and already tested. This example's answer is
`InvoiceRule`, an abstraction `InvoiceCalculator` depends on and never
edits:

```java
public interface InvoiceRule {
    long adjustmentCents(long subtotalCents);
    String name();
}
```

`PercentageTaxRule` adds a percentage of the subtotal; `PercentageDiscountRule`
subtracts one, clamped so it can never discount more than the subtotal is
worth (the next section covers why that clamp lives where it does). Both
existed before `FlatServiceFeeRule`, a flat addition with no relationship to
the subtotal at all, joined them. Adding `FlatServiceFeeRule` touched exactly
one new file; `InvoiceCalculator.totalsFor` is byte-for-byte what it was
before that file existed. Every rule receives the invoice's original
subtotal, not a running total updated rule by rule, so each rule's
adjustment is independent of the order the others ran in, a property the
test suite checks directly by running the same three rules forward and
reversed and asserting the two totals match.

`InvoiceCalculatorTest.addingANewRuleTypeRequiresNoEditToInvoiceCalculator`
proves the "no edits" claim the same way a claim like this should be proved:
it defines a brand-new `RoundUpToWholeDollarRule` right there in the test
file, a type `InvoiceCalculator`'s own source has never seen, and hands an
instance to the same constructor every other rule uses. The calculator picks
it up and applies it correctly with zero production code changed, because
`InvoiceRule` was always the actual extension point, not a convention a
developer had to remember to follow. That is also why `InvoiceRule` stays a
plain interface here instead of a sealed one: a plugin-style rule, written
by a different team or defined only inside a test, has to be able to satisfy
it without this file's permission.

A catalogue that is closed instead of open calls for a different Java 25
idiom, and sealed types are not a strictly better version of the open
interface; they trade one property for another. Picture a finance team that
wants every adjustment on an invoice explained on an audit line, and wants
the compiler, not a code reviewer, to catch a kind of adjustment nobody
wrote an explanation for yet.

{% include codetabs.html langs="Classic Java|Java 25" %}
```java
// Classic Java: an instanceof chain, with a default branch that throws.
// Adding a fourth rule kind compiles cleanly even if this method is never
// updated; the gap only surfaces when a real invoice carrying that rule
// reaches the default branch at runtime.
public static String explain(InvoiceRule rule, long subtotalCents) {
    if (rule instanceof PercentageTaxRule tax) {
        return tax.name() + ": +" + tax.adjustmentCents(subtotalCents) + "c";
    } else if (rule instanceof PercentageDiscountRule discount) {
        return discount.name() + ": " + discount.adjustmentCents(subtotalCents) + "c";
    } else if (rule instanceof FlatServiceFeeRule fee) {
        return fee.name() + ": +" + fee.adjustmentCents(subtotalCents) + "c";
    } else {
        throw new IllegalStateException("Unexplained rule kind: " + rule.getClass());
    }
}
```
```java
// Java 25: a sealed InvoiceRule permits exactly these three kinds, and the
// switch over them is exhaustive. Adding a fourth permitted kind without
// adding its case here is a compile error, not a runtime surprise reached
// by whichever invoice exercises it first.
public static String explain(InvoiceRule rule, long subtotalCents) {
    return switch (rule) {
        case PercentageTaxRule tax ->
            tax.name() + ": +" + tax.adjustmentCents(subtotalCents) + "c";
        case PercentageDiscountRule discount ->
            discount.name() + ": " + discount.adjustmentCents(subtotalCents) + "c";
        case FlatServiceFeeRule fee ->
            fee.name() + ": +" + fee.adjustmentCents(subtotalCents) + "c";
    };
}
```

Sealing `InvoiceRule` for this purpose costs something: the permits clause
and the switch both have to be edited the moment a fourth rule kind is
added, which is precisely the edit OCP exists to avoid. What sealing buys
back is that the compiler, not a runtime default branch, notices the gap.
Reach for the open interface, the one this example actually ships, when a
new rule has to arrive without a recompile of the code that applies it.
Reach for the sealed, exhaustive-switch version when the catalogue is fixed
by design and every consumer of it should have to make a visible decision
the day it grows. The two trade OCP's openness for the switch's
completeness in opposite directions; neither is a strictly better version
of the other.

## Liskov Substitution: a discount that keeps its parent's promise

LSP's statement is short: a subtype must be substitutable for its base type
everywhere the base type is used, without a caller noticing a difference in
correctness. The part that is easy to miss is that "correctness" here means
the base type's whole contract, not just its method signatures. A
`PercentageDiscountRule` that honors `InvoiceRule`'s contract promises more
than "I have an `adjustmentCents` method that returns a `long`"; it promises
the result never discounts more than the subtotal, because every caller of
`adjustmentCents` is allowed to assume that without checking it.

`PercentageDiscountRule.adjustmentCents` is declared `final` specifically to
protect that promise:

```java
public abstract class PercentageDiscountRule implements InvoiceRule {

    @Override
    public final long adjustmentCents(long subtotalCents) {
        double clampedRate = clampRate(discountRate(subtotalCents));
        return -Math.round(subtotalCents * clampedRate);
    }

    protected abstract double discountRate(long subtotalCents);

    private static double clampRate(double rate) {
        return Math.max(0.0, Math.min(1.0, rate));
    }
}
```

The clamp lives in exactly one place, and it runs after `discountRate`
returns, not before. A subtype cannot override `adjustmentCents` to skip it;
the compiler rejects the attempt outright, which is a stronger guarantee
than a reviewer catching it in a pull request. `LoyaltyDiscountRule` refines
`PercentageDiscountRule` for repeat customers, a base rate plus a bonus for
every loyalty year, by overriding only the protected `discountRate` hook:

```java
public final class LoyaltyDiscountRule extends PercentageDiscountRule {
    private final double baseRate;
    private final double bonusRatePerYear;
    private final int loyaltyYears;
    // constructor omitted

    @Override
    protected double discountRate(long subtotalCents) {
        return baseRate + (bonusRatePerYear * loyaltyYears);
    }
}
```

A loyalty customer with a 95% negotiated base rate and fifty years on the
account would, unclamped, combine to a 145% rate, discounting more than the
invoice is worth. `LoyaltyDiscountRule` never has to defend against that
case itself, because `adjustmentCents`'s clamp runs on whatever
`discountRate` returns, including a value well past 1.0.
`LiskovSubstitutionTest` makes the substitutability claim concrete with one
function, written once against the base type:

```java
private static long adjustmentThroughBaseType(InvoiceRule rule, long subtotalCents) {
    return rule.adjustmentCents(subtotalCents);
}
```

That function is called with a `StandardDiscountRule` and with a
`LoyaltyDiscountRule`, and both calls behave correctly, because neither
subtype had the chance to violate the invariant the base type owns. A
parameterized test calls the same function with rates from `0.0` to `5.0`
and even a negative rate, and asserts the adjustment always lands in
`[-subtotal, 0]`, proving the clamp holds no matter what a subtype's
`discountRate` returns.

The counter-example lives in the same file. `ClearanceEventDiscountRule`
implements `InvoiceRule` directly, instead of extending
`PercentageDiscountRule`, written in a hurry for a one-off clearance event
with a rate set above 1.0:

```java
public final class ClearanceEventDiscountRule implements InvoiceRule {
    private final double rate;
    // ...

    @Override
    public long adjustmentCents(long subtotalCents) {
        return -Math.round(subtotalCents * rate);
    }
}
```

Nothing stops this class from compiling, and nothing stops it from
implementing `InvoiceRule` correctly by the letter of the interface: it has
an `adjustmentCents` method, and it returns a `long`. What it does not do is
honor the contract every other discount rule in this example honors, and
`LiskovSubstitutionTest.clearanceEventRuleViolatesTheInvariantEveryDiscountRuleIsExpectedToHonor`
proves it: a 150% clearance rate on a $1,920.00 invoice discounts $2,880.00,
more than the invoice is worth, something no `PercentageDiscountRule`
subtype can produce. This is the shape an LSP violation actually takes in a
real codebase: not a glaring bug caught on the first run, but a type that
type-checks, compiles, and passes a quick smoke test, while quietly breaking
an assumption every other implementation of the same interface was careful
to uphold.

> **AI sidebar.** Ask an LLM pairing partner to "apply SOLID" to a working
> `Invoice` type and a common result is an `IInvoiceCalculator` interface
> with exactly one implementation, an `IInvoiceFormatter` interface with
> exactly one implementation, and a factory to wire both together, because
> that shape appears constantly in tutorials that treat "SOLID" as a
> checklist to satisfy rather than a response to a specific force. Check the
> result the way this chapter trained: SRP asks whether the split follows an
> actual difference in who changes the code and why, not just whether the
> result has more files than before. OCP asks whether a second rule,
> implementation, or variant is already in hand or clearly coming; an
> interface with one implementation and nothing else on the roadmap is OCP's
> cost with none of its benefit. LSP asks whether a generated subtype honors
> its parent's invariants, not merely its method signatures; a model asked
> to "add a specialized discount" will cheerfully write a class that
> overrides a percentage calculation in a way that breaks an invariant the
> base class never wrote down anywhere a generator could see it. That is
> exactly why this chapter's `PercentageDiscountRule` enforces its clamp in
> a `final` method instead of trusting every future subtype to remember it;
> ask the same question of generated code that does not have that
> protection built in.

## When SOLID works against you

Every principle in this chapter earns its cost only when the force behind
it is present, and all three share a failure mode in the other direction:
applying them because the name is familiar, not because the forces that
justify the name are in the code.

A billing engine with one tax rule, one discount program, and no second one
on any roadmap does not need an `InvoiceRule` interface; it needs a method.
Wrapping a single percentage calculation in an interface before a second
implementation exists adds a file, an extra level of indirection, and a
reader's detour through an abstraction to find the one case that will ever
run through it, with nothing on the other side of that cost to justify it
yet. SRP has the same failure mode from the opposite direction: splitting an
`Invoice` type with one `calculateTotal` method and no formatting or
persistence code anywhere near it into three types in advance of a second
responsibility arriving does not clarify the design, it fragments it.
Pushed far enough, this is how a codebase ends up with an anemic cluster of
`InvoiceCalculatorHelper`, `InvoiceCalculatorSupport`, and
`InvoiceCalculatorUtil` types, each holding one method, none of them owning
a real responsibility on its own, and a reader has to open three files to
follow logic a single method would have shown in one. SRP asks for one
reason to change per type, not the smallest possible type; a type that
currently has no second reason to change does not need to be carved up
preemptively.

LSP's version of this failure is subtler and more common: building a
protected extension hook, the way `PercentageDiscountRule.discountRate`
exists here, when only one subtype will ever override it. That hook earns
its design cost in this chapter's example because a second, materially
different discount rule, the loyalty variant, was already planned; absent
that, `PercentageDiscountRule` could apply its own rate directly with no
hook at all, and a hook with exactly one caller is a promise of flexibility
nobody is going to collect on. The sealed-interface idiom from the Open/
Closed section carries its own version of the same caution: sealing
`InvoiceRule` and writing an exhaustive switch over one single permitted
type buys nothing a direct method call would not, because there is nothing
for the compiler's completeness check to protect against yet. The test for
all three principles in this chapter is the same one worth asking of any
pattern: name the abstraction and the plain alternative side by side, and
let whether a second case is in hand or clearly coming decide between them,
not the principle's reputation for being good practice in general.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this loop.
From the repository root, the aggregator picks up
`examples/03-solid-first-principles` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/03-solid-first-principles
mvn test
```

Either path runs the same 29 JUnit 5 cases, asserted with AssertJ, including
one test that mocks `InvoiceRule` with Mockito to verify every rule receives
the invoice's original subtotal regardless of calling order. Observed
locally on Fedora with Temurin 25.0.2:

```
Tests run: 29, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see all three principles play out on one invoice rather than just reading
an assertion that says they hold, run the example's own demo script:

```sh
examples/03-solid-first-principles/demo.sh
```

It prices a two-line invoice for Anchor Point Imports (two 40-foot
containers of freight handling plus six weeks of warehouse storage), persists
and reloads it by id, breaks down each rule's own adjustment, and then
compares a loyalty discount against the clearance-event rule that skips the
clamp:

```
== Single Responsibility: calculate, format, persist ==
Invoice INV-3087 - Anchor Point Imports
  2 x Freight handling (40-ft container) @    $825.00 =  $1,650.00
  6 x Warehouse storage (per week)     @     $45.00 =    $270.00
  Subtotal: $1,920.00
  Total:    $2,012.60
Persisted and reloaded by id: yes

== Open/Closed: three rule kinds, InvoiceCalculator never edited ==
sales tax: $153.60
contract discount: -$96.00
dock handling fee: $35.00
Invoice total: $2,012.60

== Liskov Substitution: a loyalty discount stands in for the base rule ==
Invoice total for a 6-year loyalty customer: $1,897.40

== Liskov Substitution violated: a rule that skips the clamp ==
Invoice total with the unclamped rule: -$771.40 (negative: the invariant every other rule honors)
```

The last line is the point made visible rather than merely asserted: every
`PercentageDiscountRule` subtype keeps an invoice's total at or above zero no
matter what rate it is configured with, and a rule that bypasses that base
type does not.

## For deeper coverage

- Robert C. Martin, [*Agile Software Development: Principles, Patterns, and Practices*](https://www.pearson.com/en-gb/subject-catalog/p/agile-software-development-principles-patterns-and-practices-pearson-new-international-edition/P200000004932/9781292038360) (Prentice Hall): the original statement of the Single Responsibility, Open/Closed, and Liskov Substitution principles this chapter builds on, and the source of the "reason to change" framing this chapter uses throughout.
- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480/9780321700698) (Addison-Wesley): the Gang of Four's own advice to favor composition and small, focused abstractions, the same instinct the `InvoiceRule` extension point and the clamped `PercentageDiscountRule` base both express in idiomatic Java.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and the [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html): the testing idiom behind `examples/03-solid-first-principles`'s suite, including the mock-based proof that every rule receives the same, original subtotal.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
