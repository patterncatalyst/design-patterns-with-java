# 03 - SOLID I: SRP / OCP / LSP, a freight-billing engine

A freight invoice has to be totaled, shown to a customer, and remembered
for a dispute weeks later. This example splits those three jobs along the
Single Responsibility Principle, opens the totaling step to new rules
through an Open/Closed abstraction, and refines one of those rules through
a Liskov-honoring discount, next to a counter-example that does not honor
it.

## The domain

- `LineItem`: a `record` with `description`, `quantity`, and
  `unitPriceCents`. Money is a `long` of exact cents, never a `double`.
- `Invoice`: a `record` of `id`, `customerName`, and an immutable
  `lineItems` list (defensively copied in the compact constructor).
- `Totals`: a `record` of `subtotalCents` and `totalCents`.

## Single Responsibility: three types, three reasons to change

- `InvoiceCalculator` turns an `Invoice` and a list of `InvoiceRule`s into
  `Totals`. Its only reason to change is "the math changed."
- `InvoiceFormatter` turns an `Invoice` and an already-computed `Totals`
  into receipt text. Its only reason to change is "the receipt should read
  differently." It never computes a total itself.
- `InMemoryInvoiceRepository` (behind an `InvoiceRepository` interface)
  saves and finds invoices by id. Its only reason to change is "where
  invoices are stored changed." It never computes or renders anything.

None of the three knows the other two exist; `BillingDemo` is the only
place that calls all three in sequence.

## Open/Closed: `InvoiceRule` as the extension point

`InvoiceCalculator` depends on `InvoiceRule`, never on a concrete rule.
`PercentageTaxRule`, `PercentageDiscountRule` (and its subtypes), and
`FlatServiceFeeRule` all implement it. `FlatServiceFeeRule` shipped after
`InvoiceCalculator` and the other two rules already existed and were
already tested; adding it touched exactly one new file.
`InvoiceCalculatorTest.addingANewRuleTypeRequiresNoEditToInvoiceCalculator`
proves the "no edits" claim directly: it defines a brand-new
`RoundUpToWholeDollarRule` inside the test file itself, a type
`InvoiceCalculator`'s own source has never seen, and hands it to the same
constructor every other rule uses.

## Liskov Substitution: a discount that keeps its parent's promise

`PercentageDiscountRule` declares `adjustmentCents` `final` specifically so
no subtype can skip the clamp that keeps a discount from exceeding the
subtotal. `StandardDiscountRule` and `LoyaltyDiscountRule` only supply a
`discountRate`; the clamp runs on whatever that hook returns, including a
combined rate past 100%. `ClearanceEventDiscountRule` is the counter-example:
it implements `InvoiceRule` directly instead of extending
`PercentageDiscountRule`, so the clamp never runs, and a configured rate
above 1.0 pushes the invoice total negative. `LiskovSubstitutionTest` proves
both halves: every `PercentageDiscountRule` subtype stays within
`[-subtotal, 0]` no matter what rate it is given, and
`ClearanceEventDiscountRule` does not.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

Standalone from this directory:

```sh
cd examples/03-solid-first-principles
mvn test
```

From the repo root's aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 29 JUnit 5 cases (AssertJ assertions, one Mockito
mock verifying each rule receives the invoice's original subtotal). Observed
locally with Temurin 25.0.2:

```
Tests run: 29, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see all three principles play out on one invoice rather than just reading
an assertion that says they hold, run the demo:

```sh
./demo.sh
```

Expected output:

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

## Files

```
examples/03-solid-first-principles/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run BillingDemo
  src/main/java/com/patterncatalyst/designpatterns/ch03/billing/
    LineItem.java                 # record: description, quantity, unitPriceCents
    Invoice.java                   # record: id, customerName, lineItems
    Totals.java                     # record: subtotalCents, totalCents
    InvoiceRule.java                 # the Open/Closed extension point
    PercentageTaxRule.java            # a positive adjustment
    PercentageDiscountRule.java        # abstract base: final clamp, protected hook
    StandardDiscountRule.java           # fixed-rate discount
    LoyaltyDiscountRule.java             # base rate + bonus per loyalty year
    ClearanceEventDiscountRule.java       # counter-example: skips the clamp
    FlatServiceFeeRule.java                # a flat adjustment, added later
    InvoiceCalculator.java                  # SRP: math only; OCP: rule list
    InvoiceFormatter.java                    # SRP: formatting only
    InvoiceRepository.java                    # persistence interface
    InMemoryInvoiceRepository.java             # SRP: persistence only
    BillingDemo.java                            # exercises all three principles
  src/test/java/com/patterncatalyst/designpatterns/ch03/billing/
    InvoiceCalculatorTest.java   # totals, rule independence, OCP zero-edit proof
    LiskovSubstitutionTest.java   # clamp invariant held and violated
    InvoiceFormatterTest.java      # receipt text contents
    InMemoryInvoiceRepositoryTest.java # save/find roundtrip
    LineItemTest.java                   # value-type validation
    InvoiceTest.java                     # defensive copy + immutability
```
