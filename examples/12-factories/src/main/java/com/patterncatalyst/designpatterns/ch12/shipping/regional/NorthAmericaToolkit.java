package com.patterncatalyst.designpatterns.ch12.shipping.regional;

import com.patterncatalyst.designpatterns.ch12.shipping.CustomsForm;
import com.patterncatalyst.designpatterns.ch12.shipping.LabelFormatter;
import com.patterncatalyst.designpatterns.ch12.shipping.Money;
import com.patterncatalyst.designpatterns.ch12.shipping.RateTable;
import com.patterncatalyst.designpatterns.ch12.shipping.Shipment;
import com.patterncatalyst.designpatterns.ch12.shipping.ShippingToolkit;

/**
 * North America's compliance scheme: a USMCA certificate of origin, a
 * flat-base-plus-per-kilogram rate, and a label carrying the hub's own
 * tracking prefix. The three nested product classes stay package-private on
 * purpose; nothing outside this file ever names them, because
 * {@code RegionalShippingDesk} is built against {@link LabelFormatter},
 * {@link RateTable}, and {@link CustomsForm} alone.
 */
public final class NorthAmericaToolkit implements ShippingToolkit {
    private final String hubCode;

    public NorthAmericaToolkit(String hubCode) {
        this.hubCode = hubCode;
    }

    @Override
    public LabelFormatter labelFormatter() {
        return new NorthAmericaLabelFormatter(hubCode);
    }

    @Override
    public RateTable rateTable() {
        return new NorthAmericaRateTable();
    }

    @Override
    public CustomsForm customsForm() {
        return new NorthAmericaCustomsForm(hubCode);
    }

    private record NorthAmericaLabelFormatter(String hubCode) implements LabelFormatter {
        @Override
        public String formatLabel(Shipment shipment) {
            return "NA-LABEL hub=%s dest=%s wt=%.1fkg".formatted(
                    hubCode, shipment.destinationCountry(), shipment.weightKg());
        }
    }

    private record NorthAmericaRateTable() implements RateTable {
        private static final long BASE_CENTS = 650;
        private static final long PER_KG_CENTS = 210;

        @Override
        public Money quote(Shipment shipment) {
            long cents = BASE_CENTS + Math.round(PER_KG_CENTS * shipment.weightKg());
            return Money.ofCents(cents);
        }
    }

    private record NorthAmericaCustomsForm(String hubCode) implements CustomsForm {
        @Override
        public String declare(Shipment shipment) {
            return "USMCA-CERT hub=%s dest=%s declaredValue=%d".formatted(
                    hubCode, shipment.destinationCountry(), shipment.declaredValueCents());
        }
    }
}
