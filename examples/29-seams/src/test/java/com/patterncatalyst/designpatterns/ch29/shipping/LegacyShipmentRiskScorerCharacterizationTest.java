package com.patterncatalyst.designpatterns.ch29.shipping;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Characterization tests for LegacyShipmentRiskScorer. The base risk
 * calculation pins cleanly, but the peak-season branch has no seam to drive
 * it from a test: EnterpriseCalendar.isPeakSeason() is a global dependency
 * with no object to inject a substitute into. The only way this test can
 * know what to expect is to call the same hard dependency itself, which
 * proves the test agrees with whatever the production code's calendar
 * returns today, not that the surcharge logic is correct in both seasons.
 * That is the concrete cost Feathers describes: no seam means no control
 * over which branch a test actually exercises.
 */
class LegacyShipmentRiskScorerCharacterizationTest {

    private final LegacyShipmentRiskScorer legacy = new LegacyShipmentRiskScorer();

    @Test
    void aLightShipmentScoresFivePlusWhateverTheCalendarAddsToday() {
        Shipment light = new Shipment("SHM-3001", Instant.now().plus(1, ChronoUnit.DAYS), 10.0);

        int expected = EnterpriseCalendar.isPeakSeason() ? 5 + 15 : 5;

        assertThat(legacy.score(light)).isEqualTo(expected);
    }

    @Test
    void aHeavyShipmentScoresFortyPlusWhateverTheCalendarAddsToday() {
        Shipment heavy = new Shipment("SHM-3002", Instant.now().plus(1, ChronoUnit.DAYS), 150.0);

        int expected = EnterpriseCalendar.isPeakSeason() ? 40 + 15 : 40;

        assertThat(legacy.score(heavy)).isEqualTo(expected);
    }
}
