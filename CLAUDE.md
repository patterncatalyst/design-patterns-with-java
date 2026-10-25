# CLAUDE.md — Design Patterns with Java

Ground rules for working in this repo. Read `_plans/r01-build-plan.md` for
the current iteration's status board and acceptance criteria before
starting work; it is the source of truth for scope and is never touched by
content executors.

## Platform and tools

- Local tools only — **no containers** for building or testing examples.
  CI installs the same local toolchain directly on the runner via
  `actions/setup-java` (see `.github/workflows/ci.yml`); that is not
  something a chapter ever tells a reader to do differently.
- Every example builds with **Maven and JDK 25** and is tested with
  **JUnit 5**, **Mockito** for collaborator mocking, and **AssertJ** for
  fluent assertions. A test must verify *behavior* (the pattern does what
  it claims), not just that the build exits 0.
- Pinned toolchain (see `_plans/r01-build-plan.md` for the verified
  versions): JDK 25 (Temurin, via SDKMAN), Maven >= 3.9, JUnit Jupiter
  5.13.4 (via the JUnit BOM), Mockito 5.24.0, AssertJ 3.27.7.
  `scripts/check-prereqs.sh` verifies the floor versions; `scripts/build.sh`
  builds the aggregator; `scripts/verify.sh` runs the test suite.
- Every example under `examples/` is standalone-buildable on its own
  (`cd examples/<NN-pattern> && mvn test`), in addition to being picked up
  by the root `examples/pom.xml` aggregator.

## Java standards

- This book covers exactly **Java 25**, the current LTS. Never write
  "Java 26" or treat a pre-release feature as settled; there is one modern
  standard this book teaches, not a moving target.
- **Code is idiomatic modern Java 25 only.** Do NOT write classic- or
  older-Java code purely for contrast, and do NOT use a "Classic Java vs
  Java 25" codetabs axis. Each sample is a single fenced ```java block in
  the current idiom. The ONE exception is the legacy-code / Feathers part
  (Part 10), where comparing inherited older code with its refactored form
  is the actual subject; there, a "before" snippet is the point.
- Keep the **"modern Java retires older idioms"** through-line as a brief
  prose note where it adds value (a record replaces hand-written
  equals/hashCode and defensive copying; try-with-resources replaces
  try/finally; a switch expression replaces a switch statement), not as a
  second code tab.
- The `codetabs.html` include still exists, for comparing genuinely
  different *modern* Java 25 idioms side by side when a chapter benefits
  (for example a functional-interface form next to an enum form), relabeled
  to the idioms compared, never "Classic Java". Use the bare include
  `{% include codetabs.html langs="..." %}` on its own line, one fenced
  ```java block per label, and never wrap it in raw tags (that stops Liquid
  running it, leaks the literal tag, and makes the `|` a Markdown table).
  Default to a single Java 25 block; reach for a codetab only for a real
  idiom-to-idiom comparison.

## Example code

- Every example is **new**, real-domain Java written for this book, in the
  shipping-domain-code style: real nouns and scenarios (a reservation
  system, a telemetry pipeline, a billing engine), never `Foo`/`Bar`/`Baz`
  placeholder classes.
- The archived C++ book (`cpp-design-patterns`) is **reference-only** for
  process and series conventions. It is **not a code source** — never port
  its C++ examples into Java; every Java example is written fresh against
  its own real-domain scenario.
- References for testing idiom: the official JUnit 5 and Mockito
  documentation, and the *xUnit Test Patterns* bibliography entry for the
  four-phase test and test-double taxonomy.
- **Liquid gotcha:** Jekyll parses `{{ ... }}` as a Liquid tag even inside a
  fenced code block. If a Java snippet contains adjacent braces (for
  example nested generics like `Map<String, List<Integer>>` are fine, but a
  `Map.of(...)` chained into another brace-delimited initializer, or any
  literal `{{` sequence), wrap that whole fenced block in `{% raw %}` /
  `{% endraw %}`, or the site build fails. Verify with a real
  `bundle exec jekyll build`, not just a standalone compile.

## Diagrams

- Diagrams are heavy — most chapters get at least one — and follow a
  **C4-style** standard: context and component framing, not class diagrams
  or stereotype-arrow notation.
- Every diagram ships as a paired `diagrams/<name>.svg` +
  `diagrams/<name>.excalidraw`, generated with `lgtm-diagram-generator`.
- Emphasis/ink boxes are **light fill with dark text**, never a dark-filled
  box with light text.

## Chapter spine

Every chapter (`_docs/NN-*.md`) follows the same shape:

1. **Learning objectives** near the top.
2. **A diagram** (required for every new chapter).
3. **Why this matters**, near the top, before the mechanics.
4. The body.
5. **"For deeper coverage"** at the end, citing the hub's `books.yml` and
   this book's own `/bibliography/`.
6. **"Build, run, observe"** on every hands-on chapter, pointing at the
   matching `examples/` demo.

Every pattern chapter additionally ends with a **"when NOT to use it"**
section and carries exactly one **AI sidebar** box — no more than one per
chapter.

- Prose target: **2,000-2,500 words** of body prose (excluding code and
  diagrams). 2,000 is a hard floor; 2,500 is a target, not a ceiling — more
  is fine if it is value-driven (another diagram, another worked example),
  never padding for its own sake.
- Voice: no em-dash (zero tolerance), no rule-of-three, no contrived AI
  phrasing, no confessional register ("to be honest", "admittedly", "let's
  be real"). Run `lgtm-professional-voice` before calling a chapter done.
- Verification status (what's been run, what's pending) lives **only** in
  `_plans/`, never on a reader-facing page.
- Parts use **Arabic numbering** (Part 0, Part 1, ... Part 12) with a
  descriptive title, never Roman numerals.
- **No intra-book chapter-number cross-references** ("as we saw in ch8",
  "we cover in ch11") in prose or slides. Name the pattern or idiom
  instead; it reads correctly whether the reader jumped in or read
  straight through.
- Companion decks are the **last** deliverable for a release iteration —
  written after every chapter and example for that iteration is done, not
  alongside them.

## Git workflow

- **Never commit directly to `main`.** Every change, including a one-line
  docs fix, goes branch → PR → squash-merge.
- Each release iteration gets its own `release/rNN` branch.

## Skills to use

- `lgtm-jekyll` — site scaffolding and diagram embedding conventions.
- `lgtm-tutorial` — chapter authoring workflow and per-chapter example
  scaffolding.
- `lgtm-presentation` — companion decks (last deliverable).
- `lgtm-diagram-generator` — paired SVG + `.excalidraw` figures.
- `lgtm-professional-voice` — voice pass before a chapter or release ships.
- `lgtm-idiomatic-review` — idiomatic-Java review pass on example code.
- `lgtm-github` — branch/PR/tag/release workflow for this GitHub-hosted repo.
- `lgtm-relay` — plan/execute/validate relay for a release iteration.
- `lgtm-quarkus` — for the Chapter 9 "patterns in the wild" chapter ONLY,
  where patterns are shown inside a real Quarkus service; no other chapter
  needs this skill.
