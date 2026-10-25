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

- Name the force Template Method answers: a sequence of steps is fixed, a
  few of those steps vary by context, and the sequence itself has to stay
  out of reach of whatever supplies the varying content.
- Build a Template Method as a `final` skeleton method on an abstract class
  that calls `protected abstract` hooks, and explain why `final` on the
  entry point and `protected` on the hooks are the two choices that protect
  the sequence.
- Write the identical skeleton as a plain Java value instead of a class
  hierarchy, with the varying steps supplied as functional-interface
  lambdas, and prove both forms produce the same result for the same input.
- Tell a hook from an invariant: a step a channel overrides versus a
  precondition the skeleton enforces for every channel without exception.
- Read a JUnit 5 and Mockito test suite that asserts a fixed call order
  directly, for both forms, not just a finished result that happens to look
  right.
- Recognize when Template Method's base class, or its functional
  equivalent, is solving a problem a single method already solves, and say
  what to write instead.

## Why this matters

A fulfillment system has to push every shipment order through the same
three steps no matter which sales channel sold it: check the order against
that channel's rule, price it, and write it to the ledger. A retail web
order checks a line-item cap and prices at list. A wholesale order enforces
a minimum unit count before it applies a contracted volume discount. A
marketplace order requires the marketplace's own reference number before it
deducts that marketplace's commission. The rules differ across channels
inside two of the three steps, and all three steps run in the same order
every single time.

That is the shape this book has been calling a force since its opening
chapter: the sequence is fixed, the content of a few steps inside it
varies, and the two must not be allowed to mix. Template Method is the Gang
of Four's name for resolving it: one method owns the sequence, and each
step that varies becomes a separate piece of behavior supplied per variant.
The idea sits close to Strategy, and the two do solve related forces, but
they solve different halves of the same problem. Strategy swaps an entire
algorithm for another one of the same shape, a weight-tiered rate for a
flat rate. Template Method keeps one sequence and varies a handful of its
steps while the order they run in never moves. A checkout choosing a
pricing algorithm wants Strategy; an order pipeline whose three steps
always run in the same order, with two of them doing different work per
channel, wants Template Method.

Java gives you two ways to build that skeleton, and the choice between them
is itself worth training. An abstract class with a `final` entry point and
`protected abstract` hooks is the classic expression: each channel gets a
subclass, and the compiler refuses to compile a channel that leaves a hook
unimplemented. A plain value whose three varying steps are passed in as
functional-interface arguments gets you the same fixed sequence with no
class hierarchy at all: a channel is a set of three lambdas, not a file.
Neither form is "the" Template Method in this book's catalogue; the forces
that push toward one or the other, how many channels exist today, whether a
new one shows up as a subclass or a factory call, are what this chapter
trains.

{% include excalidraw.html name="09-template-method-skeleton" caption="Figure 9.1: OrderProcessingPipeline's fixed, final sequence runs an invariant check and three hooks in order; a channel supplies those hooks either by overriding an abstract class or by handing the pipeline three lambdas." %}

## The domain: one order, three channels

The example behind this chapter lives in `examples/09-template-method`. An
`Order` is a record carrying an id, a customer name, a list of `OrderLine`
values, and a `marketplaceReference` that stays blank unless the channel
requires one:

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

The pricing and quantity math lives in exactly one place, `OrderMath`, the
same shared-core move the shipping-rate chapter used for its three pricing
strategies: `totalQuantity`, `listPriceCents`, and a single
`applyPercentageReduction` helper that both the wholesale discount and the
marketplace commission call. Keeping that math in one utility means an
identical order totals identically no matter which channel, and which form
of pipeline, processed it, the exact claim this chapter's equivalence test
checks rather than assumes. `OrderMath` also owns the one precondition
every channel shares, `requireAtLeastOneLine`, so an order with no line
items fails before any channel-specific rule runs.

## The abstract-method skeleton

`OrderProcessingPipeline.process()` is the entry point every caller uses,
and it is `final`:

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

Four things happen in `process()`, always in this order: the shared
precondition runs, the channel's validation hook runs, the channel's
pricing hook runs, and the channel's recording hook runs, with the result
assembled from all four. A caller holding any concrete pipeline through
this base class, a `RetailWebPipeline`, a `WholesalePipeline`, a
`MarketplacePipeline`, gets the exact same four-step sequence every time.
Declaring `process()` `final` is what makes that a guarantee rather than a
convention: no subclass can override the entry point, reorder the steps,
skip the precondition, or return early before the recording hook runs. A
Template Method whose entry point is left open to override has given away
the one thing it exists to protect, and a reviewer should treat a
non-`final` skeleton method the same way they would treat a public mutable
field on an otherwise well-encapsulated class.

