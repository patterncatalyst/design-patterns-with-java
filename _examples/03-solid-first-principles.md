---
layout: example
order: 3
title: "SOLID I: SRP, OCP, LSP in a billing engine"
description: A freight-invoicing engine split by responsibility, extended without edits, and kept substitutable, proven by JUnit 5.
github_path: examples/03-solid-first-principles
card_eyebrow: "Chapter 3 · SOLID I"
card_title: "SRP, OCP, LSP in a billing engine"
card_blurb: "A freight-invoice calculator split into calculation, formatting, and persistence (SRP); an InvoiceRule abstraction that takes new rules without edits (OCP); and a discount rule that stays substitutable, with a counter-example that does not (LSP)."
---

A freight-invoicing engine shows the first three SOLID principles as concrete
code. The calculation, formatting, and persistence each live in their own type
(`InvoiceCalculator`, `InvoiceFormatter`, `InMemoryInvoiceRepository`), so one
reason to change never drags in another. The calculator depends on an
`InvoiceRule` abstraction, so a `FlatServiceFeeRule` arrives as a new type with
no edit to the calculator or the rules that came before it. A
`PercentageDiscountRule` clamps its adjustment in a `final` method so every
subtype stays substitutable, and a deliberately broken `ClearanceEventDiscountRule`
shows what a Liskov violation costs (an invoice driven negative).

Value types (`LineItem`, `Invoice`, `Totals`) are Java `record`s. The JUnit 5 +
AssertJ suite proves the behavior: correct totals across rules, a brand-new rule
needing zero calculator edits, and the substitutable subtypes honoring the base
contract while the counter-example breaks it.

## Build, run, observe

With the toolchain from the prerequisites chapter (JDK 25, Maven):

```sh
cd examples/03-solid-first-principles
mvn -q test          # 29 tests pass
./demo.sh            # SRP receipt, OCP rule breakdown, LSP totals (and the violation going negative)
```

The module also runs from the aggregator at the repository root with
`mvn -q -f examples/pom.xml test`.
