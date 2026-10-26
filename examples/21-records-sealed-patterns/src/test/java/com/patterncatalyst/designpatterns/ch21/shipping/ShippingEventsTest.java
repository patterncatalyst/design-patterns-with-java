package com.patterncatalyst.designpatterns.ch21.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ShippingEventsTest {

    @Test
    void describeDeconstructsARefrigeratedParcelTwoLevelsDeepInOneNestedPattern() {
        ShippingEvent event = new QuoteRequested(new RefrigeratedParcel(8.0, -5, 4), "EAST");

        assertThat(ShippingEvents.describe(event))
                .isEqualTo("Quote requested for a 8.0kg refrigerated parcel (keep -5C to 4C) to zone EAST");
    }

    @Test
    void describeFallsBackToTheGenericQuoteRequestedArmForOtherParcelKinds() {
        ShippingEvent event = new QuoteRequested(new StandardParcel(3.0), "WEST");

        assertThat(ShippingEvents.describe(event)).isEqualTo("Quote requested for a 3.0kg parcel to zone WEST");
    }

    @Test
    void describeHandlesEveryShippingEventKind() {
        ParcelKind parcel = new StandardParcel(2.0);

        assertThat(ShippingEvents.describe(new LabelPurchased(parcel, "SwiftShip", Money.ofCents(1_200))))
                .isEqualTo("Label purchased from SwiftShip for $12.00 (2.0kg)");
        assertThat(ShippingEvents.describe(new HubScan(parcel, "HUB-9")))
                .isEqualTo("Scanned at hub HUB-9 (2.0kg)");
        assertThat(ShippingEvents.describe(new DeliveryException(parcel, "address not found")))
                .isEqualTo("Delivery exception: address not found (2.0kg)");
        assertThat(ShippingEvents.describe(new Delivered(parcel))).isEqualTo("Delivered (2.0kg)");
    }

    @Test
    void requiresPriorityHandlingIsTrueOnlyForAColdChainBreachAtOrBelowTheCeiling() {
        ShippingEvent breach = new DeliveryException(new RefrigeratedParcel(8.0, -18, 0), "unit reads 9C");

        assertThat(ShippingEvents.requiresPriorityHandling(breach)).isTrue();
    }

    @Test
    void requiresPriorityHandlingIsFalseForARefrigeratedParcelAboveTheCeiling() {
        ShippingEvent mildException = new DeliveryException(new RefrigeratedParcel(8.0, 2, 10), "delayed one day");

        assertThat(ShippingEvents.requiresPriorityHandling(mildException)).isFalse();
    }

    @Test
    void requiresPriorityHandlingIsFalseForADeliveryExceptionOnANonRefrigeratedParcel() {
        ShippingEvent routine = new DeliveryException(new StandardParcel(3.0), "address not found");

        assertThat(ShippingEvents.requiresPriorityHandling(routine)).isFalse();
    }

    @Test
    void requiresPriorityHandlingIsFalseForEveryNonExceptionEvent() {
        ParcelKind parcel = new RefrigeratedParcel(8.0, -18, 0);

        assertThat(ShippingEvents.requiresPriorityHandling(new QuoteRequested(parcel, "EAST"))).isFalse();
        assertThat(ShippingEvents.requiresPriorityHandling(new Delivered(parcel))).isFalse();
    }
}
