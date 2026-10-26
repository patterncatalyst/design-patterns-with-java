---
title: "r11 build plan — Part 10: Changing inherited code (modernized Feathers)"
order: 11
output: false
---

# r11 — Seams, Dependency-breaking, Fowler-vs-Feathers, Refactor/rewrite

Part 10 (FOUR chapters). The Java take on Michael Feathers' legacy-code work.
Source of truth; never touched by content executors. Keep literal Liquid tags out
of this prose.

## Policy (CLAUDE.md) — THIS PART IS THE EXCEPTION
Code is still idiomatic where it is the "after", BUT this is the ONE part where
showing "before" legacy / older / messy inherited code IS the subject. Before/after
contrast is expected and correct here (the whole point of Feathers). Do NOT force
everything to pristine Java 25; the inherited code should look like real inherited
code (untested, tangled dependencies, hard-wired collaborators), and the chapters
show how to pin it with tests and break its dependencies. Still: no gratuitous
em-dash, no confessional voice, no chapter-number cross-references; run
lgtm-professional-voice.

## Scope (ships)

- **Ch 29 Seams and the test safety net** (`_docs/29-seams-and-the-test-safety-net.md`,
  order 29, part `changing-inherited-code`) + `examples/29-seams` + diagram. A seam
  is a place to change behavior without editing in place. Java seams: object seam
  (inject/override a collaborator), the extract-interface seam, subclass-and-override;
  characterization tests to pin current behavior before changing it. Mockito for the
  test double at a seam.
- **Ch 30 Dependency-breaking techniques for Java**
  (`_docs/30-dependency-breaking-techniques.md`, order 30, same part) +
  `examples/30-dependency-breaking` + diagram. Feathers' techniques adapted to Java:
  Extract Interface, Adapt Parameter, Parameterize Constructor, Extract and Override
  Factory Method, Subclass and Override Method, wrap/extend. Take hard-wired
  inherited code and make it testable.
- **Ch 31 Characterization tests; Fowler vs Feathers**
  (`_docs/31-characterization-and-fowler-vs-feathers.md`, order 31, same part) +
  `examples/31-characterization` + diagram. Writing characterization tests to lock in
  behavior you do not yet understand; then the contrast: Fowler's Refactoring (clean
  code, safety net already present, make good code better) vs Feathers (dangerous
  code, build the net first) and how they combine (Feathers to get tests green, then
  Fowler to improve).
- **Ch 32 Refactor, rewrite, or strangler fig**
  (`_docs/32-refactor-rewrite-or-strangler.md`, order 32, same part) +
  `examples/32-refactor-rewrite` + diagram. Deciding among in-place refactor,
  rewrite, and the strangler-fig incremental replacement; a worked incremental
  replacement that routes through an interface while the old and new implementations
  coexist.

## Steps
1-4. Executors (Sonnet), parallel: Ch 29 / Ch 30 / Ch 31 / Ch 32, each chapter +
example + diagram. Each reads its C++ sibling (`_docs/27-seams.md`,
`28-dependency-breaking.md`, `29-fowler-and-feathers.md`, `30-refactor-or-rewrite.md`)
for the pedagogy and translates to Java (do NOT port C++ code).
5. Orchestrator: add four modules to `examples/pom.xml` `<modules>`; register four
diagrams in `_data/diagrams.yml`; write four `_examples/*.md` cards; integration
build + full `mvn test`.
6. Opus validation → fix findings (grep+clear any stray "deliberately"/ban-tier) →
PR → squash-merge → tag `r11.x` → release.

## Acceptance criteria
- Ch 29/30/31/32 render under Part 10 with the full chapter spine (learning
  objectives, C4 diagram, "Why this matters", body, "when NOT to use it"/"when each
  applies", one AI sidebar, "Build, run, observe", "For deeper coverage" with
  PUBLISHER links incl. Feathers' Working Effectively with Legacy Code and Fowler's
  Refactoring; front-matter layout: tutorial + sectionid: docs).
- Each example builds standalone AND via the aggregator; full `mvn test` green on
  JDK 25; tests verify behavior (a characterization test pins the legacy behavior; a
  seam lets a test double replace a collaborator; a dependency-breaking technique
  makes previously-untestable code testable; the strangler routes to old vs new).
- Before/after legacy code is allowed and expected (the exception part); the "after"
  is clean, the "before" is realistic inherited code. Diagrams paired SVG+excalidraw,
  C4, light-fill, no UML, registered + embedded.
- 0 ban-tier voice per chapter; no em-dash; NO intra-book chapter-number
  cross-references; no reader-facing status footer. `jekyll build` exit 0.

## Notes
- Orchestrator owns `_data/diagrams.yml`, `_examples/*.md`, `examples/pom.xml`
  `<modules>`, `_plans/`, git.
