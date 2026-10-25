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

- Encapsulate a request as a `WarehouseCommand` object with an `execute()`
  and an `undo()`, rather than calling the receiver's method directly from
  the code that decides an action should happen.
- Build a command's undo two ways: a clean arithmetic inverse expressed as
  an immutable record, and a captured prior-state snapshot expressed as a
  mutable class, and say which Java shape fits which kind of undo.
- Build an undo/redo stack on two `Deque`s that moves a command between a
  done stack and an undone stack, and explain why running a new command
  clears the redo branch.
- Build a FIFO command queue over a `Deque` that defers execution and
  processes a batch of commands in submission order, optionally recording
  each one into an undo/redo stack as it runs.
- Reach for a one-method functional interface instead of a full Command
  object when a queued action has nothing to undo, and say what that
  shortcut costs.
- Recognize when Command is solving a problem nobody has yet, and say what
  to write instead.

## Why this matters

A warehouse operations desk runs the same handful of actions over and over:
receive stock into a bin, move stock between bins, correct a quantity after
a cycle count, cancel a picked order line. The first version calls a method
on the warehouse ledger directly from wherever the action is triggered, a
keyboard shortcut handler, an HTTP route, a batch-import script, and the
call is gone the instant it returns. That works until the desk needs any of
three things an inline method call cannot give it: a way to undo the last
thing that happened, a way to queue several actions and run them later as a
batch, or a way to let several different triggers (a handheld scanner, a UI
button, a scheduled job) submit the same kind of action without each one
re-deriving how to perform it.

Command is the Gang of Four's answer to that force: turn a request into an
object instead of a method call. The object remembers everything it needs
to run the action and, critically, everything it needs to run the action's
inverse. Once a request is an object rather than a call already in flight,
it can sit in a queue waiting to run, or sit on a stack waiting to be
reversed. Neither is possible with a bare method call, because a method
call has no existence after it returns; a command object does, for as long
as something is holding onto it.

This chapter builds that object against a warehouse operations desk. Four
actions, receiving stock, moving stock, correcting a quantity, and
cancelling an order line, become four `WarehouseCommand` implementations
sharing one receiver, `WarehouseLedger`. An undo/redo stack,
`CommandHistory`, lets an operator step backward and forward through a
shift the way a text editor's Ctrl-Z does. A `CommandQueue` defers a batch
of actions, submitted throughout the day, and runs them in submission order
when a nightly batch step processes them. Not every queued action needs the
full ceremony of a command object with an `execute()` and an `undo()`, so
the chapter closes by showing the idiomatic Java shortcut, a plain
functional interface, for a queued action that has nothing to undo, and
what that shortcut costs compared to the full object.

{% include excalidraw.html name="11-command-undo-and-queue" caption="Figure 11.1: A trigger submits a request as a WarehouseCommand; it either enqueues into a FIFO CommandQueue that later drains into the ledger, or runs through a CommandHistory that moves it between a done stack and an undone stack." %}

## The domain: a warehouse operations desk

The example behind this chapter lives in `examples/11-command`.
`WarehouseLedger` is the receiver every command in this chapter mutates: it
tracks on-hand quantity per bin/SKU pair and the order lines already picked
against that stock. It exposes exactly the primitives a command needs and
nothing about commands, undo, or queues:

```java
public final class WarehouseLedger {

    public void adjustStock(String bin, String sku, int delta) { /* ... */ }
    public int quantityOf(String bin, String sku) { /* ... */ }

    public void addOrderLine(String lineId, OrderLine line) { /* ... */ }
    public OrderLine orderLine(String lineId) { /* ... */ }
    public void setLineState(String lineId, int quantity, boolean cancelled) { /* ... */ }
}
```

`adjustStock()` takes a signed delta rather than separate receive/remove
methods. That one choice is what lets every command below express its undo
as the identical call with the sign flipped, instead of writing a second,
separately tested code path for reversing each kind of change. Keeping the
receiver this small mirrors a separation this book has used before: the
receiver knows how to change warehouse state, and the commands below know
when and why to call it, with nothing about undo or queueing leaking into
the receiver itself.

