package com.patterncatalyst.designpatterns.ch17.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link ShipmentBox#checkWeightLimit()} calls {@code alertHandler}
 * unconditionally. These tests prove both halves of that claim: the default
 * {@link NullShipmentAlertHandler} is a safe no-op, and a real, injected
 * handler is reached without any null check standing between it and the
 * call site.
 */
@ExtendWith(MockitoExtension.class)
class ShipmentBoxNullObjectTest {

    private static final PackagingSpec STD_BOX =
            new PackagingSpec("STD-BOX", "corrugated cardboard", 180, HazmatClass.NONE);
    private static final Country US = new Country("US", "United States", false);

    @Mock
    private ShipmentAlertHandler handler;

    @Test
    void theDefaultHandlerIsASafeNoOpWithNoNullCheckAtTheCallSite() {
        ShipmentBox box = new ShipmentBox("CTN-A", List.of(
                new Parcel("P-101", 3_200, 12_499, STD_BOX, US),
                new Parcel("P-102", 3_200, 12_499, STD_BOX, US)), 500);

        assertThatCode(box::checkWeightLimit).doesNotThrowAnyException();
    }

    @Test
    void aRealHandlerIsCalledUnconditionallyWhenOneIsBoundAndTheBoxIsOverweight() {
        ShipmentBox box = new ShipmentBox("CTN-A", List.of(
                new Parcel("P-101", 3_200, 12_499, STD_BOX, US),
                new Parcel("P-102", 3_200, 12_499, STD_BOX, US)), 500, handler);

        box.checkWeightLimit();

        verify(handler).onOverweight("CTN-A", 5_900L);
    }

    @Test
    void aRealHandlerIsNeverCalledWhenNothingIsOverweight() {
        ShipmentBox box = new ShipmentBox("CTN-A", List.of(
                new Parcel("P-101", 100, 999, STD_BOX, US)), 500, handler);

        box.checkWeightLimit();

        verifyNoInteractions(handler);
    }

    @Test
    void theNullObjectIsTheSameEnumSingletonEveryTime() {
        assertThat(NullShipmentAlertHandler.INSTANCE).isSameAs(NullShipmentAlertHandler.valueOf("INSTANCE"));
    }
}
