---
title: "Build and toolchain reference"
order: 35
layout: tutorial
sectionid: docs
part: appendices
appendix: "A"
description: "A one-stop reference for building and testing every example in this book: the pinned toolchain, the Maven aggregator and per-module layout, standalone versus whole-repository builds, the scripts/ helpers, both IDEs, and the Quarkus plus JBang track."
duration: "15 min"
---

## What this appendix covers

Every example in this book builds the same way, with the same local tools and no
containers. This appendix gathers the mechanics in one place: the pinned
toolchain, the Maven aggregator and per-module layout, the two ways to build an
example, the `scripts/` helpers, both supported IDEs, and the Quarkus and JBang
track that a handful of examples add on top.

The prerequisites chapter walks a first-time setup end to end. This appendix is
the reference to return to once the toolchain is installed.

## Reference platform

Everything runs with the host's own JDK, Maven, and (where it applies) JBang
installation. There is no container anywhere in the build or test loop, and no
example ever asks a reader to run one. Any recent platform with a JDK 25
distribution works; the pinned versions below are what the examples are verified
against.

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

`scripts/check-prereqs.sh` checks the floor versions for `java` and `mvn` and
prints exactly what it found:

```sh
./scripts/check-prereqs.sh
```

It reports `OK` or `MISSING`/`WARNING` per tool and never installs anything or
resolves a dependency, so it is safe to run at any time. JUnit, Mockito, and
AssertJ are not checked by this script; their versions are pinned in
`examples/pom.xml`'s `dependencyManagement`, so Maven itself enforces them the
first time it resolves a module.

## Installing the toolchain with SDKMAN

SDKMAN manages JDK, Maven, and JBang versions without touching the system
package manager:

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

sdk install java 25.0.2-tem
sdk use java 25.0.2-tem
sdk install maven
sdk install jbang
```

Any Temurin 25 build satisfies the floor, and any Maven 3.9.x or newer works
identically for every command in this appendix. JBang is only needed for the
Quarkus track, covered at the end.

## The Maven aggregator and per-module layout

The root `examples/pom.xml` is a `pom`-packaged superbuild that declares one
`<module>` entry per hands-on chapter's example, in chapter order, and
centralizes the test-stack pins in its own `dependencyManagement`:

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

Each example module declares a `<parent>` pointing at this aggregator and adds
its JUnit, Mockito, and AssertJ dependencies with no `<version>`, inheriting the
pin from one place:

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

Changing the JUnit, Mockito, or AssertJ version for the whole book is a one-line
edit to the aggregator's `dependencyManagement`, not a search-and-replace across
every module. The one example on the Quarkus track (covered below) imports the
Quarkus platform BOM in its own `pom.xml` on top of what it inherits from the
aggregator, since Quarkus coordinates come from that BOM rather than from the
book's own `dependencyManagement`.

## How `mvn test` runs the JUnit suite

`mvn test` compiles every `.java` file under `src/main/java` against
`--release 25`, compiles the sources under `src/test/java`, and hands the
compiled test classes to Surefire. Surefire discovers every `@Test`-annotated
method through the JUnit Platform and reports pass or fail per test method:

```console
$ cd examples/03-solid-first-principles
$ mvn test
...
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

A passing run proves the pattern's claimed behavior, asserted with AssertJ and,
where a collaborator needs to be observed rather than just return a value,
verified with Mockito. A green build alone (exit `0` with no tests executed)
never stands in for that.

## Building one example standalone

Every example under `examples/` is standalone-buildable on its own. A reader who
cares about one pattern never has to touch the rest of the repository:

```bash
cd examples/08-strategy
mvn test
```

The module's own `pom.xml` resolves its `<parent>` from `examples/pom.xml` on
the local filesystem (`<relativePath>../pom.xml</relativePath>`), so this works
from a full checkout with no extra configuration.

## Building the whole set from the aggregator

The same command run from `examples/` builds and tests every module in one
pass, the path CI and a straight-through read both use:

```bash
cd examples
mvn test
```

A new module needs one addition: a `<module>` entry in `examples/pom.xml`'s
`<modules>` list. Nothing else in the aggregator depends on how many modules
exist.

## The `scripts/` helpers

Three scripts wrap the common sequences from the repository root:

```sh
./scripts/check-prereqs.sh   # verify java/mvn floor versions; installs nothing
./scripts/build.sh           # mvn -f examples/pom.xml package, tests skipped
./scripts/verify.sh          # mvn -f examples/pom.xml test, every module
```