## The command contract

Every command in this example implements the same two-method interface:

```java
public interface WarehouseCommand {
    void execute();
    void undo();
}
```

`WarehouseCommand` declares two abstract methods, which rules out a lambda
satisfying it directly the way a single-method interface can, which forces
every implementation to supply both halves of the contract, the action and
its inverse, together, rather than letting an author forget the second
one.

## Undo as a clean inverse: records

`AllocateStockCommand` receives a quantity of a SKU into a bin. Its undo is
the identical call with the quantity negated, a fact known in full the
moment the command is constructed:

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

A `record` is the right shape here because nothing about this command
changes after construction. Its four components, the ledger and the three
values describing the receipt, are everything both `execute()` and
`undo()` ever need, and a record's generated constructor, accessors,
`equals()`, and `toString()` are exactly what a value this small should
carry. `TransferStockCommand` follows the identical shape with two bins
instead of one: `execute()` subtracts the quantity from the source bin and
adds it to the destination; `undo()` runs both halves in reverse. Both of
these commands get a clean, symmetric inverse at no extra design cost,
because the action they perform is itself symmetric: moving a quantity from
one bin to another undoes by moving the same quantity back.

## Undo as a captured snapshot: mutable classes

Not every action has an arithmetic inverse. "Set this bin's quantity to 12
after a cycle count" has no opposite number to add back; the only way to
undo it is to remember what the quantity was before it ran.
`RecountStockCommand` captures that prior value inside `execute()`, not the
constructor:

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

This is the point a record cannot reach. `priorQuantity` is a field that
changes after construction, set the moment `execute()` runs, and a record's
components are final by design. The mutable field is not a workaround; it
is what the command's actual lifecycle requires. Capturing
`priorQuantity` at execute time rather than construction time matters once
a command can be queued. A command built now and run an hour later, after a
`CommandQueue` has drained several other commands ahead of it, must read
the quantity the moment it runs, not the moment somebody wrote `new
RecountStockCommand(...)`. Capturing state in the constructor would record
a stale value if anything else touched that bin in between; capturing it
in `execute()` is correct no matter how long the command waited or what
ran before it.

`CancelOrderLineCommand` is the same idea against a richer piece of state,
a whole `OrderLine` rather than a single integer. Cancelling a picked line
has to return its reserved quantity to the bin it came from and mark the
line cancelled; undoing that cancellation has to reverse both parts at
once:

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

Capturing the whole `OrderLine` rather than just the quantity means
`undo()` restores the line exactly as it was, including a `cancelled` flag
that was already `true` if this line had somehow been cancelled before.
All four commands share the same `WarehouseCommand` interface despite two
different Java shapes behind it, a record for a construction-time inverse
and a class for an execute-time snapshot, and the invoker never has to know
or care which shape a given command used. That is the entire point of
hiding both behind `execute()`/`undo()`.

## The undo/redo stack

`CommandHistory` owns two `Deque`s, done and undone, and is the piece that
makes undo and redo available to a caller:

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

`execute()` runs the command immediately and pushes it onto the done stack,
clearing the undone stack first. `undo()` pops the most recently done
command, calls its `undo()`, and moves it onto the undone stack. `redo()`
does the mirror image: pop the most recently undone command, call its
`execute()` again, and move it back onto the done stack. `Deque.poll()`
returns `null` for an empty deque instead of throwing, which is what lets
`undo()` and `redo()` report "nothing left" with a boolean rather than a
caught exception.

Clearing the undone stack inside `execute()` is the rule every text
editor's undo history already follows: once a new action happens, whatever
had been undone away from is no longer reachable by `redo()`, because
redoing it now would overwrite the new action already sitting in its
place. A history that kept the old redo branch around after a new command
ran would let `redo()` jump to a state that contradicts whatever the
operator just did, which is worse than discarding that branch.

