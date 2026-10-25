---
title: "r03 build plan — Part 3: The Java substrate"
order: 3
output: false
---

# r03 — The Java substrate

Part 3. Replaces the C++ book's "C++ substrate" (RAII / value semantics) with the
Java equivalents, plus the testing chapter. Mirrors the C++ r03 pedagogy,
translated to Java 25. Source of truth for r03 scope; never touched by content
executors. (Keep literal Liquid tags out of this prose: a stray raw tag fatally
breaks the site build, since plan files are still parsed.)

## Scope (ships)

- **Ch 5 Resource management, Java's RAII** (`_docs/05-resource-management.md`,
  order 5, part `java-substrate`) + `examples/05-resource-management` + diagram.
  try-with-resources, `AutoCloseable`, the Execute-Around Method idiom.
- **Ch 6 Encapsulation and API design** (`_docs/06-encapsulation-and-api-design.md`,
  order 6, part `java-substrate`) + `examples/06-encapsulation` + diagram. Public
  interface + package-private implementation, static factory methods, records,
  immutability and defensive copying.
- **Ch 7 Testing with JUnit 5, Mockito, and AssertJ**
  (`_docs/07-testing-with-junit.md`, order 7, part `java-substrate`) +
  `examples/07-testing-with-junit` + diagram. The test loop every example runs:
  JUnit 5 lifecycle/assertions/parameterized tests, Mockito doubles, AssertJ
  fluent assertions, shown on a real shipping component.

## Steps

1. Executor (Sonnet): Ch 5 + example + diagram.
2. Executor (Sonnet): Ch 6 + example + diagram.
3. Executor (Sonnet): Ch 7 + example + diagram.
   (Parallel; different files.)
4. Orchestrator integration: add the three modules to `examples/pom.xml`
   `<modules>`; register the three diagrams in `_data/diagrams.yml`; write three
   `_examples/*.md` gallery cards; integration build + full `mvn test`.
5. Opus validation → fix findings → PR → squash-merge → tag `r03.x` → release.

## Acceptance criteria

- Ch 5/6/7 render under Part 3 with the full chapter spine (learning objectives,
  C4 diagram, "Why this matters", body, "when NOT to use it" where apt, one AI
  sidebar, "Build, run, observe", "For deeper coverage" with publisher links).
- Each example builds standalone AND via the aggregator; `mvn -f examples/pom.xml
  test` green on JDK 25; tests verify behavior.
- Idiomatic Java 25 (records, try-with-resources, static factories, Mockito and
  AssertJ used idiomatically). Codetabs axis Classic Java vs Java 25, bare
  include (never raw-wrapped). Diagrams paired SVG+excalidraw, C4, light-fill, no
  UML, registered + embedded with Figure captions.
- 0 ban-tier voice per chapter; no em-dash; NO intra-book chapter-number
  cross-references (grep multiline); no reader-facing status footer.
- `jekyll build` exits 0.

## Notes

- Orchestrator owns `_data/diagrams.yml`, `_examples/*.md`, `examples/pom.xml`
  `<modules>`, `_plans/`, and git. Content executors touch none of these.
