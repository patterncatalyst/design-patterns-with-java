// deck-101.js - "Design Patterns with Java" - 101: Patterns & SOLID
// Red Hat-branded 16:9 deck. Build:  node deck-101.js  (from presentation/)

"use strict";

const H = require("./deck-helpers.js");
const {
  COLOR, FONT, W, ASSETS, PNG,
  newDeck, addFooter, addContentTitle, addBullets,
  addStatusTable, addCaption, addSectionDivider, addNotes,
} = H;

const OUT = "design-patterns-with-java-101-patterns-and-solid.pptx";

const pres = newDeck();
pres.title = "Design Patterns with Java - 101: Patterns & SOLID";
let pageNum = 0;

function S() {
  const s = pres.addSlide(); pageNum += 1; addFooter(s, pageNum); return s;
}
function divider(code, title, subtitle, notes) {
  const s = pres.addSlide(); pageNum += 1;
  addSectionDivider(s, code, title, subtitle);
  addNotes(s, notes);
}

// A pattern slide: title + bullets, optional figure on the right.
function patternSlide(eyebrow, title, bullets, fig, caption, notes) {
  const s = S();
  addContentTitle(s, eyebrow, title);
  const hasFig = !!fig;
  addBullets(s, bullets, {
    x: 0.62, y: 1.85, w: hasFig ? 5.55 : 12.09, h: 4.55,
    fontSize: hasFig ? 15.5 : 17,
  });
  if (hasFig) {
    s.addImage({
      path: `${PNG}/${fig}.png`,
      x: 6.45, y: 1.75, w: 6.25, h: 4.35,
      sizing: { type: "contain", w: 6.25, h: 4.35 },
    });
  }
  if (caption) addCaption(s, caption);
  addNotes(s, notes);
  return s;
}

// A pattern-to-force reference table slide.
function refTable(eyebrow, title, rows, notes) {
  const s = S();
  addContentTitle(s, eyebrow, title);
  addStatusTable(s, rows, { colW: [2.60, 1.80, 7.69], rowH: 0.42 });
  addNotes(s, notes);
}

// ===========================================================================
// Cover
// ===========================================================================
{
  const s = pres.addSlide();
  pageNum += 1;
  s.background = { color: COLOR.white };
  try { s.addImage({ path: `${ASSETS}/cover-panel.png`, x: 0, y: 0, w: W, h: 7.5 }); } catch (e) {}
  s.addText("DESIGN PATTERNS WITH JAVA", {
    x: 6.00, y: 1.98, w: 6.90, h: 0.34,
    fontFace: FONT.title, fontSize: 14, bold: true, color: COLOR.red, charSpacing: 6,
    align: "left", valign: "middle",
  });
  s.addText([
    { text: "Design Patterns", options: { breakLine: true } },
    { text: "with Java" },
  ], {
    x: 5.95, y: 2.42, w: 6.95, h: 2.00,
    fontFace: FONT.title, fontSize: 48, bold: true, color: COLOR.ink,
    align: "left", valign: "top",
  });
  s.addText("101: Patterns & SOLID", {
    x: 6.00, y: 4.65, w: 6.70, h: 0.90,
    fontFace: FONT.body, fontSize: 18, italic: true, color: COLOR.caption,
    align: "left", valign: "top",
  });
  try { s.addImage({ path: `${ASSETS}/logo-candidate-2.png`, x: 11.10, y: 6.80, w: 1.55, h: 0.37 }); } catch (e) {}
  addNotes(s, "Design Patterns with Java, the 101 deck: patterns and SOLID. This is the conceptual half of the series. The question running through every slide is not what the code looks like, but what pressure in the design made someone reach for a name. We walk the five SOLID principles, then the Gang of Four catalogue grouped by intent, and close with a quick-reference appendix worth keeping open in a second window. Every example lives in a shipping and logistics domain: carriers, invoices, warehouses, manifests. Java 25 gives most of these patterns more than one idiomatic expression, an interface with named classes, a lambda, an enum with constant bodies, a sealed interface dispatched through an exhaustive switch, and choosing among them is itself part of the design decision this deck trains. The deep implementation detail and the full example code live in a separate deck; this one is about recognizing what to build, and why.");
}

// ===========================================================================
// Section: Why Patterns
// ===========================================================================
divider("01", "Why Patterns", "Recognizing a decision under forces",
  "This section sets up the vocabulary everything else depends on. A pattern is not a recipe to copy from memory; it is a name attached to a recurring set of forces, competing requirements that pull against each other in the same piece of code. Naming a pattern before naming the forces behind it is how a design decision turns into decoration. We also look at why that same discipline matters more, not less, once an AI pairing partner can produce any pattern's shape on request, and why a pattern's name only stays useful once it can still be unpacked back into the forces that earned it.");

patternSlide(
  "WHY PATTERNS",
  "Patterns as decisions under forces",
  [
    "A pattern is a name for a recurring trade-off, not a recipe to copy into new code; the same name can resolve into different Java depending on which forces are actually present.",
    "A force is a requirement that competes with another requirement in the same code: flexibility against simplicity, testability against directness, extensibility against building a seam nobody asked for.",
    "The recurring arc: recognize the forces, name a candidate pattern and a candidate non-pattern, choose the idiomatic Java 25 expression, test the behavior, and know when to stop.",
  ],
  "01-forces-and-decisions",
  "Recognize the forces, name a candidate pattern or non-pattern, choose the idiomatic Java expression, test it, and know when not to use it. Forces and trade-offs run under every step.",
  "Picture an engineer who just finished the Gang of Four catalogue and reaches for Strategy the next time a method has an if statement. The result compiles, demonstrates real knowledge of the pattern's shape, and is worse than what it replaced, because the name arrived before the question of what problem it was supposed to solve. A pattern's name is worth using because an underlying tension recurs across different code in different domains, and the name lets two engineers skip re-deriving that tension from scratch. The arc on this slide runs under every pattern in this catalogue: forces first, name second, idiomatic Java expression third, a test that proves the claimed behavior fourth, and a clear-eyed read on when the pattern stops paying for itself."
);

patternSlide(
  "WHY PATTERNS",
  "Naming the pattern: a shared vocabulary",
  [
    "A pattern name compresses a paragraph of trade-off reasoning into a word a reviewer can act on: this needs to be a Strategy, not a switch expression, because regional carriers are now owned by separate teams.",
    "The test for whether a name still does real work: can it be unpacked back into the forces it names, for this code, right now, without reaching for a textbook definition.",
    "A wrong name costs more than no name: call a validation loop a Visitor and a teammate goes looking for double-dispatch machinery that was never actually built.",
  ],
  "02-pattern-vocabulary",
  "A pattern name sits between three readers, a teammate, a future reader, and an AI assistant, compressing a cluster of forces and a chosen structure into one word all three can act on the same way.",
  "A pattern name only does useful work once everyone means the same thing by it, which is a harder property to maintain than it sounds, because a name is easy to use loosely once it comes loose from the forces that justified it. A team that shares this vocabulary holds a design conversation at the altitude where the decision lives: does a connection pool need the correctness property a pool exists to provide, or is a single shared connection guarded by a lock sufficient for the traffic a service sees today. A pull request description that states the forces behind a claimed pattern invites a sharper review than one that only names the pattern, because the reviewer can check the reasoning directly instead of reconstructing it."
);