The stack shape also explains why this is a `Deque` used as a stack, not a
list. Both operations only ever touch the most recent end. `undo()` can
only step back one command at a time, in the exact reverse of the order
those commands ran, and `redo()` can only step forward through the branch
that undo just walked back from. That is the same discipline every
mainstream undo implementation uses, from a text editor to a drawing
program, because it is the only ordering that keeps every intermediate
state consistent: skipping around to an arbitrary earlier command without
undoing everything after it first would leave the ledger in a state no
sequence of real actions would ever produce.

## The command queue: deferred, batched execution

`CommandHistory` runs a command the instant `execute()` is called.
`CommandQueue` is for the opposite case: actions submitted over time that
should not run until a batch step decides to process them:

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

`enqueue()` only records submission order; nothing runs until
`processAll()` drains the queue, FIFO, oldest command first. That ordering
guarantee is not cosmetic. If a shift queues an `AllocateStockCommand` for
five units ahead of a `TransferStockCommand` that moves ten units out of
the same bin, running them out of order would make the transfer produce a
different final quantity than the warehouse floor intended. FIFO
processing is what lets several different triggers, a handheld scanner, a
manual correction, a scheduled replenishment job, all queue into the same
`CommandQueue` over the course of a shift and trust that a nightly batch
step reproduces exactly the sequence of events that happened on the floor.

The optional `CommandHistory` parameter is what connects the queue back to
undo. Pass one in and `processAll()` runs every command through
`history.execute()` instead of calling `execute()` directly, so the entire
batch remains individually undoable afterward, one command at a time, the
same as if an operator had run each one by hand. Pass `null` and the batch
is fire-and-forget, appropriate for a queue of actions nobody will ever
want to step back through.

## The functional-interface shortcut and its trade-off

Logging that a restock happened has no undo at all; there is nothing to
reverse about having written a line to a receipt. Wrapping that in a class
implementing `execute()` and `undo()` would be ceremony with no second
method to justify it: when an action has exactly one real operation, that
operation is a function, not a two-method interface.
`ReceiptLogger` is a plain functional interface instead:

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

Both columns append the identical line to the identical kind of list, which
the example's test suite checks directly rather than assuming. The
trade-off this shortcut makes is explicit: a `ReceiptLogger` cannot be
pushed onto a `CommandHistory` and cannot be stepped backward through,
because it was never given an `undo()` to call. That is the right trade for
an action with no meaningful reverse, and the wrong one the instant a
queued action needs to be undoable, which is exactly the line this chapter
draws between the four `WarehouseCommand` types above and this one
functional interface.

> **AI sidebar.** Ask an LLM pairing partner for "an undo system" in Java
> and a common result reaches for a single global stack of
> `Runnable`-shaped closures, each one a lambda that mutates state
> directly, because that shape is quick to produce and compiles on the
> first try. Check it the way this chapter trained: a closure-based undo
> stack works until two different kinds of undo logic need two different
> kinds of captured state, at which point every closure needs its own ad
> hoc capture, and a reviewer can no longer tell by looking at the stack's
> declared type what any entry is capable of. An explicit `WarehouseCommand`
> interface costs one extra method call per action and buys a named,
> swappable, type-checked contract: a signature a reviewer can read, a
> queue that can hold commands from multiple unrelated call sites without
> those sites agreeing on a closure shape in advance, and a place,
> `undo()`, to ask directly "does this command's undo reverse what execute
> does," a question a bare `Runnable` has no slot to answer. A
> generated command slot that is just `Runnable` with a comment reading
> "caller tracks how to undo this separately" is the clearest sign the
> model reached for the closure shape instead of the object shape the
> forces call for.

## When not to use Command

Command earns its cost when a request needs to outlive the call that
created it: because it must be undoable, because it must be queued for
later or batched with others, or because more than one trigger submits the
same kind of request and none of them should have to re-derive how to
perform it. It stops paying for itself the moment none of those three is
true.

