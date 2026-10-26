---
title: "Dependency-breaking techniques for Java"
order: 30
layout: tutorial
sectionid: docs
part: "changing-inherited-code"
description: "Michael Feathers' dependency-breaking techniques adapted to Java 25, demonstrated on a hard-wired LegacyShipmentProcessor: Extract Interface paired with Parameterize Constructor opens a seam around a concrete mail dependency, Extract and Override Factory Method opens a seam around an embedded surcharge-calculator construction, Subclass and Override Method isolates a wall-clock read, and Adapt Parameter shrinks a heavyweight manifest-file parameter down to the two values a caller actually reads."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Apply Extract Interface and Parameterize Constructor together to open a seam in a class that currently builds its own collaborator, without changing a single existing call site.
- Apply Extract and Override Factory Method to isolate an embedded `new` expression behind a protected method a test subclass can override.
- Apply Subclass and Override Method to isolate a troublesome read, such as the wall clock, behind an overridable hook, so a test can pin an exact answer instead of a range.
- Apply Adapt Parameter to shrink a method's dependency on a heavyweight concrete parameter down to the handful of values it actually reads.
- Recognize Parameterize Method and wrapping as the two techniques to reach for when a dependency enters through a single method rather than through a constructor or an inline expression.
- Decide, for a given piece of inherited code, which dependency-breaking technique opens the cheapest seam for the specific test you are trying to write.

{% include excalidraw.html name="30-dependency-breaking" caption="Figure 30.1: the same dependency, two shapes. Before, LegacyShipmentProcessor constructs and calls a concrete SmtpNotificationGateway directly, with no seam. After Extract Interface and Parameterize Constructor, dispatch depends on a NotificationGateway interface, and either the real production gateway or a test double can sit behind it." %}

## Why this matters

Inherited code earns that name because changing it carries risk nobody fully understands anymore, and the risk rarely comes from the logic itself. A surcharge calculation, an eligibility rule, a notification step, these are usually a few lines any reader can follow in isolation. The risk comes from what the logic is wired to: a concrete mail client constructed with `new` inside a constructor, a direct call to `LocalDate.now()` buried in the middle of a method, a heavyweight file-parsing type passed as a parameter where only two of its values are ever read. None of these wirings is a bug. Every one of them was a reasonable choice when it was written, often the most direct way to say what the code needed to do. The cost shows up later, the day someone needs to write a test and discovers that calling one method on a small class means standing up a real SMTP connection, waiting on the real clock, or reading and writing a real file.

Michael Feathers' *Working Effectively with Legacy Code* names this problem precisely and then does something unusual for a methodology book: it refuses to offer one universal fix. Instead it catalogues two dozen distinct techniques, each one a small, mechanical move that breaks exactly one kind of dependency, chosen because together they cover nearly every shape a hard-wired collaborator can take in an object-oriented codebase. A seam, in Feathers' vocabulary, is a place where behavior can change without editing the code in that place. Extract Interface opens a seam around a concrete type's public surface. Parameterize Constructor opens a seam at the point of construction. Extract and Override Factory Method opens a seam around a single embedded `new` expression. The techniques differ in mechanism, but every one of them answers the same question: given this particular hard-wired dependency, what is the smallest change that lets a test substitute something else in its place?

That smallest-change discipline is the entire argument for working from a reference catalogue instead of inventing a fix from scratch every time. A team facing a constructor that builds its own collaborator and a team facing a single troublesome call buried inside a method body are facing structurally different problems, and the fix that works for one does nothing useful for the other. Reaching for "add dependency injection everywhere" as a blanket answer to both usually means a much larger change than either problem actually requires, touching code that did not need to change and introducing risk the original problem never carried. This chapter's worked subset of four techniques, Extract Interface combined with Parameterize Constructor, Extract and Override Factory Method, Subclass and Override Method, and Adapt Parameter, demonstrates that smallness directly: every one of the four pieces of `LegacyShipmentProcessor` this chapter touches keeps its existing behavior and every one of its existing callers, while gaining exactly the seam a test needs and nothing more.

## The catalogue, mapped to idiomatic Java

