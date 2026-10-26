---
title: "r15 build plan — three companion decks (final deliverable)"
order: 15
output: false
---

# r15 — Companion decks (LAST)

The final deliverable: three Red Hat-branded 16:9 .pptx decks via lgtm-presentation
(pptxgenjs). Mined from the finished chapters. Source of truth; never touched by
content executors. Keep literal Liquid tags out of this prose.

## The three decks
1. **101 — Patterns and SOLID** (`deck-101.js` → `design-patterns-with-java-101-patterns-and-solid.pptx`): the GoF patterns + SOLID as decisions under forces, for a broad audience. ~45-55 slides.
2. **201 — Idiomatic Java 25** (`deck-201.js` → `design-patterns-with-java-201-idiomatic-java.pptx`): how the patterns are expressed idiomatically in Java 25 (records/sealed/pattern matching, enums/functional/streams/Optional, virtual threads, the idiom-retirement theme), + Quarkus/CDI patterns-in-the-wild. ~45-55 slides.
3. **Testing, Legacy Code & AI** (`deck-testing-legacy-ai.js` → `design-patterns-with-java-testing-legacy-ai.pptx`): the testing stack (JUnit 5/Mockito/AssertJ), the Feathers legacy track (seams/dependency-breaking/characterization/strangler), and working with AI/LLM tools. NO "101/201" moniker (topical title). ~40-50 slides.

## House conventions (lgtm-presentation + series feedback) — NON-NEGOTIABLE
- Red Hat brand (Display/Text/Mono, accent #EE0000); logo on every content slide; cover + dividers.
- Reuse the site's C4 diagrams as PNGs (cairosvg svg2png ~1600px) in `presentation/png/`; no UML.
- Speaker notes on EVERY slide (notesCount == slide count); notes are the spoken script.
- **Slides AND speaker notes must NEVER name the book's chapters** ("as covered in Chapter 8", "the chapter on X") — convey ideas directly, reference patterns/idioms by NAME (feedback-chapter-cross-references extended to decks).
- No em-dash, 0 ban-tier voice (lgtm-professional-voice) in all notes/slide text.
- Shipping-domain examples, consistent with the book.
- Code is idiomatic Java 25 only (no classic-vs-modern on slides), consistent with the book.

## Steps
1. Setup (executor): scaffold `presentation/` — copy `deck-helpers.js` + `assets/` from the lgtm-presentation skill; `package.json` + `npm install pptxgenjs` (or rely on global NODE_PATH); generate `png/` from every `diagrams/*.svg` (cairosvg, output_width 1600); `.gitignore` (node_modules/, qa/, *.pdf, package-lock.json); `build-notes.md`. Confirm `node` + pptxgenjs resolve.
2. Three deck builders (executors, parallel): write each deck's `.js` against `deck-helpers.js`, mine content from the finished chapters, embed the relevant diagram PNGs, speaker notes 1:1, then `node deck-xxx.js` to emit the .pptx. Each QA-renders its own pptx to PDF via soffice and eyeballs a few pages.
3. Opus QA (brand/consistency/voice/notes-count==slides/no-chapter-names/soffice render) across all three.
4. Ship: PR -> squash-merge -> tag `r15.x` -> notes-only release. After this the book is COMPLETE end-to-end.

## Acceptance criteria
- Three .pptx files build (node exits 0) and open (soffice converts to PDF).
- Each deck: notes on every slide (notesCount == slide count); the Red Hat logo on every content slide; cover + section dividers; reused site diagrams as PNG where apt; no UML.
- NO chapter names/numbers on any slide or in any speaker note. No em-dash; 0 ban-tier voice.
- The third deck has a topical title, no "101/201" moniker.
- `presentation/` excluded from the Jekyll build (already in _config); jekyll build still exit 0.

## Notes
- Orchestrator owns git and the release. `presentation/node_modules` + `qa/` gitignored.
