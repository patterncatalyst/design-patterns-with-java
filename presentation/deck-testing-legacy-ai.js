// deck-testing-legacy-ai.js - "Design Patterns with Java: Testing, Legacy
// Code, and AI" - a standalone companion deck covering the JUnit 5 /
// Mockito / AssertJ testing stack, getting inherited Java under test with
// seams and characterization tests, the refactor-vs-rewrite-vs-strangler
// decision, and working with AI/LLM tools under a verification gate.
//
// Build:  node deck-testing-legacy-ai.js
// Paths:  deck-helpers.js reads DECK_ASSETS (./assets) and DECK_PNG (./png).

"use strict";

const H = require("./deck-helpers.js");
const {
  COLOR, FONT, W, ASSETS,
  newDeck, addFooter, addContentTitle, addBullets, addTwoColBullets,
  addStatusTable, addCaption, addCodeSlide, addDiagramSlide, addSectionDivider, addNotes,
} = H;

const OUT = "design-patterns-with-java-testing-legacy-ai.pptx";
const REV = "v1.0";

const pres = newDeck();
pres.title = "Design Patterns with Java: Testing, Legacy Code, and AI";
let pageNum = 0;

function S() {
  const s = pres.addSlide(); pageNum += 1; addFooter(s, pageNum); return s;
}
function divider(code, title, subtitle, notes) {
  const s = pres.addSlide(); pageNum += 1;
  addSectionDivider(s, code, title, subtitle);
  addNotes(s, notes);
  return s;
}

// =============================================================================
// Cover
// =============================================================================
{
  const s = pres.addSlide();
  pageNum += 1;
  s.background = { color: COLOR.white };
  try { s.addImage({ path: `${ASSETS}/cover-panel.png`, x: 0, y: 0, w: W, h: 7.5 }); } catch (e) {}
  s.addText("DESIGN PATTERNS WITH JAVA", {
    x: 6.00, y: 1.98, w: 6.90, h: 0.34,
    fontFace: FONT.title, fontSize: 14, bold: true, color: COLOR.red,
    charSpacing: 6, align: "left", valign: "middle",
  });
  s.addText("Design Patterns\nwith Java", {
    x: 5.95, y: 2.42, w: 6.95, h: 1.90,
    fontFace: FONT.title, fontSize: 48, bold: true, color: COLOR.ink,
    align: "left", valign: "top",
  });
  s.addText("Testing, Legacy Code, and AI", {
    x: 6.00, y: 4.55, w: 6.70, h: 0.60,
    fontFace: FONT.body, fontSize: 20, italic: true, color: COLOR.caption,
    align: "left", valign: "top",
  });
  s.addText(REV, {
    x: 11.85, y: 5.85, w: 0.95, h: 0.30,
    fontFace: FONT.mono, fontSize: 11, color: COLOR.caption, align: "right", valign: "middle",
  });
  try { s.addImage({ path: `${ASSETS}/logo-candidate-2.png`, x: 11.10, y: 6.80, w: 1.55, h: 0.37 }); } catch (e) {}
  addNotes(s,
    "This deck is for engineers who already know the pattern catalogue and now need to put it into " +
    "practice: proving a pattern works with a real JUnit 5, Mockito, and AssertJ suite, getting code they " +
    "inherited safe to change without a rewrite, and working alongside an AI pairing partner without " +
    "handing it the one judgment call that stays with the engineer. Every example here uses a shipping " +
    "domain: a shipment booking service, a delivery-escalation check, a freight-quote calculator, an " +
    "accessorial fee calculator, a return-eligibility policy. Nothing in this deck is a toy UI sample. The " +
    "arc runs from testing the work, through changing inherited code safely, to working with AI tooling " +
    "under a gate that catches what a casual read misses.");
}

// =============================================================================
// Agenda
// =============================================================================
{
  const s = S();
  addContentTitle(s, "AGENDA", "What this deck covers");
  addTwoColBullets(s, [
    "Testing the work: JUnit 5's lifecycle and parameterized tests, Mockito's mocks and spies, and AssertJ's fluent assertions",
    "Changing inherited code: seams, the dependency-breaking catalogue, Fowler and Feathers combined, refactor, rewrite, or strangler fig",
  ], [
    "Working with AI and LLM tools: naming the forces before prompting, the failure modes, the verification gate",
    { text: "Appendix: the xUnit test-double taxonomy and the test-smell catalogue, both as lookup tables", muted: true },
  ], { fontSize: 17 });
  addNotes(s,
    "Three working sections plus a reference appendix. Testing and changing inherited code are the two " +
    "biggest sections because they are the two skills a pattern catalogue alone never teaches: proving a " +
    "pattern's claim with a real suite, and getting existing code safe to touch before changing it. Working " +
    "with AI and LLM tools treats a model as a fast collaborator whose output still has to pass the same " +
    "gates as anyone else's code. The appendix is pure lookup: the test-double taxonomy and the test-smell " +
    "table, mapped to the JUnit 5, Mockito, and AssertJ stack this deck uses throughout, for after the talk.");
}

// =============================================================================
// SECTION 1 - Testing the work
// =============================================================================
divider("01", "Testing the work", "JUnit 5, Mockito, AssertJ, and the vocabulary behind every pattern's proof",
  "This section is the shared testing foundation the rest of the deck assumes: the JUnit 5 lifecycle and " +
  "parameterized-test styles that prove a component does what it claims, Mockito's mock and spy vocabulary " +
  "for verifying an interaction rather than only a return value, and AssertJ for making a failure read as a " +
  "sentence instead of a traced argument list. Every example from here on uses a real shipping collaborator: " +
  "a shipment validator, a freight cost calculator, a rate ledger with a side effect. None of it is a " +
  "placeholder class. By the end of this section a test suite should read as proof of a specific claim about " +
  "behavior, not boilerplate around an assertion.");

{
  const s = S();
  addContentTitle(s, "TESTING THE WORK · JUNIT 5", "Lifecycle and grouping: @BeforeEach, @AfterEach, @Nested");
  addTwoColBullets(s, [
    "@BeforeEach runs before every @Test in the class, so no test starts from state another test already left behind",
    "@AfterEach runs after every test, pass or fail, which makes it the right place for a post-condition a fatal assertion should still trigger",
    "JUnit 5 constructs a new test instance per @Test method by default (PER_METHOD), so field initializers are a fresh fixture too",
  ], [
    "@Nested groups related cases under their own class, each carrying a @DisplayName",
    "A flat list of fifteen @Test methods carries the same assertions as two or three @Nested groups, but a reader has to parse each method name to reconstruct the grouping @Nested states directly",
    "\"ShipmentValidator, when the shipment breaks a business rule, rejects a non-positive weight\" reads as a specification, not a Java identifier reverse-engineered into English",
  ], { fontSize: 15 });
  addNotes(s,
    "A ledger test's AfterEach is the clearest case for why teardown matters beyond cleanup: a capacity " +
    "invariant deserves checking after every test regardless of what that test itself asserted, and a test " +
    "that tries to overfill the ledger and expects an exception still leaves the ledger in a state worth " +
    "re-checking. Putting that check only at the bottom of each test body means skipping it on exactly the " +
    "test where an early assertion failure already signaled trouble. @Nested pays off once at least two or " +
    "three related cases share one name; organizing a single test into its own nested class is structure for " +
    "its own sake, not grouping that earns its keep.");
}

{
  const s = S();
  addDiagramSlide(s, "TESTING THE WORK · JUNIT 5", "One test, arranged, acted, and asserted two ways", "07-test-anatomy",
    "One JUnit 5 test method acting on a real and a mocked collaborator, with AssertJ and Mockito.verify answering two different questions.");
  addNotes(s,
    "A shipment booking service is sized to need both answers at once: a validator and a cost calculator are " +
    "deterministic and side-effect free, so the test calls them directly and checks the return value with " +
    "AssertJ; a rate ledger writes to storage, so the test mocks it and checks the interaction with " +
    "Mockito.verify. Those are two different facts about the same method call under test, and conflating " +
    "them, mocking a pure function or only checking a return value from a collaborator with a side effect, " +
    "is the single most common testing mistake this section exists to train out. The split is not an accident " +
    "of this example's design; it is the shape every real test suite in this book's domain has to recognize " +
    "before choosing a tool.");
}

