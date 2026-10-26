package com.patterncatalyst.designpatterns.ch34.returns;

/**
 * The pattern contract an AI pairing partner was asked to draft an
 * implementation of: a return request's outcome varies by nothing except
 * how many days have passed since delivery, and the classification rule is
 * owned by the policy team, not by whichever checkout or support code calls
 * it. Naming that force, "the outcome varies by days-since-delivery alone,
 * write it as a Strategy with the usual shape," is what gives both a human
 * reviewer and an AI assistant something concrete to check a draft against:
 * does the draft depend on anything the contract does not mention, and does
 * it honor every boundary the policy actually specifies.
 */
@FunctionalInterface
public interface ReturnEligibilityStrategy {

    ReturnEligibility classify(int daysSinceDelivery);
}