patternSlide(
  "WHY PATTERNS",
  "Forces first, for a teammate and an AI partner",
  [
    "Prompt with forces, not a keyword: \"add a listener so the dashboard updates the instant status changes, without polling\" constrains the idiom; \"make this reactive, add a pattern\" hands over a word-shaped hole with nothing inside it.",
    "A model trained on a huge corpus of code reaches for a familiar pattern shape by reflex, the identical failure as an engineer who names a pattern before its forces, now running fast enough to produce a full hierarchy in the time it took to describe the requirement.",
    "Review a generated pattern claim the way you review a human one: what forces does this solve, and was a plainer alternative considered and rejected for a stated reason.",
  ],
  undefined,
  undefined,
  "The reverse prompt is where the compression starts to leak: an assistant handed only a familiar keyword fills the gap with whichever shape dominates its training data for code that resembles the surrounding context, whether or not the forces in the actual code call for it. A generated pull request claiming an interface implements Observer deserves the same two questions a human-authored one does, because the assistant will not ask them of itself unless the forces were in the prompt that produced it. The same scrutiny runs in reverse: naming the pattern explicitly in a request, a Strategy because a discount rule is owned by a separate team and changes independently of checkout, gives the model the forces it needs to pick the right idiomatic Java expression instead of guessing from the keyword alone."
);

// ===========================================================================
// Section: SOLID
// ===========================================================================
divider("02", "SOLID", "Five principles, one force: this will change",
  "SOLID is not a separate topic from patterns; it is the forces-and-trade-offs vocabulary the whole catalogue gets judged against. All five principles answer one underlying pressure: the code around a decision should not have to change just because the decision itself changes. We walk a freight-billing engine through Single Responsibility, Open/Closed, and Liskov Substitution, then an order-notification system through Interface Segregation, Dependency Inversion, and dependency injection without a framework.");

patternSlide(
  "SOLID",
  "Single Responsibility",
  [
    "A freight invoice's one Invoice class totaling a bill, formatting a receipt, and persisting the record means a tax fix, a wording change, and a storage change all land in the same file.",
    "SRP splits by reason to change, not by line count: an InvoiceCalculator, an InvoiceFormatter, and an InvoiceRepository, each with exactly one reason to change and no knowledge the other two exist.",
    "Skip the split when nothing has a second reason to change yet; a class with one job and no second job in sight does not need to be carved into pieces in advance.",
  ],
  undefined,
  undefined,
  "The naive version reaches into the same line items from a calculateTotal method, a format method, and a save method, and a reviewer approving a tax fix has to read past formatting code and persistence code to find the handful of lines that actually matter. SRP's fix is not making the type smaller for its own sake; it is separating three unrelated reasons to change that were sharing one address. None of the three resulting types knows the other two exist, which is what makes changing the tax calculation a single-file edit instead of a careful read of a tangled class. Pushed too far in the other direction, a type with no second responsibility yet gets fragmented into an anemic cluster of helper and support classes, none of them owning a real responsibility on its own."
);

patternSlide(
  "SOLID",
  "Open/Closed",
  [
    "A sales tax rate changing twice a year is predictable, and the naive fix is a branch added to a method every time a jurisdiction or a discount program launches.",
    "An InvoiceRule abstraction lets a new tax or discount rule arrive as a new type the calculator depends on but never edits; a test can define a brand-new rule type the production code has never seen and hand it straight to the same constructor.",
    "A sealed InvoiceRule with an exhaustive switch trades OCP's openness for compiler-checked completeness, the right call once the catalogue is fixed by design rather than open to a plugin.",
  ],
  "03-open-closed-extension",
  "InvoiceCalculator depends on the InvoiceRule abstraction, never on a concrete rule. PercentageTaxRule and PercentageDiscountRule already existed; FlatServiceFeeRule arrived afterward, and InvoiceCalculator did not change.",
  "Open to new behavior, closed against editing what already works and is already tested. A percentage tax rule and a percentage discount rule both existed before a flat service fee rule joined them, and adding the flat fee touched exactly one new file; InvoiceCalculator's totalsFor method is byte for byte what it was before that file existed. The strongest proof of this is a test that defines a brand-new rule type the calculator's own source has never seen and hands an instance to the same constructor every other rule uses, because InvoiceRule was always the real extension point, not a convention a developer had to remember to follow. A sealed hierarchy with an exhaustive switch answers a different, closed version of the same question: the compiler, not a reviewer, catches a missing case the day a fourth rule kind is added."
);

patternSlide(
  "SOLID",
  "Liskov Substitution",
  [
    "A subtype must be usable anywhere its base type is, and that promise covers the whole contract, not just a matching method signature.",
    "A loyalty discount rule overrides only a protected discountRate() hook; the base class owns a final clamp the subtype can never bypass, so a combined rate can never discount more than the invoice is worth.",
    "A rule that implements InvoiceRule directly and skips the clamp type-checks, compiles, and passes a quick smoke test while quietly breaking the invariant every other rule honors.",
  ],
  undefined,
  undefined,
  "PercentageDiscountRule.adjustmentCents is declared final specifically to protect the clamp it owns: a subtype cannot override it to skip the clamp, and the compiler rejects the attempt outright, a stronger guarantee than a reviewer catching it in a pull request. A loyalty customer with a steep negotiated rate and decades on the account would, unclamped, discount more than the invoice is worth; LoyaltyDiscountRule never has to defend against that itself, because the clamp runs on whatever discountRate returns. The substitutability claim is proved with one function written once against the base type, called with both a standard rule and the refined rule, behaving correctly both times because neither subtype had the chance to violate the invariant the base type owns. A rule written in a hurry that bypasses the base type entirely shows what the violation actually looks like: code that compiles and runs, quietly breaking a promise every sibling implementation keeps."
);

patternSlide(
  "SOLID",
  "Interface Segregation",
  [
    "A fat NotificationGateway interface forces an SMS channel to implement a template renderer and a delivery-status check it does not have, which it answers by throwing or by faking a harmless-looking default.",
    "Segregating into MessageSender, TemplateRenderer, and DeliveryStatusProvider means a channel declares only the capability it actually has, with no method left to stub and no caller that can call one by mistake.",
    "Stop segregating once every remaining method on an interface is used by every remaining client; splitting a complete interface into one-method types is churn dressed up as a principle.",
  ],
  undefined,
  undefined,
  "A fat interface manufactures a Liskov violation that segregation makes structurally impossible, because there is nothing left to stub. SmsGateway implements exactly MessageSender; it never declares that it implements TemplateRenderer, so there is no render method to stub and no caller that can accidentally invoke one on an SMS channel, because the compiler never offers that call in the first place. Segregation also changes who needs to depend on what: a DeliveryAuditor that only ever checks dispatch status has no business depending on MessageSender or TemplateRenderer at all, and its constructor now says so directly. A test for a segregated role mocks one method on one interface, not three methods across a fat one, because the dependency the constructor declares is an accurate description of what the class needs."
);

patternSlide(
  "SOLID",
  "Dependency Inversion & DI by hand",
  [
    "An order notification policy that reaches straight into a concrete email gateway cannot be tested without a real or stubbed email client, and it cannot add SMS without an edit to the policy itself.",
    "Dependency inversion routes both the policy and the gateway toward one abstraction, MessageSender, so the policy depends on what a channel does, never which channel it is.",
    "Java ships no classpath scanner of its own; a composition root wires concrete gateways to policies by hand, and CDI or Spring earns its cost only once a graph grows past what a reader can trace by eye.",
  ],
  "04-dependency-inversion",
  "DIP inverts the dependency arrow from a high-level policy through an abstraction, not straight to a concrete gateway. ISP splits that abstraction into role interfaces a concrete type implements only where it has the capability.",
  "OrderNotificationPolicy depends on exactly MessageSender, never on EmailGateway or SmsGateway by name, and nothing in its own file imports a concrete gateway or links against one at compile time. That is the concrete, measurable payoff dependency inversion buys: the orchestrating layer becomes testable in isolation from the plumbing layer, substituting a Mockito double for the abstraction with no link against a real gateway anywhere. A composition root, often a single main method, constructs every concrete type exactly once and passes each abstraction's implementation to whatever constructor asks for it, with no container, no registration step, and no startup phase that resolves a graph by reflection. CDI and Spring both automate graphs like this one once hand-wiring becomes its own burden; reaching for either before that point trades a problem a team does not have for a dependency it does."
);

