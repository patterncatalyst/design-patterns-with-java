package com.patterncatalyst.designpatterns.ch12.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Asserts the static factory method and the Supplier-backed registry behind
 * it resolve every region, agree with a configuration-driven name lookup,
 * and reject a name that matches no region.
 */
class ShippingToolkitsTest {

    @Test
    void resolvesEveryRegionToATypedToolkit() {
        assertThat(ShippingToolkits.forRegion(Region.NORTH_AMERICA)).isInstanceOf(
                com.patterncatalyst.designpatterns.ch12.shipping.regional.NorthAmericaToolkit.class);
        assertThat(ShippingToolkits.forRegion(Region.EUROPEAN_UNION)).isInstanceOf(
                com.patterncatalyst.designpatterns.ch12.shipping.regional.EuropeanUnionToolkit.class);
        assertThat(ShippingToolkits.forRegion(Region.ASIA_PACIFIC)).isInstanceOf(
                com.patterncatalyst.designpatterns.ch12.shipping.regional.AsiaPacificToolkit.class);
    }

    @Test
    void eachCallReturnsAFreshToolkitInstance() {
        ShippingToolkit first = ShippingToolkits.forRegion(Region.NORTH_AMERICA);
        ShippingToolkit second = ShippingToolkits.forRegion(Region.NORTH_AMERICA);

        assertThat(first).isNotSameAs(second);
    }

    @Test
    void nameLookupIsCaseAndSpaceInsensitiveAndAgreesWithTheEnumLookup() {
        Shipment shipment = new Shipment("Canada", 4.2, 18_000, Region.NORTH_AMERICA);

        ProcessedShipment byEnum = new RegionalShippingDesk(ShippingToolkits.forRegion(Region.NORTH_AMERICA))
                .process(shipment);
        ProcessedShipment byName = new RegionalShippingDesk(ShippingToolkits.forRegionName("north america"))
                .process(shipment);

        assertThat(byName).isEqualTo(byEnum);
    }

    @Test
    void rejectsAnUnrecognizedRegionName() {
        assertThatThrownBy(() -> ShippingToolkits.forRegionName("mars-colony"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mars-colony");
    }
}
