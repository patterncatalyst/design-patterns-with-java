---
title: "r16 build plan — Appendix F: Programming principles, reviewed in code"
order: 16
output: false
---

# r16 — Appendix F (parity with the C++ book)

Post-completion addition (user request 2026-10-26): a sixth appendix mirroring the
C++ book's Appendix F, Java-flavored. A code-first review of the wider
programming-principle vocabulary beyond SOLID. Inspired by the public
iluwatar/programming-principles catalogue (a fork of Lars Kappert's) but NOT copied:
original Java prose and original shipping-domain Java 25 code. Source of truth; never
touched by content executors.

## Scope (ships)
- **Appendix F — "Programming principles, reviewed in code"**
  (`_docs/40-appendix-f-programming-principles.md`, order 40, `appendix: "F"`,
  `part: appendices`). Full catalogue, same six families as the C++ Appendix F
  (`/home/rsedor/Dev/cpp-design-patterns/_docs/38-appendix-f-programming-principles.md`
  is the template — mirror structure, translate to Java):
  1. Substitutability & responsibility (SOLID recap, cross-link the SOLID chapters by name, no duplication).
  2. Simplicity & scope (KISS, YAGNI, Do The Simplest Thing, Avoid Premature Optimization, Curly's Law).
  3. Structure & change (DRY, Separation of Concerns, Encapsulate What Changes, Hide Implementation Details, Composition Over Inheritance, Orthogonality).
  4. Coupling & cohesion (Minimise Coupling, Maximise Cohesion, Law of Demeter, Command-Query Separation, Inversion of Control).
  5. Interoperability & resilience (Robustness / Postel's Law).
  6. Team & process laws (Boy-Scout Rule, Code For The Maintainer, Murphy, Brooks, Linus) — prose only, no code.
  Each code-bearing principle: a short why, a before (violates) / after (honors)
  SHIPPING-DOMAIN Java 25 snippet, and a one-line tension note. The 5 process laws
  are prose.
- One C4 diagram `40-principle-families` (SVG + .excalidraw), light-fill/no-UML,
  embedded as Figure F.1.
- Update `_parts/12-appendices.md` blurb: five -> six appendices.
- Add the newly cited books to `bibliography.html` with publisher links: The
  Pragmatic Programmer (pragprog.com), Clean Code (pearson), The Mythical Man-Month
  (pearson). (GoF + Martin already present.)

## Steps
1. Executor (Sonnet): author Appendix F + diagram + bibliography additions + part blurb.
2. Orchestrator: register the diagram in `_data/diagrams.yml`; integration jekyll build (mvn unchanged, no new modules).
3. Opus validation → fix findings (clear any "deliberately"/ban-tier) → PR → squash-merge → tag `r16.x` → release.
4. Hub re-sync for the 3 new books (Pragmatic Programmer, Clean Code, Mythical Man-Month) with design-patterns-with-java in referenced_by.

## Acceptance criteria
- Renders as "Appendix F" under Part 12 (6 appendices A-F). All 26 principles present across the six families; code-bearing ones have before/after shipping-domain Java 25 + a tension note; the 5 laws are prose. SOLID recap cross-links the SOLID chapters by NAME (no chapter numbers).
- One paired C4 diagram, light-fill, no UML, registered + embedded with a Figure caption.
- "For deeper coverage" links the cited books to PUBLISHER pages; those books are in /bibliography/.
- Idiomatic Java 25 in the "after" snippets (records where apt). Original, not copied from the catalogue or from the C++ Appendix F.
- 0 ban-tier voice; no em-dash; no intra-book chapter-number cross-references. `jekyll build` exit 0; `mvn test` unchanged (737, no new modules).

## Notes
- Orchestrator owns `_data/diagrams.yml`, `_plans/`, git, hub re-sync.