// ===========================================================================
// Section: The GoF catalogue by intent - Creational
// ===========================================================================
divider("03", "Creational Patterns", "The GoF catalogue by intent, part 1 of 3",
  "Creational patterns answer one family of forces: how an object gets built when the decision of which concrete type, or how many related objects, is not fixed at the call site. Four patterns, one recurring question each: who decides the concrete type, how does a family of related documents stay internally consistent, how does a multi-field object get assembled safely, and when does exactly one instance actually need to be global.");

patternSlide(
  "CREATIONAL · GOF CATALOGUE",
  "Factory Method",
  [
    "A shipment inspection runs the same validate-then-checklist sequence every time, but which checklist to build depends on whether the shipment is a standard parcel or a hazardous-materials shipment.",
    "Factory Method puts that one decision behind a protected createChecklist() hook inside a final inspect() sequence, so the fixed sequence never has to branch on category.",
    "Skip it when the sequence only ever builds one product; a base class protecting a hook nobody overrides a second way is overhead standing in for a single method with no decision to make.",
  ],
  undefined,
  undefined,
  "The sequence itself, validate the shipment, run one checklist, judge pass or fail from the checklist's own log, never changes between a standard inspection and a hazmat one. Only the one step that decides which checklist to build does, and that decision belongs to a subclass overriding a protected hook, not an if statement the sequence would otherwise have to carry. A static factory method backed by a switch expression over a closed, two-member category enum resolves which inspector subclass a shipment needs, the same shape used to resolve a region to a toolkit in Abstract Factory, just narrower: one product varies here, not a whole family. Reaching for Abstract Factory when only one product varies adds creation methods to an interface nobody calls; reaching for Factory Method when a whole family needs to stay consistent forces a caller to make several calls and trust it got them all from the same place."
);

patternSlide(
  "CREATIONAL · GOF CATALOGUE",
  "Abstract Factory",
  [
    "A shipping label, a rate quote, and a customs declaration all have to come from the same region, or a parcel carries paperwork for a jurisdiction it never entered.",
    "A ShippingToolkit hands out that whole matching family from one call, so RegionalShippingDesk can never end up holding a mismatched set of documents.",
    "A Supplier<T> registry resolves a region to a toolkit at runtime; a switch expression is the simpler shape worth reaching for first while the set of regions stays small and fixed.",
  ],
  "12-factory-family-selection",
  "ShippingToolkits.forRegion() resolves one ShippingToolkit from a Region value; the chosen toolkit's whole family of LabelFormatter, RateTable, and CustomsForm reaches RegionalShippingDesk together, from one factory call.",
  "RegionalShippingDesk's constructor builds all three products from one factory call, and nothing in that constructor, or in process(), ever names a concrete region's toolkit directly. A fourth region added later needs its own toolkit and its own three product classes, with RegionalShippingDesk completely unchanged, because it was never written against a concrete region in the first place. A Supplier<T> registry trades the compile-time exhaustiveness of a switch expression for a data structure another module can extend without editing the method that resolves it, which matters once toolkits start arriving from configuration rather than from a single team's source file. The sharper trap is a three-method interface where two of the three methods have exactly one caller and no second implementation anywhere, the Abstract Factory shape solving a narrower Factory Method problem."
);

patternSlide(
  "CREATIONAL · GOF CATALOGUE",
  "Builder & Prototype",
  [
    "An international shipment has required fields, optional fields with sane defaults, and one rule that only makes sense once every field is set: no customs declaration, no international destination.",
    "Shipment.Builder assembles that shipment through a fluent chain and checks the cross-field rule exactly once, at build(), never scattered across the individual setters.",
    "A PackagingCatalog clones a named PackagingTemplate exemplar instead of re-deriving fragile-electronics or bulk-dry-goods defaults at every call site; a record's wither method derives a one-field variation of an already-complete value.",
  ],
  "13-builder-and-prototype",
  "Shipment.Builder accumulates required and optional fields and validates every invariant once, at build(); PackagingCatalog clones a named PackagingTemplate exemplar into an independent copy.",
  "Nothing is validated as the builder's chain runs; a half-built shipment is not finished yet, not an invalid one, and build() is the single moment a caller declares it complete, checking every required field and the cross-field customs rule in one method run exactly once. A record's compact canonical constructor already expresses the identical validate-once discipline for a value with no gradual assembly, and a wither method derives a focused, one-field variation, a revalued customs declaration, without ever mutating the original. Prototype's copy constructor is where the real guarantee lives: copying a mutable list field into a brand new list, not assigning the same reference to both copies, is what keeps two clones from one exemplar independent of each other and of the exemplar itself, provable with a mutate-one-check-the-other test rather than an equality check alone."
);

patternSlide(
  "CREATIONAL · GOF CATALOGUE",
  "Singleton versus dependency injection",
  [
    "A read-only carrier rate table, loaded once and never mutated, is Singleton's cleanest case; the enum form gets reflection- and serialization-safety from the language itself, and the holder idiom gets lazy, lock-free thread safety from class-loading rules.",
    "Double-checked locking needed a volatile field and two careful checks to get the identical guarantee; both modern forms retire that idiom outright, with nothing left to get wrong by hand.",
    "The same capability routed through a RateLookup constructor parameter lets a test substitute a double with zero change to the service; a global singleton gives nothing a test, or a different composition root, can point elsewhere.",
  ],
  "14-singleton-vs-injected-collaborator",
  "A global singleton's callers reach into one shared instance with no parameter announcing it; the injected alternative routes the same capability through a constructor a composition root controls.",
  "A single-element enum is Joshua Bloch's recommended form: an enum constant can never be instantiated a second time through reflection, and it serializes by name with no custom readResolve method to remember. The initialization-on-demand holder idiom defers construction until first use with no synchronized keyword anywhere, because the Java Language Specification's class-initialization rules already guarantee exactly-once, thread-safe construction. Most collaborators are not this clean, and a job that reaches into CarrierRateTable.INSTANCE directly has a real dependency with no parameter anywhere revealing it, no seam for a test to substitute a failure scenario through. Reach for Singleton only when a type must have exactly one instance, is effectively immutable, and nothing plausibly needs to substitute a different one; default to constructor injection everywhere else, including the common temptation to reach for a global purely to avoid passing one more parameter down a few layers of calls."
);

// ===========================================================================
// Section: The GoF catalogue by intent - Structural
// ===========================================================================
divider("04", "Structural Patterns", "The GoF catalogue by intent, part 2 of 3",
  "Structural patterns are not about varying an algorithm or producing a family of objects; they are about shape. How an existing, awkward interface gets one the rest of the code can use. How a client gets one call instead of several to orchestrate. How two things that each vary on their own get decoupled so adding one does not force a change to the other. How a thing with a single concern stays uncomplicated while a stack of concerns layers cleanly around it.");

