---
title: "Prerequisites and the toolchain"
order: 0
layout: tutorial
sectionid: docs
part: "orientation"
description: "The JDK 25, Maven, JUnit 5, Mockito, and AssertJ toolchain this book builds against, how the Maven example harness works, and why every code sample in this book is a single idiomatic Java 25 block."
duration: "15 min"
---

## Learning objectives

By the end of this chapter you can:

- Install and verify the exact local toolchain every example in this book builds with: JDK 25 via SDKMAN and Maven 3.9 or newer.
- Build and test any example with `mvn test`, both standalone and through the root aggregator.
- Open this repository in IntelliJ IDEA or VS Code and have either one drive the same Maven build the command line uses.
- Run `scripts/check-prereqs.sh` and read what it reports.
- Explain why every code sample in this book is a single idiomatic Java 25 block, and when a codetabs comparison is used instead to set two modern idioms side by side.

## Why this matters

Every pattern chapter in this book hands you real, compiling, tested Java. That promise only holds if the toolchain behind it is real on your machine before a single pattern shows up. There is no container here standing in for a missing JDK or a stale Maven install. You install JDK 25 directly, let Maven resolve JUnit 5, Mockito, and AssertJ from the pinned versions in the aggregator's `dependencyManagement`, and from there every example in the book builds the same way: `mvn test`. Once that loop works for you, it works for every chapter that follows, in either IDE you use or from a bare terminal.

{% include excalidraw.html name="00-build-and-test-loop" caption="Figure 0.1: The local build-and-test loop every example in this book runs: edit a .java file, mvn test compiles it against JDK 25 and runs the JUnit 5 suite (with Mockito and AssertJ), surefire reports pass or fail, and a failing assertion sends you back to the source. A standalone module and the root aggregator run the identical loop." %}

## Why local tools, no containers

A container is a fine way to pin a toolchain, but it also hides exactly the thing this book wants you to see: what `javac`, Maven, and JUnit actually do, and how they hand off to each other. Design patterns live at the level of source files, compilation units, and class loading; a reader who only ever typed `docker compose up` never watches Maven resolve a dependency tree, never watches `javac` reject a snippet for using a feature below its `--release` flag, and never develops the instinct for reading a failed Surefire run. This book's examples build straight on the host with JDK 25 and Maven installed directly. CI installs that same toolchain onto the runner with `actions/setup-java`, for the same reason any CI does that: a clean, reproducible environment for every run, not something this book ever asks you to run on your own machine.

## The stack, tool by tool

Three tools do all the work.

**JDK 25** is the Long-Term Support release this book targets, and the only Java version it teaches against. Every `pom.xml` in this repository pins `maven.compiler.release` to `25`, so a reader who builds against anything older gets a clear compiler error naming the feature that needs the newer release, rather than a subtle behavioral difference discovered later. This book installs the Temurin distribution, the OpenJDK build maintained by the Eclipse Adoptium project, because it is free, fully open source, and the build most CI providers already support with `actions/setup-java`.

**Maven** describes how each example compiles, which test runner picks up the result, and which third-party libraries (JUnit, Mockito, AssertJ) to resolve and at which exact version. This book needs Maven 3.9 or newer, the floor that supports the `maven.compiler.release` property cleanly alongside the JUnit 5 Platform's own Surefire provider.

**JUnit 5, Mockito, and AssertJ** are the testing stack every example's suite is written against. JUnit Jupiter supplies the test runner and the assertion and lifecycle annotations (`@Test`, `@BeforeEach`, `@DisplayName`). Mockito supplies collaborator mocks and verification for the cases where a pattern's test needs to observe a call rather than just a return value. AssertJ supplies the fluent, chainable assertions (`assertThat(quote).isEqualTo(...)`) that read as a sentence describing the expected behavior rather than a bare `assertEquals` call.

## Installing JDK 25 with SDKMAN