{
  const s = S();
  addContentTitle(s, "TESTING THE WORK · JUNIT 5", "@ParameterizedTest: three sources, three shapes");
  addBullets(s, [
    "@ValueSource fits a short list of primitive values checked against the same assertion, such as a handful of non-positive weights",
    "@EnumSource fits a closed set of constants checked the same way; a fifth cargo class later extends the test automatically, with no list to maintain by hand",
    "@MethodSource fits a case where each input needs more than one value and a name: a table of malformed shipments, each paired with the exact rejection reason expected",
    "A fourth rule means a fourth row in the table, not a fourth copy of the assertion logic, and a failing case reports its own case name, not an index a reader has to count rows to decode",
  ], { fontSize: 17 });
  addNotes(s,
    "The payoff is the same one a table-driven test gives in any language, and the choice of source is not " +
    "interchangeable: @EnumSource is the wrong choice the moment a test needs to exclude or specially handle " +
    "only some of an enum's constants, forcing every value through one assertion body in awkward branching " +
    "that a @ValueSource list or a @MethodSource table would avoid entirely. The display-name pattern, a " +
    "literal {0} in the parameterized-test name pulling the case name or the argument straight into the test " +
    "report, is what turns a numbered instance into a named one; without it, a failure points at an index a " +
    "reader has to go count rows in the source table to identify.");
}

{
  const s = S();
  addCodeSlide(s, "TESTING THE WORK · JUNIT 5", "A table of rules replaces four near-identical tests", "Java 25 · JUnit 5",
    [
      "@ParameterizedTest(name = \"{0}\")",
      "@MethodSource(\"invalidShipments\")",
      "void rejectsWithExpectedReason(",
      "        String caseName, Shipment shipment, String expectedReasonFragment) {",
      "    assertThatThrownBy(() -> validator.validate(shipment))",
      "            .isInstanceOf(InvalidShipmentException.class)",
      "            .hasMessageContaining(expectedReasonFragment);",
      "}",
      "",
      "static Stream<Arguments> invalidShipments() {",
      "    return Stream.of(",
      "        Arguments.of(\"hazmat over the unescorted weight ceiling\",",
      "            new Shipment(75.0, CargoClass.HAZMAT, \"SEA\", 10_000L),",
      "            \"freight approval\"));",
      "}",
    ],
    "A fifth rule is a fifth row in invalidShipments(); the method body under test never changes.");
  addNotes(s,
    "validate() checks several independent rules on a Shipment: weight must be positive, declared value must " +
    "be positive and under an insurable limit, and hazmat shipments over a weight ceiling need freight " +
    "approval. Writing one @Test per rule scales badly, a fifth rule means a fifth copy-pasted method. " +
    "@ParameterizedTest plus @MethodSource collapses that into one assertion and a table of named cases, and " +
    "the {0} in the display name means ctest-style output reports a failing case by its own name, " +
    "\"hazmat over the unescorted weight ceiling,\" not a row number a reader has to trace back by hand.");
}

{
  const s = S();
  addCodeSlide(s, "TESTING THE WORK · JUNIT 5", "Checking a thrown exception, the AssertJ way", "Java 25 · AssertJ",
    [
      "assertThatThrownBy(() -> validator.validate(shipment))",
      "        .isInstanceOf(InvalidShipmentException.class)",
      "        .hasMessageContaining(\"weight must be positive\");",
      "",
      "assertThat(bookingRecord)",
      "        .extracting(BookingRecord::bookingId,",
      "                    br -> br.amount(),",
      "                    br -> br.shipment().cargoClass())",
      "        .containsExactly(\"BK-0001\", Money.ofCents(570L), CargoClass.STANDARD);",
    ],
    "A failed containsExactly names which of the three extracted values diverged and what it held instead.");
  addNotes(s,
    "Both assertThatThrownBy and JUnit 5's own Assertions.assertThrows check an identical fact, that a call " +
    "throws and what it throws; this book's suites keep both idioms visible rather than hiding one, but " +
    "AssertJ wins by default once more than one property of a result needs checking. A bare assertEquals " +
    "checks one fact per line, and checking three facts about the same booking record means either three " +
    "separate calls or a wall of && inside one assertTrue, neither of which reports which specific fact " +
    "failed. extracting() plus containsExactly reads as one sentence about the object and names the exact " +
    "value that diverged, which is the legibility argument for AssertJ in one example.");
}

{
  const s = S();
  addContentTitle(s, "TESTING THE WORK · MOCKITO", "Choosing a mock, a spy, or the real thing");
  addTwoColBullets(s, [
    "A deterministic, side-effect-free collaborator (a validator, a cost calculator) is called directly, never mocked: a mock that always returns without throwing would hide whether the real rule ran at all",
    "A collaborator with a side effect (a rate ledger that writes somewhere durable) is a @Mock, verified with verify() and an ArgumentCaptor",
  ], [
    "A spy is right when most of a real collaborator's behavior should run unmodified and only one hard-to-produce condition needs forcing",
    "Mocking a pure function to \"isolate\" a test from code that has nothing to isolate from adds setup and removes confidence at the same time",
  ], { fontSize: 15 });
  addNotes(s,
    "A shipment booking service is the concrete case this decision turns on: it has four collaborators split " +
    "across two shapes. The validator and the cost calculator are constructed as the real classes and called " +
    "directly in every test, because calling the real method and checking its real return value is both " +
    "simpler to write and closer to what a caller of the service actually depends on. The ledger is the one " +
    "collaborator with a side effect, recording a booking somewhere a test should never have to provision, " +
    "which is exactly the shape a Mockito mock exists for.");
}

{
  const s = S();
  addCodeSlide(s, "TESTING THE WORK · MOCKITO", "A mock plus an ArgumentCaptor verifies the interaction", "Java 25 · Mockito",
    [
      "@Mock",
      "private RateLedger ledger;",
      "",
      "@Test",
      "void recordsThePricedBookingOnTheLedger() {",
      "    BookingRecord bookingRecord = service.book(shipment);",
      "",
      "    ArgumentCaptor<BookingRecord> captor = ArgumentCaptor.forClass(BookingRecord.class);",
      "    verify(ledger).record(captor.capture());",
      "    assertThat(captor.getValue()).isEqualTo(bookingRecord);",
      "}",
      "",
      "// A rejected shipment must never reach the ledger at all.",
      "verify(ledger, never()).record(any());",
    ],
    "never() makes the absence of a call the assertion; a return-value-only test could not catch this bug.");
  addNotes(s,
    "A test that only checked the thrown exception on a malformed shipment would pass even if the service " +
    "called ledger.record before validation ran and threw afterward, a bug an interaction check catches and " +
    "a return-value check cannot. The ArgumentCaptor answers a different question than verify() alone: not " +
    "only that record was called, but exactly what BookingRecord was passed, so the test confirms the " +
    "recorded amount matches what the cost calculator actually priced, not a value the test merely assumes " +
    "the service computed correctly.");
}