patternSlide(
  "STRUCTURAL · GOF CATALOGUE",
  "Adapter",
  [
    "A carrier's own rate SDK ships in its own units, grams and a numeric zone code, and its own method names, and checkout code should not have to match them.",
    "GlobalCourierGatewayAdapter wraps the legacy client behind this app's own CarrierGateway interface by composition, not inheritance, translating units once instead of at every call site.",
    "Composition over extension matters doubly in Java: a vendor class often ships final, and extending it anyway would spend the adapter's one shot at single inheritance on a type nobody controls.",
  ],
  "15-adapter-facade-bridge-forces",
  "Adapter wraps a legacy rate client behind the app's own CarrierGateway interface, Facade fronts four shipping subsystems with one call, and Bridge crosses two notification kinds with two delivery channels without a class per combination.",
  "Swap GlobalCourierGatewayAdapter for a different carrier's adapter and every caller of CarrierGateway.quote() keeps working unmodified, which is the entire point of adapting to a target interface instead of threading the vendor's shape through the rest of the codebase. Java allows a class to extend only one superclass, which makes the choice between a class adapter and an object adapter close to automatic: extending the vendor's rate client would spend the adapter's one shot at inheritance on a type this codebase does not control, and the vendor class is frequently marked final besides, foreclosing the question outright. Holding the legacy client as a plain field keeps the adapter free to wrap a second vendor's client later without touching its own inheritance at all. An adapter needs an actual mismatch to resolve; wrapping a client that already returns the right shape adds a class around a method that could be called directly."
);

patternSlide(
  "STRUCTURAL · GOF CATALOGUE",
  "Facade",
  [
    "Quoting, labeling, declaring customs, and issuing a tracking number for one shipment are four calls that have to run together, in order, or a label ships with no tracking number printed on it.",
    "ShippingFacade.prepareShipment() owns that sequence in one method, so every caller, today's checkout path and tomorrow's overnight batch job alike, inherits the correct order instead of re-deriving it.",
    "Skip it when there is only one subsystem to call; a facade with four constructor parameters and no coordination rule between the calls it makes is a parameter holder wearing a different name.",
  ],
  undefined,
  undefined,
  "The sequencing inside prepareShipment() is the actual value this facade delivers, not the four subsystem calls on their own: a tracking number has to exist before the label can print it and before the tracking URL can point at it. A checkout handler calling the tracking registry, the rate gateway, and the label printer directly would have to get that order right every single time it wrote the logic, in every place it wrote it; the facade gets it right once and every caller inherits that correctness. CustomsDeclarationService decides on its own whether a declaration applies, and ShippingFacade never branches on a shipment's zone itself, a second form of decoupling riding alongside the orchestration: the policy for when a declaration is required lives in exactly one place, not duplicated into every caller that might otherwise check the zone itself."
);

patternSlide(
  "STRUCTURAL · GOF CATALOGUE",
  "Bridge",
  [
    "A notification's kind, created or delayed, and its delivery channel, email or SMS, vary independently of each other, and a class per combination multiplies every time either axis grows by one.",
    "Bridge splits Notification from DeliveryChannel into two small hierarchies held together by composition, so a third kind or a third channel arrives as one new class, not a new row and column crossed against everything that already exists.",
    "Skip it when only one combination exists in practice; splitting an axis that never varies independently buys nothing over a single class that formats and sends directly.",
  ],
  undefined,
  undefined,
  "Two refined notification kinds crossed with two delivery channels give four working combinations, a shipment-created email, a shipment-created text, a delay email, a delay text, and none of those four needed its own class. Without the bridge, the next instinct is a class per combination, and the count keeps multiplying every time either axis grows by one. Notification.send() is marked final: it is the one piece of behavior every notification has in common regardless of kind, asking a subclass for its message text and handing that text to whichever channel was configured, and sealing it off from override keeps the bridge itself from drifting apart between subclasses. A subclass can change what the message says; it cannot change how the message reaches its channel. The moment to introduce the split is the moment a second kind or a second channel is either in hand or clearly coming, not before."
);

patternSlide(
  "STRUCTURAL · GOF CATALOGUE",
  "Decorator",
  [
    "A shipping rate quote wants a high-value surcharge, a cache so a cart priced three times does not hit the tariff lookup three times, and an audit log, each shipped on its own schedule.",
    "Decorator stacks each concern behind the RateQuoter interface the core quoter already implements, so any combination still looks like one quote to the checkout code asking for it.",
    "A lambda satisfies the identical single-method contract with no new class; reach for a named class once a decorator needs to expose its own state, the way a cache exposes hit and miss counts.",
  ],
  "16-decorator-proxy",
  "A decorator stack of surcharge, caching, and logging wraps a core rate source, next to a proxy controlling access to the same kind of real subject; both wrap the identical interface, only the intent differs.",
  "All three decorators in this example take a RateQuoter and hold it in a final field, the same way a Strategy field holds a single interchangeable algorithm, except each decorator here holds exactly the one layer directly beneath it. Decorator and Proxy can look nearly identical in code, which is exactly why the test for telling them apart is never the shape; it is what each one is for. A decorator composes independent concerns a caller assembles in whatever order it needs, and a lambda satisfies the identical contract a named class would, with the trade-off that a lambda has nowhere to hang an extra method like a cache's own hit-count accessor. A cache that is meant to compose with other concerns on the same object is a Decorator; a cache whose entire job is gating whether an expensive call happens at all is closer to a Proxy."
);

patternSlide(
  "STRUCTURAL · GOF CATALOGUE",
  "Decorator: order is part of the design",
  [
    "Surcharge wrapping a cache recomputes the markup on every call, so raising the surcharge rate between two lookups changes the second total even though the cached base price never moved.",
    "Swap the two layers, cache wrapping surcharge instead, and the identical rate change has no effect on a repeat lookup until the shipment itself changes; nothing moved but the wrapping order.",
    "Deciding where a decorator goes in a stack decides what information reaches it: a design decision with observable consequences, not a stylistic choice a reviewer can wave past.",
  ],
  "16-decorator-stacking-order",
  "The same two decorator classes, nested in opposite order around the identical core. Surcharge outside caching recomputes on every call; caching outside surcharge memoizes the already-surcharged amount, so the same rate change has no effect until the shipment itself changes.",
  "A layer only observes what happens on the side of the stack closer to the core than it is; anything further out is invisible to it. With surcharge sitting outside the cache, SurchargeRateQuoter.quote() always calls its wrapped quoter and adds a fresh markup on top of whatever comes back, even on a cache hit, so a live rate change shows up on the very next call. With caching sitting outside the surcharge, the cache memoizes the already-surcharged total, so a later change to the surcharge rate has no effect on a repeat lookup until the shipment itself changes and the cache actually misses. The two decorator classes involved are identical, constructed with identical arguments; only the order they wrap in decided whether a live rate change shows up immediately or goes unnoticed until a different shipment comes through."
);

patternSlide(
  "STRUCTURAL · GOF CATALOGUE",
  "Proxy",
  [
    "A warehouse scanning app's carrier gateway is expensive to construct, a real session against a live tariff API, and a guest account has no business reaching what a worker's badge unlocks.",
    "A hand-written LazyCarrierGatewayProxy defers construction to the first real call; java.lang.reflect.Proxy builds an access-controlled surrogate at runtime from the interface alone, routing every call through an InvocationHandler.",
    "A caller with sufficient clearance gets back exactly what the real subject would have returned; a Proxy never reformats a successful result the way a Decorator is free to.",
  ],
  undefined,
  undefined,
  "A caller holding a RateQuoter reference cannot tell the lazy proxy apart from a real gateway that happened to construct instantly, which is the entire point: the proxy's one job is to make expensive construction invisible, deferring it until a lookup demands it rather than paying for it whether a lookup ever happens or not. Proxy.newProxyInstance generates an implementation of RateQuoter at class-load time from the interface array alone, with every call routed through an InvocationHandler's invoke() method instead of a hand-written body, which is where a clearance check gates access before the real subject is ever reached. Introducing a proxy ahead of any evidence that construction is expensive enough to matter, or ahead of a real second caller with lesser clearance, defends against a cost or a distinction that does not exist in the system yet."
);

