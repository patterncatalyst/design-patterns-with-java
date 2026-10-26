package com.patterncatalyst.designpatterns.ch26.shipping;

/**
 * Exercises both dispatch shortcuts against a short sequence of events and
 * prints each routed note, so the two mechanisms can be compared side by
 * side rather than only read about.
 */
public final class ShortcutDispatchDemo {

    private ShortcutDispatchDemo() {
    }

    public static void main(String[] args) {
        System.out.println("== Form one: EnumMap built from an exhaustive switch ==");
        EnumDispatchRouter enumRouter = new EnumDispatchRouter();
        InMemoryShipmentLedger enumLedger = new InMemoryShipmentLedger();
        enumRouter.route(new ShipmentEvent(ShipmentEventType.CREATED, "SH-1001", "order placed with FastFreight"), enumLedger);
        enumRouter.route(new ShipmentEvent(ShipmentEventType.PICKED_UP, "SH-1001", "scanned at origin depot"), enumLedger);
        enumRouter.route(new ShipmentEvent(ShipmentEventType.DELIVERED, "SH-1001", "left on porch"), enumLedger);
        enumLedger.entries().forEach(entry -> System.out.println("  " + entry));

        System.out.println();
        System.out.println("== Form two: Map<String, handler>, open and extensible ==");
        CarrierWebhookRouter webhookRouter = new CarrierWebhookRouter();
        InMemoryShipmentLedger webhookLedger = new InMemoryShipmentLedger();
        System.out.println("Built-in webhook codes registered: " + webhookRouter.registeredCodeCount());
        webhookRouter.register("customs_hold", (event, ledger) ->
                ledger.record(event.trackingId(), "customs-hold: " + event.detail()));
        System.out.println("Webhook codes registered after onboarding: " + webhookRouter.registeredCodeCount());

        webhookRouter.route(new CarrierWebhookEvent("created", "SH-2002", "order placed with RegionalParcel"), webhookLedger);
        webhookRouter.route(new CarrierWebhookEvent("in_transit", "SH-2002", "departed regional hub"), webhookLedger);
        webhookRouter.route(new CarrierWebhookEvent("customs_hold", "SH-2002", "awaiting duty payment"), webhookLedger);
        webhookRouter.route(new CarrierWebhookEvent("unknown_event", "SH-2002", "raw message ignored"), webhookLedger);
        webhookLedger.entries().forEach(entry -> System.out.println("  " + entry));
    }
}
