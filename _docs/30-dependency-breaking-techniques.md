---
title: "Dependency-breaking techniques for Java"
order: 30
layout: tutorial
sectionid: docs
part: "changing-inherited-code"
description: "Michael Feathers' dependency-breaking techniques in modern Java, on a hard-wired LegacyShipmentProcessor: Extract Interface with Parameterize Constructor opens a seam around a concrete mail dependency, Extract and Override Factory Method around an embedded surcharge calculator, Subclass and Override Method around a wall-clock read, and Adapt Parameter shrinks a manifest-file parameter to the two values a caller reads."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Apply Extract Interface and Parameterize Constructor together to open a seam in a class that builds its own collaborator, without changing an existing call site.
- Apply Extract and Override Factory Method to move an embedded `new` behind a protected method a test subclass overrides.
- Apply Subclass and Override Method to put a wall-clock read behind an overridable hook, so a test pins an exact answer.
- Apply Adapt Parameter to shrink a dependency on a heavyweight parameter to the values a method reads.
- Recognize when Parameterize Method or a wrapper is the better move.
- Pick the technique that opens the cheapest seam for a given test.

{% include excalidraw.html name="30-dependency-breaking" caption="Figure 30.1: the same dependency, two shapes. Before, LegacyShipmentProcessor constructs and calls a concrete SmtpNotificationGateway directly, with no seam. After Extract Interface and Parameterize Constructor, dispatch depends on a NotificationGateway interface, and either the real production gateway or a test double can sit behind it." %}

## Why this matters

The risk in inherited code rarely sits in the logic. A surcharge calculation or an eligibility rule is a few lines anyone can follow. The risk is in what the logic is wired to: a mail client built with `new` inside a constructor, a `LocalDate.now()` in the middle of a method, a file-parsing type passed in where only two of its values are read. Each was a reasonable choice when written. The cost arrives when someone writes a test and finds that calling one method means a real SMTP connection, the real clock or a real file.

Michael Feathers's *Working Effectively with Legacy Code* doesn't offer one universal fix. It catalogues two dozen small, mechanical techniques, each breaking one kind of dependency. A **seam** is a place where behavior can change without editing the code in that place, and each technique answers one question: for this particular hard-wired dependency, what's the smallest change that lets a test substitute something else?

Smallness is the reason to work from the catalogue. A constructor that builds its own collaborator and one troublesome call inside a method are different problems. "Add dependency injection everywhere" answers both with a far larger change than either needs, touching code that didn't need to move. The four techniques below each keep `LegacyShipmentProcessor`'s behavior and every existing caller, and add only the seam a test needs.

## The catalogue, mapped to idiomatic Java

Modern Java adds no syntax for seams, but it changes how a few techniques read: a record for a small parameter object, a lambda where a hand-written class used to implement an interface, and no default arguments, which makes Parameterize Constructor an overload. The four starred techniques are worked through below and demonstrated in `examples/30-dependency-breaking`.

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

Two of the unstarred entries deserve a line each:

- **Parameterize Method** fits when one method, not the whole object, reads the dependency. Passing a `java.util.function.Supplier<Instant>` into one pricing method costs less than a constructor seam for the whole class.
- **Wrapping** fits when a class can't be touched because something external owns it, such as a third-party type or another team's module. The wrapper forwards every call and implements an interface new code depends on: Extract Interface placed in front of code nobody is allowed to edit.

## Extract Interface and Parameterize Constructor: opening a seam at construction

`LegacyShipmentProcessor` sends a dispatch confirmation to an operations mailbox each time a shipment ships. Its one constructor built a concrete `SmtpNotificationGateway` itself:

```java
// Before: no seam anywhere near the mail dependency.
public LegacyShipmentProcessor(String operationsEmail) {
    this.operationsEmail = operationsEmail;
    this.notificationGateway = new SmtpNotificationGateway("mail.internal.example");
}
```

The shape is common for a good reason. The author needed one way to send a message, an interface for a single implementation would have looked speculative, and nobody was writing a test yet.

Extract Interface comes first and targets the dependency. `NotificationGateway` holds the one method the processor calls, `sendDispatchConfirmation`, out of a surface that in a real system would also carry connection pooling and retry configuration:

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

The interface alone doesn't change how the processor gets its gateway; it makes a second implementation possible. Parameterize Constructor is the second move. Java has no default arguments, so the default becomes an overload, and the original constructor forwards to the new one:

```java
public LegacyShipmentProcessor(String operationsEmail) {
    this(operationsEmail, new SmtpNotificationGateway("mail.internal.example"));
}

public LegacyShipmentProcessor(String operationsEmail, NotificationGateway notificationGateway) {
    this.operationsEmail = Objects.requireNonNull(operationsEmail, "operationsEmail");
    this.notificationGateway = Objects.requireNonNull(notificationGateway, "notificationGateway");
}
```

Every `new LegacyShipmentProcessor("ops@example.com")` still compiles and still sends through the real gateway. Only a test calls the two-argument overload, with a Mockito mock that receives the operations email and a message naming the shipment and its total. Extract Interface answers "what could this dependency be?"; Parameterize Constructor answers "how does a caller supply a different one without breaking the callers who supply none?"

## Extract and Override Factory Method: isolating one embedded `new`

`dispatch` computes a surcharge before it notifies anyone, and it built the calculator inline:

```java
// Before: the concrete calculator is built inline, every time.
long surcharge = new StandardSurchargeCalculator().surchargeCents(shipment.baseCostCents());
```

The math isn't the problem. `dispatch` decides, at the point of use, which concrete type to build, and nothing outside can change that short of editing the line. Extract and Override Factory Method moves the construction into its own method:

```java
protected SurchargeCalculator createSurchargeCalculator() {
    return new StandardSurchargeCalculator();
}
```

