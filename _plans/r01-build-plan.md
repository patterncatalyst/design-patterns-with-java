---
title: "r01 build plan — scaffold + Strategy vertical slice"
order: 1
output: false
---

# r01 — Design Patterns with Java scaffold

First iteration of the SECOND book in the series. Mirrors the completed
`cpp-design-patterns` book, translated to Java 25 + Maven + JUnit 5/Mockito/
AssertJ. Source of truth for r01 scope and acceptance criteria. **Never touched
by content executors.**

## Verified toolchain (Fedora, 2026-10-25)

| Item | Version |
| --- | --- |
| JDK | Java 25.0.2-tem (LTS), via SDKMAN (`25.0.2-tem` current) |
| Maven | 3.9.11 |
| JUnit Jupiter | 5.13.4 (pin exact in the aggregator `dependencyManagement`) |
| Mockito | latest stable 5.x (verify on Maven Central, pin exact) |
| AssertJ Core | 3.27.7 |
| Quarkus (in-the-wild track only, NOT r01) | 3.33 LTS (full Java 25 support) |

Codetabs axis: **`Classic Java | Java 25`** (two labels, in that order). This
axis carries the book's through-line: where Java 25 retires a classic idiom.

## r01 scope (ships)

1. Public repo `patterncatalyst/design-patterns-with-java` (created, cloned),
   branch `release/r01`.
2. Jekyll site skeleton (mirror cpp-design-patterns, re-pointed to Java/Maven):
   `_config.yml` (collections docs/parts/examples/plans; `baseurl:
   /design-patterns-with-java`; Maven-aware excludes), `_layouts/` (tutorial,
   part_index, example, plan, default), `_includes/` incl. `codetabs.html`
   (`Classic Java|Java 25`) and `excalidraw.html`, 13 `_parts` stubs (00-12),
   landing/index pages (index, chapters, examples, diagrams, bibliography),
   `CLAUDE.md`, `.gitattributes` (Linguist → Java).
3. Maven harness: aggregator `examples/pom.xml` (JDK 25, pinned JUnit 5 /
   Mockito / AssertJ, surefire), `scripts/` (check-prereqs / build / verify),
   per-module layout so each example builds standalone AND from the root.
4. CI: `.github/workflows/pages.yml` (Jekyll build + deploy) and `ci.yml`
   (setup-java 25 + `mvn -q test`).
5. Content: ch0 Prerequisites, ch1 Patterns as decisions under forces, ch8
   Strategy + `examples/08-strategy` (shipping-rate domain, three Java
   expressions: interface+impl / functional interface+lambda / enum strategy;
   JUnit 5 + AssertJ suite proving identical quotes across expressions).
6. Diagrams: ch0 build/test loop, ch1 the arc, ch8 strategy forces — paired SVG
   + `.excalidraw`, C4-style, light-fill emphasis boxes, no UML.
7. Examples-gallery card for 08-strategy.
8. HOLD for explicit user go: hub card PR, squash-merge + tag `r01.x` + release.

## Acceptance criteria

1. Public repo, Pages enabled, main reached only via squash-merged PR; `r01.x`
   tag + notes-only release (at ship time).
2. Clean checkout via the prereqs page alone: `examples/08-strategy` builds and
   `mvn test` passes on JDK 25, both standalone and from the aggregator root.
3. `ci.yml` + `pages.yml` green; `jekyll build` exits 0; four collections
   resolve; `baseurl: /design-patterns-with-java`.
4. Live site renders ch0/1/8; codetabs switch `Classic Java`/`Java 25` and
   persist; ch8 has "Build, run, observe", "when NOT to use Strategy", one AI
   sidebar; no verification footer on any reader-facing page.
5. ch1 + ch8 are 2,000-2,500 prose words; `lgtm-professional-voice` scan = 0
   ban-tier; `lgtm-idiomatic-review` clean on the example (idiomatic Java 25,
   no reinvented JDK features).
6. Paired `diagrams/*.svg` + `.excalidraw` for ch0/1/8; light-fill/dark-text
   emphasis boxes; no UML.
7. NO intra-book chapter-number cross-references (prose or slides); name the
   pattern/idiom instead.

## Build cadence (mirror the cpp relay)

Executor A (Sonnet) scaffolds site + Maven + CI + CLAUDE.md, commits. Then
executors B + C (Sonnet, parallel) author content (B = ch8 + example + diagram;
C = ch0 + ch1 + diagrams); they do NOT touch `_data/diagrams.yml`, the
`_examples` gallery cards, `_plans/`, or git. Orchestrator registers diagrams in
`_data/diagrams.yml`, writes gallery cards, commits, runs the integration build,
then Opus validation → fix findings → PR → squash-merge → tag → release.

## Risks

- Quarkus 3.33 LTS vs JDK 25: resolved (full support); not exercised until the
  Quarkus chapter, pinned then.
- iluwatar name collision: mitigated by the distinct repo/site name chosen here.
