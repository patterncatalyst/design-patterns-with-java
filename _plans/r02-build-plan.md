---
title: "r02 build plan — Part 1 (Naming) + Part 2 (SOLID)"
order: 2
output: false
---

# r02 — Naming the pattern + SOLID

Completes Parts 1 and 2. Mirrors the C++ book's r02, translated to Java 25.
Source of truth for r02 scope. Never touched by content executors.

## Scope (ships)

- **Ch 2 Naming the pattern** (`_docs/02-naming-the-pattern.md`, order 2, part
  `recognizing-design`): a pattern name as shared vocabulary for teammates,
  future-you, and an AI assistant. No runnable example (vocabulary chapter).
  One diagram, one AI sidebar.
- **Ch 3 SOLID I: SRP / OCP / LSP** (`_docs/03-solid-first-principles.md`, order
  3, part `principles`) + `examples/03-solid-first-principles` (Java module) +
  diagram. Shipping/billing domain.
- **Ch 4 SOLID II: ISP / DIP + DI without a framework**
  (`_docs/04-solid-dependency-inversion.md`, order 4, part `principles`) +
  `examples/04-dependency-inversion` (Java module) + diagram. Constructor DI in
  plain Java; name CDI as the framework alternative (built later, do NOT pull
  Quarkus here).

## Steps

1. Executor D (Sonnet): Ch 3 + `examples/03-solid-first-principles` + diagram.
2. Executor E (Sonnet): Ch 4 + `examples/04-dependency-inversion` + diagram.
3. Executor F (Sonnet): Ch 2 + diagram (no example).
   (D, E, F run in parallel; different files.)
4. Orchestrator integration: add the two modules to `examples/pom.xml`
   `<modules>`; register the three diagrams in `_data/diagrams.yml`; write
   `_examples/03-*.md` and `_examples/04-*.md` gallery cards; integration
   `jekyll build` + `mvn -f examples/pom.xml test`.
5. Opus validation → fix findings → PR → squash-merge → tag `r02.x` → release.

## Acceptance criteria

- Ch 2/3/4 render under their parts; Ch 3/4 carry the full pattern-chapter
  spine (learning objectives, C4 diagram, "Why this matters", body, "when NOT
  to use it" where apt, exactly one AI sidebar, "Build, run, observe", "For
  deeper coverage" with publisher links); Ch 2 has one AI sidebar.
- `examples/03-*` and `examples/04-*` build standalone AND from the aggregator;
  `mvn -f examples/pom.xml test` is green on JDK 25; tests verify behavior.
- Idiomatic Java 25 (records, sealed where apt, interfaces, constructor DI, no
  reinvented JDK, no raw types). Codetabs `Classic Java|Java 25` (bare include,
  never wrapped in Liquid raw tags). Diagrams paired SVG+excalidraw, C4, light-fill,
  no UML, registered + embedded with Figure captions.
- `lgtm-professional-voice` = 0 ban-tier per chapter; no em-dash; NO intra-book
  chapter-number cross-references; no reader-facing status footer.
- `jekyll build` exits 0.

## Notes

- Aggregator `mvn test` already runs every module; the ci.yml guard on
  `examples/08-strategy` stays satisfied, so no CI change is needed.
- Orchestrator owns `_data/diagrams.yml`, `_examples/*.md`, `examples/pom.xml`
  `<modules>`, `_plans/`, and git. Content executors touch none of these.
