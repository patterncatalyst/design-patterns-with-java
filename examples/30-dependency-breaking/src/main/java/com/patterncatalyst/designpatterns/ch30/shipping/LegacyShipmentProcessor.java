package com.patterncatalyst.designpatterns.ch30.shipping;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;

/**
 * An inherited shipment dispatcher, still doing exactly what it always did,
 * now opened for testing with four of Feathers' dependency-breaking
 * techniques instead of a rewrite.
 *
 * <p>Before this pass, the no-argument-dependency constructor built a
 * concrete {@code SmtpNotificationGateway} internally, {@code dispatch}
 * called {@code new StandardSurchargeCalculator()} inline, the express-lane
 * check read {@code LocalDate.now()} directly, and {@code summarizeManifest}
 * took a {@code CarrierManifestFile} parameter whose constructor opens a
 * real file. None of that logic was wrong; none of it could be tested
 * without a real mail server, a real clock, or a real file on disk.
 *
 * <p>Every existing call site this class had before this chapter's pass
 * still compiles and still behaves identically: the single-argument
 * constructor still builds a real {@code SmtpNotificationGateway}, the
 * default {@code createSurchargeCalculator()} still returns a
 * {@code StandardSurchargeCalculator}, and {@code today()} still reads the
 * real clock. Only a test, by calling the new constructor overload or
 * subclassing to override one protected method, ever sees anything else.
 */
public class LegacyShipmentProcessor {

    private final String operationsEmail;
    private final NotificationGateway notificationGateway;

    /**
     * Parameterize Constructor, expressed in Java as an overload rather
     * than a default argument: every call site written against the
     * inherited one-argument constructor keeps compiling, and keeps
     * getting a real {@link SmtpNotificationGateway} wired to the same
     * host the inherited constructor built internally.
     */
    public LegacyShipmentProcessor(String operationsEmail) {
        this(operationsEmail, new SmtpNotificationGateway("mail.internal.example"));
    }

    /**
     * The constructor only a test calls directly, supplying a
     * {@link NotificationGateway} double instead of the real SMTP
     * implementation. This overload is the whole seam; nothing else about
     * the class needed to change for it to exist.
     */
    public LegacyShipmentProcessor(String operationsEmail, NotificationGateway notificationGateway) {
        this.operationsEmail = Objects.requireNonNull(operationsEmail, "operationsEmail");
        this.notificationGateway = Objects.requireNonNull(notificationGateway, "notificationGateway");
    }

    /**
     * Prices a shipment, decides express-lane eligibility, and notifies
     * operations. The logic here is untouched by this chapter; only the
     * three dependencies it reaches for, the mail gateway, the surcharge
     * calculator, and the clock, were opened for substitution.
     */
    public ShipmentDispatchResult dispatch(Shipment shipment) {
        Objects.requireNonNull(shipment, "shipment");
        long surcharge = createSurchargeCalculator().surchargeCents(shipment.baseCostCents());
        long totalCostCents = shipment.baseCostCents() + surcharge;
        boolean expressEligible = isEligibleForExpressLane();

        String message = "Shipment %s dispatched, total $%.2f%s".formatted(
                shipment.id(),
                totalCostCents / 100.0,
                expressEligible ? ", express lane eligible" : "");
        notificationGateway.sendDispatchConfirmation(operationsEmail, message);

        return new ShipmentDispatchResult(shipment.id(), totalCostCents, expressEligible);
    }

    /**
     * Extract and Override Factory Method: the inherited {@code dispatch}
     * method called {@code new StandardSurchargeCalculator()} inline. That
     * single embedded {@code new} expression is now this one protected
     * method, so a test subclass can return a different calculator without
     * touching dispatch's logic or construction order.
     */
    protected SurchargeCalculator createSurchargeCalculator() {
        return new StandardSurchargeCalculator();
    }

    /**
     * Subclass and Override Method: the inherited eligibility check read
     * {@code LocalDate.now()} inline, so its answer depended on which day
     * the test happened to run. Pulling the read into {@code today()}
     * leaves the weekday rule itself, express lane on weekdays only,
     * exactly as it was; only the clock read moved.
     */
    protected boolean isEligibleForExpressLane() {
        DayOfWeek dayOfWeek = today().getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    protected LocalDate today() {
        return LocalDate.now();
    }

    /**
     * Adapt Parameter: the inherited signature took a
     * {@code CarrierManifestFile} directly. This signature takes the
     * narrow {@link ManifestSource} the method actually reads from, so a
     * test supplies two values instead of a file on disk, while
     * {@link CarrierManifestFileAdapter} lets production code keep passing
     * a real {@code CarrierManifestFile} with no change at the call site
     * beyond wrapping it once.
     */
    public String summarizeManifest(ManifestSource manifest) {
        Objects.requireNonNull(manifest, "manifest");
        return "%s: %d package(s)".formatted(manifest.carrierCode(), manifest.packageCount());
    }
}