{
  const s = S();
  addCodeSlide(s, "TESTING THE WORK · MOCKITO", "spy(): most of a real collaborator, one method forced", "Java 25 · Mockito",
    [
      "InMemoryRateLedger realLedger = new InMemoryRateLedger(1);",
      "RateLedger ledgerSpy = spy(realLedger);",
      "doReturn(0).when(ledgerSpy).capacityRemaining();",
      "",
      "ShipmentBookingService atCapacityService =",
      "        new ShipmentBookingService(validator, costCalculator, ledgerSpy, idGenerator);",
      "",
      "assertThatThrownBy(() -> atCapacityService.book(shipment))",
      "        .isInstanceOf(IllegalStateException.class)",
      "        .hasMessageContaining(\"capacity\");",
      "",
      "verify(ledgerSpy, never()).record(any());",
      "assertThat(realLedger.recordedBookings()).isEmpty();",
    ],
    "realLedger still has zero recorded bookings: a fact about real state, not a canned answer a mock was told to give.");
  addNotes(s,
    "Forcing a real RateLedger implementation to report zero remaining capacity would otherwise mean booking " +
    "it full first, which works but obscures the one fact the test actually cares about. spy() wraps a real " +
    "InMemoryRateLedger, and doReturn stubs exactly the one method the test needs to force, while record, if " +
    "it were ever called, would still run the real implementation. The last assertion is the point a mock " +
    "could not make: realLedger, the actual object underneath the spy, still has zero recorded bookings. " +
    "Stubbing more methods on a spy than the one edge case actually requires turns a mostly-real collaborator " +
    "into a confusing hybrid, harder to reason about than either a plain mock or the unmodified real thing.");
}

{
  const s = S();
  addContentTitle(s, "TESTING THE WORK · MOCKITO", "Argument matchers and strict stubbing");
  addTwoColBullets(s, [
    "A bare literal inside verify's or EXPECT's parentheses is treated as eq() automatically; a matcher like any() or eq() is needed the moment one argument matters and another does not",
    "Mockito's default strict stubbing fails a test with UnnecessaryStubbingException if a stub configured in @BeforeEach is never used by every test in the class",
  ], [
    "lenient() tells Mockito one specific stub is allowed to go unused in some tests; reach for it exactly when a shared setup stub is only reached by some test paths, never as a blanket way to silence the check",
    "Reading an over-mocked or unverified test the same way every time: does this assertion check a fact a bug could break, or only that a stub returned what it was configured to return",
  ], { fontSize: 15 });
  addNotes(s,
    "A capacity stub set in @BeforeEach is reached by every accepted-booking test, but a shipment that fails " +
    "validate() never gets as far as checking capacity at all, so that nested test class would otherwise fail " +
    "for the wrong reason, an unused stub, rather than pass or fail on its own assertion. lenient() is the " +
    "narrow fix for exactly that shape, not a way to silence strict stubbing broadly across a suite where it " +
    "is doing useful work catching a stub nobody actually exercises anymore.");
}

{
  const s = S();
  addStatusTable(s, [
    { code: "Dummy", name: "Satisfies a signature, never exercised", purpose: "A BookingIdGenerator lambda passed in for a test whose shipment fails validation before id generation ever runs" },
    { code: "Stub", name: "Returns canned values, no call verification", purpose: "Clock.fixed(overdueAt, zone), pinning the instant a delivery-window check reads" },
    { code: "Spy", name: "Records calls, inspected after the fact", purpose: "A hand-written RecordingCarrierGateway appending every notification to a list a test reads after exercise" },
    { code: "Mock", name: "Verifies an expected interaction, fails if unmet", purpose: "Mockito's @Mock RateLedger, checked with verify(ledger).record(...)" },
    { code: "Fake", name: "A working, lightweight implementation", purpose: "A FakeCarrierGateway backed by a map, correct behavior with no SMTP connection" },
  ], { colW: [1.70, 4.20, 6.19], rowH: 0.57 });
  addContentTitle(s, "TESTING THE WORK · XUNIT VOCABULARY", "The test-double taxonomy, mapped to this stack");
  addNotes(s,
    "Meszaros names five kinds of test double, and Mockito's own spy() method is not the same concept as the " +
    "Spy in this taxonomy, even though the two share a word. A Mockito spy wrapping a real object and forcing " +
    "one method is closer to a Fake-plus-Mock hybrid in practice than to this row's hand-written Spy, which " +
    "records what happened with no expectation declared up front and no verify call afterward. The dividing " +
    "line that matters most day to day is Mock versus Fake: if the next sentence after \"this test checks " +
    "that\" names a method and an argument, reach for a mock; if it names a value the collaborator should be " +
    "holding afterward, reach for a fake.");
}

{
  const s = S();
  addStatusTable(s, [
    { code: "Fragile test", name: "Breaks on a behavior-preserving refactor", purpose: "Depend on the collaborator's interface, never a concrete implementation's internals" },
    { code: "Erratic test", name: "Passes or fails by run order or wall clock", purpose: "Inject a Clock; keep process-wide state like a counter transient, not shared across tests" },
    { code: "Obscure test", name: "A failure doesn't say what broke", purpose: "One behavior per @Test, named with @DisplayName; @Nested groups related scenarios" },
    { code: "Conditional logic", name: "A test branches or loops over cases", purpose: "@ParameterizedTest with @MethodSource, not a loop that can silently skip a case" },
    { code: "Slow test", name: "Milliseconds of logic, seconds of runtime", purpose: "A fake in-memory collaborator, or a spy with the expensive call forced, stands in for a real connection" },
  ], { colW: [2.70, 4.10, 5.29], rowH: 0.57 });
  addContentTitle(s, "TESTING THE WORK · XUNIT VOCABULARY", "Five test smells, and the fix this stack applies");
  addNotes(s,
    "Meszaros catalogs dozens of smells; these five show up most often in a suite built on JUnit 5, Mockito, " +
    "and AssertJ. The erratic-test fix is the same object seam the legacy-code section builds out in full: " +
    "replace a direct Instant.now() or LocalDate.now() call with an injected Clock, and keep any process-wide " +
    "state, such as a sequential id counter, transient rather than shared across a class's tests. The " +
    "obscure-test fix is naming discipline: a @DisplayName like \"rejects a non-positive weight\" tells a " +
    "reader what broke before they open the file; a method named test3 tells them nothing.");
}

{
  const s = S();
  addContentTitle(s, "TESTING THE WORK · AI SIDEBAR", "A generated suite that compiles, runs green, proves little");
  addBullets(s, [
    "The tautological shape: a test stubs ledger.record(any()) to return normally, then asserts the method returned a value, which only confirms the mock did what it was told",
    "The over-mocking shape: a generated suite mocks a validator and a cost calculator even though neither has a side effect worth isolating, because mocking every collaborator looks thorough regardless of purity",
    "A generated test suite with no verify(..., never()), no ArgumentCaptor, and a mock for every single collaborator is a strong sign the model reached for the mechanics without reaching for the question an interaction test exists to answer",
    "Read every generated test the same way: does this assertion check a fact a bug could break, or only that a stub returned what it was configured to return",
  ], { fontSize: 16 });
  addNotes(s,
    "Ask an LLM pairing partner to \"write tests for this service\" and a common result compiles, runs green, " +
    "and proves almost nothing. The tautological shape and the over-mocking shape share a root cause: a model " +
    "optimizing for a plausible-looking test rather than the smallest test that actually proves the claim. " +
    "This is the same forces test the rest of this book applies everywhere: for every mock the model " +
    "introduced, ask what side effect it is isolating, and delete the ones with no answer. The working-with-" +
    "AI section later in this deck builds the same discipline into a formal verification gate.");
}

// =============================================================================
// SECTION 2 - Changing inherited code
// =============================================================================
divider("02", "Changing inherited code", "Seams, the dependency-breaking catalogue, Fowler versus Feathers, and the refactor-or-rewrite call",
  "Code you inherit was written to satisfy whatever requirement was in front of its author at the time, under " +
  "whatever deadline was in front of them. Nobody asked whether a future maintainer would need to substitute " +
  "one of its collaborators for a test double, because the change that will need the substitution had not " +
  "been imagined yet. This section works through the discipline that makes changing that code safe: " +
  "characterize first, open a seam second, change behavior third, with a test suite watching the whole time. " +
  "It covers the dependency-breaking catalogue adapted to Java 25, when Fowler's refactoring catalogue and " +
  "Feathers' seam-finding catalogue each apply, and how to decide between refactoring in place and a bounded, " +
  "strangler-fig rewrite.");

