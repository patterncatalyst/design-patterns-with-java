---
title: "Patterns as decisions under forces"
order: 1
layout: tutorial
sectionid: docs
part: "recognizing-design"
description: "Why a design pattern is a named response to a recurring set of trade-offs rather than a recipe to apply from memory, and the five-step arc this book trains for every pattern that follows."
duration: "20 min"
---

## Learning objectives

By the end of this chapter you can:

- Explain why a pattern is a name for a recurring trade-off, not a template to copy.
- Name the forces behind a design decision: requirements that one piece of Java can't satisfy at once.
- Walk the five-step arc used for every pattern: recognize the forces, name a candidate pattern and a candidate non-pattern, choose the idiomatic Java expression, test the behavior, and know when not to use it.
- Hold a pattern name suggested by an AI pairing partner to the same standard as one from a teammate.

## Why this matters

A checkout quotes a shipping rate per carrier. An engineer fresh from the Gang of Four reaches for Strategy: an interface, three one-line implementations and a factory to pick among them. It compiles and runs, and it's worse than the three-case `switch` it replaced, because nothing in the code needed what the pattern provides. The name came before the problem.

A pattern is the name of a recurring set of forces, requirements that pull against each other, and the shape of code that resolves them. Recognize the forces first and the name follows; reach for the name first and you get decoration.

{% include excalidraw.html name="01-forces-and-decisions" caption="Figure 1.1: The arc this book trains for every pattern: recognize the forces, name a candidate pattern and a candidate non-pattern, choose the idiomatic Java expression, test the behavior, and know when not to use it." %}

## A pattern is a name, not a recipe

Gamma, Helm, Johnson and Vlissides define a pattern as a problem that recurs and the core of its solution, stated generally enough to apply many times without writing the same code twice. A recipe says what to type. A pattern says what problem you're looking at and leaves the code open.

Two engineers can both spot Strategy in a rate calculator and write different Java. One writes an interface with a class per carrier, because a separate integrations team adds carriers. The other writes lambdas, because each rate is a one-line calculation with no collaborators. Both are Strategy:

```java
@FunctionalInterface
public interface RateStrategy {

    RateQuote quote(Shipment shipment);
}
```

`RateStrategy` accepts a named class, a lambda or an enum constant, and the calculator that holds one doesn't change when the algorithm does. Learn one code shape per name and you'll misapply it when the forces differ. Learn the forces and you'll recognize the pattern in code that looks nothing like the first example you saw.

## Forces: the vocabulary of trade-offs

A force is a requirement that competes with another in the same code. Neither is wrong to want; the tension is the design problem. These four recur across the catalogue:

| Force | Pulls against | Shipping example |
| --- | --- | --- |
| Flexibility | Simplicity | A rate calculator that swaps carrier logic at runtime has more moving parts than one that can't. |
| Testability | Directness | A tracking service that takes a `CarrierClient` interface takes a Mockito mock in a test; one that calls `new` inside the method is a line shorter and a test harder. |
| Extensibility | YAGNI | A seam for three carriers that never sign is a cost paid now for a benefit that may never come. |
| Current idiom | Stability | A lambda replaced the anonymous inner class and a `switch` expression replaced the `switch` statement, but the old code keeps compiling, so nobody revisits it. |

No force wins everywhere. A routing service that switches its carrier-selection rule per region behind a feature flag needs the flexibility. A warehouse weight check fixed by one conveyor's physical limit doesn't, and an abstraction for swapping that rule pays off only in a facility that needs it. Read the forces in the code in front of you, not the ones a pattern's name implies.

## The arc

Every pattern runs through the same five steps.

**1. Recognize the forces.** Ask what varies, what stays fixed and what's expensive to get wrong. In a checkout, the rate calculation varies and the rest of the flow stays fixed. In a warehouse inventory service, two pickers reserving the same unit is expensive to get wrong, so the design must prevent it by construction. Describe the tension before any name, because an early name anchors you on one answer.

