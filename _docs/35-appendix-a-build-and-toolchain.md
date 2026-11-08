---
title: "Build and toolchain reference"
order: 35
layout: tutorial
sectionid: docs
part: appendices
appendix: "A"
description: "Reference for building and testing every example: the pinned toolchain, the Maven aggregator and per-module layout, standalone versus whole-repository builds, the scripts/ helpers, both IDEs, and the Quarkus plus JBang track."
duration: "15 min"
---

## Learning objectives

By the end of this appendix you can:

- Name the pinned version of each tool and where the pin lives.
- Build and test one example on its own, or every example from the aggregator.
- Change a test-library version for every module with one edit.
- Run the Quarkus example under Maven and as a single-file JBang script.

## Why this matters

Every example builds the same way, with the host's own JDK, Maven and (for the Quarkus track) JBang. Nothing in the build or test loop runs a container. First-time setup is in [Prerequisites]({{ '/docs/00-prerequisites/' | relative_url }}); this page is the reference to return to once the tools are installed.

## The pinned toolchain

| Tool | Floor | Verified against | Role |
|------|-------|-------------------|------|
| JDK | 25 | 25.0.2-tem (Temurin, via SDKMAN) | Compiles and runs every example; `maven.compiler.release` is pinned to `25` |
| Maven | 3.9 | 3.9.11 | Build, dependency resolution, test orchestration |
| JUnit Jupiter | - | 5.13.4 (via the JUnit BOM) | Test runner, lifecycle, and assertion annotations |
| Mockito | - | 5.24.0 | Collaborator mocks and call verification |
| AssertJ | - | 3.27.7 | Fluent, chainable assertions |
| maven-compiler-plugin | - | 3.16.0 | Compiles against `--release 25` |
| maven-surefire-plugin | - | 3.6.0 | Discovers and runs `@Test` methods through the JUnit Platform |
| Quarkus (Quarkus track only) | - | 3.40.1.1 LTS | Runtime and test framework for the CDI example |
| JBang (Quarkus track only) | - | latest via SDKMAN | Single-file run path for the Quarkus example |

Any JDK 25 build and any Maven 3.9.x or newer work for every command below.

`scripts/check-prereqs.sh` checks the floor versions of `java` and `mvn` and prints `OK`, `MISSING` or `WARNING` per tool. It installs nothing and resolves no dependency, so it's safe to run at any time:

```sh
./scripts/check-prereqs.sh
```

It doesn't check JUnit, Mockito or AssertJ. Those are pinned in the `dependencyManagement` of `examples/pom.xml`, and Maven enforces the pin when it first resolves a module.

## Installing with SDKMAN

SDKMAN manages the JDK, Maven and JBang without touching the system package manager:

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

sdk install java 25.0.2-tem
sdk use java 25.0.2-tem
sdk install maven
sdk install jbang
```

JBang is needed only for the Quarkus track.

## The aggregator and per-module layout

The root `examples/pom.xml` is a `pom`-packaged aggregator with one `<module>` entry per example, in chapter order. Its `dependencyManagement` pins JUnit, Mockito and AssertJ:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.junit</groupId>
            <artifactId>junit-bom</artifactId>
            <version>${junit-bom.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>${mockito.version}</version>
            <scope>test</scope>
        </dependency>
        <!-- mockito-junit-jupiter and assertj-core follow the same shape -->
    </dependencies>
</dependencyManagement>
```

Each module names the aggregator as its `<parent>` and declares JUnit, Mockito and AssertJ with no `<version>`:

```xml
<parent>
    <groupId>com.patterncatalyst.designpatterns</groupId>
    <artifactId>design-patterns-with-java</artifactId>
    <version>1.0.0</version>
    <relativePath>../pom.xml</relativePath>
</parent>

<artifactId>dp-03-solid-first-principles</artifactId>
<packaging>jar</packaging>
```

A test-library upgrade is one property edit in the aggregator. The Quarkus example also imports the Quarkus platform BOM in its own `pom.xml`, because Quarkus coordinates come from that BOM.

## What `mvn test` does

`mvn test` compiles `src/main/java` against `--release 25`, compiles `src/test/java`, and hands the compiled tests to Surefire, which runs every `@Test` method through the JUnit Platform and reports a count per class and a total. A pass means the assertions ran: AssertJ for values, Mockito where a collaborator's calls are the behavior. Exit `0` with no tests run isn't a pass.

## Building one example or all of them

