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

- Spot the fat-interface smell the Interface Segregation Principle targets and split it into role interfaces.
- State the Dependency Inversion Principle precisely: the high-level policy and the low-level detail both depend on an abstraction, which is a stronger claim than "use an interface."
- Wire constructor injection with no framework: policies hold abstractions and one composition root supplies the concrete types.
- Build a sealed result type that keeps a rejection's reason in the value the caller inspects, and weigh it against `Optional`.
- Say what CDI and Spring automate over a hand-wired composition root, and when that pays.
- Tell when segregating or inverting solves a problem nobody has yet, and what to write instead.

## Why this matters

An order-fulfillment system tells a customer when an order ships, runs late or arrives. The channel isn't fixed: email for one account, SMS for another, and a channel that doesn't exist yet. What changes is the delivery mechanism, not the decision to notify, and that split is what Interface Segregation (ISP) and Dependency Inversion (DIP) handle.

The obvious design is one `NotificationGateway` interface with `send`, `render` and `statusOf`, and every channel implements all three. Email has all three capabilities. An SMS carrier API sends a short plain-text message and reports nothing back, so `SmsGateway` is left with two bad options:

| `SmsGateway` does | Caller gets |
| --- | --- |
| throws from `render` and `statusOf` | must know the concrete type before calling, which defeats the interface |
| returns `""` or `UNKNOWN` | an answer that looks valid and isn't |

Either way the fat interface manufactures a Liskov violation. Segregation removes it, because nothing is left to stub.

{% include excalidraw.html name="04-dependency-inversion" caption="Figure 4.1: DIP inverts the dependency arrow from a high-level policy through an abstraction, not straight to a concrete gateway. ISP splits that abstraction into role interfaces a concrete type implements only where it has the capability." %}

## The domain: notifying a customer about an order

`OrderEvent` carries an order id, a customer contact and a message body:

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

    public OrderEvent withMessageBody(String renderedBody) {
        return new OrderEvent(orderId, customerContact, renderedBody);
    }
}
```

`customerContact` may be blank: an order with no contact on file is a real state, and whether to reject it is the caller's decision, not the record's. Two enums keep two similar questions apart:

| Enum | Answers | Values |
| --- | --- | --- |
| `DispatchStatus` | did the channel accept the message now? | `SENT`, `FAILED` |
| `DeliveryState` | what happened to the dispatch later? | `PENDING`, `DELIVERED`, `FAILED`, `UNKNOWN` |

## The smell: one interface, every capability

Take a third client, `DeliveryAuditor`, which only checks whether a dispatch landed. Against `NotificationGateway` its constructor would still declare `send` and `render`, which it never calls, and its unit test would need a fake implementing all three. A constructor that declares more than the class uses is the cost ISP is named for.

## Segregating into role interfaces

Three narrow interfaces replace the fat one, each named for what it promises:

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

`EmailGateway` implements all three, because it has all three capabilities; ISP doesn't stop a type from filling several roles. `SmsGateway` implements only `MessageSender`, so there's no `render` to stub, no `statusOf` to fake, and no caller can invoke either on an SMS channel because the compiler never offers the call.

`DeliveryAuditor` now depends on exactly what it uses:

```java
public final class DeliveryAuditor {

    private final DeliveryStatusProvider statusProvider;

    public DeliveryAuditor(DeliveryStatusProvider statusProvider) {
        this.statusProvider = Objects.requireNonNull(statusProvider, "statusProvider");
    }

    public String auditDelivery(String dispatchId) {
        DeliveryState state = statusProvider.statusOf(dispatchId);
        return describeState(state);
    }

    static String describeState(DeliveryState state) {
        return switch (state) {
            case PENDING -> "still in flight";
            case DELIVERED -> "confirmed delivered";
            case FAILED -> "confirmed failed";
            case UNKNOWN -> "no record of this dispatch";
        };
    }
}
```

Its test mocks one method on one role interface. The `switch` expression covers all four `DeliveryState` constants with no `default`, so a fifth constant without a case is a compile error: "the switch expression does not cover all possible input values". With a single mocked `DeliveryStatusProvider`, checking every state is cheap, and the tests run one case per constant.

## Dependency inversion: who depends on whom

`OrderNotificationPolicy` is the high-level policy. It knows an order event is worth telling the customer about, and nothing else. It depends on `MessageSender`, never on `EmailGateway` or `SmsGateway`:

```java
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

The class imports no gateway and calls no gateway constructor. Martin's statement of DIP is that high-level modules shouldn't depend on low-level modules; both depend on an abstraction, and the abstraction doesn't depend on the details. The arrow that would run from policy down to plumbing now stops at `MessageSender`, and `EmailGateway` points at the same interface from below. The policy's tests substitute a Mockito `MessageSender` and never link a real gateway.

`tryNotifyCustomer` returns a sealed type rather than `Optional<DispatchResult>`:

```java
public sealed interface NotificationOutcome permits NotificationOutcome.Notified, NotificationOutcome.Rejected {

    record Notified(DispatchResult result) implements NotificationOutcome {}

    record Rejected(String reason) implements NotificationOutcome {}
}
```

| Return type | A rejected event tells the caller |
| --- | --- |
| `Optional<DispatchResult>` | only that nothing was sent |
| `NotificationOutcome` | why, in the value it already has to inspect, with no second call or log to read |

The caller handles it with an exhaustive pattern-matching `switch`, and the compiler confirms both branches are there:

```java
        String outcomeDescription = switch (outcome) {
            case NotificationOutcome.Notified notified -> "Notified: " + notified.result();
            case NotificationOutcome.Rejected rejected -> "Rejected: " + rejected.reason();
        };
```

