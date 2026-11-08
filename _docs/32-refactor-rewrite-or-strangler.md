---
title: "Refactor, rewrite, or strangler fig"
order: 32
layout: tutorial
sectionid: docs
part: "changing-inherited-code"
description: "Choosing between an in-place refactor, a wholesale rewrite and the strangler fig, on a legacy accessorial fee calculator with an undocumented renegotiated fuel rate, a weight-based fee waiver and a carrier-specific oversize penalty. A strangler router sends each shipment to the legacy or modern calculator behind one AccessorialFeeCalculator interface, and a shared characterization table proves the two agree before cutover."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Weigh the factors that decide refactor versus rewrite: risk, cost, undocumented knowledge in the code, how well it's understood, whether a safety net exists, and the cost of being wrong.
- Explain why "let's just rewrite it" usually loses.
- State the conditions under which a bounded rewrite is the defensible call.
- Describe the strangler fig: a stable interface, a routing seam that picks old or new per request, and traffic shifted as confidence grows.
- Build a characterization table that both the old and the new implementation must pass.
- Judge when a seam and a gradual cutover aren't worth paying for.

{% include excalidraw.html name="32-strangler-fee-router" caption="Figure 32.1: a shipment service calls AccessorialFeeCalculator; StranglerFeeCalculatorRouter sits behind that interface, consults MigratedShipmentRegistry, and sends each shipment to LegacyAccessorialFeeCalculator or ModernAccessorialFeeCalculator." %}

## Why this matters

Every inherited codebase has one component nobody wants to touch. In a shipping system it's often the accessorial fee calculator, which adds a fuel surcharge, a residential fee, a liftgate fee and an oversize penalty to the base freight charge. It has worked for years and finance trusts its numbers. Nobody knows why one carrier's fuel rate is 0.15 instead of 0.12, or why the residential fee disappears under twenty kilograms, and the person who knew left two reorganizations ago.

The question is concrete: keep refactoring it in place, one safe step at a time, or set it aside and build a replacement? Both answers are right some of the time, so the call needs a repeatable set of factors rather than a gut decision under deadline. A team that always refactors sometimes polishes a design that should have been replaced. A team that always rewrites sometimes throws away years of edge-case handling and spends a quarter reimplementing it badly from memory. When a bounded rewrite is right and the current system can't stop serving traffic, the strangler fig is the technique.

## The decision factors

Age and ugliness aren't on the list. Old, unlovely code that's well tested and understood is a fine refactoring candidate; code from last quarter that nobody can explain and nothing tests is a rewrite candidate.

| Factor | The question | What it argues for |
| --- | --- | --- |
| Risk | What breaks if it misbehaves, and who notices first? | Silent damage (six months of undercharged residential deliveries before an audit) favors small, reversible steps: refactoring or an incremental cutover. |
| Cost | What shape is the spending? | Refactoring spreads time out while the system keeps shipping. A rewrite concentrates it, and changes needed meanwhile land twice. |
| Undocumented knowledge | What does the code know that nothing else records? | A renegotiated rate, a fee waiver, an auditor's penalty rule are the business logic. The more there is, the more a rewrite risks dropping it. |
| Understanding | Can the current team explain it line by line? | The less understood, the more a rewrite bets on re-deriving what the organization already paid to learn, and the more a characterization table pays for itself. |
| Safety net | Do tests exist? | With tests, refactor aggressively. Without them, building the net is the first step whichever path you take; a refactor without one can be riskier than a rewrite. |
| Cost of being wrong | What does each failure look like? | A wrong refactor wastes time. A wrong rewrite leaves a half-built replacement and an older calculator nobody dares touch. The asymmetry is why the default is to refactor. |

Undocumented knowledge is the factor teams underweight most. Feathers defines legacy code as code without tests, and the missing tests usually come with missing documentation, so the code's behavior is the only surviving record of decisions the business made years ago. A rewrite that treats that behavior as noise ships something that compiles and reads well and stops honoring a rule nobody mentioned, because nobody remembered it existed outside the code.

