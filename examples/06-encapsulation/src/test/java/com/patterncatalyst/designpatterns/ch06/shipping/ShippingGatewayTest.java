package com.patterncatalyst.designpatterns.ch06.shipping;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShippingGatewayTest {

    private static final Manifest SAMPLE_MANIFEST = new Manifest(
            "MF-3000",
            List.of(new ManifestLineItem("CRATE-REF", 1, "Refrigerated crate")),
            Instant.parse("2026-01-01T00:00:00Z"));

    @Test
    void inMemoryReturnsThePackagePrivateImplementationBehindTheInterface() {
        ShippingGateway gateway = ShippingGateway.inMemory();

        assertThat(gateway.getClass().getSimpleName()).isEqualTo("InMemoryShippingGateway");
    }

    @Test
    void createShipmentStoresItForLaterLookup() {
        ShippingGateway gateway = ShippingGateway.inMemory();

        Shipment created = gateway.createShipment("Seattle, WA", "Spokane, WA", SAMPLE_MANIFEST);

        assertThat(gateway.find(created.trackingId())).contains(created);
    }

    @Test
    void findReturnsEmptyForAnUnknownTrackingId() {
        ShippingGateway gateway = ShippingGateway.inMemory();

        assertThat(gateway.find("NOPE")).isEmpty();
    }

    @Test
    void recordEventAppendsWithoutMutatingThePreviouslyReturnedShipment() {
        ShippingGateway gateway = ShippingGateway.inMemory();
        Shipment created = gateway.createShipment("Seattle, WA", "Spokane, WA", SAMPLE_MANIFEST);

        Shipment updated = gateway.recordEvent(
                created.trackingId(),
                new TrackingEvent(Instant.parse("2026-01-01T08:00:00Z"), "Seattle, WA", TrackingStatus.PICKED_UP));

        assertThat(created.history().events()).isEmpty();
        assertThat(updated.history().events()).hasSize(1);
        assertThat(gateway.find(created.trackingId())).contains(updated);
    }

    @Test
    void twoGatewaysFromTheFactoryAreIndependent() {
        ShippingGateway first = ShippingGateway.inMemory();
        ShippingGateway second = ShippingGateway.inMemory();

        first.createShipment("Seattle, WA", "Spokane, WA", SAMPLE_MANIFEST);

        assertThat(second.find("SHIP-1")).isEmpty();
    }
}
