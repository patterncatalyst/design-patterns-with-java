# 34 - Working with AI and LLM tools: a return-eligibility verification gate

A worked verification gate over an AI-drafted Strategy. The prompt behind this
example is specific and forces-based: "a customer's return request varies by
nothing except how many days have passed since delivery, and the
classification rule is owned by the policy team, not by checkout or support
code, so write it as a Strategy with the usual shape." The gate that decides
whether a draft ships is two checks, both required: a behavior-test suite run
against the policy's exact boundary table, and an idiomatic-Java review of the
draft itself.

## The contract (`ReturnEligibilityStrategy`)

One method, `classify(int daysSinceDelivery)`, returning a `ReturnEligibility`:
`FULL_REFUND`, `STORE_CREDIT`, or `INELIGIBLE`. The policy: full refund through
day 29, store credit from day 30 through day 59, no return from day 60 on.
Both lower bounds are inclusive.

## Two drafts

- `DraftedReturnEligibilityStrategy`: the draft that passes the gate. It
  implements the policy's boundaries exactly.
- `PlausibleWrongReturnEligibilityStrategy`: the draft an AI pairing partner
  handed back for the identical prompt. Same interface, same three outcomes,
  same early-return structure. It carries one bug: `<=` instead of `<` on the
  first boundary, so day 30 stays classified as a full refund instead of
  crossing into store credit. A read-only review is unlikely to catch this;
  every other value in a casual spot check still agrees with the policy.

Neither draft fails an idiomatic-review pass on its own: both are small,
readable, use the enum contract directly, and avoid any retired idiom. The
bug here is purely behavioral, which is exactly why a verification gate needs
both checks rather than treating "it looks like clean Java" as sufficient.

## The gate (`VerificationGateTest`)

A parameterized test drives `DraftedReturnEligibilityStrategy` through the
full boundary table (days 0, 29, 30, 59, 60) and passes on every case,
which is what lets it ship. A second test drives
`PlausibleWrongReturnEligibilityStrategy` through the same values: every
boundary except day 30 agrees with the policy, and the off-by-one is caught
at exactly the boundary a test that only checks one comfortable value per
tier would never reach.

## The dollar impact (`RefundImpactTest`, `RefundPolicy`)

`RefundPolicy` turns a classification into an actual refund: a full refund
returns the whole order total, store credit returns eighty percent of it (the
restocking adjustment this retailer's policy applies to a late return), and
an ineligible return returns nothing. On a $200 order filed on day 30, the
drafted strategy credits $160 in store credit; the plausible-wrong strategy
credits a full $200 refund, a $40 overpayment traceable to one comparison
operator.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

This example is standalone-buildable from this directory:

```sh
cd examples/34-ai-verification-gate
mvn test
```

It is also picked up by the repo root's aggregator:

```sh
mvn -f examples/pom.xml test
```

To see the drafted and plausible-wrong strategies classify the same days
side by side, and the dollar amount their disagreement costs, run the demo:

```sh
./demo.sh
```

## Files

```
examples/34-ai-verification-gate/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run VerificationGateDemo
  src/main/java/com/patterncatalyst/designpatterns/ch34/returns/
    ReturnEligibility.java            # the three classification outcomes
    ReturnEligibilityStrategy.java    # the pattern contract
    DraftedReturnEligibilityStrategy.java    # the draft that passes the gate
    PlausibleWrongReturnEligibilityStrategy.java  # the draft the gate rejects
    Money.java                         # exact integer cents
    RefundPolicy.java                   # classification -> dollar refund
    VerificationGateDemo.java            # runs both drafts side by side
  src/test/java/com/patterncatalyst/designpatterns/ch34/returns/
    VerificationGateTest.java      # the gate: accepts the drafted strategy, rejects the wrong one
    RefundImpactTest.java           # the day-30 bug priced in dollars
    MoneyTest.java                   # exact-cents formatting
```
