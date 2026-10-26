package com.patterncatalyst.designpatterns.ch12.shipping.regional;

import com.patterncatalyst.designpatterns.ch12.shipping.CustomsForm;
import com.patterncatalyst.designpatterns.ch12.shipping.LabelFormatter;
import com.patterncatalyst.designpatterns.ch12.shipping.Money;
import com.patterncatalyst.designpatterns.ch12.shipping.RateTable;
import com.patterncatalyst.designpatterns.ch12.shipping.Shipment;
import com.patterncatalyst.designpatterns.ch12.shipping.ShippingToolkit;

/**
 * The European Union's compliance scheme: an EORI-number declaration, a
 * weight-tiered rate that folds in a VAT handling fee, and a label format
 * entirely unlike North America's.
 */
public final class EuropeanUnionToolkit implements ShippingToolkit {
    private final String hubCode;

    public EuropeanUnionToolkit(String hubCode) {
        this.hubCode = hubCode;
    }

    @Override
    public LabelFormatter labelFormatter() {
        return new EuropeanUnionLabelFormatter(hubCode);
    }

    @Override
    public RateTable rateTable() {
        return new EuropeanUnionRateTable();
    }

    @Override
    public CustomsForm customsForm() {
        return new EuropeanUnionCustomsForm(hubCode);
    }

    private record EuropeanUnionLabelFormatter(String hubCode) implements LabelFormatter {
        @Override
        public String formatLabel(Shipment shipment) {
            return "EU-LABEL hub:%s to:%s weight:%.1f".formatted(
                    hubCode, shipment.destinationCountry(), shipment.weightKg());
        }
    }

    private record EuropeanUnionRateTable() implements RateTable {
        private static final long TIER_BASE_CENTS = 480;
        private static final long PER_KG_CENTS = 260;
        private static final double VAT_HANDLING_RATE = 0.02;

        @Override
        public Money quote(Shipment shipment) {
            long tieredCents = TIER_BASE_CENTS + Math.round(PER_KG_CENTS * shipment.weightKg());
            long vatHandlingCents = Math.round(shipment.declaredValueCents() * VAT_HANDLING_RATE);
            return Money.ofCents(tieredCents + vatHandlingCents);
        }
    }

    private record EuropeanUnionCustomsForm(String hubCode) implements CustomsForm {
        @Override
        public String declare(Shipment shipment) {
            return "EORI-DECLARATION hub:%s dest:%s value:%d".formatted(
                    hubCode, shipment.destinationCountry(), shipment.declaredValueCents());
        }
    }
}
