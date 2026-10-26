---
title: "r09 build plan — Part 9 (plain-Java slice): virtual threads, concurrency/resource patterns, shortcuts, anti-patterns"
order: 9
output: false
---

# r09 — Virtual threads, Resource/concurrency patterns, Shortcut patterns, Anti-patterns

Part 9, plain-Java slice (FOUR chapters). The Quarkus/CDI chapter (ch28) is split
into r10 so the Quarkus 3.33 LTS toolchain is introduced in isolation; r09 stays
plain Java. Source of truth; never touched by content executors. Keep literal
Liquid tags out of this prose.

## Policy (CLAUDE.md)
Idiomatic modern Java 25 ONLY. No classic/older-Java comparison code, no "Classic
Java" codetabs. "modern Java retires older idioms" stays brief prose. Codetab only
for two modern idioms (relabeled); else single Java 25 block.

## Scope (ships)

- **Ch 24 Virtual threads** (`_docs/24-virtual-threads.md`, order 24, part
  `concurrency-patterns-in-the-wild`) + `examples/24-virtual-threads` + diagram.
  NEW Java-only chapter. How virtual threads work (carrier threads, mounting /
  unmounting, cheap blocking), what they are GOOD for (high-concurrency blocking
  I/O: many carrier/API calls, fan-out) AND what they are NOT good for (CPU-bound
  work; pinning on synchronized/native; pooling them is an anti-pattern;
  ThreadLocal cost). `Executors.newVirtualThreadPerTaskExecutor()`, structured
  concurrency where apt. No C++ sibling.
- **Ch 25 Resource and concurrency patterns** (`_docs/25-resource-and-concurrency-patterns.md`,
  order 25, same part) + `examples/25-resource-concurrency` + diagram. Object /
  connection pools (bounded, blocking acquire), command processor, Monitor and
  Active Object reconsidered under virtual threads; `ThreadLocal`; unmodifiable
  wrappers / defensive copying for shared state. Adapts C++ 24-resource-pools.
- **Ch 26 Shortcut patterns** (`_docs/26-shortcut-patterns.md`, order 26, same part)
  + `examples/26-shortcut-patterns` + diagram. Enum dispatch and
  `Map<K, Function>` dispatch tables (the Java analog of C++ function-pointer jump
  tables): when a key selects one short handler and a full class hierarchy is
  overkill. Adapts C++ 25-dispatch-tables.
- **Ch 27 Anti-patterns and pattern misuse** (`_docs/27-anti-patterns.md`, order 27,
  same part) + `examples/27-anti-patterns` + diagram. A pattern applied with no
  force that calls for it; over-engineering; the exhibit is an over-patterned
  shipping design taken apart into the simpler appropriate form (both modern Java —
  this is NOT classic-vs-modern; it is over-engineered-vs-appropriate). Adapts C++
  26-anti-patterns.

## Steps
1-4. Executors (Sonnet), parallel: Ch 24 / Ch 25 / Ch 26 / Ch 27, each chapter +
example + diagram.
5. Orchestrator: add four modules to `examples/pom.xml` `<modules>`; register four
diagrams in `_data/diagrams.yml`; write four `_examples/*.md` cards; integration
build + full `mvn test`.
6. Opus validation → fix findings (grep+clear any stray "deliberately"/ban-tier) →
PR → squash-merge → tag `r09.x` → release.

## Acceptance criteria
- Ch 24/25/26/27 render under Part 9 with the full chapter spine (learning
  objectives, C4 diagram, "Why this matters", body, "when NOT to use it", one AI
  sidebar, "Build, run, observe", "For deeper coverage" publisher links;
  front-matter layout: tutorial + sectionid: docs).
- Each example builds standalone AND via the aggregator; full `mvn test` green on
  JDK 25; tests verify behavior (virtual-thread fan-out completes; a bounded pool
  blocks past capacity and reuses; dispatch routes by key; the anti-pattern's
  simplified form passes the same tests as the over-engineered one).
- ch24 must cover good-for AND not-good-for virtual threads explicitly. Idiomatic
  Java 25 (virtual threads via `Executors.newVirtualThreadPerTaskExecutor`,
  `ReentrantLock`/`Semaphore` where apt, enum/Map dispatch, records). No
  classic-comparison code; no "Classic Java". Diagrams paired SVG+excalidraw, C4,
  light-fill, no UML, registered + embedded.
- 0 ban-tier voice per chapter; no em-dash; NO intra-book chapter-number
  cross-references; no reader-facing status footer. `jekyll build` exit 0.

## Notes
- Orchestrator owns `_data/diagrams.yml`, `_examples/*.md`, `examples/pom.xml`
  `<modules>`, `_plans/`, git. ch28 Quarkus/CDI deferred to r10.
