---
title: "Mockito reference"
order: 37
layout: tutorial
sectionid: docs
part: appendices
appendix: "C"
description: "A reference for Mockito: creating mocks with mock() and @Mock, stubbing with when/thenReturn and doReturn/doThrow, spy() and the nice-real/strict-mock spectrum, verify() with times/never/InOrder, argument matchers, ArgumentCaptor, and strict stubbing, all on shipping collaborators."
duration: "15 min"
---

## What this appendix covers

Every test double in this book that stands in for a collaborator interface
is built with Mockito. It arrives through the `mockito-core` and
`mockito-junit-jupiter` artifacts the aggregator already pulls in for every
example module, so there is no extra dependency to add: wherever
`org.junit.jupiter.api.Test` already works, `org.mockito.Mockito` does too.
This appendix gathers the mechanics in one place: how to create a mock, how
to stub it, the verification and matcher vocabulary, and the spectrum from a
Mockito mock through a spy to a plain real object.

The testing-stack chapter and the legacy-seams chapter both build and use
mocks as part of a larger argument about design; this appendix is the
reference to return to once that design is in place and the exact syntax is
the only thing left to look up.

## When a mock is the right double

A mock verifies an interaction: that a collaborator was called, how many
times, with which arguments, and in what order relative to other calls. A
fake or a stub verifies state: it stores data and hands it back, and a test
inspects the result afterward without caring how many calls it took to get
there.

A `CarrierGateway` that a shipment-escalation service calls to notify a
carrier is a mock because the thing worth asserting is the call itself:
does the service call `notifyCarrier` exactly once, with the shipment and
the reason it computed, and does it skip the call entirely when the
shipment is still on time? No amount of inspecting a gateway's internal
state answers "did it get called zero times or one time with the wrong
reason." A `ShipmentRepository` that a dispatch workflow writes a finalized
shipment to is a mock for the same reason: the behavior under test is that
`dispatch()` saves exactly the shipment it just priced, not some earlier,
unpriced version of it.

Contrast that with an in-memory rate ledger that just needs to track how
much capacity it has left and hand bookings back on request. That is state,
not interaction, and a real implementation or a hand-written fake is
usually the better fit. The section near the end of this appendix returns
to that line in more detail.

## Creating a mock: `mock()`, `@Mock`, and `MockitoExtension`

`Mockito.mock(SomeType.class)` creates a mock inline, useful for a one-off
double in a single test method:

```java
RateLookup rateLookup = Mockito.mock(RateLookup.class);
```

The idiomatic form for a test class with more than one collaborator is
`@Mock` combined with `MockitoExtension`, which creates every annotated
field before each test method runs and validates framework usage (an
unused stub, a mock passed to the wrong matcher) at the end of the test:

```java
@ExtendWith(MockitoExtension.class)
class DispatchWorkflowTest {

    @Mock
    private CarrierGateway carrierGateway;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private RateLookup rateLookup;

    // test methods use carrierGateway, shipmentRepository, and rateLookup
    // directly; MockitoExtension constructs and resets them per test.
}
```

A mock built either way implements the interface it stands in for and
answers every method with a harmless default, `null` for an object return
type, `0` or `false` for a primitive, an empty `Optional` or empty
collection for those types, until a test stubs a specific call or verifies
one happened. A mock never runs the real implementation of any method; that
is what separates it from a spy, covered next.

## Stubbing a call: `when`/`thenReturn`, `thenThrow`, and `doReturn`/`doThrow`

`when(...).thenReturn(...)` is the everyday stub: tell a mock what to
answer when a specific call arrives.

```java
Money quoted = new Money(new BigDecimal("48.50"));
when(rateLookup.quote(shipment)).thenReturn(quoted);
```

`thenThrow` stubs an exception instead of a value, for a test that proves a
caller reacts correctly when a collaborator fails:

```java
when(rateLookup.quote(shipment)).thenThrow(new CarrierUnavailableException("GRD"));
```

Both forms read naturally for a method with a return type, because
`when(...)` has to call the real method on the mock first to capture which
call is being stubbed, and a `void` method has nothing to pass to `when`.
`doReturn`/`doThrow` invert the order to stub a `void` method instead:

```java
doThrow(new IllegalStateException("shipment already dispatched"))
        .when(shipmentRepository)
        .save(duplicateShipment);
```

The same `doReturn`/`doThrow` form is also the only safe way to stub a
method on a spy, covered next, because `when(spy.someMethod())` on a spy
runs the real method first to figure out what is being stubbed, which
defeats the point of forcing one method's answer without triggering its
real side effects.