{
  const s = S();
  addContentTitle(s, "CHANGING INHERITED CODE", "Characterization tests: pinning what the code actually does");
  addBullets(s, [
    "A characterization test records what inherited code actually does right now, wrong parts included, not what it should do",
    "The goal is a safety net: a way to tell, after a later edit, whether behavior that wasn't supposed to change actually stayed put",
    "A wide test margin, a full day rather than a few seconds, is a symptom of a hard dependency, not a stylistic choice",
    "A boundary a characterization test can't even reach is telling you exactly where a seam has to go before anything else is testable",
  ], { fontSize: 17 });
  addNotes(s,
    "Michael Feathers calls code with no tests covering it legacy code, independent of how old or how well " +
    "written it is, precisely because the absence of a test is what makes a change risky. A characterization " +
    "test for a delivery-escalation check that reads the wall clock directly can only pin a deadline well in " +
    "the past or well in the future; it cannot pin a deadline exactly equal to the current moment, because no " +
    "two calls to the real clock can land on the identical nanosecond. That is not a minor coverage gap. It " +
    "is the concrete, measurable cost of the hard dependency, and it names exactly where a seam has to go " +
    "before the next step can even be attempted.");
}

{
  const s = S();
  addCodeSlide(s, "CHANGING INHERITED CODE · CHARACTERIZATION", "A wide margin hides a hard dependency", "Java 25 · before",
    [
      "public EscalationResult escalate(Shipment shipment) {",
      "    Instant now = Instant.now();",
      "    Instant overdueAt = shipment.promisedDelivery().plus(OVERDUE_GRACE);",
      "    if (now.isAfter(overdueAt)) {",
      "        CarrierGateway gateway = new SmtpCarrierGateway();",
      "        String reason = \"promised delivery window missed\";",
      "        gateway.notifyCarrier(shipment, reason);",
      "        return EscalationResult.escalated(shipment, reason);",
      "    }",
      "    return EscalationResult.notOverdue(shipment);",
      "}",
    ],
    "A one-day margin is a workaround, not a fix: it hides the hard dependency, it doesn't remove it.");
  addNotes(s,
    "This passes today and will keep passing after any later change that preserves the legacy behavior, " +
    "exactly what a characterization test is for. escalate() calls Instant.now() a second time inside its " +
    "own body, microseconds after a test computes its own now to build the promised delivery instant, so a " +
    "margin small enough for those two readings to land close together would exercise scheduling jitter " +
    "instead of the service's logic. Widening the margin to a full day makes that impossible, but it is a " +
    "workaround, not a fix. A second, independent symptom sits in the same method: it constructs its own " +
    "SmtpCarrierGateway inline, so no test can hold a reference to the gateway that actually ran and ask it " +
    "what it did.");
}

{
  const s = S();
  addDiagramSlide(s, "CHANGING INHERITED CODE · SEAMS", "Making hard-wired dependencies substitutable", "29-seams-and-substitution",
    "The same inherited dependencies made substitutable two ways: an object seam for an injected Clock, an extract-interface seam for a CarrierGateway.");
  addNotes(s,
    "A seam, in Feathers' vocabulary, is a place where behavior can change without editing the logic that " +
    "behavior lives in. An object seam gives a class a way to receive its dependency from outside, through a " +
    "constructor, instead of reaching out and constructing it itself. An extract-interface seam gives a " +
    "hard-wired concrete collaborator an interface a test can substitute. A third seam, subclass-and-" +
    "override, moves one troublesome call into a protected method a test subclass can override, for a " +
    "dependency with no constructor cheap enough to change yet. All three answer the identical question: " +
    "given this hard-wired dependency, what is the smallest change that lets a test substitute something " +
    "else in its place.");
}

{
  const s = S();
  addCodeSlide(s, "CHANGING INHERITED CODE · OBJECT SEAM", "java.time.Clock: a seam the JDK ships ready-made", "Java 25 · after",
    [
      "public ShipmentEscalationService(Clock clock, CarrierGateway gateway) {",
      "    this.clock = Objects.requireNonNull(clock, \"clock\");",
      "    this.gateway = Objects.requireNonNull(gateway, \"gateway\");",
      "}",
      "",
      "public EscalationResult escalate(Shipment shipment) {",
      "    Instant now = clock.instant();",
      "    Instant overdueAt = shipment.promisedDelivery().plus(OVERDUE_GRACE);",
      "    if (!now.isBefore(overdueAt)) {",
      "        String reason = \"promised delivery window missed\";",
      "        gateway.notifyCarrier(shipment, reason);",
      "        return EscalationResult.escalated(shipment, reason);",
      "    }",
      "    return EscalationResult.notOverdue(shipment);",
      "}",
    ],
    "Clock.systemUTC() in production, Clock.fixed(instant, zone) in a test; no custom abstraction needed.");
  addNotes(s,
    "Clock.systemUTC() reads the real wall clock, the only instance production code ever wires in, and " +
    "Clock.fixed(instant, zone) returns a fixed instant on every call, exactly what a test needs. " +
    "Introducing the seam alone changes nothing observable: a service built with Clock.systemUTC() has to " +
    "answer every wide-margin question the legacy version already answered, the same way, because it reads " +
    "the identical clock through one extra method call. That equivalence, proven by running the same wide-" +
    "margin scenarios against both versions, is the entire argument that the seam is behavior-preserving on " +
    "its own. Only afterward does a deliberate change become safe: the comparison moves from strict greater-" +
    "than to greater-or-equal, because a deadline arriving right now is the moment an escalation should fire.");
}

{
  const s = S();
  addCodeSlide(s, "CHANGING INHERITED CODE · EXTRACT-INTERFACE SEAM", "An exact boundary, caught with a mock", "Java 25 · JUnit 5 + Mockito",
    [
      "@Test",
      "void aDeadlineExactlyAtTheGracePeriodBoundaryIsEscalated() {",
      "    Instant overdueAt = Instant.parse(\"2026-01-10T12:00:00Z\");",
      "    Instant promised = overdueAt.minus(OVERDUE_GRACE);",
      "    ShipmentEscalationService service = new ShipmentEscalationService(",
      "            Clock.fixed(overdueAt, ZoneOffset.UTC), gateway);",
      "    Shipment shipment = new Shipment(\"SHM-2101\", promised, 10.0);",
      "",
      "    EscalationResult result = service.escalate(shipment);",
      "",
      "    assertThat(result.escalated()).isTrue();",
      "    verify(gateway).notifyCarrier(eq(shipment), any());",
      "}",
    ],
    "No sleeping, no margin: Clock.fixed hits the exact boundary a wall-clock read never could.");
  addNotes(s,
    "CarrierGateway, extracted from the concrete SmtpCarrierGateway that used to be the only option, is the " +
    "second seam opened here, and a Mockito mock substituted through it turns an unobservable " +
    "interaction into a direct verify() assertion. Combined with Clock.fixed, the exact boundary a " +
    "characterization test could never reach becomes three ordinary, instant, flake-proof tests: a deadline " +
    "exactly at the grace-period boundary, one nanosecond before it, and one nanosecond after it. The " +
    "legacy service is never edited to match; it stays in the codebase exactly as found, a frozen reference " +
    "a reviewer can compare against the one line that changed.");
}

{
  const s = S();
  addCodeSlide(s, "CHANGING INHERITED CODE · SUBCLASS-AND-OVERRIDE SEAM", "No constructor to change, so override the call site", "Java 25 · after",
    [
      "public class ShipmentRiskScorer {",
      "    public int score(Shipment shipment) {",
      "        int score = baseRiskScore(shipment);",
      "        if (isHighRiskWindow()) {",
      "            score += PEAK_SEASON_SURCHARGE;",
      "        }",
      "        return score;",
      "    }",
      "",
      "    protected boolean isHighRiskWindow() {",
      "        return EnterpriseCalendar.isPeakSeason();",
      "    }",
      "}",
      "",
      "// Test-only subclass, both branches now reachable on demand:",
      "class TestableShipmentRiskScorer extends ShipmentRiskScorer {",
      "    @Override protected boolean isHighRiskWindow() { return true; }",
      "}",
    ],
    "Neither the constructor nor the public score() contract changes at all; one protected method does the work.");
  addNotes(s,
    "EnterpriseCalendar.isPeakSeason() is a process-wide, legacy calendar singleton several services read " +
    "from and no single team owns. Parameterizing score() with a boolean is the correct long-term fix, and " +
    "it stops being cheap the moment the same call shows up at a dozen other sites across a scoring pipeline " +
    "nobody wants to touch more than this change requires. A subclass-and-override seam sidesteps that cost: " +
    "the one call moves into its own protected method production code never overrides, and a test substitutes " +
    "an answer by extending the class. Compare this to a legacy characterization test that has to call the " +
    "real calendar a second time from inside the test and compute the expected value from whatever the " +
    "calendar says today, proving only that the scorer agrees with the calendar, never that the surcharge " +
    "logic is correct in the branch the calendar is not currently returning.");
}

