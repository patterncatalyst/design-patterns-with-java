---
title: "Characterization tests; Fowler vs Feathers"
order: 31
layout: tutorial
sectionid: docs
part: "changing-inherited-code"
description: "Writing characterization tests to lock in behavior you do not yet understand against a gnarly inherited freight-quote calculator, then the methodology contrast between Martin Fowler's Refactoring, which assumes a safety net already exists, and Michael Feathers' Working Effectively with Legacy Code, which exists to build one, and how the two combine in sequence on one worked example."
duration: "35 min"
---

## Learning objectives

By the end of this chapter you can:

- Write a characterization test against inherited code you do not fully
  trust: probe it with representative and boundary inputs, record what it
  actually returns, and assert that recorded value rather than what the
  code "should" return.
- Explain why a characterization test pins current behavior, quirks
  included, and why fixing a quirk you find along the way is a separate,
  visible change rather than something a refactor is allowed to do quietly.
- State the starting assumption each book makes about tests: Fowler's
  *Refactoring* presumes a fast, trustworthy suite already exists; Feathers'
  *Working Effectively with Legacy Code* exists because that suite usually
  does not, and its technique catalogue builds one.
- Run both stages on one component: characterize first, then apply named,
  behavior-preserving refactorings once every path sits under a test, and
  check the result by re-running the same pinned assertions against the
  refactored code.
- Judge, for a piece of code in front of you, whether it needs a safety net
  first, a named refactoring next, or neither, because nothing currently
  depends on its behavior.

{% include excalidraw.html name="31-characterization-as-a-safety-net" caption="Figure 31.1: characterization tests as the safety net under a refactor. Probing the legacy calculator pins its current outputs into a test suite; the Fowler refactor reshapes the code underneath that suite, and the same pinned assertions staying green is the proof nothing observable changed." %}

## Why this matters

A freight-quote calculator shows up in the codebase with no author anyone
can name and no test anywhere near it. It computes transit days and a
surcharge for a shipment, it has been in production long enough that three
different carriers depend on its exact numbers, and reading it closely
reveals a weight calculation that looks like it rounds down when it should
round up. Nobody wants to touch it, and everybody who has to touch it asks
the same question first: does changing this risk breaking something a
customer is currently relying on, and how would anyone find out before a
customer does.

That question has a direct answer, and it does not start with reading the
code more carefully. It starts with running the code and writing down what
it currently does. A characterization test is exactly that: a test built
not from a specification of what the code should do, but from an
observation of what it actually does, recorded as the expected value a
later change has to preserve. The word choice matters. A unit test for new
code typically starts from a requirement and checks the code meets it; a
characterization test for inherited code starts from the code and checks
that the code still agrees with itself. Those are different exercises with
different failure meanings, and conflating them is the single most common
mistake engineers make the first time they try to add tests to code they
inherited rather than wrote.

Once that first test exists, a second, related question follows close
behind: which book answers what to do next, Martin Fowler's *Refactoring*
or Michael Feathers' *Working Effectively with Legacy Code*. Both books are
about improving existing code without breaking it, and a lot of advice
treats them as interchangeable or competing. They are neither. They answer
different halves of the same job, and the freight-quote calculator this
chapter builds is sized to need both halves in the order they actually
have to run.

## Probing a legacy calculator for its actual behavior

The example behind this chapter lives in `examples/31-characterization`.
`LegacyFreightQuoteCalculator` prices a shipment from a carrier code, two
zone identifiers, a weight, a residential flag, and a declared value, all
six as positional arguments on one `quote` method. Internally it dispatches
on the carrier name through two separate if/else chains, one that picks the
transit days and a second, nearly identical chain a few lines later that
picks the base rate, because whoever added the `express` carrier years ago
copied the first chain instead of extending something shared:

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

That duplication is a legitimate design smell and a later section of this
chapter deals with it directly, but it is not the first thing a
characterization test cares about. The first thing it cares about is
whether this method is even deterministic enough to pin, and `quote` is: no
clock, no database, no random number anywhere in it. Every path is a pure
function of its six arguments, which means every path is characterizable
today, with no structural change needed first.