## Composing more than one role

`TemplatedOrderNotifier` renders a template before sending, so it declares both roles:

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

Its tests mock `MessageSender` and `TemplateRenderer` separately, with no fake that implements both, and an `ArgumentCaptor` confirms that the rendered body, not the original, reaches the sender.

## Dependency injection without a framework

The Java language has no classpath scanner or container. For a graph this size the idiom is a composition root: one place that constructs each concrete type once and passes it to the constructors that ask for its role. In `Main`:

```java
        EmailGateway emailGateway = new EmailGateway();
        SmsGateway smsGateway = new SmsGateway();

        OrderNotificationPolicy smsNotifier = new OrderNotificationPolicy(smsGateway);
        TemplatedOrderNotifier emailNotifier = new TemplatedOrderNotifier(emailGateway, emailGateway, "order-delayed");
        DeliveryAuditor auditor = new DeliveryAuditor(emailGateway);
```

- **`Main` is the only class that names a concrete gateway.** Every policy depends on a role interface.
- **`emailGateway` goes in twice**, as a `MessageSender` and as a `TemplateRenderer`, because it's the one type that fills both roles. `TemplatedOrderNotifier` doesn't know both references are the same object.
- **No registration or reflection step.** The graph is these five lines, read top to bottom.

CDI, the annotation model under Jakarta EE and Quarkus, and Spring automate this wiring: `@Inject` or `@Autowired` on a constructor plus a classpath scan at startup replaces the `new` calls.

| | Composition root | CDI or Spring container |
| --- | --- | --- |
| Pays off when | the graph fits on one screen and is wired once at startup | the graph outgrows what a reader can trace, bindings differ between test and production, or lifetimes need scopes such as one instance per request |
| Costs | constructor calls to maintain by hand | a dependency to pin and audit, a wiring model to learn, and indirection between "this type is built" and the line that builds it |

For this five-object graph the composition root is the better call. A container added before the graph needs one trades a problem you don't have for a dependency you do.

> **🤖 Explore with your agent**
>
> "In `examples/04-dependency-inversion`, add a push-notification channel as a `PushGateway` that can send but can't render templates or report delivery state. Implement only `MessageSender`; don't add `TemplateRenderer` or `DeliveryStatusProvider` to it, and don't create a combined gateway interface. Wire it in `Main` with `new OrderNotificationPolicy(pushGateway)`, keeping `Main` the only class that names a concrete gateway. Use no Spring or CDI annotations. Add JUnit 5 tests with AssertJ for its dispatch id, in the style of `SmsGatewayTest`."
>
> Check: `mvn -q -f examples/04-dependency-inversion/pom.xml test -Dtest='GatewayRoleSeparationTest,OrderNotificationPolicyTest'` still passes, and no policy class imports `PushGateway`.

## When not to use it

- **DIP with one implementation.** The abstraction pays when a second implementation is in hand or clearly coming, or when a test must replace something slow or awkward: a network call, a clock, a carrier API. A service with one channel, no second planned and nothing to fake needs a class called by name, not a `MessageSender` interface.
- **ISP past the point of need.** Splitting a two-method interface into two one-method interfaces because narrow is good gives readers a maze of files and no client a benefit. Segregate when clients need different capabilities, as `SmsGateway` and `EmailGateway` do. Once every client uses every method, the interface is complete, not fat.
- **A composition root past one screen.** Forty collaborators, request-scoped lifetimes and bindings that differ between test and production make a container the simpler option. Wire by hand while the graph fits on one screen.

## Build, run, observe

From the repository root, through the aggregator, or standalone:

```sh
mvn -f examples/pom.xml test
```

```sh
cd examples/04-dependency-inversion
mvn test
```

All 25 tests pass, including Mockito tests that give `OrderNotificationPolicy` a `MessageSender` double and a parameterized test over every `DeliveryState`. Run the demo to see the composition root wire two gateways into three policies:

```sh
examples/04-dependency-inversion/demo.sh
```

It prints an SMS dispatch, a templated email dispatch, an audit against the email gateway's delivery log, and the rejection path through `NotificationOutcome`:

```
SMS dispatch (sms-ORD-1001): Your order has shipped.
Email dispatch (email-ORD-1002): rendered and sent
Audit for email-ORD-1002: confirmed delivered

== Java 25: a sealed outcome that keeps the rejection reason ==
Rejected: order event has no customerContact to notify
```

Two experiments, each in a scratch copy:

- **Widen `SmsGateway`.** Add `DeliveryStatusProvider` to its `implements` clause with a `statusOf` that returns `UNKNOWN`. It compiles, and the role-separation test fails, since SMS can't report delivery.
- **Add a fifth `DeliveryState`.** Add `RETURNED` to the enum. The build stops at `describeState` with "the switch expression does not cover all possible input values".

## For deeper coverage

- Robert C. Martin, [*Agile Software Development: Principles, Patterns, and Practices*](https://www.pearson.com/store/p/agile-software-development-principles-patterns-and-practices/P200000003363/9780135974445) (Prentice Hall): the original statement of the Dependency Inversion Principle and the Interface Segregation Principle this chapter builds on, alongside the SRP/OCP/LSP trio covered previously.
- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the book's foundational advice to "program to an interface, not an implementation," which DIP and ISP both sharpen into concrete, testable rules.
- The official [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html) and [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/): the testing idiom behind this chapter's suite, which substitutes a mock for a role interface rather than a fake implementing a fat one.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
