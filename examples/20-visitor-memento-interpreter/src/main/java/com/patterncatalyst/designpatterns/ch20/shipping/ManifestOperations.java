package com.patterncatalyst.designpatterns.ch20.shipping;

import java.util.ArrayList;
import java.util.List;

/**
 * Three operations over the fixed ManifestEntry hierarchy, each one a
 * static method dispatching on an exhaustive pattern-matching switch. None
 * of the four ManifestEntry implementations declares an accept() method or
 * implements a visitor interface; adding shippingCost(), customsSummary(),
 * and validate() in turn never touched StandardParcel, InternationalParcel,
 * HazardousParcel, or OversizedParcel. Each switch has no default branch,
 * so a fifth ManifestEntry kind added later fails this file at compile
 * time until every switch here accounts for it.
 */
public final class ManifestOperations {

    private static final long BASE_RATE_CENTS_PER_KG = 50;
    private static final long CUSTOMS_BROKERAGE_FLAT_CENTS = 1_500;
    private static final long HAZMAT_HANDLING_CENTS_PER_KG = 200;
    private static final long DIMENSIONAL_SURCHARGE_CENTS_PER_CUBIC_METER = 300;

    private ManifestOperations() {
    }

    /** Operation one: what a carrier charges to move this entry. */
    public static Money shippingCost(ManifestEntry entry) {
        return switch (entry) {
            case StandardParcel p -> baseRate(p.weightKg());
            case InternationalParcel p -> baseRate(p.weightKg()).plus(Money.ofCents(CUSTOMS_BROKERAGE_FLAT_CENTS));
            case HazardousParcel p ->
                    baseRate(p.weightKg()).plus(Money.ofCents(Math.round(p.weightKg() * HAZMAT_HANDLING_CENTS_PER_KG)));
            case OversizedParcel p -> baseRate(p.weightKg()).plus(dimensionalSurcharge(p));
        };
    }

    /** Operation two: what a customs or routing desk needs to know about this entry. */
    public static String customsSummary(ManifestEntry entry) {
        return switch (entry) {
            case StandardParcel p -> "%s: domestic parcel, no customs filing required".formatted(p.id());
            case InternationalParcel p -> "%s: customs code %s, origin %s, brokerage required"
                    .formatted(p.id(), p.customsCode(), p.originCountry());
            case HazardousParcel p -> "%s: hazmat class %s (UN%s), placard required, domestic handling only"
                    .formatted(p.id(), p.hazardClass(), p.unNumber());
            case OversizedParcel p -> "%s: oversized parcel, no customs filing, freight-desk routing required"
                    .formatted(p.id());
        };
    }

    /**
     * Operation three: every reason this entry cannot be submitted, collected
     * in one pass rather than thrown one at a time. A compact record
     * constructor could reject a blank customs code the moment one is
     * constructed, but it can only ever report the first problem it finds;
     * a merchant fixing a draft manifest wants the whole list at once.
     */
    public static List<String> validate(ManifestEntry entry) {
        List<String> issues = new ArrayList<>();
        if (entry.weightKg() <= 0.0) {
            issues.add("%s: weight must be positive, was %.2fkg".formatted(entry.id(), entry.weightKg()));
        }
        switch (entry) {
            case StandardParcel ignored -> { }
            case InternationalParcel p -> {
                if (p.customsCode().isBlank()) {
                    issues.add("%s: customs code is required".formatted(p.id()));
                }
                if (p.originCountry().isBlank()) {
                    issues.add("%s: origin country is required".formatted(p.id()));
                }
            }
            case HazardousParcel p -> {
                if (p.hazardClass().isBlank()) {
                    issues.add("%s: hazard class is required".formatted(p.id()));
                }
                if (p.unNumber().isBlank()) {
                    issues.add("%s: UN number is required".formatted(p.id()));
                }
            }
            case OversizedParcel p -> {
                if (p.lengthCm() <= 0.0 || p.widthCm() <= 0.0 || p.heightCm() <= 0.0) {
                    issues.add("%s: all dimensions must be positive".formatted(p.id()));
                }
            }
        }
        return List.copyOf(issues);
    }

    private static Money baseRate(double weightKg) {
        return Money.ofCents(Math.round(weightKg * BASE_RATE_CENTS_PER_KG));
    }

    private static Money dimensionalSurcharge(OversizedParcel parcel) {
        double cubicMeters = (parcel.lengthCm() * parcel.widthCm() * parcel.heightCm()) / 1_000_000.0;
        return Money.ofCents(Math.round(cubicMeters * DIMENSIONAL_SURCHARGE_CENTS_PER_CUBIC_METER));
    }
}
