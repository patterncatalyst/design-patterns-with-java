---
title: "r12 build plan — Part 11: Putting it together (final project + AI/LLM)"
order: 12
output: false
---

# r12 — Final project + Working with AI and LLM tools

Part 11 (TWO chapters). Completes the chapter set (0-34). Mirrors the C++ Part 11,
idiomatic modern Java 25. Source of truth; never touched by content executors. Keep
literal Liquid tags out of this prose.

## Policy (CLAUDE.md)
Idiomatic modern Java 25 ONLY. No classic/older-Java comparison code, no "Classic
Java" codetabs. USER TERM: "final project", NEVER "capstone".

## Scope (ships)

- **Ch 33 Final project** (`_docs/33-final-project.md`, order 33, part
  `putting-it-together`) + `examples/33-final-project` + diagram(s). Compose several
  patterns the book taught into one coherent shipping system end to end: e.g.
  object creation (Factory/Builder), a shipment lifecycle (State/FSM), undoable
  operations (Command), status events (Observer), a swappable rate (Strategy), all
  wired together. Show the patterns cooperating; reference them by NAME. A runtime
  sequence diagram is apt. Larger integrated example; thorough JUnit tests proving
  the composed system behaves (a shipment flows through its lifecycle, an undo
  restores state, an observer sees an event, the rate strategy is swappable).
- **Ch 34 Working with AI and LLM tools** (`_docs/34-working-with-ai-and-llm-tools.md`,
  order 34, same part) + `examples/34-ai-verification-gate` + diagram. Recognizing
  WHAT to build and WHY; a pattern name is the precise vocabulary you give an
  assistant; and reviewing AI-generated pattern code (does it assert behavior, is
  the pattern justified by a real force, is it idiomatic Java 25). A worked
  "verification gate": behavior tests + an idiomatic check deciding whether an
  AI-drafted implementation is accepted or sent back. The example is a small
  harness demonstrating that gate over a drafted pattern implementation.

## Steps
1-2. Executors (Sonnet), parallel: Ch 33 / Ch 34, each chapter + example +
diagram. Read the C++ siblings (`_docs/31-final-project.md`,
`_docs/32-ai-and-llm-tools.md`) for pedagogy; translate to Java (do NOT port C++
code). Reference earlier patterns by NAME, never chapter number.
3. Orchestrator: add two modules to `examples/pom.xml` `<modules>`; register the
diagrams in `_data/diagrams.yml`; write `_examples/*.md` cards; integration build +
full `mvn test`.
4. Opus validation → fix findings → PR → squash-merge → tag `r12.x` → release.

## Acceptance criteria
- Ch 33/34 render under Part 11 with the full chapter spine (learning objectives,
  C4 diagram, "Why this matters", body, "when NOT to use it" where apt, one AI
  sidebar, "Build, run, observe", "For deeper coverage" publisher links;
  front-matter layout: tutorial + sectionid: docs). Ch 33 uses "final project",
  never "capstone".
- Each example builds standalone AND via the aggregator; full `mvn test` green on
  JDK 25; tests verify behavior (the composed final-project system works end to
  end; the AI verification gate accepts a good draft and rejects a bad one).
- Idiomatic Java 25; no classic-comparison code; no "Classic Java". Diagrams paired
  SVG+excalidraw, C4, light-fill, no UML, registered + embedded.
- 0 ban-tier voice per chapter; no em-dash; NO intra-book chapter-number
  cross-references; no reader-facing status footer. `jekyll build` exit 0.

## Notes
- Orchestrator owns `_data/diagrams.yml`, `_examples/*.md`, `examples/pom.xml`
  `<modules>`, `_plans/`, git. After r12, chapters 0-34 are COMPLETE; remaining:
  appendices (r13-14), backlog/diagram-audit/bibliography (r15), decks (r16).
