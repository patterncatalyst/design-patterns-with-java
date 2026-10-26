package com.patterncatalyst.designpatterns.ch15.shipping;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

/**
 * Verifies the Facade's two real jobs: handing back one consistent manifest,
 * and calling its four subsystems in the order the manifest's own fields
 * depend on, not just calling all four of them somehow.
 */
@ExtendWith(MockitoExtension.class)
class ShippingFacadeTest {

    @Mock
    private CarrierGateway carrierGateway;

    @Mock
    private LabelPrinter labelPrinter;

    @Mock
    private CustomsDeclarationService customsDeclarationService;

    @Mock
    private TrackingRegistry trackingRegistry;

    @Test
    void ordersSubsystemCallsSoTheLabelAndUrlSeeAnAlreadyIssuedTrackingNumber() {
        Shipment shipment = new Shipment(3.5, DestinationZone.REGIONAL, 12_000);
        when(trackingRegistry.issueTrackingNumber(1)).thenReturn("TRK-1001");
        when(carrierGateway.quote(shipment)).thenReturn(Money.ofCents(650));
        when(labelPrinter.render(shipment, "TRK-1001")).thenReturn("LABEL tracking=TRK-1001");
        when(trackingRegistry.trackingUrl("TRK-1001")).thenReturn("https://track.example/TRK-1001");
        when(customsDeclarationService.declareIfNeeded(shipment)).thenReturn(Optional.empty());
        ShippingFacade facade =
                new ShippingFacade(carrierGateway, labelPrinter, customsDeclarationService, trackingRegistry);

        ShipmentManifest manifest = facade.prepareShipment(shipment, 1);

        assertThat(manifest.quote()).isEqualTo(Money.ofCents(650));
        assertThat(manifest.label()).isEqualTo("LABEL tracking=TRK-1001");
        assertThat(manifest.trackingNumber()).isEqualTo("TRK-1001");
        assertThat(manifest.trackingUrl()).isEqualTo("https://track.example/TRK-1001");
        assertThat(manifest.customsDeclaration()).isEmpty();

        InOrder order = inOrder(trackingRegistry, carrierGateway, labelPrinter);
        order.verify(trackingRegistry).issueTrackingNumber(1);
        order.verify(carrierGateway).quote(shipment);
        order.verify(labelPrinter).render(shipment, "TRK-1001");
        order.verify(trackingRegistry).trackingUrl("TRK-1001");
    }

    @Test
    void endToEndManifestIsConsistentForADomesticShipment() {
        Shipment shipment = new Shipment(2.0, DestinationZone.NATIONAL, 5_000);
        CarrierGateway realGateway = ignored -> Money.ofCents(900);
        ShippingFacade facade = new ShippingFacade(
                realGateway, new LabelPrinter("GlobalCourier"), new CustomsDeclarationService(),
                new TrackingRegistry("globalcourier.example"));

        ShipmentManifest manifest = facade.prepareShipment(shipment, 7);

        assertThat(manifest.trackingNumber()).isEqualTo("TRK-1007");
        assertThat(manifest.label()).contains(manifest.trackingNumber());
        assertThat(manifest.trackingUrl()).endsWith(manifest.trackingNumber());
        assertThat(manifest.customsDeclaration()).isEmpty();
    }

    @Test
    void endToEndManifestIncludesACustomsDeclarationForAnInternationalShipment() {
        Shipment shipment = new Shipment(2.0, DestinationZone.INTERNATIONAL, 45_000);
        CarrierGateway realGateway = ignored -> Money.ofCents(900);
        ShippingFacade facade = new ShippingFacade(
                realGateway, new LabelPrinter("GlobalCourier"), new CustomsDeclarationService(),
                new TrackingRegistry("globalcourier.example"));

        ShipmentManifest manifest = facade.prepareShipment(shipment, 9);

        assertThat(manifest.customsDeclaration()).isPresent();
        assertThat(manifest.customsDeclaration().orElseThrow().declaredValue()).isEqualTo(Money.ofCents(45_000));
    }
}
