// deck-201.js -- "Design Patterns with Java" / 201: Idiomatic Java 25 Implementations
// Red Hat-branded 16:9 companion deck. Build: node deck-201.js (from this dir).

"use strict";

const H = require("./deck-helpers.js");
const {
  COLOR, FONT, W, ASSETS,
  newDeck, addFooter, addContentTitle, addBullets, addTwoColBullets,
  addStatusTable, addCaption, addCodeSlide, addDiagramSlide, addSectionDivider, addNotes,
} = H;

const OUT = "./design-patterns-with-java-201-idiomatic-java.pptx";
const REV = "r01.0";

const pres = newDeck();
pres.title = "Design Patterns with Java -- 201: Idiomatic Java 25 Implementations";
let pageNum = 0;

function S() {
  const s = pres.addSlide();
  pageNum += 1;
  addFooter(s, pageNum);
  return s;
}
function divider(code, title, subtitle, notes) {
  const s = pres.addSlide();
  pageNum += 1;
  addSectionDivider(s, code, title, subtitle);
  addNotes(s, notes);
}

// ============================================================================
// COVER
// ============================================================================
{
  const s = pres.addSlide();
  pageNum += 1;
  s.background = { color: COLOR.white };
  try { s.addImage({ path: `${ASSETS}/cover-panel.png`, x: 0, y: 0, w: W, h: 7.5 }); } catch (e) {}
  s.addText("JAVA 25 COMPANION DECK", {
    x: 6.00, y: 1.98, w: 6.90, h: 0.34,
    fontFace: FONT.title, fontSize: 14, bold: true, color: COLOR.red, charSpacing: 6,
    align: "left", valign: "middle",
  });
  s.addText([
    { text: "Design Patterns with Java", options: { breakLine: true } },
    { text: "201: Idiomatic Java 25 Implementations" },
  ], {
    x: 5.95, y: 2.20, w: 6.95, h: 2.65, fontFace: FONT.title, fontSize: 32, bold: true,
    color: COLOR.ink, align: "left", valign: "top", lineSpacingMultiple: 1.08,
  });
  s.addText("How the patterns you already know get expressed in idiomatic Java 25.", {
    x: 6.00, y: 5.05, w: 6.70, h: 0.75,
    fontFace: FONT.body, fontSize: 18, italic: true, color: COLOR.caption, align: "left", valign: "top",
  });
  s.addText(REV, { x: 11.85, y: 5.85, w: 0.95, h: 0.30, fontFace: FONT.mono, fontSize: 11, color: COLOR.caption, align: "right", valign: "middle" });
  try { s.addImage({ path: `${ASSETS}/logo-candidate-2.png`, x: 11.10, y: 6.80, w: 1.55, h: 0.37 }); } catch (e) {}
  addNotes(s,
    "This deck is for engineers who already know Strategy, State, Visitor, Factory, Builder, and " +
    "Singleton conceptually and want to see how Java 25 actually expresses them: records and sealed " +
    "interfaces closing a varying hierarchy, an exhaustive pattern-matching switch replacing an " +
    "accept-and-visit pair, enum constants and lambdas satisfying Strategy and Template Method with no " +
    "new class file, Optional composing past a missing answer with no Null Object anywhere, and virtual " +
    "threads making blocking I/O nearly free. Close with the same five forces answered consistently by a " +
    "real framework's own composition root, Quarkus and CDI. Every code slide is a trimmed, real snippet " +
    "from a shipping and logistics domain: parcels, manifests, carriers, rate cards, tracking feeds. " +
    "Nothing here is a placeholder class.");
}

// ============================================================================
// SECTION 00: THE JAVA SUBSTRATE
// ============================================================================
divider("00", "The Java substrate",
  "Resource cleanup and encapsulation, the foundation every idiom in this deck builds on.",
  "This section is review for this audience, but every idiomatic expression that follows depends on " +
  "two mechanisms landing solidly first. Java has no destructor, so deterministic cleanup is an explicit " +
  "choice a type makes by implementing AutoCloseable, not a guarantee the language makes automatically; " +
  "try-with-resources and the Execute-Around Method idiom are the two shapes that choice takes. " +
  "Encapsulation is the second: a public interface over a package-private implementation, a static " +
  "factory method, a record's implicitly final fields, and List.copyOf for a collection field are four " +
  "separate tools that close four separate ways a type's internal state can leak past the boundary it is " +
  "supposed to draw. Keep this section brief in the room; the payoff is everything that follows.");

{
  const s = S();
  addContentTitle(s, "SUBSTRATE · RESOURCE MANAGEMENT", "Deterministic cleanup with no destructor");
  addBullets(s, [
    "Java has no destructor; try-with-resources over AutoCloseable is the primary, explicit answer: a compiler-generated release that runs on a normal return and on an exception unwinding the block.",
    "Finalizers and the Cleaner API run on the garbage collector's own schedule, never a program's; both are a safety net behind close(), never a primary release mechanism.",
    "The Execute-Around Method idiom moves a whole open, use, and close sequence into one library method that takes the caller's one varying step as a lambda, so there is nowhere left for a call site to forget the commit or skip the close.",
    "A resource whose life is supposed to outlast one call, a pooled connection, a tracking-feed subscription, belongs to a registry or a pool closed once, not a per-call try-with-resources block.",
  ], { fontSize: 17 });
  addNotes(s,
    "Lead with the cost: a connection to a carrier's booking API is a socket the carrier meters per " +
    "tenant, and the garbage collector reclaiming the CarrierConnection object eventually tells a reader " +
    "nothing about when the socket itself closes. Try-with-resources closes that gap by generating the " +
    "release at a fixed, specified point in the source, the moment the block's scope ends, whether that " +
    "end is a normal return or a thrown, checked exception. Flag why finalize() and Cleaner never " +
    "substitute for that guarantee: neither runs on a schedule a program can rely on, so a resource whose " +
    "only release path is garbage-collector-driven can stay open for the rest of a batch job's entire " +
    "run. Execute-Around is the one level up from try-with-resources: once an open-use-close sequence " +
    "repeats across call sites, a shared method removes every place that sequence could drift.");
}
{
  const s = S();
  addDiagramSlide(s, "SUBSTRATE · RESOURCE MANAGEMENT", "A try-with-resources scope guarantees close() runs",
    "05-resource-lifecycle",
    "A thrown exception still funnels into the same compiler-generated release as a clean exit.");
  addNotes(s,
    "Walk the diagram left to right: a connection opens, the happy path returns normally and close() " +
    "runs, and the exception path still ends at the same close() call during the block's unwinding. The " +
    "point worth landing is that the Java Language Specification defines try-with-resources as sugar for " +
    "a try/finally form the compiler generates, not something a reader hand-verifies by tracing every " +
    "branch of a finally clause for a forgotten call or a statement that throws before reaching it. A " +
    "hand-written try/finally still works, but it has a close() call in the source for a future edit to " +
    "delete or move; try-with-resources has no such call anywhere in the source for anyone to get wrong.");
}
{
  const s = S();
  addCodeSlide(s, "SUBSTRATE · RESOURCE MANAGEMENT", "A compiler-generated release, and a shared open, use, close sequence", "Java 25",
    [
      "// try-with-resources: close() runs on both the normal path and",
      "// a thrown, checked exception unwinding the block.",
      "try (CarrierConnection connection = CarrierConnection.open(endpoint)) {",
      "    ManifestReceipt receipt = connection.submitManifest(manifest);",
      "    System.out.println(\"Receipt: \" + receipt);",
      "} catch (ManifestRejectedException e) {",
      "    System.out.println(\"Rejected: \" + e.getMessage());",
      "}",
      "",
      "// Execute-Around Method: the whole open/commit/close sequence",
      "// lives in one method; a caller supplies only what varies.",
      "LedgerOperations.withLedger(ledgerPath, writer -> {",
      "    writer.writeEntry(batch);",
      "    return null;",
      "});",
    ],
    "Neither snippet names close() or commit() at the call site; both run from inside the method that acquired the resource.",
    { fontSize: 11 });
  addNotes(s,
    "The top block is the common case, one method opens a resource, uses it, and releases it in the same " +
    "block. The bottom block is the Execute-Around Method idiom answering a different shape: every place " +
    "this codebase writes a shipment batch needs to open a writer, write entries, commit, and close, in " +
    "that exact order, every time. withLedger owns all four steps; the lambda a caller supplies is the " +
    "only part that varies. A Mockito spy proves both guarantees directly rather than by inspection: " +
    "close() runs exactly once even when the caller's lambda throws, and the writer's committed flag " +
    "stays false when that happens, so a failed write never masquerades as a recorded one.");
}
{
  const s = S();
  addContentTitle(s, "SUBSTRATE · ENCAPSULATION", "A boundary a caller cannot see past");
  addBullets(s, [
    "A public interface with a package-private implementation is Java's own PImpl: a caller depends on a contract, and javac rejects a direct reference to the concrete class from outside the package outright.",
    "A static factory method does what a public constructor cannot: name itself, cache and return a shared instance, or hand back a subtype the caller never names.",
    "A record's fields are implicitly final; equals, hashCode, toString, and every accessor derive from one component list instead of four or five hand-written pieces that can drift out of sync.",
    "List.copyOf in a compact constructor closes both defensive-copying gaps in one call: a caller's later mutation of the source list cannot reach in, and a caller holding the accessor's return value cannot mutate it back out.",
  ], { fontSize: 17 });
  addNotes(s,
    "Set up the stakes: a shipment's tracking history moves through dozens of hands before a package " +
    "arrives, and a caller that can mutate a tracking record it was only supposed to read produces a bug " +
    "report with no way to establish what the record said when a dispute started. ShippingGateway is a " +
    "public interface with exactly one static factory, inMemory(); the class behind it carries no access " +
    "modifier at all, which in Java means package-private, so nothing outside that package can construct " +
    "it or cast a ShippingGateway reference down to it. That is a guarantee the compiler enforces, not " +
    "one a reviewer has to check by reading every file that touches shipping.");
}
{
  const s = S();
  addDiagramSlide(s, "SUBSTRATE · ENCAPSULATION", "A caller depends on the interface, never the concrete class",
    "06-encapsulation-boundary",
    "The concrete implementation and the representation it owns live entirely behind the package boundary.");
  addNotes(s,
    "ShippingGateway.inMemory() returns a ShippingGateway; a caller's variable is typed to the interface " +
    "and the concrete InMemoryShippingGateway class is never named outside its own package. That changes " +
    "what swapping the implementation later costs: a caller coupled to one concrete gateway class is " +
    "coupled to it forever, while a caller coupled to a three-method interface stays unaffected when a " +
    "database-backed implementation replaces the in-memory one behind the same factory. A second test " +
    "implementation, written entirely in the test tree and satisfying the identical interface, proves the " +
    "contract is what every caller and every alternate implementation actually agrees on.");
}
{
  const s = S();
  addCodeSlide(s, "SUBSTRATE · ENCAPSULATION", "A cached static factory, and a defended collection field", "Java 25",
    [
      "public interface ShippingGateway {",
      "    static ShippingGateway inMemory() {",
      "        return new InMemoryShippingGateway();  // package-private",
      "    }",
      "}",
      "",
      "public record TrackingHistory(List<TrackingEvent> events) {",
      "    private static final TrackingHistory EMPTY = new TrackingHistory(List.of());",
      "",
      "    public TrackingHistory {",
      "        events = List.copyOf(events);",
      "    }",
      "",
      "    public static TrackingHistory empty() {",
      "        return EMPTY;  // TrackingHistory.empty() == TrackingHistory.empty()",
      "    }",
      "}",
    ],
    "A plain constructor can never return a cached instance; only a named static factory can.",
    { fontSize: 10 });
  addNotes(s,
    "inMemory() can return a subtype the caller never names because a static factory, unlike a " +
    "constructor, is not required to return the exact type it is declared on. TrackingHistory.empty() " +
    "demonstrates the caching capability a constructor flatly lacks: calling it twice returns the " +
    "identical object reference, proven directly in the example's demo output, where " +
    "new TrackingHistory(List.of()) would always allocate. List.copyOf in the compact constructor does " +
    "two jobs in one line, copying whatever list a caller passed in so a later mutation of the original " +
    "cannot reach this record's state, and wrapping that copy as immutable so a caller holding the " +
    "accessor's return value cannot mutate it either. A test proves both halves as separate, observable " +
    "facts: a caller's list growing after construction leaves the record unchanged, and calling add() on " +
    "what the accessor returns throws UnsupportedOperationException.");
}

