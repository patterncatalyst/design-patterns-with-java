package com.patterncatalyst.designpatterns.ch29.shipping;

/**
 * Inherited exactly as found. {@link #score(Shipment)} reaches directly into
 * {@link EnterpriseCalendar#isPeakSeason()}, a global, process-wide
 * dependency with no object anywhere for a test to inject a substitute into.
 * This class stays unedited as the historical record the characterization
 * tests pin; {@link ShipmentRiskScorer} is the refactored version with a
 * subclass-and-override seam opened around the one call site.
 */
public class LegacyShipmentRiskScorer {

    private static final int PEAK_SEASON_SURCHARGE = 15;

    public int score(Shipment shipment) {
        int score = baseRiskScore(shipment);
        if (EnterpriseCalendar.isPeakSeason()) {
            score += PEAK_SEASON_SURCHARGE;
        }
        return score;
    }

    protected int baseRiskScore(Shipment shipment) {
        if (shipment.weightKg() > 100.0) {
            return 40;
        }
        if (shipment.weightKg() > 25.0) {
            return 20;
        }
        return 5;
    }
}
