# 11 - Command, undo, and command queues: warehouse operations

A warehouse operations desk runs the same handful of actions over and over:
receive stock into a bin, move stock between bins, correct a quantity after a
cycle count, cancel a picked order line. Calling a method on the ledger
directly from wherever the action is triggered works until three things show
up: a need to undo the last action, a need to defer and batch several actions,
or more than one trigger (a scanner, a UI button, a nightly batch job)
submitting the same kind of action. This example turns each action into a
`WarehouseCommand` object so all three become possible.

## The domain

- `OrderLine`: a `record` with `sku`, `bin`, `quantity`, and `cancelled`.
- `WarehouseLedger`: the receiver. Tracks on-hand quantity per bin/SKU pair
  and picked order lines, behind exactly two primitives (`adjustStock`,
  order-line read/write) with nothing about commands, undo, or queueing in
  it.
- `WarehouseCommand`: the Command contract, `execute()` and `undo()`.

## The four commands

1. **`AllocateStockCommand`** and **`TransferStockCommand`** (`record`s):
   their undo is a clean arithmetic inverse, known in full at construction
   time, so an immutable record holds everything both methods need.
2. **`RecountStockCommand`** and **`CancelOrderLineCommand`** (mutable
   classes): "set the quantity to 12" and "cancel this line" have no
   arithmetic inverse. Each captures its prior state (a quantity, or a whole
   `OrderLine`) inside `execute()`, not the constructor, so a command queued
   now and run later always reads state at the moment it actually runs.

## Undo/redo and the queue

- **`CommandHistory`**: two `Deque`-backed stacks, done and undone.
  `execute()` runs a command and pushes it; `undo()` pops the most recent
  command and moves it to the undone stack; `redo()` does the mirror image.
  Running a new command clears the undone stack.
- **`CommandQueue`**: a FIFO queue over a `Deque`. `enqueue()` only records
  submission order; `processAll()` drains it oldest-first, optionally
  routing each command through a `CommandHistory` so the whole batch stays
  individually undoable afterward.

## The functional shortcut

Logging a restock has nothing to undo. `ReceiptLogger` is a one-method
functional interface satisfied by a lambda (`RestockLog.appendingTo`);
`LogRestockCommand` builds the identical logging action as a full
`WarehouseCommand` instead, whose `undo()` can only throw, to show the cost
of the Command shape on an action that never needed it.

`WarehouseDemo.java` runs every command, the undo/redo stack, the queue, and
the shortcut end to end and prints the ledger state after each step.

## Build, run, observe

No containers: local Maven + JDK 25 only (see the prerequisites chapter and
`scripts/check-prereqs.sh` at the repo root).

This example is standalone-buildable from this directory:

```sh
cd examples/11-command
mvn test
```

It is also picked up by the repo root's aggregator:

```sh
mvn -f examples/pom.xml test
```

To see every command, the undo/redo stack, the queue, and the shortcut run
end to end rather than just reading assertions that say they work, run the
demo:

```sh
./demo.sh
```

Expected output:

```
== Command objects: execute changes state, undo restores it ==
A1/WIDGET-100 after AllocateStockCommand: 50
A1/WIDGET-100 after TransferStockCommand: 30
PICK-FACE-3/WIDGET-100 after TransferStockCommand: 20
A1/WIDGET-100 after undo: 50
PICK-FACE-3/WIDGET-100 after undo: 0
PICK-FACE-3/WIDGET-100 after redo: 20

== A cycle-count correction and a cancelled order line ==
PICK-FACE-3/WIDGET-100 after cycle-count correction: 12
PICK-FACE-3/WIDGET-100 after cancelling LINE-9001: 17
PICK-FACE-3/WIDGET-100 after undoing the cancellation: 12

== Command queue: deferred, batched, FIFO execution ==
Pending before processing: 3
Processed in submission order: 3
B2/GASKET-7 after the batch: 18
PICK-FACE-1/GASKET-7 after the batch: 10

== ReceiptLogger shortcut: a queued action with nothing to undo ==
GASKET-7: +30 into B2
GASKET-7: +5 into B2
```

## Files

```
examples/11-command/
  pom.xml                       # standalone + aggregator-integrated module
  demo.sh                       # compile + run WarehouseDemo
  src/main/java/com/patterncatalyst/designpatterns/ch11/warehouse/
    OrderLine.java                 # record: sku, bin, quantity, cancelled
    WarehouseLedger.java           # the receiver
    WarehouseCommand.java          # the Command contract: execute()/undo()
    AllocateStockCommand.java      # record: clean arithmetic inverse
    TransferStockCommand.java      # record: clean arithmetic inverse
    RecountStockCommand.java       # class: captures prior quantity at execute time
    CancelOrderLineCommand.java    # class: captures prior OrderLine at execute time
    CommandHistory.java            # undo/redo stack (two Deques)
    CommandQueue.java              # FIFO deferred batch queue (Deque)
    ReceiptLogger.java             # functional interface shortcut
    RestockLog.java                # the shortcut's lambda factory
    LogRestockCommand.java         # the same action as a full Command, for comparison
    WarehouseDemo.java             # exercises everything end to end
  src/test/java/com/patterncatalyst/designpatterns/ch11/warehouse/
    WarehouseLedgerTest.java          # receiver behavior
    AllocateStockCommandTest.java     # execute/undo on a single bin
    TransferStockCommandTest.java     # execute/undo across two bins
    RecountStockCommandTest.java      # execute-time vs construction-time capture
    CancelOrderLineCommandTest.java   # execute/undo, including a double cancel
    CommandHistoryTest.java           # LIFO undo/redo, redo-branch clearing
    CommandQueueTest.java             # FIFO processing, with and without history
    RestockLoggingTest.java           # lambda vs full-command equivalence
```
