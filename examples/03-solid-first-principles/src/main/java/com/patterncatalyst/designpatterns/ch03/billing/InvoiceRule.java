package com.patterncatalyst.designpatterns.ch03.billing;

/**
 * The Open/Closed extension point this chapter builds. {@link
 * InvoiceCalculator} depends on this interface and never on a concrete rule,
 * so a brand-new rule type satisfies it without a single line of {@code
 * InvoiceCalculator} changing. The interface is deliberately left open, not
 * sealed: a rule shipped by a different team, or defined only inside a test,
 * has to be able to implement it without touching this file's permits.
 *
 * <p>Every implementation is expected to honor one invariant this interface
 * cannot enforce by itself: it must never throw for a well-formed, non-null
 * subtotal. {@link InvoiceCalculator} applies every rule in sequence and has
 * no recovery path for a rule that refuses to answer.
 */
public interface InvoiceRule {

    /**
     * The amount this rule adds to (positive) or subtracts from (negative)
     * an invoice's subtotal. Every rule receives the invoice's original
     * subtotal, never a running total updated by earlier rules, so a rule's
     * adjustment never depends on what order the rules ran in.
     */
    long adjustmentCents(long subtotalCents);

    /** A short label identifying this rule on a receipt or an audit line. */
    String name();
}