## Why rewrite from scratch usually loses

"Let's just rewrite it" loses for three repeatable reasons:

- **Lost embedded knowledge.** A legacy calculator's quirks are rarely bugs; they're fossilized decisions. A fuel rate bumped in a contract renegotiation, a weight floor that waives a fee, a doubled penalty for one carrier's oversized freight were each put there because a contract, an audit or a carrier agreement needed it. A rewrite from a clean model of how fees should work replaces all of that with the author's assumptions.
- **Dual maintenance.** The business keeps needing changes while the rewrite is under way. Every rate change and new contract lands in the old calculator to keep invoices right, then lands again in the rewrite or gets forgotten. It's invisible at planning time and shows up months later as "did the new calculator get that rate change too?"
- **No delivery until the end.** Nothing pays off until the whole replacement is built, tested and cut over, and regulators, customers and competitors don't wait. Incremental refactoring ships an improvement with every step.

A bounded rewrite earns its cost when the problems are structural: the platform is the limit, the architecture blocks a change the business needs, or the code is so poorly understood and tested that incremental change is the riskier path. Even then it needs to ship without one all-or-nothing cutover day.

## The strangler fig approach

Martin Fowler named it after the strangler fig, which germinates in a host tree's canopy and sends roots to the ground until it envelops the host and stands on its own. The **strangler fig** replaces a legacy component behind a stable interface, shifting traffic to the new implementation as confidence grows, and removes the old one only after the new one passes the same bar.

It has three parts:

- **A stable interface.** Every caller depends on `AccessorialFeeCalculator`, never on a concrete implementation, so what sits behind it changes without touching a call site. It's dependency inversion applied to a migration.
- **A routing seam.** `StranglerFeeCalculatorRouter` decides per shipment which implementation answers, by consulting a `MigratedShipmentRegistry`. A registry of migrated IDs is enough to move one shipment, carrier or customer at a time, and to move it back by removing an entry.
- **A shared characterization table.** The new implementation earns traffic by passing the cases that pin the old one, including the undocumented rules. A new suite proves the code does what its author meant; the old table proves it does what the business needs.

The traffic shift is what makes it a strangler fig rather than a facade. The old implementation's share shrinks one shipment, carrier or percentage point at a time, each step observable and reversible. When the last of its traffic moves, the legacy calculator can be deleted and nobody in production notices, because the difference was already shown not to exist.

## The migration, worked in code

`AccessorialFeeCalculator` has one method, `calculateFees()`. In `LegacyAccessorialFeeCalculator` the fuel surcharge varies by carrier through string comparisons, with one undocumented rate change in a comment:

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

Two more undocumented rules sit further down: a residential fee waived under twenty kilograms for a reason no ticket explains, and an oversize penalty that doubles for air freight, found by an auditor comparing invoices with a carrier contract. Nothing marks either as a negotiated term. You have to know the rule exists to recognize the code that enforces it.

`AccessorialFeeCharacterizationCases` pins five shipment profiles with the exact fee totals the legacy calculator returns today, captured by running it, since no specification exists. `ModernAccessorialFeeCalculator` is judged against the same table:

```java
@ParameterizedTest(name = "{0}")
@MethodSource("...AccessorialFeeCharacterizationCases#cases")
void matchesPinnedFees(String caseName, ShipmentManifest manifest, AccessorialFees expectedFees) {
    AccessorialFees actualFees = modern.calculateFees(manifest);

    assertThat(actualFees).isEqualTo(expectedFees);
}
```

The modern calculator restructures the knowledge instead of discarding it. A switch expression replaces the string-equality chain and named constants replace the inline numbers, with every rate and threshold unchanged:

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

The switch expression names each carrier once, as one expression that yields the rate, and the compiler requires it to cover every input, which a `String` switch meets with its `default`. Beyond the pinned table, both calculators also price one shipment profile the table doesn't cover, and their fee breakdowns match exactly.

The router stays small:

