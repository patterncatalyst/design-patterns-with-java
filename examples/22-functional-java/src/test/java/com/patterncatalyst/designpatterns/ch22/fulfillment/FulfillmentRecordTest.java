package com.patterncatalyst.designpatterns.ch22.fulfillment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * {@code manifestId} and {@code rejectionReason} are plain {@code String}
 * fields rather than {@code Optional<String>} fields; this checks the
 * "empty, never missing" convention each factory method relies on.
 */
class FulfillmentRecordTest {

    private static final Shipment SHIPMENT =
            new Shipment("RETAIL-1", 1.0, DestinationZone.LOCAL, PackagingTier.STANDARD, 1_000);

    @Test
    void acceptedCarriesAManifestIdAndNoRejectionReason() {
        FulfillmentRecord record = FulfillmentRecord.accepted("MANIFEST-000001", SHIPMENT, Money.ofCents(300));

        assertThat(record.accepted()).isTrue();
        assertThat(record.manifestId()).isEqualTo("MANIFEST-000001");
        assertThat(record.rejectionReason()).isEmpty();
    }

    @Test
    void rejectedCarriesAReasonAndNoManifestId() {
        FulfillmentRecord record = FulfillmentRecord.rejected(SHIPMENT, "over the declared value cap");

        assertThat(record.accepted()).isFalse();
        assertThat(record.manifestId()).isEmpty();
        assertThat(record.total()).isEqualTo(Money.ZERO);
        assertThat(record.rejectionReason()).isEqualTo("over the declared value cap");
    }
}
