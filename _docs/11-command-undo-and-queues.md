---
title: "Command, undo, and command queues"
order: 11
layout: tutorial
sectionid: docs
part: "selecting-varying-behavior"
description: "Turning a warehouse desk's operations into Command objects with their own execute and undo, an undo/redo stack built on a Deque, a FIFO command queue for deferred batch processing, and a functional-interface shortcut for a queued action with nothing to undo."
duration: "30 min"
---

## Learning objectives

By the end of this chapter you can:

- Encapsulate a request as a `WarehouseCommand` with an `execute()` and an `undo()`.
- Write undo as an arithmetic inverse in a record, or as a snapshot captured at execute time in a mutable class.
- Build an undo/redo stack on two `Deque`s, and say why a new command clears the redo branch.
- Build a FIFO command queue that defers a batch and can keep it undoable.
- Use a one-method functional interface for a queued action with nothing to undo, and say what it costs.
- Recognize when a direct method call is enough.

## Why this matters

A warehouse operations desk runs the same few actions all day: receive stock into a bin, move stock between bins, correct a quantity after a cycle count, cancel a picked order line. The first version calls the ledger directly from a keyboard handler, an HTTP route or an import script, and the call is gone when it returns. That breaks once the desk needs to undo the last action, queue actions for a nightly batch, or let a handheld scanner, a UI button and a scheduled job all submit the same kind of action.

{% include excalidraw.html name="11-command-undo-and-queue" caption="Figure 11.1: A trigger submits a request as a WarehouseCommand; it either enqueues into a FIFO CommandQueue that later drains into the ledger, or runs through a CommandHistory that moves it between a done stack and an undone stack." %}

## Pattern: Command

### The problem

`WarehouseLedger` tracks on-hand quantity per bin and SKU, and the order lines picked against that stock. It exposes primitives and knows nothing about undo or queues:

```java
public final class WarehouseLedger {

    public void adjustStock(String bin, String sku, int delta) { /* ... */ }
    public int quantityOf(String bin, String sku) { /* ... */ }

    public void addOrderLine(String lineId, OrderLine line) { /* ... */ }
    public OrderLine orderLine(String lineId) { /* ... */ }
    public void setLineState(String lineId, int quantity, boolean cancelled) { /* ... */ }
}
```

A call to `adjustStock` has no existence after it returns, so nothing can put it on a stack to reverse or in a queue to run later.

### The solution

A **Command** turns a request into an object that holds everything needed to perform the action and to reverse it.

`adjustStock()` takes a signed delta rather than separate receive and remove methods, so most undos are the same call with the sign flipped.

| Command | Undo strategy | Java shape |
| --- | --- | --- |
| `AllocateStockCommand` | Negate the quantity | Record |
| `TransferStockCommand` | Move the quantity back | Record |
| `RecountStockCommand` | Restore the quantity read at execute time | Mutable class |
| `CancelOrderLineCommand` | Restore the `OrderLine` read at execute time | Mutable class |

### How Java models it

Every command implements the same interface:

```java
public interface WarehouseCommand {
    void execute();
    void undo();
}
```

Two abstract methods mean a lambda can't satisfy it, so every implementation supplies the action and its inverse together.

### Undo as an inverse: records

`AllocateStockCommand` receives a quantity into a bin. Its undo is known in full at construction:

```java
public record AllocateStockCommand(WarehouseLedger ledger, String bin, String sku, int quantity)
        implements WarehouseCommand {

    @Override
    public void execute() {
        ledger.adjustStock(bin, sku, quantity);
    }

    @Override
    public void undo() {
        ledger.adjustStock(bin, sku, -quantity);
    }
}
```

Nothing changes after construction, so a record fits: its components are everything both methods need. `TransferStockCommand` has the same shape with two bins: `execute()` subtracts from the source and adds to the destination, and `undo()` reverses both.

### Undo as a snapshot: mutable classes

"Set this bin to 12 after a cycle count" has no number to add back. The undo must remember the prior quantity, and `RecountStockCommand` reads it in `execute()`, not the constructor:

```java
public final class RecountStockCommand implements WarehouseCommand {
    private final WarehouseLedger ledger;
    private final String bin;
    private final String sku;
    private final int countedQuantity;
    private int priorQuantity;

    public RecountStockCommand(WarehouseLedger ledger, String bin, String sku, int countedQuantity) {
        this.ledger = ledger;
        this.bin = bin;
        this.sku = sku;
        this.countedQuantity = countedQuantity;
    }

    @Override
    public void execute() {
        priorQuantity = ledger.quantityOf(bin, sku);
        ledger.adjustStock(bin, sku, countedQuantity - priorQuantity);
    }

    @Override
    public void undo() {
        ledger.adjustStock(bin, sku, priorQuantity - countedQuantity);
    }
}
```

