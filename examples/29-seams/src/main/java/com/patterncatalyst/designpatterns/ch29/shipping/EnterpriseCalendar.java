package com.patterncatalyst.designpatterns.ch29.shipping;

import java.time.LocalDate;
import java.time.Month;

/**
 * A process-wide, legacy enterprise calendar that several shipment-side
 * services read from and no single team owns. It has no constructor, no
 * instance, and no seam of its own, which is exactly the shape that calls
 * for a subclass-and-override seam at the one call site that reaches into
 * it, {@link ShipmentRiskScorer#isHighRiskWindow()}, rather than rewriting
 * this calendar or every one of its other callers.
 */
final class EnterpriseCalendar {

    private EnterpriseCalendar() {
    }

    static boolean isPeakSeason() {
        Month month = LocalDate.now().getMonth();
        return month == Month.NOVEMBER || month == Month.DECEMBER;
    }
}
