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

- Install and verify the local toolchain: JDK 25 through SDKMAN and Maven 3.9 or newer.
- Build and test any example with `mvn test`, standalone or through the root aggregator.
- Open the repository in IntelliJ IDEA or VS Code and run the same Maven build from either.
- Run `scripts/check-prereqs.sh` and read what it reports.
- Read a code sample as one idiomatic Java 25 block, and a codetab as two modern idioms side by side.

## Why this matters

Every example compiles and runs its tests on your machine, with no container standing in for the JDK. Install JDK 25 and Maven once, and every chapter builds the same way: `mvn test`, from a terminal or an IDE.

{% include excalidraw.html name="00-build-and-test-loop" caption="Figure 0.1: The local build-and-test loop every example in this book runs: edit a .java file, mvn test compiles it against JDK 25 and runs the JUnit 5 suite (with Mockito and AssertJ), surefire reports pass or fail, and a failing assertion sends you back to the source. A standalone module and the root aggregator run the identical loop." %}

## Why local tools, no containers

Patterns live in source files, compilation units and class loading. Building on the host keeps that visible: you watch Maven resolve the dependency tree, `javac` reject a feature above its `--release` level, and Surefire report which test failed. CI installs the same toolchain on its runner with `actions/setup-java`, so a clean run there matches a clean run on your machine.

## The stack, tool by tool

| Tool | Version | Role |
| --- | --- | --- |
| JDK | 25 (Temurin 25.0.2) | The Long-Term Support release and the only Java version taught. Every `pom.xml` sets `maven.compiler.release` to `25`, so an older JDK fails with a compiler error naming the feature. |
| Maven | 3.9 or newer (3.9.11) | Compiles each example, runs Surefire and resolves the JUnit, Mockito and AssertJ libraries at pinned versions. |
| JUnit Jupiter | 5.13.4, through the JUnit BOM | Runs the tests and supplies the `@Test`, `@BeforeEach` and `@DisplayName` annotations. |
| Mockito | 5.24.0 | Collaborator mocks and call verification, where a test observes a call rather than a return value. |
| AssertJ | 3.27.7 | Fluent assertions such as `assertThat(quote).isEqualTo(...)`. |
| JBang | current, through SDKMAN | Runs the single-file script beside the Quarkus example, which pins the Quarkus LTS, 3.40.1.1. |

Temurin is the Eclipse Adoptium build of OpenJDK. It's free, open source, and the build `actions/setup-java` supports out of the box.

## Installing JDK 25 with SDKMAN

SDKMAN manages JDK versions without the system package manager, so two projects on one machine can each use their own. Install it:

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
```

Then install the build the examples are verified against:

```bash
sdk install java 25.0.2-tem
sdk use java 25.0.2-tem
```

The `-tem` suffix names Temurin. `sdk use` sets it for the current shell, and `sdk default java 25.0.2-tem` makes it the default for new shells. Confirm it:

```bash
java -version
# openjdk version "25.0.2" 2026-01-20
# OpenJDK Runtime Environment Temurin-25.0.2+... (build 25.0.2+...)
```

Any JDK 25 build meets the floor. SDKMAN installs Maven too:

```bash
sdk install maven
mvn -v
# Apache Maven 3.9.11
```

Any Maven 3.9.x or newer runs every command here.

Install JBang the same way, for the Quarkus example:

```bash
sdk install jbang
jbang version
```

The Quarkus example ships a JBang run path next to its Maven module: one `.java` file under its `jbang/` directory, with a `//DEPS` line pinning the same Quarkus version as the module, run with `jbang jbang/<script>.java`. The Maven module holds the tests; the script is a faster way to read or try the same code.

## The build loop every example follows

Every example builds and tests with one command, run from its own directory:

```console
$ cd examples/08-strategy
$ mvn test
```

`mvn test` resolves the pinned dependencies, compiles `src/main/java` against `--release 25`, compiles `src/test/java`, and hands the compiled tests to Surefire. Surefire finds every `@Test` method through the JUnit Platform and reports pass or fail per method.

