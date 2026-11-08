---
title: "Characterization tests; Fowler vs Feathers"
order: 31
layout: tutorial
sectionid: docs
part: "changing-inherited-code"
description: "Characterization tests lock in the behavior of an inherited freight-quote calculator before anyone understands it. Fowler's Refactoring assumes that safety net exists, Feathers' Working Effectively with Legacy Code builds it, and the two run in sequence on one calculator."
duration: "35 min"
---

## Learning objectives

By the end of this chapter you can:

- Write a characterization test: probe inherited code with representative and boundary inputs, record what it returns, and assert that value rather than what it "should" return.
- Keep a quirk you find as pinned behavior, and fix it only as a separate, visible change.
- State each book's starting assumption: Fowler's *Refactoring* presumes a fast, trustworthy suite; Feathers's *Working Effectively with Legacy Code* exists because there usually isn't one.
- Characterize first, then apply named refactorings, and confirm the result against the same pinned assertions.
- Judge whether code needs a safety net, a refactoring, or neither because nothing depends on it.

{% include excalidraw.html name="31-characterization-as-a-safety-net" caption="Figure 31.1: characterization tests as the safety net under a refactor. Probing the legacy calculator pins its current outputs into a test suite; the Fowler refactor reshapes the code underneath that suite, and the same pinned assertions staying green is the proof nothing observable changed." %}

## Why this matters

A freight-quote calculator has no known author and no tests. It computes transit days and a surcharge, three carriers depend on its exact numbers, and its weight calculation looks like it rounds down where it should round up. Anyone who touches it first asks: would this change break something a customer relies on, and how would we find out before they do?

The answer doesn't start with reading the code more carefully. It starts with running it and writing down what it does. A **characterization test** is built from an observation of what code does, recorded as the value a later change has to preserve. A test for new code starts from a requirement and checks the code meets it; a characterization test starts from the code and checks the code still agrees with itself. They fail for different reasons, and mixing them up is the most common mistake when adding tests to inherited code.

The next question is which book to follow: Fowler's *Refactoring* or Feathers's *Working Effectively with Legacy Code*. They're neither interchangeable nor competing. They cover two halves of one job, in a fixed order.

## Probing a legacy calculator for its actual behavior

The code is in `examples/31-characterization`. `LegacyFreightQuoteCalculator.quote` takes a carrier code, two zones, a weight, a residential flag and a declared value as positional arguments. It dispatches on the carrier through two if/else chains, one for transit days and a near-copy for the base rate, because whoever added `express` copied the first chain:

```java
int transitDays;
if (carrierCode.equals("ground")) {
    transitDays = originZone.equals(destinationZone) ? 2 : 5;
} else if (carrierCode.equals("air")) {
    transitDays = originZone.equals(destinationZone) ? 1 : 2;
} else if (carrierCode.equals("express")) {
    transitDays = 1;
} else {
    throw new IllegalArgumentException("unknown carrier: " + carrierCode);
}

long baseCents;
if (carrierCode.equals("ground")) {
    baseCents = 500L;
} else if (carrierCode.equals("air")) {
    baseCents = 1500L;
} else if (carrierCode.equals("express")) {
    baseCents = 3000L;
} else {
    throw new IllegalArgumentException("unknown carrier: " + carrierCode);
}
```

The duplication is a real smell, but a characterization test cares first about whether the method is deterministic enough to pin. It is: no clock, no database, no randomness. Every path is a pure function of its arguments, so every path can be characterized today with no structural change.

Pick inputs by reading for every place a condition could flip and probing near it. The weight surcharge is the clearest case:

```java
int weightTier = (int) (weightKg / 10.0);
long weightSurchargeCents = weightTier * 150L;
```

What happens at 10 kilograms? Run it at 9.99kg and 10.0kg and record the answer:

```java
FreightQuote justUnder = legacy.quote("ground", "A", "B", 9.99, false, 50_000L);
FreightQuote atBoundary = legacy.quote("ground", "A", "B", 10.0, false, 50_000L);

assertThat(justUnder.surchargeCents()).isZero();
assertThat(atBoundary.surchargeCents()).isEqualTo(150L);
```

That's a characterization test complete: call the code, observe the result, assert it. It doesn't claim 150 cents is the right charge for crossing the boundary. It claims that's what the code charges now, and that any change to the number has to arrive as its own visible test update, never as a side effect.

The same probing finds two more quirks:

| Quirk | Rule in the code | Observed |
| --- | --- | --- |
| Weight tier cliff | `(int) (weightKg / 10.0)`, 150 cents a tier | 9.99kg pays $0.00, 10.0kg pays $1.50 |
| Residential threshold | `weightKg > 20.0`, strict | 20.0kg pays nothing extra; 20.01kg pays $2.50 |
| Declared-value insurance | `(long) (excessCents * 0.01)` over $1,000, truncated | $2.50 over pays 2 cents, not 2.5; 1 cent over pays nothing |

None of these came from a specification. Each came from running the code near a boundary. A parameterized table pins eight shipments, each pairing a readable case name with the inputs and the four `FreightQuote` fields the legacy calculator returned:

```java
Arguments.of("ground, cross zone, exactly at the 10kg tier boundary",
        "ground", "A", "B", 10.0, false, 50_000L,
        5, 500L, 150L, 650L),
Arguments.of("ground, cross zone, just under the 10kg tier boundary",
        "ground", "A", "B", 9.99, false, 50_000L,
        5, 500L, 0L, 500L),
```

One AssertJ `extracting(...)` chain checks all four fields, so a failure names the number that moved. Eight rows is one per boundary found plus a couple of ordinary cases far from any boundary, which show the common path still works. A row at 10,000 kilograms would add coverage and no information; the table stops where the boundaries stop.

## Two books, two starting assumptions

With the calculator characterized, its duplicated dispatch is safe to change for the first time, because a suite will say if a change breaks something. Fowler's *Refactoring* assumes the reader already has that suite. Feathers's *Working Effectively with Legacy Code* exists to build it.

| Dimension | Fowler, *Refactoring* | Feathers, *Working Effectively with Legacy Code* |
|---|---|---|
| Starting assumption about tests | A fast, trustworthy suite already exists and stays green through every step. | No such suite exists yet, and the code often has no obvious way to get one. |
| Primary goal | Reshape a design for clarity: smaller functions, named concepts, less duplication, same behavior. | Make a specific piece of code observable enough to write a test against it at all. |
| Risk profile | Managed by keeping each step small and mechanical, re-running the tests after every one. | Managed by changing as little as possible; current behavior, including its bugs, is protected until a test proves otherwise. |
| Core technique | A catalogue of named, mechanical transformations (Extract Function, Replace Conditional with Polymorphism, Introduce Parameter Object, among many others). | Characterization tests plus a smaller catalogue of dependency-breaking moves aimed at making one collaborator substitutable or one calculation observable. |
| Mindset | "This code already works and is trusted; make it easier to read and extend." | "This code might not work the way anyone thinks; find out what it actually does before touching it." |

The risk-profile row trips people who've read only one of the two. Fowler's refactorings are safe because a suite catches a mistake within seconds, and that safety is borrowed from the asset Feathers's techniques create. Refactoring untested code isn't dangerous because a transformation is risky; most are small and well documented. It's dangerous because nothing tells you when you got one wrong. Fowler starts from trust in current behavior and asks how to keep it while improving the code. Feathers starts from doubt that anyone knows the behavior well enough to say what "the same" means, and answers with a characterization test before any question of design.

## How they work together

The order follows from the contrast. Feathers runs first because its job is producing the tests Fowler assumes. Fowler runs second because his catalogue is where the design improves. Reverse them and each stage loses what makes it safe.

On the calculator, the pinned table and the named quirk cases come first, with no structural change. Then three named refactorings reshape the same arithmetic. Replace Conditional with Polymorphism retires the two carrier chains in favor of one `CarrierProfile` per carrier, owning both its transit-day rule and its base rate:

```java
public sealed interface CarrierProfile permits GroundCarrierProfile, AirCarrierProfile, ExpressCarrierProfile {
    int transitDays(String originZone, String destinationZone);
    long baseCents();

    static CarrierProfile forCode(String carrierCode) {
        return switch (carrierCode) {
            case "ground" -> new GroundCarrierProfile();
            case "air" -> new AirCarrierProfile();
            case "express" -> new ExpressCarrierProfile();
            default -> throw new IllegalArgumentException("unknown carrier: " + carrierCode);
        };
    }
}
```

- **Extract Function** gives each surcharge rule a named method: `weightSurchargeCents`, `residentialSurchargeCents`, `declaredValueSurchargeCents`.
- **Introduce Parameter Object** collapses the six positional arguments into one `ShipmentDetails`, so a caller builds a shipment once instead of remembering the order.