SDKMAN manages JDK versions without touching your system package manager, which matters once you have more than one Java project on the same machine expecting different versions. Install it if you don't already have it:

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
```

Then install the exact build this book is verified against:

```bash
sdk install java 25.0.2-tem
sdk use java 25.0.2-tem
```

The `-tem` suffix names the Temurin distribution; `sdk use` sets it for the current shell, and `sdk default java 25.0.2-tem` makes it the default for every new shell. Confirm it landed:

```bash
java -version
# openjdk version "25.0.2" 2026-01-20
# OpenJDK Runtime Environment Temurin-25.0.2+... (build 25.0.2+...)
```

Any Temurin 25 build satisfies this book's floor. SDKMAN also installs Maven, so the same tool handles both pieces of the toolchain:

```bash
sdk install maven
mvn -v
# Apache Maven 3.9.11
```

Maven 3.9.11 is the version this book's own toolchain is verified against; any Maven 3.9.x or newer works identically for every command in this book.

**JBang** rounds out the toolchain wherever this book touches Quarkus. Install it through SDKMAN alongside JDK 25 and Maven:

```bash
sdk install jbang
jbang version
```

Every Quarkus example in this book ships a JBang run path next to its Maven module: a single `.java` file under that example's `jbang/` directory with its own `//DEPS` line pinning the same Quarkus version the Maven module uses, runnable with `jbang jbang/<script>.java` and no project scaffolding. The Maven module remains the chapter's test of record; the JBang script is a faster way to read or prototype the same code.

## The build loop every example follows

Every example in this book, from the orientation chapters through the final project, builds and tests the exact same way: one command, `mvn test`, run from the example's own directory.

```console
$ cd examples/08-strategy
$ mvn test
```

Underneath, `mvn test` resolves the dependencies pinned in the aggregator's `dependencyManagement` (JUnit 5.13.4 via the JUnit BOM, Mockito 5.24.0, AssertJ 3.27.7), compiles every `.java` file under `src/main/java` against `--release 25`, compiles the test sources under `src/test/java`, and hands the compiled test classes to the Surefire plugin. Surefire discovers every `@Test`-annotated method through the JUnit Platform and reports pass or fail per test method, not just per module.

This same command works two ways, and both are first-class:

**Standalone**, from inside one example's own directory. A reader who cares about exactly one pattern never has to build every other example in the repository:

```bash
cd examples/08-strategy && mvn test
```

**From the root aggregator**, which builds every example module in the book in one pass, useful for CI and for a reader working straight through:

```bash
cd examples && mvn test
```

The root `examples/pom.xml` declares one `<module>` entry per hands-on chapter's example and centralizes the JUnit, Mockito, and AssertJ version pins in its `dependencyManagement`, so every example module adds those dependencies without a `<version>`, inheriting the pin from one place. Changing the test-stack version for the whole book is a one-line edit to the aggregator, not a search-and-replace across every module.

## Verifying your setup

`scripts/check-prereqs.sh` checks the floor versions for `java` and `mvn` and prints exactly what it found, without resolving any dependencies or building anything:

```console
$ ./scripts/check-prereqs.sh
Checking local toolchain for Design Patterns with Java...

OK: java -> openjdk version "25.0.2" 2026-01-20
OK: mvn -> Apache Maven 3.9.11

All prerequisites found.
```

Your own output shows whatever versions SDKMAN installed for you, which is expected; the script exits non-zero and names the specific missing or too-old tool if something falls short of the floor, rather than letting a mysterious compile error be the first signal something is wrong.

## Build, run, observe

Once `check-prereqs.sh` passes, confirm the whole loop end to end against the aggregator:

```bash
cd examples
mvn test
```

A clean run ends with a `BUILD SUCCESS` line and a per-module summary of tests run, failures, and errors (all zero on a passing build). That is the same signal every hands-on chapter in this book asks you to reproduce for its own example: change one thing, run `mvn test` again, and read the Surefire output to confirm the pattern's claimed behavior still holds.

## Using this book in IntelliJ IDEA

Open the repository's `examples/` directory as a Maven project; IntelliJ reads `examples/pom.xml` and imports every module automatically, including the test dependencies pinned in its `dependencyManagement`. IntelliJ's own bundled JUnit 5 runner recognizes every `@Test` method with a green gutter icon you can run or debug individually, against the same compiled output `mvn test` would produce from the command line. If IntelliJ's auto-detected project SDK is not JDK 25, set it under **File > Project Structure > Project SDK** to the SDKMAN-installed 25.0.2-tem build before the first build; after that, IntelliJ's own incremental compiler and the command-line `mvn test` stay interchangeable for every chapter that follows.