{
  const s = S();
  addContentTitle(s, "CHANGING INHERITED CODE", "The safety-net discipline, in order");
  addBullets(s, [
    "Characterize first: observe and pin what the inherited code does right now, wide margins and unreachable boundaries included",
    "Open a seam second, whichever kind fits the dependency, and prove with the existing characterization tests that the seam alone changed nothing",
    "Change behavior third, and only third: add a test pinning the corrected behavior at the exact point it lives, with everything else staying green",
    "Skipping the first step means changing code you have not actually observed; skipping the second means changing behavior in code that still has no seam",
  ], { fontSize: 17 });
  addNotes(s,
    "The order is not optional. A seam a test proves behavior-preserving before the deliberate change happens " +
    "is what converts 'I am fairly sure this is still correct' into 'the suite that passed before this change " +
    "still passes after it, plus one new test for exactly what I meant to change.' This is the discipline a " +
    "generated 'fix the bug' or 'make this testable' suggestion routinely skips: a full rewrite that injects " +
    "every dependency through a new constructor in one change bundles an untested, higher-risk rewrite with " +
    "the one thing actually needed, a passing test. The smaller technique gets the identical seam with a " +
    "change small enough to review in minutes and with zero existing callers touched.");
}

{
  const s = S();
  addDiagramSlide(s, "CHANGING INHERITED CODE · DEPENDENCY BREAKING", "Opening a seam around an embedded concrete type", "30-dependency-breaking",
    "Before: a concrete SmtpNotificationGateway constructed inline. After Extract Interface plus Parameterize Constructor, either the real gateway or a test double sits behind NotificationGateway.");
  addNotes(s,
    "Extract Interface pulls the one method a dependent class actually calls, sendDispatchConfirmation, out " +
    "of a concrete gateway's full surface; extracting the interface alone changes nothing about how the " +
    "dependent gets its dependency, it only creates the possibility of a second implementation. Parameterize " +
    "Constructor is the move that opens the seam, and in Java it takes a shape Feathers' original examples " +
    "did not quite need: Java has no default-argument syntax, so a default parameter value becomes a " +
    "constructor overload, with the original one-argument constructor forwarding to the new two-argument one. " +
    "Every existing call site written against the old constructor still compiles and still behaves " +
    "identically, because the single-argument overload forwards to the same real gateway it used to build " +
    "internally.");
}

{
  const s = S();
  addStatusTable(s, [
    { code: "Extract Interface", name: "Pull the minimal contract into an interface", purpose: "A dependent class can only be built against one concrete collaborator type" },
    { code: "Parameterize Constructor", name: "A constructor overload accepts the collaborator", purpose: "A constructor builds its own collaborator internally; Java has no default arguments, so the overload is the seam" },
    { code: "Parameterize Method", name: "Pass the collaborator as a method argument", purpose: "A single method, not the whole object, reaches for a hard-wired collaborator" },
    { code: "Extract and Override Factory Method", name: "Move a new expression into its own protected method", purpose: "An embedded new expression hard-wires which concrete type gets constructed" },
    { code: "Subclass and Override Method", name: "A test-only subclass overrides just that call", purpose: "Any method with a troublesome implementation, such as a wall-clock read" },
    { code: "Adapt Parameter", name: "Extract a minimal interface; wrap the heavyweight type", purpose: "A method depends on a heavyweight concrete parameter it only reads a few values from" },
    { code: "Wrap / decorate", name: "A thin wrapper forwards to the untouched original", purpose: "New code needs a seam in front of a class nobody is allowed to modify" },
  ], { colW: [3.00, 3.90, 5.19], rowH: 0.47 });
  addContentTitle(s, "CHANGING INHERITED CODE · DEPENDENCY BREAKING", "Seven techniques, each breaking one kind of dependency");
  addNotes(s,
    "Feathers' catalogue stays narrow by design: a team facing a constructor that builds its own collaborator " +
    "and a team facing a single troublesome call buried inside a method body are facing structurally " +
    "different problems, and reaching for 'add dependency injection everywhere' as a blanket answer to both " +
    "usually means a much larger change than either problem actually requires. Reach for Parameterize " +
    "Constructor or Parameterize Method first whenever a dependency enters through a parameter list a caller " +
    "already controls, since nothing beats a plain argument for simplicity; reach for Extract and Override " +
    "Factory Method or Subclass and Override Method only when the dependency is embedded inside a method body " +
    "with no parameter list to extend without changing the contract for every caller.");
}

{
  const s = S();
  addCodeSlide(s, "CHANGING INHERITED CODE · EXTRACT AND OVERRIDE FACTORY METHOD", "One embedded new expression, isolated behind a protected method", "Java 25 · after",
    [
      "// Before: the concrete calculator is built inline, every time.",
      "long surcharge = new StandardSurchargeCalculator()",
      "        .surchargeCents(shipment.baseCostCents());",
      "",
      "// After: construction moves into its own overridable method.",
      "protected SurchargeCalculator createSurchargeCalculator() {",
      "    return new StandardSurchargeCalculator();",
      "}",
      "",
      "long surcharge = createSurchargeCalculator()",
      "        .surchargeCents(shipment.baseCostCents());",
    ],
    "A test-only subclass overrides createSurchargeCalculator() to return a Mockito double; every production path is unchanged.");
  addNotes(s,
    "Nothing about the surcharge math is the problem; the problem is that dispatch decided, at the exact " +
    "point it needed a calculator, which concrete type to build, with no way for a caller or a test to " +
    "influence that decision short of changing the line itself. The seam this buys is narrower than Extract " +
    "Interface plus Parameterize Constructor, and that narrowness is the point: no new constructor parameter, " +
    "no new field, no change to the public surface at all, just one embedded decision moved into a method a " +
    "subclass can intercept. Adapt Parameter follows the same discipline from the opposite direction, " +
    "shrinking a method's dependency on a heavyweight concrete parameter, a manifest file whose constructor " +
    "opens a real file on disk, down to the two values the method actually reads, through a minimal " +
    "ManifestSource interface and a thin adapter.");
}

{
  const s = S();
  addDiagramSlide(s, "CHANGING INHERITED CODE · FOWLER AND FEATHERS", "Characterization as the safety net under a refactor", "31-characterization-as-a-safety-net",
    "Probing a legacy calculator pins its current outputs into a suite; a Fowler-style refactor reshapes the code underneath, and the same pinned assertions staying green is the proof nothing observable changed.");
  addNotes(s,
    "A freight-quote calculator with no author anyone can name and no test anywhere near it is a familiar " +
    "shape: it computes transit days and a surcharge, three carriers depend on its exact numbers, and reading " +
    "it closely reveals a weight calculation that looks like it rounds down when it should round up. The " +
    "question that matters is not answered by reading the code more carefully. It starts with running the " +
    "code and writing down what it currently does, probing near every boundary a condition could flip: a " +
    "10-kilogram tier cutoff, a strict-greater-than residential threshold, a declared-value surcharge that " +
    "truncates toward zero instead of rounding. None of those three quirks was found by reading a " +
    "specification; each was found by running the code and writing down what came back.");
}