None of the three touches the arithmetic. The weight tier still truncates, the residential threshold is still strict, the insurance surcharge still rounds toward zero. Fowler's catalogue reorganizes code; it doesn't audit it for bugs. A refactor that quietly "corrected" the truncation while renaming methods would read as a clean diff and still be the wrong change, at the wrong time, hidden in a commit that claimed to be structural.

The proof is the same table run twice. The eight rows go through `FreightQuoteCalculator` with no expected value changed, and the two calculators agree on every named quirk. Reshaping the internals changed nothing a caller can observe, quirks included. A characterization test doesn't bless behavior as correct. It makes "unchanged" checkable, so that fixing a quirk becomes its own commit with its own updated assertion.

> **🤖 Explore with your agent**
>
> "In `examples/31-characterization`, `FreightQuoteCharacterizationTest` pins `LegacyFreightQuoteCalculator` as it behaves today, including the 10kg tier cliff, the strict `weightKg > 20.0` residential threshold and the `(long)` truncation of the declared-value surcharge. Refactor `FreightQuoteCalculator` only: keep the sealed `CarrierProfile` and `ShipmentDetails`, and extract any further named methods you like, but change no arithmetic and no expected value in its table. Don't round the insurance surcharge or move a threshold; list any quirk you'd fix as a separate proposal."
>
> Check: `mvn -q -f examples/31-characterization/pom.xml test -Dtest=FreightQuoteCharacterizationTest`, with no edits to the expected values in the diff.

## When each applies

- **No test covers the path:** Feathers first. The calculator needed it because none of its three quirks was observable without running the code, and no refactoring is safe on a calculation nobody can verify stayed the same.
- **A test covers it:** Fowler. A fully covered codebase with a tangled conditional needs nothing from Feathers; the safety net already runs in seconds.
- **Nothing calls the code:** neither. A characterization test protects behavior something depends on. Check for callers first; sometimes the answer is to delete the file.
- **Making it testable costs more than the rules it holds:** consider a rewrite. Feathers's techniques stay minimal, one broken dependency at a time, which helps when one or two things block a test. When a component has a dozen tangled collaborators and a first test needs surgery out of proportion to its few business rules, compare a bounded rewrite against the in-place work. A rewrite is still checked the same way: old and new code against the same pinned table.

## Build, run, observe

From the repository root:

```sh
mvn -f examples/pom.xml test
```

Or standalone:

```sh
cd examples/31-characterization
mvn test
```

All 22 cases pass: the eight-row table against both calculators, four named quirk cases and two unknown-carrier error paths. To see the two calculators side by side:

```sh
examples/31-characterization/demo.sh
```

```
== Legacy and refactored calculators agree on every shipment ==
ground   A/A -> $5.00  total $5.00  [identical]
ground   A/B -> $6.50  total $6.50  [identical]
air      A/A -> $20.50  total $20.50  [identical]
express  A/B -> $30.02  total $30.02  [identical]

== A quirk the characterization suite pins: the 10kg tier cliff ==
9.99kg:  surcharge $0.00
10.00kg: surcharge $1.50  (0.01kg more weight, $1.50 more surcharge)
```

- **"Fix" a quirk during the refactor.** In a scratch copy, change the insurance surcharge in `FreightQuoteCalculator` from `(long) (excessCents * 0.01)` to `Math.round(excessCents * 0.01)`. Two cases fail: one table row against the refactored calculator and the cross-calculator quirk check. The legacy pins still pass.

## For deeper coverage

- Michael Feathers, [*Working Effectively with Legacy Code*](https://www.oreilly.com/library/view/working-effectively-with/0131177052/) (Prentice Hall): the characterization-test technique and the dependency-breaking catalogue behind the first stage.
- Martin Fowler, [*Refactoring: Improving the Design of Existing Code*](https://martinfowler.com/books/refactoring.html), 2nd ed. (Addison-Wesley): the named-refactoring catalogue, including Extract Function, Replace Conditional with Polymorphism and Introduce Parameter Object, applied once a pinned suite exists.
- Gerard Meszaros, [*xUnit Test Patterns: Refactoring Test Code*](https://www.informit.com/store/xunit-test-patterns-refactoring-test-code-9780132800051) (Addison-Wesley): the four-phase test structure and test-double vocabulary.
- The [bibliography page]({{ '/bibliography/' | relative_url }}) lists the full reading list.