patternSlide(
  "STRUCTURAL · GOF CATALOGUE",
  "Composite",
  [
    "A pallet holds cartons, a carton can hold more cartons before it holds a parcel, and \"how much does this weigh\" should mean the same call at every level of that tree.",
    "A sealed ShipmentNode interface over Parcel and ShipmentBox records lets one exhaustive pattern-matching switch answer that question recursively, with no leaf-versus-branch type check anywhere in the code.",
    "Skip it when nesting never happens; a flat List<Parcel> summed in a loop says the same thing with one interface fewer.",
  ],
  "17-composite-flyweight-tree-and-pool",
  "A composite shipment tree sharing one interface on the left; a flyweight pool of shared packaging descriptors referenced by many parcels on the right.",
  "A naive answer writes one method for weighing a parcel and a nearly identical one for weighing a carton, and the moment a carton gets nested inside another carton, which happens the first time a warehouse consolidates two partial shipments into one box, the carton method has to learn to call itself. Sealing ShipmentNode to exactly Parcel and ShipmentBox is what lets the compiler check every switch over it for completeness: totalWeightGrams, totalValueCents, and itemCount are all the identical two-arm shape, and a third permitted type added later without a matching case anywhere fails to compile rather than silently under-counting a tree. A flat list answers a whole pallet's question just as easily, right up until a warehouse needs to move one carton as a single unit or needs the weight of one sub-tree without summing parcels it never holds."
);

patternSlide(
  "STRUCTURAL · GOF CATALOGUE",
  "Flyweight & Null Object",
  [
    "One packaging code can describe hundreds of parcels a day, and storing its full description on every parcel copies facts that never change between one parcel and the next.",
    "A FlyweightPool shares one immutable PackagingSpec across every parcel that references it, proven by a reference-identity check rather than an equals() check a copy-per-parcel bug would also pass.",
    "A weight-limit alert needs a handler most boxes never wire up; a NullShipmentAlertHandler enum singleton lets the weight check call it unconditionally, with the null branch gone from every call site, not just the one that constructs it.",
  ],
  undefined,
  undefined,
  "Returning a packaging descriptor by reference rather than by value is what makes the sharing real and checkable: two separate lookups for the same code return references that compare equal with ==, proving the second call reused the first call's instance instead of quietly allocating a private copy that merely looks identical. A Flyweight implementation that rebuilt a fresh object from the same fields on every call would pass a field-equality test while still allocating one object per parcel, defeating the pattern while looking correct from the outside. Null Object removes a null-check decision instead of asking every caller to remember it: an enum with one constant is the idiomatic Java shape for a type that must have exactly one instance, and resolving an Optional handler to that constant once, at a construction boundary, keeps every downstream call site branch-free permanently rather than repeating the unwrap at every call."
);

// ===========================================================================
// Section: The GoF catalogue by intent - Behavioral
// ===========================================================================
divider("05", "Behavioral Patterns", "The GoF catalogue by intent, part 3 of 3",
  "Behavioral patterns coordinate decisions, sequences, requests, and state. This is the largest group in the catalogue, because behavior is where most of a system's day-to-day change pressure actually lands: a rule that varies, a status that governs what is legal next, a request that needs to outlive the call that created it, a set of collaborators that should not know about each other, a fixed structure that keeps gaining new operations.");

patternSlide(
  "BEHAVIORAL · GOF CATALOGUE",
  "Strategy",
  [
    "A checkout flow prices a shipment by a rule set per carrier contract, not per line of code: flat rate, weight-tiered, or a distance-zone table.",
    "The identical RateStrategy contract resolves three idiomatic ways: an interface with named classes for a strategy with real state, a lambda for one built and discarded at a call site, an enum with constant-specific bodies for a closed set the compiler can check for completeness.",
    "A lambda composes: withInsuranceSurcharge wraps one RateStrategy around another with no decorator class anywhere, because a lambda is already a value that closes over whatever it needs.",
  ],
  "08-strategy-forces",
  "Strategy's forces: one caller, one contract, and a set of interchangeable algorithms behind it, each expressed a different idiomatic way.",
  "None of the three expressions is \"the\" Strategy pattern in this catalogue; all three are, and choosing among them, based on whether the set is open or closed and whether swapping happens at runtime or compile time, is itself the design decision. ShippingRateCalculator never mentions a concrete strategy class by name; it depends only on the interface, so a new carrier's pricing rule arrives as a new class or a new lambda with nothing in the calculator touched to admit it. An enum trades runtime flexibility for a stronger guarantee: a Carrier field can never be reassigned to a fourth, as-yet-unwritten carrier the way a RateStrategy field can be reassigned to a brand-new lambda, and a constant left without its own method body fails to compile rather than surfacing as a runtime surprise."
);

patternSlide(
  "BEHAVIORAL · GOF CATALOGUE",
  "Template Method",
  [
    "An order pipeline validates, prices, and records every order in the same three-step sequence, but which rule runs inside two of those steps differs by sales channel: retail, wholesale, marketplace.",
    "A final process() method on an abstract class calls protected abstract hooks a channel overrides; final on the entry point and protected on the hooks are what keep the sequence itself out of reach of whatever supplies the varying content.",
    "The identical skeleton written as a record holding three functional-interface fields needs no class hierarchy at all; a channel becomes three lambdas and a constructor call instead of a file.",
  ],
  "09-template-method-skeleton",
  "OrderProcessingPipeline's fixed, final sequence runs an invariant check and three hooks in order; a channel supplies those hooks either by overriding an abstract class or by handing the pipeline three lambdas.",
  "A Template Method whose entry point is left open to override has given away the one thing it exists to protect: process() is final specifically so no subclass can reorder the steps, skip the shared precondition, or return early before the recording hook runs. The functional form resolves the identical fixed sequence through fields assigned at construction time instead of an override chosen at subclassing time, and a reflection-based test confirms the abstract class's entry point really is final rather than trusting a reader to take the keyword on faith. Strategy swaps an entire algorithm for another of the same shape; this pattern keeps one sequence and varies a handful of its steps while the order they run in never moves, which is the test for telling the two apart when both look plausible."
);

patternSlide(
  "BEHAVIORAL · GOF CATALOGUE",
  "State",
  [
    "A shipment's legal next moves depend on its current status, and the real question is which events are legal right now, not which pricing rule applies.",
    "An enum plus a nested EnumMap transition table puts every legal move in one literal structure an auditor can scan with no Java knowledge required beyond recognizing a missing entry.",
    "A sealed ShipmentState interface of per-status records carries data an enum constant cannot, a carrier tracking number on InTransit, and an exhaustive switch turns a forgotten status into a compile error, not a production surprise.",
  ],
  "10-shipment-state-transitions",
  "A shipment's legal states and transitions, plus the weighted-probability outlet a stochastic variant of the same machine draws from out of in-transit.",
  "A compliance reviewer checking whether a delivered shipment can ever be cancelled scans the transition table's entries and finds DELIVERED absent as a key entirely, no Java knowledge required. A sealed interface earns its ceremony once different statuses need meaningfully different behavior or data beyond just \"what comes next,\" the exact gap a flat transition table has no natural place to fill. Both deterministic expressions throw the identical exception type from a shared vocabulary, which is what lets a cross-check test drive an identical event sequence through both machines and assert they agree on every step, each one stating the rules independently rather than delegating to a shared function that would make the agreement trivial."
);

