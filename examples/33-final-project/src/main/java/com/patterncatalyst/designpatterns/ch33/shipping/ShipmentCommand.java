package com.patterncatalyst.designpatterns.ch33.shipping;

/**
 * The Command contract every reversible operation in this final project
 * shares. Declaring two abstract methods, rather than one, rules out a
 * lambda satisfying it directly, which forces every implementation to
 * supply both the action and its inverse together.
 */
public interface ShipmentCommand {

    void execute();

    void undo();
}