// ============================================================================
// SECTION 01: IDIOMATIC JAVA 25 EXPRESSIONS
// ============================================================================
divider("01", "Idiomatic Java 25 expressions",
  "Records, sealed interfaces, pattern matching, enums, and functional interfaces as the patterns themselves.",
  "This section is data-oriented programming, pulled together as one lesson instead of several separate " +
  "idioms learned in isolation. Records carry data and validate it once in a compact constructor; sealed " +
  "interfaces close a hierarchy so the compiler, not a convention, enforces its boundaries; a " +
  "pattern-matching switch asks which variant and what parts of it in one expression, checked for " +
  "completeness at compile time. Enum constants, lambdas, method references, and streams answer Strategy, " +
  "Singleton, Command, and Template Method with everyday constructs. The section closes by naming, out " +
  "loud, which classic hand-written idiom each Java 25 feature retires for new code: double-checked " +
  "locking, the hand-rolled value class, Visitor's accept-and-visit pair, and the Null Object.");

{
  const s = S();
  addContentTitle(s, "IDIOMS · RECORDS & SEALED INTERFACES", "A closed set the compiler checks, not a convention");
  addBullets(s, [
    "A record's compact constructor validates and normalizes every construction path once, in exactly one place; nothing is left to a second constructor or a forgotten setter.",
    "A sealed interface's permits clause is an instruction to javac, not documentation: no type outside the list can ever implement it, from any package or module, at compile time or through any runtime trick.",
    "An exhaustive switch over a sealed interface, with no default branch, fails to compile the moment a permitted type goes unhandled, turning a missed case into a build failure instead of a production surprise.",
    "Record deconstruction patterns nest arbitrarily deep, reading a far-inside component straight into a name with no accessor call anywhere in the arm.",
  ], { fontSize: 17 });
  addNotes(s,
    "Set up the rating-engine scenario: a parcel's kind, a shipping event's type, and a rate card's " +
    "structure are three separate closed sets, and a record for the data plus a sealed interface for the " +
    "closed set plus a pattern-matching switch for the operation answers all three with the same small " +
    "toolkit. RefrigeratedParcel's compact constructor rejects a non-positive weight and an inverted " +
    "temperature band in the one place a negative value could ever be constructed; no setter, no builder, " +
    "and no second constructor can skip it. The permits clause is the stronger half of the claim: a " +
    "fifth parcel kind added to the sealed interface without a matching switch arm anywhere in the " +
    "codebase fails every one of those switches at compile time, caught by whichever developer runs the " +
    "build next, not by whichever shipment reaches the missing case first.");
}
{
  const s = S();
  addDiagramSlide(s, "IDIOMS · RECORDS & SEALED INTERFACES", "Four record variants, three exhaustive operations, no accept() method",
    "21-records-sealed-switches",
    "Behavior lives in static methods, each its own exhaustive switch over the sealed set.");
  addNotes(s,
    "ParcelKind's four record variants sit on one side, and three separate operations classes sit on the " +
    "other, each one an exhaustive switch over the identical sealed set. Nothing here declares a visitor " +
    "interface and nothing on any of the twelve record types across the three hierarchies declares an " +
    "accept() method. That absence is the entire argument: adding a new operation tomorrow touches only " +
    "the operations class gaining it, with every existing record left untouched, the same guarantee " +
    "classic Visitor gives through an accept-and-visit pair, with one fewer interface and zero boilerplate " +
    "methods on the data itself.");
}
{
  const s = S();
  addCodeSlide(s, "IDIOMS · RECORDS & SEALED INTERFACES", "An exhaustive switch, and a guard that narrows one arm further", "Java 25",
    [
      "public sealed interface ParcelKind",
      "        permits StandardParcel, FragileParcel, RefrigeratedParcel, OversizedParcel {",
      "    double weightKg();",
      "}",
      "",
      "public static double billableWeightKg(ParcelKind parcel) {",
      "    return switch (parcel) {",
      "        case OversizedParcel(var w, var l, var wd, var h) ->",
      "                Math.max(w, (l * wd * h) / VOLUMETRIC_DIVISOR);",
      "        case StandardParcel(var w) -> w;",
      "        case FragileParcel(var w, var note) -> w;",
      "        case RefrigeratedParcel(var w, var min, var max) -> w;",
      "    };  // no default: a fifth record fails the build until handled",
      "}",
      "",
      "case DeliveryException(RefrigeratedParcel parcel, var reason)",
      "        when parcel.maxTempC() <= COLD_CHAIN_CEILING_C -> true;",
    ],
    "The guard runs only once the type pattern already matched; a failed guard falls through, not errors.",
    { fontSize: 10 });
  addNotes(s,
    "billableWeightKg deconstructs OversizedParcel directly into its four components and computes a " +
    "dimensional weight inline, with no accessor call anywhere in that arm. The last line is a second, " +
    "separate idiom stacked on top: a type pattern that narrows DeliveryException's parcel component to " +
    "specifically a RefrigeratedParcel, followed by a guarded when clause that only runs once that type " +
    "check already passed. A RefrigeratedParcel whose temperature ceiling is comfortably above freezing " +
    "falls through to a plainer arm immediately below; a guarded pattern that fails its guard is a " +
    "non-match, not an error, and switch keeps trying the remaining arms in order exactly as written.");
}
{
  const s = S();
  addCodeSlide(s, "IDIOMS · RECORDS & SEALED INTERFACES", "Nesting decides the result: Interpreter with no evaluate() method", "Java 25",
    [
      "public static Money quote(RateRule rule, ParcelKind parcel, String zone) {",
      "    return switch (rule) {",
      "        case FlatRate(var amount) -> amount;",
      "        case PerKilogram(var rate) -> rate.timesWeight(billableWeightKg(parcel));",
      "        case ZoneSurcharge(var base, var z, var fee) when z.equals(zone) ->",
      "                quote(base, parcel, zone).plus(fee);",
      "        case ZoneSurcharge(var base, var z, var fee) -> quote(base, parcel, zone);",
      "        case MinimumCharge(var base, var floor) -> {",
      "            Money computed = quote(base, parcel, zone);",
      "            yield computed.isGreaterThan(floor) ? computed : floor;",
      "        }",
      "    };",
      "}",
      "// Wrapping a floor around a surcharge, or a surcharge around a floor,",
      "// quotes two different totals from the identical three pieces.",
    ],
    "MinimumCharge wrapping a ZoneSurcharge and a ZoneSurcharge wrapping a MinimumCharge price differently.",
    { fontSize: 10 });
  addNotes(s,
    "RateRule's four kinds are built to nest around each other, and quote() walks whatever tree a rate " +
    "card author built with ordinary recursion, one layer of pattern per call. This is Interpreter's whole " +
    "claim, that the structure itself, not a separate configuration flag, decides what an expression " +
    "evaluates to, done here with the same record-pattern mechanism this deck already used for " +
    "ParcelKind. A test quotes the identical twelve-kilogram shipment to the identical zone through both " +
    "nestings and asserts two different totals, twenty-three dollars one way, twenty dollars the other, " +
    "which is the claim made visible as a number rather than inferred from reading the nesting by eye.");
}
{
  const s = S();
  addContentTitle(s, "IDIOMS · ENUMS, LAMBDAS & STREAMS", "Strategy, Singleton, and Template Method with no new class");
  addBullets(s, [
    "An enum constant with its own method body is Strategy, a closed, compiler-checked set of algorithms with no interface-plus-three-classes ceremony.",
    "A single-element enum is Singleton with no synchronized block, no volatile field, and no readResolve() to remember: the language guarantees exactly one instance through class loading, reflection, and serialization alike.",
    "A one-method functional interface satisfied by a lambda, a method reference, or an enum constant is indistinguishable to the field that holds it; a fixed sequence that takes its two varying steps as Function parameters is Template Method with no abstract base class.",
    "UnaryOperator inherits andThen and compose with no extra code; the two orders do not commute, and the order still encodes a real business decision.",
  ], { fontSize: 17 });
  addNotes(s,
    "The running example is a nightly fulfillment batch: price a packaging surcharge, validate an order, " +
    "price the shipment, assign a manifest number, notify whoever is watching. None of that needed a new " +
    "interface hierarchy. The skill this whole deck trains is naming the force correctly and picking the " +
    "idiom whose shape matches it; a team that reaches for a new interface and three implementing classes " +
    "every time it needs to vary an algorithm is paying a tax the language stopped charging years ago. " +
    "Emphasize the closed-versus-open test directly: PackagingTier's three tiers are the entire, fixed set " +
    "of ways this warehouse packs a box, which earns the enum form, while a holiday surcharge built once " +
    "for a single sales campaign earns the lambda form, with no file anywhere named for it.");
}
{
  const s = S();
  addDiagramSlide(s, "IDIOMS · ENUMS, LAMBDAS & STREAMS", "A nightly batch run through four everyday constructs",
    "22-functional-java-pipeline",
    "Each construct does the work a named pattern usually needs a dedicated class or hierarchy to do.");
  addNotes(s,
    "Point at each stage of the batch and name the construct answering it: an enum carrying its own " +
    "surcharge algorithm, a lambda satisfying the identical contract for a seasonal rule, a function " +
    "standing in for Template Method's two varying steps, a stream pipeline replacing an accumulator loop, " +
    "and Optional as an explicit answer instead of null for a zone with no base rate on file. None of these " +
    "five stages needed a new interface hierarchy to be written; every one of them is a construct a Java " +
    "developer has very likely already used this week without naming it.");
}
{
  const s = S();
  addCodeSlide(s, "IDIOMS · ENUMS, LAMBDAS & STREAMS", "A closed Strategy, and a Singleton with no ceremony", "Java 25",
    [
      "public enum PackagingTier implements SurchargeRule {",
      "    STANDARD { public long surchargeCents(Shipment s) { return 0L; } },",
      "    FRAGILE {",
      "        public long surchargeCents(Shipment s) {",
      "            return 350L + Math.round(s.weightKg() * 40);",
      "        }",
      "    },",
      "    OVERSIZED {",
      "        public long surchargeCents(Shipment s) {",
      "            return 900L + Math.round(s.weightKg() * 75);",
      "        }",
      "    }",
      "}",
      "",
      "public enum ManifestSequence {",
      "    INSTANCE;",
      "    private final AtomicLong issuedCount = new AtomicLong();",
      "",
      "    public String nextManifestId() {",
      "        return \"MANIFEST-%06d\".formatted(issuedCount.incrementAndGet());",
      "    }",
      "}",
    ],
    "A fourth tier with no surchargeCents body is a compile error, not a shipment priced at zero.",
    { fontSize: 9 });
  addNotes(s,
    "PackagingTier is Strategy, a varying algorithm selected by which constant a shipment carries, " +
    "expressed as a closed set of three enum constants rather than three named classes behind an " +
    "interface. ManifestSequence is a separate, unrelated force getting the identical free ride from " +
    "enum: the language guarantees exactly one INSTANCE exists, a guarantee reflection and serialization " +
    "both respect specifically because enum is a language feature, not library code imitating one. " +
    "AtomicLong inside that single instance is what makes concurrent access to the counter itself safe, a " +
    "separate concern from how many instances of ManifestSequence can ever exist.");
}
{
  const s = S();
  addCodeSlide(s, "IDIOMS · ENUMS, LAMBDAS & STREAMS", "Composed adjustments: order is a business decision, not a style choice", "Java 25",
    [
      "UnaryOperator<Money> discount = PricingAdjustments.percentDiscount(0.10);",
      "UnaryOperator<Money> fee = PricingAdjustments.flatHandlingFee(500);",
      "",
      "discount.andThen(fee).apply(startingPrice);   // discount first, then the fee",
      "discount.compose(fee).apply(startingPrice);   // the fee first, then discount",
      "",
      "// Discount then fee (andThen):  $95.00",
      "// Fee then discount (compose):  $94.50",
      "// Same starting price, two different totals, from the identical two steps.",
    ],
    "A ten percent discount applied before a flat fee never discounts that fee; applied after, it does.",
    { fontSize: 12 });
  addNotes(s,
    "UnaryOperator inherits andThen and compose from Function with no code of this example's own, and " +
    "composing functions reads as declarative math. The point this slide exists to make is that the order " +
    "inside that composition still encodes a real business decision: applying the discount first means the " +
    "flat handling fee is added afterward at full price, while applying the fee first means the discount " +
    "scales the fee along with the rest of the order. A codebase that reaches for andThen without checking " +
    "which order the business rule actually requires can produce a quote that is off by exactly the fee " +
    "amount's discount share, a bug that passes every test built against only one of the two orders.");
}
{
  const s = S();
  addContentTitle(s, "IDIOMS · RETIRING CLASSIC IDIOMS", "What Java 25 makes unnecessary for new code");
  addBullets(s, [
    "An enum singleton retires double-checked locking: no volatile field, no double check, no three facts an author has to get right together.",
    "A record retires the hand-written value class: a compact constructor replaces a constructor, four accessors, equals, hashCode, and toString written and kept in sync by hand.",
    "A sealed interface and an exhaustive switch retire Visitor's accept-and-visit pair and the instanceof chain's silent default: a missing case fails the build, not a shipment in production.",
    "Optional retires the Null Object and the scattered null check: map and orElse compose past a possible absence with no stand-in class and no forgotten check.",
  ], { fontSize: 17 });
  addNotes(s,
    "This is the through-line pulled together into one lesson: every step in this book's own arc has " +
    "trained recognizing a force, then building the idiomatic Java answer to it, and for a long stretch of " +
    "the language's history that answer meant writing something by hand. A warehouse system that hands " +
    "packages to carriers needs exactly one source of confirmation numbers; before an enum singleton was " +
    "common knowledge, that force resolved to double-checked locking, a volatile field, and an author who " +
    "remembers why both are required together. The method has not changed, recognize the force and reach " +
    "for the idiom that answers it; what changed is which idiom answers it now, with less code and a " +
    "stronger compiler or runtime guarantee than the idiom it replaces ever offered.");
}
{
  const s = S();
  addDiagramSlide(s, "IDIOMS · RETIRING CLASSIC IDIOMS", "Six idioms built by hand, each retired by one Java 25 feature",
    "23-idiom-retirement-map",
    "Each retirement gets the identical guarantee from the compiler or the runtime instead of from an author's discipline.");
  addNotes(s,
    "Walk the map left to right: double-checked locking retires to an enum singleton, a hand-rolled value " +
    "class retires to a record, the Visitor pattern and the instanceof chain both retire to a sealed " +
    "interface with an exhaustive switch, and the Null Object retires to Optional composed with map and " +
    "orElse. None of these four retirements is unconditional; a resource that must be reopened or rotated " +
    "at runtime still does not fit either modern singleton shape, and a plugin architecture that needs to " +
    "extend a type set at runtime still needs an open interface, not a sealed one. The judgment behind " +
    "every one of these is the same judgment this whole deck trains: name the force present in the " +
    "specific code at hand, and let that force choose the idiom.");
}
{
  const s = S();
  addContentTitle(s, "IDIOMS · RETIRING CLASSIC IDIOMS", "What used to be hand discipline is now a language guarantee");
  addStatusTable(s, [
    { code: "Exactly one instance", name: "Double-checked locking (volatile + synchronized)", purpose: "Single-element enum singleton" },
    { code: "An immutable value type", name: "Hand-written equals, hashCode, toString", purpose: "A record's compact constructor" },
    { code: "A closed, varying hierarchy", name: "Visitor accept()/visit(), or an instanceof chain", purpose: "Sealed interface + exhaustive switch" },
    { code: "A lookup that may not answer", name: "null, or a Null Object stand-in", purpose: "Optional, composed with map/orElse" },
  ], { colW: [3.6, 4.7, 3.79], rowH: 0.85 });
  addNotes(s,
    "Use this as the lookup slide the retirement diagram just taught: every row names a force on the " +
    "left, the idiom that used to resolve it in the middle, and the Java 25 feature that resolves it now " +
    "on the right. None of these four retirements is a hand-wave at a newer, trendier style; each one " +
    "closes a specific, nameable gap the older idiom left open to a reviewer's memory, a volatile field " +
    "that has to be remembered, a sixth accessor that has to be kept in sync by hand, a default branch " +
    "that silently absorbs an unhandled case, a null check a caller forgets on the one input path that " +
    "exercises it. The feature map at the end of this deck extends this same table with the resource, " +
    "concurrency, and dispatch idioms the rest of this deck still has to cover.");
}

