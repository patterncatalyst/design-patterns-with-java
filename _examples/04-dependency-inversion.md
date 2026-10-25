---
layout: example
order: 4
title: "SOLID II: ISP, DIP, and DI without a framework"
description: A notification policy that depends on role interfaces, not concrete gateways, hand-wired and proven with Mockito.
github_path: examples/04-dependency-inversion
card_eyebrow: "Chapter 4 · SOLID II"
card_title: "ISP, DIP, and DI without a framework"
card_blurb: "A fat notification gateway split into MessageSender, TemplateRenderer, and DeliveryStatusProvider (ISP); a policy that depends on those abstractions, not a concrete gateway (DIP); and constructor injection hand-wired in plain Java, with Mockito standing in for a collaborator."
---

An order-notification system shows interface segregation and dependency
inversion as working code. A fat gateway is split into role interfaces
(`MessageSender`, `TemplateRenderer`, `DeliveryStatusProvider`), so a client
depends only on the capability it uses, and an `SmsGateway` implements the one
role it supports while an `EmailGateway` implements all three. A high-level
`OrderNotificationPolicy` depends on the `MessageSender` abstraction, never a
concrete channel, so the policy links and tests without naming a gateway.
Dependencies are wired by hand in a small composition root, no framework
involved; the chapter names CDI and Spring as the automation that replaces that
wiring later.

The JUnit 5 + AssertJ suite uses **Mockito** to substitute a `MessageSender`
double and verify the policy dispatches through the abstraction, and a
parameterized test confirms two `switch` forms of the delivery auditor agree.

## Build, run, observe

With the toolchain from the prerequisites chapter (JDK 25, Maven):

```sh
cd examples/04-dependency-inversion
mvn -q test          # 26 tests pass
./demo.sh            # hand-wired composition root dispatching a notification
```

The module also runs from the aggregator at the repository root with
`mvn -q -f examples/pom.xml test`.
