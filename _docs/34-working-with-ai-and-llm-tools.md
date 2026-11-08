---
title: "Working with AI and LLM tools"
order: 34
layout: tutorial
sectionid: docs
part: "putting-it-together"
description: "Using an LLM as a pattern-literate collaborator: name the forces and the candidate pattern before prompting, recognize patternitis, hallucinated APIs and non-idiomatic Java, and pass every draft through a verification gate of behavior tests plus an idiomatic review. A return-eligibility Strategy pairs a correct draft with a plausible one carrying a single off-by-one, caught by the same boundary table."
duration: "35 min"
---

## Learning objectives

By the end of this chapter you can:

- Prompt with the forces and a candidate pattern name instead of a vague feature request.
- Recognize three failure modes of generated Java: a pattern no force calls for, an API that doesn't exist, and code that ignores the codebase's modern idioms.
- Apply the verification gate: behavior tests and an idiomatic review, both required, neither satisfied by compiling.
- Find the one comparison that separates a correct Strategy from a plausible wrong one, and the boundary case that catches it.
- Decide when prompting costs more than it returns, and name the calls that stay with the engineer.

{% include excalidraw.html name="34-ai-verification-gate" caption="Figure 34.1: the engineer names the forces and a candidate pattern, the AI assistant drafts an implementation, and nothing ships past the verification gate, behavior tests plus an idiomatic review, without passing both." %}

## Why this matters

An assistant can draft a Strategy, a Factory Method or a State machine in seconds, in your system's vocabulary if you supply it. That changes how fast a design decision becomes running code. It doesn't change who answers for the code once it ships.

The forces vocabulary (Strategy when behavior varies by a condition a caller controls; a plain method or `switch` when no second implementation is coming) is also the most reliable way to direct a model that has seen a vast amount of Java and has no idea which of it applies to the method in front of you. A vague prompt such as "make this configurable" or "add some error handling" leaves the model nothing to anchor the answer's shape except the question's. A precise prompt names the varying behavior, the caller who controls it and the pattern you expect, and the answer can be judged by the same test applied everywhere: does this code need it, and does the reply solve it the way the forces call for?

AI changes who writes the code, not who is responsible for whether it's correct and idiomatic. The rest follows from that.

## Naming the forces before prompting

A pattern name compresses a cluster of forces, and it's the interface between engineer and assistant the way an API contract is between two services. Compare two prompts:

| Prompt | What it fixes |
| --- | --- |
| "A customer's return request classifies into a refund outcome that varies only by days since delivery, and checkout and support code shouldn't know the policy boundaries. Write it as a Strategy: an interface and an injected implementation." | The force, the pattern to anchor on, and the scope: nothing outside the classification changes. |
| "Write me a function that figures out if someone can return something." | Nothing: whether the outcome set is closed, how often a tier changes, what must be decoupled. |

The second leaves every gap to be filled with whatever shape is common for "figure out X from Y," which is rarely the minimal one. Naming the candidate pattern turns an underspecified request into a specified one.

The vocabulary also reads the reply. A generated `ReturnPolicyEngine` that wraps one `ReturnEligibilityStrategy` behind a factory, a registry and a builder hasn't built a Strategy; it has built three types around a decision already made. Ask where the second implementation is and who chooses it. If there isn't one, the scaffolding is speculative generality with a pattern's name, and the single implementation is the answer.

A pattern name can also rule one out: "This return policy has exactly three tiers decided by one number, and nothing needs a second way to source that number, so don't wrap it behind a configurable rule engine." That instruction heads off the most common failure mode below.

