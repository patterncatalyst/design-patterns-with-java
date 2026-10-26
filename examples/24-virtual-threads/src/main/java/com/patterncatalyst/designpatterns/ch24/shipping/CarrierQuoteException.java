package com.patterncatalyst.designpatterns.ch24.shipping;

/**
 * Wraps a failure surfaced from a carrier call made during a fan-out, so a
 * caller deals with one unchecked failure type regardless of which carrier
 * failed or why.
 */
public final class CarrierQuoteException extends RuntimeException {

    public CarrierQuoteException(String message, Throwable cause) {
        super(message, cause);
    }
}
