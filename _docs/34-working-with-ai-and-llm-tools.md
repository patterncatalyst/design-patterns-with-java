---
title: "Working with AI and LLM tools"
order: 34
layout: tutorial
sectionid: docs
part: "putting-it-together"
description: "Using an LLM as a pattern-literate collaborator rather than a code vending machine: naming the forces and the candidate pattern before prompting, the failure modes an unchecked collaborator produces (patternitis, hallucinated APIs, non-idiomatic Java), and the verification gate, behavior tests plus an idiomatic review, that decides whether a draft ships. A runnable return-eligibility Strategy pairs a drafted implementation against a plausible-looking one carrying a single off-by-one at a policy boundary, caught by the identical boundary table run against both."
duration: "35 min"
---

## Learning objectives

By the end of this chapter you can:

- Prompt an AI pairing partner with the forces vocabulary and a candidate
  pattern name instead of a vague feature request, and explain why that
  framing changes the quality of what comes back.
- Recognize the three recurring failure modes of AI-generated Java: reaching
  for a pattern no force calls for, inventing an API that does not exist,
  and writing code that ignores a modern idiom the rest of the codebase
  already uses.
- State the verification gate that decides whether an AI-drafted pattern
  ships: a behavior-test suite and an idiomatic-review pass, neither one
  optional and neither one satisfied by the code merely compiling.
- Read a drafted Strategy implementation against a plausible-looking,
  subtly wrong one and say exactly which comparison produces the
  difference, then show the single test that catches it.
- Decide when prompting an assistant costs more than it returns, and name
  the judgment calls that stay with the engineer regardless of how capable
  the tooling gets.

{% include excalidraw.html name="34-ai-verification-gate" caption="Figure 34.1: the engineer names the forces and a candidate pattern, the AI assistant drafts an implementation, and nothing ships past the verification gate, behavior tests plus an idiomatic review, without passing both." %}

## Why this matters

An AI pairing partner can draft a Strategy, a Factory Method, or a State
machine in seconds, and it can do it in a shipping system's actual
vocabulary if you give it that vocabulary to work with. That capability
changes how fast a design decision turns into running code. It does not
change who answers for the code once it ships. The forces vocabulary and
pattern catalogue this book builds, Strategy when behavior varies by a
condition a caller controls, a plain method or a plain `switch` when no
second implementation is coming, were never only for a human reviewer
reading someone else's pull request. They're also the most reliable way to
get useful work out of a model that has seen a vast amount of Java in its
training data and no idea which three lines of that corpus apply to the
method in front of you right now.

Treat an assistant as a collaborator who has read every pattern book ever
indexed and applied none of the judgment that decides when a pattern
belongs. That collaborator is extraordinarily fast at producing a
plausible-looking answer to whatever question you ask, and the
plausibility of the answer has only a loose relationship to whether it is
correct, idiomatic, or proportionate to the problem. A vague prompt, "make
this configurable" or "add some error handling," gets a vague and usually
over-engineered answer, because the model has nothing to anchor the shape
of its response except the shape of the question. A precise prompt, naming
the varying behavior, the caller who controls it, and the candidate pattern
you expect to resolve it, gets an answer you can evaluate against the same
forces test this book applies everywhere else: does the code in front of
you need this, and does what came back solve it the way the forces call
for.

The throughline for this chapter, and for using AI tooling on any codebase
that has to run in production, is a single sentence: AI changes who writes
the code, not who is responsible for whether it is correct and idiomatic.
Everything else in this chapter is a consequence of taking that sentence
seriously.

## Naming the forces before prompting