> **🤖 Explore with your agent**
>
> "In `examples/34-ai-verification-gate`, `PlausibleWrongReturnEligibilityStrategy.classify` is meant to follow the same policy as `DraftedReturnEligibilityStrategy`: `FULL_REFUND` through day 29, `STORE_CREDIT` from day 30 through day 59, `INELIGIBLE` from day 60, both lower bounds inclusive. Find every input where the two disagree, name the comparison responsible, and propose the one-operator fix. Don't change `ReturnEligibilityStrategy`, `RefundPolicy` or any other method, and don't restructure the early returns."
>
> Check: the reply names day 30 and the `<= 30` comparison, nothing else. Apply the fix in a scratch copy and `mvn -q -f examples/34-ai-verification-gate/pom.xml test -Dtest=VerificationGateTest` fails one case: the gate no longer finds a disagreement to reject.

## The failure modes of an unchecked collaborator

All three come from optimizing for a plausible answer to the prompt rather than the smallest correct answer to the problem.

**Reaching for a pattern no force calls for.** Ask for a class that reads one configuration value and you can get an interface, one implementation, a factory returning it and a holder everything resolves through. Each piece has a textbook justification, and none answers a force present in reading one value. This is patternitis: reaching for a familiar shape because it's familiar. For every type the reply introduces, ask what force justifies it, and delete the ones without an answer.

**Hallucinated APIs.** Generated code can call a method a standard library type doesn't have, pass arguments in an order no overload accepts, or use a collector from a different JDK. It's produced by predicting text that looks like correct code, with no compiler in the loop, so a `java.util.stream` call with another overload's signature reads as plausible and fails on compile. Compile it, and read the Javadoc rather than trusting a description of an API.

**Code that ignores a modern idiom.** Training data spans more than two decades of Java. The result is a hand-written `equals`/`hashCode` where a record gives both, a mutable field with getter and setter where an immutable value says what's meant, a `switch` with a throwing `default` where enum constant-specific bodies make a missing case a compile error, or a loop into a mutable list where a `Collector` reads more clearly. The answer is correct; the code doesn't look like the codebase it joins, and accepting it on "it compiles and the tests pass" erodes the baseline one method at a time.

## The verification gate that makes AI output safe

None of the three shows up by reading and nodding. A pattern with no force compiles. A hallucinated API sometimes compiles against the wrong overload and survives a smoke test. Idiom isn't something a compiler checks. Two gates sit between a draft and a merge, and both are required:

| Gate | Checks | Catches |
| --- | --- | --- |
| Behavior tests | The specification, written by a human, not the draft's own output | Wrong answers, including a misread boundary |
| Idiomatic review | The project's conventions: a record over a hand-written value class, a sealed hierarchy over an open one, a stream over a mutable accumulator where it reads naturally | Code that works but doesn't fit, which no compiler flags |

Tests written by asking the same model to "write tests for the function you just wrote" inherit its blind spots. If it read a boundary as exclusive, its tests usually encode that misreading as the expected answer, and they pass. The table has to come from the specification, written or reviewed by someone who holds it.

A draft that passes its behavior tests but ignores an idiom the rest of the file uses has failed half the gate.

An assistant also helps build the checks. Asked to "generate a test for every branch this method currently takes, asserting whatever it currently returns, without changing anything," it produces characterization coverage faster than typing the cases by hand. What it shouldn't decide unsupervised is whether a boundary, tier or branch is the one the business meant. That call belongs to the engineer who holds the specification.

## Catching the bug in code: a return-eligibility Strategy

The code is in `examples/34-ai-verification-gate`. The prompt is the forces-based one above: a return request classifies into a full refund, store credit or no return, varying only by days since delivery, written as a Strategy. `ReturnEligibilityStrategy` has one method, `classify(int daysSinceDelivery)`, returning a `ReturnEligibility`.

`DraftedReturnEligibilityStrategy` implements the policy: a full refund through day 29, store credit from day 30 through day 59, no return from day 60. Both lower bounds are inclusive, so a return on exactly day 30 gets store credit.

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

`PlausibleWrongReturnEligibilityStrategy` is the kind of draft the same prompt can return: the same interface, outcomes and early-return structure, which a reviewer checking "does this look like a Strategy" would wave through. It has one bug, `<=` instead of `<` on the first boundary:

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

