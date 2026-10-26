---
title: "Refactor, rewrite, or strangler fig"
order: 32
layout: tutorial
sectionid: docs
part: "changing-inherited-code"
description: "The decision between an in-place refactor, a wholesale rewrite, and the strangler fig, taught against a legacy accessorial fee calculator carrying a renegotiated fuel rate nobody documented, a weight-based fee waiver, and a carrier-specific oversize penalty. A strangler router sends each shipment to the legacy or modern calculator behind one AccessorialFeeCalculator interface, migrating one shipment at a time while a shared characterization suite proves the new calculator agrees with the old one before the cutover."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Name the concrete factors that decide refactor versus rewrite: risk,
  cost, how much undocumented knowledge lives in the current code, how
  well that code is understood today, whether a safety net already
  exists, and what being wrong costs in each direction.
- Explain why "let's just rewrite it" usually loses: lost embedded
  knowledge, the dual-maintenance burden of running two systems, and a
  long delivery window with nothing shipping in the meantime.
- Recognize when a bounded rewrite wins over continued refactoring, and
  state the conditions that make that call defensible rather than a
  reflex.
- Describe the strangler fig approach: a stable interface, a routing seam
  that chooses between old and new behavior per request, and traffic
  shifted incrementally as confidence grows.
- Build a characterization suite that both an old and a new
  implementation must pass, so a disagreement is caught before a
  customer ever sees it.
- Judge when the overhead of a seam and a gradual cutover is not worth
  paying, and a direct in-place change or a straight swap is the better
  call.

{% include excalidraw.html name="32-strangler-fee-router" caption="Figure 32.1: a shipment service calls AccessorialFeeCalculator; StranglerFeeCalculatorRouter sits behind that interface, consults MigratedShipmentRegistry, and sends each shipment to LegacyAccessorialFeeCalculator or ModernAccessorialFeeCalculator." %}

## Why this matters

Every inherited codebase eventually produces one component nobody wants to
touch directly. In a shipping system it is rarely the checkout flow or the
label printer; it is the accessorial fee calculator, the small, unglamorous
piece that adds a fuel surcharge, a residential delivery fee, a liftgate
fee, and an oversize penalty on top of the base freight charge. It has
worked for years. Finance trusts the numbers it produces. Nobody fully
understands why one carrier's fuel rate is 0.15 instead of 0.12, or why the
residential fee disappears under twenty kilograms, and the person who could
have explained either rule left the company two reorganizations ago.

The question that arrives at this point is never abstract: does the team
keep refactoring this component in place, moving it toward a better design
one safe step at a time, or does it set the component aside and build a
replacement from a cleaner starting point? Both answers are correct some
of the time, which is exactly why this decision deserves a repeatable set
of factors rather than a gut call made under deadline pressure. A team
that always refactors will occasionally spend months polishing a design
that should have been replaced wholesale. A team that always rewrites will
occasionally throw away years of hard-won edge-case handling and spend a
quarter reimplementing it badly from memory. This chapter gives you a
repeatable way to make that call, and a specific technique, the
strangler fig, for the case where a bounded rewrite is the right answer
and the current system cannot stop serving traffic while the replacement
gets built.

## The decision factors

Six factors drive this decision, and none of them is "how old is the
code" or "how ugly is the code," two questions that feel diagnostic and
mostly are not. Legacy code that is old and unlovely but well covered by
tests and fully understood is a refactoring candidate in good standing.
Code written last quarter that nobody can explain and that has no tests
at all is a rewrite candidate despite being brand new.

**Risk.** What breaks if this component misbehaves, and who notices
first? An accessorial fee calculator that undercharges every residential
delivery for six months before an audit catches it carries more risk than
one that throws a visible error the moment something is wrong. High risk
argues for the smaller, more reversible steps that
refactoring and an incremental strangler fig cutover both offer, since
either one lets the team stop and back out the instant something looks
wrong.

**Cost.** Refactoring in place costs engineering time spread over a long
period, with the system shipping the whole time. A rewrite costs a
concentrated block of time with a specific, nameable risk: the project
runs long, the business keeps needing changes to the old calculator in
the meantime, and now two systems need those changes applied twice. Cost
is not just calendar time; it is the shape of the spending.