A pattern's name is a compression of a specific cluster of forces, and that
compression is the interface between an engineer and an assistant in the
same way an API contract is the interface between two services. A request
phrased as "a customer's return request needs to classify into a refund
outcome that varies only by how many days have passed since delivery, and
checkout or support code should not need to know the exact policy
boundaries, write this as a Strategy with the usual shape" gives the model
three things a vague request withholds: the force that justifies a pattern
at all, the pattern name to anchor its answer against so it reaches for an
interface and an injected implementation rather than a chain of unrelated
conditionals, and an implicit scope boundary, nothing outside the
classification rule needs to change. Each of those constraints narrows the
space of plausible answers the model can produce, and a narrower space of
plausible answers is a more reliable space.

Compare that to "write me a function that figures out if someone can
return something." Nothing in that sentence tells the model whether the
outcome set is closed or open, whether a new tier is a common event or a
once-a-year policy change, or whether anything else in the system needs to
be decoupled from the choice. The model fills every gap with a guess drawn
from whatever shape is statistically common in its training data for
"figure out X based on Y," and that shape is rarely the minimal one. Naming
the candidate pattern isn't a courtesy to the model. It's the mechanism
that turns an underspecified request into a specified one, the same
discipline this book applies to recognizing design before writing any code
at all.

The same vocabulary works in the other direction too, reading what the
model hands back. A generated class named `ReturnPolicyEngine` that wraps a
single `ReturnEligibilityStrategy` implementation behind a factory, a
registry, and a builder has not built a Strategy in the sense the catalogue
describes. It has built three additional types around one decision that was
already made before any of them were written. Naming the forces lets you
ask the follow-up question immediately: where is the second implementation
this machinery is supposed to select between, and who chooses it. If the
answer is "there isn't one yet," the generated scaffolding is speculative
generality wearing a pattern's name, and the plain, single implementation
the forces support is the smaller, correct answer.

Naming a pattern in a prompt also works for ruling one out. "This return
policy has exactly three tiers, decided by one number, and nothing in this
codebase needs a second way of sourcing that number, so don't wrap it
behind a configurable rule engine" is a legitimate and useful instruction,
and it short-circuits the single most common AI failure mode this chapter
covers next: reaching for an abstraction that nothing in the problem calls
for.

> **AI sidebar.** Ask an assistant to "fix the bug where the boundary check
> is wrong" and it will frequently do exactly that, and nothing more,
> flipping the one comparison operator the sentence pointed at. That narrow
> scoping has a consequence: an assistant answers the question it was
> asked, not the question you meant to ask, and it has no instinct for a
> related boundary two lines away unless you point at it. The inverse
> failure is just as real: ask it to "make the whole classification more
> robust" with no scope at all, and it will frequently touch far more than
> the one method you had in mind, restructuring working code nearby on the
> theory that robustness is a property of the whole file. Scope the request
> as precisely as you would scope a code review comment, naming the method,
> the input, and the specific boundary you want corrected, and treat
> anything the response touches outside that scope as a change that needs
> its own look before it merges.

## The failure modes of an unchecked collaborator

Three failure modes show up often enough in AI-generated Java to name
individually, and all three share a root cause: a model optimizing for "a
plausible-looking answer to this prompt" rather than for "the smallest
correct answer to the actual problem."

**Reaching for a pattern no force calls for.** Ask for a class that reads
one configuration value and returns it, and a sufficiently eager model will
sometimes hand back an interface, a single implementation, a factory that
returns the one implementation, and a holder class everything else resolves
through. Every piece of that answer is individually defensible: an
interface decouples consumers from implementations, a factory decouples
construction from use. None of those justifications is a force present in
reading one value out of one place. This is patternitis, the reflex of
reaching for a pattern's shape because it's familiar, not because anything
in the code in front of you calls for it, and a model trained on a large
corpus of tutorial code reproduces that reflex reliably whenever a prompt
leaves room for it. The fix is the same forces test this book applies
everywhere else: for every type the model introduces, ask what force
justifies it existing, and delete the ones with no answer.

