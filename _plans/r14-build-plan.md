---
title: "r14 build plan — cleanup: bibliography reconciliation, diagram audit, voice sweep"
order: 14
output: false
---

# r14 — Bibliography reconciliation + diagram-coverage audit + hub re-sync

A quality/cleanup iteration (no new chapters). Source of truth; never touched by
content executors. Keep literal Liquid tags out of this prose.

## Workstreams

### 1. Bibliography reconciliation (executor A)
Scan every chapter's "For deeper coverage" for BOOK citations (not doc links like
junit.org / javadoc.io / assertj.github.io / docs.oracle.com / quarkus.io — those
are docs, keep them but they are not bibliography books). Ensure every cited BOOK is
listed in `bibliography.html` with a PUBLISHER link (never Amazon/Google). Current
bibliography has: GoF, Martin Agile SD, Bloch Effective Java, Feathers WELC, Fowler
Refactoring, Meszaros xUnit. Known MISSING (add them): **POSA2** (Pattern-Oriented
Software Architecture Vol. 2, Wiley — cited in the resource/concurrency chapter) and
**Java Concurrency in Practice** (Goetz et al., Addison-Wesley/jcip.net — cited in
the resource/concurrency chapter). Verify there are no other missing books. Keep the
table well-formed; `jekyll build` green.

### 2. Diagram-coverage audit (executor B)
Audit all 35 chapters for where a SECOND diagram would materially aid understanding
(most chapters currently have one; ch33 has two). Produce a RANKED report, then add
up to ~6 of the highest-value second diagrams (paired SVG+excalidraw via
lgtm-diagram-generator, C4-style, light-fill #f1f5f9, no UML), embed each in its
chapter as "Figure N.2", and report the names + chapters. Do NOT touch
`_data/diagrams.yml` (orchestrator registers). Candidates to weigh: State/FSM
transitions detail, Builder assemble-then-validate, Decorator stacking order, Visitor
sealed+switch dispatch, virtual-thread mount/unmount timeline, strangler cutover
over time, Observer fan-out. Only add where it genuinely helps; quality over count.

### 3. Full-book voice/idiomatic sweep (orchestrator)
Run `lgtm-professional-voice` scan over all `_docs/*.md` and fix any ban-tier
(expected near-zero since each shipped at 0). Spot-check `lgtm-idiomatic-review`.

## Steps
1. Executor A (bibliography) + Executor B (diagram audit) in parallel.
2. Orchestrator: register new diagrams in `_data/diagrams.yml`; full-book voice scan +
   fixes; integration `jekyll build` + `mvn test` (unchanged 737 unless a diagram
   executor touched code, which it should not).
3. Opus validation → fix → PR → squash-merge → tag `r14.x` → release.
4. **Hub re-sync** (after the Java bibliography.html is updated): update the hub
   `patterncatalyst-workshops-tutorials-list` `_data/books.yml` so every Java
   bibliography book is present with `design-patterns-with-java` in referenced_by
   (hand edit, scoped). Separate hub PR.

## Acceptance criteria
- Every BOOK cited in any chapter's For-deeper-coverage is in `bibliography.html`
  with a publisher link; POSA2 and Java Concurrency in Practice added.
- New second diagrams are paired SVG+excalidraw, C4/light-fill/no-UML, registered,
  embedded as Figure N.2 in the right chapters; `jekyll build` exit 0.
- 0 ban-tier voice book-wide; no em-dash; no new example modules (mvn test 737/737).
- Hub `_data/books.yml` reflects all Java bibliography books (shipped as its own PR).

## Notes
- Orchestrator owns `_data/diagrams.yml`, `_plans/`, git, and the hub edits.