`build.sh` and `verify.sh` both resolve the repository root relative to their
own location, so they run correctly from any working directory. Running
`build.sh` before `verify.sh` is optional. `mvn test` compiles what it needs on
its own; the split exists so a reader can confirm a clean compile separately
from a clean test run when narrowing down a failure.

## Using this book in IntelliJ IDEA

Open the repository's `examples/` directory as a Maven project. IntelliJ reads
`examples/pom.xml` and imports every module automatically, including the test
dependencies pinned in its `dependencyManagement`. The bundled JUnit 5 runner
recognizes every `@Test` method with a runnable or debuggable gutter icon,
against the same compiled output `mvn test` produces from the command line. If
the auto-detected project SDK is not JDK 25, set it under **File > Project
Structure > Project SDK** to the SDKMAN-installed 25.0.2-tem build before the
first build.

## Using this book in VS Code

Install the Extension Pack for Java, which bundles the Language Support for
Java extension, the Maven for Java extension, and the Test Runner for Java.
Opening the repository folder is enough: the Java extension reads
`examples/pom.xml`, resolves the same dependency tree Maven would from the
command line, and the Testing sidebar lists every JUnit 5 test method
discovered in the project, runnable or debuggable individually. The integrated
terminal runs the identical `mvn test` command, so switching between the
Testing sidebar and the command line mid-chapter needs no reconfiguration.

## The Quarkus and JBang track

One example in this book, the CDI chapter's shipping-rate checkout service,
builds against Quarkus instead of plain JUnit on Arc alone. It adds two things
on top of everything above: a Quarkus-flavored Maven module, and a JBang
run path.

**The Quarkus Maven module.** The example's own `pom.xml` imports the Quarkus
platform BOM, pinned to the current LTS line:

```xml
<properties>
    <quarkus.platform.group-id>io.quarkus.platform</quarkus.platform.group-id>
    <quarkus.platform.artifact-id>quarkus-bom</quarkus.platform.artifact-id>
    <quarkus.platform.version>3.40.1.1</quarkus.platform.version>
</properties>
```

The module declares exactly one Quarkus extension, `quarkus-arc`, and tests
against `io.quarkus:quarkus-junit` (the coordinate `@QuarkusTest` resolves
from today; `quarkus-junit5` is now a relocation stub pointing at it). With no
datasource, messaging, or other extension in the module, there is nothing for
Quarkus Dev Services to start, so `mvn test` boots the Arc container in-process
on the local JVM with no Docker and no network access:

```bash
cd examples/28-quarkus-cdi
mvn test
```

**JBang.** Install it through SDKMAN alongside the JDK and Maven:

```bash
sdk install jbang
jbang version
```

The example ships a single-file JBang script under its own `jbang/` directory,
with an inline `//DEPS` line pinning the identical Quarkus version the Maven
module uses:

```java
//usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.quarkus:quarkus-arc:3.40.1.1
```

Running it needs no project scaffolding, no `pom.xml`, and no prior build step:

```bash
jbang jbang/cdi-patterns.java
```

JBang detects the `io.quarkus:quarkus-arc` dependency line and hands the file
to Quarkus's own JBang integration, which runs the same build-time
augmentation, bean discovery and Arc bytecode generation, that the Maven
module's `mvn test` triggers through the `quarkus-maven-plugin`. A plain
`Arc.initialize()` call over annotated classes with no augmentation step would
not wire the beans, since annotation-based Arc beans are processed at build
time; the JBang script gets that build step from JBang itself, so every
annotation in the script is live, not simulated.

The Maven module stays the example's test of record: its `@QuarkusTest` suite
is what a passing build verifies. The JBang script is a faster way to
read or run the same code without a Maven invocation, and it accepts a system
property override before the script path to swap its externalized
configuration at the command line:

```bash
jbang -Dshipping.rate.strategy=flat jbang/cdi-patterns.java
```

## For deeper coverage

- The official [Apache Maven documentation](https://maven.apache.org/guides/)
  for the aggregator, `dependencyManagement`, and plugin mechanics this build
  relies on.
- The official [JUnit 5 user guide](https://junit.org/junit5/docs/current/user-guide/)
  for the test runner and lifecycle annotations every example's suite is
  written against.
- The official [Quarkus documentation](https://quarkus.io/guides/) for the Arc
  container, `@QuarkusTest`, and the platform BOM the CDI example builds on.
- The official [JBang documentation](https://www.jbang.dev/documentation/guide/latest/index.html)
  for the `//DEPS` directive and the single-file run model the Quarkus
  example's script uses.