**Hallucinated APIs.** A model will, with complete confidence and
syntactically valid-looking code, call a method that doesn't exist on a
standard library type, pass arguments in an order a real overload never
accepted, or reference a record component or collector from a newer or
older JDK than the one this book targets. This isn't a rare edge case; it's
a predictable consequence of how these models generate text, producing
the token sequence that looks most like correct code for the surrounding
context, with no compiler in the loop to reject what it wrote until a human
or a build step catches it. A generated call into `java.util.stream`
with a signature that belongs to a different overload reads as entirely
plausible to someone skimming it and fails the instant it's compiled. The
practical defense is unglamorous: compile it, and don't treat a model's
description of an API's behavior as a substitute for the Javadoc.

**Code that ignores a modern idiom.** Training data spans more than two
decades of Java written against standards from the original language
specification forward, and a model has no inherent preference for the
idiom this book teaches. The result shows up as a hand-written
`equals`/`hashCode` pair where a record already gives you both, a
mutable field with a getter and a setter where an immutable value type
communicates intent directly, a `switch` statement with a `default` branch
that throws where an enum's constant-specific method bodies would make a
missing case a compile error, or a loop collecting into a mutable list
where a stream `Collector` says the same thing more clearly. None of this
code is wrong in the sense of producing an incorrect answer. It's wrong in
the sense that it doesn't look like the rest of the codebase it's
joining, and a reviewer who accepts it on the strength of "it compiles and
the tests pass" has let the project's idiomatic baseline erode one
generated method at a time.

## The verification gate that makes AI output safe

Nothing in the three failure modes above is detectable by reading the code
and nodding. A pattern applied with no force present compiles. A
hallucinated API sometimes compiles against the wrong overload and runs
without throwing on the one input a casual smoke test happens to exercise.
Code that ignores a modern idiom compiles by definition, since idiom is not
something the compiler checks. The only thing that catches all three
reliably, applied the same way regardless of who or what wrote the code, is
the same discipline this book has applied to every pattern in its
catalogue: a behavior test that proves the code does what it claims, not
merely that the build exits zero.

Two gates sit between an AI-drafted pattern and a merge, and both are
required, not either-or. The first is a behavior-test suite, written
against the specification the code is supposed to satisfy rather than
against whatever the generated implementation happens to do. A test suite
written by asking the same model "write tests for the function you just
wrote" inherits the model's own blind spots; if it misread a boundary as
exclusive on the low end, a self-generated test suite will usually encode
that same misreading as the expected answer, passing tests of that kind
without a human writing or reviewing the test table first. The second gate
is an idiomatic-code review pass, checking the draft against the project's
actual conventions, a record over a hand-written value class, a sealed
hierarchy over an unconstrained one, a stream pipeline over a mutable
accumulator loop where the collection already reads naturally that way, and
fixing what the compiler will never flag because idiom isn't a compiler's
job.

Both gates exist because they catch different classes of defect. A
behavior test proves the code is correct against a specification a human
wrote down and understood. An idiomatic review proves the code fits the
codebase it is joining, so the next engineer, human or AI, reading this
file inherits a consistent baseline rather than a patchwork of whichever
era's Java habits happened to generate each method. A pattern that passes
its behavior tests but ignores a modern idiom the rest of the file already
uses has still failed half of what this discipline checks for.

AI tooling is useful on the other side of this discipline too, not only as
the thing being checked but as a tool for building the checks themselves.
Given an existing method and a request to "generate a test for every branch
this method currently takes, asserting whatever it currently returns,
without changing anything," a model can produce a wide net of coverage
faster than a human typing the same cases by hand, the mechanical half of
pinning down an existing system's behavior before changing anything near
it. What a model should never decide unsupervised is whether a given
boundary, tier, or branch is the one the business meant; that
judgment call, like the pattern-selection judgment call this whole book
trains, belongs to the engineer who holds the actual specification, not the
one who only holds the code.

## Catching the bug in code: a return-eligibility Strategy

