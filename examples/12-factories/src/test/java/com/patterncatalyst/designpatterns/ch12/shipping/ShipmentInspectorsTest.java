package com.patterncatalyst.designpatterns.ch12.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the static factory method resolves each {@link ShipmentCategory}
 * to the matching {@code ShipmentInspector} subclass.
 */
class ShipmentInspectorsTest {

    @Test
    void standardCategoryResolvesToTheStandardInspector() {
        assertThat(ShipmentInspectors.forCategory(ShipmentCategory.STANDARD))
                .isInstanceOf(StandardShipmentInspector.class);
    }

    @Test
    void hazmatCategoryResolvesToTheHazmatInspector() {
        assertThat(ShipmentInspectors.forCategory(ShipmentCategory.HAZMAT))
                .isInstanceOf(HazmatShipmentInspector.class);
    }
}
