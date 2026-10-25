---
title: "SOLID II: ISP / DIP and dependency injection without a framework"
order: 4
layout: tutorial
sectionid: docs
part: "principles"
description: "Splitting a fat interface into focused role interfaces, inverting the dependency between a high-level policy and a low-level detail, and wiring the resulting object graph by hand in idiomatic Java with no DI container."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Recognize the fat-interface smell the Interface Segregation Principle
  targets, and split it into focused role interfaces instead.
- State the Dependency Inversion Principle precisely: both the high-level
  policy and the low-level detail depend on an abstraction, and explain why
  that is a stronger claim than "use an interface."
- Build constructor injection with no framework: a high-level policy holds a
  reference to an abstraction, and one composition root wires concrete types
  to it.
- Compare a Classic Java `Optional` result with a Java 25 sealed interface
  for the same decision, and say what the sealed version keeps that the
  `Optional` throws away.
- Name CDI and Spring as the framework alternative to a hand-wired
  composition root, and say what each one automates.
- Recognize when segregating an interface or inverting a dependency solves a
  problem nobody has yet, and say what to write instead.

## Why this matters

An order-fulfillment system has to tell a customer when an order ships, runs
late, or arrives, and the way it reaches that customer is not fixed. An
account on file might take email, a phone number might take SMS, and a
channel that does not exist today will exist eventually. The Single
Responsibility, Open/Closed, and Liskov Substitution principles already
named the general force behind a stable design: something will change, and
the code around it should not have to. Here the thing that changes is the
delivery mechanism, not the decision to notify, and that split motivates the
two principles this chapter adds: Interface Segregation (ISP) and
Dependency Inversion (DIP).

The naive shape for this system reaches for one interface, something like
`NotificationGateway`, with a method for sending, a method for rendering a
template, and a method for checking delivery status, and asks every channel
to implement all three:

```java
public interface NotificationGateway {
    DispatchResult send(OrderEvent event);
    String render(String templateId, OrderEvent event);
    DeliveryState statusOf(String dispatchId);
}
```

An `EmailGateway` built against this has all three capabilities, so
implementing it costs nothing extra. An `SmsGateway` does not: a carrier API
sends a short plain-text message and reports nothing else back. Forced to
implement `NotificationGateway` anyway, `SmsGateway` has two choices, both
bad. It can throw from `render` and `statusOf`, which means every caller
holding a `NotificationGateway` now has to know, out of band, which concrete
type it actually has before calling those two methods safely, defeating the
entire point of programming against an interface. Or it can return a
harmless-looking default, an empty string from `render` or `UNKNOWN` from
`statusOf` regardless of what actually happened, which is worse: the caller
gets an answer that looks valid and is not. A fat interface like this one
manufactures a Liskov violation that segregation makes structurally
impossible, because there is nothing left to stub.

{% include excalidraw.html name="04-dependency-inversion" caption="Figure 4.1: DIP inverts the dependency arrow from a high-level policy through an abstraction, not straight to a concrete gateway. ISP splits that abstraction into role interfaces a concrete type implements only where it has the capability." %}

## The domain: notifying a customer about an order

The example behind this chapter lives in `examples/04-dependency-inversion`.
`OrderEvent` carries an order ID, a `customerContact`, and a `messageBody`:

```java
public record OrderEvent(String orderId, String customerContact, String messageBody) {

    public OrderEvent {
        Objects.requireNonNull(customerContact, "customerContact");
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId must not be blank");
        }
        if (messageBody == null || messageBody.isBlank()) {
            throw new IllegalArgumentException("messageBody must not be blank");
        }
    }
}
```

`customerContact` is allowed to be blank. An order placed with
no contact on file is a real state this domain has to carry, and whether
that state is worth rejecting is a decision for the code that uses
`OrderEvent`, not a rule the record enforces on construction. Two small
enums separate two questions that look similar and are not:
`DispatchStatus` (`SENT` or `FAILED`) answers whether a channel accepted a
message right now, and `DeliveryState` (`PENDING`, `DELIVERED`, `FAILED`, or
`UNKNOWN`) answers what happened to that dispatch later, when someone asks.

