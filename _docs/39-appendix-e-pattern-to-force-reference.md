---
title: "Pattern-to-force quick reference"
order: 39
layout: tutorial
sectionid: docs
part: appendices
appendix: "E"
description: "A completeness ledger for the whole book: every GoF pattern, SOLID principle, and Java idiom mapped to the force that justifies reaching for it, each linked to the chapter that builds it. Scan it to find the tool for a force in front of you, or to confirm the catalogue is complete."
---

## How to read this ledger

A pattern is a response to a force, a pressure in the design that the plain,
direct solution handles badly. This book teaches patterns in that order:
recognize the force, then name the pattern that answers it. This appendix
inverts the book so you can scan from the force back to the tool.

Each row names a pattern, principle, or idiom, states the force that
justifies reaching for it, and links the chapter that builds it against a
runnable example. The right-hand column is the test: if no force in that
column is present in the code in front of you, applying the pattern is not a
neutral choice, it is the over-engineering the anti-patterns chapter warns
about. Match the mechanism to the force actually present, not to the force a
textbook example implies.

The ledger covers all twenty-three Gang of Four patterns, the five SOLID
principles, and the Java idioms the book treats as first-class patterns in
their own right.

## Principles (SOLID)

The principles are not patterns; they are the forces-and-tradeoffs
vocabulary the patterns are judged against.

| Principle | Reach for it when | Chapter |
|-----------|-------------------|---------|
| Single Responsibility | A class has more than one reason to change, so one concern's edit risks another | [SOLID I]({{ '/docs/03-solid-first-principles/' | relative_url }}) |
| Open/Closed | New behavior should arrive as new code, not edits to working code | [SOLID I]({{ '/docs/03-solid-first-principles/' | relative_url }}) |
| Liskov Substitution | A subtype must be usable anywhere its base is, without surprising a caller | [SOLID I]({{ '/docs/03-solid-first-principles/' | relative_url }}) |
| Interface Segregation | A client is forced to depend on methods it never calls | [SOLID II]({{ '/docs/04-solid-dependency-inversion/' | relative_url }}) |
| Dependency Inversion | High-level policy should not depend on low-level detail; both depend on an abstraction | [SOLID II]({{ '/docs/04-solid-dependency-inversion/' | relative_url }}) |

## Foundational Java idioms

The substrate the rest of the catalogue is built on.

| Idiom | Reach for it when | Chapter |
|-------|-------------------|---------|
| Try-with-resources and `AutoCloseable` | A resource must be released exactly once, on every path out, including an exception | [Resource management]({{ '/docs/05-resource-management/' | relative_url }}) |
| Execute-Around Method | A resource's open/use/close sequence belongs to a library, not to every call site that uses it | [Resource management]({{ '/docs/05-resource-management/' | relative_url }}) |
| Interface with a package-private implementation reached through a static factory | A caller must depend on what a type does, not on the concrete class behind it | [Encapsulation and API design]({{ '/docs/06-encapsulation-and-api-design/' | relative_url }}) |
| Record with a validating compact constructor | A value type's invariants must hold on every instance, with no setter left to break them later | [Encapsulation and API design]({{ '/docs/06-encapsulation-and-api-design/' | relative_url }}) |

## Creational patterns

| Pattern | Reach for it when | Chapter |
|---------|-------------------|---------|
| Factory Method | Which concrete type to build is a decision resolved at runtime, not at the call site | [Factory Method and Abstract Factory]({{ '/docs/12-factory-method-and-abstract-factory/' | relative_url }}) |
| Abstract Factory | A whole family of related objects must be built in a consistent variant together | [Factory Method and Abstract Factory]({{ '/docs/12-factory-method-and-abstract-factory/' | relative_url }}) |
| Builder | An object has many optional parts and construction should read as a sequence of choices | [Builder and Prototype]({{ '/docs/13-builder-and-prototype/' | relative_url }}) |
| Prototype | A new object is best made by copying a configured exemplar rather than rebuilding it | [Builder and Prototype]({{ '/docs/13-builder-and-prototype/' | relative_url }}) |
| Singleton | Exactly one instance must exist, it is effectively immutable, and nothing needs to substitute another | [Singleton and the DI alternative]({{ '/docs/14-singleton-and-the-di-alternative/' | relative_url }}) |

## Structural patterns