**How much undocumented knowledge lives in the current code.** This is
the factor teams underweight most often. A renegotiated fuel surcharge
rate, a weight threshold that waives a fee, a carrier-specific penalty an
auditor once reverse-engineered from an invoice, none of that is
incidental complexity a rewrite can skip past. It is the business logic.
Michael Feathers' definition of legacy code, code without tests rather
than code by age, matters here for a specific reason: the same absence of
tests that makes the code risky to change is usually paired with an
absence of documentation that makes its behavior the only surviving
record of a decision the business made years ago. A rewrite that treats
that behavior as noise to clean up instead of a requirement to preserve
ships something that compiles and reads well, yet stops honoring a rule
nobody remembered to mention because nobody remembered it existed outside
the code.

**How well the current code is understood.** A component three engineers
on the current team can explain line by line is a different proposition
from one where "removing that branch broke something once, so leave it"
is the closest thing to documentation that exists. The less understood a
component is, the more a rewrite is a bet on re-deriving knowledge the
organization already paid once to learn, and the more a characterization
suite, pinning current behavior with tests before changing anything, pays
for itself by turning tribal knowledge into an executable, enforceable
fact.

**Whether a safety net exists.** A component backed by a thorough test
suite can be refactored aggressively, because a mistake announces itself
immediately. A component with no tests at all is not safe to refactor in
any meaningful sense: every change is a bet that nothing invisible
depends on the thing just touched. Building that safety net is itself the
first concrete step regardless of which path the team ultimately
chooses. A refactor without a safety net is not more conservative than a
rewrite; it is often riskier, because it changes the one thing that was
observed working while having no way to detect when it stops.

**The cost of being wrong.** If refactoring turns out to be the wrong
call, the cost is wasted time and a component marginally better than
before. If a rewrite turns out to be the wrong call, the cost is a
half-built replacement, a legacy calculator nobody has touched in a year
and has drifted further from understood, and a team explaining to
leadership why the thing that was supposed to replace the old calculator
does not exist yet and the old one still cannot be touched safely
either. The two failure modes are not symmetric, which is itself a
reason most components should default toward refactoring unless a
specific force argues otherwise.

## Why rewrite from scratch usually loses

"Let's just rewrite it" is the single most expensive sentence available
to a team staring at an unpleasant component, and it loses for three
concrete, repeatable reasons rather than because rewrites are inherently
bad ideas.

The first is lost embedded knowledge. A legacy fee calculator's quirks
are rarely bugs; they are fossilized decisions. A fuel rate bumped during
a contract renegotiation, a weight floor that waives a fee, a doubled
penalty for one carrier's oversized freight, each one was put there
because something, a contract, an audit finding, a carrier agreement,
needed it at the time. A rewrite built from a clean mental model of "how
accessorial fees should work" throws all of that away and replaces it
with how fees should work in the rewrite author's head, which is a
different, unvalidated thing.

The second is dual maintenance. The moment a rewrite starts, the business
does not stop needing changes to the calculator it depends on today.
Every rate change, every new carrier contract, every regulatory
adjustment has to land in the old calculator to keep invoices correct,
and then either land again in the half-finished rewrite or get forgotten
outright, so the rewrite is already behind before it ships. Teams
routinely underestimate this cost because it is invisible during
planning and only shows up as "did the new calculator get the carrier
rate change too?" months in.

The third is the long window with no delivery. A rewrite is an
all-or-nothing bet: nothing the team builds pays off until the whole
replacement is done, tested, and cut over. A regulator, a customer, or a
competitor does not wait for that date. An incremental refactoring
effort, by contrast, ships value continuously, because every safe step is
itself a small, real improvement to the system already in production.

None of this means a bounded rewrite is always wrong. It means a rewrite
earns its cost only when the current design's problems are structural
rather than cosmetic: the platform itself is the limiting factor, the
architecture actively prevents a change the business needs, or the code
is so poorly understood and so sparsely tested that incremental change is
itself the riskier path. When a bounded rewrite is the right call, it
needs a way to ship without a single all-or-nothing cutover day, and that
is what the strangler fig approach provides.

