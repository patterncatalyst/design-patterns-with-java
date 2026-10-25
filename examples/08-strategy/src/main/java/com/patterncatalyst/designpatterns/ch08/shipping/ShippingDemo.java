package com.patterncatalyst.designpatterns.ch08.shipping;

import com.patterncatalyst.designpatterns.ch08.shipping.named.DistanceRateStrategy;
import com.patterncatalyst.designpatterns.ch08.shipping.named.FlatRateStrategy;
import com.patterncatalyst.designpatterns.ch08.shipping.named.WeightTieredRateStrategy;

/**
 * Prices the same {@link Shipment} with all three Strategy expressions and
 * prints each quote, so the agreement this example's tests check for is also
 * something you can watch happen at the terminal.
 */
public final class ShippingDemo {

    private ShippingDemo() {
    }

    public static void main(String[] args) {
        Shipment shipment = new Shipment(3.5, DestinationZone.REGIONAL, 12_000);

        System.out.println("== Interface: named classes implementing RateStrategy ==");
        ShippingRateCalculator calculator =
                new ShippingRateCalculator(new WeightTieredRateStrategy("ground", 500, 180));
        System.out.println("Weight-tiered strategy: " + calculator.quote(shipment));
        calculator.useStrategy(new DistanceRateStrategy("freight-zone"));
        System.out.println("Distance-zone strategy (swapped at runtime): " + calculator.quote(shipment));
        System.out.println("Flat-rate strategy (new instance): "
                + new FlatRateStrategy("air-flat", 999).quote(shipment));

        System.out.println();
        System.out.println("== Lambda: the same contract as a one-expression value ==");
        RateStrategy weightTiered = RateStrategies.weightTiered("ground", 500, 180);
        System.out.println("Weight-tiered lambda: " + weightTiered.quote(shipment));
        RateStrategy insured = RateStrategies.withInsuranceSurcharge(weightTiered, 0.80);
        System.out.println("Weight-tiered lambda + insurance (composed): " + insured.quote(shipment));

        System.out.println();
        System.out.println("== Enum: a closed set as constants ==");
        for (Carrier carrier : Carrier.values()) {
            System.out.println(carrier + ": " + carrier.quote(shipment));
        }

        System.out.println();
        System.out.println("Notice GROUND, the weight-tiered lambda, and WeightTieredRateStrategy");
        System.out.println("all quote the same amount for the same shipment: one shared pricing");
        System.out.println("function, three dispatch mechanisms.");
    }
}
