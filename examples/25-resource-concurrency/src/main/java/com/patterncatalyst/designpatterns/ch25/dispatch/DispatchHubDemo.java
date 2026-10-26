package com.patterncatalyst.designpatterns.ch25.dispatch;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Exercises the connection pool, the monitor object, the active object, and
 * the ThreadLocal tracking-code generator against one narrative: a regional
 * dispatch hub during a morning surge.
 */
public final class DispatchHubDemo {

    private DispatchHubDemo() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("== Connection pool: bounded, blocking, reused ==");
        CarrierConnectionPool pool = new CarrierConnectionPool(2, CarrierApiConnection::new);
        try (CarrierConnectionPool.Lease leaseA = pool.acquire(Duration.ofSeconds(1));
                CarrierConnectionPool.Lease leaseB = pool.acquire(Duration.ofSeconds(1))) {
            System.out.println(leaseA.connection().fetchTrackingStatus("SHM-7001"));
            System.out.println(leaseB.connection().fetchTrackingStatus("SHM-7002"));
            System.out.println("Connections created so far: " + pool.connectionsCreated());
        }
        try (CarrierConnectionPool.Lease reused = pool.acquire(Duration.ofSeconds(1))) {
            System.out.println("Reused connection id " + reused.connection().id()
                    + "; still only " + pool.connectionsCreated() + " connection(s) ever created");
        }

        System.out.println();
        System.out.println("== Monitor object: DispatchDock serializes concurrent scanners ==");
        DispatchDock dock = new DispatchDock(4);
        dock.stageParcel(new ParcelScan("SHM-8001", "scanned"));
        dock.stageParcel(new ParcelScan("SHM-8002", "scanned"));
        dock.stageParcel(new ParcelScan("SHM-8003", "scanned"));
        dock.stageParcel(new ParcelScan("SHM-8004", "scanned"));
        List<ParcelScan> manifest = dock.awaitFullBay(Duration.ofSeconds(1));
        System.out.println("Bay reached capacity; dispatcher received " + manifest.size() + " parcels");

        System.out.println();
        System.out.println("== Active object: LabelPrintActiveObject decouples call from execution ==");
        LabelPrintActiveObject printer = new LabelPrintActiveObject();
        CompletableFuture<PrintReceipt> printA = printer.submitPrint(new ShippingLabelRequest("SHM-9001", "PHX"));
        CompletableFuture<PrintReceipt> printB = printer.submitPrint(new ShippingLabelRequest("SHM-9002", "DEN"));
        System.out.println("submitPrint returned immediately on " + Thread.currentThread().getName());
        System.out.println(printA.get());
        System.out.println(printB.get());
        printer.shutdown();

        System.out.println();
        System.out.println("== ThreadLocal: a per-thread MessageDigest instance ==");
        TrackingCodeGenerator codes = new TrackingCodeGenerator();
        System.out.println("SHM-7001 -> " + codes.trackingCodeFor("SHM-7001"));
        System.out.println("SHM-7002 -> " + codes.trackingCodeFor("SHM-7002"));
    }
}