| Pattern | Reach for it when | Chapter |
|---------|-------------------|---------|
| Adapter | An existing type has the right behavior behind the wrong interface | [Adapter, Facade, Bridge]({{ '/docs/15-adapter-facade-bridge/' | relative_url }}) |
| Facade | A subsystem's many moving parts should present one small, intentional surface | [Adapter, Facade, Bridge]({{ '/docs/15-adapter-facade-bridge/' | relative_url }}) |
| Bridge | An abstraction and its implementation must vary independently | [Adapter, Facade, Bridge]({{ '/docs/15-adapter-facade-bridge/' | relative_url }}) |
| Decorator | Behavior should be added to one object at runtime without subclassing every combination | [Decorator and Proxy]({{ '/docs/16-decorator-and-proxy/' | relative_url }}) |
| Proxy | Access to an object needs a stand-in for lazy creation, caching, or control | [Decorator and Proxy]({{ '/docs/16-decorator-and-proxy/' | relative_url }}) |
| Composite | Clients should treat a single object and a tree of objects the same way | [Composite, Flyweight, Null Object]({{ '/docs/17-composite-flyweight-null-object/' | relative_url }}) |
| Flyweight | Many objects share most of their state and the duplication is too expensive to keep | [Composite, Flyweight, Null Object]({{ '/docs/17-composite-flyweight-null-object/' | relative_url }}) |
| Null Object | A do-nothing case should remove a null check from every call site | [Composite, Flyweight, Null Object]({{ '/docs/17-composite-flyweight-null-object/' | relative_url }}) |

## Behavioral patterns

| Pattern | Reach for it when | Chapter |
|---------|-------------------|---------|
| Strategy | An algorithm chosen by a caller must be swappable without touching the caller | [Strategy]({{ '/docs/08-strategy/' | relative_url }}) |
| Template Method | A fixed sequence has steps that vary, and the sequence itself must not | [Template Method]({{ '/docs/09-template-method/' | relative_url }}) |
| State | Behavior depends on a current status, and illegal transitions must be refused | [State and finite state machines]({{ '/docs/10-state-and-finite-state-machines/' | relative_url }}) |
| Command | A request must become an object so it can be queued, logged, or undone | [Command, undo, and command queues]({{ '/docs/11-command-undo-and-queues/' | relative_url }}) |
| Observer | Subscribers must react to a change without the subject knowing who they are | [Observer and Mediator]({{ '/docs/18-observer-and-mediator/' | relative_url }}) |
| Mediator | Many objects' direct references to each other have become a tangle | [Observer and Mediator]({{ '/docs/18-observer-and-mediator/' | relative_url }}) |
| Chain of Responsibility | A request should pass along a line of handlers until one takes it | [Chain of Responsibility and Iterator]({{ '/docs/19-chain-of-responsibility-and-iterator/' | relative_url }}) |
| Iterator | A collection's elements must be traversed without exposing its structure | [Chain of Responsibility and Iterator]({{ '/docs/19-chain-of-responsibility-and-iterator/' | relative_url }}) |
| Visitor | A new operation must be added across a fixed set of types without editing each | [Visitor, Memento, and Interpreter]({{ '/docs/20-visitor-memento-interpreter/' | relative_url }}) |
| Memento | An object's state must be captured and restored without exposing its internals | [Visitor, Memento, and Interpreter]({{ '/docs/20-visitor-memento-interpreter/' | relative_url }}) |
| Interpreter | A small, recurring grammar is best expressed as types that evaluate themselves | [Visitor, Memento, and Interpreter]({{ '/docs/20-visitor-memento-interpreter/' | relative_url }}) |

## Idiomatic Java 25 expressions

Patterns that are specific to modern Java rather than general
object-oriented design, several of them retiring a hand-written GoF
implementation outright.

| Idiom | Reach for it when | Chapter |
|-------|-------------------|---------|
| Sealed interface with exhaustive pattern-matching `switch` | A closed, known set of variants should be dispatched with the compiler refusing an unhandled case, with no visitor interface and no `accept()` method | [Records, sealed interfaces, and pattern matching]({{ '/docs/21-records-sealed-pattern-matching/' | relative_url }}) |
| Enum singleton | A single instance must be serialization-safe and reflection-proof with no extra code | [Singleton and the DI alternative]({{ '/docs/14-singleton-and-the-di-alternative/' | relative_url }}) |
| Initialization-on-demand holder idiom | Lazy, thread-safe construction is needed with no synchronized block on the hot path | [Singleton and the DI alternative]({{ '/docs/14-singleton-and-the-di-alternative/' | relative_url }}) |
| Functional interface satisfied by a lambda or method reference | A one-method contract does not need a named implementing class | [Enums, functional interfaces, streams, and Optional]({{ '/docs/22-enums-functional-streams/' | relative_url }}) |
| Stream pipeline | A collection transformation reads better as filter/map/collect than as a hand-written accumulator loop | [Enums, functional interfaces, streams, and Optional]({{ '/docs/22-enums-functional-streams/' | relative_url }}) |
| `Optional` as an explicit answer | A method does not always have an answer, and a caller must be forced to handle the absence | [Enums, functional interfaces, streams, and Optional]({{ '/docs/22-enums-functional-streams/' | relative_url }}) |
| Virtual threads | A workload blocks on I/O far more than it computes, and a platform thread per task would not scale | [Virtual threads]({{ '/docs/24-virtual-threads/' | relative_url }}) |
| `EnumMap` or `Map` dispatch table | A key selects one short handler and a full class hierarchy would be overkill | [Shortcut patterns: dispatch tables and maps]({{ '/docs/26-shortcut-patterns/' | relative_url }}) |

