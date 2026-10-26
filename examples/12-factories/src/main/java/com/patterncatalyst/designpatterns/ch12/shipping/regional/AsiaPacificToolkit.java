package com.patterncatalyst.designpatterns.ch12.shipping.regional;

import com.patterncatalyst.designpatterns.ch12.shipping.CustomsForm;
import com.patterncatalyst.designpatterns.ch12.shipping.LabelFormatter;
import com.patterncatalyst.designpatterns.ch12.shipping.Money;
import com.patterncatalyst.designpatterns.ch12.shipping.RateTable;
import com.patterncatalyst.designpatterns.ch12.shipping.Shipment;
import com.patterncatalyst.designpatterns.ch12.shipping.ShippingToolkit;

/**
 * Asia-Pacific's compliance scheme: an HS tariff-code declaration, a
 * declared-value-tiered rate, and its own manifest-style label.
 */
public final class AsiaPacificToolkit implements ShippingToolkit {
    private final String hubCode;

    public AsiaPacificToolkit(String hubCode) {
        this.hubCode = hubCode;
    }

    @Override
    public LabelFormatter labelFormatter() {
        return new AsiaPacificLabelFormatter(hubCode);
    }

    @Override
    public RateTable rateTable() {
        return new AsiaPacificRateTable();
    }

    @Override
    public CustomsForm customsForm() {
        return new AsiaPacificCustomsForm(hubCode);
    }

    private record AsiaPacificLabelFormatter(String hubCode) implements LabelFormatter {
        @Override
        public String formatLabel(Shipment shipment) {
            return "APAC-MANIFEST hub=%s dest=%s wt=%.1fkg".formatted(
                    hubCode, shipment.destinationCountry(), shipment.weightKg());
        }
    }

    private record AsiaPacificRateTable() implements RateTable {
        private static final long LOW_VALUE_BASE_CENTS = 420;
        private static final long HIGH_VALUE_BASE_CENTS = 710;
        private static final long HIGH_VALUE_THRESHOLD_CENTS = 50_000;
        private static final long PER_KG_CENTS = 190;

        @Override
        public Money quote(Shipment shipment) {
            long base = shipment.declaredValueCents() >= HIGH_VALUE_THRESHOLD_CENTS
                    ? HIGH_VALUE_BASE_CENTS
                    : LOW_VALUE_BASE_CENTS;
            return Money.ofCents(base + Math.round(PER_KG_CENTS * shipment.weightKg()));
        }
    }

    private record AsiaPacificCustomsForm(String hubCode) implements CustomsForm {
        @Override
        public String declare(Shipment shipment) {
            return "HS-TARIFF-DECLARATION hub=%s dest=%s value=%d".formatted(
                    hubCode, shipment.destinationCountry(), shipment.declaredValueCents());
        }
    }
}