Feathers wrote the catalogue against languages where an interface and a `new` expression were close to the only tools available for opening a seam. Java 25 adds no new syntax for seams specifically, but it does change how cheaply a few of these techniques read in practice: a record for a small immutable parameter object, a functional interface satisfied by a lambda instead of a hand-written class, and the complete absence of default arguments, which turns Parameterize Constructor into a Java-specific idiom of its own. The table below carries the full set of techniques this chapter and its Java expression draw from; the four marked with a star are worked through in the sections that follow and demonstrated in `examples/30-dependency-breaking`.

| Technique | What it breaks | Java 25 expression |
|---|---|---|
| Extract Interface★ | A dependent class can only ever be built against one concrete collaborator type | Pull the collaborator's minimal contract into an interface, so a Mockito double or a lambda can implement just that contract |
| Parameterize Constructor★ | A constructor builds its own collaborator internally, with no way for a caller to supply another one | Add a constructor overload that accepts the collaborator, with the original constructor forwarding to it; Java has no default arguments, so the overload itself is the seam |
| Parameterize Method | A single method, not the whole object, reaches for a hard-wired collaborator | Pass the collaborator as a method argument instead of reaching for it internally, often as a functional-interface parameter a caller satisfies with a lambda |
| Extract and Override Factory Method★ | An embedded `new` expression hard-wires which concrete type gets constructed | Move the construction into its own protected method a test subclass overrides |
| Subclass and Override Method★ | Any method with a troublesome implementation, such as a wall-clock read, that a test needs to suppress or replace | Create a test-only subclass overriding just that method; the move underlying several of the other entries |
| Adapt Parameter★ | A method depends on a heavyweight concrete parameter type it only reads a few values from | Extract a minimal interface for just the needed surface; wrap the heavyweight type in an adapter class |
| Wrap/decorate | New code needs a seam in front of a class nobody wants to modify at all | Write a thin wrapper implementing a new interface, forwarding every call to the untouched original, so new code depends on the wrapper instead |

\* Demonstrated in `examples/30-dependency-breaking`.

Parameterize Method and wrapping deserve a word each, since neither gets a full worked example in this chapter. Parameterize Method is the right move whenever a hard-wired dependency is read inside one method rather than captured by the whole object: passing a `java.util.function.Supplier<Instant>` into a single pricing method, for instance, costs less than giving the entire class a constructor seam for a dependency only one method needs. Wrapping is the right move when a class cannot be touched at all, not because it is risky to change but because something external owns it, a third-party library type or a module boundary a team does not control. The wrapper forwards every call unchanged and implements an interface new code depends on instead, which is structurally the same move as Extract Interface except the interface sits in front of code nobody is allowed to edit rather than code somebody is willing to open.

## Extract Interface and Parameterize Constructor: opening a seam at construction

`LegacyShipmentProcessor` sends a dispatch confirmation to an operations mailbox every time a shipment ships. Before this chapter's pass, its single constructor took an operations email address and built its own mail dependency internally, a concrete `SmtpNotificationGateway` with no interface anywhere near it:

```java
// Before: no seam anywhere near the mail dependency.
public LegacyShipmentProcessor(String operationsEmail) {
    this.operationsEmail = operationsEmail;
    this.notificationGateway = new SmtpNotificationGateway("mail.internal.example");
}
```

That shape is common in inherited code for a reason that has nothing to do with bad judgment: the author needed exactly one way to send a message, writing an interface for a single implementation would have looked like speculative generality, and nobody involved was thinking about a test yet. The cost shows up only once someone tries to write one.

Extract Interface is the first move, and it targets the dependency, not the dependent. `NotificationGateway` pulls the one method `LegacyShipmentProcessor` actually calls, `sendDispatchConfirmation`, out of `SmtpNotificationGateway`'s full surface, which in a real system would also expose connection pooling and retry configuration nothing here touches:

```java
public interface NotificationGateway {
    void sendDispatchConfirmation(String operationsEmail, String message);
}

public final class SmtpNotificationGateway implements NotificationGateway {
    private final String smtpHost;

    public SmtpNotificationGateway(String smtpHost) {
        this.smtpHost = Objects.requireNonNull(smtpHost, "smtpHost");
    }

    @Override
    public void sendDispatchConfirmation(String operationsEmail, String message) {
        System.out.printf("[smtp:%s] to=%s \"%s\"%n", smtpHost, operationsEmail, message);
    }
}
```

