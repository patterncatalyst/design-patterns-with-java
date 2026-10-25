package com.patterncatalyst.designpatterns.ch03.billing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceCalculatorTest {

    private static final Invoice INVOICE = new Invoice("INV-3087", "Anchor Point Imports", List.of(
            new LineItem("Freight handling", 2, 82_500),
            new LineItem("Warehouse storage", 6, 4_500)));

    @Mock
    private InvoiceRule taxRule;

    @Mock
    private InvoiceRule discountRule;

    @Test
    void subtotalIsTheSumOfEveryLineItemsExtendedPrice() {
        InvoiceCalculator calculator = new InvoiceCalculator(List.of());

        Totals totals = calculator.totalsFor(INVOICE);

        assertThat(totals.subtotalCents()).isEqualTo(192_000);
        assertThat(totals.totalCents()).isEqualTo(192_000);
    }

    @Test
    void totalAppliesEveryRulesAdjustmentToTheSubtotal() {
        InvoiceCalculator calculator = new InvoiceCalculator(List.of(
                new PercentageTaxRule("sales tax", 0.08),
                new StandardDiscountRule("contract discount", 0.05),
                new FlatServiceFeeRule("dock handling fee", 3_500)));

        Totals totals = calculator.totalsFor(INVOICE);

        assertThat(totals.subtotalCents()).isEqualTo(192_000);
        assertThat(totals.totalCents()).isEqualTo(201_260);
    }

    @Test
    void everyRuleReceivesTheOriginalSubtotalRegardlessOfOrder() {
        // Each rule is a mock, so this proves InvoiceCalculator's contract
        // directly: every InvoiceRule sees the same, original subtotal,
        // never a running total another rule already changed. If the
        // calculator fed rule N+1 a total that already included rule N's
        // adjustment, this stub would never match and the test would fail.
        when(taxRule.adjustmentCents(eq(192_000L))).thenReturn(15_360L);
        when(discountRule.adjustmentCents(eq(192_000L))).thenReturn(-9_600L);
        InvoiceCalculator calculator = new InvoiceCalculator(List.of(taxRule, discountRule));

        Totals totals = calculator.totalsFor(INVOICE);

        assertThat(totals.totalCents()).isEqualTo(192_000 + 15_360 - 9_600);
        verify(taxRule).adjustmentCents(192_000L);
        verify(discountRule).adjustmentCents(192_000L);
    }

    @Test
    void addingANewRuleTypeRequiresNoEditToInvoiceCalculator() {
        // RoundUpToWholeDollarRule is defined below, inside this test file.
        // InvoiceCalculator's own source has never seen this type and never
        // will; it is constructed here, handed to the same constructor every
        // other rule uses, and InvoiceCalculator applies it correctly. That
        // is the Open/Closed claim made concrete: a new rule is a new type,
        // not an edited InvoiceCalculator.
        Invoice oddCentsInvoice = new Invoice("INV-9001", "Anchor Point Imports",
                List.of(new LineItem("Freight handling", 1, 82_550)));
        InvoiceRule roundUp = new RoundUpToWholeDollarRule();
        InvoiceCalculator calculator = new InvoiceCalculator(List.of(roundUp));

        Totals totals = calculator.totalsFor(oddCentsInvoice);

        assertThat(totals.subtotalCents()).isEqualTo(82_550);
        assertThat(totals.totalCents()).isEqualTo(82_600);
    }

    @Test
    void totalDoesNotDependOnTheOrderRulesWereSuppliedIn() {
        PercentageTaxRule tax = new PercentageTaxRule("sales tax", 0.08);
        StandardDiscountRule discount = new StandardDiscountRule("contract discount", 0.05);
        FlatServiceFeeRule fee = new FlatServiceFeeRule("dock handling fee", 3_500);
        InvoiceCalculator forwardOrder = new InvoiceCalculator(List.of(tax, discount, fee));
        InvoiceCalculator reverseOrder = new InvoiceCalculator(List.of(fee, discount, tax));

        Totals forwardTotals = forwardOrder.totalsFor(INVOICE);
        Totals reverseTotals = reverseOrder.totalsFor(INVOICE);

        assertThat(forwardTotals.totalCents()).isEqualTo(reverseTotals.totalCents());
    }

    /**
     * A rule InvoiceCalculator has never been compiled against: it rounds
     * the subtotal up to the next whole dollar. Its existence, defined only
     * in this test, is the proof that {@link InvoiceRule} is the real
     * extension point and not just a convention.
     */
    private static final class RoundUpToWholeDollarRule implements InvoiceRule {
        @Override
        public long adjustmentCents(long subtotalCents) {
            long remainder = subtotalCents % 100;
            return remainder == 0 ? 0 : 100 - remainder;
        }

        @Override
        public String name() {
            return "round up to whole dollar";
        }
    }
}
