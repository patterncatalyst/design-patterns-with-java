package com.patterncatalyst.designpatterns.ch15.shipping;

import com.patterncatalyst.designpatterns.ch15.shipping.globalcourier.GlobalCourierRateClient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that the Adapter translates this app's units into GlobalCourier's
 * legacy ones on every call, not just that it returns something.
 */
@ExtendWith(MockitoExtension.class)
class GlobalCourierGatewayAdapterTest {

    @Mock
    private GlobalCourierRateClient legacyClient;

    @Test
    void convertsKilogramsToGramsBeforeCallingTheLegacyClient() {
        when(legacyClient.rateCentsForParcel(3_500L, 1)).thenReturn(650L);
        GlobalCourierGatewayAdapter adapter = new GlobalCourierGatewayAdapter(legacyClient);
        Shipment shipment = new Shipment(3.5, DestinationZone.REGIONAL, 0);

        Money quote = adapter.quote(shipment);

        assertThat(quote).isEqualTo(Money.ofCents(650));
        verify(legacyClient).rateCentsForParcel(3_500L, 1);
    }

    @Test
    void roundsAFractionalKilogramWeightToTheNearestGram() {
        when(legacyClient.rateCentsForParcel(1_235L, 0)).thenReturn(200L);
        GlobalCourierGatewayAdapter adapter = new GlobalCourierGatewayAdapter(legacyClient);
        Shipment shipment = new Shipment(1.2345, DestinationZone.LOCAL, 0);

        adapter.quote(shipment);

        verify(legacyClient).rateCentsForParcel(1_235L, 0);
    }

    @ParameterizedTest
    @CsvSource({
        "LOCAL, 0",
        "REGIONAL, 1",
        "NATIONAL, 2",
        "INTERNATIONAL, 3",
    })
    void mapsEveryDestinationZoneToTheLegacyNumericCode(DestinationZone zone, int expectedZoneCode) {
        when(legacyClient.rateCentsForParcel(1_000L, expectedZoneCode)).thenReturn(300L);
        GlobalCourierGatewayAdapter adapter = new GlobalCourierGatewayAdapter(legacyClient);
        Shipment shipment = new Shipment(1.0, zone, 0);

        adapter.quote(shipment);

        verify(legacyClient).rateCentsForParcel(1_000L, expectedZoneCode);
    }
}