The example behind this chapter, in `examples/34-ai-verification-gate`,
builds exactly the scenario the verification gate exists to catch. The
prompt an engineer might give an AI pairing partner here is specific and
forces-based: "a customer's return request classifies into one of three
outcomes, a full refund, store credit, or no return at all, and the
classification varies by nothing except how many days have passed since
delivery, so write it as a Strategy with the usual shape, an interface and
a concrete implementation behind it." `ReturnEligibilityStrategy` is the
resulting contract, one method, `classify(int daysSinceDelivery)`,
returning a `ReturnEligibility`.

`DraftedReturnEligibilityStrategy` implements the policy's actual
boundaries: a full refund through day 29, store credit from day 30 through
day 59, no return accepted from day 60 on. Both lower bounds are inclusive,
so a customer's return filed on exactly day 30 has crossed into store
credit, not stayed eligible for a full refund.

```java
public ReturnEligibility classify(int daysSinceDelivery) {
    if (daysSinceDelivery < 30) {
        return ReturnEligibility.FULL_REFUND;
    }
    if (daysSinceDelivery < 60) {
        return ReturnEligibility.STORE_CREDIT;
    }
    return ReturnEligibility.INELIGIBLE;
}
```

`PlausibleWrongReturnEligibilityStrategy` is the shape an AI pairing
partner handed back for the identical prompt: the same interface, the same
three outcomes, the same early-return structure a reviewer skimming for
"does this look like a Strategy" would wave through. It carries one known
bug, a single comparison operator on the first boundary, `<=` instead of
`<`:

```java
public ReturnEligibility classify(int daysSinceDelivery) {
    if (daysSinceDelivery <= 30) { // off-by-one: should be < 30
        return ReturnEligibility.FULL_REFUND;
    }
    if (daysSinceDelivery < 60) {
        return ReturnEligibility.STORE_CREDIT;
    }
    return ReturnEligibility.INELIGIBLE;
}
```

Reading the two implementations side by side, the bug is easy to miss. Both
methods have the identical structure, the identical number of branches, and
the identical second comparison; only the operator on the first branch
differs by one character, and `<=` reads as an entirely reasonable choice
if you aren't holding the policy's exact wording in your head while you
review it. This is precisely the shape this chapter's failure modes
describe: structurally idiomatic and wrong at exactly one input a
read-only review is unlikely to notice.

`VerificationGateTest` is the gate. A parameterized test drives
`DraftedReturnEligibilityStrategy` through the full boundary table, day 0,
29, 30, 59, and 60, against the expected outcome at each point, and every
case passes, which is what lets it ship. A second test drives the
plausible-wrong strategy through the identical values:

```java
assertThat(plausibleWrong.classify(0)).isEqualTo(ReturnEligibility.FULL_REFUND);
assertThat(plausibleWrong.classify(29)).isEqualTo(ReturnEligibility.FULL_REFUND);
assertThat(plausibleWrong.classify(59)).isEqualTo(ReturnEligibility.STORE_CREDIT);
assertThat(plausibleWrong.classify(60)).isEqualTo(ReturnEligibility.INELIGIBLE);

assertThatThrownBy(() -> assertThat(plausibleWrong.classify(30))
        .isEqualTo(ReturnEligibility.STORE_CREDIT))
        .isInstanceOf(AssertionError.class);
```

Every boundary except day 30 agrees with the policy, which is exactly what
makes a one-operator bug this easy to miss on a casual read and this
reliable to catch with a table that bothers to test every boundary rather
than one value comfortably inside each tier.

