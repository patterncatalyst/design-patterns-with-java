package com.patterncatalyst.designpatterns.ch17.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link ShipmentNodes} must aggregate a {@link Parcel} leaf and a {@link
 * ShipmentBox} branch through the exact same recursive call, including a
 * box nested two levels deep inside another box.
 */
class ShipmentNodesCompositeTest {

    private static final PackagingSpec STD_BOX =
            new PackagingSpec("STD-BOX", "corrugated cardboard", 180, HazmatClass.NONE);
    private static final Country US = new Country("US", "United States", false);

    @Test
    void aLeafReportsItsOwnWeightAndValueWithNoChildrenToSum() {
        Parcel parcel = parcel("P-900", 500, 1_000);

        assertThat(ShipmentNodes.totalWeightGrams(parcel)).isEqualTo(500);
        assertThat(ShipmentNodes.totalValueCents(parcel)).isEqualTo(1_000);
        assertThat(ShipmentNodes.itemCount(parcel)).isEqualTo(1);
    }

    @Test
    void totalsRecurseThroughATwoLevelNestedBranch() {
        ShipmentBox innerCarton = new ShipmentBox("CTN-C", List.of(
                parcel("P-301", 2_400, 5_999),
                parcel("P-302", 2_400, 5_999)), 10_000);
        ShipmentBox outerCarton = new ShipmentBox("CTN-B", List.of(innerCarton), 10_000);
        ShipmentBox pallet = new ShipmentBox("PAL-9", List.of(outerCarton), 20_000);

        assertThat(ShipmentNodes.totalWeightGrams(pallet)).isEqualTo(4_800);
        assertThat(ShipmentNodes.totalValueCents(pallet)).isEqualTo(11_998);
        assertThat(ShipmentNodes.itemCount(pallet)).isEqualTo(2);
    }

    @Test
    void aMixedTreeSumsEveryBranchAndLeafUniformly() {
        ShipmentBox cartonA = new ShipmentBox("CTN-A", List.of(
                parcel("P-101", 3_200, 12_499),
                parcel("P-102", 3_200, 12_499)), 10_000);
        ShipmentBox innerCarton = new ShipmentBox("CTN-C", List.of(parcel("P-301", 2_400, 5_999)), 10_000);
        ShipmentBox cartonB = new ShipmentBox("CTN-B", List.of(innerCarton), 10_000);
        ShipmentBox pallet = new ShipmentBox("PAL-9", List.of(cartonA, cartonB), 20_000);

        assertThat(ShipmentNodes.totalWeightGrams(pallet)).isEqualTo(8_800);
        assertThat(ShipmentNodes.totalValueCents(pallet)).isEqualTo(30_997);
        assertThat(ShipmentNodes.itemCount(pallet)).isEqualTo(3);
    }

    private static Parcel parcel(String id, int weightGrams, long valueCents) {
        return new Parcel(id, weightGrams, valueCents, STD_BOX, US);
    }
}
