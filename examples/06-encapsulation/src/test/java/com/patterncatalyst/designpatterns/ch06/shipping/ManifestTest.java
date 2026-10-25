package com.patterncatalyst.designpatterns.ch06.shipping;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManifestTest {

    @Test
    void compactConstructorDefensivelyCopiesTheCallersList() {
        List<ManifestLineItem> mutable = new ArrayList<>();
        mutable.add(new ManifestLineItem("PALLET-STD", 2, "Standard pallet"));

        Manifest manifest = new Manifest("MF-1000", mutable, Instant.parse("2026-01-01T00:00:00Z"));
        mutable.add(new ManifestLineItem("SNEAKY", 1, "added after construction"));

        assertThat(manifest.items()).hasSize(1);
        assertThat(manifest.totalUnits()).isEqualTo(2);
    }

    @Test
    void itemsCannotBeMutatedByACaller() {
        Manifest manifest = new Manifest(
                "MF-1000",
                List.of(new ManifestLineItem("PALLET-STD", 2, "Standard pallet")),
                Instant.parse("2026-01-01T00:00:00Z"));

        assertThatThrownBy(() -> manifest.items().add(new ManifestLineItem("HACK", 1, "")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsAnEmptyItemList() {
        assertThatThrownBy(() -> new Manifest("MF-1000", List.of(), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void totalUnitsSumsEveryLineItemsQuantity() {
        Manifest manifest = new Manifest(
                "MF-1000",
                List.of(
                        new ManifestLineItem("PALLET-STD", 4, "Standard pallet"),
                        new ManifestLineItem("CRATE-REF", 3, "Refrigerated crate")),
                Instant.now());

        assertThat(manifest.totalUnits()).isEqualTo(7);
    }
}
