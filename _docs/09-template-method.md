---
title: "Template Method"
order: 9
layout: tutorial
sectionid: docs
part: "selecting-varying-behavior"
description: "Two idiomatic Java 25 ways to keep a processing sequence fixed while its steps vary, taught against a shipment order pipeline that validates, prices, and records an order the same three steps every time, with different content per sales channel: an abstract base with a final skeleton and overridden hooks, and the same skeleton written once as a value whose steps arrive as lambdas."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Say when a fixed sequence with varying steps calls for Template Method rather than Strategy.
- Build the skeleton as a `final` method on an abstract class that calls `protected abstract` hooks.
- Write the same skeleton as a record whose varying steps arrive as lambdas.
- Tell a hook a channel overrides from an invariant the skeleton enforces for every channel.
- Recognize when a single method already does the job.

## Why this matters

A fulfillment system pushes every shipment order through the same three steps, whichever sales channel sold it: check the order against the channel's rule, price it, and write it to the ledger.

| Channel | Validation | Pricing |
| --- | --- | --- |
| Retail web | At most 40 line items | List price |
| Wholesale | At least 50 units | List less a 12% volume discount |
| Marketplace | A marketplace reference is required | List less a 15% commission |

Two of the three steps differ by channel, and all three run in the same order every time. Strategy swaps a whole algorithm for another of the same shape. Template Method keeps one sequence and varies some of its steps; the order never moves.

{% include excalidraw.html name="09-template-method-skeleton" caption="Figure 9.1: OrderProcessingPipeline's fixed, final sequence runs an invariant check and three hooks in order; a channel supplies those hooks either by overriding an abstract class or by handing the pipeline three lambdas." %}

## Pattern: Template Method

### The problem

An `Order` is a record carrying an id, a customer name, a list of `OrderLine` values, and a `marketplaceReference` that stays blank unless the channel needs one:

```java
public record Order(String orderId, String customerName, List<OrderLine> lines, String marketplaceReference) {

    public Order {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId must not be blank");
        }
        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("customerName must not be blank");
        }
        Objects.requireNonNull(lines, "lines");
        lines = List.copyOf(lines);
        marketplaceReference = marketplaceReference == null ? "" : marketplaceReference;
    }
}
```

Three channels each writing their own `process` method would each restate the sequence, and nothing would stop one from pricing before it validates, or skipping the check that an order has at least one line.

### The solution

A **Template Method** fixes the sequence of an algorithm in one method and lets each variant supply only the steps that differ.

The math lives once, in `OrderMath`: `totalQuantity`, `listPriceCents`, and an `applyPercentageReduction` helper that both the wholesale discount and the marketplace commission call. `OrderMath` also owns the precondition every channel shares, `requireAtLeastOneLine`, so an empty order fails before any channel rule runs.

| Form | A channel is | Missing step caught | Fits |
| --- | --- | --- | --- |
| Abstract class, `final` skeleton | A subclass | At compile time | Long-lived channels a team owns |
| Record of functional interfaces | Three lambdas | At construction: the compact constructor rejects a null step | Channels assembled at one call site |

### How Java models it

`OrderProcessingPipeline.process()` is the entry point, and it's `final`:

```java
public abstract class OrderProcessingPipeline {

    public final ProcessingResult process(Order order) {
        OrderMath.requireAtLeastOneLine(order);
        validateChannelRules(order);
        long totalCents = priceOrder(order);
        String ledgerNote = recordToLedger(order, totalCents);
        return new ProcessingResult(
                order.orderId(), channelName(), totalCents, OrderMath.totalQuantity(order), ledgerNote);
    }

    protected abstract String channelName();
    protected abstract void validateChannelRules(Order order);
    protected abstract long priceOrder(Order order);
    protected abstract String recordToLedger(Order order, long totalCents);
}
```

Two modifiers protect the sequence:

