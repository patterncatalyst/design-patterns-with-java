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

- State the Single Responsibility Principle as "one reason to change" and split a type by who asks for the change, not by line count.
- Build an Open/Closed extension point that a new rule satisfies without an edit to its caller, and know when a sealed, compiler-checked set fits better.
- State the Liskov Substitution Principle as a promise about behavior, not a matching signature.
- Enforce an invariant once, in a `final` method a subtype can't override.
- Tell when splitting, opening or protecting a type solves a problem nobody has yet, and what to write instead.

## Why this matters

A freight-billing engine does three jobs with one invoice: total it, show it to the customer and keep it for the dispute that arrives six weeks later. The first version is often one `Invoice` type with `calculateTotal`, `format` and `save`. Then the three jobs change on their own schedules:

| Job | Changes when | Asked for by |
| --- | --- | --- |
| Totaling | a tax rate moves, once or twice a year | finance |
| Formatting | support wants a clearer line or a new currency format | customer support |
| Storage | the business outgrows a flat file | engineering, perhaps once |

In the one-type version all three land in the same file. Robert C. Martin frames each SOLID principle as an answer to one question: this will change, so what else must change with it? SRP asks which code changes for which reason. OCP asks what happens when a new rule arrives, and answers with a new type rather than an edited one. LSP asks whether a refined type can stand in for its parent everywhere, keeping every promise the parent made. Interface Segregation and Dependency Inversion take over once a dependency has to cross a boundary.

{% include excalidraw.html name="03-open-closed-extension" caption="Figure 3.1: InvoiceCalculator depends on the InvoiceRule abstraction, never on a concrete rule. PercentageTaxRule and PercentageDiscountRule already existed; FlatServiceFeeRule arrived afterward as a new implementation, and InvoiceCalculator did not change." %}

## The domain: billing a freight invoice

`LineItem` carries a description, a quantity and a unit price; `Invoice` carries an id, a customer name and its line items. Money is a `long` of cents, never a `double`, so totals are exact:

```java
public record LineItem(String description, int quantity, long unitPriceCents) {

    public LineItem {
        Objects.requireNonNull(description, "description");
        if (description.isBlank()) {
            throw new IllegalArgumentException("description cannot be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
        if (unitPriceCents < 0) {
            throw new IllegalArgumentException("unitPriceCents cannot be negative: " + unitPriceCents);
        }
    }

    /** The extended price for this line: quantity times the per-unit price. */
    public long extendedPriceCents() {
        return (long) quantity * unitPriceCents;
    }
}
```

A line item is a value: two with the same fields are interchangeable, and none changes after it's built. The record supplies the accessors, `equals`, `hashCode` and `toString` from its header, and the compact constructor validates once.

## Single Responsibility: splitting by reason to change

In the unsplit `Invoice`, a tax fix, a wording change and a storage move all touch one file, and a reviewer of the tax fix reads past formatting and persistence code to find it. SRP doesn't ask for small types. It asks that unrelated reasons to change stop sharing an address.

The split gives three types, one reason each:

| Type | Job | Knows nothing about |
| --- | --- | --- |
| `InvoiceCalculator` | turns an `Invoice` and a list of rules into `Totals` | receipts, storage |
| `InvoiceFormatter` | turns an `Invoice` and its `Totals` into receipt text | how the totals were computed |
| `InMemoryInvoiceRepository` | saves invoices and finds them by id | totals, receipts |

```java
public final class InvoiceCalculator {

    private final List<InvoiceRule> rules;

    public InvoiceCalculator(List<InvoiceRule> rules) {
        this.rules = List.copyOf(Objects.requireNonNull(rules, "rules"));
    }

    public Totals totalsFor(Invoice invoice) {
        Objects.requireNonNull(invoice, "invoice");
        long subtotalCents = subtotalOf(invoice);
        long totalCents = subtotalCents;
        for (InvoiceRule rule : rules) {
            totalCents += rule.adjustmentCents(subtotalCents);
        }
        return new Totals(subtotalCents, totalCents);
    }

    private static long subtotalOf(Invoice invoice) {
        long subtotal = 0L;
        for (LineItem item : invoice.lineItems()) {
            subtotal += item.extendedPriceCents();
        }
        return subtotal;
    }
}
```