patternSlide(
  "BEHAVIORAL · GOF CATALOGUE",
  "State: two idioms, and a third that draws",
  [
    "The identical (PACKED, SHIP) -> IN_TRANSIT rule reads as one line of data in the transition table and as one arm of a dedicated method in the switch; neither idiom reads the other's rules.",
    "A third expression breaks the caller-issued-event contract entirely: once a shipment is in transit, a StochasticShipmentMachine draws a weighted outcome from an injected, seedable RandomGenerator instead of waiting for a caller to call deliver().",
    "Seeding the same generator with the same value twice reproduces the exact sequence of draws, which is what turns an 85/10/5 delivered-delayed-lost distribution into a property a test can check, not a demo that happens to look plausible once.",
  ],
  "10-table-vs-switch-dispatch",
  "The identical (PACKED, SHIP) to IN_TRANSIT rule stated two ways. The table only lists statuses that have any legal move at all; the switch lists every status, with no default, so a missing case is a compile error rather than a silent miss.",
  "Constructing the random generator outside the machine and passing it in through the constructor is the same dependency-inversion move this catalogue trains for a collaborator: depend on an interface, not on a concrete source of entropy a class owns for itself. A transition with real uncertainty behind it in the world, a carrier's delivery outcome, a flaky retry result, is exactly what a weighted draw over an injected generator is for; a transition that is merely inconvenient to model precisely is a different problem, and modeling a data bug as a probability distribution can hide a fixable integration issue behind a parameter that looks intentional. A status with two values and one transition between them needs a boolean, not any of these three machines."
);

patternSlide(
  "BEHAVIORAL · GOF CATALOGUE",
  "Command",
  [
    "A warehouse desk's receive, move, recount, and cancel actions need to be undoable, queued for a nightly batch, or submitted from more than one trigger without each one re-deriving how to run them.",
    "A WarehouseCommand turns a request into an object with execute() and undo(); a clean arithmetic inverse fits a record, a captured prior-state snapshot needs a mutable class instead.",
    "A CommandQueue drains a Deque FIFO into an optional CommandHistory, so a whole batch stays individually undoable afterward; a plain functional interface serves a queued action, like a receipt log line, that has nothing to undo.",
  ],
  "11-command-undo-and-queue",
  "A trigger submits a request as a WarehouseCommand; it either enqueues into a FIFO CommandQueue that later drains into the ledger, or runs through a CommandHistory that moves it between a done stack and an undone stack.",
  "A bare method call has no existence after it returns; a command object does, for as long as something is holding onto it, which is what makes undo, batching, and multiple submitters possible at all. AllocateStockCommand expresses a clean, symmetric inverse as a record, the identical call with the quantity negated, while RecountStockCommand has no arithmetic opposite and must capture the prior quantity inside execute() rather than the constructor, because a queued command might run an hour after it was built. An action triggered from exactly one place, never undone, that always runs immediately needs a plain method call, not a WarehouseCommand wrapped around it with nothing on the other side of that cost to justify it."
);

patternSlide(
  "BEHAVIORAL · GOF CATALOGUE",
  "Command: the undo/redo stack",
  [
    "CommandHistory keeps two Deques, done and undone; execute() runs a command and clears the redo branch, because once a new action happens, the old redo path would contradict it.",
    "undo() pops the most recently done command and calls its own undo(); redo() runs the mirror operation, moving a command back across the same two stacks in the exact reverse of the order it ran.",
    "Skip the whole history when a desk runs one action at a time with nobody ever asking to step backward; the stack is machinery with nothing to protect until undo is a real requirement.",
  ],
  "11-undo-redo-stack-mechanics",
  "Three snapshots of CommandHistory's two Deques. Running three commands fills the done stack; undo() pops the most recent one onto undone; executing a new command clears undone entirely rather than leaving the old branch reachable by a later redo().",
  "Clearing the undone stack inside execute() is the rule every mainstream undo implementation already follows: once a new action happens, whatever had been undone away from is no longer reachable by redo, because redoing it now would overwrite the new action already sitting in its place. Both operations only ever touch the most recent end of their deque, which is the same discipline a text editor or a drawing program uses, because it is the only ordering that keeps every intermediate state consistent; skipping around to an arbitrary earlier command without undoing everything after it first would leave the ledger in a state no real sequence of actions would ever produce. An optional CommandHistory parameter on the queue's drain step is what connects batching back to undo: pass one in and the entire batch stays individually undoable, one command at a time."
);

patternSlide(
  "BEHAVIORAL · GOF CATALOGUE",
  "Observer",
  [
    "A shipment's status change matters to a support dashboard, an alerting service, and a billing dispute log, each reacting on its own schedule, added independently of the others.",
    "ShipmentTracker fans a change out to subscribers behind a one-method ShipmentStatusListener; a Subscription implementing AutoCloseable lets try-with-resources close a listener instead of trusting every exit path to remember.",
    "Notification runs over a snapshot copy of the listener list, so a listener that unsubscribes mid-callback cannot raise a ConcurrentModificationException on the live collection it is being called from.",
  ],
  "18-observer-mediator",
  "A ShipmentTracker subject fanning a status change out to independent listeners, next to a DispatchCoordinator mediator routing between three collaborators that never call each other directly.",
  "A listener is a single-method functional interface, so a long-lived collaborator like a dashboard earns a named class, while a one-shot reaction like an audit log is a bare lambda with no class file and nothing for a reader to look up. The classic hazard in every Observer implementation is a listener that stays subscribed after whatever it belonged to has stopped caring; a Subscription handle rather than a bare unsubscribe call is what makes closing one a natural fit for try-with-resources, including on an exception path a caller might otherwise forget. Once a subject's listeners need backpressure or asynchronous delivery at real scale, java.util.concurrent.Flow's Publisher and Subscriber protocol is the standard library's answer to the identical force, though it is pure overhead for a handful of fast, synchronous listeners like these."
);

patternSlide(
  "BEHAVIORAL · GOF CATALOGUE",
  "Mediator",
  [
    "A warehouse, a carrier, and a notification station calling each other's methods directly means a fourth collaborator, a returns-processing stage, requires editing every existing class it needs to hear from.",
    "DispatchCoordinator routes every event through one hub; colleagues hold no reference to each other, only to the mediator, bound after construction to break the circular wiring requirement.",
    "A coordinator that quietly absorbs validation logic and business rules on top of routing has stopped being a hub and become a god object wearing Mediator's name.",
  ],
  undefined,
  undefined,
  "WarehouseStation.markOrderReady() raises an event with no idea which station, if any, responds; wiring a colleague to its mediator happens once, in the coordinator's own constructor, which is what breaks the circular requirement of colleagues needing a mediator reference while the mediator needs live colleague objects to construct itself from. Calling one method on the warehouse can cascade through two hops the warehouse itself never has to know about, three stations taking part in one customer-visible outcome with no one of them ever holding a reference to another. Three stations that only ever grow by one and never gain a fourth collaborator do not need a hub standing between them; a plain, visible sequence of calls is the whole story. The fix for a coordinator that has absorbed a station's own business logic is keeping its job narrow: deciding who hears about what, never what a station does once it hears about it."
);

patternSlide(
  "BEHAVIORAL · GOF CATALOGUE",
  "Chain of Responsibility",
  [
    "A shipment approval needs a weight check, a hazmat clearance check, and an embargo check, each owned by a different team, and the submitting code should not need to know how many checks exist.",
    "Each ApprovalHandler either returns a decision from attempt() or hands the request to the next handler; a final handle() method guarantees every handler logs itself and nobody skips delegation by accident.",
    "A ManualReviewHandler fallback with no next handler guarantees the chain always lands in a named outcome; the identical rules rebuilt as a list of Function checks trade that identity and call-logging for far less ceremony.",
  ],
  "19-chain-iterator",
  "An ApprovalRequest flowing down a chain of handlers until one of them rejects it or the chain falls back to manual review, next to a ShipmentManifest traversed by a hand-written Iterator and by a Stream built on the same Iterable.",
  "A caller holding only the first handler in the chain cannot tell, from one call, whether a request stopped at the first check or traveled through all four, which is exactly what a call-log assertion in the test suite makes observable: a rejected request's log is short, while a request that clears every automatic check reaches all four. Without the fallback handler, nothing in the chain's own type guarantees a request ever reaches a decision; designing one handler whose attempt() is unconditional turns \"falls off the end unhandled\" into \"always lands in a known, named outcome.\" A fixed set of two checks owned by one team that never needs to reorder or disable a check independently of the others needs a plain two-step check inside one method, not a chain of handler objects."
);

