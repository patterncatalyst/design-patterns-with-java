package com.patterncatalyst.designpatterns.ch11.warehouse;

/**
 * A warehouse operation captured as an object instead of a direct method
 * call on {@link WarehouseLedger}. Every command in this example implements
 * exactly these two methods, which is what lets a {@link CommandHistory} and
 * a {@link CommandQueue} treat every one of them identically: run it, and
 * know how to run its inverse.
 */
public interface WarehouseCommand {

    void execute();

    void undo();
}