{
  const s = S();
  addStatusTable(s, [
    { code: "Starting assumption", name: "A fast, trustworthy suite already exists", purpose: "No such suite exists, and the code's structure is usually why" },
    { code: "Primary goal", name: "Reshape a design for clarity, same behavior", purpose: "Make a piece of code observable enough to write a test against it at all" },
    { code: "Risk profile", name: "A mistake is caught within seconds by the suite", purpose: "Change as little as possible; current behavior, bugs included, is protected until proven otherwise" },
    { code: "Core technique", name: "About ninety named, mechanical refactorings", purpose: "Characterization tests plus a catalogue of dependency-breaking moves" },
    { code: "Right lens when", name: "Coverage exists; the question is how to shape it", purpose: "No coverage exists; the question is what the code does today" },
  ], { colW: [2.40, 4.70, 4.99], rowH: 0.57 });
  addContentTitle(s, "CHANGING INHERITED CODE · FOWLER AND FEATHERS", "Contrasted: Refactoring versus Working Effectively with Legacy Code");
  addNotes(s,
    "The risk-profile row is the one that trips engineers who have read only one of the two books. Fowler's " +
    "refactorings are safe specifically because a test suite exists to catch a mistake within seconds, and " +
    "that safety is borrowed from an asset Feathers' techniques exist to create in the first place. A " +
    "codebase with full coverage and a tangled conditional needs Fowler's catalogue and nothing from " +
    "Feathers, there is no missing safety net to build. A freight-quote calculator with no tests needs " +
    "Feathers first, because no named refactoring is safe to apply to a calculation nobody can currently " +
    "verify stayed the same.");
}

{
  const s = S();
  addContentTitle(s, "CHANGING INHERITED CODE · FOWLER AND FEATHERS", "How the two combine, in order, on one calculator");
  addBullets(s, [
    "Characterize first: a parameterized test pins eight representative-and-boundary rows against the legacy calculator, quirks included, before any structural change",
    "Only then open the refactoring catalogue: Replace Conditional with Polymorphism retires two duplicated carrier dispatch chains in favor of one sealed CarrierProfile per carrier",
    "Extract Function and Introduce Parameter Object reshape the rest; none of the three refactorings touches a single piece of arithmetic",
    "Proof the refactor preserved every pinned behavior: the identical characterization table, run again against the refactored calculator, with not one expected value changed",
  ], { fontSize: 16 });
  addNotes(s,
    "A sealed CarrierProfile interface with one implementation per carrier, selected through a static " +
    "forCode() switch expression, replaces a pair of duplicated if/else chains, one for transit days and a " +
    "near-identical one for the base rate, that drifted apart years ago when a new carrier was added by " +
    "copy-paste instead of extension. A refactor that quietly corrected the weight-tier truncation while " +
    "renaming methods would still pass code review as a clean diff and would still be the wrong change, made " +
    "at the wrong time, hidden inside a commit that claimed to be purely structural. A characterization " +
    "test's job is not to bless the code's behavior as correct; it is to make 'unchanged' checkable, so a " +
    "decision to fix a quirk shows up in its own commit, with its own updated assertion.");
}

{
  const s = S();
  addContentTitle(s, "CHANGING INHERITED CODE · REFACTOR OR REWRITE", "Six decision factors, never \"how old or ugly\"");
  addTwoColBullets(s, [
    "Risk: what breaks, and who notices first",
    "Cost: a long calendar spread versus a concentrated block with dual-maintenance overhead",
    "Undocumented knowledge: a renegotiated fuel rate or a weight-based fee waiver is the business logic, not noise to clean up",
  ], [
    "How well the current code is understood today",
    "Whether a safety net already exists",
    "The cost of being wrong: wasted time versus a half-built replacement and a frozen legacy system nobody can touch either",
  ], { fontSize: 16 });
  addNotes(s,
    "None of these six factors is how old the code is or how ugly it is, two questions that feel diagnostic " +
    "and mostly are not. Legacy code that's old and unlovely but well covered by tests and fully understood " +
    "is a refactoring candidate in good standing; code written last quarter that nobody can explain and has " +
    "no tests at all is a rewrite candidate despite being brand new. The asymmetry in the cost of being wrong " +
    "is itself a reason most components should default toward refactoring unless a specific force argues " +
    "otherwise: a wrong refactor costs wasted time, a wrong rewrite costs a half-built replacement plus a " +
    "legacy system nobody can touch safely either.");
}

{
  const s = S();
  addDiagramSlide(s, "CHANGING INHERITED CODE · STRANGLER FIG", "A stable interface, a routing seam, a shared suite", "32-strangler-fee-router",
    "A shipment service calls AccessorialFeeCalculator; a router sits behind that interface, checks a migration registry, and sends each shipment to the legacy or modern implementation.");
  addNotes(s,
    "Named for a tree that germinates in the canopy of a host and gradually envelops it until the host can be " +
    "removed, the strangler fig replaces a legacy component behind a stable interface incrementally. Every " +
    "caller depends only on AccessorialFeeCalculator, never on a concrete legacy or modern implementation, " +
    "which is the same force that justifies dependency inversion anywhere else: the implementation behind " +
    "the interface can change without touching a single call site. The old implementation's share of traffic " +
    "does not drop to zero in one release. It shrinks one shipment at a time, each increment observable and " +
    "reversible if something looks wrong.");
}

{
  const s = S();
  addContentTitle(s, "CHANGING INHERITED CODE · STRANGLER FIG", "Three pieces, each earning its keep");
  addBullets(s, [
    "A stable interface: every caller depends on calculateFees(), never on a concrete legacy or modern engine",
    "A routing seam: a registry of migrated shipment IDs is enough to move one shipment at a time, and to move it back by removing an entry",
    "A shared characterization suite: the new implementation earns traffic by passing the old implementation's own pinned cases, not a fresh suite written to match it",
    "Passing a different, newly written suite proves the new code does what its author intended; passing the old suite proves it does what the business needs, a stronger and different claim",
  ], { fontSize: 17 });
  addNotes(s,
    "A legacy accessorial fee calculator's undocumented knowledge, a fuel surcharge rate bumped during a " +
    "contract renegotiation years ago and left as a bare comment, a residential fee waived under a weight " +
    "threshold nobody can trace to a ticket, an oversize penalty an auditor once had to reverse-engineer from " +
    "an invoice, is exactly the kind of knowledge a characterization suite pulled from the running system " +
    "captures and a clean rewrite from a tidy mental model of 'how fees should work' would otherwise silently " +
    "drop. A modern replacement restructures that knowledge with a switch expression and named constants; it " +
    "does not rediscover it from a specification that does not exist.");
}

{
  const s = S();
  addCodeSlide(s, "CHANGING INHERITED CODE · STRANGLER FIG", "The router stays small", "Java 25 · router",
    [
      "@Override",
      "public AccessorialFees calculateFees(ShipmentManifest manifest) {",
      "    AccessorialFeeCalculator target = registry.isMigrated(manifest.shipmentId())",
      "            ? modern",
      "            : legacy;",
      "    return target.calculateFees(manifest);",
      "}",
      "",
      "@ParameterizedTest(name = \"{0}\")",
      "@MethodSource(\"...AccessorialFeeCharacterizationCases#cases\")",
      "void matchesPinnedFees(String caseName, ShipmentManifest manifest, AccessorialFees expectedFees) {",
      "    assertThat(modern.calculateFees(manifest)).isEqualTo(expectedFees);",
      "}",
    ],
    "The modern calculator is judged against the legacy calculator's own pinned table, not a fresh suite written to match it.");
  addNotes(s,
    "StranglerFeeCalculatorRouterTest proves this routing with mocks standing in for both calculators, " +
    "checking the interaction rather than only the return value: a migrated shipment reaches the modern " +
    "calculator and the legacy one is verified never() called, an unmigrated shipment reaches the legacy " +
    "calculator with the same guarantee in reverse, and once every shipment has migrated, " +
    "verifyNoInteractions(legacy) confirms the legacy calculator is not touched at all. The migration policy " +
    "lives entirely in a small registry, separate from the router that consults it, so the policy can change " +
    "without touching the seam. A separate agreement test drives both calculators through one shipment " +
    "profile the pinned table does not cover and asserts their fee breakdowns match exactly, independent of " +
    "either calculator's own pinned expectations.");
}