// ============================================================================
// SECTION 02: PATTERNS, EXPRESSED IN JAVA
// ============================================================================
divider("02", "Patterns, expressed in Java",
  "The classic catalogue, rewritten in the idioms this deck just built.",
  "This section is the payoff: Strategy, Factory Method, Abstract Factory, Builder, Prototype, " +
  "Singleton, and dependency injection, each shown in the Java idiom that actually ships in this " +
  "codebase. State, Visitor, and Interpreter already got their idiomatic Java treatment in the previous " +
  "section through sealed interfaces and exhaustive switches, so this section does not repeat them. " +
  "Strategy gets three runtime-swappable expressions, a named class behind an interface, a lambda, and a " +
  "closed enum. Abstract Factory assembles a matching family of products behind one factory; Factory " +
  "Method decides one product inside a fixed sequence. Builder validates once at build(); a record's " +
  "wither and Prototype's copy constructor answer two smaller, related forces. Singleton and dependency " +
  "injection turn out to be answers to two different questions, not competing answers to one.");

{
  const s = S();
  addContentTitle(s, "PATTERNS · OVERVIEW", "Same forces, idiomatic Java resolutions");
  addBullets(s, [
    "Strategy: an algorithm that varies independently of its caller, resolved as a named class behind an interface, a lambda assigned to a functional interface, or a closed set of enum constants.",
    "State, Visitor, and Interpreter: a closed, varying structure, already resolved by a sealed interface and an exhaustive pattern-matching switch, with no accept() method anywhere.",
    "Factory Method and Abstract Factory: client code that needs one product, or a matching family of products, without naming the concrete class that builds it.",
    "Builder, a record's wither, and Prototype: a product assembled gradually, a focused variation of one already-complete value, and a pre-configured exemplar cloned rather than rebuilt.",
    "Singleton and dependency injection: exactly one instance is a separate question from how a caller reaches it, and only one of the two answers stays substitutable in a test.",
  ], { fontSize: 16 });
  addNotes(s,
    "This slide is the map for the rest of the section. Every pattern here is the one the audience " +
    "already knows from the Gang of Four catalogue; what is new is which concrete Java 25 construct " +
    "resolves each force in this codebase. Keep returning to one discipline across every pattern in this " +
    "section: name the plain alternative, a direct constructor call, a bare field, a single method, next " +
    "to the pattern, and let the forces actually present decide between them, not the pattern's " +
    "familiarity or how often it appears in training data.");
}
{
  const s = S();
  addDiagramSlide(s, "PATTERNS · STRATEGY", "Three resolutions of the same varying algorithm",
    "08-strategy-forces",
    "A named class behind an interface, a lambda, and a closed enum all satisfy the identical RateStrategy contract.");
  addNotes(s,
    "A checkout flow holds exactly one interchangeable shipping-cost strategy at a time, and this diagram " +
    "is the map of the three ways this codebase expresses that: a named class implementing RateStrategy " +
    "for a rule carrying its own constructor state, a lambda for a rule assembled once at a call site and " +
    "discarded, and a closed enum for a fixed set of carriers that changes only when a contract is signed. " +
    "None of the three is the canonical Strategy in this book's catalogue; choosing among them, based on " +
    "whether the set is open or closed and whether a rule needs a name of its own, is itself the design " +
    "decision.");
}
{
  const s = S();
  addCodeSlide(s, "PATTERNS · STRATEGY", "The identical contract, with and without a dedicated class", "Java 25",
    [
      "public interface RateStrategy {",
      "    RateQuote quote(Shipment shipment);",
      "}",
      "",
      "// Named class: a file, a constructor, two fields, before the math runs.",
      "public final class WeightTieredRateStrategy implements RateStrategy {",
      "    private final String carrierName;",
      "    private final long baseCents;",
      "    private final long perKgCents;",
      "    // constructor omitted",
      "    public RateQuote quote(Shipment s) {",
      "        long cents = ShippingMath.weightTieredCents(baseCents, perKgCents, s.weightKg());",
      "        return new RateQuote(carrierName, Money.ofCents(cents));",
      "    }",
      "}",
      "",
      "// Lambda: the identical contract, one expression, no file.",
      "public static RateStrategy weightTiered(String carrier, long base, long perKg) {",
      "    return s -> new RateQuote(carrier,",
      "            Money.ofCents(ShippingMath.weightTieredCents(base, perKg, s.weightKg())));",
      "}",
    ],
    "Both produce byte-for-byte identical RateQuote values; a parameterized test checks it directly.",
    { fontSize: 9 });
  addNotes(s,
    "ShippingRateCalculator, the object that uses a strategy, never mentions WeightTieredRateStrategy by " +
    "name either way; it depends only on RateStrategy, so a new carrier's rule can arrive as a new class or " +
    "a new lambda with nothing in the calculator touched to admit it. Reach for the lambda form when a " +
    "strategy is built, used once or twice, and discarded, especially one assembled from values only known " +
    "at the call site, where a dedicated class would exist purely to hold a constructor nobody reuses. The " +
    "named-class form fits better when a strategy carries enough internal state that giving it a name and " +
    "a file clarifies the code, or when it needs to be discovered by reflection or a dependency-injection " +
    "container that expects a type, not a value.");
}
{
  const s = S();
  addCodeSlide(s, "PATTERNS · STRATEGY", "Composing a lambda needs no decorator class; a closed set earns an enum", "Java 25",
    [
      "public static RateStrategy withInsuranceSurcharge(RateStrategy base, double centsPerDollar) {",
      "    return shipment -> {",
      "        RateQuote baseQuote = base.quote(shipment);",
      "        long surcharge = ShippingMath.insuranceSurchargeCents(",
      "                shipment.declaredValueCents(), centsPerDollar);",
      "        return new RateQuote(baseQuote.quotedBy(), baseQuote.amount().plus(Money.ofCents(surcharge)));",
      "    };",
      "}",
      "",
      "public enum Carrier implements RateStrategy {",
      "    GROUND { public RateQuote quote(Shipment s) { /* ... */ } },",
      "    AIR_FLAT { public RateQuote quote(Shipment s) { /* ... */ } },",
      "    FREIGHT_ZONE { public RateQuote quote(Shipment s) { /* ... */ } }",
      "    // a fourth constant with no quote() body fails to compile",
      "}",
    ],
    "A RateStrategy field cannot tell a lambda from an enum constant; the choice is the design decision.",
    { fontSize: 10 });
  addNotes(s,
    "withInsuranceSurcharge is the capability a lambda adds that the interface-plus-named-classes form " +
    "does not offer without a second pattern: it takes an existing RateStrategy and returns a new one that " +
    "wraps it, because a lambda is already a value closing over whatever state it needs. Getting the " +
    "identical effect from the named-class form means introducing a Decorator class just to wrap one " +
    "RateStrategy around another. Carrier is the opposite force: a company might contract with exactly " +
    "three carriers, a list that changes only when legal signs a new contract, rare enough that requiring " +
    "a recompile to add a fourth is the right tradeoff, trading runtime flexibility for the compiler " +
    "catching a forgotten carrier's missing quote() body before a shipment ever reaches it.");
}
{
  const s = S();
  addDiagramSlide(s, "PATTERNS · FACTORY", "A family that must match, selected by region",
    "12-factory-family-selection",
    "A single ShippingToolkit implementation is a promise every product it returns belongs to one region.");
  addNotes(s,
    "Three regions, North America, the European Union, and Asia-Pacific, each price differently and ask " +
    "for different customs paperwork, and ShippingToolkit's three creation methods guarantee a single " +
    "implementation returns a label formatter, a rate table, and a customs form that all agree on which " +
    "region they belong to. A test checking this directly with a mock ShippingToolkit confirms " +
    "RegionalShippingDesk's constructor calls all three creation methods exactly once each, and that " +
    "process() never calls any of them again, no matter how many shipments it handles afterward. The " +
    "family is fixed the moment the desk is built, not reassembled on every call.");
}
{
  const s = S();
  addCodeSlide(s, "PATTERNS · FACTORY", "One factory, three matching products, no region named downstream", "Java 25",
    [
      "public interface ShippingToolkit {",
      "    LabelFormatter labelFormatter();",
      "    RateTable rateTable();",
      "    CustomsForm customsForm();",
      "}",
      "",
      "public final class RegionalShippingDesk {",
      "    private final LabelFormatter labelFormatter;",
      "    private final RateTable rateTable;",
      "    private final CustomsForm customsForm;",
      "",
      "    public RegionalShippingDesk(ShippingToolkit toolkit) {",
      "        this.labelFormatter = toolkit.labelFormatter();",
      "        this.rateTable = toolkit.rateTable();",
      "        this.customsForm = toolkit.customsForm();",
      "    }",
      "    // process() calls all three; none is ever named by this class",
      "}",
    ],
    "A fourth region needs its own toolkit; RegionalShippingDesk does not change at all.",
    { fontSize: 10 });
  addNotes(s,
    "The product classes behind each region's toolkit stay package-private inside that toolkit's own " +
    "file, a nested record for each stateless product once a hub code is fixed, because nothing outside " +
    "that file is ever meant to name them. RegionalShippingDesk is the client, and its constructor is the " +
    "one place the matching-family guarantee gets made: nothing in process() can reach past the three " +
    "interface-typed fields to ask which region produced them. That is Abstract Factory's job description " +
    "word for word, a single factory call producing a matching family so nothing downstream can end up " +
    "holding a mismatched set from two different regions.");
}
{
  const s = S();
  addCodeSlide(s, "PATTERNS · FACTORY", "Factory Method's lookup: exhaustive today, extensible tomorrow", "Java 25",
    [
      "// A switch is exhaustive over Region today: a fourth region with no",
      "// branch fails to compile.",
      "public static ShippingToolkit forRegion(Region region) {",
      "    return switch (region) {",
      "        case NORTH_AMERICA -> new NorthAmericaToolkit(\"hub-na-01\");",
      "        case EUROPEAN_UNION -> new EuropeanUnionToolkit(\"hub-eu-07\");",
      "        case ASIA_PACIFIC -> new AsiaPacificToolkit(\"hub-apac-14\");",
      "    };",
      "}",
      "",
      "// A Supplier<T> registry trades that compile-time exhaustiveness for a",
      "// table another module can extend with no edit to this method's body.",
      "Map<Region, Supplier<ShippingToolkit>> suppliers = new EnumMap<>(Region.class);",
      "suppliers.put(Region.NORTH_AMERICA, () -> new NorthAmericaToolkit(\"hub-na-01\"));",
    ],
    "Reach for the switch while the set is closed and small; reach for the registry once it can grow at runtime.",
    { fontSize: 10 });
  addNotes(s,
    "Factory Method solves a narrower problem than Abstract Factory: a fixed inspection sequence, " +
    "validate, run one checklist, judge pass or fail, never changes, while which checklist runs depends on " +
    "whether a shipment is standard or hazardous. The lookup shown here is the companion decision, which " +
    "concrete toolkit to build for a given region, shown both idiomatic ways. The switch expression is " +
    "exhaustive and reads top to bottom in one method, exactly right for three regions one team owns. The " +
    "Supplier registry trades that compile-time exhaustiveness for extensibility, the shape to reach for " +
    "once toolkits start arriving from a plugin or a configuration-driven module and a new region should " +
    "not require editing this method's body at all.");
}
{
  const s = S();
  addDiagramSlide(s, "PATTERNS · BUILDER & PROTOTYPE", "Assembled gradually, or cloned from a configured exemplar",
    "13-builder-and-prototype",
    "Builder validates once at build(); Prototype's copy constructor is what makes clone() independent.");
  addNotes(s,
    "Builder and Prototype solve different problems even though both end with one finished object. " +
    "Builder exists because a Shipment is assembled gradually across several calls, from a dozen optional " +
    "pieces, before it is complete. Prototype exists because a PackagingTemplate is already a complete, " +
    "configured exemplar, and the cheapest way to get a second one with the same defaults is to clone it " +
    "rather than rebuild it field by field. A record's wither sits between the two, answering a third, " +
    "related force, a focused variation of one already-complete value with every other field carried over " +
    "unchanged.");
}
{
  const s = S();
  addCodeSlide(s, "PATTERNS · BUILDER & PROTOTYPE", "Every setter returns this; build() is the one place that validates", "Java 25",
    [
      "public Shipment build() {",
      "    String error = validationError();",
      "    if (error != null) {",
      "        throw new IllegalStateException(error);",
      "    }",
      "    return new Shipment(this);",
      "}",
      "",
      "Shipment shipment = Shipment.builder()",
      "        .trackingId(\"SHIP-4473\")",
      "        .carrier(\"Northline Freight\")",
      "        .destinationCountry(\"CA\")",
      "        .customsDeclaration(declaration)",
      "        .build();",
      "// A half-built shipment is not finished yet, not an invalid one.",
    ],
    "A missing customsDeclaration fails at build(), naming both the gap and the destination that required it.",
    { fontSize: 11 });
  addNotes(s,
    "Every chained setter returns this, which is what lets a caller supply fields in whatever order is " +
    "convenient rather than a fixed constructor parameter list, and nothing is validated as the chain " +
    "runs. The discipline worth repeating: a common reflex validates inside every setter instead of once " +
    "at build(), which reintroduces the fixed-ordering problem Builder exists to remove, just spread " +
    "across several methods instead of one. Checking every required field and the one cross-field " +
    "invariant, an international destination needs a customs declaration, in a single method run exactly " +
    "once means the claim a finished Shipment is always complete has exactly one place it could fail to " +
    "hold.");
}
{
  const s = S();
  addCodeSlide(s, "PATTERNS · BUILDER & PROTOTYPE", "A focused variation of a finished value, and an independent clone", "Java 25",
    [
      "public record CustomsDeclaration(String harmonizedCode, String originCountry,",
      "        String contentsDescription, long declaredValueCents) {",
      "    public CustomsDeclaration withDeclaredValueCents(long newValue) {",
      "        return new CustomsDeclaration(harmonizedCode, originCountry,",
      "                contentsDescription, newValue);",
      "    }",
      "}",
      "",
      "private PackagingTemplate(PackagingTemplate source) {",
      "    this.kind = source.kind;",
      "    this.weightKg = source.weightKg;",
      "    this.protectiveMaterials = new ArrayList<>(source.protectiveMaterials);",
      "}",
      "public PackagingTemplate copy() { return new PackagingTemplate(this); }",
    ],
    "Skip the ArrayList copy and every clone would share, and silently mutate, the same materials list.",
    { fontSize: 10 });
  addNotes(s,
    "A record has no setters, so withDeclaredValueCents returns a brand new CustomsDeclaration with one " +
    "field changed and every other field carried over, the shape to reach for when a value is already " +
    "complete and a caller wants a focused variation of it, reappraising a declared value after a customs " +
    "audit without disturbing the original. PackagingTemplate's copy() is one line, but the reason it " +
    "produces a fully independent clone comes from the private copy constructor behind it: " +
    "new ArrayList<>(source.protectiveMaterials) builds a brand new list rather than sharing the same list " +
    "reference, which is the one line a test proves matters by mutating one clone and asserting a second " +
    "clone, and the original exemplar, both still report their unmutated defaults.");
}
{
  const s = S();
  addDiagramSlide(s, "PATTERNS · SINGLETON & DI", "Exactly one instance is a separate question from how a caller reaches it",
    "14-singleton-vs-injected-collaborator",
    "CarrierRateTable.INSTANCE and a constructor-injected RateLookup both answer with one shared table.");
  addNotes(s,
    "CarrierRateTable and HolderCarrierRateTable both already settle the question of how many instances " +
    "exist: exactly one. Singleton and dependency injection are competing answers to a different question, " +
    "how does code that needs a shared capability get a reference to it. WarehousePricingJob calling " +
    "CarrierRateTable.INSTANCE directly has no seam a test or a different composition root can point " +
    "somewhere else; ShippingQuoteService depending on a RateLookup parameter stays substitutable no " +
    "matter how the force behind the capability changes later, including a Mockito double standing in for " +
    "a withdrawn carrier or a temporarily unavailable rate source.");
}
{
  const s = S();
  addCodeSlide(s, "PATTERNS · SINGLETON & DI", "Both answer with one table; only one stays substitutable in a test", "Java 25",
    [
      "public enum CarrierRateTable implements RateLookup {",
      "    INSTANCE;",
      "    private final Map<String, Money> ratesByCarrierCode = CarrierRates.baseRatesByCarrierCode();",
      "    public Money baseRateFor(String code) { return ratesByCarrierCode.get(code); }",
      "}",
      "",
      "public final class ShippingQuoteService {",
      "    private final RateLookup rateLookup;",
      "    public ShippingQuoteService(RateLookup rateLookup) {",
      "        this.rateLookup = Objects.requireNonNull(rateLookup, \"rateLookup\");",
      "    }",
      "    // a test hands this constructor a Mockito double; a class calling",
      "    // CarrierRateTable.INSTANCE directly has no such seam",
      "}",
    ],
    "The same ShippingQuoteService class passes unchanged against the enum, the holder, and a test double.",
    { fontSize: 10 });
  addNotes(s,
    "CarrierRateTable gets two guarantees from the language itself: an enum type can never be " +
    "instantiated a second time through reflection, even after setAccessible(true), and an enum constant " +
    "serializes by name and deserializes by looking that name back up, so there is no readResolve() to " +
    "remember. ShippingQuoteService depends on RateLookup, never on CarrierRateTable by name, the same " +
    "inversion the dependency-inversion principle trains for any collaborator. The payoff is not that the test " +
    "is short; it is that the identical ShippingQuoteService class runs unchanged against the enum " +
    "singleton, the holder singleton, and a one-line lambda, which is a claim WarehousePricingJob cannot " +
    "make about anything, because it has nothing a test or a different composition root can point " +
    "elsewhere.");
}

