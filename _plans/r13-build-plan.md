---
title: "r13 build plan — the five appendices (A-E)"
order: 13
output: false
---

# r13 — Appendices A-E

All five appendices in one iteration (avoids appendix-letter gaps). Reference docs:
LIGHTER spine than chapters — no AI sidebar, no 2,000-word floor, and NO new
`examples/` Maven modules. Inline code snippets only, and every snippet is
SHIPPING-DOMAIN (shipments/carriers/rates/tracking/invoices), NEVER contrived
(no MockTurtle, no UI/Foo examples). Source of truth; never touched by content
executors. Keep literal Liquid tags out of this prose.

## Appendix wiring (confirmed)
Part 12 `appendices` exists; `_layouts/tutorial.html` + `part_index.html` render
"Appendix X" from the `appendix:` front-matter. Each appendix doc: `_docs/NN-...`,
front-matter `layout: tutorial`, `sectionid: docs`, `part: appendices`,
`appendix: "X"`, `order:` (A=35, B=36, C=37, D=38, E=39), `description`, `duration`.

## Scope (ships)
- **Appendix A — Build and toolchain reference** (`_docs/35-appendix-a-build-and-toolchain.md`,
  order 35, appendix "A"). The pinned toolchain (SDKMAN JDK 25.0.2-tem, Maven 3.9.11,
  JUnit 5.13.4 / Mockito 5.24.0 / AssertJ 3.27.7), the Maven aggregator + per-module
  layout, how `mvn test` runs JUnit, standalone vs aggregator build, `scripts/`,
  IntelliJ/VS Code, and the Quarkus track (Quarkus 3.40.1.1 LTS + JBang, `sdk install
  jbang`, the jbang run-path).
- **Appendix B — JUnit 5 reference** (`_docs/36-appendix-b-junit5.md`, order 36,
  appendix "B"). The GoogleTest-appendix analog for Java: `@Test`, lifecycle
  (`@BeforeEach`/`@AfterEach`/`@BeforeAll`), assertions, `assertThrows`,
  `@ParameterizedTest` (`@ValueSource`/`@EnumSource`/`@MethodSource`/`@CsvSource`),
  `@Nested`, `@DisplayName`, `@TestProfile`-style tagging, dynamic tests. Shipping
  snippets.
- **Appendix C — Mockito reference** (`_docs/37-appendix-c-mockito.md`, order 37,
  appendix "C"). The gMock analog: `mock`/`spy`, `when/thenReturn`, `doThrow`,
  `verify`/`times`/`never`, argument matchers, `ArgumentCaptor`, `@Mock` +
  `MockitoExtension`, strict stubbing, mock-vs-fake guidance. Shipping collaborators
  (e.g. a CarrierGateway/Clock mock), NEVER MockTurtle.
- **Appendix D — xUnit test patterns** (`_docs/38-appendix-d-xunit-patterns.md`,
  order 38, appendix "D"). Meszaros: four-phase test (setup/exercise/verify/teardown),
  the test-double taxonomy (dummy/stub/spy/mock/fake), fixture strategies
  (fresh/shared/transient), test smells, mapped to JUnit 5 / Mockito / AssertJ and
  the book's own examples. Shipping-domain recasts only.
- **Appendix E — Pattern-to-force quick reference**
  (`_docs/39-appendix-e-pattern-to-force-reference.md`, order 39, appendix "E"). A
  completeness ledger: every GoF pattern (23), the 5 SOLID principles, and the Java
  idioms the book treats as first-class, each mapped to the force that justifies it
  and linked to its chapter via `{{ '/docs/NN-slug/' | relative_url }}`. Mirror the
  C++ Appendix E (`/home/rsedor/Dev/cpp-design-patterns/_docs/37-appendix-e-pattern-to-force-reference.md`)
  but with Java idioms and the Java chapter slugs.

## Steps
1-5. Executors (Sonnet), parallel: Appendix A / B / C / D / E, each one doc + (if it
helps) a diagram. A, B, C, D are reference docs (likely no diagram needed; a small
C4 figure is OK if it genuinely helps). E is a table (no diagram). Each reads its
C++ sibling appendix for shape and the relevant Java chapters for accuracy.
6. Orchestrator: register any diagrams in `_data/diagrams.yml`; integration build.
(No new example modules, so `examples/pom.xml` is unchanged; still run `mvn test`
once to confirm nothing broke, and `jekyll build`.)
7. Opus validation → fix findings → PR → squash-merge → tag `r13.x` → release.

## Acceptance criteria
- Appendices A-E render as "Appendix A".."Appendix E" (layout reads `appendix:`),
  listed under the Appendices part, in order.
- Reference-doc spine: each has a clear intro and "For deeper coverage" where apt;
  no AI sidebar required; no 2k floor; NO new runnable example modules.
- Every code snippet in B/C/D is SHIPPING-DOMAIN (grep for turtle/foo/bar → none).
- Appendix E lists all 23 GoF + 5 SOLID + the Java idioms, each with a force and a
  working chapter link (relative_url, NOT raw-wrapped).
- Publisher links for any cited book (JUnit/Mockito/AssertJ docs are fine as
  official-docs links; Meszaros xUnit → publisher). `jekyll build` exit 0; full
  `mvn test` still 737/737 (unchanged — no new modules).
- 0 ban-tier voice; no em-dash; NO intra-book chapter-number cross-references in
  prose (Appendix E's chapter LINKS are navigation, by pattern/idiom name + a URL,
  which is the allowed exception — like the C++ Appendix E).

## Notes
- Orchestrator owns `_data/diagrams.yml`, `_plans/`, `_parts/`, git. Reference
  appendices add no example modules.
