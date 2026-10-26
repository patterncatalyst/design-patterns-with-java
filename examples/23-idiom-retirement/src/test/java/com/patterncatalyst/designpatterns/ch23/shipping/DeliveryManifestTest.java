package com.patterncatalyst.designpatterns.ch23.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeliveryManifestTest {

    @Test
    void mutatingTheSourceListAfterConstructionDoesNotChangeTheManifest() {
        List<String> source = new ArrayList<>(List.of("GROUND-1", "GROUND-2"));
        DeliveryManifest manifest = new DeliveryManifest("BATCH-1", source);

        source.add("GROUND-3");

        assertThat(manifest.confirmationNumbers()).containsExactly("GROUND-1", "GROUND-2");
    }

    @Test
    void theReturnedListCannotBeMutatedByACaller() {
        DeliveryManifest manifest = new DeliveryManifest("BATCH-1", List.of("GROUND-1"));

        assertThatThrownBy(() -> manifest.confirmationNumbers().add("GROUND-2"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void twoManifestsWithEqualContentsAreEqual() {
        DeliveryManifest first = new DeliveryManifest("BATCH-1", List.of("GROUND-1", "GROUND-2"));
        DeliveryManifest second = new DeliveryManifest("BATCH-1", List.of("GROUND-1", "GROUND-2"));

        assertThat(first).isEqualTo(second);
    }
}
