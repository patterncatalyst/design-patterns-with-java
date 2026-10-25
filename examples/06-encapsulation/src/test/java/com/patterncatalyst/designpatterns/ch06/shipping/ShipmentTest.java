package com.patterncatalyst.designpatterns.ch06.shipping;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentTest {

    private static final Manifest SAMPLE_MANIFEST = new Manifest(
            "MF-2000",
            List.of(new ManifestLineItem("PALLET-STD", 2, "Standard pallet")),
            Instant.parse("2026-01-01T00:00:00Z"));

    @Test
    void createdStartsWithTheSharedEmptyHistoryInstance() {
        Shipment shipment = Shipment.created("SHIP-1", "Portland, OR", "Boise, ID", SAMPLE_MANIFEST);

        assertThat(shipment.history()).isSameAs(TrackingHistory.empty());
    }

    @Test
    void withEventReturnsANewShipmentAndLeavesTheOriginalUntouched() {
        Shipment original = Shipment.created("SHIP-1", "Portland, OR", "Boise, ID", SAMPLE_MANIFEST);
        TrackingEvent pickedUp = new TrackingEvent(Instant.parse("2026-01-01T09:00:00Z"), "Portland, OR", TrackingStatus.PICKED_UP);

        Shipment updated = original.withEvent(pickedUp);

        assertThat(original.history().events()).isEmpty();
        assertThat(updated.history().events()).containsExactly(pickedUp);
        assertThat(updated).isNotSameAs(original);
    }

    @Test
    void rejectsABlankTrackingId() {
        assertThatThrownBy(() -> Shipment.created("  ", "Portland, OR", "Boise, ID", SAMPLE_MANIFEST))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANullManifest() {
        assertThatThrownBy(() -> Shipment.created("SHIP-1", "Portland, OR", "Boise, ID", null))
                .isInstanceOf(NullPointerException.class);
    }
}
