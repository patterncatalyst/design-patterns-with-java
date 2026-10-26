package com.patterncatalyst.designpatterns.ch21.shipping;

/**
 * Evaluates and renders a RateRule tree. quote() recurses through whichever
 * shape a caller built, with a guarded when clause deciding whether a
 * ZoneSurcharge applies at all. describe() goes one step further: its first
 * case deconstructs a MinimumCharge wrapping a ZoneSurcharge wrapping a
 * PerKilogram, three levels, in a single nested record pattern, rather than
 * binding each layer to a name and calling an accessor down through it.
 */
public final class RateRules {

    private RateRules() {
    }

    public static Money quote(RateRule rule, ParcelKind parcel, String destinationZone) {
        return switch (rule) {
            case FlatRate(var amount) -> amount;
            case PerKilogram(var ratePerKg) -> ratePerKg.timesWeight(ParcelKinds.billableWeightKg(parcel));
            case ZoneSurcharge(var base, var zone, var surcharge) when zone.equals(destinationZone) ->
                    quote(base, parcel, destinationZone).plus(surcharge);
            case ZoneSurcharge(var base, var zone, var surcharge) -> quote(base, parcel, destinationZone);
            case MinimumCharge(var base, var floor) -> {
                Money computed = quote(base, parcel, destinationZone);
                yield computed.isGreaterThan(floor) ? computed : floor;
            }
        };
    }

    public static String describe(RateRule rule) {
        return switch (rule) {
            case MinimumCharge(ZoneSurcharge(PerKilogram(var ratePerKg), var zone, var surcharge), var floor) ->
                    "max(%s/kg + %s in zone %s, floor %s)".formatted(ratePerKg, surcharge, zone, floor);
            case MinimumCharge(var base, var floor) -> "max(%s, floor %s)".formatted(describe(base), floor);
            case ZoneSurcharge(var base, var zone, var surcharge) ->
                    "%s + %s in zone %s".formatted(describe(base), surcharge, zone);
            case PerKilogram(var ratePerKg) -> "%s/kg".formatted(ratePerKg);
            case FlatRate(var amount) -> amount.toString();
        };
    }
}