**2. Name a candidate pattern and a candidate non-pattern.** Name a plain alternative just as explicitly, every time. For the rate calculation:

| | Strategy | `switch` expression over the carrier |
| --- | --- | --- |
| Wins when | eleven cases, changing every sprint, owned by different teams | three cases, changing twice a year, read by one team |
| Adding a carrier | a new class or lambda; no shared file changes | a new `case` in a file every carrier shares |
| Cost | an interface and a selection step | none beyond the `switch` |

The forces pick the winner. Naming both keeps the choice visible.

**3. Choose the idiomatic Java expression.** The name constrains the shape, not the code. The Gang of Four wrote for languages without lambdas or Java's enums. The same Strategy can be:

- an interface with one class per carrier, for strategies with collaborators or separate owners;
- an enum whose constants override a method, for a closed set the compiler can check;
- a functional interface holding lambdas, for one-expression algorithms or wrapping one strategy in another.

Choosing among them is part of the decision.

**4. Test it.** A pattern claims a behavior. Strategy claims that swapping the implementation changes the outcome without changing the caller; an inventory reservation claims no two pickers get the same unit. A build that exits zero proves neither. Each example carries JUnit 5 tests, with Mockito and AssertJ, that fail when the claimed behavior breaks. A claim you can't write as a failing assertion hasn't been verified.

**5. Know when not to use it.** A Strategy with one carrier and no credible second pays rent for a future that hasn't arrived. A factory that only calls one constructor is a layer with nothing under it. Every pattern chapter closes with this section, the other half of the decision.

## Judgment over trivia

Reciting what Observer looks like answers an interview question. Code asks something else: which forces are present, and does a pattern help. In review that becomes two questions: "What does this abstraction buy us?" about an existing Strategy, and "What would make this `switch` worth replacing?" about an existing non-pattern. The catalogue is the vocabulary for those answers.

## A shared vocabulary, for a team and for an AI

A pattern name compresses a paragraph into a sentence a reviewer can act on: "this needs a Strategy, not a `switch`, because separate teams now own the regional carriers." The compression works only while everyone means the same thing by the word. A suggestion that carries a pattern name, from a teammate or a model, gets the same questions: what varies, what stays fixed, and is the second implementation real or hypothetical.

> **🤖 Explore with your agent**
>
> "The checkout quotes rates with a `switch` expression over a `Carrier` enum with three cases, changed about twice a year by one team. Add a fourth carrier. First list what varies, what stays fixed and who owns each carrier's rate logic. Then compare two candidates: one more `case` in the existing `switch`, and a `RateStrategy` functional interface with one implementation per carrier. Recommend one, and name the force that would change your answer."
>
> Check: if the reply proposes an interface, a factory and a registry for four cases with one owner, ask which force it answers.

## How later chapters apply the arc

The next chapters ground the vocabulary in SOLID. "Testability pulls against directness" is close kin to the Dependency Inversion Principle. They also cover the Java underneath every pattern: immutability, testing with JUnit 5 and Mockito, and the records and sealed interfaces that reshape the classic patterns. Each pattern chapter then runs the arc against one pattern and a shipping-domain example, and closes with "When not to use it" and one "Explore with your agent" prompt.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480/9780321700698) (Addison-Wesley): the original definition of a pattern as a recurring problem and the core of its solution, and the catalogue this book's chapters track pattern by pattern.
- Robert C. Martin, [*Agile Software Development: Principles, Patterns, and Practices*](https://www.pearson.com/en-gb/subject-catalog/p/agile-software-development-principles-patterns-and-practices-pearson-new-international-edition/P200000004932/9781292038360) (Prentice Hall): the SOLID principles the next part of this book builds on directly, and an early, explicit treatment of patterns chosen by the forces in front of you rather than applied from memory.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); every citation in this book is a pointer to the source, not a substitute for reading it.