## The smell: one interface, every capability

The `NotificationGateway` shown above is worth keeping in view a moment
longer, because the segregated version that replaces it only reads as a fix
once the cost of the fat interface is concrete. Picture a third client,
`DeliveryAuditor`, that only ever checks whether a dispatch landed. Against
`NotificationGateway`, its constructor would still declare a dependency on
`send` and `render`, methods its own logic never calls, and its unit test
would need a fake implementing all three just to satisfy the type. That
mismatch between what a constructor signature declares and what the class
actually uses is the cost Interface Segregation is named for.

## Segregating into role interfaces

Three narrow interfaces replace the fat one, each named for the one thing
it promises:

```java
public interface MessageSender {
    DispatchResult send(OrderEvent event);
}

public interface TemplateRenderer {
    String render(String templateId, OrderEvent event);
}

public interface DeliveryStatusProvider {
    DeliveryState statusOf(String dispatchId);
}
```

`EmailGateway` implements all three, because it has all three
capabilities; nothing about ISP forbids a type from satisfying more than one
role. `SmsGateway` implements exactly `MessageSender`. It never declares
`class SmsGateway implements TemplateRenderer`, so there is no `render`
method to stub, no `statusOf` to fake, and no caller that can accidentally
invoke either one on an SMS channel, because the compiler never offers that
call in the first place. `GatewayRoleSeparationTest` turns that absence into
a behavioral assertion, `assertThat(smsGateway).isNotInstanceOf(TemplateRenderer.class)`,
which fails the moment someone "helpfully" widens `SmsGateway`'s
implements clause without updating the rest of the codebase to match, a
regression a Java compiler already catches for any caller that tries to
treat an `SmsGateway` as a `TemplateRenderer`.

Segregation also changes who needs to depend on what. `DeliveryAuditor` has
no business depending on `MessageSender` or `TemplateRenderer` at all; it
needs exactly `DeliveryStatusProvider`, and its constructor now says so
directly:

```java
public DeliveryAuditor(DeliveryStatusProvider statusProvider) {
    this.statusProvider = Objects.requireNonNull(statusProvider, "statusProvider");
}
```

A test for this class mocks one method on one role interface, not three
methods across a fat one, because the dependency the constructor declares
is an accurate description of what the class needs.

## Dependency inversion: who depends on whom

`OrderNotificationPolicy` is a high-level policy: it knows an order event is
worth notifying a customer about, and it knows nothing else. It depends on
`MessageSender`, never on `EmailGateway` or `SmsGateway`:

{% include codetabs.html langs="Classic Java|Java 25" %}

```java
// Classic Java: Optional says whether the event was sent, nothing more.
// A rejected event and a rejected event for a different reason are
// indistinguishable to the caller.
public final class OrderNotificationPolicy {
    private final MessageSender sender;

    public OrderNotificationPolicy(MessageSender sender) {
        this.sender = Objects.requireNonNull(sender, "sender");
    }

    public DispatchResult notifyCustomer(OrderEvent event) {
        return sender.send(event);
    }

    public Optional<DispatchResult> tryNotifyCustomerOptional(OrderEvent event) {
        if (event.customerContact().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(sender.send(event));
    }
}
```
```java
// Java 25: a sealed NotificationOutcome keeps the rejection reason in the
// value a caller already has to inspect, with no second call or logging
// side channel required to recover it.
public final class OrderNotificationPolicy {
    private final MessageSender sender;

    public OrderNotificationPolicy(MessageSender sender) {
        this.sender = Objects.requireNonNull(sender, "sender");
    }

    public DispatchResult notifyCustomer(OrderEvent event) {
        return sender.send(event);
    }

    public NotificationOutcome tryNotifyCustomer(OrderEvent event) {
        if (event.customerContact().isBlank()) {
            return new NotificationOutcome.Rejected("order event has no customerContact to notify");
        }
        return new NotificationOutcome.Notified(sender.send(event));
    }
}
```