patternSlide(
  "BEHAVIORAL · GOF CATALOGUE",
  "Iterator",
  [
    "A warehouse manifest sorts packages into one lane per destination zone internally, and a caller walking every package in zone order should not have to know the manifest is bucketed at all.",
    "A hand-written ZoneOrderIterator hides that bucketing behind the standard Iterable and Iterator contract; most Java code never hand-writes one, because a List or a Set already hides nothing worth hiding.",
    "Implementing Iterable buys a free Stream through the default spliterator() method, with the real traversal work still living entirely inside the one hand-written iterator underneath it.",
  ],
  undefined,
  undefined,
  "A caller holding a manifest's iterator has no way to observe that the real storage underneath it is four zone-keyed lists rather than one; hasNext() and next() are the entire contract, and inserting shipments in one arrival order still returns them in zone order, not insertion order. Writing a custom iterator is justified specifically because the manifest's real storage is not what a caller should see; a flat ArrayList already implements Iterable and already hides nothing, so wrapping one in a hand-written iterator adds an indirection layer with nothing behind it. A Stream pipeline answers a different question than a for-each loop: what subset of this, transformed how, declaratively, over any Iterable at all, with no bucket-aware code required anywhere in the pipeline itself."
);

patternSlide(
  "BEHAVIORAL · GOF CATALOGUE",
  "Visitor",
  [
    "An export manifest's four parcel kinds barely change, but the operations asked of them keep growing: a shipping cost, a customs summary, a full validation pass that reports every problem at once.",
    "A sealed ManifestEntry interface dispatched by an exhaustive pattern-matching switch gets the Gang of Four's completeness guarantee with no accept() method and no visitor interface declared anywhere.",
    "Reach for the classic accept-and-visit hierarchy only when the set of kinds stays open to a plugin or a third party; a fixed, known set changed through code review wants the switch instead.",
  ],
  "20-visitor-switch-operations",
  "ManifestEntry's four fixed kinds on the left, three independent operations on the right, each one its own exhaustive switch with no accept() method anywhere on the node types.",
  "Because ManifestEntry's permits clause names exactly four types and each switch over it has no default branch, javac refuses to compile unless all four are covered, the identical completeness guarantee a Gang of Four visitor interface gets from declaring every visit() overload abstract, with no accept() boilerplate and no second class hierarchy to maintain alongside the first. A validation pass collects every issue into a list rather than throwing on the first one, because a compact record constructor that rejected a blank field immediately could only ever report the first problem found, and a merchant staring at a rejected manifest wants every reason at once. A set of kinds that stays open to a plugin fights a sealed switch instead of fitting it, because permits demands every implementor be known and compiled together; that case wants ordinary polymorphism instead."
);

patternSlide(
  "BEHAVIORAL · GOF CATALOGUE",
  "Memento",
  [
    "Editing a draft manifest sometimes needs to discard everything since an earlier step and start over, a whole-state rollback, not one reversed field edit.",
    "ManifestDraft.Snapshot is a package-private record: a caretaker can hold what save() returns and hand it back to restore(), but it can never read a field out of a type it cannot even name.",
    "Cloning an entire manifest to support undoing one keystroke is Command's job misapplied; name which rollback shape is actually needed, one field reverted or one whole batch discarded, before reaching for either.",
  ],
  undefined,
  undefined,
  "No access modifier on a nested record means package-private: a caller outside the originator's own package can hold whatever save() returns, assigned to a local declared with var, and pass that identical value straight into restore(), but it can never write out the snapshot's type in its own source and has no way to call an accessor on a value whose type it cannot name. That is Memento's classic contract done with Java's own package boundary instead of a language-level friend declaration. save() copies its entries list rather than aliasing it, because every entry inside is already an immutable record, so no deeper cloning is needed beyond that one copy. restore() replaces every field wholesale in a single call, undoing an entire editing session regardless of how many edits happened in between, with no caretaker ever needing to know how many it is discarding."
);

patternSlide(
  "BEHAVIORAL · GOF CATALOGUE",
  "Interpreter",
  [
    "A surcharge expression built from a handling fee, an overweight threshold, and a fuel factor needs to answer what the whole nested expression evaluates to, not just which pieces are present.",
    "A sealed SurchargeExpr interface evaluated by a record-pattern switch deconstructs Literal, Add, Multiply, and IfWeightAbove directly into named locals, with no accessor call anywhere in the method.",
    "The identical three pieces nested two different ways evaluate to two different totals, $16.20 against $15.80; that is precedence expressed as tree shape instead of as a parser's operator-binding rule.",
  ],
  undefined,
  undefined,
  "A record pattern is one more idiomatic way to write an exhaustive switch over a sealed hierarchy: Literal(long amountCents) pulls the record's single component straight into a local variable named in the case itself, with no accessor call anywhere in that arm, and a recursive case like Add(var left, var right) removes two accessor calls from every branch beneath it. Building the expression tree directly in code, rather than parsing text a user typed, sidesteps writing a tokenizer or a precedence-climbing parser entirely while keeping the exact behavior a parsed grammar would have; a grammar that needs to accept untrusted text is a different and much larger problem, best handed to an established parsing library rather than grown out of a tree-building example. Interpreter pays for itself when a grammar is small, stable, and built from a handful of call sites that construct expressions directly in code."
);

// ===========================================================================
// Section: Closing
// ===========================================================================
divider("06", "Closing", "The forces test, every time",
  "This closing section ties the five SOLID principles and the Gang of Four catalogue back to the single discipline that ran under every slide: forces first, then a name, then the idiomatic Java 25 expression the forces actually call for. The two closing slides summarize how Java's own menu of expressions, interfaces, lambdas, enums, records, and sealed types, changes which idiom resolves a given force best, and restate the forces test one last time as something that travels past this catalogue, into any review of a teammate's diff or an AI assistant's generated code.");

patternSlide(
  "CLOSING",
  "Choosing the idiomatic Java 25 expression",
  [
    "An interface with named classes fits a strategy, a decorator, or a rule that carries real state, a constructor, or a need to be discovered by reflection or dependency injection.",
    "A lambda fits a contract built once at a call site and discarded, composes two strategies or two decorators into a third value with no new class, and costs nothing beyond the one expression.",
    "An enum with constant-specific bodies fits a closed, known-today set where the compiler should refuse a missing case; a sealed interface with a record per kind fits a fixed structure that keeps gaining new operations, each dispatched through an exhaustive pattern-matching switch.",
  ],
  undefined,
  undefined,
  "This choice repeats across nearly every pattern in this catalogue, not as a stylistic preference but as part of the design decision itself: Strategy, Template Method's hooks, and a Decorator's wrapped behavior all resolve equally well as a named class or a one-expression lambda, and the right answer depends on whether the thing being expressed needs a name, a field, and a file, or whether it is built once and thrown away. A closed, small set of algorithms or statuses wants an enum or a sealed interface specifically for the compile-time completeness check neither a lambda nor an open interface can offer; an open set that a plugin or a third party might extend wants ordinary polymorphism instead, because a sealed permits clause demands every implementor be known and compiled together. None of these expressions is more correct than another in the abstract; each one is correct for a specific shape of force, and naming that shape before reaching for an idiom is the same discipline this whole catalogue trains for naming a pattern before reaching for its shape."
);