{
  const s = S();
  addDiagramSlide(s, "CHANGING INHERITED CODE · STRANGLER FIG", "Three releases, one invisible migration", "32-strangler-cutover-timeline",
    "The same three shipments across three releases. One shipment migrates at a time, from legacy to modern, and every printed total holds steady through the cutover.");
  addNotes(s,
    "A cold start where every shipment is still priced by the legacy calculator, a release where one " +
    "shipment has migrated while the other two have not, and a final release where every shipment has " +
    "migrated and the legacy calculator sits unused: every total printed across all three releases is " +
    "identical. That is the strangler fig's actual promise, made visible rather than asserted in prose. The " +
    "migration is invisible to the business because the characterization table proved it would be before " +
    "the router ever routed a single shipment to the modern calculator. A strangler fig is not free, though: " +
    "a component with low risk, low cost, and a codebase the team already understands does not need a " +
    "router or a gradual cutover policy at all, and building the seam before pinning current behavior only " +
    "gives a new implementation a routing mechanism with nothing yet proven for it to agree with.");
}

// =============================================================================
// SECTION 3 - Working with AI and LLM tools
// =============================================================================
divider("03", "Working with AI and LLM tools", "A pattern-literate collaborator, its failure modes, and the gate that catches them",
  "An AI pairing partner can draft a Strategy, a Factory Method, or a dependency-breaking seam in seconds, " +
  "and it can do it in a shipping system's actual vocabulary if you give it that vocabulary to work with. " +
  "That capability changes how fast a design decision turns into running code. It does not change who " +
  "answers for the code once it ships. This section covers how to prompt with the forces vocabulary this " +
  "deck has used throughout, the three failure modes that show up reliably in generated Java, and the two-" +
  "gate verification discipline, a behavior suite and an idiomatic review, that decides whether a draft " +
  "merges.");

{
  const s = S();
  addContentTitle(s, "WORKING WITH AI & LLM TOOLS", "A collaborator who has read everything and judges nothing");
  addBullets(s, [
    "An LLM has read a vast amount of Java and applied none of the judgment that decides when a pattern belongs",
    "Naming the force and the candidate pattern narrows the space of plausible answers; a vague request gets a vague, usually over-engineered one back",
    "The same vocabulary works in reverse: a generated ReturnPolicyEngine wrapping one implementation behind a factory, a registry, and a builder hasn't built a Strategy, it's three types around a decision already made",
    "AI changes who writes the code, not who answers for whether it's correct and idiomatic",
  ], { fontSize: 17 });
  addNotes(s,
    "A request phrased as 'a return request classifies into a refund outcome that varies only by days since " +
    "delivery, and checkout code shouldn't need to know the exact policy boundaries, write this as a " +
    "Strategy with the usual shape' gives the model the force, the pattern name, and an implicit scope " +
    "boundary all at once, each one narrowing the space of plausible answers. Compare that to 'write a " +
    "function that figures out if someone can return something,' which tells the model nothing about " +
    "whether the outcome set is closed or open, so it fills every gap with whatever shape is statistically " +
    "common in a large Java corpus, which is rarely the minimal one. Naming a pattern also works to rule one " +
    "out: 'this policy has exactly three tiers and nothing needs a second way of sourcing that number, don't " +
    "wrap it behind a rule engine' short-circuits patternitis before it starts.");
}

{
  const s = S();
  addTwoColBullets(s, [
    "Patternitis: an interface, a factory, and a holder class for reading one configuration value out of one place",
    "Hallucinated APIs: a confident call into an overload that doesn't exist, or a record component from a different JDK than the one this book targets",
  ], [
    "Non-idiomatic Java: a hand-written equals/hashCode where a record already gives you both, a switch with a throwing default where an enum's constant-specific bodies make a missing case a compile error",
    "None of these fail to compile; idiom and correctness at a boundary are not things a compiler checks",
  ], { fontSize: 16 });
  addContentTitle(s, "WORKING WITH AI & LLM TOOLS", "Three failure modes of an unchecked collaborator");
  addNotes(s,
    "All three share a root cause: a model optimizing for a plausible-looking answer to the prompt rather " +
    "than the smallest correct answer to the actual problem. Patternitis is this book's own anti-pattern " +
    "material applied to a model trained on a corpus containing plenty of over-engineered example code; the " +
    "fix is the same forces test applied everywhere else, ask what force justifies each type the model " +
    "introduced and delete the ones with no answer. Hallucinated APIs are a predictable consequence of how " +
    "these models generate text, the token sequence that looks most like correct code for the surrounding " +
    "context, with no compiler in the loop until a human or a build catches it; the practical defense is " +
    "unglamorous: compile it, and don't treat a model's description of an API's behavior as a substitute for " +
    "the Javadoc.");
}

{
  const s = S();
  addDiagramSlide(s, "WORKING WITH AI & LLM TOOLS", "The human-AI loop with a verification gate", "34-ai-verification-gate",
    "The engineer names the forces and a candidate pattern; the AI drafts; nothing ships past a behavior suite plus an idiomatic review.");
  addNotes(s,
    "The engineer names the forces and the candidate pattern, the AI drafts an implementation, and a " +
    "verification gate of behavior tests plus an idiomatic review decides whether the draft is accepted or " +
    "sent back. Responsibility for correctness stays with the engineer at every point in this loop. AI " +
    "tooling is useful on the other side of this gate too: given an existing method and a request to " +
    "generate a test for every branch it currently takes, asserting whatever it currently returns without " +
    "changing anything, a model produces wide characterization coverage fast, the mechanical half of pinning " +
    "an existing system's behavior. What it should never decide unsupervised is whether a given boundary or " +
    "tier is the one the business meant; that judgment call belongs to the engineer who holds the actual " +
    "specification.");
}

{
  const s = S();
  addContentTitle(s, "WORKING WITH AI & LLM TOOLS", "The verification gate: two required checks, not either-or");
  addBullets(s, [
    "A behavior-test suite, written against the specification the code must satisfy, not against whatever the draft happens to do",
    "A test suite the same model wrote for its own draft inherits its blind spots; a human has to write or review the boundary table",
    "An idiomatic-review pass checks the draft against the project's real conventions: a record over a hand-written value class, a sealed hierarchy over an unconstrained one",
    "Both gates are required: one proves correctness against a specification, the other proves the code fits the codebase it's joining",
  ], { fontSize: 17 });
  addNotes(s,
    "Nothing in the three failure modes is detectable by reading the code and nodding. Patternitis compiles. " +
    "A hallucinated API sometimes compiles against the wrong overload and runs without throwing on the one " +
    "input a casual smoke test happens to exercise. Non-idiomatic Java compiles by definition, since idiom " +
    "isn't something the compiler checks. A pattern that passes its behavior tests but reads like it ignores " +
    "a modern idiom the rest of the file already uses has still failed half of what this discipline checks " +
    "for, which is exactly why both gates are required and neither one is optional.");
}

