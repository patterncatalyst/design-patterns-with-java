package com.patterncatalyst.designpatterns.ch17.shipping;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link ShipmentBox#withAlertHandler} accepts an {@link Optional} at a
 * construction boundary and resolves it immediately into a concrete
 * handler, so {@code alertHandler} itself is never an Optional field and
 * {@link ShipmentBox#checkWeightLimit()} still has nothing to branch on.
 */
@ExtendWith(MockitoExtension.class)
class ShipmentBoxOptionalBoundaryTest {

    private static final PackagingSpec STD_BOX =
            new PackagingSpec("STD-BOX", "corrugated cardboard", 180, HazmatClass.NONE);
    private static final Country US = new Country("US", "United States", false);

    @Mock
    private ShipmentAlertHandler handler;

    @Test
    void anEmptyOptionalResolvesToTheSameNullObjectAtConstruction() {
        ShipmentBox box = ShipmentBox.withAlertHandler("CTN-A", List.of(
                new Parcel("P-101", 3_200, 12_499, STD_BOX, US)), 500, Optional.empty());

        assertThatCode(box::checkWeightLimit).doesNotThrowAnyException();
    }

    @Test
    void aPresentOptionalBindsTheRealHandler() {
        ShipmentBox box = ShipmentBox.withAlertHandler("CTN-A", List.of(
                new Parcel("P-101", 3_200, 12_499, STD_BOX, US)), 500, Optional.of(handler));

        box.checkWeightLimit();

        verify(handler).onOverweight("CTN-A", 2_700L);
    }
}