Characterizing it starts with picking inputs, not by guessing what matters,
but by reading the code for every place a condition could flip and probing
near that line. The weight surcharge is the clearest example:

```java
int weightTier = (int) (weightKg / 10.0);
long weightSurchargeCents = weightTier * 150L;
```

Reading this line raises an immediate question: what happens right at a
10-kilogram boundary? Running the calculator at 9.99kg and at 10.0kg
answers it directly rather than by inspection:

```java
FreightQuote justUnder = legacy.quote("ground", "A", "B", 9.99, false, 50_000L);
FreightQuote atBoundary = legacy.quote("ground", "A", "B", 10.0, false, 50_000L);

assertThat(justUnder.surchargeCents()).isZero();
assertThat(atBoundary.surchargeCents()).isEqualTo(150L);
```

That is a characterization test in its complete form: call the code,
observe the actual result, assert it. The assertion does not claim 150
cents is the right surcharge for crossing a 10-kilogram boundary by one
gram; it claims that is what this code currently charges, and that any
future change to this method has to either preserve that number or change
it through its own visible test update, never as a side effect of some
other change to the code. The same probing method turns up two more
quirks. A residential surcharge applies only when
`weightKg > 20.0`, a strict inequality, so a shipment at exactly 20.0
kilograms pays nothing extra while one one-hundredth of a kilogram over
pays $2.50. A declared-value insurance surcharge is computed as `(long)
(excessCents * 0.01)`, a cast that truncates toward zero instead of
rounding, so a shipment two dollars and fifty cents over the insurable
threshold is charged 2 cents of surcharge on the true 2.5-cent calculation,
and a shipment only one cent over the threshold is charged nothing at all,
because 0.01 cents truncates to zero.

None of these three numbers, 150, strict-greater-than, truncate-not-round,
was found by reading a specification. Each was found by running the code
near a boundary and writing down what came back. `FreightQuoteCharacterizationTest`'s
`representativeShipments` table does this systematically across eight rows,
pairing a human-readable case name with a carrier, a zone pair, a weight, a
residential flag, a declared value, and the four fields of the
`FreightQuote` the legacy calculator actually returned for that input:

```java
Arguments.of("ground, cross zone, exactly at the 10kg tier boundary",
        "ground", "A", "B", 10.0, false, 50_000L,
        5, 500L, 150L, 650L),
Arguments.of("ground, cross zone, just under the 10kg tier boundary",
        "ground", "A", "B", 9.99, false, 50_000L,
        5, 500L, 0L, 500L),
```

A `@ParameterizedTest` runs every row through `LegacyFreightQuoteCalculator`
and asserts the four pinned fields with one AssertJ `extracting(...)`
chain, so a failure names exactly which of the four numbers moved rather
than reporting a single opaque `false`. Eight rows is not an arbitrary
count; it is one row per boundary this chapter's reading of the code
turned up, plus a couple of ordinary cases far from any boundary, which
matter precisely because they prove the common path still works once the
boundary cases are covered. A ninth row covering a weight of 10,000
kilograms would add coverage without adding information; this suite stops
where the boundaries stop, not at a round number chosen in advance.

## Two books, two starting assumptions

With the legacy calculator now characterized, the question of what to do
with its obvious design smell, the duplicated carrier dispatch, has a clear
answer available for the first time: it is now safe to change, because a
suite exists that will say so if a change breaks something. That safety is
exactly the asset Fowler's *Refactoring* assumes a reader already has
before opening the book, and exactly the asset Feathers' *Working
Effectively with Legacy Code* exists to build when a reader does not have
it yet.

| Dimension | Fowler, *Refactoring* | Feathers, *Working Effectively with Legacy Code* |
|---|---|---|
| Starting assumption about tests | A fast, trustworthy suite already exists and stays green through every step. | No such suite exists yet, and the code often has no obvious way to get one. |
| Primary goal | Reshape a design for clarity: smaller functions, named concepts, less duplication, same behavior. | Make a specific piece of code observable enough to write a test against it at all. |
| Risk profile | Managed by keeping each step small and mechanical, re-running the suite after every one. | Managed by changing as little as possible; current behavior, including its bugs, is protected until a test proves otherwise. |
| Core technique | A catalogue of roughly ninety named, mechanical transformations (Extract Function, Replace Conditional with Polymorphism, Introduce Parameter Object, among many others). | Characterization tests plus a smaller catalogue of dependency-breaking moves aimed at making one collaborator substitutable or one calculation observable. |
| Mindset | "This code already works and is trusted; make it easier to read and extend." | "This code might not work the way anyone thinks; find out what it actually does before touching it." |