{
  const s = S();
  addCodeSlide(s, "WORKING WITH AI & LLM TOOLS", "One operator, one boundary, a $40 overpayment", "Java 25 · Strategy",
    [
      "// Drafted: both lower bounds inclusive, as the policy specifies.",
      "public ReturnEligibility classify(int daysSinceDelivery) {",
      "    if (daysSinceDelivery < 30)  return ReturnEligibility.FULL_REFUND;",
      "    if (daysSinceDelivery < 60)  return ReturnEligibility.STORE_CREDIT;",
      "    return ReturnEligibility.INELIGIBLE;",
      "}",
      "",
      "// Plausible-wrong: one operator wrong on the first boundary.",
      "public ReturnEligibility classify(int daysSinceDelivery) {",
      "    if (daysSinceDelivery <= 30) return ReturnEligibility.FULL_REFUND; // off-by-one",
      "    if (daysSinceDelivery < 60)  return ReturnEligibility.STORE_CREDIT;",
      "    return ReturnEligibility.INELIGIBLE;",
      "}",
    ],
    "On day 30 the plausible-wrong strategy credits a full $200 refund instead of $160 in store credit.");
  addNotes(s,
    "Both implementations have identical structure, an identical number of branches, and the same second " +
    "comparison; only the operator on the first branch differs by one character, and <= reads as an entirely " +
    "reasonable choice for an inclusive-sounding boundary if you're not holding the exact policy wording in " +
    "your head while reviewing it. A parameterized test driving both strategies through the full boundary " +
    "table, day 0, 29, 30, 59, and 60, catches the single disagreeing case at exactly day 30; every other " +
    "case still matches, which is exactly what makes a one-operator bug this easy to miss on a casual read " +
    "and this reliable to catch with a table that tests every boundary rather than one value comfortably " +
    "inside each tier. Turned into a refund policy on a $200 order, that one operator is a $40 overpayment, " +
    "traceable to one character.");
}

{
  const s = S();
  addTwoColBullets(s, [
    "A specification problem (deciding what the system should do) is a liability for an assistant, not a shortcut: a plausible-sounding wrong answer to an unclear requirement looks finished and is more dangerous than an obviously incomplete one",
    "A change touching shared mutable state or an invariant spanning files the model was not shown is bounded by what fits in its context window, not by what the system guarantees",
  ], [
    "When the bottleneck is a design decision, which pattern the forces call for, delegating it doesn't speed up the work; it just moves the judgment call to someone less equipped to make it",
    "The judgment calls that stay with the engineer: naming the forces present, deciding whether a pattern's cost is worth paying, and accepting responsibility for the answer once it ships",
  ], { fontSize: 15 });
  addContentTitle(s, "WORKING WITH AI & LLM TOOLS", "When not to reach for an assistant");
  addNotes(s,
    "An AI pairing partner is worth reaching for on a task with a clear specification and a cheap way to " +
    "check the answer: a method whose contract is well understood, a test suite that can be written or " +
    "already exists, a refactor whose behavior preservation can be proven mechanically. An assistant can " +
    "draft any pattern in this book's catalogue in seconds and can draft the test suite that checks its own " +
    "draft just as fast. It cannot tell you whether the problem in front of you needed a pattern at all, and " +
    "it has no stake in being right. That stake is the one thing no amount of tooling capability can " +
    "delegate.");
}

// =============================================================================
// Close
// =============================================================================
{
  const s = S();
  addContentTitle(s, "CLOSING", "Test it, change it safely, let AI accelerate");
  addBullets(s, [
    "Test it: JUnit 5, Mockito, and AssertJ give every pattern a proof, not just a compile",
    "Change it safely: characterize, open a seam, then refactor, with the suite green at every step",
    "Let AI accelerate: name the forces and the pattern, then gate every draft on a behavior suite and an idiomatic review",
    "The judgment that stays with the engineer: whether a problem needed a pattern at all, and who answers for the code once it ships",
  ], { fontSize: 17 });
  addNotes(s,
    "A pattern catalogue by itself only ever answers 'what shape should this code take.' This deck covers " +
    "the three questions a catalogue alone never answers: does it actually do what it claims, is it safe to " +
    "change once it's inherited, and how do you get useful work out of an AI pairing partner without handing " +
    "it the one decision it has no stake in. An assistant can draft any pattern in this book's catalogue in " +
    "seconds and can draft the test suite that checks its own draft just as fast. It can't tell you whether " +
    "the problem in front of you needed a pattern at all, and that judgment call is the one thing this deck " +
    "cannot delegate to tooling, now or later.");
}

// =============================================================================
// Appendix
// =============================================================================
divider("04", "Appendix", "Reference tables: the test-double taxonomy and the test-smell catalogue",
  "Two lookup tables for after the talk. The first maps Meszaros's five-kind test-double taxonomy, dummy, " +
  "stub, spy, mock, fake, onto the exact JUnit 5, Mockito, and hand-written-fake vocabulary this deck's " +
  "examples use. The second is the test-smell catalogue, each smell paired with the fix this stack applies. " +
  "Neither table introduces new material; both exist purely as a reference to return to once a real piece of " +
  "inherited code is in front of you and you need the exact name for the seam or the fix it calls for.");

{
  const s = S();
  addStatusTable(s, [
    { code: "Dummy", name: "A property of what one test exercises, not the object", purpose: "A BookingIdGenerator lambda every other test consults, unused in exactly the test whose shipment fails validation first" },
    { code: "Stub", name: "Returns canned values, no call verification", purpose: "Clock.fixed(overdueAt, zone); the test never checks how many times instant() was called" },
    { code: "Spy", name: "Records calls, inspected after the fact, no expectation declared up front", purpose: "A hand-written RecordingCarrierGateway, distinct from Mockito's own spy() method" },
    { code: "Mock", name: "Verifies an expected interaction, fails if unmet", purpose: "@Mock RateLedger, judged by whether it was called, not only by what it returns" },
    { code: "Fake", name: "A correct, lightweight implementation, unsuited to production", purpose: "A FakeCarrierGateway with a real queryable map and no SMTP connection behind it" },
  ], { colW: [1.70, 5.00, 5.39], rowH: 0.57 });
  addContentTitle(s, "APPENDIX", "The test-double taxonomy, mapped to this stack");
  addNotes(s,
    "Mockito's own spy() method is not the same concept as Meszaros's Spy, even though the two share a word: " +
    "spy() wraps a real object and lets specific methods be forced while every other method keeps the real " +
    "object's behavior, sitting closer to a Fake-plus-Mock hybrid in practice, while a Meszaros Spy is hand-" +
    "written and records what happened with no scripted answer and no expectation declared beforehand. The " +
    "dividing line between a Mock and a Fake is usually this: if the next sentence after 'this test checks " +
    "that' names a method and an argument, reach for a mock; if it names a value the collaborator should be " +
    "holding afterward, reach for a fake.");
}

{
  const s = S();
  addStatusTable(s, [
    { code: "Fragile test", name: "A behavior-preserving refactor still breaks it", purpose: "Depend on the collaborator's interface, never a concrete implementation's internals" },
    { code: "Erratic test", name: "Passes or fails by run order or wall-clock timing", purpose: "Inject a Clock; keep process-wide state like a counter transient, not shared across tests" },
    { code: "Obscure test", name: "A reader cannot tell what behavior a failure indicates", purpose: "One behavior per @Test, a @DisplayName naming it, @Nested classes grouping related scenarios" },
    { code: "Conditional test logic", name: "A test branches on if/else or loops over cases", purpose: "@ParameterizedTest with @MethodSource, not a loop that can silently skip a case" },
    { code: "Slow test", name: "Milliseconds of logic, seconds of runtime", purpose: "A fake in-memory collaborator, or a spy with the expensive call forced, replaces a real connection; tag I/O-bound tests and keep them out of the default run" },
  ], { colW: [2.70, 4.10, 5.29], rowH: 0.57 });
  addContentTitle(s, "APPENDIX", "The test-smell catalogue, and the fix this stack applies");
  addNotes(s,
    "Meszaros catalogs dozens of test smells; these five show up most often in a suite built on JUnit 5, " +
    "Mockito, and AssertJ. A shared, @BeforeAll-built fixture is tempting when a collaborator is expensive to " +
    "build, but it reintroduces the exact problem fresh, per-method fixtures exist to avoid the moment that " +
    "collaborator is mutable, since state one test leaves behind becomes visible to the next test in the same " +
    "class. Reach for @BeforeAll only when the collaborator is read-only for the whole class's lifetime, and " +
    "reach for an explicit reset hook, called in both @BeforeEach and @AfterEach, the moment any shared state " +
    "is mutable, because a failed assertion partway through a test still has to leave that state known for " +
    "the next one.");
}

// =============================================================================
pres.writeFile({ fileName: OUT })
  .then(p => console.log("WROTE", p))
  .catch(e => { console.error(e); process.exit(1); });