## `spy()`: wrapping a real collaborator

A spy wraps a real object and runs its real methods by default; a test
overrides only the one method whose behavior is inconvenient to produce
through the real object directly.

```java
InMemoryShipmentRepository realRepository = new InMemoryShipmentRepository();
ShipmentRepository repositorySpy = spy(realRepository);
doThrow(new DuplicateShipmentException("SHM-9001"))
        .when(repositorySpy)
        .save(argThat(s -> s.shipmentId().equals("SHM-9001")));
```

Every other call to `repositorySpy.save(...)` still runs
`InMemoryShipmentRepository`'s real logic and is visible by inspecting
`realRepository` afterward, the same object underneath the spy. That last
point is what a spy buys that a mock cannot: a test can assert on real
state (`realRepository.findById(...)` returning what was stored) at the
same time it forces one specific call down an exceptional path.

Reach for a spy specifically when most of a collaborator's behavior should
run unmodified and only one hard-to-produce condition needs forcing. Reach
for a full mock when none of the real implementation needs to run, because
every stubbed method still carries the overhead of a real object underneath
it, construction cost, internal state, anything its constructor requires,
that a mock never pays.

## When to prefer a real object instead of a mock or a spy

Not every collaborator needs a test double at all. `java.time.Clock` is the
clearest case in this book: production code calls `Clock.systemUTC()`
exactly once, and a test that needs a specific, repeatable instant reaches
for `Clock.fixed(instant, zoneOffset)`, a real `Clock` instance the JDK
ships, not a Mockito mock of one.

```java
Clock noon = Clock.fixed(Instant.parse("2026-01-10T12:00:00Z"), ZoneOffset.UTC);
ShipmentEscalationService service = new ShipmentEscalationService(noon, carrierGateway);
```

`Clock.fixed` answers the same question a mock would, "what instant does
`now()` return," with less code, no stubbing syntax to get wrong, and a
real implementation whose contract is already tested by the JDK itself.
The same reasoning applies to any immutable value type or pure function a
collaborator calls: a real `BigDecimal`, a real `RateLookup` implementation
with deterministic math and no I/O, or a real in-memory repository used as
a fake rather than wrapped in a spy. A mock earns its cost when the real
object is slow, nondeterministic, or reaches outside the process, a
network call, a clock reading the actual wall clock, a database. A mock of
something that is already fast, deterministic, and side-effect-free adds
stubbing ceremony in exchange for nothing.

## `verify()`: call-count and ordering assertions

`verify(mock)` with no further arguments checks exactly one call happened;
`times`, `never`, and `atLeastOnce` make the cardinality explicit.

```java
verify(carrierGateway, times(1)).notifyCarrier(shipment, "promised delivery window missed by more than 24h");
verify(shipmentRepository, never()).save(any());
verify(rateLookup, atLeastOnce()).quote(any());
```

`never()` turns the absence of a call into the assertion itself. A test
that only checks a returned value would pass even if a bug called
`shipmentRepository.save(...)` before validation ran and threw afterward; a
`verify(shipmentRepository, never())` check catches that bug directly,
because it asserts on the interaction, not just the outcome.

`InOrder` adds a constraint `times`/`never` cannot express on their own:
that calls across one or more mocks happened in a specific sequence, such
as a dispatch workflow that must look up a rate before it ever saves a
shipment.

```java
InOrder dispatchOrder = inOrder(rateLookup, shipmentRepository);
dispatchOrder.verify(rateLookup).quote(shipment);
dispatchOrder.verify(shipmentRepository).save(any());
```

`InOrder` only constrains the calls it names; any other call on either mock
outside that chain is unconstrained unless a separate `verify` covers it.

## Argument matchers

A bare literal argument to a stub or a `verify` call, like
`quote(shipment)` or `save(dispatchedShipment)`, is compared with
`equals()`. A matcher replaces that literal with a rule: `any()` to accept
anything, `eq(x)` to compare by equality explicitly when it needs to
combine with other matchers, and a custom predicate through `argThat` for a
check no built-in matcher expresses directly.

```java
verify(carrierGateway).notifyCarrier(eq(shipment), any());

verify(shipmentRepository).save(argThat(
        s -> s.status() == ShipmentStatus.DISPATCHED && s.shipmentId().equals("SHM-9001")));
```

