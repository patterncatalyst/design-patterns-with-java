---
layout: example
order: 16
title: "Decorator and Proxy: wrapping a rate quoter"
description: Stacking behavior with decorators and controlling access with a hand-written and a dynamic proxy.
github_path: examples/16-decorator-proxy
card_eyebrow: "Chapter 16 · Decorator and Proxy"
card_title: "Wrapping a rate quoter"
card_blurb: "Logging, caching, and surcharge decorators stacked over a RateQuoter (named classes and functional equivalents), a hand-written lazy proxy that defers construction, and a java.lang.reflect.Proxy that gates access by clearance. Tests prove stacking order matters and the dynamic proxy intercepts every call."
---

Two patterns that share an interface with the thing they wrap. **Decorator** adds
behavior at runtime by stacking layers (logging, caching, surcharge) over a
`RateQuoter`, shown as named classes and as functional equivalents. **Proxy**
stands in front of the real subject to control access: a hand-written proxy that
defers construction until first use, and a dynamic `java.lang.reflect.Proxy` with
an `InvocationHandler` that gates calls by clearance level.

The JUnit 5 tests prove decorator stacking order changes the result, the lazy
proxy defers construction, and the dynamic proxy forwards or denies by clearance
and counts every intercepted call.

## Build, run, observe

```sh
cd examples/16-decorator-proxy
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
