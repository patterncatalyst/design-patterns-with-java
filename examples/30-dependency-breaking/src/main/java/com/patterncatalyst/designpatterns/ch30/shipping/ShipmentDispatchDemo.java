package com.patterncatalyst.designpatterns.ch30.shipping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs all four dependency-breaking techniques against production wiring,
 * at the terminal, so the seams are visible as real output rather than
 * only as test assertions. No containers, no real SMTP, no real clock
 * substitution in production: the double classes below live only in this
 * demo, the same way a test's double would.
 */
public final class ShipmentDispatchDemo {

    private ShipmentDispatchDemo() {
    }

    public static void main(String[] args) throws IOException {
        System.out.println("== Extract Interface + Parameterize Constructor: LegacyShipmentProcessor ==");
        LegacyShipmentProcessor liveProcessor = new LegacyShipmentProcessor("ops-supervisor@example.com");
        liveProcessor.dispatch(new Shipment("SH-7001", 12_000));

        List<String> recorded = new ArrayList<>();
        NotificationGateway recordingGateway = (operationsEmail, message) ->
                recorded.add("to=%s \"%s\"".formatted(operationsEmail, message));
        LegacyShipmentProcessor testStyleProcessor =
                new LegacyShipmentProcessor("ops-supervisor@example.com", recordingGateway);
        testStyleProcessor.dispatch(new Shipment("SH-7002", 8_000));
        System.out.println("  recorded (no real SMTP call): " + recorded.get(0));

        System.out.println();
        System.out.println("== Extract and Override Factory Method: surcharge calculator ==");
        System.out.println("  live surcharge (8%): " + liveProcessor.dispatch(new Shipment("SH-7003", 10_000)).totalCostCents() + " cents");
        LegacyShipmentProcessor flatRateProcessor = new LegacyShipmentProcessor("ops-supervisor@example.com", recordingGateway) {
            @Override
            protected SurchargeCalculator createSurchargeCalculator() {
                return baseCostCents -> 500L;
            }
        };
        System.out.println("  overridden flat-rate surcharge: "
                + flatRateProcessor.dispatch(new Shipment("SH-7004", 10_000)).totalCostCents() + " cents");

        System.out.println();
        System.out.println("== Subclass and Override Method: express-lane clock ==");
        LegacyShipmentProcessor weekdayProcessor = new LegacyShipmentProcessor("ops-supervisor@example.com", recordingGateway) {
            @Override
            protected LocalDate today() {
                return LocalDate.of(2026, Month.OCTOBER, 20); // a Tuesday
            }
        };
        LegacyShipmentProcessor weekendProcessor = new LegacyShipmentProcessor("ops-supervisor@example.com", recordingGateway) {
            @Override
            protected LocalDate today() {
                return LocalDate.of(2026, Month.OCTOBER, 24); // a Saturday
            }
        };
        System.out.println("  Tuesday dispatch express eligible: "
                + weekdayProcessor.dispatch(new Shipment("SH-7005", 6_000)).expressLaneEligible());
        System.out.println("  Saturday dispatch express eligible: "
                + weekendProcessor.dispatch(new Shipment("SH-7006", 6_000)).expressLaneEligible());

        System.out.println();
        System.out.println("== Adapt Parameter: manifest summary ==");
        ManifestSource fake = new ManifestSource() {
            @Override
            public String carrierCode() {
                return "FASTFREIGHT";
            }

            @Override
            public int packageCount() {
                return 275;
            }
        };
        System.out.println("  fake source, no file read: " + liveProcessor.summarizeManifest(fake));

        Path manifestPath = Files.createTempFile("carrier-manifest", ".txt");
        try {
            Files.writeString(manifestPath, "SLOWHAUL" + System.lineSeparator() + "48" + System.lineSeparator());
            CarrierManifestFile realFile = new CarrierManifestFile(manifestPath);
            CarrierManifestFileAdapter adapter = new CarrierManifestFileAdapter(realFile);
            System.out.println("  real file via adapter: " + liveProcessor.summarizeManifest(adapter));
        } finally {
            Files.deleteIfExists(manifestPath);
        }
    }
}