The rule every Mockito user eventually trips over is all-or-nothing: once a
single argument in a call uses a matcher, every argument in that same call
must use a matcher, never a mix of a matcher and a plain literal.
`notifyCarrier(eq(shipment), "promised delivery window missed")` throws an
`InvalidUseOfMatchersException` at runtime, not a compile error, because
the literal second argument has no matcher wrapping it. The fix is to wrap
every argument, using `eq(...)` for the ones that should still compare by
equality:

```java
verify(carrierGateway).notifyCarrier(eq(shipment), eq("promised delivery window missed"));
```

## `ArgumentCaptor`

A matcher narrows which call satisfies a stub or a verification; it cannot
hand the matched value back for a separate assertion. `ArgumentCaptor` does
exactly that, which matters whenever the object worth asserting on is
constructed inside the method under test and never appears as a literal a
test could write down in advance.

```java
@Captor
private ArgumentCaptor<Shipment> shipmentCaptor;

@Test
void dispatchSavesTheShipmentWithItsQuotedRate() {
    when(rateLookup.quote(shipment)).thenReturn(new Money(new BigDecimal("48.50")));

    workflow.dispatch(shipment);

    verify(shipmentRepository).save(shipmentCaptor.capture());
    assertThat(shipmentCaptor.getValue().quotedRate()).isEqualTo(new Money(new BigDecimal("48.50")));
    assertThat(shipmentCaptor.getValue().status()).isEqualTo(ShipmentStatus.DISPATCHED);
}
```

`@Captor` is the field-injection form, paired with `MockitoExtension` the
same way `@Mock` is; `ArgumentCaptor.forClass(Shipment.class)` is the
inline equivalent for a single test method. Either way, `capture()` goes
inside the `verify(...)` call as the stand-in for the argument, and
`getValue()` (or `getAllValues()` when a method is called more than once)
retrieves what was passed, after the fact, for an assertion a
matcher alone could not make.

## Strict stubbing and `lenient()`

`MockitoExtension` runs with strict stubbing by default: a stub configured
anywhere in a test class that no test method ever triggers fails that test
with an `UnnecessaryStubbingException`, not a silent pass. This catches a
real problem, a stub left behind after the code path that used it was
refactored away, but it also surfaces a false positive whenever a shared
`@BeforeEach` stub is only reached by some of a class's test methods.

```java
@BeforeEach
void setUp() {
    lenient().when(rateLookup.quote(any())).thenReturn(new Money(new BigDecimal("10.00")));
    workflow = new DispatchWorkflow(rateLookup, shipmentRepository, carrierGateway);
}
```

A test method that rejects a shipment before it ever reaches `rateLookup`
never calls the stubbed method, which strict stubbing would otherwise flag
as unused for that one method and fail for the wrong reason, an unused
stub, rather than pass or fail on its own assertion. `lenient()` tells
Mockito this particular stub is allowed to go unused in some tests. Reach
for it exactly in that situation, a stub shared across a fixture that only
some test methods exercise, never as a blanket way to silence the strict-
stubbing check across an entire test class.

## Mock vs. stub vs. fake: choosing the right double

The vocabulary matters because each double fits a different condition, and
reaching for the wrong one either weakens a test or makes it needlessly
expensive to write.

A mock is the right double when the test's assertion is about the call
itself: did `notifyCarrier` get called, with which reason, how many times.
A stub, which in Mockito is just a mock with `when`/`thenReturn` configured
and no `verify` ever checked against it, is the right double when a
collaborator only needs to hand back a canned answer so a test can reach
the behavior it cares about, with no claim made about whether or
how the stub itself was called. A fake, usually a small hand-written
implementation like `InMemoryShipmentRepository`, is the right double when
the test's assertion is about resulting state across more than one call,
save a shipment, then find it by ID, then confirm its status changed, in a
way that chaining `when(...).thenReturn(...)` calls on a mock would make
into a brittle, call-order-dependent setup.

The dividing line is usually this: if the sentence after "this test
checks that..." names a method and an argument, reach for a mock. If it
names a value a collaborator should be holding afterward, reach for a fake
or a real object. A stub sits in between, a mock used purely to get a test
past a collaborator it does not care about, with no `verify` ever
attached to it.

## For deeper coverage

- The official [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html): the complete mock, spy, matcher, `ArgumentCaptor`, and strict-stubbing API this appendix summarizes.
- Gerard Meszaros, [*xUnit Test Patterns: Refactoring Test Code*](https://www.informit.com/store/xunit-test-patterns-refactoring-test-code-9780132800051) (Addison-Wesley): the mock, stub, spy, and fake taxonomy this appendix's closing section draws its vocabulary from.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