## The strangler fig approach

Martin Fowler borrowed the name from a tree, the strangler fig, that
germinates in the canopy of a host tree and sends roots down to the
ground, gradually enveloping the host until the host can be removed and
the fig stands on its own. The software version replaces a legacy
component behind a stable interface, incrementally, routing more traffic
to the new implementation as confidence grows, with the old
implementation removable only once the new one has proven itself against
the same bar the old one was held to.

Three pieces make this work, and each one exists for a specific reason
rather than being extra ceremony layered onto a simple problem.

**A stable interface.** Every caller depends on `AccessorialFeeCalculator`,
never on a concrete legacy or modern implementation. This is the same
force that justifies dependency inversion anywhere else in this book:
callers are insulated from which concrete type answers them, so the
implementation behind the interface can change without touching a single
call site.

**A routing seam.** Something has to decide, for a given shipment,
whether the legacy implementation or the modern one handles it. In this
chapter's worked example that seam is `StranglerFeeCalculatorRouter`,
which consults a `MigratedShipmentRegistry` to decide per shipment ID. The
seam does not need any more machinery than that: a registry of migrated
identifiers is enough to move one shipment, one carrier, or one customer
at a time, and to move them back by removing an entry if something looks
wrong.

**A shared characterization suite.** The new implementation does not earn
the right to take traffic by looking cleaner. It earns that right by
passing the same cases that pin the old implementation's behavior,
including the cases that encode undocumented business rules nobody wrote
down anywhere else. Passing a different, newly written test suite proves
the new code does what its author intended; passing the old suite proves
it does what the business needs, which is a different and much stronger
claim.

The traffic shift is the part that makes this a strangler fig
specifically, rather than just "build a facade." The old implementation's
share of traffic does not drop to zero in one release. It shrinks, one
shipment, one carrier, or one percentage point at a time, with each
increment observable and reversible if something looks wrong. The host
component, the legacy calculator, keeps doing its job for the traffic
still routed to it right up until the day the last sliver of that traffic
moves, at which point it can be deleted with nobody noticing the
difference in production, because the difference was already proven to
not exist.

## The migration, worked in code

`AccessorialFeeCalculator` is this example's stable interface: one
method, `calculateFees()`, that every concrete calculator implements.
`LegacyAccessorialFeeCalculator` is the component under review. Its fuel
surcharge varies by carrier through a chain of string comparisons, with
one undocumented rate change buried in a comment:

```java
if (carrier.equals("OCEAN")) {
    // bumped from 0.12 to 0.15 during the 2019 contract renegotiation;
    // nobody who signed that contract still works here
    fuelSurcharge = Math.round(manifest.declaredValueCents() * 0.15);
} else if (carrier.equals("AIR")) {
    fuelSurcharge = Math.round(manifest.declaredValueCents() * 0.22);
} else if (carrier.equals("GROUND")) {
    fuelSurcharge = Math.round(manifest.declaredValueCents() * 0.08);
} else {
    fuelSurcharge = Math.round(manifest.declaredValueCents() * 0.10);
}
```

Two more undocumented rules sit further down the same method: a
residential delivery fee that is waived under twenty kilograms for a
reason nobody can trace to a ticket, and an oversize penalty that doubles
specifically for air freight, discovered by an auditor comparing invoices
against a carrier contract rather than written down anywhere this code
points to. Nothing marks either rule as implementing a negotiated term; a
reader has to already know the rule exists to recognize this code as the
thing that enforces it, the kind of undocumented knowledge the decision
factors above warn against discarding.

`AccessorialFeeCharacterizationCases` pins all of this with five shipment
profiles and the exact fee totals `LegacyAccessorialFeeCalculator`
returns today, captured by running the legacy calculator, not derived
from a specification that does not exist. `ModernAccessorialFeeCalculator`
is judged against that same table:

```java
@ParameterizedTest(name = "{0}")
@MethodSource("...AccessorialFeeCharacterizationCases#cases")
void matchesPinnedFees(String caseName, ShipmentManifest manifest, AccessorialFees expectedFees) {
    AccessorialFees actualFees = modern.calculateFees(manifest);

    assertThat(actualFees).isEqualTo(expectedFees);
}
```

