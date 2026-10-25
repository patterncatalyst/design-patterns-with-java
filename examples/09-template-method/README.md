# 09 - Template Method: shipment order processing across channels

A shipment order has to run through the same three steps no matter which
channel sold it: validate it against that channel's rule, price it, and
record it to the ledger. The sequence never changes. What varies is the
content of each step: a retail web order checks a line-item cap and prices
at list, a wholesale order enforces a minimum unit count and applies a
volume discount, a marketplace order requires a reference number and
deducts a commission. This example prices and validates the same shape of
order two idiomatic Java ways, so you can compare the mechanism each one
uses to keep the sequence fixed while letting its steps vary.

## The domain

- `Order`: a `record` with an `orderId`, a `customerName`, a list of
  `OrderLine` values, and a `marketplaceReference` (blank unless the channel
  requires one).
- `OrderLine`: a `record` of a SKU, a quantity, and a unit price in cents.
  Money stays an integer `long` everywhere, never a `double`.
- `ProcessingResult`: a `record` carrying the order id, the channel name,
  the total in cents, the line count, and a ledger note, the one output
  both forms of the pipeline below produce.
- `OrderMath` holds the quantity and pricing math shared by every channel
  and both forms of the pipeline, including the invariant precondition,
  `requireAtLeastOneLine`, that runs before any varying step does.

## The two Template Method expressions

1. **Abstract-method skeleton** (`OrderProcessingPipeline`): `process()` is
   `final`, so no subclass can reorder its steps or skip the precondition.
   Three `protected abstract` hooks, `validateChannelRules`, `priceOrder`,
   and `recordToLedger`, are each overridden once per channel:
   `channel/RetailWebPipeline.java`, `channel/WholesalePipeline.java`, and
   `channel/MarketplacePipeline.java`.
2. **Functional skeleton** (`FunctionalOrderPipeline`): the identical fixed
   sequence written once as a record whose three varying steps arrive as
   `ChannelValidator`, `ChannelPricer`, and `ChannelRecorder` lambdas instead
   of overridden methods. `ChannelPipelines` builds the same three channel
   configurations as values: `retailWeb()`, `wholesale()`, `marketplace()`.

`OrderProcessingDemo.java` runs one order through each channel using both
forms and prints both results so you can see them agree.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

This example is standalone-buildable from this directory:

```sh
cd examples/09-template-method
mvn test
```

It is also picked up by the repo root's aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 35 JUnit 5 cases (AssertJ assertions, Mockito for
the functional form's collaborators), including a suite that asserts the
abstract-method and functional pipelines produce an identical
`ProcessingResult` for an identical order, not just that each one runs
without throwing.

To see both forms process the same three orders side by side rather than
just reading an assertion that says they agree, run the demo:

```sh
./demo.sh
```

Expected output:

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

Notice each abstract-method pipeline and its functional counterpart agree
on every field for the same order: one fixed sequence, two ways to supply
the steps that vary.
```

The abstract-method pipeline and its functional counterpart quote identical
totals, line counts, and ledger notes for every channel. That agreement is
the shared `OrderMath` and the identical fixed sequence showing up as an
observable fact at the terminal, not just as a passing test.

## Files

```
examples/09-template-method/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run OrderProcessingDemo
  src/main/java/com/patterncatalyst/designpatterns/ch09/shipping/
    OrderLine.java                # record: sku, quantity, unitPriceCents
    Order.java                     # record: orderId, customerName, lines, marketplaceReference
    ProcessingResult.java           # record: the one output of either pipeline form
    OrderMath.java                   # shared quantity/pricing math + the invariant precondition
    OrderProcessingPipeline.java      # abstract-method form: final skeleton, protected abstract hooks
    ChannelValidator.java              # functional form: the varying validation step
    ChannelPricer.java                  # functional form: the varying pricing step
    ChannelRecorder.java                 # functional form: the varying recording step
    FunctionalOrderPipeline.java          # functional form: the fixed sequence as a record of lambdas
    ChannelPipelines.java                  # functional form: the three channel configurations
    OrderProcessingDemo.java                # exercises both forms across all three channels
    channel/
      RetailWebPipeline.java                # abstract-method form: retail web channel
      WholesalePipeline.java                 # abstract-method form: wholesale channel
      MarketplacePipeline.java                # abstract-method form: marketplace channel
  src/test/java/com/patterncatalyst/designpatterns/ch09/shipping/
    OrderLineTest.java              # value validation edge cases
    OrderTest.java                   # value validation + defensive copy
    OrderMathTest.java                # shared math + invariant precondition
    OrderProcessingPipelineTest.java   # fixed hook order, precondition-first, process() is final
    FunctionalOrderPipelineTest.java    # fixed order via Mockito InOrder, precondition-first
    ChannelPipelinesTest.java            # functional-form channel behavior
    AbstractMethodAndFunctionalEquivalenceTest.java # both forms agree per channel
    channel/
      RetailWebPipelineTest.java         # retail channel behavior
      WholesalePipelineTest.java          # wholesale channel behavior
      MarketplacePipelineTest.java         # marketplace channel behavior
```
