---
title: "r07 build plan — Part 7: Coordinating and traversing"
order: 7
output: false
---

# r07 — Observer/Mediator, Chain of Responsibility/Iterator, Visitor/Memento/Interpreter

Part 7. Mirrors the C++ Part 7, idiomatic modern Java 25. Source of truth; never
touched by content executors. Keep literal Liquid tags out of this prose.

## Policy (CLAUDE.md)
Idiomatic modern Java 25 ONLY. No classic/older-Java comparison code, no "Classic
Java" codetabs. "Modern Java retires older idioms" stays brief prose. Codetab only
for two modern idioms (relabeled); else single Java 25 block.

## Scope (ships)

- **Ch 18 Observer and Mediator** (`_docs/18-observer-and-mediator.md`, order 18,
  part `coordinating-traversing`) + `examples/18-observer-mediator` + diagram.
  Observer (subscribers react to a change without the subject knowing who they are;
  a listener list, and `java.util.concurrent.Flow` as the modern reactive option),
  Mediator (many objects' direct references collapse into a coordinating hub).
- **Ch 19 Chain of Responsibility and Iterator**
  (`_docs/19-chain-of-responsibility-and-iterator.md`, order 19, same part) +
  `examples/19-chain-iterator` + diagram. Chain (a request passes along handlers
  until one takes it; an object chain and a functional composition), Iterator
  (`Iterable`/`Iterator`, and Streams as the idiomatic traversal).
- **Ch 20 Visitor, Memento, Interpreter**
  (`_docs/20-visitor-memento-interpreter.md`, order 20, same part) +
  `examples/20-visitor-memento-interpreter` + diagram. Visitor (a new operation
  across a fixed set of types; **sealed interface + exhaustive pattern-matching
  switch is the Java 25 idiom** — mention classic double-dispatch only in prose),
  Memento (capture and restore state without exposing internals; records as
  immutable snapshots), Interpreter (a small grammar as sealed types that evaluate
  themselves).

## Steps
1-3. Executors (Sonnet), parallel: Ch 18 / Ch 19 / Ch 20, each chapter + example +
diagram.
4. Orchestrator: add three modules to `examples/pom.xml` `<modules>`; register three
diagrams in `_data/diagrams.yml`; write three `_examples/*.md` cards; integration
build + full `mvn test`.
5. Opus validation → fix findings (incl. grep+clear any stray "deliberately"/ban-tier)
→ PR → squash-merge → tag `r07.x` → release.

## Acceptance criteria
- Ch 18/19/20 render under Part 7 with the full pattern-chapter spine (learning
  objectives, C4 diagram, "Why this matters", body, "when NOT to use it", one AI
  sidebar, "Build, run, observe", "For deeper coverage" publisher links;
  front-matter has layout: tutorial + sectionid: docs).
- Each example builds standalone AND via the aggregator; full `mvn test` green on
  JDK 25; tests verify behavior (observer notifies subscribers; mediator decouples;
  chain stops at the first handler that takes it; iterator/stream traverses;
  visitor-via-switch handles every type exhaustively; memento restores prior state;
  interpreter evaluates a small expression).
- Idiomatic Java 25 (sealed interfaces + pattern matching, records, functional
  interfaces, Streams, `Flow`). No classic-comparison code; no "Classic Java".
  Diagrams paired SVG+excalidraw, C4, light-fill, no UML, registered + embedded.
- 0 ban-tier voice per chapter; no em-dash; NO intra-book chapter-number
  cross-references; no reader-facing status footer. `jekyll build` exit 0.

## Notes
- Orchestrator owns `_data/diagrams.yml`, `_examples/*.md`, `examples/pom.xml`
  `<modules>`, `_plans/`, git.
