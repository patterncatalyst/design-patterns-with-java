package com.patterncatalyst.designpatterns.ch13.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.Test;

class ShipmentBuilderTest {

    @Test
    void buildsAValidDomesticShipmentWithDefaults() {
        Shipment shipment = Shipment.builder()
                .trackingId("SHIP-1001")
                .carrier("Northline Freight")
                .destinationCountry("US")
                .build();

        assertThat(shipment.trackingId()).isEqualTo("SHIP-1001");
        assertThat(shipment.priority()).isEqualTo(ShippingPriority.STANDARD);
        assertThat(shipment.insuredValueCents()).isZero();
        assertThat(shipment.signatureRequired()).isFalse();
        assertThat(shipment.customsDeclaration()).isEmpty();
        assertThat(shipment.isInternational()).isFalse();
    }

    @Test
    void buildsAValidInternationalShipmentWithACustomsDeclaration() {
        CustomsDeclaration declaration = new CustomsDeclaration("8517.12", "US", "Wireless router", 24_900L);

        Shipment shipment = Shipment.builder()
                .trackingId("SHIP-1002")
                .carrier("Northline Freight")
                .destinationCountry("CA")
                .customsDeclaration(declaration)
                .priority(ShippingPriority.OVERNIGHT)
                .insuredValueCents(30_000L)
                .signatureRequired(true)
                .build();

        assertThat(shipment.isInternational()).isTrue();
        assertThat(shipment.customsDeclaration()).contains(declaration);
        assertThat(shipment.priority()).isEqualTo(ShippingPriority.OVERNIGHT);
        assertThat(shipment.signatureRequired()).isTrue();
    }

    @Test
    void rejectsAnInternationalShipmentWithNoCustomsDeclaration() {
        Shipment.Builder builder = Shipment.builder()
                .trackingId("SHIP-1003")
                .carrier("Northline Freight")
                .destinationCountry("CA");

        assertThatIllegalStateException()
                .isThrownBy(builder::build)
                .withMessageContaining("customsDeclaration is required");
    }

    @Test
    void rejectsAShipmentMissingARequiredField() {
        Shipment.Builder builder = Shipment.builder()
                .carrier("Northline Freight")
                .destinationCountry("US");

        assertThatIllegalStateException()
                .isThrownBy(builder::build)
                .withMessageContaining("trackingId is required");
    }

    @Test
    void aBuilderIsReusableForASecondShipmentAfterTheFirstIsBuilt() {
        Shipment.Builder builder = Shipment.builder()
                .trackingId("SHIP-1004")
                .carrier("Northline Freight")
                .destinationCountry("US");

        Shipment first = builder.build();
        Shipment second = builder.trackingId("SHIP-1005").build();

        assertThat(first.trackingId()).isEqualTo("SHIP-1004");
        assertThat(second.trackingId()).isEqualTo("SHIP-1005");
    }
}
