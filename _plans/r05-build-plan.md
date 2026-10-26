---
title: "r05 build plan — Part 5: Creating objects under control"
order: 5
output: false
---

# r05 — Factory Method/Abstract Factory, Builder/Prototype, Singleton & DI

Part 5. Mirrors the C++ Part 5, in idiomatic modern Java 25. Source of truth for
r05 scope; never touched by content executors. Keep literal Liquid tags out of
this prose.

## Policy reminder (CLAUDE.md)
Idiomatic modern Java 25 ONLY. No classic/older-Java comparison code, no "Classic
Java" codetabs. Keep the "modern Java retires older idioms" point as brief prose.
A codetab is allowed only to compare two genuinely-different modern idioms
(relabeled); otherwise a single Java 25 block.

## Scope (ships)

- **Ch 12 Factory Method and Abstract Factory**
  (`_docs/12-factory-method-and-abstract-factory.md`, order 12, part
  `creating-objects-under-control`) + `examples/12-factories` + diagram. Static
  factory methods, `Supplier`-based factories, and an Abstract Factory as a family
  of related products behind interfaces.
- **Ch 13 Builder and Prototype** (`_docs/13-builder-and-prototype.md`, order 13,
  same part) + `examples/13-builder-prototype` + diagram. Bloch's Builder for many
  optional parameters; records with wither/copy methods; Prototype via copying a
  configured exemplar.
- **Ch 14 Singleton and the DI alternative**
  (`_docs/14-singleton-and-the-di-alternative.md`, order 14, same part) +
  `examples/14-singleton` + diagram. **This is the first idiom-retirement anchor.**
  Show the modern forms: the **enum singleton** (recommended) and the
  **initialization-on-demand holder** idiom (lazy, thread-safe by class-load
  semantics). Discuss double-checked locking with `volatile` as the historically
  tricky approach these retire, in PROSE / at most a short cautionary snippet, not
  a classic-vs-modern comparison codetab. Make the real point: prefer dependency
  injection (pass the collaborator) over a global singleton; show the DI
  alternative.

## Steps

1. Executor (Sonnet): Ch 12 + example + diagram.
2. Executor (Sonnet): Ch 13 + example + diagram.
3. Executor (Sonnet): Ch 14 + example + diagram.
   (Parallel; different files.)
4. Orchestrator: add the three modules to `examples/pom.xml` `<modules>`; register
   the three diagrams in `_data/diagrams.yml`; write three `_examples/*.md` cards;
   integration build + full `mvn test`.
5. Opus validation → fix findings → PR → squash-merge → tag `r05.x` → release.

## Acceptance criteria

- Ch 12/13/14 render under Part 5 with the full pattern-chapter spine (learning
  objectives, C4 diagram, "Why this matters", body, "when NOT to use it", one AI
  sidebar, "Build, run, observe", "For deeper coverage" with publisher links;
  front-matter carries layout: tutorial + sectionid: docs).
- Each example builds standalone AND via the aggregator; full `mvn test` green on
  JDK 25; tests verify behavior (a factory returns the right family; a builder
  rejects an invalid combination and builds a valid object; a prototype copy is
  independent; the enum/holder singleton yields one instance; the DI alternative
  is substitutable, e.g. via a Mockito double).
- Idiomatic Java 25 (records, static factories, `Supplier`, enum singleton, holder
  idiom). No classic-comparison code; no "Classic Java". Diagrams paired
  SVG+excalidraw, C4, light-fill, no UML, registered + embedded with captions.
- 0 ban-tier voice per chapter; no em-dash; NO intra-book chapter-number
  cross-references; no reader-facing status footer. `jekyll build` exit 0.

## Notes
- Orchestrator owns `_data/diagrams.yml`, `_examples/*.md`, `examples/pom.xml`
  `<modules>`, `_plans/`, git. Content executors touch none of these.
