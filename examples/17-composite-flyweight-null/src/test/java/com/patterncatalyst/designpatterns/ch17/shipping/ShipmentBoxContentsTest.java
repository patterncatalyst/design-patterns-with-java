package com.patterncatalyst.designpatterns.ch17.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShipmentBoxContentsTest {

    private static final PackagingSpec STD_BOX =
            new PackagingSpec("STD-BOX", "corrugated cardboard", 180, HazmatClass.NONE);
    private static final Country US = new Country("US", "United States", false);

    @Test
    void copiesContentsDefensivelySoTheCallersListCannotMutateTheTree() {
        List<ShipmentNode> mutable = new ArrayList<>();
        mutable.add(new Parcel("P-1", 100, 100, STD_BOX, US));

        ShipmentBox box = new ShipmentBox("CTN-A", mutable, 1_000);
        mutable.add(new Parcel("P-2", 100, 100, STD_BOX, US));

        assertThat(box.contents()).hasSize(1);
    }

    @Test
    void rejectsANegativeWeightLimit() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ShipmentBox("CTN-A", List.of(), -1));
    }
}