The risk-profile row is the one that trips engineers who have read only one
of the two books. Fowler's refactorings are safe specifically because a
test suite exists to catch a mistake within seconds of making it; that
safety is borrowed from an asset Feathers' techniques exist to create in
the first place. Applying a Fowler-style refactoring to code with no tests
is not dangerous because the individual transformation is risky in
isolation, most of the catalogue's entries are small, mechanical, and
well-documented. It is dangerous because nothing will tell the refactorer
if they got it wrong. The mindset row carries the rest of the distinction:
Fowler's book opens from a position of trust in the code's current
behavior and asks how to make the code better without changing that
behavior; Feathers' book opens from a position of doubt about whether
anyone currently understands the behavior well enough to say what "the
same" would even mean, and answers that doubt with a characterization test
before asking anything about design quality at all.

## How they work together

Once that contrast is named, the order the two books run in follows
directly from it, rather than needing to be chosen by taste. Feathers runs
first, because its entire job is producing the one asset Fowler's catalogue
assumes already exists. Fowler runs second, once that asset is in place,
because his catalogue is where the actual design improvement happens. This
is not two competing schools a team picks between. It is two sequential
stages of one job, and reversing the order removes the thing that makes
each stage safe.

The worked sequence in this chapter's example makes that order concrete.
Characterization came first: `representativeShipments` plus the quirk-named
tests in `FreightQuoteCharacterizationTest`'s `PinnedQuirks` nested class
pin the legacy calculator's behavior across every boundary this chapter
found, with no structural change made yet. Only once that suite exists does
the Fowler stage begin, applying three named refactorings to the exact same
arithmetic. Replace Conditional with Polymorphism retires the two
duplicated carrier chains in favor of one `CarrierProfile` per carrier,
each owning both its transit-day rule and its base rate together:

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

Extract Function pulls each surcharge rule out of one long method into its
own named method, `weightSurchargeCents`, `residentialSurchargeCents`,
`declaredValueSurchargeCents`, each answerable by its name instead of its
body. Introduce Parameter Object collapses the six positional arguments
into one `ShipmentDetails` value, so a caller builds a shipment once instead
of remembering an argument order six deep. None of the three refactorings
touches a single piece of arithmetic; the weight tier still truncates, the
residential threshold is still a strict greater-than, the declared-value
surcharge still rounds toward zero. Fowler's catalogue reorganizes code. It
does not audit it for bugs, and conflating the two is a mistake this
chapter's example is built specifically to make visible: a refactor that
quietly "corrected" the truncation while renaming methods would still pass
code review as a clean diff and would still be the wrong change, made at
the wrong time, hidden inside a commit that claimed to be purely
structural.

The proof that the refactor preserved every pinned behavior is the same
table, run twice.
`refactoredCalculatorMatchesPinnedBehavior` feeds the identical
`representativeShipments` rows into `FreightQuoteCalculator` instead of
`LegacyFreightQuoteCalculator`, with not one expected value changed, and
`refactorReproducesEveryPinnedQuirk` checks the two calculators agree on
the specific boundary cases by name. Every assertion passes, which is
direct evidence that reshaping the code's internal structure changed
nothing a caller can observe, quirks included. That last clause carries the
chapter's main point: a characterization test's job is not to bless the
code's behavior as correct. It is to make "unchanged" checkable, so that
when someone decides a quirk should be fixed, that decision is visible in
its own commit, with its own updated assertion, rather than buried inside a
refactor that was never supposed to change anything a test could see.