- **`final` on `process()`.** No subclass can reorder the steps, skip the precondition or return before the ledger hook runs. A skeleton left open to override gives away the one thing it exists to protect; treat a non-`final` entry point in review as you would a public mutable field.
- **`protected` on the hooks.** A subclass can override them, but no code outside the hierarchy can call one and bypass `process()`.

`RetailWebPipeline` overrides all four hooks:

```java
public final class RetailWebPipeline extends OrderProcessingPipeline {

    private static final int MAX_LINES_PER_ORDER = 40;

    @Override
    protected String channelName() {
        return "retail-web";
    }

    @Override
    protected void validateChannelRules(Order order) {
        if (order.lines().size() > MAX_LINES_PER_ORDER) {
            throw new IllegalArgumentException(
                    "retail-web orders are capped at " + MAX_LINES_PER_ORDER + " line items");
        }
    }

    @Override
    protected long priceOrder(Order order) {
        return OrderMath.listPriceCents(order);
    }

    @Override
    protected String recordToLedger(Order order, long totalCents) {
        return "retail-web:" + order.orderId();
    }
}
```

`WholesalePipeline` and `MarketplacePipeline` override the same four methods with the rules in the table above. None overrides `process()`, and none can. Leave a hook unimplemented and the subclass doesn't compile.

### The functional skeleton

The abstract class costs a file, a class declaration and four overrides per channel. When a channel is closer to configuration, three small behaviors assembled at one call site, the same sequence can be written once as a value. `FunctionalOrderPipeline` is a record holding a channel name and three functional-interface fields:

```java
public record FunctionalOrderPipeline(
        String channelName, ChannelValidator validator, ChannelPricer pricer, ChannelRecorder recorder) {

    public ProcessingResult process(Order order) {
        OrderMath.requireAtLeastOneLine(order);
        validator.validate(order);
        long totalCents = pricer.price(order);
        String ledgerNote = recorder.record(order, totalCents);
        return new ProcessingResult(
                order.orderId(), channelName, totalCents, OrderMath.totalQuantity(order), ledgerNote);
    }
}
```

The body matches the abstract `process()`. The difference is where each step comes from: an override chosen when the caller picked a subclass, or a field set when the caller passed a lambda. A record is implicitly `final`, so no subclass can override its `process()`, and the sequence is as closed as in the abstract form. `ChannelPipelines` builds the three channels as values:

```java
public static FunctionalOrderPipeline wholesale() {
    return new FunctionalOrderPipeline(
            "wholesale",
            order -> {
                int totalUnits = OrderMath.totalQuantity(order);
                if (totalUnits < WHOLESALE_MINIMUM_UNITS) {
                    throw new IllegalArgumentException(
                            "wholesale orders require at least " + WHOLESALE_MINIMUM_UNITS
                                    + " units, got " + totalUnits);
                }
            },
            order -> OrderMath.applyPercentageReduction(OrderMath.listPriceCents(order), WHOLESALE_VOLUME_DISCOUNT),
            (order, totalCents) -> "wholesale:" + order.orderId() + ":discount=" + WHOLESALE_VOLUME_DISCOUNT);
}
```

A fourth channel is a fourth static method; the first three don't change.

### The two forms agree

An identical order gives an equal `ProcessingResult` from either form. Sixty units at 2,500 cents price to 132,000 cents through both `WholesalePipeline` and `ChannelPipelines.wholesale()`, because both call `OrderMath.listPriceCents` and `OrderMath.applyPercentageReduction` with the same arguments. Only where the discount rate and the validation rule are declared differs.

The fixed order holds in both forms: validation, then pricing, then the ledger. An empty order throws before any hook or lambda runs.

## When not to use it

| Situation | Write instead |
| --- | --- |
| Steps vary independently, such as retail validation with wholesale pricing for a one-off case | Separate Strategy values the caller combines; a fixed sequence would guard an order nobody needs. |
| One channel and no second one coming | A method that validates and prices the order. Add the skeleton when a second channel arrives with its own rules for the same steps. |
| The steps' order isn't the fact worth protecting | Plain methods; Template Method's whole premise is the fixed sequence. |