Both columns depend on exactly `MessageSender`; only the shape of the
rejection answer changed. `NotificationOutcome` is a small sealed interface:

```java
public sealed interface NotificationOutcome permits NotificationOutcome.Notified, NotificationOutcome.Rejected {
    record Notified(DispatchResult result) implements NotificationOutcome {}
    record Rejected(String reason) implements NotificationOutcome {}
}
```

A caller consumes it with an exhaustive pattern-matching switch, and the
compiler, not a code reviewer, confirms every branch is handled:

```java
String outcomeDescription = switch (outcome) {
    case NotificationOutcome.Notified notified -> "Notified: " + notified.result();
    case NotificationOutcome.Rejected rejected -> "Rejected: " + rejected.reason();
};
```

Notice what is absent from `OrderNotificationPolicy`: no import of
`EmailGateway`, no `new EmailGateway()` anywhere in this class, and no
compile-time link between this policy and any specific channel's
implementation. That is the concrete, measurable payoff DIP buys. The
dependency that would naturally run from the orchestrating policy down into
plumbing detail is redirected to stop at `MessageSender`, an abstraction the
policy's own package defines, not one the gateway package hands it. The
orchestrating layer becomes testable in isolation from the plumbing layer,
which is exactly what `OrderNotificationPolicyTest` demonstrates: it
substitutes a Mockito `MessageSender` double for the abstraction and never
links against a real gateway. Robert C. Martin's own formulation of DIP is
that high-level modules should not depend on low-level modules; both should
depend on an abstraction, and the abstraction should not depend on the
details. `EmailGateway` implements `MessageSender` rather than being
depended on directly, so the dependency that used to run from policy to
concrete gateway now runs from both of them toward the interface.

## Composing more than one role

`TemplatedOrderNotifier` is a second, independent high-level client that
renders a template before sending. It declares two dependencies because it
needs both capabilities:

```java
public TemplatedOrderNotifier(MessageSender sender, TemplateRenderer renderer, String templateId) {
    this.sender = Objects.requireNonNull(sender, "sender");
    this.renderer = Objects.requireNonNull(renderer, "renderer");
    this.templateId = Objects.requireNonNull(templateId, "templateId");
}

public DispatchResult notifyCustomer(OrderEvent event) {
    String renderedBody = renderer.render(templateId, event);
    return sender.send(event.withMessageBody(renderedBody));
}
```

`TemplatedOrderNotifierTest` mocks `MessageSender` and `TemplateRenderer`
independently, with no fake that has to implement both roles at once; each
mock answers exactly the one method `TemplatedOrderNotifier` calls on it,
and an `ArgumentCaptor` confirms the rendered body, not the original one,
is what actually reaches the sender.

## Dependency injection without a framework

Java ships no classpath scanner and no runtime container in the language
itself. The idiomatic answer for a graph this size is not to build one; it
is to write a composition root, one place that constructs every concrete
type exactly once and passes each abstraction's concrete implementation to
whatever constructor asks for it:

```java
EmailGateway emailGateway = new EmailGateway();
SmsGateway smsGateway = new SmsGateway();

OrderNotificationPolicy smsNotifier = new OrderNotificationPolicy(smsGateway);
TemplatedOrderNotifier emailNotifier = new TemplatedOrderNotifier(emailGateway, emailGateway, "order-delayed");
DeliveryAuditor auditor = new DeliveryAuditor(emailGateway);
```