Extracting the interface alone changes nothing about how `LegacyShipmentProcessor` gets its dependency; it only creates the possibility of a second implementation. Parameterize Constructor is the second move, and in Java it takes a shape Feathers' original C++ and Java examples do not quite need, because Java has no default-argument syntax. A default parameter value in another language becomes a constructor overload in Java, with the original constructor forwarding to the new one:

```java
public LegacyShipmentProcessor(String operationsEmail) {
    this(operationsEmail, new SmtpNotificationGateway("mail.internal.example"));
}

public LegacyShipmentProcessor(String operationsEmail, NotificationGateway notificationGateway) {
    this.operationsEmail = Objects.requireNonNull(operationsEmail, "operationsEmail");
    this.notificationGateway = Objects.requireNonNull(notificationGateway, "notificationGateway");
}
```

Every call site written against the old one-argument constructor, `new LegacyShipmentProcessor("ops@example.com")`, still compiles, and still behaves identically, because the single-argument overload forwards to the same real `SmtpNotificationGateway` the inherited constructor used to build internally. A test, and only a test, calls the two-argument overload directly. `LegacyShipmentProcessorNotificationTest` injects a Mockito mock and verifies `sendDispatchConfirmation` was called with the operations email and a message naming the shipment and its total, then separately confirms the original one-argument constructor still runs end to end against a real gateway with no exception. The pairing of these two techniques names two different questions: Extract Interface answers "what could this dependency be," and Parameterize Constructor, expressed as a Java overload, answers "how does a caller supply a different one without breaking every caller who supplies neither."

## Extract and Override Factory Method: isolating one embedded `new`

`dispatch` computes a surcharge before it notifies anyone, and before this chapter's pass that computation looked like this:

```java
// Before: the concrete calculator is built inline, every time.
long surcharge = new StandardSurchargeCalculator().surchargeCents(shipment.baseCostCents());
```

Nothing about the surcharge math is the problem. The problem is that `dispatch` decides, at the exact point it needs a calculator, which concrete type to build, with no way for a caller or a test to influence that decision short of changing the line itself. Extract and Override Factory Method targets exactly this shape: pull the construction into its own method, and let a subclass override just that method:

```java
protected SurchargeCalculator createSurchargeCalculator() {
    return new StandardSurchargeCalculator();
}
```

`dispatch` now calls `createSurchargeCalculator().surchargeCents(...)` instead of constructing the calculator inline, and every production call path gets the identical `StandardSurchargeCalculator` it always did. `LegacyShipmentProcessorSurchargeTest` defines a small test-only subclass that overrides `createSurchargeCalculator()` to return a Mockito double, then verifies both that `dispatch`'s total reflects the double's stubbed answer and that the double was invoked with the exact base cost `dispatch` passed it:

```java
private static final class OverriddenCalculatorProcessor extends LegacyShipmentProcessor {
    private final SurchargeCalculator calculator;

    OverriddenCalculatorProcessor(NotificationGateway gateway, SurchargeCalculator calculator) {
        super("ops-supervisor@example.com", gateway);
        this.calculator = calculator;
    }

    @Override
    protected SurchargeCalculator createSurchargeCalculator() {
        return calculator;
    }
}
```

The seam this buys is narrower than Extract Interface plus Parameterize Constructor, and that narrowness is the point. `LegacyShipmentProcessor` gained no new constructor parameter, no new field, and no change to its public surface at all. A single embedded decision, which concrete `SurchargeCalculator` to build, moved into a method a subclass can intercept, which is the entire cost Extract and Override Factory Method asks a reader to pay for the seam it opens.

## Subclass and Override Method: isolating the wall clock

The inherited express-lane eligibility check read the system clock directly:

```java
// Before: every test run gets a different answer, depending on when it runs.
private boolean isEligibleForExpressLane() {
    DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
    return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
}
```

