package com.patterncatalyst.designpatterns.ch27.shipping;

import com.patterncatalyst.designpatterns.ch27.shipping.overengineered.PricedShipmentReceipt;
import com.patterncatalyst.designpatterns.ch27.shipping.overengineered.RateCalculator;
import com.patterncatalyst.designpatterns.ch27.shipping.overengineered.RateCalculatorFactoryProvider;
import com.patterncatalyst.designpatterns.ch27.shipping.overengineered.ShipmentOperationsManager;
import com.patterncatalyst.designpatterns.ch27.shipping.overengineered.ShippingSurchargeRegistry;

/**
 * Exercises the over-engineered design and the simplified design side by
 * side against the same inputs, so the spooky-action-at-a-distance cost and
 * the independent-configuration fix are observable facts at the terminal,
 * not just assertions in a test file.
 */
public final class ShipmentPricingDemo {

    private ShipmentPricingDemo() {
    }

    public static void main(String[] args) {
        System.out.println("== Over-engineered: singleton + factory-of-factories ==");
        ShippingSurchargeRegistry.INSTANCE.setSurchargeMultiplier(1.10);
        RateCalculator regionA = RateCalculatorFactoryProvider.INSTANCE.factory().createCalculator();
        System.out.println("Region A quote (surcharge 1.10): " + regionA.calculate(Money.ofCents(10_000)));

        ShipmentOperationsManager manager = new ShipmentOperationsManager();
        PricedShipmentReceipt first = manager.priceShipment("ORD-3001", Money.ofCents(10_000));
        System.out.println("ORD-3001 priced via the manager: " + first.totalCost());

        ShippingSurchargeRegistry.INSTANCE.setSurchargeMultiplier(1.30);
        PricedShipmentReceipt second = manager.priceShipment("ORD-3002", Money.ofCents(10_000));
        System.out.println("ORD-3002 priced via the manager: " + second.totalCost());

        System.out.println(
                "Region A quote, re-read with no code touching it: " + regionA.calculate(Money.ofCents(10_000)));

        // Reset the shared global before the simplified side runs, so the
        // demo's second half is not itself at the mercy of the first half's
        // leftover state. A real process could not do this between callers.
        ShippingSurchargeRegistry.INSTANCE.setSurchargeMultiplier(1.0);

        System.out.println();
        System.out.println("== Simplified: plain dependency injection of RateSettings ==");
        var domestic = new ShipmentPricingService(new RateSettings(1.0, 250));
        var international = new ShipmentPricingService(new RateSettings(1.35, 900));

        System.out.println(
                "Domestic total (surcharge 1.00): " + domestic.price(Money.ofCents(10_000)).totalCost());
        System.out.println(
                "International total (surcharge 1.35): " + international.price(Money.ofCents(10_000)).totalCost());
        System.out.println("Domestic total, re-read after pricing international: "
                + domestic.price(Money.ofCents(10_000)).totalCost());
    }
}
