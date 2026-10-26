package com.patterncatalyst.designpatterns.ch21.shipping;

/**
 * Operations over the closed ShippingEvent set, each its own exhaustive
 * pattern-matching switch. describe() deconstructs a QuoteRequested whose
 * parcel is a RefrigeratedParcel two levels deep, in a single nested record
 * pattern; requiresPriorityHandling() binds a RefrigeratedParcel by type and
 * narrows it further with a guarded when clause. Neither ShippingEvent nor
 * any of its five implementing records declares a method named here.
 */
public final class ShippingEvents {

    private static final int COLD_CHAIN_CEILING_C = 4;

    private ShippingEvents() {
    }

    public static String describe(ShippingEvent event) {
        return switch (event) {
            case QuoteRequested(RefrigeratedParcel(var weightKg, var minTempC, var maxTempC), var zone) ->
                    "Quote requested for a %.1fkg refrigerated parcel (keep %dC to %dC) to zone %s"
                            .formatted(weightKg, minTempC, maxTempC, zone);
            case QuoteRequested(var parcel, var zone) ->
                    "Quote requested for a %.1fkg parcel to zone %s".formatted(parcel.weightKg(), zone);
            case LabelPurchased(var parcel, var carrier, var cost) ->
                    "Label purchased from %s for %s (%.1fkg)".formatted(carrier, cost, parcel.weightKg());
            case HubScan(var parcel, var hubCode) ->
                    "Scanned at hub %s (%.1fkg)".formatted(hubCode, parcel.weightKg());
            case DeliveryException(var parcel, var reason) ->
                    "Delivery exception: %s (%.1fkg)".formatted(reason, parcel.weightKg());
            case Delivered(var parcel) -> "Delivered (%.1fkg)".formatted(parcel.weightKg());
        };
    }

    /**
     * True only for a delivery exception on a refrigerated parcel whose
     * ceiling is at or below the cold-chain threshold; every other event,
     * including a delivery exception on a parcel of any other kind, is not
     * urgent enough to page a dispatcher outside business hours.
     */
    public static boolean requiresPriorityHandling(ShippingEvent event) {
        return switch (event) {
            case DeliveryException(RefrigeratedParcel parcel, var reason) when parcel.maxTempC() <= COLD_CHAIN_CEILING_C ->
                    true;
            case DeliveryException e -> false;
            case QuoteRequested q -> false;
            case LabelPurchased l -> false;
            case HubScan h -> false;
            case Delivered d -> false;
        };
    }
}