`dispatch` calls `createSurchargeCalculator().surchargeCents(...)`, and production still gets `StandardSurchargeCalculator`. A test-only subclass returns a Mockito double, so the dispatched total reflects the stubbed surcharge and the double receives the exact base cost:

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

This seam is narrower than a constructor parameter, and that's the point. The processor gains no parameter, no field and no change to its public surface. One embedded decision moves into a method a subclass can intercept.

## Subclass and Override Method: isolating the wall clock

The express-lane eligibility check read the system clock directly:

```java
// Before: every test run gets a different answer, depending on when it runs.
private boolean isEligibleForExpressLane() {
    DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
    return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
}
```

With `LocalDate.now()` inside, a test can only check the rule for whatever day it runs on, never confirm that a known Tuesday and a known Saturday fall on the right sides. Subclass and Override Method moves the one call into a protected method, unchanged in production:

```java
protected boolean isEligibleForExpressLane() {
    DayOfWeek dayOfWeek = today().getDayOfWeek();
    return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
}

protected LocalDate today() {
    return LocalDate.now();
}
```

A test subclass overrides `today()` with a fixed `LocalDate`, and a known Tuesday, Saturday and Sunday each give an exact answer whatever day the build runs. No mocking framework is needed: overriding one method is cheaper than configuring a clock abstraction for a single read.

> **🤖 Explore with your agent**
>
> "In `examples/30-dependency-breaking`, keep `LegacyShipmentProcessor`'s one-argument constructor and every existing call site. Extract `NotificationGateway` from `SmtpNotificationGateway` and add a two-argument constructor overload that the one-argument constructor forwards to. Move the inline `new StandardSurchargeCalculator()` in `dispatch` into a protected `createSurchargeCalculator()`, and `LocalDate.now()` into a protected `today()`. Change `summarizeManifest` to take a `ManifestSource` interface, with `CarrierManifestFileAdapter` wrapping `CarrierManifestFile`. Don't add a `Clock` parameter or change `CarrierManifestFile`."
>
> Check: `mvn -q -f examples/30-dependency-breaking/pom.xml test -Dtest='LegacyShipmentProcessor*Test'`, and the one-argument constructor's signature unchanged in the diff.

## Adapt Parameter: shrinking a dependency to what it actually reads

`summarizeManifest` took a `CarrierManifestFile`, whose constructor opens and parses a real file:

```java
// Before: every test needs a real file on disk just to call this method.
public String summarizeManifest(CarrierManifestFile manifestFile) {
    return "%s: %d package(s)".formatted(manifestFile.carrierCode(), manifestFile.packageCount());
}
```

The method reads `carrierCode()` and `packageCount()` and nothing else, yet every call needs a file. Adapt Parameter extracts that minimal surface into an interface and wraps the heavyweight type to satisfy it:

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

`summarizeManifest` takes `ManifestSource`, and `CarrierManifestFile` doesn't change. A Mockito double drives the method with no file access, and a real temporary file wrapped in the adapter gives the same summary. The adapter itself is checked against a real file, so it's a faithful stand-in. Adapt Parameter is Extract Interface applied at a call boundary: it shrinks what a method needs from a parameter type it doesn't get to redesign.

## Build, run, observe

From the repository root:

```sh
mvn -f examples/pom.xml test
```

Or standalone:

```sh
cd examples/30-dependency-breaking
mvn test
```

All 21 cases pass, with Mockito doubles for the notification gateway, the surcharge calculator and the manifest source. To see the four techniques against production wiring:

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

The second line of the first block is a processor built with a recording lambda instead of an `SmtpNotificationGateway`. `dispatch` runs the same logic and formats the same message, and nothing in `LegacyShipmentProcessor` knows the difference.

- **Inline the calculator again.** In a scratch copy, change `dispatch` back to `new StandardSurchargeCalculator()`. One case fails: the overridden calculator's 10500-cent total is no longer used.

## When NOT to use it

- **No test needs the seam yet.** A technique pays the moment a real test needs the seam it opens. A `NotificationGateway` extracted when no present or planned test substitutes it is indirection bought for nothing. Name the missing test, find the dependency blocking it, and break only that one.
- **You want a better design.** Feathers is explicit: these techniques get code under test; they don't improve its design. Depending on `NotificationGateway` makes `LegacyShipmentProcessor` more testable, not better organized, and the surcharge rule is as simple and as correct as before. Fowler's named refactorings (Extract Method, Rename, Replace Conditional with Polymorphism) do that later work, made safe by the tests these techniques let you write.
- **A plain argument would do.** Reach for Parameterize Constructor or Parameterize Method first when the dependency enters through a parameter list a caller already controls. Use Extract and Override Factory Method or Subclass and Override Method only when the dependency sits inside a method body with no parameter list to extend. Applying the more flexible technique when a constructor overload already reaches the dependency pays a complexity cost for nothing.

## For deeper coverage

- Michael Feathers, [*Working Effectively with Legacy Code*](https://www.oreilly.com/library/view/working-effectively-with/0131177052/) (Prentice Hall): the source of every technique adapted here, with worked examples across several languages and the seam vocabulary.
- Martin Fowler, [*Refactoring: Improving the Design of Existing Code*](https://martinfowler.com/books/refactoring.html), 2nd ed. (Addison-Wesley): the named-refactoring catalogue to apply once a dependency is broken and a test exists.
- The official [JUnit 5](https://junit.org/junit5/docs/current/user-guide/) and [Mockito](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html) documentation: the testing idiom behind `examples/30-dependency-breaking`, including Mockito doubles that verify an interaction rather than only reading state back.
- The [bibliography page]({{ '/bibliography/' | relative_url }}) lists the full reading list.
