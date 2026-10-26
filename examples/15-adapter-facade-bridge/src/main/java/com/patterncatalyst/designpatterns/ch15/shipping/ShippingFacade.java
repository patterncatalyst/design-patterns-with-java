package com.patterncatalyst.designpatterns.ch15.shipping;

import java.util.Objects;
import java.util.Optional;

/**
 * The Facade. It owns a {@link CarrierGateway}, a {@link LabelPrinter}, a
 * {@link CustomsDeclarationService}, and a {@link TrackingRegistry}, and it
 * exposes exactly one method that matters to a caller: {@link
 * #prepareShipment(Shipment, int)}.
 *
 * <p>The sequencing inside that method is the actual value this Facade
 * delivers, not the four subsystem calls on their own. A tracking number has
 * to exist before the label can print it and before the tracking URL can
 * point at it. A checkout handler calling the four subsystems directly would
 * have to get that order right every time it wrote this logic, in every
 * place it wrote this logic; this Facade gets it right once, and every
 * caller inherits that correctness instead of re-deriving it.
 */
public final class ShippingFacade {

    private final CarrierGateway carrierGateway;
    private final LabelPrinter labelPrinter;
    private final CustomsDeclarationService customsDeclarationService;
    private final TrackingRegistry trackingRegistry;

    public ShippingFacade(
            CarrierGateway carrierGateway,
            LabelPrinter labelPrinter,
            CustomsDeclarationService customsDeclarationService,
            TrackingRegistry trackingRegistry) {
        this.carrierGateway = Objects.requireNonNull(carrierGateway, "carrierGateway");
        this.labelPrinter = Objects.requireNonNull(labelPrinter, "labelPrinter");
        this.customsDeclarationService =
                Objects.requireNonNull(customsDeclarationService, "customsDeclarationService");
        this.trackingRegistry = Objects.requireNonNull(trackingRegistry, "trackingRegistry");
    }

    public ShipmentManifest prepareShipment(Shipment shipment, int sequence) {
        Objects.requireNonNull(shipment, "shipment");
        String trackingNumber = trackingRegistry.issueTrackingNumber(sequence);
        Money quote = carrierGateway.quote(shipment);
        String label = labelPrinter.render(shipment, trackingNumber);
        String trackingUrl = trackingRegistry.trackingUrl(trackingNumber);
        Optional<CustomsDeclaration> customsDeclaration = customsDeclarationService.declareIfNeeded(shipment);
        return new ShipmentManifest(quote, label, trackingNumber, trackingUrl, customsDeclaration);
    }
}
