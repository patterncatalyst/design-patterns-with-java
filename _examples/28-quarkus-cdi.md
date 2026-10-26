---
layout: example
order: 28
title: "Patterns in a real framework: Quarkus and CDI"
description: The book's patterns as Quarkus/CDI framework features, verified with @QuarkusTest and no container.
github_path: examples/28-quarkus-cdi
card_eyebrow: "Chapter 28 · Quarkus and CDI"
card_title: "Patterns as framework features"
card_blurb: "A Quarkus (Arc) module showing the same forces answered by the container: @ApplicationScoped + constructor @Inject as Singleton/DI, @Produces as Factory, an @AroundInvoke interceptor as Decorator/Proxy, a CDI event + @Observes as Observer, and @ConfigMapping as externalized Strategy. @QuarkusTest suite runs in-JVM on JDK 25 with no container."
---

This is the book's one framework chapter: it shows how a real dependency-injection
container (Quarkus, via Arc/CDI) provides, as built-in features, the patterns the
earlier chapters built by hand in plain Java. The forces are the same; the container
answers them. The module demonstrates `@ApplicationScoped` beans with constructor
`@Inject` (Singleton and dependency injection), a `@Produces` factory, an
`@AroundInvoke` interceptor with a binding annotation (Decorator/Proxy), a CDI event
with `@Observes` (Observer), and `@ConfigMapping` typed config (externalized
Strategy selection).

Pinned to Quarkus 3.40.1.1 LTS. It uses only `quarkus-arc`, so the `@QuarkusTest`
suite boots the CDI container in-JVM and runs on JDK 25 with no Docker and no Dev
Services.

## Build, run, observe

```sh
cd examples/28-quarkus-cdi
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
