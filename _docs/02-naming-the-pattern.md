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

- Explain what a pattern name compresses: a specific cluster of forces plus
  a chosen structure, not a block of code to memorize.
- Use a pattern name the way a team uses any shared vocabulary term, so a
  reviewer can reason about the decision instead of re-reading the syntax.
- Prompt an AI assistant with a pattern name precisely enough that the
  request constrains the forces behind it, not just a familiar keyword.
- Recognize when a name an assistant has attached to generated code does
  not match the forces actually present in that code.
- Explain why naming precision is a review skill, not just a writing habit.

## Why this matters

A reviewer leaves a comment on a pull request: "this should be an Observer,
not a field the billing service polls every few seconds." The comment is
one sentence, and it lands because everyone reading the thread already
knows what that sentence is standing in for: a component that needs to
react to a change it does not own, a cost that polling pays over and over
for no benefit, a design where the thing that changes should announce the
change instead of being interrogated for it. Nobody re-derives that
reasoning in the thread. The name does the work the sentence would
otherwise have to spell out in full.

That compression is the entire point of a pattern name, and it is worth
examining on its own, separate from the forces-first habit the previous
chapter built, because the same compression that saves a team real time can
also cost that team real time the moment the name stops being accurate. A
name that no longer matches the forces behind it does not just fail to
help; it actively misleads, because everyone who hears it assumes the
paragraph behind the word is still true. This chapter treats a pattern name
as a tool with a failure mode, and shows how that failure mode plays out
twice: once between people, and once between a person and an AI assistant
reading or producing the same word.

{% include excalidraw.html name="02-pattern-vocabulary" caption="Figure 2.1: A pattern name sits between three readers, an engineer, a teammate, and an AI assistant, each one supposed to read the same forces off the same word." %}

## A name is shorthand for a paragraph

A force is a requirement that competes with
another requirement in the same piece of code, and a pattern is the name a
team gives to a recurring, specific way of resolving a cluster of those
forces. Naming the pattern is the step where that definition becomes
useful in conversation, not just in code. Say "Strategy" to an engineer who
already carries the vocabulary this book builds and an entire paragraph
arrives intact: something in this code varies, the variation is selected
independently of the caller, and more than one way of varying it needs to
coexist without the caller changing when a new one is added. Say "an
interface with three implementations and a factory" instead, and you have
described a shape without saying why the shape exists.

That difference is why a pattern name survives being spoken in a stand-up
or typed into a one-line review comment, while a description of the code's
structure does not. The shape is visible in the diff already; a reviewer
can see the interface and its implementations without being told anything.
What the diff cannot show on its own is whether the forces that justify
that shape are present, today, in this code, for this team. The
name is supposed to carry that judgment alongside the shape, which is why
"this is a Strategy" is a defensible design claim when the forces back it
up, and empty decoration borrowed from a catalogue when they do not.

The test for whether a name is still doing real work is simple to state:
can it be unpacked back into the forces it names, right now, for this
code, without reaching for a textbook definition from memory. If a
teammate asks "why is this a Strategy and not a `switch` expression," and
the answer is a sentence about which requirements compete and which one
wins, the name is still attached to the thing it names. If the answer is
"that is just what you reach for when a value needs to vary," the name has
come loose from its forces, and the recognize-first habit this book trains
has not been applied.

## Reasoning at the level the decision lives

A team that shares pattern vocabulary gets something a team without it
does not: the ability to hold a design conversation at the altitude where
the decision lives, instead of at the altitude of individual
lines. Consider a pull request that introduces a connection pool for a
billing service's datastore. A reviewer without shared vocabulary reads
the diff line by line, confirms the lock is acquired correctly, confirms
the connection is returned on every exit path, confirms the tests pass,
and approves. A reviewer who shares the vocabulary this book builds asks a
different, prior question first: does this service need the
correctness property a pool exists to provide, that no two callers ever
hold the same live connection at once, or is a single shared connection
guarded by a lock sufficient for the traffic this service sees today. That
question is only askable once "connection pool" is a name both people hold
in their heads as a claim about forces, not merely a block of code waiting
for a syntax check.

This is where naming the pattern pays off in review, not only in design.
A pull request description that reads "implements Observer for cache
invalidation" invites one kind of review, line by line, correctness-only.
One that reads "the cache now needs to invalidate itself the instant the
underlying record changes, without the reader polling for it; Observer
over a scheduled refresh because staleness has to be measured in
milliseconds, not in a polling interval" invites a sharper kind, because
the author did the forces-first reasoning before reaching for the name,
and the reviewer can check that reasoning directly instead of having to
reconstruct it.

The same shift changes what a design discussion can accomplish in an
hour. A meeting that spends its time re-deriving what a Factory is, from
first principles, every time the word comes up, has less time left for the
decision in front of it: does this construction logic have the
forces a Factory addresses, variation in which concrete type gets built,
construction involved enough that inlining it at every call site would
hurt, or is `new PremiumAccount(...)` already the right answer and a
Factory would be a layer wrapped around nothing. Shared vocabulary does
not make that decision for the team. It clears the table so the team can
make it without first re-litigating what the words mean.

## The same word, read by a teammate and by an AI assistant

Every claim above about a teammate reading a pattern name applies, almost
unchanged, to an AI assistant reading one, and that parallel is worth
sitting with rather than rushing past. When you prompt an assistant with
"add an Observer so the billing dashboard updates the instant an invoice
status changes, without polling," you are not handing it a code shape to
produce. You are handing it the forces: something changes, a dependent
needs to react to the change without being asked whether it happened, and
polling is specifically the cost this design has to avoid. An assistant
that receives the forces, not just the keyword, can make the same
idiomatic choice a human engineer would: a listener interface registered
on the subject here, a `java.util.function.Consumer` passed to a
registration method there, because the forces, not the word alone,
determine which idiomatic Java expression actually fits the situation.