// ============================================================================
// SECTION 03: IN THE WILD
// ============================================================================
divider("03", "In the wild",
  "Virtual threads, resource and concurrency patterns, dispatch shortcuts, and a real framework's own composition root.",
  "This section leaves the Gang of Four catalogue for what production Java reaches for daily. Virtual " +
  "threads make blocking I/O nearly free, turning a design that was expensive at platform-thread scale " +
  "into one cheap enough to fan out to a hundred collaborators with no rewrite. A bounded resource pool, " +
  "a monitor object, and an active object are the three idiomatic answers to something scarce, shared, or " +
  "single-threaded by nature, forces virtual threads do not touch. Two dispatch shortcuts, an EnumMap " +
  "built from an exhaustive switch and an open Map keyed by a runtime-registered string, stand in for a " +
  "full Strategy or Command when a handler is one line. The section closes with Quarkus and CDI answering " +
  "five of this book's patterns at once, consistently, across a whole application.");

{
  const s = S();
  addContentTitle(s, "IN THE WILD · VIRTUAL THREADS", "Blocking I/O becomes nearly free");
  addBullets(s, [
    "A virtual thread mounts a small, fixed carrier (platform) thread to run Java code and unmounts the instant it blocks on I/O, freeing that carrier for the next task; the blocked thread itself costs nothing in OS resources.",
    "Executors.newVirtualThreadPerTaskExecutor() fans a request out to many blocking collaborators at once, one virtual thread per call, with no rewrite to asynchronous or reactive style.",
    "A Semaphore bounds how many of those virtual threads are inside a blocking call at the same time, without reintroducing a bounded thread pool.",
    "Pinning narrowed sharply in JDK 24: a virtual thread blocking inside synchronized no longer pins its carrier; what remains is native-boundary work, JNI, legacy synchronous file I/O, blocking class initializers.",
  ], { fontSize: 17 });
  addNotes(s,
    "A checkout flow that needs a shipping quote from eight carriers under contract has a concurrency " +
    "problem hiding behind a sequential-looking loop: calling each one with its own platform thread spends " +
    "eight megabyte-scale stacks on work that is almost entirely waiting, not computing, and that approach " +
    "runs out of threads before it runs out of carriers at scale. A virtual thread resolves that directly: " +
    "it is a Thread the JVM schedules onto a small pool of carrier threads, and when it blocks on I/O the " +
    "JVM unmounts it and hands that carrier to a different, ready virtual thread. The code inside never " +
    "knows this happened; it is still the same imperative, blocking code a thread-per-request design has " +
    "always been, with a different cost model underneath it.");
}
{
  const s = S();
  addDiagramSlide(s, "IN THE WILD · VIRTUAL THREADS", "A handful of carriers serving many virtual threads",
    "24-mount-unmount-carriers",
    "Each virtual thread mounts to run Java code and unmounts the moment it blocks.");
  addNotes(s,
    "A handful of real operating-system carrier threads serve many more virtual threads than that at " +
    "once, because the JVM's default scheduler unmounts a virtual thread the instant it blocks, which in a " +
    "rate-quote fan-out is nearly all of them, nearly all of the time. A blocked virtual thread continues " +
    "to exist only as a small object on the heap until its blocking operation completes and it remounts, " +
    "often on a different carrier than the one it started on. That remount is invisible to the Java code " +
    "running inside it; the whole appeal is a design that was expensive at platform-thread scale becoming " +
    "cheap at virtual-thread scale with zero rewrite.");
}
{
  const s = S();
  addCodeSlide(s, "IN THE WILD · VIRTUAL THREADS", "One virtual thread per carrier call, closed with try-with-resources", "Java 25",
    [
      "public List<RateQuote> fanOut(Shipment shipment, List<String> carrierIds) throws InterruptedException {",
      "    List<Callable<RateQuote>> tasks = carrierIds.stream()",
      "            .map(id -> (Callable<RateQuote>) () -> gateway.fetchQuote(shipment, id))",
      "            .toList();",
      "    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {",
      "        List<Future<RateQuote>> futures = executor.invokeAll(tasks);",
      "        List<RateQuote> quotes = new ArrayList<>(futures.size());",
      "        for (Future<RateQuote> f : futures) quotes.add(f.get());",
      "        return List.copyOf(quotes);",
      "    }",
      "}",
    ],
    "Eight carriers launch eight virtual threads; the design does not change shape at a hundred.",
    { fontSize: 11 });
  addNotes(s,
    "newVirtualThreadPerTaskExecutor() does exactly what its name says: every task submitted here gets a " +
    "brand-new virtual thread, never one recycled from a pool. ExecutorService has implemented " +
    "AutoCloseable since Java 19, so the try-with-resources block closes it automatically, requesting an " +
    "orderly shutdown that, by the time control reaches the end of the block, has already finished, " +
    "because invokeAll itself blocks until every task completes or throws. invokeAll returns futures in " +
    "submission order regardless of completion order, which is why the loop calls get() on each one in " +
    "turn rather than racing them, keeping the returned list's order tied to the caller's carrier list.");
}
{
  const s = S();
  addDiagramSlide(s, "IN THE WILD · VIRTUAL THREADS", "Unbounded finishes with the slowest call; bounded caps concurrency",
    "24-virtual-thread-timeline",
    "Eight carrier calls over wall-clock time: unbounded around 301ms, bounded to three in flight around 700ms.");
  addNotes(s,
    "The unbounded run finishes in roughly the time of the single slowest carrier call, not the sum of " +
    "all eight, because every call is in flight simultaneously on its own virtual thread. The bounded run " +
    "takes longer by design, because only three calls are ever blocking at once; a fourth, fifth, and " +
    "sixth call only start once a Semaphore permit frees up. Both runs still launch one virtual thread per " +
    "carrier call; only the number of carrier calls allowed to be inside the blocking operation at the " +
    "same instant is limited, which is a different knob than how many virtual threads the program is " +
    "allowed to create.");
}
{
  const s = S();
  addCodeSlide(s, "IN THE WILD · VIRTUAL THREADS", "Bounding concurrent calls, and a lock choice that no longer dodges pinning", "Java 25",
    [
      "Semaphore inFlight = new Semaphore(maxConcurrentCalls);",
      "Callable<RateQuote> task = () -> {",
      "    inFlight.acquire();",
      "    try { return gateway.fetchQuote(shipment, carrierId); }",
      "    finally { inFlight.release(); }",
      "};",
      "",
      "// Since JEP 491 (JDK 24), synchronized no longer pins a virtual",
      "// thread's carrier. Reach for ReentrantLock for tryLock(), a timed",
      "// or interruptible acquisition, or more than one Condition, not to",
      "// dodge a pinning cost that no longer applies to synchronized.",
    ],
    "A virtual thread that cannot acquire a permit blocks exactly as cheaply as any other block.",
    { fontSize: 11 });
  addNotes(s,
    "A virtual thread that cannot acquire a Semaphore permit blocks on acquire() rather than on the " +
    "carrier call itself, and that block is exactly as cheap as any other: the JVM unmounts it while it " +
    "waits for a permit to free up. The second block corrects a piece of pre-JDK-24 folklore that still misleads " +
    "directly to the room: before JEP 491, a virtual thread blocking while holding a synchronized monitor " +
    "pinned its carrier, because the JVM tracked monitor ownership by the carrier thread rather than the " +
    "virtual thread riding it. JEP 491 rearchitected that ownership around the virtual thread itself, so " +
    "swapping synchronized for ReentrantLock specifically to dodge pinning is outdated advice on Java 25; " +
    "reach for ReentrantLock for its actual extra capabilities, not for a pinning cost synchronized no " +
    "longer carries.");
}
{
  const s = S();
  addContentTitle(s, "IN THE WILD · RESOURCE & CONCURRENCY", "Bound it, serialize it, or decouple the call from the thread");
  addBullets(s, [
    "A bounded resource pool pairs a Semaphore as the hard cap with a BlockingQueue of idle connections; an AutoCloseable Lease returns a connection on every exit path, with no separate release() method to forget.",
    "A monitor object owns its own lock and takes it at the top of every public method, so a caller can never forget to lock before touching shared state; ReentrantLock plus a named Condition earns its ceremony over synchronized once a wait needs a timeout.",
    "An active object enqueues a request onto a private queue and returns a future immediately; one dedicated worker thread runs requests one at a time, decoupling invocation from execution entirely.",
    "Virtual threads make the calling thread cheap; none of that cheapness touches the carrier connection, the shared manifest, or the single-job printer these three patterns still protect.",
  ], { fontSize: 16 });
  addNotes(s,
    "A regional dispatch hub calls a carrier's tracking API on every parcel scan, and that call needs a " +
    "connection expensive to open, a DNS lookup, a handshake, often a TLS negotiation, work measured in " +
    "milliseconds against a tracking update that itself takes microseconds once the connection exists. The " +
    "same hub has a shared dock where several scanners stage parcels onto one manifest at once, and a " +
    "physical label printer that can only run one job at a time. Three different problems, one thing in " +
    "common: something is scarce, shared, or single-threaded by nature, and letting every caller touch it " +
    "directly breaks under concurrent load. Virtual threads change the calling side of this calculus and " +
    "leave the resource side entirely alone.");
}
{
  const s = S();
  addDiagramSlide(s, "IN THE WILD · RESOURCE & CONCURRENCY", "A Semaphore caps it, an idle queue reuses it, a Lease returns it",
    "25-bounded-resource-pool",
    "A caller past capacity blocks and times out rather than failing instantly or silently growing the pool.");
  addNotes(s,
    "A Semaphore sized to the pool's capacity is the hard bound: a caller can only proceed past acquire() " +
    "once a permit is available, so the number of connections outstanding can never exceed capacity no " +
    "matter how many callers contend for one. A BlockingQueue holds whichever connections are currently " +
    "idle, so a caller whose turn comes up gets a connection someone already paid to open, not a brand new " +
    "one. tryAcquire with a timeout blocks the calling thread until a permit frees up or the timeout " +
    "elapses, rather than failing the instant every connection is in use, and the pool never silently " +
    "grows past its capacity to avoid making someone wait.");
}
{
  const s = S();
  addCodeSlide(s, "IN THE WILD · RESOURCE & CONCURRENCY", "No release() method anywhere for a caller to forget", "Java 25",
    [
      "public Lease acquire(Duration timeout) throws InterruptedException, TimeoutException {",
      "    if (!permits.tryAcquire(timeout.toNanos(), TimeUnit.NANOSECONDS)) {",
      "        throw new TimeoutException(\"no connection within \" + timeout);",
      "    }",
      "    CarrierApiConnection connection = idle.poll();",
      "    if (connection == null) { connection = factory.create(); }",
      "    return new Lease(connection);",
      "}",
      "",
      "try (CarrierConnectionPool.Lease lease = pool.acquire(Duration.ofSeconds(1))) {",
      "    System.out.println(lease.connection().fetchTrackingStatus(\"SHM-7001\"));",
      "}  // close() returns the connection here, on every exit path",
    ],
    "A parallel manual release(connection) method is exactly what a Lease's close() makes unnecessary.",
    { fontSize: 11 });
  addNotes(s,
    "Lease is the half of this idiom that makes it safe under an exception: it implements AutoCloseable, " +
    "so a caller writes a try-with-resources block and the lease returns its connection to the idle queue " +
    "and releases its permit on every exit path, a normal return, an early return, or an exception " +
    "unwinding past it. There is no separate release(connection) method anywhere on CarrierConnectionPool " +
    "for a caller to forget on an error path, which is deliberate: a pool whose only way back to the idle " +
    "queue is a lease's close() method cannot leak a connection out of the pool the way a pool with a " +
    "parallel manual-release method can, where one missed call on one exception branch permanently shrinks " +
    "the pool by one.");
}
{
  const s = S();
  addCodeSlide(s, "IN THE WILD · RESOURCE & CONCURRENCY", "Serialize every call, or decouple the call from its execution", "Java 25",
    [
      "// Monitor object: every public method takes the dock's own lock.",
      "public void stageParcel(ParcelScan scan) {",
      "    lock.lock();",
      "    try {",
      "        staged.add(scan);",
      "        if (staged.size() >= bayCapacity) bayFull.signalAll();",
      "    } finally { lock.unlock(); }",
      "}",
      "",
      "// Active object: invoking the method only enqueues the work.",
      "public CompletableFuture<PrintReceipt> submitPrint(ShippingLabelRequest request) {",
      "    CompletableFuture<PrintReceipt> result = new CompletableFuture<>();",
      "    requests.add(() -> result.complete(render(request)));",
      "    return result;  // caller never blocks on the printer",
      "}",
    ],
    "A monitor still runs on the caller's thread; an active object's worker runs on its own.",
    { fontSize: 10 });
  addNotes(s,
    "DispatchDock is a monitor object: its defining trait is that it owns its own lock and takes it at " +
    "the top of every public method, so a caller never sees the lock, never names it, and cannot forget to " +
    "take it before touching the dock's staged parcels. ReentrantLock paired with a named Condition earns " +
    "its extra ceremony over synchronized here specifically because awaitFullBay() is a timed, named wait " +
    "that rechecks the staged count on every wakeup. LabelPrintActiveObject answers a different question " +
    "entirely: what happens when a caller wants to say print this and move on immediately, while the " +
    "actual printing happens on its own timeline, one request after another, on a thread the caller never " +
    "touches.");
}
{
  const s = S();
  addDiagramSlide(s, "IN THE WILD · RESOURCE & CONCURRENCY", "One queue, one dedicated worker, many callers that never block",
    "25-active-object-decoupling",
    "A direct synchronous call blocks each caller until the print finishes; the active object never does.");
  addNotes(s,
    "Three callers enqueue onto the same BlockingQueue and get a CompletableFuture back immediately; one " +
    "dedicated worker thread drains the queue in submission order and completes each future later, off the " +
    "caller's own thread. A test proves the decoupling two ways: the receipt's printedByThread field never " +
    "matches the submitting thread's name, and when several threads submit concurrently, every " +
    "submission's sequence number comes back distinct and contiguous, proof that one queue and one worker " +
    "linearized every call no matter how many callers raced to enqueue one. An active object's worker " +
    "gains nothing from being a virtual thread; there is exactly one of it, running for the program's " +
    "whole lifetime, which is the opposite of the create-once, discard-cheaply shape virtual threads are " +
    "built for.");
}
{
  const s = S();
  addContentTitle(s, "IN THE WILD · DISPATCH SHORTCUTS", "When a key selects one short handler, not a full hierarchy");
  addBullets(s, [
    "A Strategy or Command class per branch is ceremony with nothing underneath it when a handler is one line; a dispatch table answers which function runs for a key, and nothing more.",
    "An EnumMap built from an exhaustive switch expression resolves a closed, known key set in one lookup, with the compiler refusing to let a new enum constant go unhandled.",
    "A Map<String, handler> dispatch table serves an open key set a third party controls; register() lets a new code route correctly after the program is already running, with no recompile.",
    "The moment a handler needs memory across calls, more context than the key carries, or an undo path, the table has outgrown itself and the full Strategy or Command it was standing in for is the right tool again.",
  ], { fontSize: 17 });
  addNotes(s,
    "A shipping ingestion service answers the same question for two different key shapes: domain code " +
    "raises lifecycle events it fully controls, a closed set of six, while third-party carriers deliver " +
    "webhooks with their own wire codes, strings this codebase never declared and cannot enumerate ahead " +
    "of time. A chain of if-else branches answers which short block of code should run the least " +
    "disciplined way, growing sideways with every event kind. Building a named class with a constructor " +
    "and an interface override for a one-line handler is over-engineering in the other direction; a " +
    "dispatch table is the right-sized middle answer, built two idiomatic ways depending on whether the " +
    "key set is closed or open.");
}
{
  const s = S();
  addDiagramSlide(s, "IN THE WILD · DISPATCH SHORTCUTS", "A closed key and an open key, routed through one shared set of handlers",
    "26-shortcut-dispatch",
    "An unmatched open key still has a fallback route defined, never a silent drop.");
  addNotes(s,
    "Both forms route to the identical shared handler methods, which is deliberate: if the routing " +
    "mechanism changes, the behavior behind each route should not, and the only way to make that true by " +
    "construction is to call the same methods from every mechanism. The closed enum side serves a wire " +
    "tag this codebase compiles against directly; the open map side serves a carrier's webhook code, a " +
    "string this codebase does not control and cannot enumerate ahead of time, which is exactly why it " +
    "needs to stay open at runtime rather than closed at compile time, with a defined fallback for a code " +
    "nobody has seen yet.");
}
{
  const s = S();
  addCodeSlide(s, "IN THE WILD · DISPATCH SHORTCUTS", "Checked complete by the compiler, or extended while the program runs", "Java 25",
    [
      "private static BiConsumer<ShipmentEvent, ShipmentLedger> handlerFor(ShipmentEventType type) {",
      "    return switch (type) {",
      "        case CREATED -> ShipmentHandlers::handleCreated;",
      "        case PICKED_UP -> ShipmentHandlers::handlePickedUp;",
      "        case DELIVERED -> ShipmentHandlers::handleDelivered;",
      "        // ... one case per remaining constant, no default",
      "    };",
      "}",
      "",
      "public void register(String code, BiConsumer<CarrierWebhookEvent, ShipmentLedger> handler) {",
      "    handlers.put(code, handler);  // routable on the very next route() call",
      "}",
      "public void route(CarrierWebhookEvent event, ShipmentLedger ledger) {",
      "    handlers.getOrDefault(event.code(), CarrierWebhookRouter::handleUnrecognizedCode)",
      "            .accept(event, ledger);",
      "}",
    ],
    "A seventh ShipmentEventType with no case fails the build; an unrecognized webhook code falls back.",
    { fontSize: 10 });
  addNotes(s,
    "The EnumMap route() call costs one lookup and one call through a method reference, with the real " +
    "payoff in how the map gets built: handlerFor's switch expression has no default branch, so the " +
    "compiler itself checks every declared ShipmentEventType constant has a case, catching a forgotten " +
    "seventh constant at build time rather than on whichever shipment exercises the gap first. " +
    "getOrDefault on the open side matters for more than brevity: a two-step containsKey-then-get check " +
    "invites the check and the fetch drifting apart as a method grows, while getOrDefault makes the " +
    "fallback part of the same expression as the lookup, and a string key carries no compile-time " +
    "guarantee at all, so every open dispatch table needs that defined fallback from the start, not as an " +
    "edge case patched on later.");
}
{
  const s = S();
  addContentTitle(s, "IN THE WILD · QUARKUS AND CDI", "The container as composition root");
  addBullets(s, [
    "@ApplicationScoped plus constructor @Inject is Singleton and dependency injection, provided: Arc constructs exactly one bean and hands every caller the same instance, with no static accessor anywhere.",
    "A @Produces method is Factory: it centralizes which concrete implementation to build behind one method, and nothing outside that method ever names the concrete class.",
    "An @AroundInvoke interceptor bound through a custom annotation is Decorator and Proxy, applied with no wrapping class visible at the call site at all.",
    "A CDI event fired to one or more @Observes methods is Observer with no listener list; @ConfigMapping is Strategy selected from a properties file instead of a line of compiled code.",
  ], { fontSize: 17 });
  addNotes(s,
    "Quarkus is built on CDI, and its implementation, Arc, is a dependency-injection container: " +
    "infrastructure whose entire job is resolving which object satisfies which dependency, constructing " +
    "it, and handing it to whatever asked, the exact job earlier patterns in this deck did by hand with " +
    "holder classes and constructor parameters. Nothing about the underlying force changed; a checkout " +
    "service still needs exactly one rate-quoting collaborator for the life of the process, and something " +
    "still has to decide which concrete implementation to build. What changed is who writes the code that " +
    "answers those forces, and Quarkus answers four or five of these forces at once, consistently, across " +
    "an entire application, instead of one hand-rolled class at a time.");
}
{
  const s = S();
  addDiagramSlide(s, "IN THE WILD · QUARKUS AND CDI", "Five CDI mechanisms, five patterns this book already built by hand",
    "28-quarkus-cdi-composition-root",
    "Each box is a container feature; the label names the plain-Java pattern it answers.");
  addNotes(s,
    "@ConfigMapping maps to Strategy, @Produces maps to Factory, @ApplicationScoped plus constructor " +
    "@Inject maps to Singleton and dependency injection, an @AroundInvoke interceptor maps to Decorator " +
    "and Proxy, and a CDI event with @Observes maps to Observer. Walk the room through the one example " +
    "that ties all five together: a CheckoutService built by constructor injection, a RateQuoterProducer's " +
    "@Produces factory method, an OperationLoggingInterceptor bound through a custom annotation, a " +
    "ShipmentStatusChanged event fired to two independent @Observes listeners, and a RateConfig interface " +
    "bound to application.properties with no string parsed by hand anywhere.");
}
{
  const s = S();
  addCodeSlide(s, "IN THE WILD · QUARKUS AND CDI", "Declared, not constructed: injection, a producer, and an interceptor binding", "Java 25",
    [
      "@ApplicationScoped",
      "public class CheckoutService {",
      "    @Inject",
      "    public CheckoutService(RateQuoter rateQuoter, Event<ShipmentStatusChanged> statusChanged) { /* ... */ }",
      "",
      "    @LoggedOperation",
      "    public RateQuote quote(Shipment shipment) { return rateQuoter.quote(shipment); }",
      "}",
      "",
      "@Produces @ApplicationScoped",
      "public RateQuoter rateQuoter() {",
      "    return switch (rateConfig.strategy()) {",
      "        case \"flat\" -> new FlatRateQuoter(rateConfig.baseCents());",
      "        case \"weight-tiered\" -> new WeightTieredRateQuoter(rateConfig.baseCents(), rateConfig.perKgCents());",
      "        default -> throw new IllegalStateException(\"unknown strategy: \" + rateConfig.strategy());",
      "    };",
      "}",
    ],
    "Nothing outside rateQuoter() ever names FlatRateQuoter or WeightTieredRateQuoter directly.",
    { fontSize: 9 });
  addNotes(s,
    "@ApplicationScoped tells Arc to construct exactly one CheckoutService for the life of the " +
    "application and hand that same instance to every injection point, the Singleton pattern's intent " +
    "restated as an annotation. CheckoutService never calls new on a concrete RateQuoter and never reaches " +
    "for a static accessor; it declares what it needs and Arc fills that role instead of a hand-written " +
    "main method. @LoggedOperation is a custom interceptor binding; a caller holding an injected " +
    "CheckoutService reference cannot tell from the call site that a logging layer sits between it and the " +
    "quoting logic, the exact transparency a hand-written decorator proves by wrapping the identical " +
    "interface. rateQuoter() is Factory's job description word for word: centralize the decision behind " +
    "one method so every caller depends on the interface alone, now driven by a typed @ConfigMapping " +
    "instead of a line a programmer picked at a call site.");
}

