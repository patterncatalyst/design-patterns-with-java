package com.patterncatalyst.designpatterns.ch34.returns;

/**
 * Runs the drafted (accepted) and plausible-wrong (sent-back) strategies
 * side by side across every boundary the return policy specifies, then
 * isolates the exact day the two disagree and prices that disagreement in
 * dollars.
 */
public final class VerificationGateDemo {

    private VerificationGateDemo() {
    }

    public static void main(String[] args) {
        ReturnEligibilityStrategy drafted = new DraftedReturnEligibilityStrategy();
        ReturnEligibilityStrategy plausibleWrong = new PlausibleWrongReturnEligibilityStrategy();
        int[] sampleDays = {0, 29, 30, 59, 60};

        System.out.println("== Drafted strategy (accepted by the verification gate) ==");
        for (int days : sampleDays) {
            System.out.printf("Day %2d: %s%n", days, drafted.classify(days));
        }

        System.out.println();
        System.out.println("== Plausible-wrong strategy (sent back by the verification gate) ==");
        for (int days : sampleDays) {
            System.out.printf("Day %2d: %s%n", days, plausibleWrong.classify(days));
        }

        System.out.println();
        System.out.println("== Where they disagree ==");
        Money orderTotal = Money.ofCents(20_000);
        ReturnEligibility draftedDay30 = drafted.classify(30);
        ReturnEligibility wrongDay30 = plausibleWrong.classify(30);
        Money draftedRefund = RefundPolicy.refundAmount(draftedDay30, orderTotal);
        Money wrongRefund = RefundPolicy.refundAmount(wrongDay30, orderTotal);

        System.out.printf("Day 30, order total %s%n", orderTotal);
        System.out.printf("Drafted strategy:        %s -> refund %s%n", draftedDay30, draftedRefund);
        System.out.printf("Plausible-wrong strategy: %s -> refund %s%n", wrongDay30, wrongRefund);
        System.out.printf(
                "Overpayment from the off-by-one: %s%n",
                Money.ofCents(wrongRefund.cents() - draftedRefund.cents()));
    }
}
