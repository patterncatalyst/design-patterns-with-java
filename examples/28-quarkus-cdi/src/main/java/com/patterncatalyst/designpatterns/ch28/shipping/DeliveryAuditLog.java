package com.patterncatalyst.designpatterns.ch28.shipping;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The second, independent observer of {@link ShipmentStatusChanged}.
 * Arc delivers the identical event object to this bean and to
 * {@link DashboardNotifier} in the same {@code fire()} call;
 * neither observer knows the other exists.
 */
@ApplicationScoped
public class DeliveryAuditLog {

    private final List<ShipmentStatusChanged> entries = new CopyOnWriteArrayList<>();

    public void onStatusChanged(@Observes ShipmentStatusChanged event) {
        entries.add(event);
    }

    public List<ShipmentStatusChanged> entries() {
        return List.copyOf(entries);
    }
}
