---
layout: example
order: 25
title: "Resource and concurrency patterns: pools, monitors, active objects"
description: A bounded connection pool, a monitor object, and an active object, reconsidered under virtual threads.
github_path: examples/25-resource-concurrency
card_eyebrow: "Chapter 25 · Resource and concurrency patterns"
card_title: "Pools, monitors, active objects"
card_blurb: "A bounded CarrierConnectionPool with a blocking, timed acquire and AutoCloseable leases, a DispatchDock monitor serializing access via ReentrantLock + Condition, a LabelPrintActiveObject decoupling a call from its worker thread, and the ThreadLocal cost. Deterministic concurrency tests, no sleeps."
---

Real-world concurrency patterns for scarce resources. This module builds a bounded
`CarrierConnectionPool` (a `Semaphore` plus a free list, a blocking timed
`acquire`, `AutoCloseable` leases for try-with-resources), a `DispatchDock` monitor
object that serializes concurrent access with `ReentrantLock` and a `Condition`, and
a `LabelPrintActiveObject` that decouples a method call from the worker thread that
runs it. It also shows the `ThreadLocal` cost and defensive immutable snapshots. The
point under virtual threads: pool the scarce resource, not the threads.

The JUnit 5 tests are deterministic (latches and futures, no sleeps): the pool never
exceeds its bound and reuses instances, the monitor holds its invariant under
concurrent callers, and the active object processes queued calls off the caller
thread.

## Build, run, observe

```sh
cd examples/25-resource-concurrency
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