> **🤖 Explore with your agent**
>
> "In `examples/09-template-method`, make `OrderProcessingPipeline.process(Order)` `final` and have it call `OrderMath.requireAtLeastOneLine`, then `validateChannelRules`, `priceOrder` and `recordToLedger`, in that order. Declare the hooks `protected abstract`, never `public`. Keep all price and quantity math in `OrderMath`. Add the same sequence as a `FunctionalOrderPipeline` record of `ChannelValidator`, `ChannelPricer` and `ChannelRecorder`, with channels built as static methods in `ChannelPipelines`. Each subclass and its `ChannelPipelines` counterpart must return an equal `ProcessingResult` for the same `Order`."
>
> Check: `mvn -q -f examples/09-template-method/pom.xml test -Dtest=OrderProcessingPipelineTest,FunctionalOrderPipelineTest,AbstractMethodAndFunctionalEquivalenceTest`; a non-`final` `process()` or a reordered step fails it.

## Build, run, observe

From the repository root, or standalone from `examples/09-template-method`:

```sh
mvn -f examples/pom.xml test
```

```sh
cd examples/09-template-method
mvn test
```

All 35 tests pass. To see both forms process the same three orders:

```sh
examples/09-template-method/demo.sh
```

```
== Retail web channel ==
RetailWebPipeline:             ProcessingResult[orderId=ORD-4001, channel=retail-web, totalCents=9000, lineCount=3, ledgerNote=retail-web:ORD-4001]
ChannelPipelines.retailWeb():  ProcessingResult[orderId=ORD-4001, channel=retail-web, totalCents=9000, lineCount=3, ledgerNote=retail-web:ORD-4001]

== Wholesale channel ==
WholesalePipeline:             ProcessingResult[orderId=ORD-4002, channel=wholesale, totalCents=132000, lineCount=60, ledgerNote=wholesale:ORD-4002:discount=0.12]
ChannelPipelines.wholesale():  ProcessingResult[orderId=ORD-4002, channel=wholesale, totalCents=132000, lineCount=60, ledgerNote=wholesale:ORD-4002:discount=0.12]

== Marketplace channel ==
MarketplacePipeline:           ProcessingResult[orderId=ORD-4003, channel=marketplace, totalCents=10200, lineCount=3, ledgerNote=marketplace:ORD-4003:ref=MKT-998877:commission=0.15]
ChannelPipelines.marketplace():ProcessingResult[orderId=ORD-4003, channel=marketplace, totalCents=10200, lineCount=3, ledgerNote=marketplace:ORD-4003:ref=MKT-998877:commission=0.15]
```

Each channel's two lines agree on every field. Two experiments, each in a scratch copy:

- **Open the skeleton.** Remove `final` from `OrderProcessingPipeline.process()`. Everything still compiles, and the one check that `process()` is `final` fails.
- **Reorder the functional steps.** In `FunctionalOrderPipeline.process()`, call `pricer.price` before `validator.validate`. The call-order check fails; the other 34 pass.

## What you can do now

- Fix a sequence with a `final` entry point and `protected abstract` hooks.
- Keep a shared precondition such as `requireAtLeastOneLine` inside the skeleton, before any hook.
- Write the same skeleton as a record of functional interfaces when channels are configuration.
- Keep shared math in one place, like `OrderMath`, so both forms agree.
- Choose Strategy when steps recombine, and a method when there's one channel.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original Template Method entry, including the hook-and-fixed-sequence framing that `OrderProcessingPipeline` and `FunctionalOrderPipeline` both build on.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 20 covers favoring interfaces over abstract classes, and Item 21 covers designing interfaces for posterity, both background for choosing between the abstract-method and functional forms of the same skeleton.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html): the testing idiom behind `examples/09-template-method`, which checks hook-call order with a recording subclass and with Mockito's `InOrder`.
- The full reading list lives on the [bibliography page]({{ '/bibliography/' | relative_url }}).
