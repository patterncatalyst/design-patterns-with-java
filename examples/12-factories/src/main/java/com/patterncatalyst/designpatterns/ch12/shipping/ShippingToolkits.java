package com.patterncatalyst.designpatterns.ch12.shipping;

import com.patterncatalyst.designpatterns.ch12.shipping.regional.AsiaPacificToolkit;
import com.patterncatalyst.designpatterns.ch12.shipping.regional.EuropeanUnionToolkit;
import com.patterncatalyst.designpatterns.ch12.shipping.regional.NorthAmericaToolkit;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Looks a {@link ShippingToolkit} up by {@link Region}, the decision a
 * checkout makes once per shipment after reading which regional hub the
 * shipment departs from. {@code forRegion} is a static factory method; which
 * concrete toolkit it returns is decided here, at this one call, instead of
 * at every call site that needs a toolkit.
 *
 * <p>The lookup itself is a {@code Map<Region, Supplier<ShippingToolkit>>}
 * rather than a direct {@code Map<Region, ShippingToolkit>}. A
 * {@code Supplier<ShippingToolkit>} defers construction until a region is
 * actually requested, so adding a fourth region only ever means adding one
 * more map entry here, never touching {@link RegionalShippingDesk} or any
 * other caller.
 */
public final class ShippingToolkits {

    private static final Map<Region, Supplier<ShippingToolkit>> TOOLKIT_SUPPLIERS =
            new EnumMap<>(Region.class);

    static {
        TOOLKIT_SUPPLIERS.put(Region.NORTH_AMERICA, () -> new NorthAmericaToolkit("hub-na-01"));
        TOOLKIT_SUPPLIERS.put(Region.EUROPEAN_UNION, () -> new EuropeanUnionToolkit("hub-eu-07"));
        TOOLKIT_SUPPLIERS.put(Region.ASIA_PACIFIC, () -> new AsiaPacificToolkit("hub-apac-14"));
    }

    private ShippingToolkits() {
    }

    public static ShippingToolkit forRegion(Region region) {
        Supplier<ShippingToolkit> supplier = TOOLKIT_SUPPLIERS.get(region);
        if (supplier == null) {
            throw new IllegalArgumentException("no toolkit registered for region: " + region);
        }
        return supplier.get();
    }

    /**
     * The entry point a checkout actually calls: a region name read from a
     * merchant's configuration, normalized and resolved to the same
     * toolkits {@link #forRegion(Region)} returns.
     */
    public static ShippingToolkit forRegionName(String regionName) {
        String normalized = regionName.strip().toUpperCase(Locale.ROOT).replace(' ', '_');
        try {
            return forRegion(Region.valueOf(normalized));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown region name: " + regionName, e);
        }
    }
}