The four hooks are `protected`, not `public`. A `protected` hook can be
overridden by a subclass, which is the capability a channel needs, without
exposing a way to call the hook from outside the class hierarchy that
skips `process()` entirely. `RetailWebPipeline` overrides all four:

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

`WholesalePipeline` overrides the identical four methods with different
content: `validateChannelRules` enforces a fifty-unit minimum before any
pricing happens, and `priceOrder` applies a twelve percent volume discount
through the same `OrderMath.applyPercentageReduction` the marketplace
channel also calls. `MarketplacePipeline` requires a non-blank
`marketplaceReference` in its validation hook and deducts a fifteen percent
commission in its pricing hook. None of the three subclasses overrides
`process()`, and none of them can; the fixed sequence is the one thing
every channel inherits without having to restate it.

`examples/09-template-method`'s test suite proves the fixed order directly
rather than inferring it from reading the source twice. `RecordingPipeline`
is a test-only subclass that logs each hook call into a list:

```java
@Test
void callsHooksInTheFixedSkeletonOrder() {
    RecordingPipeline pipeline = new RecordingPipeline();

    pipeline.process(SAMPLE_ORDER);

    assertThat(pipeline.calls)
            .containsExactly("validateChannelRules", "priceOrder", "recordToLedger");
}
```

A second test confirms the invariant precondition runs before any hook at
all: given an order with no line items, `process()` throws and
`RecordingPipeline`'s call log stays empty, because the exception unwinds
before `validateChannelRules` is ever reached. A third test reaches for
reflection to check the claim this chapter makes about `final` directly,
rather than trusting a reader to take the keyword's effect on faith:

```java
@Test
void processIsDeclaredFinalSoNoChannelCanOverrideTheSequence() throws NoSuchMethodException {
    Method process = OrderProcessingPipeline.class.getMethod("process", Order.class);

    assertThat(Modifier.isFinal(process.getModifiers())).isTrue();
}
```

## The functional skeleton

`OrderProcessingPipeline` buys a channel a compile-time guarantee: leave a
hook unimplemented and the subclass does not compile. That guarantee costs
a file, a class declaration, and four overridden methods per channel,
ceremony that is worth paying when a channel is a long-lived type a team
owns. When a channel is closer to a configuration, three narrow pieces of
behavior assembled at a single call site, the identical fixed sequence can
be written once as a value instead. `FunctionalOrderPipeline` is a record
holding a channel name and three functional-interface fields:

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

Compare this `process()` against the abstract class's `process()` line by
line and they read the same way: the shared precondition first, then three
calls in the same fixed order, then the same `ProcessingResult`
constructed from the same four values. The difference is where each step
comes from. The abstract class resolves `validateChannelRules(order)`
through an override chosen at construction time, when a caller decided
which subclass to instantiate. `FunctionalOrderPipeline` resolves
`validator.validate(order)` through a field assigned at construction time,
when a caller decided which lambda to pass in. Both resolve to the same
shape of call, one indirection to reach channel-specific behavior, from two
different places a channel's content can live.

`ChannelPipelines` builds the same three channels `RetailWebPipeline`,
`WholesalePipeline`, and `MarketplacePipeline` express as classes, as
values instead:

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

No new class exists anywhere in `ChannelPipelines.wholesale()`, only three
lambdas and a constructor call, and adding a fourth channel means adding a
fourth static method that builds a fourth value, never touching the three
that came before it. The test suite proves the functional form keeps the
same two guarantees the abstract class makes, using Mockito to stand in for
the three collaborators instead of a recording subclass:

```java
@Test
void callsTheSuppliedStepsInTheFixedSkeletonOrder() {
    FunctionalOrderPipeline pipeline = new FunctionalOrderPipeline("test-channel", validator, pricer, recorder);

    pipeline.process(SAMPLE_ORDER);

    InOrder order = inOrder(validator, pricer, recorder);
    order.verify(validator).validate(SAMPLE_ORDER);
    order.verify(pricer).price(SAMPLE_ORDER);
    order.verify(recorder).record(SAMPLE_ORDER, 1_800L);
}
```

A matching test builds a pipeline from an empty order and asserts
`verifyNoInteractions(validator, pricer, recorder)`, the same
precondition-first guarantee the abstract class's empty-call-log test
checks, proven here against mocked collaborators rather than a logged call
list.

## Proving the two forms agree

Two forms of the same skeleton are only worth teaching side by side if they
produce the same answer, so the example's equivalence suite runs an
identical order through both:

```java
@Test
void wholesaleAgreesAcrossBothForms() {
    Order order = new Order(
            "ORD-4002", "Harborline Hardware Co.", List.of(new OrderLine("SKU-100", 60, 2_500)), "");

    ProcessingResult fromSubclass = new WholesalePipeline().process(order);
    ProcessingResult fromLambdas = ChannelPipelines.wholesale().process(order);

    assertThat(fromSubclass).isEqualTo(fromLambdas);
}
```

`WholesalePipeline` and `ChannelPipelines.wholesale()` price sixty units at
two thousand five hundred cents each down to the same one hundred
thirty-two thousand cents, because both call `OrderMath.listPriceCents` and
`OrderMath.applyPercentageReduction` with the same arguments; the only
thing that differs between the two is where the twelve percent discount
rate and the validation rule are declared. That agreement is not a
coincidence. It is `OrderMath` doing the one job a shared core is supposed
to do: make the two Template Method expressions provably interchangeable
rather than accidentally similar.

> **AI sidebar.** Ask an LLM pairing partner for a Template Method base
> class in Java and a common result leaves the entry point overridable,
> because most generated examples teach "override this method" as the
> whole idea without naming the sequence it is meant to protect. Check the
> result against the two things this chapter trained: is the entry point
> `final`, and are the hooks `protected` rather than `public`, so a
> subclass can supply their content but has no way to call one directly
> from outside the sequence? A generated hierarchy that leaves `process()`
> open has reintroduced the exact failure mode this chapter's `final`
> keyword closes, a channel free to skip the precondition or run the steps
> out of order, and it will pass every demo that only ever calls the base
> class's own sequence through its intended entry point. If the model
> reaches for a hierarchy at all when a single call site with three lambdas
> would do, ask directly whether a `FunctionalOrderPipeline`-shaped value
> serves the channel better than a file.

## When not to use Template Method

Template Method, in either form, pays for itself when a multi-step
sequence is shared by more than one variant and the sequence itself, not
just one
step in isolation, is the fact worth protecting from drift. It stops paying
for itself quickly outside that case.

Steps that vary independently of each other, rather than as a matched set
belonging to one channel, want Strategy instead. If a caller needs to mix a
retail validation rule with a wholesale pricing rule for some one-off
scenario, forcing both through a single pipeline's fixed three-step shape
fights the actual problem; three independent `Strategy` values a caller
composes in whatever combination it needs serves that shape better than any
Template Method, abstract or functional, whose entire premise is a fixed
sequence. The test is the one this book keeps returning to: if the order
the steps run in is the fact worth guaranteeing, Template Method is the
right name for the skeleton, abstract-method or functional; if the steps
need to recombine in ways their order does not predict, the skeleton is
guarding an invariant nobody asked for.

A skeleton with a single channel and no second one credibly coming does
not need `OrderProcessingPipeline`, `RetailWebPipeline`, and a one-subclass
hierarchy under it, and it does not need `FunctionalOrderPipeline` either;
it needs a method that validates an order and prices it. Introducing either
form of Template Method ahead of a second channel arriving adds an
abstraction, a hierarchy or a set of functional-interface fields, for a
design decision, protecting the sequence from variation, that has nothing
yet to protect against. The plainer non-pattern here is the one this book
keeps naming: write the method, and reach for the skeleton the day a second
channel shows up with its own validation rule and its own pricing
rule to plug into the same three steps.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this
loop. From the repository root, the aggregator picks up
`examples/09-template-method` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/09-template-method
mvn test
```

Either path runs the same 35 JUnit 5 cases, asserted with AssertJ and
Mockito, including a suite that checks the abstract-method and functional
pipelines against each other directly for every channel rather than
against three separate expected values that happen to match:

```
Tests run: 35, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see both forms process three orders side by side rather than just
reading an assertion that says they agree, run the example's own demo
script:

```sh
examples/09-template-method/demo.sh
```

It prices a two-line retail order, a sixty-unit wholesale order, and a
three-unit marketplace order, each one through both its abstract-method
pipeline and its functional counterpart:

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

Every channel's two lines agree on every field: the same total, the same
line count, the same ledger note. That agreement is `OrderMath` and the
identical fixed sequence showing up as an observable fact at the terminal,
not just as a passing test.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original Template Method entry, including the hook-and-fixed-sequence framing this chapter's `OrderProcessingPipeline` and `FunctionalOrderPipeline` both build on.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 20 covers favoring interfaces over abstract classes, and Item 21 covers designing interfaces for posterity, both background for choosing between this chapter's abstract-method and functional expressions of the same skeleton.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html): the testing idiom behind `examples/09-template-method`'s suite, which asserts a fixed hook-call order directly, through a recording subclass for the abstract-method form and through Mockito's `InOrder` for the functional form.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
