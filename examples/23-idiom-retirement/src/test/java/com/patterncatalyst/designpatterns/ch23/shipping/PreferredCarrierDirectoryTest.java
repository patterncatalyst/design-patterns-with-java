package com.patterncatalyst.designpatterns.ch23.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PreferredCarrierDirectoryTest {

    private final PreferredCarrierDirectory directory =
            new PreferredCarrierDirectory(Map.of("customer-42", "freight-zone"));

    @Test
    void returnsThePreferredCarrierWhenOneIsOnFile() {
        Optional<String> preferred = directory.preferredCarrierFor("customer-42");

        assertThat(preferred).contains("freight-zone");
    }

    @Test
    void returnsAnEmptyOptionalWhenNoPreferenceIsOnFile() {
        Optional<String> preferred = directory.preferredCarrierFor("customer-99");

        assertThat(preferred).isEmpty();
    }

    @Test
    void mutatingTheSourceMapAfterConstructionDoesNotChangeTheDirectory() {
        Map<String, String> source = new HashMap<>(Map.of("customer-7", "ground"));
        PreferredCarrierDirectory copy = new PreferredCarrierDirectory(source);

        source.put("customer-7", "air");
        source.put("customer-8", "freight-zone");

        assertThat(copy.preferredCarrierFor("customer-7")).contains("ground");
        assertThat(copy.preferredCarrierFor("customer-8")).isEmpty();
    }
}
