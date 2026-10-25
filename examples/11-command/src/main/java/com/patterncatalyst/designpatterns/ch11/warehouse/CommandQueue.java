package com.patterncatalyst.designpatterns.ch11.warehouse;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * A FIFO queue of deferred warehouse operations. {@link CommandHistory} runs
 * a command the instant {@code execute()} is called; this class is for the
 * opposite case, operations submitted over time (a handheld scanner, a
 * manual correction, a scheduled replenishment job) that should not run
 * until a batch step decides to process them, in exactly the order they
 * were submitted.
 */
public final class CommandQueue {

    private final Deque<WarehouseCommand> pending = new ArrayDeque<>();

    public void enqueue(WarehouseCommand command) {
        pending.addLast(Objects.requireNonNull(command, "command"));
    }

    public int pendingCount() {
        return pending.size();
    }

    /**
     * Drains the queue, oldest command first, calling {@code execute()}
     * directly. The batch is fire-and-forget: nothing about it can be
     * stepped back through afterward.
     *
     * @return the number of commands processed
     */
    public int processAll() {
        return processAll(null);
    }

    /**
     * Drains the queue, oldest command first. When a {@link CommandHistory}
     * is supplied, each command runs through {@code history.execute()}
     * instead of a direct call, so the whole batch remains individually
     * undoable afterward, one command at a time, the same as if an operator
     * had run each one by hand.
     *
     * @return the number of commands processed
     */
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
