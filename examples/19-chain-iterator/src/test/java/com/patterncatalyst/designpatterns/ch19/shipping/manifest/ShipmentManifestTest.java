package com.patterncatalyst.designpatterns.ch19.shipping.manifest;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ShipmentManifestTest {

    private ShipmentManifest manifestWithFiveShipments() {
        ShipmentManifest manifest = new ShipmentManifest();
        manifest.add(new Shipment("TRK-INTL-1", DestinationZone.INTERNATIONAL, 3.0));
        manifest.add(new Shipment("TRK-LOC-1", DestinationZone.LOCAL, 1.0));
        manifest.add(new Shipment("TRK-NAT-1", DestinationZone.NATIONAL, 5.0));
        manifest.add(new Shipment("TRK-NAT-2", DestinationZone.NATIONAL, 7.5));
        manifest.add(new Shipment("TRK-REG-1", DestinationZone.REGIONAL, 2.0));
        return manifest;
    }

    @Test
    void iteratorVisitsShipmentsInZoneOrderRegardlessOfInsertionOrder() {
        ShipmentManifest manifest = manifestWithFiveShipments();

        List<String> trackingNumbers = new ArrayList<>();
        for (Shipment shipment : manifest) {
            trackingNumbers.add(shipment.trackingNumber());
        }

        assertThat(trackingNumbers).containsExactly(
                "TRK-LOC-1", "TRK-REG-1", "TRK-NAT-1", "TRK-NAT-2", "TRK-INTL-1");
    }

    @Test
    void streamProducesTheSameOrderAsTheIterator() {
        ShipmentManifest manifest = manifestWithFiveShipments();

        List<String> fromStream = manifest.stream()
                .map(Shipment::trackingNumber)
                .collect(Collectors.toList());

        assertThat(fromStream).containsExactly(
                "TRK-LOC-1", "TRK-REG-1", "TRK-NAT-1", "TRK-NAT-2", "TRK-INTL-1");
    }

    @Test
    void streamFiltersAndMapsOverTheManifest() {
        ShipmentManifest manifest = manifestWithFiveShipments();

        List<Double> nationalWeights = manifest.stream()
                .filter(shipment -> shipment.zone() == DestinationZone.NATIONAL)
                .map(Shipment::weightKg)
                .collect(Collectors.toList());

        assertThat(nationalWeights).containsExactly(5.0, 7.5);
    }

    @Test
    void emptyManifestProducesNoElements() {
        ShipmentManifest manifest = new ShipmentManifest();

        assertThat(manifest.iterator().hasNext()).isFalse();
        assertThat(manifest.stream()).isEmpty();
    }
}
