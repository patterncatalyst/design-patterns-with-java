package com.patterncatalyst.designpatterns.ch15.shipping;

import com.patterncatalyst.designpatterns.ch15.shipping.globalcourier.GlobalCourierRateClient;

import java.time.Duration;

/**
 * Exercises the Adapter, the Facade, and the Bridge against one
 * GlobalCourier integration and prints what each one produces.
 */
public final class ShippingDemo {

    private ShippingDemo() {}

    public static void main(String[] args) {
        System.out.println("== Adapter: GlobalCourier's legacy SDK behind CarrierGateway ==");
        GlobalCourierRateClient legacyClient = new GlobalCourierRateClient(150);
        CarrierGateway carrierGateway = new GlobalCourierGatewayAdapter(legacyClient);
        Shipment domesticShipment = new Shipment(3.5, DestinationZone.REGIONAL, 12_000);
        System.out.println("Adapted quote: " + carrierGateway.quote(domesticShipment));

        System.out.println();
        System.out.println("== Facade: one call fronts rating, labeling, customs, and tracking ==");
        ShippingFacade facade = new ShippingFacade(
                carrierGateway, new LabelPrinter("GlobalCourier"), new CustomsDeclarationService(),
                new TrackingRegistry("globalcourier.example"));
        ShipmentManifest domesticManifest = facade.prepareShipment(domesticShipment, 1);
        System.out.println("quote:    " + domesticManifest.quote());
        System.out.println("label:    " + domesticManifest.label());
        System.out.println("tracking: " + domesticManifest.trackingUrl());
        System.out.println("customs:  " + domesticManifest.customsDeclaration()
                .map(CustomsDeclaration::note)
                .orElse("not required for this zone"));

        Shipment internationalShipment = new Shipment(2.0, DestinationZone.INTERNATIONAL, 45_000);
        ShipmentManifest internationalManifest = facade.prepareShipment(internationalShipment, 2);
        System.out.println("customs:  " + internationalManifest.customsDeclaration()
                .map(CustomsDeclaration::note)
                .orElse("not required for this zone"));

        System.out.println();
        System.out.println("== Bridge: two notification kinds, two channels, no class explosion ==");
        DeliveryChannel email = new EmailDeliveryChannel();
        DeliveryChannel sms = new SmsDeliveryChannel();
        Notification createdByEmail = new ShipmentCreatedNotification(email, domesticManifest.trackingNumber());
        Notification createdBySms = new ShipmentCreatedNotification(sms, domesticManifest.trackingNumber());
        Notification delayedByEmail =
                new DeliveryDelayedNotification(email, domesticManifest.trackingNumber(), Duration.ofHours(6));
        Notification delayedBySms =
                new DeliveryDelayedNotification(sms, domesticManifest.trackingNumber(), Duration.ofHours(6));

        System.out.println(createdByEmail.send("customer@example.com"));
        System.out.println(createdBySms.send("+1-555-0100"));
        System.out.println(delayedByEmail.send("customer@example.com"));
        System.out.println(delayedBySms.send("+1-555-0100"));
    }
}