> **AI sidebar.** Ask an LLM pairing partner to "add tests and clean up this
> legacy calculator" and a frequent response skips straight to Fowler's
> catalogue: a generated diff extracts functions, introduces a parameter
> object, and renames a few things, often with well-chosen
> transformations read in isolation. What that answer typically skips is
> the Feathers question that has to come first: does a characterization
> test exist yet to say whether any of this changed observable behavior? A
> generated refactor applied before a pinned test exists is not wrong
> because the transformation is poorly chosen. It is wrong because nothing
> confirms it preserved behavior, and a diff that compiles and reads well is
> not the same claim as a diff that was checked against the code's actual
> prior output. A second, subtler failure shows up specifically around
> quirks: a generated test suite for `LegacyFreightQuoteCalculator` will
> often assert what the weight surcharge or the declared-value surcharge
> "should" compute, correctly rounded, rather than what the method actually
> returns today. That test looks reasonable and fails the moment it runs
> against the real method, and the useful move is not to loosen the
> assertion until it passes; it is to ask which number is the actual
> contract this test is supposed to protect, the code's current output or a
> corrected one chosen because the current behavior is wrong,
> and write the test for that choice.

## When each applies

Reach for Feathers' techniques first whenever the answer to "does a
test already cover this path" is no, and reach for Fowler's catalogue the
moment that answer becomes yes. A codebase with full coverage and a
tangled conditional needs Fowler and nothing from Feathers, because there
is no missing safety net to build; a suite already runs in seconds and
already catches a mistake. The freight-quote calculator in this chapter
needed Feathers first because none of its three quirks were observable
without running the code, and no named refactoring from Fowler's catalogue
is safe to apply to a calculation nobody can currently verify stayed the
same.

Neither book is the right tool, and spending effort on either one is pure
cost with nothing purchased, for code with no callers left. A
characterization test exists to protect behavior something currently
depends on; writing one for a method nothing calls anymore is ceremony
around a decision that should have been deleting the file. Check for
callers before spending a single characterization test on a class, because
the correct answer is sometimes "this is already dead code," not "this
needs a safety net."

The combined workflow also has a natural stopping point on the Feathers
side. Its techniques stay minimal by design, one broken dependency or
one named interface at a time, and that restraint is a feature when one or
two things are blocking a test. It stops being a feature once a component
has a dozen tangled collaborators and getting even a first characterization
test in place would require surgery disproportionate to the handful of
business rules the component actually contains. At that point the
comparison worth making is not "which technique first" but "does a bounded
rewrite, validated against whatever characterization tests are cheap to
write today, cost less than the structural work this component would take
to make testable in place." That decision belongs to its own analysis, and
reaching it does not require abandoning either book: a rewrite's output is
still checked the same way this chapter checked a refactor, by running old
and new code against the same pinned table and confirming they agree.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this
loop. From the repository root, the aggregator picks up
`examples/31-characterization` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/31-characterization
mvn test
```

Either path runs the same 22 JUnit 5 cases: the pinned table run against
both calculators, the four named quirk tests, and the two unknown-carrier
error-path tests. Observed locally on Fedora with Temurin 25:

```
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see the legacy and refactored calculators agree on identical totals,
and the weight-tier cliff pinned by the suite, rather than just reading an
assertion that says so, run the example's own demo script:

```sh
examples/31-characterization/demo.sh
```

It prices four shipments through both calculators side by side, then
prints the 9.99kg-versus-10.0kg boundary directly:

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

Every `[identical]` line is the same claim the test suite makes, visible at
the terminal: reorganizing the code under the pinned table changed nothing
a caller can observe.

## For deeper coverage

- Michael Feathers, [*Working Effectively with Legacy Code*](https://www.oreilly.com/library/view/working-effectively-with/0131177052/) (Prentice Hall): the characterization-test technique and the dependency-breaking catalogue this chapter's first stage draws from.
- Martin Fowler, [*Refactoring: Improving the Design of Existing Code*](https://martinfowler.com/books/refactoring.html), 2nd ed. (Addison-Wesley): the named-refactoring catalogue, including Extract Function, Replace Conditional with Polymorphism, and Introduce Parameter Object, this chapter's second stage applies once a pinned suite exists.
- Gerard Meszaros, [*xUnit Test Patterns: Refactoring Test Code*](https://www.informit.com/store/xunit-test-patterns-refactoring-test-code-9780131495050) (Addison-Wesley): the four-phase test structure and test-double vocabulary behind every assertion in this chapter's suite.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