`RefundImpactTest` and `RefundPolicy` turn the same bug into a dollar
figure, because a return-eligibility misclassification is not an abstract
correctness issue, it is a wrong amount credited to a customer's account.
On a $200 order filed on day 30, the drafted strategy correctly classifies
store credit and `RefundPolicy` credits eighty percent of the order total,
$160. The plausible-wrong strategy keeps the customer in full-refund
territory, crediting the entire $200, a $40 overpayment traceable to one
comparison operator. That number is the concrete argument for why a
pattern this small still goes through the verification gate: nothing about
a Strategy with three branches looks risky enough to skip testing, and the
risk was never in the pattern's complexity. It was in the boundary a
plausible-looking draft got wrong.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there's no container anywhere in this
loop. From the repository root, the aggregator picks up
`examples/34-ai-verification-gate` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/34-ai-verification-gate
mvn test
```

Either path runs the same 10 JUnit 5 cases, asserted with AssertJ,
including a parameterized suite that checks the drafted strategy against
the policy's full boundary table. Observed locally on Fedora with Temurin
25:

```
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see the drafted and plausible-wrong strategies classify the same days
side by side, rather than just reading an assertion that says they
disagree, run the example's own demo script:

```sh
examples/34-ai-verification-gate/demo.sh
```

It prints both strategies' classification for a sample of days spanning
every policy boundary, then isolates the exact day-30 case where they
disagree and the dollar amount that disagreement costs:

```
Day 30, order total $200.00
Drafted strategy:        STORE_CREDIT -> refund $160.00
Plausible-wrong strategy: FULL_REFUND -> refund $200.00
Overpayment from the off-by-one: $40.00
```

## When not to reach for an assistant

An AI pairing partner is worth reaching for on a task with a clear
specification and a cheap way to check the answer: a method whose contract
is well understood, a test suite that can be written or already exists, a
refactor whose behavior preservation can be proven mechanically. Reaching
for one costs more than it returns in a few recurring situations.

When the specification itself is the hard part, deciding what the system
should do rather than how to make it do that, a fluent-sounding answer to
an underspecified question is a liability, not a shortcut. It will produce
something plausible for almost any prompt, and a plausible-sounding wrong
answer to an unclear requirement is more dangerous than an obviously
incomplete one, because it looks finished. When a change touches shared
mutable state, concurrency, or an invariant that spans multiple files the
model was not shown, the model's view of "this looks safe" is bounded by
what fits in its context window, not by what the system guarantees. A change that looks safe in isolation can break an invariant a
different file depends on, and only an engineer who holds the whole
system's shape has the standing to make that call. When the actual
bottleneck is a design decision, which pattern the forces call for, whether
an abstraction is premature, whether a class has outgrown its single
responsibility, delegating the decision to a model doesn't speed up the
work. It just moves the judgment call to someone, or something, less
equipped to make it, and the engineer ends up re-deriving the same forces
analysis afterward to check the model's choice anyway.

The judgment calls that stay with the engineer, regardless of how capable
the tooling gets, are the same ones this book has trained from its first
chapter: naming the forces present in the actual problem, deciding whether
a pattern's cost is worth paying for the benefit it buys, and accepting
responsibility for the answer once it ships. An assistant can draft any of
this book's patterns in seconds and can draft the test suite that checks
its own draft just as fast. It cannot tell you whether the problem in front
of you needed a pattern at all, and it has no stake in being right. That
stake is the one thing this chapter cannot delegate.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the shared pattern vocabulary this chapter treats as the actual interface between an engineer and an AI pairing partner, and the same catalogue a model's patternitis reflex misapplies when no force in the code calls for it.
- Robert C. Martin, [*Agile Software Development: Principles, Patterns, and Practices*](https://www.pearson.com/store/p/agile-software-development-principles-patterns-and-practices/P200000003363/9780135974445) (Prentice Hall): the Single Responsibility and Dependency Inversion reasoning behind the idiomatic-review gate this chapter applies to AI-drafted code, the same forces test this book has used on every pattern in its catalogue.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): the idiom baseline, records over hand-written value classes, enum constants over a `switch` with a throwing default, that an idiomatic-review pass checks a generated draft against.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/34-ai-verification-gate`'s suite, which asserts the drafted and plausible-wrong strategies against the same boundary table rather than against one comfortable value per tier.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
