---
title: "r06 build plan — Part 6: Composing and adapting structure"
order: 6
output: false
---

# r06 — Adapter/Facade/Bridge, Decorator/Proxy, Composite/Flyweight/Null Object

Part 6. Mirrors the C++ Part 6, idiomatic modern Java 25. Source of truth; never
touched by content executors. Keep literal Liquid tags out of this prose.

## Policy (CLAUDE.md)
Idiomatic modern Java 25 ONLY. No classic/older-Java comparison code, no "Classic
Java" codetabs. Keep "modern Java retires older idioms" as brief prose. Codetab
only to compare two modern idioms (relabeled); else single Java 25 block.

## Scope (ships)

- **Ch 15 Adapter, Facade, Bridge** (`_docs/15-adapter-facade-bridge.md`, order 15,
  part `composing-adapting-structure`) + `examples/15-adapter-facade-bridge` +
  diagram. Adapter (right behavior behind the wrong interface), Facade (one small
  surface over a subsystem), Bridge (an abstraction and its implementation vary
  independently). Do NOT include PImpl (folded into the encapsulation chapter).
- **Ch 16 Decorator and Proxy** (`_docs/16-decorator-and-proxy.md`, order 16, same
  part) + `examples/16-decorator-proxy` + diagram. Decorator (add behavior at
  runtime without subclassing; functional decoration too), Proxy (a stand-in for
  lazy creation, caching, or access control; show `java.lang.reflect.Proxy` as the
  dynamic-proxy option).
- **Ch 17 Composite, Flyweight, Null Object** (`_docs/17-composite-flyweight-null-object.md`,
  order 17, same part) + `examples/17-composite-flyweight-null` + diagram.
  Composite (treat a leaf and a tree uniformly; sealed interface fits), Flyweight
  (share immutable intrinsic state via interning/cache), Null Object (a do-nothing
  implementation; discuss `Optional` as the modern alternative that often removes
  the need).

## Steps
1-3. Executors (Sonnet), parallel: Ch 15 / Ch 16 / Ch 17, each chapter + example +
diagram.
4. Orchestrator: add three modules to `examples/pom.xml` `<modules>`; register three
diagrams in `_data/diagrams.yml`; write three `_examples/*.md` cards; integration
build + full `mvn test`.
5. Opus validation → fix findings → PR → squash-merge → tag `r06.x` → release.

## Acceptance criteria
- Ch 15/16/17 render under Part 6 with the full pattern-chapter spine (learning
  objectives, C4 diagram, "Why this matters", body, "when NOT to use it", one AI
  sidebar, "Build, run, observe", "For deeper coverage" publisher links;
  front-matter has layout: tutorial + sectionid: docs).
- Each example builds standalone AND via the aggregator; full `mvn test` green on
  JDK 25; tests verify behavior (adapter maps correctly; decorator composes;
  dynamic proxy intercepts; composite aggregates a tree; flyweight shares
  instances; null object is a safe no-op).
- Idiomatic Java 25 (interfaces, records, sealed interfaces + pattern matching,
  functional decoration, `java.lang.reflect.Proxy`, interning cache, Optional). No
  classic-comparison code; no "Classic Java". Diagrams paired SVG+excalidraw, C4,
  light-fill, no UML, registered + embedded with captions.
- 0 ban-tier voice per chapter; no em-dash; NO intra-book chapter-number
  cross-references; no reader-facing status footer. `jekyll build` exit 0.

## Notes
- Orchestrator owns `_data/diagrams.yml`, `_examples/*.md`, `examples/pom.xml`
  `<modules>`, `_plans/`, git.
