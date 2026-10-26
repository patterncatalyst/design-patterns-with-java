package com.patterncatalyst.designpatterns.ch19.shipping;

import com.patterncatalyst.designpatterns.ch19.shipping.approval.ApprovalChains;
import com.patterncatalyst.designpatterns.ch19.shipping.approval.ApprovalChecks;
import com.patterncatalyst.designpatterns.ch19.shipping.approval.ApprovalHandler;
import com.patterncatalyst.designpatterns.ch19.shipping.approval.ApprovalOutcome;
import com.patterncatalyst.designpatterns.ch19.shipping.approval.ApprovalRequest;
import com.patterncatalyst.designpatterns.ch19.shipping.approval.FunctionalApprovalChain;
import com.patterncatalyst.designpatterns.ch19.shipping.manifest.DestinationZone;
import com.patterncatalyst.designpatterns.ch19.shipping.manifest.Shipment;
import com.patterncatalyst.designpatterns.ch19.shipping.manifest.ShipmentManifest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Exercises both halves of this chapter's example and prints the results. */
public final class ChainIteratorDemo {

    public static void main(String[] args) {
        runApprovalChain();
        System.out.println();
        runManifest();
    }

    private static void runApprovalChain() {
        List<ApprovalRequest> requests = List.of(
                new ApprovalRequest(55.0, false, "US"),
                new ApprovalRequest(10.0, true, "FR"),
                new ApprovalRequest(10.0, false, "KP"),
                new ApprovalRequest(10.0, false, "DE"));

        System.out.println("== Chain of Responsibility: the object chain ==");
        ApprovalHandler chain = ApprovalChains.defaultChain();
        for (ApprovalRequest request : requests) {
            List<String> callLog = new ArrayList<>();
            ApprovalOutcome outcome = chain.handle(request, callLog);
            System.out.println(request.destinationCountry() + ": " + describe(outcome));
            System.out.println("  chain visited: " + String.join(" -> ", callLog));
        }

        System.out.println();
        System.out.println("== Chain of Responsibility: the functional composition ==");
        FunctionalApprovalChain functionalChain = FunctionalApprovalChain.of(
                ApprovalChecks.weightLimit(40.0),
                ApprovalChecks.hazmatClearance(Set.of("US", "CA")),
                ApprovalChecks.embargo(Set.of("KP", "IR")));
        for (ApprovalRequest request : requests) {
            System.out.println(request.destinationCountry() + ": " + describe(functionalChain.decide(request)));
        }
    }

    private static String describe(ApprovalOutcome outcome) {
        return switch (outcome) {
            case ApprovalOutcome.Rejected rejected -> "Rejected by " + rejected.decidedBy() + " -- " + rejected.reason();
            case ApprovalOutcome.NeedsManualReview review ->
                    "Needs manual review by " + review.decidedBy() + " -- " + review.reason();
        };
    }

    private static void runManifest() {
        ShipmentManifest manifest = new ShipmentManifest();
        manifest.add(new Shipment("TRK-INTL-1", DestinationZone.INTERNATIONAL, 3.0));
        manifest.add(new Shipment("TRK-LOC-1", DestinationZone.LOCAL, 1.0));
        manifest.add(new Shipment("TRK-NAT-1", DestinationZone.NATIONAL, 5.0));
        manifest.add(new Shipment("TRK-NAT-2", DestinationZone.NATIONAL, 7.5));
        manifest.add(new Shipment("TRK-REG-1", DestinationZone.REGIONAL, 2.0));

        System.out.println("== Iterator: a hand-written Iterator over a zone-bucketed manifest ==");
        System.out.println("Inserted INTL, LOC, NAT, NAT, REG order; the Iterator still visits zone order:");
        for (Shipment shipment : manifest) {
            System.out.println("  " + shipment.trackingNumber());
        }

        System.out.println();
        System.out.println("== Iterator: the same data through a Stream pipeline ==");
        List<Double> nationalWeights = manifest.stream()
                .filter(shipment -> shipment.zone() == DestinationZone.NATIONAL)
                .map(Shipment::weightKg)
                .collect(Collectors.toList());
        System.out.println("National-zone weights (kg): " + nationalWeights);
    }
}