## Using this book in VS Code

With the Extension Pack for Java installed (which bundles the Language Support for Java extension, the Maven for Java extension, and the Test Runner for Java), opening the repository folder is enough. VS Code's Java extension reads `examples/pom.xml`, resolves the same dependency tree Maven would from the command line, and the Testing sidebar lists every JUnit 5 test method discovered in the project, runnable or debuggable individually. The integrated terminal runs the identical `mvn test` command either way, so you can switch between the Testing sidebar and the command line mid-chapter without reconfiguring anything.

## How code samples appear in this book

Every code sample in this book is idiomatic modern Java 25, shown once, in a single fenced block. There is no older-Java tab to switch away from; the block you read is the form this book commits to, compiles against, and tests. A pattern's shape rarely changes between Java versions, but its idiomatic expression does, and the prose around a sample names that whenever it matters: a record replacing hand-written getters, equals, hashCode, and a defensive copy; a sealed interface and a pattern-matching switch replacing an `instanceof` chain; a switch expression replacing a switch statement that needed an explicit `break` on every branch. Naming that retirement, pattern by pattern, is as much a part of this book as the Gang of Four catalogue itself.

Occasionally two modern idioms both solve the same problem well enough that seeing them side by side clarifies the choice between them, never a modern form set against a retired one. For those cases this book reaches for the `codetabs.html` include, switchable with a click and labeled with the two idioms actually being compared. Here is the convention applied to the smallest useful example: describing a shipment's status, once as a lookup table and once as a switch expression.

{% include codetabs.html langs="Lookup map|Switch expression" %}

```java
// Lookup map: a Map<ShipmentStatus, String> built once, at class-init time.
enum ShipmentStatus { PENDING, IN_TRANSIT, DELIVERED, DELAYED }

private static final Map<ShipmentStatus, String> DESCRIPTIONS = Map.of(
    ShipmentStatus.PENDING, "waiting for pickup",
    ShipmentStatus.IN_TRANSIT, "en route",
    ShipmentStatus.DELIVERED, "delivered",
    ShipmentStatus.DELAYED, "delayed, revised ETA pending"
);

static String describe(ShipmentStatus status) {
    return DESCRIPTIONS.get(status);
}
```
```java
// Switch expression: exhaustive over the enum, no fallthrough or default.
enum ShipmentStatus { PENDING, IN_TRANSIT, DELIVERED, DELAYED }

static String describe(ShipmentStatus status) {
    return switch (status) {
        case PENDING -> "waiting for pickup";
        case IN_TRANSIT -> "en route";
        case DELIVERED -> "delivered";
        case DELAYED -> "delayed, revised ETA pending";
    };
}
```

Both compile under this chapter's toolchain and produce the same output; the choice between them is a real design decision, not a vestige of an older release. Most chapters from here on never need this include at all. A single Java 25 block is the default for every sample in this book, and a codetab only appears where two modern idioms are both worth setting side by side.

## How this book works

Every chapter after this one follows the same shape: learning objectives up front, a diagram, a "why this matters" section before any mechanics, the body, and a "for deeper coverage" pointer at the end to the books behind this book's opinions. Chapters group into parts, each with its own landing page listing the chapters inside it; this chapter is the first in the Orientation part. Every pattern chapter additionally closes with a "when not to use it" section, because a pattern applied past the point it still pays for itself is a mistake, named as clearly as the pattern itself, and carries exactly one short sidebar on how an AI pairing partner tends to reach for that pattern, and where its judgment needs a check.

Every pattern chapter also ships a runnable example under `examples/NN-pattern-name/`, built from new, real-domain Java written for this book: a reservation system, a telemetry pipeline, a billing engine, never a placeholder `Foo`/`Bar` class hierarchy. Each example carries a JUnit 5 suite, using Mockito and AssertJ, that verifies the pattern's claimed behavior, not just that the build exits zero. Once the loop in this chapter works for you, it works identically for every one of those examples: `mvn test`, same command, same output shape, from the first chapter to the last.

## For deeper coverage

- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991) (Addison-Wesley), third edition: the source for the static-factory and immutability idioms this book's Java 25 examples apply throughout.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