## Patterns in the wild

| Pattern | Reach for it when | Chapter |
|---------|-------------------|---------|
| Resource pool | An expensive-to-create resource must be reused under a bound | [Resource and concurrency patterns]({{ '/docs/25-resource-and-concurrency-patterns/' | relative_url }}) |
| Monitor object | An object's methods must be serialized against concurrent access | [Resource and concurrency patterns]({{ '/docs/25-resource-and-concurrency-patterns/' | relative_url }}) |
| Active object | A method call must be decoupled from the thread that runs it | [Resource and concurrency patterns]({{ '/docs/25-resource-and-concurrency-patterns/' | relative_url }}) |
| CDI managed bean with constructor injection | A single, container-managed instance must receive its collaborators rather than construct them | [Patterns in a real framework: Quarkus and CDI]({{ '/docs/28-patterns-in-the-wild-quarkus-cdi/' | relative_url }}) |
| CDI producer method | A construction decision belongs to the container's composition root, not to the class that needs the result | [Patterns in a real framework: Quarkus and CDI]({{ '/docs/28-patterns-in-the-wild-quarkus-cdi/' | relative_url }}) |
| CDI interceptor binding | Behavior such as logging must wrap a method call without editing the method | [Patterns in a real framework: Quarkus and CDI]({{ '/docs/28-patterns-in-the-wild-quarkus-cdi/' | relative_url }}) |
| CDI event | One bean's change must reach other beans that it has no reference to | [Patterns in a real framework: Quarkus and CDI]({{ '/docs/28-patterns-in-the-wild-quarkus-cdi/' | relative_url }}) |
| Typed configuration (`@ConfigMapping`) | A caller-swappable algorithm should be selected from external configuration, not from a conditional in code | [Patterns in a real framework: Quarkus and CDI]({{ '/docs/28-patterns-in-the-wild-quarkus-cdi/' | relative_url }}) |
| Recognizing an anti-pattern | A pattern is present with no force that calls for it | [Anti-patterns and pattern misuse]({{ '/docs/27-anti-patterns/' | relative_url }}) |

## Changing inherited code

| Technique | Reach for it when | Chapter |
|-----------|-------------------|---------|
| Characterization test | Inherited code must be pinned before it is safe to change | [Seams and the test safety net]({{ '/docs/29-seams-and-the-test-safety-net/' | relative_url }}) |
| Seam (object, extract-interface, subclass-and-override) | Behavior must be altered without editing the logic in place | [Seams and the test safety net]({{ '/docs/29-seams-and-the-test-safety-net/' | relative_url }}) |
| Dependency-breaking techniques | A hard-wired dependency blocks a class from being tested | [Dependency-breaking techniques for Java]({{ '/docs/30-dependency-breaking-techniques/' | relative_url }}) |
| Fowler-then-Feathers workflow | Inherited code needs both a safety net and a design cleanup | [Characterization tests; Fowler vs Feathers]({{ '/docs/31-characterization-and-fowler-vs-feathers/' | relative_url }}) |
| Strangler fig | A legacy component must be replaced incrementally with no big-bang cutover | [Refactor, rewrite, or strangler fig]({{ '/docs/32-refactor-rewrite-or-strangler/' | relative_url }}) |

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480/9780321700698) (Addison-Wesley): the canonical catalogue this ledger indexes, where each pattern's intent, structure, and consequences are set out in full.
- Robert C. Martin, [*Agile Software Development: Principles, Patterns, and Practices*](https://www.pearson.com/en-gb/subject-catalog/p/agile-software-development-principles-patterns-and-practices-pearson-new-international-edition/P200000004932/9781292038360) (Prentice Hall): the SOLID principles the ledger's principle rows condense.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991) (Addison-Wesley): the source for the static-factory, enum-singleton, and holder-idiom techniques the foundational and idiomatic-Java rows apply.
