# 14 - Singleton and the DI alternative

A carrier rate table needs exactly one instance: it is read-only after it
loads, nothing about it changes while the process runs, and no caller ever
needs a second, different copy. That is the case where Singleton is the
right call, and this example builds it two idiomatic Java 25 ways before making the
chapter's real argument: most collaborators are not this clean, and a
constructor-injected abstraction beats a global for all of them.

## The domain

- `Money`: a `record` wrapping cents, so a per-kilogram rate times a weight
  stays exact.
- `RateLookup`: the one-method functional interface both singleton forms,
  and any test double, satisfy: `Money baseRateFor(String carrierCode)`.
- `CarrierRates`: the shared, package-private rate data (`GROUND`, `AIR`,
  `FREIGHT`), loaded by both singleton forms so a rate correction lands in
  both idioms at once.

## The two modern singleton idioms

1. **Enum singleton** (`CarrierRateTable`): Joshua Bloch's recommended
   form. The language guarantees exactly one `INSTANCE`; reflection cannot
   forge a second one (`CarrierRateTableTest` proves this directly by
   catching the `IllegalArgumentException` the JVM throws for
   `Constructor.newInstance` on an enum type), and serialization needs no
   `readResolve` method.
2. **Initialization-on-demand holder idiom** (`HolderCarrierRateTable`):
   lazy construction with no `synchronized` keyword anywhere, relying on
   the JVM's class-initialization guarantees instead. A nested static
   `Holder` class is not loaded until `getInstance()` first runs, and the
   JVM serializes concurrent class-loading for you.
   `HolderCarrierRateTableTest` proves the thread-safety claim by racing 16
   threads at `getInstance()` and asserting they all observe the same
   instance.

## The hidden-dependency cost, made concrete

`WarehousePricingJob` reaches directly into `CarrierRateTable.INSTANCE`.
Nothing in its constructor or its one method's signature reveals that
dependency, and `WarehousePricingJobTest` has no way to substitute anything
else: the only thing it can assert is that the job matches the real,
global table.

## The DI alternative

`ShippingQuoteService` takes a `RateLookup` through its constructor instead.
`ShippingQuoteServiceTest` substitutes a Mockito double for it, verifies the
service's output and the exact call the service made, then runs the
identical service unchanged against `CarrierRateTable.INSTANCE`,
`HolderCarrierRateTable.getInstance()`, and a hand-written lambda, proving
the substitutability a global singleton cannot offer.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

This example is standalone-buildable from this directory:

```sh
cd examples/14-singleton
mvn test
```

It is also picked up by the repo root's aggregator:

```sh
mvn -f examples/pom.xml test
```

To see both singletons, the hidden-dependency job, and the DI-based service
run side by side rather than just reading an assertion that says they
differ, run the demo:

```sh
./demo.sh
```

Expected output:

```
== Enum singleton: CarrierRateTable ==
Same instance from both references: true
GROUND base rate: $1.80 per kg

== Holder idiom: HolderCarrierRateTable ==
Same instance from both calls: true
GROUND base rate: $1.80 per kg

== Hidden dependency: WarehousePricingJob reaches into the global ==
Warehouse job GROUND quote: $1.80 per kg

== Dependency injection: ShippingQuoteService ==
Production service, 12kg GROUND shipment: $21.60
Promotional service (fake RateLookup), 12kg shipment: $0.12
```

## Files

```
examples/14-singleton/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run ShippingQuoteDemo
  src/main/java/com/patterncatalyst/designpatterns/ch14/shipping/
    Money.java                    # record: exact integer cents
    RateLookup.java                 # the shared contract (functional interface)
    CarrierRates.java                 # shared rate data, package-private
    CarrierRateTable.java               # enum singleton
    HolderCarrierRateTable.java           # initialization-on-demand holder idiom
    WarehousePricingJob.java                # hidden-dependency cost, made concrete
    ShippingQuoteService.java                 # the DI alternative
    ShippingQuoteDemo.java                      # exercises all four side by side
  src/test/java/com/patterncatalyst/designpatterns/ch14/shipping/
    CarrierRatesTest.java          # shared rate data, immutability
    CarrierRateTableTest.java        # enum identity + reflection-safety proof
    HolderCarrierRateTableTest.java    # holder identity + concurrent-access proof
    WarehousePricingJobTest.java         # no seam to substitute a double
    ShippingQuoteServiceTest.java           # Mockito double + cross-singleton reuse
```