That single call to `LocalDate.now()` is the entire reason the method could never be tested with an exact assertion: every run produces a different "today," so a test could only ever check that the rule holds for whatever day it happened to run on, never confirm a known Tuesday and a known Saturday both land on the correct side of the rule. Subclass and Override Method targets exactly this shape: pull the one troublesome call into its own protected method, changing nothing about when it runs or what it returns in production:

```java
protected boolean isEligibleForExpressLane() {
    DayOfWeek dayOfWeek = today().getDayOfWeek();
    return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
}

protected LocalDate today() {
    return LocalDate.now();
}
```

`LegacyShipmentProcessorExpressLaneTest` defines a `FixedDateShipmentProcessor` subclass that overrides `today()` to return a fixed `LocalDate`, then drives `dispatch` through a known Tuesday, a known Saturday, and a known Sunday, asserting the exact boolean each date produces. No test here depends on which day it happens to run on, and none of them needed a mocking framework at all, since overriding one method on a small subclass is cheaper than configuring a clock-abstraction library for a single read.

> **AI sidebar.** Ask an LLM pairing partner to "make this legacy class testable" and a common answer is a full rewrite: inject every dependency through a new constructor, including the clock and the surcharge calculator, and update the class's entire public surface in one change. That answer is not wrong about where a design should eventually land, and it is often the worse first move, because it bundles an untested, higher-risk rewrite with the one thing you were actually trying to achieve, a passing test. Feathers' catalogue exists because the smaller move, Subclass and Override Method here, Extract and Override Factory Method for the calculator, gets the identical seam with a change small enough to review in minutes and with zero existing callers touched. When a generated suggestion reaches for the comprehensive rewrite, ask the question this chapter's catalogue is built around: what is the smallest technique that opens the specific seam this test needs, and does it require touching any call site that does not already know it is being tested?

## Adapt Parameter: shrinking a dependency to what it actually reads

`summarizeManifest` used to take a `CarrierManifestFile` parameter directly, a type whose constructor opens a real file on disk and parses it:

```java
// Before: every test needs a real file on disk just to call this method.
public String summarizeManifest(CarrierManifestFile manifestFile) {
    return "%s: %d package(s)".formatted(manifestFile.carrierCode(), manifestFile.packageCount());
}
```

Every test that wanted to call `summarizeManifest` needed a real file, a cost disproportionate to what the method actually does with the parameter: it reads `carrierCode()` and `packageCount()` and nothing else. Adapt Parameter extracts exactly that minimal surface into its own interface, then wraps the heavyweight type to satisfy it:

```java
public interface ManifestSource {
    String carrierCode();
    int packageCount();
}

public final class CarrierManifestFileAdapter implements ManifestSource {
    private final CarrierManifestFile manifestFile;

    public CarrierManifestFileAdapter(CarrierManifestFile manifestFile) {
        this.manifestFile = Objects.requireNonNull(manifestFile, "manifestFile");
    }

    @Override
    public String carrierCode() {
        return manifestFile.carrierCode();
    }

    @Override
    public int packageCount() {
        return manifestFile.packageCount();
    }
}
```

`summarizeManifest` now takes `ManifestSource` instead of `CarrierManifestFile`, and `CarrierManifestFile` itself is unchanged; nothing about its file-parsing constructor needed to move or disappear. `LegacyShipmentProcessorManifestTest` drives `summarizeManifest` through a Mockito double with no filesystem access at all, and a second test builds a real `CarrierManifestFile` from a temporary file and wraps it in the adapter, confirming the adapted path produces the identical summary the double path does. `CarrierManifestFileAdapterTest` goes one step further and tests the adapter directly against a real file, proof that the adapter is a faithful stand-in and not just a convenient shortcut that happens to pass. Adapt Parameter is the mirror image of Extract Interface applied at a call boundary instead of at a class's own public surface: Extract Interface shrinks what a dependent needs from a collaborator it owns the relationship to, while Adapt Parameter shrinks what a method needs from a parameter it does not get to redesign.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the prerequisites chapter installs; there is no container anywhere in this loop. From the repository root, the aggregator picks up `examples/30-dependency-breaking` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/30-dependency-breaking
mvn test
```

Either path runs the same 21 JUnit 5 cases, asserted with AssertJ, with Mockito supplying the collaborator doubles for the notification gateway, the surcharge calculator, and the manifest source. Observed locally with Temurin 25:

```
Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see all four techniques run against production wiring at the terminal, run the example's own demo script:

