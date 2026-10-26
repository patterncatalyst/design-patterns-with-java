package com.patterncatalyst.designpatterns.ch17.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PackagingSpecCatalogTest {

    @Test
    void sharesOneInstancePerCodeAcrossManyParcelLookups() {
        PackagingSpecCatalog catalog = new PackagingSpecCatalog(Map.of(
                "STD-BOX", new PackagingSpec("STD-BOX", "corrugated cardboard", 180, HazmatClass.NONE),
                "LI-PACK", new PackagingSpec("LI-PACK", "UN 3480 lithium pack", 95, HazmatClass.LITHIUM)));

        PackagingSpec first = catalog.get("STD-BOX");
        for (int i = 0; i < 60; i++) {
            catalog.get(i % 2 == 0 ? "STD-BOX" : "LI-PACK");
        }

        assertThat(catalog.get("STD-BOX")).isSameAs(first);
        assertThat(catalog.materializedCount()).isEqualTo(2);
    }
}
