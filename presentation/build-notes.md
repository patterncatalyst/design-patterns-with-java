# Building the companion decks

This directory holds the companion `.pptx` decks for *Design Patterns with
Java*, built with the `lgtm-presentation` skill's pptxgenjs engine. Three
deck builders are planned here (separate executors write these, not this
scaffold):

- `deck-101.js` — 101-level overview deck
- `deck-201.js` — 201-level deep-dive deck
- `deck-testing-legacy-ai.js` — testing / legacy-code / AI-assisted-patterns deck

## What's already in place

- `deck-helpers.js` — bundled helper library (COLOR/FONT/W/H + slide
  helpers: `addContentTitle`, `addBullets`, `addTwoColBullets`,
  `addStatusTable`, `addCodeSlide`, `addDiagramSlide`, `addPerfCallout`,
  `addCaption`, `addSectionDivider`, `addFooter`, `addNotes`). Do not
  rewrite it; extend only if a genuinely new slide shape is needed.
- `assets/` — cover panel, section divider panel, and logos used by
  `deck-helpers.js` (`ASSETS` constant, defaults to `./assets`).
- `png/` — PNG renders of every chapter diagram in `../diagrams/*.svg`,
  rendered at `output_width=1600` via cairosvg. `addDiagramSlide` reads
  these via the `PNG` constant (defaults to `./png`).
- `package.json` — local `pptxgenjs` dependency (installed into
  `node_modules/`, gitignored).

## Prerequisites

- Node.js with `pptxgenjs`. It is installed locally in this directory
  (`npm install` already run — see `node_modules/`), so `require("pptxgenjs")`
  resolves with no extra setup.
  - Fallback, only if `node_modules/pptxgenjs` is ever missing and npm is
    offline: `export NODE_PATH=$(npm root -g)` before running `node`, so a
    global install resolves instead.
- Python 3 with `cairosvg` (already used once to render `png/`; re-run only
  if a diagram SVG changes — see "Regenerating diagram PNGs" below).
- LibreOffice (`soffice`) and poppler (`pdftoppm`) for QA rendering.

## Building a deck

From this directory:

```bash
node deck-101.js
node deck-201.js
node deck-testing-legacy-ai.js
```

(If `node_modules/pptxgenjs` is missing, run
`export NODE_PATH=$(npm root -g)` first.)

Each builder writes its own `.pptx` per its `OUT` constant, named
`<deck>-rNN.x.pptx` per house versioning (new section → bump the major,
post-review fix → bump `.x`).

## QA workflow (run for every build, before calling a deck done)

```bash
mkdir -p qa
soffice --headless --convert-to pdf --outdir qa <out>.pptx
pdftoppm -jpeg -r 110 -f N -l N qa/<out>.pdf qa/pg
```

Eyeball the rendered JPEGs: table first columns don't wrap, code-slide
captions sit below the dark box, diagrams are centered, and the Red Hat
logo appears bottom-right on every content slide. `qa/` is gitignored —
it is scratch QA output, not a deliverable.

## Regenerating diagram PNGs

If a chapter diagram SVG changes under `../diagrams/`, re-render its PNG:

```bash
python3 -c "
import cairosvg
cairosvg.svg2png(url='../diagrams/<name>.svg', write_to='png/<name>.png', output_width=1600)
"
```

or re-render all 42 at once with the same `output_width=1600` convention
used to build this directory's `png/`.
