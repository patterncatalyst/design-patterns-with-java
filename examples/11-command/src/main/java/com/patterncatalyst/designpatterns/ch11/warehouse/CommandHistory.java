package com.patterncatalyst.designpatterns.ch11.warehouse;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * An undo/redo stack built on two {@link Deque}s: commands already done, and
 * commands undone away from. {@link #execute(WarehouseCommand)} runs a
 * command and pushes it onto the done stack, clearing the undone stack
 * first. {@link #undo()} pops the most recently done command and moves it
 * onto the undone stack. {@link #redo()} does the mirror image. Both
 * operations only ever touch the most recent end of their stack, which is
 * why this class works a command's lifetime like a text editor's Ctrl-Z
 * history rather than a list an operator can jump around in arbitrarily.
 */
public final class CommandHistory {

    private final Deque<WarehouseCommand> done = new ArrayDeque<>();
    private final Deque<WarehouseCommand> undone = new ArrayDeque<>();

    public void execute(WarehouseCommand command) {
        Objects.requireNonNull(command, "command");
        command.execute();
        done.push(command);
        undone.clear();
    }

    /**
     * Undoes the most recently executed command. Returns {@code false} with
     * no effect if there is nothing left to undo.
     */
    public boolean undo() {
        WarehouseCommand command = done.poll();
        if (command == null) {
            return false;
        }
        command.undo();
        undone.push(command);
        return true;
    }

    /**
     * Re-executes the most recently undone command. Returns {@code false}
     * with no effect if there is nothing left to redo.
     */
    public boolean redo() {
        WarehouseCommand command = undone.poll();
        if (command == null) {
            return false;
        }
        command.execute();
        done.push(command);
        return true;
    }

    public boolean canUndo() {
        return !done.isEmpty();
    }

    public boolean canRedo() {
        return !undone.isEmpty();
    }
}
