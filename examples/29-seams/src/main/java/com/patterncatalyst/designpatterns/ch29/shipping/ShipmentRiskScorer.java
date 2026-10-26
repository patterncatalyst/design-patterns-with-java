package com.patterncatalyst.designpatterns.ch29.shipping;

/**
 * {@link LegacyShipmentRiskScorer} refactored through a subclass-and-override
 * seam. There is no constructor parameter here and no extracted interface;
 * the one hard dependency, {@link EnterpriseCalendar#isPeakSeason()}, moves
 * into its own protected method that production code never overrides. A
 * test substitutes an answer by extending this class and overriding
 * {@link #isHighRiskWindow()}, which needs no change to the public
 * {@link #score(Shipment)} contract and no change to how callers construct
 * this class.
 */
public class ShipmentRiskScorer {

    private static final int PEAK_SEASON_SURCHARGE = 15;

    public int score(Shipment shipment) {
        int score = baseRiskScore(shipment);
        if (isHighRiskWindow()) {
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

    /**
     * The subclass-and-override seam. Production code always falls through
     * to the real enterprise calendar; a test subclass overrides this one
     * method to force either branch on demand.
     */
    protected boolean isHighRiskWindow() {
        return EnterpriseCalendar.isPeakSeason();
    }
}
