---
title: "Naming the pattern"
order: 2
layout: tutorial
sectionid: docs
part: "recognizing-design"
description: "Why a pattern's name is a compression of forces a team, a future reader, and an AI pairing partner can all act on, and why a loose name misleads every one of them the same way."
duration: "15 min"
---

## Learning objectives

By the end of this chapter you can:

- Explain what a pattern name compresses: a cluster of forces plus a chosen structure, not a block of code.
- Use a pattern name so a reviewer can reason about the decision instead of re-reading the syntax.
- Prompt an AI assistant with a pattern name and the forces behind it, not just the keyword.
- Spot a name on generated code that doesn't match the forces in that code.
- Treat naming precision as a review skill.

## Why this matters

A review comment reads: "this should be an Observer, not a field the billing service polls every few seconds." It lands in one sentence because everyone on the thread unpacks the same paragraph: a component must react to a change it doesn't own, polling pays for that over and over, and the thing that changes should announce it. Nobody re-derives the reasoning.

The same compression costs time once the name stops being accurate. Everyone who hears it assumes the paragraph behind it is still true. A name is a tool with a failure mode, and the failure plays out the same way between two people as between a person and an AI assistant.

{% include excalidraw.html name="02-pattern-vocabulary" caption="Figure 2.1: A pattern name sits between three readers, an engineer, a teammate, and an AI assistant, each one supposed to read the same forces off the same word." %}

## A name is shorthand for a paragraph

Say "Strategy" and a paragraph arrives: something varies, the variant is chosen independently of the caller, and several variants coexist without the caller changing when one is added. Say "an interface with three implementations and a factory" and you've described a shape without saying why it exists.

The diff already shows the shape. What it can't show is whether the forces that justify the shape are present, today, in this code. The name carries that judgment. "This is a Strategy" is a design claim when the forces back it and decoration when they don't.

The check: can you unpack the name into the forces it names, for this code, without a textbook definition?

| Teammate asks | Answer | The name is |
| --- | --- | --- |
| "Why a Strategy and not a `switch` expression?" | "Separate teams own the regional carriers and each changes on its own schedule." | attached to its forces |
| same | "That's what you use when a value varies." | loose |

## Reasoning where the decision lives

Shared vocabulary lets a review start at the decision instead of at the lines. Take a pull request that adds a connection pool to a billing service's datastore. Without the vocabulary, a reviewer checks that the lock is taken, the connection is returned on every exit path and the build passes. With it, the reviewer asks first whether the service needs what a pool guarantees, that no two callers hold the same live connection, or whether one shared connection behind a lock covers today's traffic.

The pull request description sets the kind of review it gets:

| Description | Review it invites |
| --- | --- |
| "Implements Observer for cache invalidation." | Line by line, correctness only. |
| "The cache must invalidate the instant the record changes, without readers polling. Observer over a scheduled refresh, because staleness must be milliseconds, not a polling interval." | A check of the stated forces and the rejected alternative. |

Design meetings work the same way. A meeting that re-derives Factory each time the word comes up has less time for the question in front of it: does this construction vary in which concrete type gets built, or is involved enough that inlining it would hurt? Or is `new PremiumAccount(...)` already the answer, and a Factory a layer around nothing? Shared vocabulary doesn't make that call. It clears the table for it.

## The same word, read by an AI assistant

Everything above holds when the reader is an AI assistant. "Add an Observer so the billing dashboard updates the instant an invoice status changes, without polling" hands over forces, not a shape: something changes, a dependent must react without asking, and polling is the cost to avoid. With those forces the choice between a listener interface registered on the subject and a `java.util.function.Consumer` passed to a registration method can be made on the merits.

"Make this reactive, add a pattern" hands over a word-shaped hole with no forces in it. That's the same failure as an engineer reaching for a shape the moment it comes to mind, now producing a full implementation in the time it took to type the request. The fix is the same for both: state the forces before the name.

Generated code is still code someone answers for. A generated pull request that claims "implements Observer for cache invalidation" gets the same two questions as a human one: which forces does it resolve, and which plainer alternative was rejected, and why.

> **🤖 Explore with your agent**
>
> "The billing dashboard must show an invoice's new status the instant it changes, without polling. The invoice service owns the status; the dashboard only reacts. Add change notification in Java: compare a listener interface registered on the invoice service with a `java.util.function.Consumer` passed to a registration method, pick one, and say which force decided it. Then name the plainer alternative you rejected and why."
>
> Check: the reply names a fact about the invoice service, not the textbook definition of Observer.

## A wrong name costs more than no name

A missing name is an inconvenience. A wrong one sends every reader who trusts it to the wrong forces.

- **A loop called Visitor.** A validation method that walks a tree with a type check, documented as a Visitor, sends the next maintainer looking for double-dispatch machinery that was never built.
- **A constructor called Builder.** A four-argument constructor documented as "implements the Builder pattern" leads anyone asked to "add an optional field to the Builder", person or model, toward a full builder class, when a fifth parameter or a small parameter object is the smaller change.
- **A vague name.** "Uses a design pattern for flexibility" names nothing. It passes review without making a claim anyone can check, and it gives a prompt nothing to constrain the answer.

An assistant reads a misnamed comment or Javadoc as ground truth about the design, just as it reads a prompt. A name is accurate only if it unpacks into the forces present. Correct it or remove it.

## Naming is the hinge of the method

The arc runs: recognize the forces, name a candidate pattern and a candidate non-pattern, choose the idiomatic Java expression, test the behavior, and know when not to use it. Naming sits in the middle:

- forces recognized but never named stay in one engineer's head;
- a name without recognized forces is decoration;
- a named decision is a word a teammate can challenge, a later reader can trust and an assistant can be prompted with and reviewed against.

A name used loosely drifts a little further from its forces each time, until "Strategy" means "has an interface" and the compression is gone for the whole team. Each pattern chapter ties its name to a shipping-domain example so the name stays attached to the forces that earned it.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480/9780321700698) (Addison-Wesley): the catalogue whose names this chapter treats as shared vocabulary, each entry stated as a recurring problem and the core of a solution, not a label for a shape.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); every citation in this book is a pointer to the source, not a substitute for reading it.
