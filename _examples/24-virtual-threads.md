---
layout: example
order: 24
title: "Virtual threads: concurrent carrier fan-out"
description: Fanning a rate quote out across many carriers on virtual threads, bounded and unbounded, with the pinning and pooling caveats.
github_path: examples/24-virtual-threads
card_eyebrow: "Chapter 24 · Virtual threads"
card_title: "Concurrent carrier fan-out"
card_blurb: "A rate quote fanned out across carriers on virtual threads with newVirtualThreadPerTaskExecutor, an unbounded fan-out and a Semaphore-bounded one, and a ReentrantLock-guarded cache. Tests prove genuine concurrency with CyclicBarrier rendezvous, and the chapter covers what virtual threads are good for and not."
---

Virtual threads make high-concurrency blocking I/O cheap. This module fans a rate
quote out across many carriers, each a blocking call, on a
`newVirtualThreadPerTaskExecutor()` (one virtual thread per task), with both an
unbounded fan-out and a `Semaphore`-bounded one that caps concurrent calls while
keeping one virtual thread per task. The chapter covers what virtual threads are
good for (blocking I/O fan-out) and what they are not (CPU-bound work, pooling them,
`ThreadLocal` at scale), accurately for Java 25 (synchronized no longer pins the
carrier; the remaining pinning causes are named).

The JUnit 5 tests prove genuine concurrency with `CyclicBarrier` rendezvous (not
timing), and that the bounded form never exceeds its cap.

## Build, run, observe

```sh
cd examples/24-virtual-threads
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
