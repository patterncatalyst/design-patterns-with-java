# 04 - SOLID II: ISP / DIP and dependency injection without a framework

An order-fulfillment system has to tell a customer when an order ships, runs
late, or arrives, and the channel it reaches that customer through is not
fixed: email, SMS, and a channel that does not exist yet all need to fit
behind one shape. This example splits a fat `NotificationGateway` into role
interfaces (Interface Segregation), inverts the dependency between a
high-level policy and the concrete gateways (Dependency Inversion), and
wires the resulting object graph by hand in a plain Java composition root,
with no DI container.

## The domain

- `OrderEvent`: a `record` carrying an order ID, a `customerContact` (which
  may legitimately be blank: an order with no contact on file is a real
  state), and a `messageBody`.
- `DispatchResult` / `DispatchStatus`: what a channel hands back immediately
  after a `send` call (`SENT` or `FAILED`).
- `DeliveryState`: what a channel reports later when asked about a dispatch
  (`PENDING`, `DELIVERED`, `FAILED`, `UNKNOWN`), a narrower and later
  question than `DispatchStatus`.

## Interface Segregation: role interfaces instead of one fat gateway

`MessageSender`, `TemplateRenderer`, and `DeliveryStatusProvider` each
declare exactly one capability.

- `EmailGateway` implements all three, because email has all three
  capabilities.
- `SmsGateway` implements only `MessageSender`, because an SMS carrier API
  sends plain text and reports nothing else back. There is no `render` or
  `statusOf` method on `SmsGateway` to stub or fake.

`GatewayRoleSeparationTest` checks that boundary as a runtime fact
(`isNotInstanceOf`), the same thing a Java compiler already enforces at
compile time for any caller that tries to use `SmsGateway` as a
`TemplateRenderer`.

## Dependency Inversion: who depends on whom

`OrderNotificationPolicy`, `TemplatedOrderNotifier`, and `DeliveryAuditor`
are three independent high-level clients. None of them imports or
constructs `EmailGateway` or `SmsGateway` directly:

- `OrderNotificationPolicy` depends on `MessageSender` alone.
- `TemplatedOrderNotifier` depends on `MessageSender` and `TemplateRenderer`,
  because it needs both.
- `DeliveryAuditor` depends on `DeliveryStatusProvider` alone.

`OrderNotificationPolicy` also carries two idiomatic answers to the same
question, whether an event with no customer contact should be rejected:
`tryNotifyCustomerOptional` returns `Optional<DispatchResult>` and loses the
rejection reason, the shape available before sealed interfaces and pattern
matching. `tryNotifyCustomer` returns a sealed `NotificationOutcome`
(`Notified` or `Rejected`) and keeps the reason in the value itself.
`DeliveryAuditor` carries the same before/after contrast for describing a
`DeliveryState`: a Classic Java `switch` statement with a `default` branch,
and a Java 25 `switch` expression with no `default`, where a missed
`DeliveryState` constant is a compile error instead of a runtime surprise.

## Dependency injection without a framework

`Main` is the composition root: the only class in this example that
constructs a concrete `EmailGateway` or `SmsGateway`. Every other class
depends only on the role interface it needs, injected through its
constructor. `emailGateway` is passed into `TemplatedOrderNotifier` twice,
once as a `MessageSender` and once as a `TemplateRenderer`, because it is
the one concrete type here that satisfies both roles; `TemplatedOrderNotifier`
never knows the two references resolve to the same object. CDI (the Jakarta
EE annotation model Quarkus builds on) and Spring both automate graphs like
this one through classpath scanning and `@Inject`/`@Autowired`; this example
hand-wires it instead, to show exactly what that automation
replaces.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

Standalone, from this directory:

```sh
cd examples/04-dependency-inversion
mvn test
```

Also picked up by the repository root's aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 26 JUnit 5 cases, asserted with AssertJ, including
a Mockito-based suite that substitutes a `MessageSender` test double for
`OrderNotificationPolicy` and never links against a real gateway, and a
parameterized suite that checks `DeliveryAuditor`'s Classic Java `switch`
statement and Java 25 `switch` expression against each other for every
`DeliveryState`.

To see the composition root wire two gateways into three policies and
exercise all of them, run the demo:

```sh
./demo.sh
```

Expected output:

```
SMS dispatch (sms-ORD-1001): Your order has shipped.
Email dispatch (email-ORD-1002): rendered and sent
Audit for email-ORD-1002: confirmed delivered

== Java 25: a sealed outcome that keeps the rejection reason ==
Rejected: order event has no customerContact to notify

== Classic Java comparison: Optional<DispatchResult> loses the reason ==
Rejected event present in Optional? false
```

## Files

```
examples/04-dependency-inversion/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run Main
  src/main/java/com/patterncatalyst/designpatterns/ch04/notification/
    OrderEvent.java               # record: orderId, customerContact, messageBody
    DispatchStatus.java            # SENT / FAILED (immediate send outcome)
    DispatchResult.java             # record: status, dispatchId
    DeliveryState.java               # PENDING/DELIVERED/FAILED/UNKNOWN (later query)
    NotificationOutcome.java          # sealed: Notified / Rejected
    MessageSender.java                 # role interface
    TemplateRenderer.java               # role interface
    DeliveryStatusProvider.java          # role interface
    EmailGateway.java                     # implements all three roles
    SmsGateway.java                        # implements MessageSender only
    OrderNotificationPolicy.java            # high-level; depends on MessageSender
    TemplatedOrderNotifier.java              # high-level; depends on two roles
    DeliveryAuditor.java                      # high-level; depends on DeliveryStatusProvider
    Main.java                                  # composition root
  src/test/java/com/patterncatalyst/designpatterns/ch04/notification/
    GatewayRoleSeparationTest.java   # ISP as a runtime fact
    OrderNotificationPolicyTest.java  # Mockito MessageSender double
    TemplatedOrderNotifierTest.java    # Mockito doubles for two roles
    DeliveryAuditorTest.java            # Classic vs Java 25 switch, parameterized
    EmailGatewayTest.java                # gateway behavior
    SmsGatewayTest.java                   # gateway behavior
    NotificationOutcomeTest.java           # sealed-interface behavior
    OrderEventTest.java                     # record validation
```