The reverse prompt is where the compression starts to leak. "Make this
reactive, add a pattern" hands the assistant a word-shaped hole with no
forces inside it, and a model will reliably fill that hole with whichever
pattern shape is statistically most familiar for code that resembles the
surrounding context, whether or not the forces in your actual code call
for it. This is not a flaw unique to AI tooling; it is the identical
failure mode of reaching for a pattern before its forces are present, the engineer who reaches
for a pattern's shape the moment it comes to mind, now running at a speed
that can produce a full implementation in the time it used to take to
describe the requirement. The fix is identical in both cases: supply the
forces before the name enters the conversation, whether the name is about
to come out of your own head or out of a model's.

This matters doubly for review, because an AI assistant's output is still
code someone has to be accountable for, and a precise pattern name is the
fastest tool available for holding that code to the same standard a
human co-author's code gets held to. A generated pull request that claims
"implements Observer for cache invalidation" deserves exactly the two
questions a human-authored one does: what forces does this solve, and was
a plainer alternative considered and rejected for a stated reason. The
assistant will not ask those questions of itself unless the forces were in
the prompt it received. That means naming a pattern precisely, alongside
the forces that justify it, is now a primary review tool for a machine
collaborator's output, not an optional afterthought layered on top of
accepting the diff.

> **AI sidebar.** Treat a pattern name attached to generated code the same
> way you would treat one attached to a teammate's code: as a claim to
> verify, not a label to trust. Ask the assistant to state the forces
> alongside the name, for example "which requirement in this code actually
> varies, and why does polling not satisfy it," before accepting the shape
> it produced. A model that reaches for Observer because the request said
> "reactive," with no actual change-notification requirement underneath
> it, will usually answer that question with a restatement of the pattern's
> textbook definition rather than a fact about your code, which is the
> signal that the name arrived before the forces did. The same scrutiny
> runs in reverse: when you are the one prompting, naming the pattern
> explicitly in the request, "a Strategy, because the discount rule is
> owned by a separate pricing team and changes independently of checkout,"
> gives the model the forces it needs to pick the right idiomatic Java
> expression instead of guessing from the keyword alone.

## A wrong name costs more than no name

A missing name is an inconvenience; a wrong one is a liability, because a
wrong name sends every reader who trusts it toward the wrong set of
forces. Call a plain validation method a "Visitor" because it happens to
walk a tree of objects, and a teammate who later needs to extend that
validation will go looking for the double-dispatch machinery the Visitor
name promises, machinery that was never actually built, because the
original code was a loop with a type check, not a Visitor at all. The
teammate loses time not because the code was bad, but because the name
told them to expect a different shape than the one actually sitting in
the file.

The same liability compounds with an AI assistant in the loop, because
the assistant treats a misnamed comment or docstring as ground truth about
the surrounding code's design, the same way it treats a prompt. A method
documented as "implements the Builder pattern" that is actually a
four-argument constructor with named parameters will lead an assistant
asked to "add a new optional field to the Builder" toward inventing a full
builder class where a fifth constructor parameter, or a small parameter
object, would have been the smaller, correct change. The fix in both
cases, for a human reader and for a model, is the same discipline of recognizing
the forces first: a name is accurate only if it can be unpacked back into the
forces actually present, and a name that fails that test should be
corrected or removed rather than left to mislead the next reader, human or
otherwise.

This is also why a vague name is its own failure mode, distinct from a
wrong one. "This uses a design pattern for flexibility" names nothing at
all. It passes a code review comment without making a verifiable claim,
and it gives an assistant prompted with the same vagueness nothing to
constrain its answer with. A name that cannot be stated specifically,
Strategy, Observer, Adapter, and backed by the one or two forces that
justify it, has not actually been chosen yet. Precision here is not
pedantry. It is the difference between a sentence a reviewer can check and
a sentence that only sounds like one.

## Naming as the hinge of the method

The arc this book trains, recognize the forces, name a candidate pattern
and a candidate non-pattern, choose the idiomatic Java expression, test
the behavior, and know when not to use it, has naming sitting at its
center for a reason. Recognizing forces without ever naming them stays
private; the reasoning lives in one engineer's head and has to be
re-derived by everyone else who touches the code later. Naming a pattern
without first recognizing the forces skips straight to decoration, the
exact failure of applying a pattern with no force that calls for it. Naming is the hinge
between the two: it is the step where private reasoning about trade-offs
becomes a public, checkable word that a teammate can challenge, a future
reader can trust, and an AI assistant can be prompted with and reviewed
against.

That is also why this book spends a full catalogue entry on getting a
name specifically right, chapter by chapter, instead of treating the
Gang of Four's list as a glossary to skim once. A name used loosely drifts
from its forces a little more each time it is applied without the
justification behind it, until "Strategy" means nothing more precise than
"this has an interface," and the compression the name was supposed to
provide is gone for the whole team, not just for one careless use of it.
Every pattern chapter ahead names its pattern against a specific,
real-domain example precisely so that the name stays attached to the
forces that earned it, chapter after chapter, rather than drifting into a
generic label for "code that looks object-oriented."

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480/9780321700698) (Addison-Wesley): the catalogue whose names this chapter treats as shared vocabulary, each entry stated as a recurring problem and the core of a solution, not a label for a shape.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); every citation in this book is a pointer to the source, not a substitute for reading it.
