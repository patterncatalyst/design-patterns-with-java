---
title: "r08 build plan — Part 8: Idiomatic Java 25 expressions"
order: 8
output: false
---

# r08 — Records/Sealed/Pattern-matching, Functional Java, Idiom retirement

Part 8. This is the book's SIGNATURE part and is NEW content (it does NOT mirror
the C++ Part 8, which was CRTP / type-erasure / expression-templates — all dropped
as C++-only). Source of truth; never touched by content executors. Keep literal
Liquid tags out of this prose.

## Policy (CLAUDE.md)
Idiomatic modern Java 25 ONLY. No classic/older-Java comparison code and no
"Classic Java" codetabs. The "modern Java retires older idioms" through-line is the
SUBJECT of this part, but it is carried in PROSE plus modern code; show the modern
idiom as the worked code and describe the retired idiom in prose (at most a brief
inline cautionary snippet where it truly aids understanding, like the
double-checked-locking note in the singleton chapter). Do NOT write a maintained
classic-vs-modern example or a classic-vs-modern codetab. A codetab may compare two
MODERN idioms (relabeled).

## Scope (ships)

- **Ch 21 Records, sealed interfaces, and pattern matching**
  (`_docs/21-records-sealed-pattern-matching.md`, order 21, part
  `idiomatic-java-expressions`) + `examples/21-records-sealed-patterns` + diagram.
  Data-oriented programming: records as data carriers, sealed interfaces as closed
  hierarchies, pattern matching for `switch` and record deconstruction patterns.
  Show how these express several GoF patterns (State, Visitor, Interpreter) more
  directly, referenced by pattern NAME.
- **Ch 22 Enums, functional interfaces, streams, and Optional as patterns**
  (`_docs/22-enums-functional-streams.md`, order 22, same part) +
  `examples/22-functional-java` + diagram. Enums carrying behavior (Strategy /
  State / Singleton), functional interfaces + lambdas + method references (Strategy
  / Command / Template Method), streams as declarative pipelines, Optional instead
  of null.
- **Ch 23 Where Java 25 retires classic idioms** (the through-line CAPSTONE)
  (`_docs/23-java-25-retires-classic-idioms.md`, order 23, same part) +
  `examples/23-idiom-retirement` + diagram. Pull the theme together: an enum (or
  holder) singleton retires double-checked locking; a record retires hand-written
  getters/equals/hashCode and defensive copying; a sealed interface + exhaustive
  switch retires the double-dispatch Visitor and the instanceof chain; Optional
  retires the Null Object and the null check. The example implements the MODERN
  forms with tests; the retired idioms are described in prose. Reference every
  pattern/idiom by NAME, never by chapter number.

## Steps
1-3. Executors (Sonnet), parallel: Ch 21 / Ch 22 / Ch 23, each chapter + example +
diagram. These are NEW chapters — do NOT look for a C++ sibling to mirror; build
from this plan and match the shipped Java chapters' conventions.
4. Orchestrator: add three modules to `examples/pom.xml` `<modules>`; register three
diagrams in `_data/diagrams.yml`; write three `_examples/*.md` cards; integration
build + full `mvn test`.
5. Opus validation → fix findings (grep+clear any stray "deliberately"/ban-tier) →
PR → squash-merge → tag `r08.x` → release.

## Acceptance criteria
- Ch 21/22/23 render under Part 8 with the full chapter spine (learning objectives,
  C4 diagram, "Why this matters", body, "when NOT to use it" where apt, one AI
  sidebar, "Build, run, observe", "For deeper coverage" publisher links;
  front-matter layout: tutorial + sectionid: docs).
- Each example builds standalone AND via the aggregator; full `mvn test` green on
  JDK 25; tests verify behavior.
- Idiomatic Java 25 throughout. No classic-vs-modern maintained example or codetab;
  no "Classic Java". ch23 argues the retirement in prose + modern code. Diagrams
  paired SVG+excalidraw, C4, light-fill, no UML, registered + embedded.
- 0 ban-tier voice per chapter; no em-dash; NO intra-book chapter-number
  cross-references; no reader-facing status footer. `jekyll build` exit 0.

## Notes
- Orchestrator owns `_data/diagrams.yml`, `_examples/*.md`, `examples/pom.xml`
  `<modules>`, `_plans/`, git.
