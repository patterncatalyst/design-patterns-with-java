package com.patterncatalyst.designpatterns.ch16.shipping;

import com.patterncatalyst.designpatterns.ch16.shipping.decorator.CachingRateQuoter;
import com.patterncatalyst.designpatterns.ch16.shipping.decorator.LoggingRateQuoter;
import com.patterncatalyst.designpatterns.ch16.shipping.decorator.SurchargeRateQuoter;
import com.patterncatalyst.designpatterns.ch16.shipping.proxy.LazyCarrierGatewayProxy;
import com.patterncatalyst.designpatterns.ch16.shipping.proxy.RateQuoterAccessControl;

import java.util.ArrayList;
import java.util.List;

/** Exercises both halves of this chapter's example and prints what happens. */
public final class ShippingDemo {

    private ShippingDemo() {
    }

    public static void main(String[] args) {
        Shipment shipment = new Shipment(2.0, DestinationZone.REGIONAL, 12_000);

        System.out.println("== Decorator: surcharge wraps caching wraps logging wraps core ==");
        List<String> callLog = new ArrayList<>();
        LoggingRateQuoter logging = new LoggingRateQuoter(new LiveCarrierGateway("ground"), callLog);
        CachingRateQuoter caching = new CachingRateQuoter(logging);
        SurchargeRateQuoter surcharge = new SurchargeRateQuoter(caching, 1.5);

        System.out.println("First quote (rate 1.5 cents/declared dollar): " + surcharge.quote(shipment).amount());
        surcharge.setCentsPerDeclaredDollar(3.0);
        System.out.println("Second quote, rate raised to 3.0 (base still cached): " + surcharge.quote(shipment).amount());
        System.out.println("Cache misses: " + caching.missCount() + ", cache hits: " + caching.hitCount()
                + ", log lines: " + callLog.size());
        System.out.println("The surcharge recomputes fresh on every call, so raising the rate changes the "
                + "total even though the base rate underneath it only computed once.");

        System.out.println();
        System.out.println("== Decorator: reordering it to caching wraps surcharge wraps logging wraps core ==");
        List<String> callLog2 = new ArrayList<>();
        LoggingRateQuoter logging2 = new LoggingRateQuoter(new LiveCarrierGateway("ground"), callLog2);
        SurchargeRateQuoter surcharge2 = new SurchargeRateQuoter(logging2, 1.5);
        CachingRateQuoter caching2 = new CachingRateQuoter(surcharge2);

        System.out.println("First quote (rate 1.5 cents/declared dollar): " + caching2.quote(shipment).amount());
        surcharge2.setCentsPerDeclaredDollar(3.0);
        System.out.println("Second quote, rate raised to 3.0 (total now cached): " + caching2.quote(shipment).amount());
        System.out.println("Cache misses: " + caching2.missCount() + ", cache hits: " + caching2.hitCount()
                + ", log lines: " + callLog2.size());
        System.out.println("Caching now sits outside the surcharge, so the already-surcharged total is what "
                + "gets memoized; raising the rate has no effect until the shipment key changes.");

        System.out.println();
        System.out.println("== Proxy: lazy initialization defers the real carrier gateway ==");
        LazyCarrierGatewayProxy lazy = new LazyCarrierGatewayProxy("air-flat");
        System.out.println("Real gateway constructed before first quote? " + lazy.isLoaded());
        System.out.println("Quote: " + lazy.quote(shipment));
        System.out.println("Real gateway constructed after first quote?  " + lazy.isLoaded());

        System.out.println();
        System.out.println("== Proxy: java.lang.reflect.Proxy gates access by clearance ==");
        RateQuoter real = new LiveCarrierGateway("freight-zone");
        RateQuoter warehouseView = RateQuoterAccessControl.guarded(real, ClearanceLevel.WAREHOUSE, ClearanceLevel.WAREHOUSE);
        System.out.println("Warehouse caller: " + warehouseView.quote(shipment));

        RateQuoter guestView = RateQuoterAccessControl.guarded(real, ClearanceLevel.GUEST, ClearanceLevel.WAREHOUSE);
        try {
            guestView.quote(shipment);
        } catch (SecurityException e) {
            System.out.println("Guest caller denied: " + e.getMessage());
        }
        System.out.println("Calls intercepted by the warehouse proxy: " + RateQuoterAccessControl.callCount(warehouseView));
    }
}
