# 23 - Where Java 25 retires classic idioms

This example is the book's through-line capstone. Four classic idioms, each
covered earlier in this book as the thing you reached for by hand, retired
here by a modern Java 25 feature that gets the same guarantee from the
compiler or the language runtime instead. Every class below is the modern
form. The retired idiom is described in prose, not maintained as a second
implementation.

## The four retirements

1. **Enum singleton retires double-checked locking.**
   `ConfirmationNumberGenerator` is a single-element enum wrapping an
   `AtomicLong` sequence. The JVM constructs `INSTANCE` exactly once and
   guards it without a `synchronized` block anywhere in sight.
   `ConfirmationNumberGeneratorTest` races 32 threads at
   `nextConfirmationNumber` and asserts every result is distinct.

2. **Records retire a hand-written value class and its defensive
   copying.** `ShippingAddress` gets `equals`, `hashCode`, and `toString`
   from the compiler and canonicalizes its country code in a compact
   constructor. `DeliveryManifest` goes one step further: its compact
   constructor calls `List.copyOf` on the incoming confirmation-number
   list, which gets both ends of defensive copying, a copy on the way in
   and an unmodifiable view on the way out, from one line.
   `ShippingAddressTest` and `DeliveryManifestTest` prove equality,
   canonicalization, and that mutating the caller's source list after
   construction cannot change the manifest.

3. **A sealed interface with an exhaustive pattern-matching switch
   retires the double-dispatch Visitor and the `instanceof`-cast chain.**
   `ShipmentEvent` closes its hierarchy to five records
   (`PickedUp`, `InTransit`, `Delayed`, `Delivered`, `Lost`).
   `ShipmentEventNarrator` pattern-matches and deconstructs each record
   directly, including a guarded pattern on `Delayed` that reads the delay
   duration out of the record's own component. Neither switch needs a
   `default` arm; the compiler proves every `permits` case is handled.
   `ShipmentEventNarratorTest` exercises all five event shapes and both
   notification-decision branches on `Delayed`.

4. **`Optional` retires the Null Object and the scattered null check.**
   `PreferredCarrierDirectory.preferredCarrierFor` returns
   `Optional<String>` instead of `null` or a stand-in preference object.
   `ShipmentRouter.carrierToUse` composes past the possible absence with
   `map` and `orElse`, with no null check or Null Object dispatch anywhere
   in its body. `PreferredCarrierDirectoryTest` and `ShipmentRouterTest`
   cover both the present and the absent case.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

This example is standalone-buildable from this directory:

```sh
cd examples/23-idiom-retirement
mvn test
```

It is also picked up by the repo root's aggregator:

```sh
mvn -f examples/pom.xml test
```

Either path runs the same 27 JUnit 5 cases, asserted with AssertJ:

```
Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see all four retirements run side by side rather than just reading an
assertion that says they behave correctly, run the demo:

```sh
./demo.sh
```

Expected output:

```
== Enum singleton: ConfirmationNumberGenerator ==
First confirmation number:  GROUND-100000
Second confirmation number: GROUND-100001

== Record: ShippingAddress + DeliveryManifest ==
Address (canonicalized country code): ShippingAddress[street=500 Dock Street, city=Akron, postalCode=44301, countryCode=US]
Manifest: DeliveryManifest[batchId=BATCH-7, confirmationNumbers=[GROUND-100000, GROUND-100001]]

== Sealed interface + switch: ShipmentEvent ==
picked up at Akron Hub on 2026-10-20T09:00:00Z (notify customer: false)
delayed (weather), expect an extra 180 minutes, reported 2026-10-21T14:00:00Z (notify customer: true)
delivered on 2026-10-22T11:30:00Z, signed by J. Alvarez (notify customer: true)

== Optional: PreferredCarrierDirectory + ShipmentRouter ==
customer-42 routes to:  FREIGHT-ZONE
customer-99 routes to:  GROUND
```

## Files

```
examples/23-idiom-retirement/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run IdiomRetirementDemo
  src/main/java/com/patterncatalyst/designpatterns/ch23/shipping/
    ConfirmationNumberGenerator.java  # enum singleton retiring double-checked locking
    ShippingAddress.java                # record retiring a hand-written value class
    DeliveryManifest.java                 # record retiring defensive copying
    ShipmentEvent.java                      # sealed interface, five event records
    ShipmentEventNarrator.java                # exhaustive pattern-matching switch
    PreferredCarrierDirectory.java              # Optional retiring the Null Object
    ShipmentRouter.java                           # caller composing past Optional
    IdiomRetirementDemo.java                        # exercises all four side by side
  src/test/java/com/patterncatalyst/designpatterns/ch23/shipping/
    ConfirmationNumberGeneratorTest.java   # uniqueness under 32 concurrent callers
    ShippingAddressTest.java                 # equality, canonicalization, validation
    DeliveryManifestTest.java                  # defensive-copy proof both directions
    ShipmentEventNarratorTest.java               # all five event shapes + guard logic
    PreferredCarrierDirectoryTest.java              # present/absent lookup, map copy
    ShipmentRouterTest.java                           # fallback composition via Optional
```
