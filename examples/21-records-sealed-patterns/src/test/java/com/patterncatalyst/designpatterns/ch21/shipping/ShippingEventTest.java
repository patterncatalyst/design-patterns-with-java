package com.patterncatalyst.designpatterns.ch21.shipping;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class ShippingEventTest {

    private static final ParcelKind PARCEL = new StandardParcel(3.0);

    @Test
    void quoteRequestedRejectsABlankDestinationZone() {
        assertThatIllegalArgumentException().isThrownBy(() -> new QuoteRequested(PARCEL, ""));
        assertThatNullPointerException().isThrownBy(() -> new QuoteRequested(null, "EAST"));
    }

    @Test
    void labelPurchasedRejectsABlankCarrier() {
        assertThatIllegalArgumentException().isThrownBy(() -> new LabelPurchased(PARCEL, " ", Money.ofCents(500)));
        assertThatNullPointerException().isThrownBy(() -> new LabelPurchased(PARCEL, "SwiftShip", null));
    }

    @Test
    void hubScanRejectsABlankHubCode() {
        assertThatIllegalArgumentException().isThrownBy(() -> new HubScan(PARCEL, ""));
    }

    @Test
    void deliveryExceptionRejectsABlankReason() {
        assertThatIllegalArgumentException().isThrownBy(() -> new DeliveryException(PARCEL, ""));
    }

    @Test
    void deliveredRejectsANullParcel() {
        assertThatNullPointerException().isThrownBy(() -> new Delivered(null));
    }
}