An action triggered from exactly one place, that nobody has ever asked to
undo, and that always runs immediately, does not need a `WarehouseCommand`
wrapped around it; it needs a method call. Wrapping
`ledger.adjustStock(bin, sku, delta)` in a command object before any of
undo, queueing, or multiple triggers is in hand or clearly coming adds an
allocation, an interface implementation, and a reader's detour through a
class definition to find a single call site that will ever use it. The
plainer non-pattern is the direct call: name the abstraction and the plain
alternative side by side, and let whether a second concern, undo, batching,
multiple submitters, is present decide between them.

The `ReceiptLogger` shortcut earlier in this chapter is itself a smaller
version of the same caution in the other direction. Reaching for a full
`WarehouseCommand` implementation for an action with no undo and no
batching requirement, logging an event, sending a notification, firing a
webhook, buys nothing a bare functional interface does not already
provide, at the cost of a class definition and a method, `undo()`, that has
no real body to put in it. The forces decide which of the two shapes an
action gets, not which one appeared most recently in this chapter.

A queue is also not automatically the right default for every action that
happens to run more than once. If a warehouse desk only ever runs one
action at a time, waits for it to finish, and never needs to defer or batch
anything, a `CommandQueue` and a `CommandHistory` are both unnecessary
machinery around a call that was always going to run synchronously anyway.
Reach for the queue when deferral or batching is an actual requirement, not
because a `WarehouseCommand` happens to be sitting right there once an
action has been turned into one.

## Build, run, observe

Every command below runs with the local Maven and JDK 25 toolchain the
prerequisites chapter installs; there is no container anywhere in this
loop. From the repository root, the aggregator picks up
`examples/11-command` automatically:

```sh
mvn -f examples/pom.xml test
```

This example is also standalone-buildable from its own directory:

```sh
cd examples/11-command
mvn test
```

Either path runs the same 31 JUnit 5 cases, asserted with AssertJ,
including a case that executes a command and asserts its undo restores
prior state, a case that undoes a three-command sequence in reverse order,
and a case that proves the queue only produces a correct final state when
it processes commands in submission order. Observed locally on Fedora with
Temurin 25.0.2:

```
Tests run: 31, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

To see the commands, the undo/redo stack, the queue, and the functional
shortcut run end to end rather than just reading assertions that say they
work, run the example's own demo script:

```sh
examples/11-command/demo.sh
```

It receives 50 units of `WIDGET-100` into bin `A1`, moves 20 of them to a
pick face, undoes and redoes that transfer, corrects a quantity after a
cycle count, cancels and un-cancels an order line, then queues and
batch-processes three operations against a second SKU before logging two
restocks through the `ReceiptLogger` shortcut:

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

The queue's batch runs against `GASKET-7`, a receipt of 25 into `B2`, a
transfer of 10 to `PICK-FACE-1`, and a cycle-count correction to 18,
landing on the same final quantities every time because `processAll()`
always drains the queue in the order those three commands were submitted.

## For deeper coverage

- Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides, [*Design Patterns: Elements of Reusable Object-Oriented Software*](https://www.pearson.com/en-us/subject-catalog/p/design-patterns-elements-of-reusable-object-oriented-software/P200000009480) (Addison-Wesley): the original Command entry, including the undo-support discussion this chapter's `CommandHistory` directly implements.
- Joshua Bloch, [*Effective Java*](https://www.informit.com/store/effective-java-9780134685991), third edition (Addison-Wesley): Item 17 on minimizing mutability is the case for expressing `AllocateStockCommand` and `TransferStockCommand` as records, and the case against forcing `RecountStockCommand` and `CancelOrderLineCommand` into that same immutable shape once they need to capture state at execute time.
- The official [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/) and [AssertJ documentation](https://assertj.github.io/doc/): the testing idiom behind `examples/11-command`'s suite, which asserts that execute changes state, undo restores it, redo reapplies it, and a FIFO batch lands on the same final state regardless of how many commands ran before it.
- The full reading list behind this book's opinions lives on the [bibliography page]({{ '/bibliography/' | relative_url }}); each citation there is a pointer to the full book, not a substitute for reading it.
