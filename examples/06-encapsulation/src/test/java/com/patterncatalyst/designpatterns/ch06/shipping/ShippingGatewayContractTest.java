package com.patterncatalyst.designpatterns.ch06.shipping;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the point of the interface-plus-factory indirection this chapter
 * teaches: the same assertions pass against {@link ShippingGateway#inMemory()}
 * and against {@link SimpleMapShippingGateway}, a second implementation
 * written entirely in this test file. Neither implementation knows the
 * other exists, and this test never changes to accommodate either one; both
 * sides depend only on the {@link ShippingGateway} contract.
 */
class ShippingGatewayContractTest {

    private static final Manifest SAMPLE_MANIFEST = new Manifest(
            "MF-4000",
            List.of(new ManifestLineItem("PALLET-STD", 1, "Standard pallet")),
            Instant.parse("2026-01-01T00:00:00Z"));

    static Stream<ShippingGateway> gateways() {
        return Stream.of(ShippingGateway.inMemory(), new SimpleMapShippingGateway());
    }

    @ParameterizedTest
    @MethodSource("gateways")
    void everyImplementationHonorsTheSameCreateFindRecordContract(ShippingGateway gateway) {
        Shipment created = gateway.createShipment("Tacoma, WA", "Yakima, WA", SAMPLE_MANIFEST);

        Shipment updated = gateway.recordEvent(
                created.trackingId(),
                new TrackingEvent(Instant.parse("2026-01-01T07:00:00Z"), "Tacoma, WA", TrackingStatus.PICKED_UP));

        assertThat(gateway.find(created.trackingId())).contains(updated);
        assertThat(gateway.find("unknown-id")).isEmpty();
    }

    /**
     * A second, independent {@link ShippingGateway} implementation. It never
     * references {@code InMemoryShippingGateway}, and the test above requires
     * no change to admit it: that is Open/Closed-style extensibility showing
     * up as a direct consequence of depending on the interface.
     */
    private static final class SimpleMapShippingGateway implements ShippingGateway {
        private final Map<String, Shipment> shipments = new LinkedHashMap<>();

        @Override
        public Shipment createShipment(String origin, String destination, Manifest manifest) {
            String trackingId = "TEST-" + (shipments.size() + 1);
            Shipment shipment = Shipment.created(trackingId, origin, destination, manifest);
            shipments.put(trackingId, shipment);
            return shipment;
        }

        @Override
        public Optional<Shipment> find(String trackingId) {
            return Optional.ofNullable(shipments.get(trackingId));
        }

        @Override
        public Shipment recordEvent(String trackingId, TrackingEvent event) {
            Shipment existing = shipments.get(trackingId);
            if (existing == null) {
                throw new NoSuchElementException("no shipment tracked under " + trackingId);
            }
            Shipment updated = existing.withEvent(event);
            shipments.put(trackingId, updated);
            return updated;
        }
    }
}