Three things stand out in those five lines. First, `Main` is the only class
in this example that names a concrete gateway type; every policy class
depends only on the role interface it needs. Second, `emailGateway` is
passed in twice, once as a `MessageSender` and once as a `TemplateRenderer`,
because it is the one concrete type in this graph that satisfies both
roles; `TemplatedOrderNotifier`'s constructor neither knows nor cares that
both references resolve to the same object. Third, there is no container,
no registration step, and no startup phase that resolves a dependency graph
by reflection; the graph is exactly as deep as these five lines make it, and
a reader traces every wire by reading top to bottom once.

CDI, the annotation model Jakarta EE and Quarkus build on, and Spring both
automate graphs like this one: `@Inject` or `@Autowired` on a constructor
parameter, paired with a classpath scan at startup, replaces the five
explicit `new` calls above with a container that resolves the same wiring
by convention. That machinery earns its cost once a graph grows past what a
reader can trace by eye, once bindings need to vary by build or test
configuration from the same registration code, or once object lifetimes
need scoping rules a flat `main` cannot express, one instance per request,
for example. None of that is true of this chapter's five-object graph. A
container also costs something real: another dependency to pin and audit,
an annotation-driven wiring model a new team member has to learn before
reading `main` tells the whole story, and a layer of indirection between
"this type is constructed" and "here is the line that constructs it" that a
debugger's stack trace does not always make obvious. For a graph wired once
at startup and read start to finish, the manual composition root is the
stronger engineering call, and reaching for a container before a graph is
large enough to need one trades a problem you do not have for a dependency
you do.

## A worked audit: describing a closed set of delivery states

`DeliveryAuditor` turns a `DeliveryState` into a human-readable description,
and it is a second place this chapter's codetabs axis shows up, this time on
dispatch rather than on a result type:

{% include codetabs.html langs="Classic Java|Java 25" %}

```java
// Classic Java: a switch statement with an explicit break after every
// branch and a default clause nobody is reminded to update. Adding a
// fifth DeliveryState constant compiles cleanly even if this method is
// never touched; the gap only shows up when a dispatch in that state
// reaches the default branch at runtime.
static String describeStateClassic(DeliveryState state) {
    String description;
    switch (state) {
        case PENDING:
            description = "still in flight";
            break;
        case DELIVERED:
            description = "confirmed delivered";
            break;
        case FAILED:
            description = "confirmed failed";
            break;
        default:
            description = "no record of this dispatch";
            break;
    }
    return description;
}
```
```java
// Java 25: a switch expression with no default. Adding a fifth
// DeliveryState constant without a matching case here is a compile
// error, not a runtime surprise on whichever dispatch reaches the
// missing case first.
static String describeState(DeliveryState state) {
    return switch (state) {
        case PENDING -> "still in flight";
        case DELIVERED -> "confirmed delivered";
        case FAILED -> "confirmed failed";
        case UNKNOWN -> "no record of this dispatch";
    };
}
```

`DeliveryAuditorTest` is a parameterized suite over every `DeliveryState`
constant that checks both methods agree with `auditDelivery`'s own answer,
rather than hand-writing one expected string per state and trusting that
both switches stayed consistent with it. The same role-interface structure
that makes `DeliveryAuditor` easy to unit test, a single mocked
`DeliveryStatusProvider`, is what makes exercising every state in the switch
cheap enough to actually do instead of spot-checking one or two branches.

> **AI sidebar.** Ask an LLM pairing partner to "add dependency injection"
> to a class and it will often reach for Spring or CDI annotations by
> reflex, because that shape dominates the training data for Java DI
> examples, even when nothing in the prompt mentioned a framework and the
> project has none on its classpath. It will also tend to generate one
> interface per concrete class and a field for every collaborator typed as
> the widest possible abstraction, regardless of whether a second
> implementation is in hand or coming. Check the result against the forces
> this chapter named: a role interface is worth adding when two or more
> clients need different capabilities, the way `SmsGateway` and
> `EmailGateway` differ here, or when a test needs to substitute a double
> for something a concrete type makes awkward to exercise, not merely
> because "DI" or "SOLID" appeared in the request. A generated
> `IEmailGateway` with exactly one implementation and no second one planned
> is premature abstraction with an interface-shaped costume on, and a
> generated constructor that reaches for a framework annotation nobody
> asked for is adding a dependency the codebase does not have yet to solve
> a wiring problem five lines of plain Java already solve.

