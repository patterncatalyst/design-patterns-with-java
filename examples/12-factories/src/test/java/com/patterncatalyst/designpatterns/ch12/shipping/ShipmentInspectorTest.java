package com.patterncatalyst.designpatterns.ch12.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the Factory Method hook: each {@code ShipmentInspector} subclass
 * runs the one fixed sequence declared in the base class, differing only in
 * which {@link InspectionChecklist} {@code createChecklist()} hands back.
 */
class ShipmentInspectorTest {

    @Test
    void standardInspectorRunsTheStandardChecklistAndPasses() {
        Shipment shipment = new Shipment("Canada", 4.2, 18_000, Region.NORTH_AMERICA);

        InspectionResult result = new StandardShipmentInspector().inspect(shipment);

        assertThat(result.passed()).isTrue();
        assertThat(result.log()).noneMatch(line -> line.contains("placard"));
    }

    @Test
    void hazmatInspectorRunsTheHazmatChecklistAndPassesForAnAcceptingDestination() {
        Shipment shipment = new Shipment("Japan", 12.0, 92_000, Region.ASIA_PACIFIC);

        InspectionResult result = new HazmatShipmentInspector().inspect(shipment);

        assertThat(result.passed()).isTrue();
        assertThat(result.log()).anyMatch(line -> line.contains("placard"));
    }

    @Test
    void hazmatInspectorFailsForAnEmbargoedDestination() {
        Shipment shipment = new Shipment("North Korea", 12.0, 92_000, Region.ASIA_PACIFIC);

        InspectionResult result = new HazmatShipmentInspector().inspect(shipment);

        assertThat(result.passed()).isFalse();
        assertThat(result.log()).anyMatch(line -> line.contains("FAILED"));
    }

    @Test
    void standardInspectorNeverRunsTheHazmatEmbargoCheckEvenForAnEmbargoedDestination() {
        Shipment shipment = new Shipment("North Korea", 4.2, 18_000, Region.NORTH_AMERICA);

        InspectionResult result = new StandardShipmentInspector().inspect(shipment);

        assertThat(result.passed()).isTrue();
    }
}
