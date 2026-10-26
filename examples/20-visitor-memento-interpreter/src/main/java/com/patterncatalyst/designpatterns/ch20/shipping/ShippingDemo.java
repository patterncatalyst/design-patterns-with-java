package com.patterncatalyst.designpatterns.ch20.shipping;

import java.util.List;

/** Exercises Visitor, Memento, and Interpreter against one shipping manifest. */
public final class ShippingDemo {

    private ShippingDemo() {
    }

    public static void main(String[] args) {
        List<ManifestEntry> manifest = List.of(
                new StandardParcel("PCL-100", Money.ofCents(5_00), 3.2),
                new InternationalParcel("PCL-200", Money.ofCents(42_00), 6.0, "8471.30", "DE"),
                new HazardousParcel("PCL-300", Money.ofCents(18_00), 9.5, "Class 9", "3481"),
                new OversizedParcel("PCL-400", Money.ofCents(60_00), 22.0, 180.0, 90.0, 70.0));

        System.out.println("== Visitor: three operations over a fixed ManifestEntry hierarchy ==");
        for (ManifestEntry entry : manifest) {
            System.out.println(entry.id() + ": cost=" + ManifestOperations.shippingCost(entry)
                    + ", customs=" + ManifestOperations.customsSummary(entry));
        }

        System.out.println();
        System.out.println("== Visitor: validation collects every issue, not just the first ==");
        ManifestEntry incomplete = new InternationalParcel("PCL-999", Money.ofCents(10_00), 0.0, "", "");
        System.out.println("Issues for PCL-999: " + ManifestOperations.validate(incomplete));

        System.out.println();
        System.out.println("== Memento: editing a draft, then restoring a prior snapshot ==");
        ManifestDraft draft = new ManifestDraft("Q4-export-batch");
        draft.addEntry(manifest.get(0));
        var checkpoint = draft.save();
        System.out.println("Before edits:  name=" + draft.draftName() + " entries=" + draft.entries().size()
                + " submitted=" + draft.isSubmitted());

        draft.rename("DRAFT-EDIT");
        draft.addEntry(manifest.get(1));
        draft.submit();
        System.out.println("After edits:   name=" + draft.draftName() + " entries=" + draft.entries().size()
                + " submitted=" + draft.isSubmitted());

        draft.restore(checkpoint);
        System.out.println("After restore: name=" + draft.draftName() + " entries=" + draft.entries().size()
                + " submitted=" + draft.isSubmitted());

        System.out.println();
        System.out.println("== Interpreter: nesting changes the evaluated surcharge ==");
        SurchargeExpr fuelOnHandling = new Multiply(
                new Add(new Literal(500), new IfWeightAbove(20.0, new Literal(1000), new Literal(0))),
                1.08);
        SurchargeExpr handlingOnFuel = new Add(
                new Literal(500),
                new IfWeightAbove(20.0, new Multiply(new Literal(1000), 1.08), new Literal(0)));

        System.out.println(SurchargeExprs.describe(fuelOnHandling) + " -> "
                + Money.ofCents(SurchargeExprs.evaluateCents(fuelOnHandling, 22.0)));
        System.out.println(SurchargeExprs.describe(handlingOnFuel) + " -> "
                + Money.ofCents(SurchargeExprs.evaluateCents(handlingOnFuel, 22.0)));
    }
}
