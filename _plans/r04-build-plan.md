---
title: "r04 build plan — Part 4: Selecting and varying behavior (remainder)"
order: 4
output: false
---

# r04 — Template Method, State/FSM, Command

Completes Part 4 (Ch 8 Strategy shipped in r01). Mirrors the C++ Part 4, in
idiomatic modern Java 25. Source of truth for r04 scope; never touched by content
executors. Keep literal Liquid tags out of this prose.

## Policy reminder (from CLAUDE.md)
Code is idiomatic modern Java 25 ONLY. No classic/older-Java comparison code, no
"Classic Java" codetabs. Keep the "modern Java retires older idioms" point as
brief prose. A codetab is allowed ONLY to compare two genuinely-different modern
idioms (relabeled), otherwise use a single Java 25 block.

## Scope (ships)

- **Ch 9 Template Method** (`_docs/09-template-method.md`, order 9, part
  `selecting-varying-behavior`) + `examples/09-template-method` + diagram. A fixed
  skeleton with varying steps: abstract-method hooks, and a functional form where
  the varying steps are passed as lambdas. (NVI is C++-specific; do not port it.)
- **Ch 10 State and finite state machines** (`_docs/10-state-and-finite-state-machines.md`,
  order 10, same part) + `examples/10-state-machines` + diagram. An enum state
  machine and a sealed-interface state with a pattern-matching switch; refuse
  illegal transitions; include a probabilistic/stochastic FSM. Shipment lifecycle.
- **Ch 11 Command, undo, and command queues** (`_docs/11-command-undo-and-queues.md`,
  order 11, same part) + `examples/11-command` + diagram. Commands as objects /
  functional interfaces, an undo stack (`Deque`), and a command queue.

## Steps

1. Executor (Sonnet): Ch 9 + example + diagram.
2. Executor (Sonnet): Ch 10 + example + diagram.
3. Executor (Sonnet): Ch 11 + example + diagram.
   (Parallel; different files.)
4. Orchestrator: add the three modules to `examples/pom.xml` `<modules>`; register
   the three diagrams in `_data/diagrams.yml`; write three `_examples/*.md` cards;
   integration build + full `mvn test`.
5. Opus validation → fix findings → PR → squash-merge → tag `r04.x` → release.

## Acceptance criteria

- Ch 9/10/11 render under Part 4 with the full pattern-chapter spine (learning
  objectives, C4 diagram, "Why this matters", body, "when NOT to use it", one AI
  sidebar, "Build, run, observe", "For deeper coverage" with publisher links).
- Each example builds standalone AND via the aggregator; `mvn -f examples/pom.xml
  test` green on JDK 25; tests verify behavior (incl. refused illegal state
  transitions; undo restores prior state; the stochastic FSM's distribution).
- Idiomatic Java 25 (sealed interfaces + pattern matching, enums, records,
  functional interfaces/lambdas, `Deque`). No classic-comparison code; no
  "Classic Java". Diagrams paired SVG+excalidraw, C4, light-fill, no UML,
  registered + embedded with Figure captions.
- 0 ban-tier voice per chapter; no em-dash; NO intra-book chapter-number
  cross-references; no reader-facing status footer. `jekyll build` exit 0.

## Notes
- Orchestrator owns `_data/diagrams.yml`, `_examples/*.md`, `examples/pom.xml`
  `<modules>`, `_plans/`, git. Content executors touch none of these.
