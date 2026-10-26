package com.patterncatalyst.designpatterns.ch15.shipping;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class CustomsDeclarationServiceTest {

    private final CustomsDeclarationService service = new CustomsDeclarationService();

    @Test
    void declaresAnInternationalShipmentAtItsFullDeclaredValue() {
        Shipment shipment = new Shipment(1.0, DestinationZone.INTERNATIONAL, 45_000);

        CustomsDeclaration declaration = service.declareIfNeeded(shipment).orElseThrow();

        assertThat(declaration.declaredValue()).isEqualTo(Money.ofCents(45_000));
        assertThat(declaration.note()).contains("$450.00");
    }

    @ParameterizedTest
    @EnumSource(value = DestinationZone.class, names = "INTERNATIONAL", mode = EnumSource.Mode.EXCLUDE)
    void leavesEveryNonInternationalShipmentWithoutADeclaration(DestinationZone zone) {
        Shipment shipment = new Shipment(1.0, zone, 45_000);

        assertThat(service.declareIfNeeded(shipment)).isEmpty();
    }
}