Side by side, the two differ by one character, and `<=` looks reasonable unless you're holding the policy's exact wording. It's structurally idiomatic and wrong at one input.

The gate is a boundary table: days 0, 29, 30, 59 and 60. The drafted strategy matches the policy at all five, which is what lets it ship. The plausible one matches at four:

| Day | Policy | Drafted | Plausible wrong |
| --- | --- | --- | --- |
| 0 | `FULL_REFUND` | `FULL_REFUND` | `FULL_REFUND` |
| 29 | `FULL_REFUND` | `FULL_REFUND` | `FULL_REFUND` |
| 30 | `STORE_CREDIT` | `STORE_CREDIT` | `FULL_REFUND` |
| 59 | `STORE_CREDIT` | `STORE_CREDIT` | `STORE_CREDIT` |
| 60 | `INELIGIBLE` | `INELIGIBLE` | `INELIGIBLE` |

Agreement everywhere but day 30 is why the bug slips past a casual read, and why a table that tests every boundary, rather than one comfortable value inside each tier, catches it.

`RefundPolicy` turns the bug into money. Store credit is eighty percent of the order total, so on a $200 order returned on day 30 the drafted strategy credits $160 and the plausible one credits $200, a $40 overpayment from one operator. Nothing about a three-branch Strategy looks risky enough to skip the gate. The risk was never the pattern's complexity; it was the boundary.

## Build, run, observe

From the repository root:

```sh
mvn -f examples/pom.xml test
```

Or standalone:

```sh
cd examples/34-ai-verification-gate
mvn test
```

All 10 cases pass. To see both strategies classify the same days:

```sh
examples/34-ai-verification-gate/demo.sh
```

It prints each strategy's answer at every boundary, then the one disagreement and its cost:

```
Day 30, order total $200.00
Drafted strategy:        STORE_CREDIT -> refund $160.00
Plausible-wrong strategy: FULL_REFUND -> refund $200.00
Overpayment from the off-by-one: $40.00
```

- **Accept the plausible draft.** In a scratch copy, change `DraftedReturnEligibilityStrategy`'s first comparison to `<= 30`. Two cases fail: the day-30 row of the boundary table and the day-30 refund amount.

## When not to reach for an assistant

An assistant pays on a task with a clear specification and a cheap check: a well-understood contract, a test suite that exists or can be written, a refactor whose behavior preservation can be shown mechanically. It costs more than it returns when:

- **The specification is the hard part.** Deciding what the system should do, not how. A fluent answer to an unclear requirement looks finished, which makes it more dangerous than an obviously incomplete one.
- **The change spans state the model wasn't shown.** Shared mutable state, concurrency, or an invariant across files outside the context window. "Looks safe" is bounded by what the model saw, not by what the system guarantees.
- **The bottleneck is a design decision.** Which pattern the forces call for, whether an abstraction is premature, whether a class has outgrown its responsibility. Delegating it moves the judgment to something less equipped, and you redo the forces analysis afterward to check it.

The calls that stay with the engineer don't change with the tooling: naming the forces in the actual problem, deciding whether a pattern's cost is worth what it buys, and owning the answer once it ships. An assistant can draft any pattern and the tests for its own draft in seconds. It can't tell you whether the problem needed a pattern at all, and it has no stake in being right.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the shared pattern vocabulary that works as the interface between engineer and assistant, and the catalogue patternitis misapplies.
- Robert C. Martin, [*Agile Software Development: Principles, Patterns, and Practices*](https://www.pearson.com/store/p/agile-software-development-principles-patterns-and-practices/P200000003363/9780135974445) (Prentice Hall): the Single Responsibility and Dependency Inversion reasoning behind the idiomatic-review gate.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): the idiom baseline, records over hand-written value classes and enum constants over a `switch` with a throwing default, that the review checks a draft against.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/34-ai-verification-gate`, which runs both strategies against the same boundary table.
- The [bibliography page]({{ '/bibliography/' | relative_url }}) lists the full reading list.
