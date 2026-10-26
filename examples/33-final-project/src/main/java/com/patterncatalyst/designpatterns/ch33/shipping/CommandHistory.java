package com.patterncatalyst.designpatterns.ch33.shipping;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * An undo/redo stack over two {@link Deque}s. Running a new command clears
 * the redo branch, the same rule every mainstream undo implementation
 * follows: once a new action happens, whatever had been undone away from is
 * no longer reachable, because redoing it now would overwrite the new
 * action already sitting in its place.
 */
public final class CommandHistory {

    private final Deque<ShipmentCommand> done = new ArrayDeque<>();
    private final Deque<ShipmentCommand> undone = new ArrayDeque<>();

    public void execute(ShipmentCommand command) {
        Objects.requireNonNull(command, "command").execute();
        done.push(command);
        undone.clear();
    }

    public boolean undo() {
        ShipmentCommand command = done.poll();
        if (command == null) {
            return false;
        }
        command.undo();
        undone.push(command);
        return true;
    }

    public boolean redo() {
        ShipmentCommand command = undone.poll();
        if (command == null) {
            return false;
        }
        command.execute();
        done.push(command);
        return true;
    }
}
