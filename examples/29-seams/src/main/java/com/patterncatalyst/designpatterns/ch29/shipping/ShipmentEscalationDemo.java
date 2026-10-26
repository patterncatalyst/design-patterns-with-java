package com.patterncatalyst.designpatterns.ch29.shipping;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Runs the legacy escalation service, the seamed escalation service, and the
 * risk scorer's subclass-and-override seam side by side, so the terminal
 * shows the same facts the test suites assert.
 */
public final class ShipmentEscalationDemo {

    private ShipmentEscalationDemo() {
    }

    public static void main(String[] args) {
        Shipment overdue = new Shipment("SHM-7001", Instant.now().minus(1, ChronoUnit.DAYS), 12.5);
        Shipment onTime = new Shipment("SHM-7002", Instant.now().plus(1, ChronoUnit.DAYS), 8.0);

        System.out.println("== Legacy service: hard-wired clock and gateway, no seam ==");
        LegacyShipmentEscalationService legacy = new LegacyShipmentEscalationService();
        System.out.println("legacy " + overdue.id() + ": " + legacy.escalate(overdue));
        System.out.println("legacy " + onTime.id() + ": " + legacy.escalate(onTime));

        System.out.println();
        System.out.println("== Seamed service: Clock and CarrierGateway injected through the constructor ==");
        SmtpCarrierGateway gateway = new SmtpCarrierGateway();
        ShipmentEscalationService seamed = new ShipmentEscalationService(Clock.systemUTC(), gateway);
        System.out.println("seamed " + overdue.id() + ": " + seamed.escalate(overdue));
        System.out.println("seamed " + onTime.id() + ": " + seamed.escalate(onTime));
        System.out.println("gateway log: " + gateway.sentNotifications());

        System.out.println();
        System.out.println("== Risk scorer: subclass-and-override seam ==");
        Shipment heavy = new Shipment("SHM-7003", Instant.now().plus(3, ChronoUnit.DAYS), 150.0);
        ShipmentRiskScorer scorer = new ShipmentRiskScorer();
        System.out.println("production risk score for a heavy shipment (today's calendar): " + scorer.score(heavy));
    }
}
