---
layout: example
order: 11
title: "Command, undo, and command queues: a warehouse desk"
description: Warehouse operations captured as commands, with an undo-redo history and a FIFO queue.
github_path: examples/11-command
card_eyebrow: "Chapter 11 · Command"
card_title: "A warehouse operations desk"
card_blurb: "Warehouse operations captured as command objects with execute and undo: immutable records whose undo is an arithmetic inverse, mutable commands that snapshot prior state at execute time, an undo-redo history on two deques, and a FIFO command queue."
---

Command turns a request into an object so it can be queued, logged, and undone.
This module models a warehouse operations desk: allocate and transfer stock as
immutable `record` commands whose undo is a construction-time inverse, and
recount and cancel as commands that snapshot prior state at execute time. An
undo-redo history runs on two deques (a new command clears the redo branch), and a
FIFO queue processes pending operations in order.

The JUnit 5 tests prove a command's undo restores prior state, a sequence undoes
in reverse order, redo replays correctly, and the queue processes in order.

## Build, run, observe

```sh
cd examples/11-command
mvn -q test
./demo.sh
```

The module also runs from the aggregator root with
`mvn -q -f examples/pom.xml test`.