`ModernAccessorialFeeCalculator` restructures the same knowledge rather
than discarding it. A switch expression replaces the string-equality
if/else chain for the fuel rate, and named constants replace the inline
magic numbers for the residential and oversize rules, but every rate and
every threshold is unchanged:

```java
private double fuelSurchargeRate(String carrierCode) {
    return switch (carrierCode) {
        case "OCEAN" -> 0.15;
        case "AIR" -> 0.22;
        case "GROUND" -> 0.08;
        default -> DEFAULT_FUEL_SURCHARGE_RATE;
    };
}
```

That substitution illustrates this book's "modern Java retires older
idioms" theme directly: a switch expression names every branch once, as an
expression, and the compiler enforces that every carrier case is
considered, which a chain of `if`/`else if` comparisons cannot promise on
its own. A separate test, `agreesWithLegacyOnAnUncoveredCase`, drives both
calculators through one shipment profile the pinned table does not
cover and asserts their fee breakdowns match exactly, a direct
engine-to-engine comparison independent of either calculator's own pinned
expectations.

`StranglerFeeCalculatorRouter` is the seam, and it stays small on
purpose:

```java
@Override
public AccessorialFees calculateFees(ShipmentManifest manifest) {
    AccessorialFeeCalculator target = registry.isMigrated(manifest.shipmentId()) ? modern : legacy;
    return target.calculateFees(manifest);
}
```

`StranglerFeeCalculatorRouterTest` proves this routing with Mockito mocks
standing in for both calculators, checking the interaction rather than
only the return value: a migrated shipment reaches the modern calculator
and the legacy one is verified `never()` called, an unmigrated shipment
reaches the legacy calculator with the same guarantee in reverse, and
once every shipment has migrated, `verifyNoInteractions(legacy)` confirms
the legacy calculator is not touched at all. `MigratedShipmentRegistry`
itself is a small set of shipment IDs with an `isMigrated()` query and a
`migrate()` mutator, nothing more elaborate than that, by design: the
routing policy belongs in one place, separate from the router that
consults it, so the policy can change without touching the seam.

The demo program plays this out as three releases: a cold start where
every shipment is still priced by the legacy calculator, a release where
one shipment has migrated while the other two have not, and a final
release where every shipment has migrated and the legacy calculator sits
unused. Every total printed across all three releases is identical,
which is the strangler fig's actual promise: the migration is invisible
to the business because the characterization table proved it would be
before `StranglerFeeCalculatorRouter` ever routed a single shipment to
`ModernAccessorialFeeCalculator`.

> **AI sidebar.** Ask an LLM pairing partner to "modernize this legacy fee
> calculator" and a common first answer is a full rewrite: a tidy
> configuration record, maybe a rules engine, built from whatever the
> model infers the fee rules ought to be. What it will not do on its own
> is go looking for the specific undocumented knowledge, a renegotiated
> fuel rate, a weight-based waiver, a carrier-specific penalty an auditor
> once had to dig out of an invoice, sitting inside the branches it is
> about to delete. A generated rewrite is judged by whether it compiles
> and reads cleanly, not by whether it still honors a rule that exists
> nowhere except in the code being replaced. Before accepting a generated
> rewrite of anything that has been in production for years, ask the same
> question this chapter's decision factors ask: what does the current
> code know that is not written down anywhere else, and has anything been
> built to prove the replacement still knows it? A characterization suite
> pulled from the running legacy component, not written from a
> specification, is the only artifact that answers that question with
> evidence instead of confidence.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this
loop. From the repository root, the aggregator picks up
`examples/32-refactor-rewrite` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/32-refactor-rewrite
mvn test
```

Either path runs the same 17 JUnit 5 cases: five characterization cases
against the legacy calculator, the same five plus one direct agreement
check against the modern calculator, three router tests proving the
routing and the cutover, and three tests on the migration registry
itself. Observed locally on Fedora with Temurin 25:

```
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see the migration play out rather than read about it, run the
example's own demo script, which steps through three releases from a
cold legacy-only start to every shipment migrated:

```sh
examples/32-refactor-rewrite/demo.sh
```

```
== Release 1: every shipment still quoted by the legacy calculator ==
SHIP-1001 (OCEAN): $150.00
SHIP-1002 (AIR): $137.00
SHIP-1003 (GROUND): $16.00

== Release 2: SHIP-1002 migrated, the other two still legacy ==
SHIP-1001 (OCEAN): $150.00
SHIP-1002 (AIR): $137.00
SHIP-1003 (GROUND): $16.00
shipments migrated so far: 1 of 3

== Release 3: every shipment migrated, legacy calculator is unused ==
SHIP-1001 (OCEAN): $150.00
SHIP-1002 (AIR): $137.00
SHIP-1003 (GROUND): $16.00
shipments migrated so far: 3 of 3
```

Every total holds steady from the first release to the last. That is the
strangler fig's entire argument made visible at a terminal instead of
asserted in prose: the cutover is invisible because the characterization
table proved it would be before a single shipment was ever routed to the
modern calculator.

## When not to use it

A strangler fig migration is not free, and the decision factors that
justify a bounded rewrite in the first place cut against the seam itself
once the circumstances change.

A component with low risk, low cost, and a codebase the team already
understands does not need a router or a gradual cutover policy at all.
If a bug in the replacement would be caught in code review and fixed in
an afternoon, build the replacement, run it against a characterization
suite once, and cut over directly. The entire apparatus this chapter
builds exists to make a mistake observable and reversible before it
reaches a customer; paying that cost for a change nobody would notice if
it went briefly wrong is solving a problem that was never present.

A component with no meaningful way to segment traffic, no identifier like
a shipment ID or a customer ID to route on, cannot support a gradual,
per-request cutover in any case that matters.
`StranglerFeeCalculatorRouter` works here because every shipment carries
an ID the registry can key on. A system that processes anonymous,
unsegmented work can still build a characterization suite and compare the
two implementations before cutover, but the gradual, traffic-shifted
migration this chapter spends most of its space on collapses to a single
switch: compare until confident, then flip everyone at once. That is a
legitimate simplification, not a failure to apply the pattern correctly.

A small or short-lived component does not justify building a second,
parallel implementation at all. If the component in question is fifty
lines, fully covered by a test suite that already exists, and understood
by everyone who touches it, that is squarely a refactoring candidate, not
a rewrite candidate, and introducing interface indirection purely so two
implementations can coexist is speculative generality: an abstraction
built for a variation that the actual situation does not call for.

Finally, do not reach for a strangler fig when the real blocker is that
nobody has built the safety net yet. If no characterization tests exist
for the legacy component, building the seam before pinning current
behavior only gives a new implementation a routing mechanism with nothing
yet proven for it to agree with. The decision factors at the start of
this chapter, not the seam itself, are the first checkpoint: establish
risk, cost, how much undocumented knowledge is in play, and whether a
safety net exists, and let those answers decide whether this chapter's
machinery is worth building at all.

## For deeper coverage

- Michael Feathers, [*Working Effectively with Legacy Code*](https://www.oreilly.com/library/view/working-effectively-with/0131177052/)
  (Prentice Hall): the definition of legacy code as code without tests and
  the case for characterization tests as the first step before any
  change, the foundation this book's inherited-code part modernizes for
  Java 25.
- Martin Fowler, [*Refactoring: Improving the Design of Existing Code*](https://martinfowler.com/books/refactoring.html),
  2nd ed. (Addison-Wesley): the refactoring catalogue and the discipline
  of small, behavior-preserving steps that this chapter's "usually
  refactor" default rests on.
- Martin Fowler, ["StranglerFigApplication"](https://martinfowler.com/bliki/StranglerFigApplication.html):
  the original description of the strangler fig technique this chapter's
  worked example builds, including the naming history behind the
  metaphor.
- The full reading list behind this book's opinions lives on the
  [bibliography page]({{ '/bibliography/' | relative_url }}); each
  citation there is a pointer to the full book, not a substitute for
  reading it.