Only `BillingDemo` calls all three. A tax change edits `InvoiceCalculator` and nothing else; a wording change edits `InvoiceFormatter` and nothing else. The formatter never recomputes: hand it a `Totals` that disagrees with the line items and it prints the `Totals` it was given.

## Open/Closed: a rule abstraction instead of an edited method

OCP says a type is open to new behavior and closed to edits of its tested code. The alternative is a method that grows a branch for every new jurisdiction or discount program, where each addition risks a branch that already worked. `InvoiceCalculator` depends on `InvoiceRule` instead:

```java
public interface InvoiceRule {

    long adjustmentCents(long subtotalCents);

    String name();
}
```

`PercentageTaxRule` adds a percentage of the subtotal and `PercentageDiscountRule` subtracts one. `FlatServiceFeeRule`, a flat amount unrelated to the subtotal, arrived later as one new file, and `totalsFor` didn't change. A rule defined only inside a test file, which the calculator's source has never seen, is applied the same way.

Every rule receives the original subtotal, not a running total, so the result doesn't depend on rule order. Pass `totalCents` instead and a tax applied after a discount is taxed on the discounted amount, and the same rules in reverse order give a different total.

`InvoiceRule` stays a plain interface because a rule written by another team, or only in a test, must be able to implement it without editing this package. A closed catalogue calls for the opposite trade:

| | Open `InvoiceRule` interface | Sealed `InvoiceRule` with an exhaustive `switch` |
| --- | --- | --- |
| Adding a rule kind | a new file; no existing code changes | edit the `permits` clause and every `switch` over it |
| A missing case is caught | at run time, by whichever invoice hits a default branch | at compile time |
| Fits | plugin-style rules from other teams | a fixed set where every consumer, such as an audit line per adjustment, must decide when it grows |

Neither is better in general. The sealed version gives up OCP's openness to get completeness checking.

## Liskov Substitution: a discount that keeps its parent's promise

A subtype must stand in for its base type everywhere without a caller seeing a difference in correctness, and correctness means the whole contract, not only the signature. A discount rule promises more than "returns a `long`": it never discounts more than the subtotal, and every caller may assume that without checking.

`PercentageDiscountRule` keeps the promise in one `final` method:

```java
public abstract class PercentageDiscountRule implements InvoiceRule {

    private final String name;

    protected PercentageDiscountRule(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    @Override
    public final long adjustmentCents(long subtotalCents) {
        double clampedRate = clampRate(discountRate(subtotalCents));
        return -Math.round(subtotalCents * clampedRate);
    }

    protected abstract double discountRate(long subtotalCents);

    @Override
    public final String name() {
        return name;
    }

    private static double clampRate(double rate) {
        return Math.max(0.0, Math.min(1.0, rate));
    }
}
```

The clamp runs on whatever `discountRate` returns. A subtype can't override `adjustmentCents` to skip it; the compiler rejects the attempt, which is stronger than a reviewer catching it. `LoyaltyDiscountRule` overrides only the hook:

```java
public final class LoyaltyDiscountRule extends PercentageDiscountRule {

    private final double baseRate;
    private final double bonusRatePerYear;
    private final int loyaltyYears;

    public LoyaltyDiscountRule(String name, double baseRate, double bonusRatePerYear, int loyaltyYears) {
        super(name);
        if (loyaltyYears < 0) {
            throw new IllegalArgumentException("loyaltyYears cannot be negative: " + loyaltyYears);
        }
        this.baseRate = baseRate;
        this.bonusRatePerYear = bonusRatePerYear;
        this.loyaltyYears = loyaltyYears;
    }

    @Override
    protected double discountRate(long subtotalCents) {
        return baseRate + (bonusRatePerYear * loyaltyYears);
    }
}
```

A 95% base rate plus 1% for each of fifty years is 145%, which would discount more than the invoice is worth. `LoyaltyDiscountRule` needn't defend against it, because the clamp caps the rate at 100%. Code written once against `InvoiceRule` gets a correct adjustment from `StandardDiscountRule` and `LoyaltyDiscountRule` alike, and for any configured rate, including 5.0 and a negative one, the adjustment stays between minus the subtotal and zero.

`ClearanceEventDiscountRule` is the counter-example. It implements `InvoiceRule` directly, written in a hurry for a one-off event:

```java
    @Override
    public long adjustmentCents(long subtotalCents) {
        return -Math.round(subtotalCents * rate);
    }
```

It compiles and matches the interface by the letter. At a 150% rate on a $1,920.00 invoice it discounts $2,880.00, which no `PercentageDiscountRule` subtype can do. That's the usual shape of an LSP violation: a type that compiles and passes a smoke test while breaking an assumption every other implementation keeps.

> **🤖 Explore with your agent**
>
> "In `examples/03-solid-first-principles`, add a `SeasonalDiscountRule` that discounts a configured base rate plus a peak-season bonus. Extend `PercentageDiscountRule` and override only `discountRate(long subtotalCents)`; don't implement `InvoiceRule` directly and don't touch `adjustmentCents`, its clamp, or `InvoiceCalculator`. Keep money in `long` cents. Add JUnit 5 tests with AssertJ showing that a combined rate above 1.0 discounts exactly the subtotal and no more."
>
> Check: `mvn -q -f examples/03-solid-first-principles/pom.xml test -Dtest=LiskovSubstitutionTest` passes along with the new tests, and the diff leaves `InvoiceCalculator` unchanged.

## When SOLID works against you

Each principle earns its cost only when its force is present. All three fail the same way in the other direction: applied because the name is familiar.

- **OCP with one rule.** A billing engine with one tax rule and no second on any roadmap needs a method, not an `InvoiceRule` interface. The interface adds a file and a detour with nothing behind it yet.
- **SRP ahead of the second job.** Splitting an `Invoice` with only `calculateTotal`, and no formatting or persistence near it, fragments the design. Pushed further, you get `InvoiceCalculatorHelper`, `InvoiceCalculatorSupport` and `InvoiceCalculatorUtil`, one method each, and three files to read logic one method would show.
- **An LSP hook with one override.** `discountRate` is worth its cost because the loyalty variant was planned. Without a second rule, `PercentageDiscountRule` can apply its rate directly.
- **A sealed set of one.** An exhaustive `switch` over one permitted type gives the compiler nothing to check.

Decide as for any pattern: name the abstraction and the plain alternative side by side, and let a second case in hand, or clearly coming, decide.

## Build, run, observe

From the repository root, through the aggregator, or standalone:

```sh
mvn -f examples/pom.xml test
```

```sh
cd examples/03-solid-first-principles
mvn test
```

All 29 tests pass. One mocks `InvoiceRule` with Mockito to verify that each rule receives the original subtotal. Run the demo to see the three principles on one invoice:

```sh
examples/03-solid-first-principles/demo.sh
```

It prices a two-line invoice for Anchor Point Imports, saves and reloads it by id, breaks down each rule's adjustment, and compares a loyalty discount with the clearance rule that skips the clamp:

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

Every `PercentageDiscountRule` subtype keeps the total at or above zero; the rule that bypasses the base type drives it negative. Two experiments, each in a scratch copy:

- **Drop the clamp.** Make `clampRate` return `rate` unchanged. Four tests fail: the rates 1.5, 5.0 and -3.0 escape the zero-to-subtotal range, and the 145% loyalty customer is discounted past the subtotal.
- **Pass the running total.** In `totalsFor`, call `rule.adjustmentCents(totalCents)`. Three tests fail: the total, the order-independence check and the Mockito verification that each rule sees the original subtotal.

## For deeper coverage

- Robert C. Martin, [*Agile Software Development: Principles, Patterns, and Practices*](https://www.pearson.com/en-gb/subject-catalog/p/agile-software-development-principles-patterns-and-practices-pearson-new-international-edition/P200000004932/9781292038360) (Prentice Hall): the original statement of the Single Responsibility, Open/Closed, and Liskov Substitution principles this chapter builds on, and the source of the "reason to change" framing this chapter uses throughout.
- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480/9780321700698) (Addison-Wesley): the Gang of Four's own advice to favor composition and small, focused abstractions, the same instinct the `InvoiceRule` extension point and the clamped `PercentageDiscountRule` base both express in idiomatic Java.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and the [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html): the testing idiom behind `examples/03-solid-first-principles`'s suite, including the mock-based proof that every rule receives the same, original subtotal.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