A record's components are final, so `priorQuantity` needs a class. Reading it at execute time matters once commands queue: a recount built now and run an hour later, after other commands touched the bin, must snapshot the quantity when it runs. Read in the constructor, the snapshot is stale and undo restores the wrong number.

`CancelOrderLineCommand` snapshots a whole `OrderLine`. Cancelling returns the line's quantity to its bin and marks it cancelled; undo reverses both:

```java
@Override
public void execute() {
    priorState = ledger.orderLine(lineId);
    ledger.adjustStock(priorState.bin(), priorState.sku(), priorState.quantity());
    ledger.setLineState(lineId, 0, true);
}

@Override
public void undo() {
    ledger.adjustStock(priorState.bin(), priorState.sku(), -priorState.quantity());
    ledger.setLineState(lineId, priorState.quantity(), priorState.cancelled());
}
```

Restoring the whole line, `cancelled` flag included, returns it exactly as it was. The invoker never knows whether a command is a record or a class; both hide behind `execute()` and `undo()`.

### The undo/redo stack

`CommandHistory` owns two `Deque`s, done and undone:

```java
public final class CommandHistory {
    private final Deque<WarehouseCommand> done = new ArrayDeque<>();
    private final Deque<WarehouseCommand> undone = new ArrayDeque<>();

    public void execute(WarehouseCommand command) {
        command.execute();
        done.push(command);
        undone.clear();
    }

    public boolean undo() {
        WarehouseCommand command = done.poll();
        if (command == null) {
            return false;
        }
        command.undo();
        undone.push(command);
        return true;
    }

    public boolean redo() {
        WarehouseCommand command = undone.poll();
        if (command == null) {
            return false;
        }
        command.execute();
        done.push(command);
        return true;
    }
}
```

`Deque.poll()` returns `null` on an empty deque instead of throwing, so `undo()` and `redo()` report "nothing left" as `false`.

A new command clears the undone stack, the rule every text editor follows. Redoing an old branch after a new action would jump to a state that contradicts what the operator just did. Both stacks only touch their most recent end, so undo walks back in exact reverse order and redo walks forward along the same branch; jumping to an arbitrary earlier command would leave the ledger in a state no real sequence of actions produces.

{% include excalidraw.html name="11-undo-redo-stack-mechanics" caption="Figure 11.2: three snapshots of CommandHistory's two Deques. Running C1 through C3 fills the done stack; undo() pops C3 off done and pushes it onto undone; executing a new command, C4, clears undone entirely rather than leaving C3 reachable by a later redo()." %}

### The command queue

`CommandQueue` defers commands until a batch step runs them:

```java
public final class CommandQueue {
    private final Deque<WarehouseCommand> pending = new ArrayDeque<>();

    public void enqueue(WarehouseCommand command) {
        pending.addLast(command);
    }

    public int processAll(CommandHistory history) {
        int processed = 0;
        WarehouseCommand command;
        while ((command = pending.pollFirst()) != null) {
            if (history != null) {
                history.execute(command);
            } else {
                command.execute();
            }
            processed++;
        }
        return processed;
    }
}
```

`enqueue()` only records order; `processAll()` drains oldest first. Order matters as soon as a batch mixes relative and absolute changes. Receive 25 into `B2`, transfer 10 out, then recount `B2` to 18: FIFO leaves 18 in `B2`. Run the same three last-in first-out and `B2` ends at 33, because the recount runs first and the receipt lands on top of it. FIFO lets several triggers queue into one `CommandQueue` through a shift and have the batch reproduce what happened on the floor.

Pass a `CommandHistory` to `processAll()` and every command runs through `history.execute()`, so the batch stays undoable one command at a time. Pass `null` and the batch is fire-and-forget.

### The functional-interface shortcut

Logging a restock has nothing to undo. A two-method command would need an `undo()` with no real body:

{% include codetabs.html langs="Full command|Plain lambda" %}