| Build | Command | Use it when |
| --- | --- | --- |
| One example | `cd examples/08-strategy && mvn test` | You're reading one pattern |
| Every example | `cd examples && mvn test` | You're checking the whole set, as CI does |

A standalone build finds its parent through `<relativePath>../pom.xml</relativePath>`, so it works from a full checkout with no extra setup. A new module needs only a `<module>` entry in the aggregator's `<modules>` list.

## The `scripts/` helpers

```sh
./scripts/check-prereqs.sh   # verify java/mvn floor versions; installs nothing
./scripts/build.sh           # mvn -f examples/pom.xml package, tests skipped
./scripts/verify.sh          # mvn -f examples/pom.xml test, every module
```

Both `build.sh` and `verify.sh` resolve the repository root from their own location, so they run from any directory. `mvn test` compiles on its own; `build.sh` exists to separate a compile failure from a test failure.

## IDEs

| IDE | Setup |
| --- | --- |
| IntelliJ IDEA | Open `examples/` as a Maven project; every module imports with its test dependencies. The bundled JUnit 5 runner puts run and debug icons in the gutter. If the project SDK isn't JDK 25, set it under **File > Project Structure > Project SDK**. |
| VS Code | Install the Extension Pack for Java (Language Support for Java, Maven for Java, Test Runner for Java) and open the repository folder. The Testing sidebar lists every JUnit 5 test, and the integrated terminal runs the same `mvn test`. |

## The Quarkus and JBang track

The CDI example, a shipping-rate checkout service, runs on Quarkus. It adds a Quarkus Maven module and a JBang run path.

**The Maven module** imports the Quarkus platform BOM, pinned to the LTS line:

```xml
<properties>
    <quarkus.platform.group-id>io.quarkus.platform</quarkus.platform.group-id>
    <quarkus.platform.artifact-id>quarkus-bom</quarkus.platform.artifact-id>
    <quarkus.platform.version>3.40.1.1</quarkus.platform.version>
</properties>
```

It declares one extension, `quarkus-arc`, and tests against `io.quarkus:quarkus-junit`, the coordinate `@QuarkusTest` comes from (`quarkus-junit5` is now a relocation stub). With no datasource or messaging extension, Dev Services has nothing to start, so `mvn test` boots Arc in-process with no Docker and no network:

```bash
cd examples/28-quarkus-cdi
mvn test
```

**JBang** installs through SDKMAN:

```bash
sdk install jbang
jbang version
```

The script under `jbang/` pins the same Quarkus version in a `//DEPS` line:

```java
//usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.quarkus:quarkus-arc:3.40.1.1
```

It runs with no `pom.xml` and no prior build:

```bash
jbang jbang/cdi-patterns.java
```

JBang sees the `io.quarkus:quarkus-arc` dependency and hands the file to Quarkus's JBang integration, which runs the same build-time augmentation (bean discovery and Arc bytecode generation) that `quarkus-maven-plugin` runs for `mvn test`. That step is required: annotation-based Arc beans are processed at build time, so a bare `Arc.initialize()` over annotated classes wires nothing.

The Maven module's `@QuarkusTest` classes are the tests of record; the script is a quicker way to run the same code. A system property before the script path swaps the configured rate strategy:

```bash
jbang -Dshipping.rate.strategy=flat jbang/cdi-patterns.java
```

## Build, run, observe

```bash
./scripts/check-prereqs.sh
cd examples/03-solid-first-principles && mvn test
cd ../28-quarkus-cdi && mvn test && jbang jbang/cdi-patterns.java
```

The SOLID example passes 29 tests and the Quarkus example 7. The script prints a weight-tiered quote for `TRK-100`, the interceptor's log line and the two event observers' output.

- Run the script with `-Dshipping.rate.strategy=flat`: the quote becomes `flat-rate $5.00`.

## For deeper coverage

- The official [Apache Maven documentation](https://maven.apache.org/guides/)
  for the aggregator, `dependencyManagement`, and plugin mechanics this build
  relies on.
- The official [JUnit 5 user guide](https://junit.org/junit5/docs/current/user-guide/)
  for the test runner and lifecycle annotations every example uses.
- The official [Quarkus documentation](https://quarkus.io/guides/) for the Arc
  container, `@QuarkusTest`, and the platform BOM the CDI example builds on.
- The official [JBang documentation](https://www.jbang.dev/documentation/guide/latest/index.html)
  for the `//DEPS` directive and the single-file run model of the Quarkus
  script.