```java
@Override
public AccessorialFees calculateFees(ShipmentManifest manifest) {
    AccessorialFeeCalculator target = registry.isMigrated(manifest.shipmentId()) ? modern : legacy;
    return target.calculateFees(manifest);
}
```

With Mockito mocks for both calculators, a migrated shipment reaches only the modern one, an unmigrated shipment reaches only the legacy one, and once every shipment has migrated the legacy calculator sees no interactions at all. `MigratedShipmentRegistry` is a set of shipment IDs with `isMigrated()` and `migrate()`. The routing policy lives there, apart from the router, so the policy can change without touching the seam.

The demo plays this out as three releases: every shipment on the legacy calculator, then one migrated, then all three. Every total is identical across all three, which is the strangler fig's promise: the business can't see the migration, because the characterization table showed the two calculators agree before any shipment reached `ModernAccessorialFeeCalculator`.

{% include excalidraw.html name="32-strangler-cutover-timeline" caption="Figure 32.2: the same three shipments across three releases. One shipment migrates at a time, from legacy to modern, and every printed total holds steady through the cutover, because the characterization suite proved the two calculators agree before any traffic moved." %}

> **🤖 Explore with your agent**
>
> "In `examples/32-refactor-rewrite`, change `ModernAccessorialFeeCalculator` only. Keep every rate and threshold `LegacyAccessorialFeeCalculator` applies, including the OCEAN fuel rate of 0.15, the residential fee waiver under twenty kilograms and the doubled oversize penalty for AIR, and keep it passing every row of `AccessorialFeeCharacterizationCases` unchanged. Don't edit the legacy calculator, the cases or `StranglerFeeCalculatorRouter`, and don't replace the rules with a configuration file or a rules engine."
>
> Check: `mvn -q -f examples/32-refactor-rewrite/pom.xml test -Dtest='ModernAccessorialFeeCalculatorAgreementTest,StranglerFeeCalculatorRouterTest'`.

## Build, run, observe

From the repository root:

```sh
mvn -f examples/pom.xml test
```

Or standalone:

```sh
cd examples/32-refactor-rewrite
mvn test
```

All 17 cases pass: the five pinned profiles against each calculator, the uncovered-profile agreement check, three routing cases and three registry cases. To watch the three releases:

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

Every total holds from the first release to the last.

- **Forget the renegotiated rate.** In a scratch copy, set the OCEAN fuel rate in `ModernAccessorialFeeCalculator` back to 0.12. Two cases fail before any traffic moves: the OCEAN pinned profile and the uncovered-profile agreement check.

## When not to use it

- **Low risk, low cost, well understood.** If a bug in the replacement would be caught in review and fixed in an afternoon, build it, run it against a characterization table once, and cut over directly. The router exists to make a mistake observable and reversible before a customer sees it.
- **No way to segment traffic.** The router works because every shipment carries an ID. Anonymous, unsegmented work can still be compared old against new before cutover, but the gradual shift collapses to one switch: compare until confident, then flip everyone. That's a legitimate simplification.
- **A small component.** Fifty lines, fully tested and understood by everyone who touches it, is a refactoring candidate. An interface added only so two implementations can coexist is speculative generality.
- **No safety net yet.** Without characterization tests, the seam routes to a new implementation with nothing proven for it to agree with. The decision factors are the first checkpoint, not the router.

## For deeper coverage

- Michael Feathers, [*Working Effectively with Legacy Code*](https://www.oreilly.com/library/view/working-effectively-with/0131177052/) (Prentice Hall): legacy code as code without tests, and characterization tests as the first step before any change.
- Martin Fowler, [*Refactoring: Improving the Design of Existing Code*](https://martinfowler.com/books/refactoring.html), 2nd ed. (Addison-Wesley): the refactoring catalogue and the discipline of small, behavior-preserving steps behind the "usually refactor" default.
- Martin Fowler, ["StranglerFigApplication"](https://martinfowler.com/bliki/StranglerFigApplication.html): the original description of the strangler fig technique, including the naming history behind the metaphor.
- The [bibliography page]({{ '/bibliography/' | relative_url }}) lists the full reading list.