```java
// Full command: a dedicated class, a constructor, three fields, and an
// undo() with no real implementation to give. It throws rather than
// silently doing nothing, so a caller that mistakenly tries to undo it
// fails loudly instead of leaving a stale receipt line behind.
public final class LogRestockCommand implements WarehouseCommand {
    private final List<String> receiptLines;
    private final int quantity;
    private final String bin;

    public LogRestockCommand(List<String> receiptLines, int quantity, String bin) {
        this.receiptLines = receiptLines;
        this.quantity = quantity;
        this.bin = bin;
    }

    @Override
    public void execute() {
        receiptLines.add("+%d into %s".formatted(quantity, bin));
    }

    @Override
    public void undo() {
        throw new UnsupportedOperationException("a restock log entry has no undo");
    }
}
```
```java
// Plain lambda: the identical action, satisfied by a one-method functional
// interface. No class, no constructor, no undo() forced into existence
// with nothing real to put in it.
@FunctionalInterface
public interface ReceiptLogger {
    void log(int quantity, String bin);
}

public static ReceiptLogger appendingTo(List<String> receiptLines) {
    return (quantity, bin) -> receiptLines.add("+%d into %s".formatted(quantity, bin));
}
```

Both append the same line. The cost of the shortcut: a `ReceiptLogger` can't go on a `CommandHistory`, because it has no `undo()`. That's right for an action with no reverse and wrong once a queued action must be undoable.

## When not to use it

Command pays when a request must outlive the call that made it: to be undone, to be queued or batched, or to be submitted by several triggers.

| Situation | Write instead |
| --- | --- |
| One trigger, never undone, always immediate | Call `ledger.adjustStock(bin, sku, delta)` directly. |
| An action with no undo and no batching, such as a log line, a notification or a webhook | A functional interface like `ReceiptLogger`. |
| Actions run one at a time, synchronously, with no deferral | No `CommandQueue` or `CommandHistory`; add them when deferral or batching is required. |

> **🤖 Explore with your agent**
>
> "In `examples/11-command`, add a new warehouse action as a `WarehouseCommand` against `WarehouseLedger`. Use a record when the undo is the inverse call known at construction, as in `AllocateStockCommand`, and a class that captures prior state inside `execute()`, never the constructor, as `RecountStockCommand` does. Run interactive actions through `CommandHistory`, which clears the redo stack on every new command, and batch actions through `CommandQueue.processAll(history)` in FIFO order so the batch stays undoable. Don't model undo as a stack of `Runnable` closures. Use `ReceiptLogger` only for an action with nothing to undo."
>
> Check: `mvn -q -f examples/11-command/pom.xml test -Dtest=CommandHistoryTest,RecountStockCommandTest,CommandQueueTest`; a stale snapshot or a kept redo branch fails it.

## Build, run, observe

From the repository root, or standalone from `examples/11-command`:

```sh
mvn -f examples/pom.xml test
```

```sh
cd examples/11-command
mvn test
```

All 31 tests pass. To run the commands, the stack, the queue and the shortcut end to end:

```sh
examples/11-command/demo.sh
```

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

The batch receives 25 `GASKET-7` into `B2`, transfers 10 to `PICK-FACE-1` and recounts `B2` to 18. Three experiments, each in a scratch copy:

- **Keep the redo branch.** Delete `undone.clear()` from `CommandHistory.execute()`. The check that a new command clears the redo branch fails.
- **Snapshot too early.** Move the `priorQuantity` read in `RecountStockCommand` from `execute()` to the constructor. The execute-time snapshot check fails.
- **Make the queue LIFO.** Change `pending.addLast` to `pending.addFirst` in `CommandQueue.enqueue()`. The undoable-batch check fails, and the demo's batch leaves 33 in `B2` instead of 18.

## What you can do now

- Wrap an action and its inverse in one `WarehouseCommand`.
- Use a record for an inverse known at construction and a class for a snapshot taken at execute time.
- Build undo and redo on two `Deque`s, clearing redo on every new command.
- Defer a batch through a FIFO `CommandQueue`, recorded in a `CommandHistory` when it must stay undoable.
- Use a functional interface, or a plain call, when there's nothing to undo or queue.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original Command entry, including the undo-support discussion `CommandHistory` implements.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 17 on minimizing mutability is the case for `AllocateStockCommand` and `TransferStockCommand` as records, and the case against forcing `RecountStockCommand` and `CancelOrderLineCommand` into that shape once they capture state at execute time.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/11-command`.
- The full reading list lives on the [bibliography page]({{ '/bibliography/' | relative_url }}).
