---
layout: example
order: 34
title: "Working with AI and LLM tools: a verification gate"
description: A behavior-test gate that accepts a correct AI-drafted strategy and rejects a plausible-but-wrong one.
github_path: examples/34-ai-verification-gate
card_eyebrow: "Chapter 34 · Working with AI/LLM tools"
card_title: "A verification gate for AI output"
card_blurb: "A return-eligibility Strategy with two drafts: a correct one and a plausible-but-wrong one (an off-by-one on the day-30 boundary). The gate, a parameterized behavior-test suite, accepts the correct draft and rejects the wrong one, turning a silent misclassification into a caught $40 overpayment."
---

AI tools emit fluent pattern code without judgment, so the engineer keeps
responsibility through a verification gate. This module shows a return-eligibility
`Strategy` with a correct drafted implementation and a plausible-but-wrong one (an
off-by-one `<=` on the day-30 boundary). The gate, a parameterized behavior-test
suite over the boundary table, accepts the correct draft and rejects the wrong one,
turning a misclassification that looks right into a caught $40 overpayment.

The chapter covers naming the forces before prompting, a pattern name as precise
vocabulary for the assistant, and reviewing AI output for justified, idiomatic,
behavior-correct patterns.

## Build, run, observe

```sh
cd examples/34-ai-verification-gate
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