// ============================================================================
// CLOSING
// ============================================================================
{
  const s = S();
  addContentTitle(s, "CLOSING", "The idiomatic Java 25 toolkit, in one pass");
  addTwoColBullets(s,
    [
      { text: "Data and closed sets", options: { bullet: false, bold: true } },
      "Records carry validated, immutable data; sealed interfaces close a varying hierarchy; exhaustive switches check completeness at compile time.",
      { text: "Everyday functional constructs", options: { bullet: false, bold: true } },
      "Enum constants, lambdas, method references, and streams satisfy Strategy, Singleton, Command, and Template Method with no new interface hierarchy.",
      { text: "What got retired", options: { bullet: false, bold: true } },
      "Double-checked locking, hand-written value classes, Visitor's accept/visit pair, and the Null Object all give way to one language feature apiece.",
    ],
    [
      { text: "Concurrency at scale", options: { bullet: false, bold: true } },
      "Virtual threads make blocking I/O nearly free; a bounded pool, a monitor, and an active object still protect what is actually scarce or shared.",
      { text: "A real framework's composition root", options: { bullet: false, bold: true } },
      "Quarkus and CDI answer Singleton, Factory, Decorator, Observer, and Strategy consistently across a whole application.",
      { text: "The discipline", options: { bullet: false, bold: true } },
      "Name the plain alternative next to the idiom, and let a measured force decide, not the idiom's novelty.",
    ],
    { fontSize: 15 });
  addNotes(s,
    "Close by tying the whole deck back to one habit: every idiom shown here answers a force this " +
    "audience already recognizes from the Gang of Four catalogue, records and sealed interfaces for a " +
    "closed, varying structure, enum constants and lambdas for a varying algorithm, Optional for a lookup " +
    "that may not answer, virtual threads for blocking I/O at scale, and a dependency-injection container " +
    "for five of those forces answered at once across a whole application. None of these constructs change " +
    "what a pattern does; all of them remove hand-written ceremony a reviewer used to check line by line. " +
    "The bigger habit worth leaving the room with is the one repeated under every pattern and idiom in " +
    "this deck: name the plain, non-generic alternative next to the sophisticated one, and let a real " +
    "force decide between them.");
}

