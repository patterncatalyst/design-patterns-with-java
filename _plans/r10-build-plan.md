---
title: "r10 build plan — Ch 28: Patterns in a real framework (Quarkus and CDI)"
order: 10
output: false
---

# r10 — Patterns in a real framework: Quarkus and CDI

A single chapter (ch28), isolated so the Quarkus toolchain is introduced on its own.
Closes Part 9. Source of truth; never touched by content executors. Keep literal
Liquid tags out of this prose.

## Policy (CLAUDE.md)
Idiomatic modern Java 25. This is the ONE chapter that uses a framework (Quarkus),
to show how a real DI container expresses the patterns the book taught in plain
Java. No "Classic Java" codetabs. The chapter is additive, not a rewrite of earlier
chapters.

## Scope (ships)
- **Ch 28 Patterns in a real framework: Quarkus and CDI**
  (`_docs/28-patterns-in-the-wild-quarkus-cdi.md`, order 28, part
  `concurrency-patterns-in-the-wild`) + `examples/28-quarkus-cdi` + diagram.
  Show how Quarkus/CDI (Arc) provides the patterns as framework features:
  - CDI beans + `@Inject` constructor injection = the Factory/Singleton/DI the
    plain-Java chapters built by hand (`@ApplicationScoped` singletons; the
    container is the composition root).
  - `@Produces` producer methods = Factory.
  - CDI interceptors (`@AroundInvoke` + a binding annotation) = Decorator/Proxy.
  - CDI events (`Event<T>` + `@Observes`) = Observer.
  - `@ConfigMapping` / typed config = externalized Strategy selection.
  Frame it as "the same forces, now answered by the framework" — reference the
  patterns by NAME (not chapter number). Shipping domain.

## Toolchain (verify at build)
- **Quarkus 3.40.1.1 LTS** (the current latest LTS; full Java 25 support). Use the
  Quarkus BOM `io.quarkus.platform:quarkus-bom:3.40.1.1` in the module. Dependencies: `quarkus-arc` (CDI)
  and `quarkus-junit5` (@QuarkusTest). Pure CDI/Arc only — NO Dev Services, NO
  container, NO database/Kafka — so `mvn test` runs the @QuarkusTest suite in-JVM
  on JDK 25 with no Docker. Read the `lgtm-quarkus` skill for conventions.
- The module still parents the aggregator `../pom.xml` for the Java version, and
  imports the Quarkus BOM for the Quarkus deps. It must build BOTH standalone and
  via `mvn -f examples/pom.xml test` without a container.

## Steps
1. Executor (Sonnet): Ch 28 + `examples/28-quarkus-cdi` + diagram. Read `lgtm-quarkus`.
2. Orchestrator: add the module to `examples/pom.xml` `<modules>`; register the
   diagram in `_data/diagrams.yml`; write the `_examples/28-*.md` card; integration
   build + full `mvn test`; confirm CI (ci.yml) still green with the Quarkus module
   (no Docker).
3. Opus validation → fix findings → PR → squash-merge → tag `r10.x` → release.

## Acceptance criteria
- Ch 28 renders under Part 9 with the full chapter spine (learning objectives, C4
  diagram, "Why this matters", body, "when NOT to use it" — i.e. when plain Java /
  no framework is the better call, one AI sidebar, "Build, run, observe", "For
  deeper coverage" publisher links incl. the Quarkus docs; front-matter layout:
  tutorial + sectionid: docs).
- `examples/28-quarkus-cdi` builds standalone AND via the aggregator; `mvn test`
  green on JDK 25 with NO container (verify no Dev Services / Docker needed). The
  @QuarkusTest suite proves each CDI mechanism (injection, producer, interceptor,
  event, config) behaves.
- Quarkus pinned to 3.40.1.1 LTS (exact version recorded). Idiomatic Quarkus/CDI
  (lgtm-quarkus). Diagram paired SVG+excalidraw, C4, light-fill, no UML, registered
  + embedded.
- 0 ban-tier voice; no em-dash; NO intra-book chapter-number cross-references; no
  reader-facing status footer. `jekyll build` exit 0.

## Risks
- Quarkus module slowing or breaking the aggregator `mvn test` in CI. If the
  @QuarkusTest boot is heavy or needs a container, fall back to building the Quarkus
  module in a SEPARATE CI job (and keep it out of the plain aggregator run) — but
  first try the single-aggregator path; it should work for pure Arc.
- Quarkus 3.40.1.1 vs JDK 25: resolved (full support); pin exactly and
  record the version. If an incompatibility surfaces, record the hold + reason.

## Notes
- Orchestrator owns `_data/diagrams.yml`, `_examples/*.md`, `examples/pom.xml`
  `<modules>`, `_plans/`, git.