{
  const s = S();
  addContentTitle(s, "CLOSING", "How to choose: the forces test");
  addBullets(s, [
    "Ask what varies in the code, what stays fixed, and what is expensive to get wrong, before any pattern name enters the conversation.",
    "Name a candidate pattern and a candidate plain alternative side by side, and let the forces actually present decide between them.",
    "A pattern belongs in this catalogue because a force justifies it; the same pattern belongs in your code only when that force shows up there too.",
    "The test travels: it works the same way reviewing a teammate's pull request or an AI assistant's generated diff.",
  ], { fontSize: 17 });
  addNotes(s, "Every principle and pattern walked through in this deck, SOLID's five principles and the Gang of Four's catalogue grouped by intent, runs through the same arc: recognize the forces, name a candidate pattern and a candidate plain alternative, choose the idiomatic Java 25 expression, test the behavior, and know when it stops paying for itself. A pattern name shows up often enough in interviews that it is tempting to treat the whole catalogue as trivia; the actual skill is asking what an abstraction is buying you right now, in this code, for this team, and having a real answer either way. That question does not change whether the diff in front of you was written by a teammate or generated by an assistant, and it is the one question worth carrying out of this room.");
}

// ===========================================================================
// Appendix
// ===========================================================================
divider("07", "Appendix", "Reference tables and reading list, worth a second window",
  "The following reference tables are mined directly from the patterns-and-forces catalogue this deck just walked. Keep this section open in a second window during a design discussion: scan from the force you are facing back to the pattern name that answers it, rather than the other way around.");

refTable(
  "APPENDIX · REFERENCE",
  "Principles: SOLID",
  [
    { code: "Single Responsibility", name: "Principle", purpose: "A class has more than one reason to change, so one concern's edit risks another." },
    { code: "Open/Closed", name: "Principle", purpose: "New behavior should arrive as new code, not edits to working code." },
    { code: "Liskov Substitution", name: "Principle", purpose: "A subtype must be usable anywhere its base is, without surprising a caller." },
    { code: "Interface Segregation", name: "Principle", purpose: "A client is forced to depend on methods it never calls." },
    { code: "Dependency Inversion", name: "Principle", purpose: "High-level policy should not depend on low-level detail; both depend on an abstraction." },
  ],
  "A quick-reference scan from force to principle. Use this table the way the SOLID section used it: a competing requirement in the left-hand column names the principle that resolves it in the row. Keep this slide open as a checklist during a design review."
);

refTable(
  "APPENDIX · REFERENCE",
  "Creational patterns",
  [
    { code: "Factory Method", name: "Creational", purpose: "Which concrete type to build is a decision resolved at runtime, not at the call site." },
    { code: "Abstract Factory", name: "Creational", purpose: "A whole family of related objects must be built in a consistent variant together." },
    { code: "Builder", name: "Creational", purpose: "An object has many optional parts and construction should read as a sequence of choices." },
    { code: "Prototype", name: "Creational", purpose: "A new object is best made by copying a configured exemplar rather than rebuilding it." },
    { code: "Singleton", name: "Creational", purpose: "Exactly one instance must exist, it is effectively immutable, and nothing needs to substitute another." },
  ],
  "Five creational patterns, each mapped to the one force that justifies reaching for it. This is the same ledger used earlier for Factory Method, Abstract Factory, Builder, Prototype, and Singleton; use it as a scan-from-force lookup during a design discussion rather than re-deriving each pattern's intent from memory."
);

refTable(
  "APPENDIX · REFERENCE",
  "Structural patterns",
  [
    { code: "Adapter", name: "Structural", purpose: "An existing type has the right behavior behind the wrong interface." },
    { code: "Facade", name: "Structural", purpose: "A subsystem's many moving parts should present one small, intentional surface." },
    { code: "Bridge", name: "Structural", purpose: "An abstraction and its implementation must vary independently." },
    { code: "Decorator", name: "Structural", purpose: "Behavior should be added to one object at runtime without subclassing every combination." },
    { code: "Proxy", name: "Structural", purpose: "Access to an object needs a stand-in for lazy creation, caching, or control." },
    { code: "Composite", name: "Structural", purpose: "Clients should treat a single object and a tree of objects the same way." },
    { code: "Flyweight", name: "Structural", purpose: "Many objects share most of their state and the duplication is too expensive to keep." },
    { code: "Null Object", name: "Structural", purpose: "A do-nothing case should remove a null check from every call site." },
  ],
  "Eight structural entries, covering Adapter, Facade, and Bridge as a group, Decorator and Proxy as the pair told apart by intent, and Composite, Flyweight, and Null Object as the trio that shares one shipment-tree domain. Scan down the right column for the force in front of you."
);

refTable(
  "APPENDIX · REFERENCE",
  "Behavioral patterns (1 of 2)",
  [
    { code: "Strategy", name: "Behavioral", purpose: "An algorithm chosen by a caller must be swappable without touching the caller." },
    { code: "Template Method", name: "Behavioral", purpose: "A fixed sequence has steps that vary, and the sequence itself must not." },
    { code: "State", name: "Behavioral", purpose: "Behavior depends on a current status, and illegal transitions must be refused." },
    { code: "Command", name: "Behavioral", purpose: "A request must become an object so it can be queued, logged, or undone." },
    { code: "Observer", name: "Behavioral", purpose: "Subscribers must react to a change without the subject knowing who they are." },
    { code: "Mediator", name: "Behavioral", purpose: "Many objects' direct references to each other have become a tangle." },
  ],
  "The first six of eleven behavioral entries. Strategy and Template Method both touch an algorithm's shape, one by swapping it whole, one by varying a few of its steps. State, Command, Observer, and Mediator each coordinate a different kind of change: a status, a request, a notification, or a mesh of peers."
);

refTable(
  "APPENDIX · REFERENCE",
  "Behavioral patterns (2 of 2)",
  [
    { code: "Chain of Responsibility", name: "Behavioral", purpose: "A request should pass along a line of handlers until one takes it." },
    { code: "Iterator", name: "Behavioral", purpose: "A collection's elements must be traversed without exposing its structure." },
    { code: "Visitor", name: "Behavioral", purpose: "A new operation must be added across a fixed set of types without editing each." },
    { code: "Memento", name: "Behavioral", purpose: "An object's state must be captured and restored without exposing its internals." },
    { code: "Interpreter", name: "Behavioral", purpose: "A small, recurring grammar is best expressed as types that evaluate themselves." },
  ],
  "The remaining five behavioral entries. Chain of Responsibility and Iterator are paired by shape, coordinating a sequence of decisions versus a sequence of elements. Visitor, Memento, and Interpreter all operate over the same kind of fixed structure, told apart by what each one is for: adding an operation, snapshotting state, or evaluating a grammar."
);

{
  const s = S();
  addContentTitle(s, "APPENDIX · FOR DEEPER COVERAGE", "Reading list");
  addBullets(s, [
    "Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, Design Patterns: Elements of Reusable Object-Oriented Software (Addison-Wesley).",
    "Robert C. Martin, Agile Software Development: Principles, Patterns, and Practices (Prentice Hall).",
    "Joshua Bloch, Effective Java, third edition (Addison-Wesley).",
  ], { fontSize: 18 });
  addNotes(s, "Three sources behind everything in this deck. The Gang of Four book is the canonical catalogue this whole pattern-to-force ledger indexes, where each pattern's intent, structure, and consequences are set out in full. Robert Martin's book is the original statement of the SOLID principles and an early, explicit treatment of patterns chosen by the forces in front of you rather than applied from memory. Joshua Bloch's Effective Java is the idiomatic-Java throughline behind nearly every idiom choice in this deck: static factory methods, the enum singleton, favoring composition over inheritance, enum constants with constant-specific bodies, and returning Optional judiciously. All three are worth reading in full once a pattern's name alone stops being enough to settle a design review.");
}

// ===========================================================================
pres.writeFile({ fileName: OUT })
  .then(p => console.log("WROTE", p, "slides:", pageNum))
  .catch(e => { console.error(e); process.exit(1); });
