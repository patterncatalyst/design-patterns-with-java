# Design Patterns with Java

Gang of Four design patterns, the SOLID principles, and the Java idioms that
express them, taught as decisions under forces with a runnable, JUnit
5-backed example for every pattern in Java 25.

**Live site:** https://patterncatalyst.github.io/design-patterns-with-java/

This is the second book in a planned series, after the completed
[Design Patterns with C++](https://patterncatalyst.github.io/cpp-design-patterns/);
.NET/C# and Python editions follow. It is part of the
[Pattern Catalyst](https://github.com/patterncatalyst) collection.

## What this is

A chapter-based book and tutorial site for new and intermediate Java
engineers who want to recognize what to build and why, not memorize UML
diagrams. Each pattern is shown as a response to a set of forces,
implemented idiomatically in Java 25, tested, and paired with a note on when
not to use it. The book also covers Java-specific idioms (records and sealed
interfaces, static factory methods, functional interfaces in place of
classic callback hierarchies), virtual threads and concurrency, patterns as
they appear in production, a legacy-code modernization track, and a chapter
on working with AI and LLM tools.

Where Java 25 retires a classic idiom, the codetabs convention shows both
side by side: **Classic Java** and **Java 25**.

## Prerequisites

Local tools only, no containers.

| Tool | Version |
| --- | --- |
| JDK | 25 (Temurin, via SDKMAN) |
| Maven | 3.9 or newer |
| JUnit Jupiter | 5.13.4 |
| Mockito | 5.24.0 |
| AssertJ | 3.27.7 |

Run `scripts/check-prereqs.sh` to verify the toolchain. Chapter 0 covers
setup in full.

## Build and test

Every example is standalone-buildable and also part of a root aggregator:

```sh
./scripts/build.sh       # mvn -f examples/pom.xml package (tests skipped)
./scripts/verify.sh      # mvn -f examples/pom.xml test
```

A single example on its own:

```sh
cd examples/08-strategy
mvn test
```

## Layout

| Path | Contents |
| --- | --- |
| `_docs/` | Chapters (`NN-*.md`) |
| `_parts/` | Part landing pages |
| `examples/` | One runnable JUnit 5 example per hands-on chapter, plus the aggregator `pom.xml` |
| `diagrams/` | Paired `.svg` + `.excalidraw` figures |
| `_plans/` | Build plans and verification status (not published) |

## Reading the site locally

```sh
bundle install
bundle exec jekyll serve
```

## Status

r01 scaffolds the site skeleton, the Maven example harness, and CI, and
ships the first vertical slice: Chapter 0 (prerequisites), Chapter 1
(patterns as decisions under forces), and Chapter 8 (Strategy) with its
`examples/08-strategy` demo. Everything else in the table of contents is
still to come.

## License

Apache License 2.0. See [LICENSE](LICENSE).