## When not to use it

Both principles in this chapter earn their cost only under specific
conditions, and both stop paying for themselves well before those
conditions arrive.

Dependency Inversion is worth the abstraction when more than one
implementation is in hand or clearly coming, or when a test needs to
substitute a double for something a concrete type would make slow or
awkward to exercise directly, a network call, a clock, a real carrier API.
A service with exactly one notification channel, no second one on any
roadmap, and no test that would benefit from faking it does not need a
`MessageSender` interface; it needs a method, or a class called directly by
name. Introducing an abstraction there adds an interface, a constructor
parameter, and a reader's detour through a contract to find the one
implementation that will ever exist, with nothing on the other side of that
cost to justify it.

Interface Segregation has a matching failure mode in the other direction:
splitting every method of a two-method interface into its own single-method
interface because "narrow is good" produces a maze of one-method types a
reader has to chase across files to understand what a class actually does,
with no client ever benefiting from the extra seams because every client
already needed every method. Segregate when two or more clients have
different needs, the way `SmsGateway` and `EmailGateway` differ in capability
here; stop segregating once every remaining method on an interface is used
by every remaining client, because at that point the interface is not fat,
it is complete, and splitting it further is churn dressed up as a
principle.

A composition root can overstay its welcome too. The five-line graph in
`Main` reads clearly today because five lines is all there is to trace. A
service with forty collaborators, request-scoped lifetimes, and bindings
that differ between a test binary and a production one is the point where
CDI's or Spring's machinery stops being overhead and starts being the
simpler option, because hand-wiring forty constructors by hand is its own
maintenance burden with none of a container's lifecycle management to show
for it. Reach for manual wiring while the graph fits on one screen; reach
for the framework once it stops fitting.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this loop.
From the repository root, the aggregator picks up
`examples/04-dependency-inversion` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/04-dependency-inversion
mvn test
```

Either path runs the same 26 JUnit 5 cases, asserted with AssertJ, including
a Mockito-based suite that substitutes a `MessageSender` test double for
`OrderNotificationPolicy` and a parameterized suite that checks
`DeliveryAuditor`'s two `switch` forms against each other for every
`DeliveryState`. Observed locally on Fedora with Temurin 25:

```
Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see the composition root wire two gateways into three high-level
policies rather than just reading an assertion that says the wiring works,
run the example's own demo script:

```sh
examples/04-dependency-inversion/demo.sh
```

It prints an SMS dispatch, a templated email dispatch, an audit against the
email gateway's delivery log, and finally the rejection path compared
between the Java 25 sealed outcome and its Classic Java `Optional`
counterpart:

```
SMS dispatch (sms-ORD-1001): Your order has shipped.
Email dispatch (email-ORD-1002): rendered and sent
Audit for email-ORD-1002: confirmed delivered

== Java 25: a sealed outcome that keeps the rejection reason ==
Rejected: order event has no customerContact to notify

== Classic Java comparison: Optional<DispatchResult> loses the reason ==
Rejected event present in Optional? false
```

## For deeper coverage

- Robert C. Martin, [*Agile Software Development: Principles, Patterns, and Practices*](https://www.pearson.com/store/p/agile-software-development-principles-patterns-and-practices/P200000003363/9780135974445) (Prentice Hall): the original statement of the Dependency Inversion Principle and the Interface Segregation Principle this chapter builds on, alongside the SRP/OCP/LSP trio covered previously.
- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the book's foundational advice to "program to an interface, not an implementation," which DIP and ISP both sharpen into concrete, testable rules.
- The official [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html) and [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/): the testing idiom behind this chapter's suite, which substitutes a mock for a role interface rather than a fake implementing a fat one.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
