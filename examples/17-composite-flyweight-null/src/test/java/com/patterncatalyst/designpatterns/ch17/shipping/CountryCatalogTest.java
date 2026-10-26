package com.patterncatalyst.designpatterns.ch17.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class CountryCatalogTest {

    @Test
    void sharesOneInstancePerIsoCodeAcrossManyDestinationLookups() {
        CountryCatalog catalog = new CountryCatalog(Map.of(
                "US", new Country("US", "United States", false),
                "DE", new Country("DE", "Germany", true)));

        Country first = catalog.get("US");
        for (int i = 0; i < 40; i++) {
            catalog.get(i % 2 == 0 ? "US" : "DE");
        }

        assertThat(catalog.get("US")).isSameAs(first);
        assertThat(catalog.materializedCount()).isEqualTo(2);
    }
}