// ============================================================================
// APPENDIX
// ============================================================================
divider("Appendix", "Reference material",
  "The build toolchain and a one-page classic-idiom-to-Java-25 feature map. Keep this open in another window.",
  "This appendix is reference material, not a narrative arc; it is meant to stay open in a second " +
  "window while working through the rest of the deck's examples, not to be read front to back. The " +
  "first slide covers the local build-and-test loop every example in the source book follows; the " +
  "second is a single-page map of every classic idiom this deck named, for a quick lookup when a " +
  "reviewer asks which Java 25 feature a given refinement belongs to.");

{
  const s = S();
  addDiagramSlide(s, "APPENDIX · BUILD AND TOOLCHAIN", "The local build-and-test loop",
    "00-build-and-test-loop",
    "Edit, mvn test, verify with JUnit 5 and AssertJ, on the host, no containers.");
  addNotes(s,
    "Every example behind this deck builds the same way: a pinned local toolchain, JDK 25 Temurin " +
    "installed through SDKMAN, Maven as the build tool, and JUnit 5 with AssertJ and Mockito for " +
    "behavior-verifying tests rather than tests that only check a build exits zero. There is no container " +
    "anywhere in the build or test loop, and no example ever asks a reader to run one; CI installs the " +
    "identical local toolchain directly on the runner. Every example is standalone-buildable from its own " +
    "directory in addition to being picked up by the repository's aggregator, so a reader can run " +
    "mvn test from inside any single example without touching the rest of the book's code.");
}
{
  const s = S();
  addContentTitle(s, "APPENDIX · FEATURE MAP", "Classic idiom versus its Java 25 replacement");
  addStatusTable(s, [
    { code: "Exactly one instance", name: "Double-checked locking (volatile + synchronized)", purpose: "Single-element enum singleton" },
    { code: "An immutable value type", name: "Hand-written equals, hashCode, toString + getters", purpose: "A record with a compact constructor" },
    { code: "A closed, varying hierarchy", name: "Visitor accept()/visit(), or an instanceof chain", purpose: "Sealed interface + exhaustive switch" },
    { code: "A lookup that may not answer", name: "null, or a Null Object stand-in", purpose: "Optional, composed with map/orElse" },
    { code: "A fixed sequence, varying steps", name: "Abstract base class + protected abstract hooks", purpose: "A static method taking Function parameters" },
    { code: "Resource cleanup on every exit", name: "try/finally with a hand-written close() call", purpose: "try-with-resources over AutoCloseable" },
    { code: "A scarce external connection", name: "A raw pool with a release() method to forget", purpose: "An AutoCloseable Lease, returned by close()" },
    { code: "Many blocking collaborators at once", name: "A bounded platform-thread pool", purpose: "One virtual thread per task" },
  ], { colW: [4.1, 4.0, 3.99], rowH: 0.56 });
  addNotes(s,
    "Use this as the lookup slide, not a teaching slide: every row names the force on the left, the " +
    "hand-written idiom that used to answer it in the middle, and the Java 25 feature that answers it now " +
    "on the right. The pattern worth repeating one last time is visible across every row: the idiom in the " +
    "middle column still compiles and still ships correct behavior on its own, and the column on the right " +
    "never changes what the force needs, it only removes ceremony a reviewer used to check by hand, a " +
    "volatile field, a hand-written equals, a default branch that silently absorbs an unhandled case, a " +
    "release() call a caller could forget. A codebase that cannot yet move to Java 25 loses none of the " +
    "patterns in this deck; it only keeps paying for these eight specific pieces of hand discipline a " +
    "little longer.");
}

pres.writeFile({ fileName: OUT })
  .then(p => console.log("WROTE", p))
  .catch(e => { console.error(e); process.exit(1); });