```sh
examples/30-dependency-breaking/demo.sh
```

```
== Extract Interface + Parameterize Constructor: LegacyShipmentProcessor ==
[smtp:mail.internal.example] to=ops-supervisor@example.com "Shipment SH-7001 dispatched, total $129.60"
  recorded (no real SMTP call): to=ops-supervisor@example.com "Shipment SH-7002 dispatched, total $86.40"

== Extract and Override Factory Method: surcharge calculator ==
[smtp:mail.internal.example] to=ops-supervisor@example.com "Shipment SH-7003 dispatched, total $108.00"
  live surcharge (8%): 10800 cents
  overridden flat-rate surcharge: 10500 cents

== Subclass and Override Method: express-lane clock ==
  Tuesday dispatch express eligible: true
  Saturday dispatch express eligible: false

== Adapt Parameter: manifest summary ==
  fake source, no file read: FASTFREIGHT: 275 package(s)
  real file via adapter: SLOWHAUL: 48 package(s)
```

The second line of the first block is the chapter's argument in one run: the second processor is constructed with a recording lambda instead of a real `SmtpNotificationGateway`, and `dispatch` runs its identical logic, formatting the identical message, with nothing in `LegacyShipmentProcessor` aware that anything changed.

## When NOT to use it

A dependency-breaking technique is worth applying the moment a real test needs the seam it opens. Applying one before that moment is the same speculative-generality mistake this book warns against everywhere else: a `NotificationGateway` interface extracted from a mail dependency that no test, present or planned, will ever substitute is a layer of indirection bought for nothing. The discipline these techniques share with every pattern in this book's catalogue is forces-first: name the specific test you are trying to write, find the specific dependency blocking it, then reach for the one technique that breaks exactly that dependency, no more.

These techniques also have a real ceiling, and Feathers is explicit about where it sits: they get code under test, they do not improve its design. `LegacyShipmentProcessor` depending on `NotificationGateway` instead of a concrete class is more testable, not better organized; the surcharge rule inside `StandardSurchargeCalculator` is exactly as simple, and exactly as correct, after Extract and Override Factory Method as before it. Once a test exists and a dependency is broken, the named refactorings for improving the design itself, extract method, rename, replace conditional with polymorphism, are separate tools for separate work; this chapter's techniques exist only to make that later work safe, by giving a test something to hold onto while it happens.

Order matters, too. Reach for Parameterize Constructor or Parameterize Method first whenever a dependency enters through a parameter list a caller already controls, since nothing beats a plain argument for simplicity and compile-time safety. Reach for Extract and Override Factory Method or Subclass and Override Method only when the dependency is embedded inside a method body itself, with no parameter list to extend without changing the method's contract for every caller. A smaller change often suffices on its own: if a single test can reach the dependency it needs by calling a constructor overload a caller already uses, applying Subclass and Override Method anyway, purely because it is the more flexible technique, pays a complexity cost the simpler constructor change never needed to.

## For deeper coverage

- Michael Feathers, [*Working Effectively with Legacy Code*](https://www.oreilly.com/library/view/working-effectively-with/0131177052/) (Prentice Hall): the source of every technique this chapter adapts, with the full worked examples across several languages and the seam vocabulary this book's inherited-code part is built on.
- Martin Fowler, [*Refactoring: Improving the Design of Existing Code*](https://martinfowler.com/books/refactoring.html), 2nd ed. (Addison-Wesley): the named-refactoring catalogue to apply once a dependency is broken and a test exists, picking up exactly where this chapter's techniques stop.
- The official [JUnit 5](https://junit.org/junit5/docs/current/user-guide/) and [Mockito](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html) documentation: the testing idiom behind `examples/30-dependency-breaking`'s suite, including the Mockito collaborator doubles that verify an interaction rather than only reading state back.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
