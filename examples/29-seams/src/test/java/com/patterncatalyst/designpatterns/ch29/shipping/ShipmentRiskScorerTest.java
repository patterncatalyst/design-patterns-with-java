package com.patterncatalyst.designpatterns.ch29.shipping;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ShipmentRiskScorerTest {

    private static final Shipment LIGHT =
            new Shipment("SHM-4001", Instant.now().plus(1, ChronoUnit.DAYS), 10.0);
    private static final Shipment HEAVY =
            new Shipment("SHM-4002", Instant.now().plus(1, ChronoUnit.DAYS), 150.0);

    @Test
    void appliesNoSurchargeOutsidePeakSeason() {
        ShipmentRiskScorer scorer = new TestableShipmentRiskScorer(false);

        assertThat(scorer.score(LIGHT)).isEqualTo(5);
        assertThat(scorer.score(HEAVY)).isEqualTo(40);
    }

    @Test
    void appliesTheSurchargeDuringPeakSeason() {
        ShipmentRiskScorer scorer = new TestableShipmentRiskScorer(true);

        assertThat(scorer.score(LIGHT)).isEqualTo(20);
        assertThat(scorer.score(HEAVY)).isEqualTo(55);
    }

    @Test
    void productionWiringStillReadsTheRealEnterpriseCalendar() {
        ShipmentRiskScorer scorer = new ShipmentRiskScorer();

        int expected = EnterpriseCalendar.isPeakSeason() ? 20 : 5;
        assertThat(scorer.score(LIGHT)).isEqualTo(expected);
    }

    /**
     * The subclass-and-override seam: no constructor parameter, no
     * extracted interface, just one protected method overridden by a test
     * subclass so both branches of isHighRiskWindow() become reachable on
     * demand instead of waiting for November.
     */
    private static final class TestableShipmentRiskScorer extends ShipmentRiskScorer {
        private final boolean peakSeason;

        TestableShipmentRiskScorer(boolean peakSeason) {
            this.peakSeason = peakSeason;
        }

        @Override
        protected boolean isHighRiskWindow() {
            return peakSeason;
        }
    }
}