The command works two ways:

- **Standalone**, from one example's directory, when you care about one pattern: `cd examples/08-strategy && mvn test`.
- **From the root aggregator**, which builds every example in one pass, for CI or for reading straight through: `cd examples && mvn test`.

The root `examples/pom.xml` lists one `<module>` per hands-on chapter and pins JUnit, Mockito and AssertJ in its `dependencyManagement`. Example modules declare those dependencies without a `<version>`, so each version changes in one place, the aggregator.

## Verifying your setup

`scripts/check-prereqs.sh` checks the floor versions of `java` and `mvn` and prints what it found, without resolving dependencies or building anything:

```console
$ ./scripts/check-prereqs.sh
Checking local toolchain for Design Patterns with Java...

OK: java -> openjdk version "25.0.2" 2026-01-20
OK: mvn -> Apache Maven 3.9.11

All prerequisites found.
```

Your output shows the versions you installed. When a tool is missing or below the floor, the script names it and exits non-zero, before a compile error can.

## Build, run, observe

Once `check-prereqs.sh` passes, run the whole aggregator:

```bash
cd examples
mvn test
```

All 32 example modules build and their 737 tests pass, ending in `BUILD SUCCESS`. Each hands-on chapter repeats the loop for its own example: change one thing, run `mvn test`, and read the Surefire report.

## Using IntelliJ IDEA

Open `examples/` as a Maven project. IntelliJ reads `examples/pom.xml`, imports every module with its pinned test dependencies, and puts a run icon in the gutter beside every `@Test` method. If the project SDK isn't JDK 25, set it under **File > Project Structure > Project SDK** to the SDKMAN `25.0.2-tem` build before the first build. After that the IDE build and `mvn test` are interchangeable.

## Using VS Code

Install the Extension Pack for Java, which bundles Language Support for Java, Maven for Java and the Test Runner for Java, and open the repository folder. VS Code reads `examples/pom.xml`, resolves the same dependency tree, and lists every JUnit test in the Testing sidebar, each runnable or debuggable on its own. The integrated terminal runs the same `mvn test`.

## How code samples appear

Every code sample is idiomatic Java 25, shown once in a single fenced block. There's no older-Java tab: the block you read is the code that compiles and passes its tests. A pattern's shape rarely changes between Java versions, but its expression does, and the prose names the change where it matters:

- a record replaces hand-written getters, `equals`, `hashCode` and a defensive copy;
- a sealed interface and a pattern-matching `switch` replace an `instanceof` chain;
- a `switch` expression replaces a `switch` statement that needed `break` on every branch.

When two modern idioms both solve a problem well, a codetab sets them side by side, labeled with the two idioms. It never sets a modern form against a retired one. Here are both ways of describing a shipment's status:

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

Both produce the same descriptions. The map is data you can load or extend; the `switch` expression is checked for exhaustiveness at compile time, so a new constant without a case fails the build. A single block is the default, and a codetab appears only where both idioms are worth showing.

## How the chapters are laid out

Every chapter opens with learning objectives, a figure and "Why this matters", then the body, and closes with "For deeper coverage", which points to the books behind its advice. Chapters group into parts, each with a landing page; this one opens Orientation.

Pattern chapters add three things:

- a `Pattern:` section per pattern, with the problem, a one-sentence solution and how Java models it;
- a "When not to use it" section, because a pattern applied past its payoff is a mistake;
- one "Explore with your agent" prompt you can paste, naming the types and decisions of the pattern at hand.

Hands-on chapters ship a runnable example under `examples/NN-pattern-name/`, written in a shipping domain of consignments, carriers, rate quotes, routing, tracking and billing, never a `Foo`/`Bar` hierarchy. Each example's JUnit tests check the behavior the pattern claims, not only that the build exits zero.

## For deeper coverage

- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991) (Addison-Wesley), third edition: the source for the static-factory and immutability idioms this book's Java 25 examples apply throughout.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
