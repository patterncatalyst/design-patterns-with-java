package com.patterncatalyst.designpatterns.ch25.dispatch;

/**
 * Proof that a {@link LabelPrintActiveObject} request ran: which parcel, its
 * position in the submission order, and the name of the worker thread that
 * actually executed it, never the thread that called {@code submitPrint()}.
 */
public record PrintReceipt(String parcelId, long sequence, String printedByThread) {
}
