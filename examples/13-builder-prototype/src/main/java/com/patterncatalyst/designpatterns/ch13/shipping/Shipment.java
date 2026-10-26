package com.patterncatalyst.designpatterns.ch13.shipping;

import java.util.Objects;
import java.util.Optional;

/**
 * A shipment manifest: three required header fields, several optional ones
 * already holding sensible defaults, and a cross-field invariant that only
 * holds once every field is in place: a shipment headed outside the
 * domestic market must carry a {@link CustomsDeclaration}.
 *
 * <p>No public constructor exists. A {@code Shipment} comes only from
 * {@link Builder#build()}, which is the single place that invariant is
 * checked, so every {@code Shipment} a caller ever holds is already known
 * to satisfy it.
 */
public final class Shipment {

    /** The one country code this example treats as domestic; every other destination is international. */
    public static final String DOMESTIC_COUNTRY = "US";

    private final String trackingId;
    private final String carrier;
    private final String destinationCountry;
    private final ShippingPriority priority;
    private final long insuredValueCents;
    private final boolean signatureRequired;
    private final CustomsDeclaration customsDeclaration;
    private final String specialHandlingNotes;

    private Shipment(Builder builder) {
        this.trackingId = builder.trackingId;
        this.carrier = builder.carrier;
        this.destinationCountry = builder.destinationCountry;
        this.priority = builder.priority;
        this.insuredValueCents = builder.insuredValueCents;
        this.signatureRequired = builder.signatureRequired;
        this.customsDeclaration = builder.customsDeclaration;
        this.specialHandlingNotes = builder.specialHandlingNotes;
    }

    public String trackingId() {
        return trackingId;
    }

    public String carrier() {
        return carrier;
    }

    public String destinationCountry() {
        return destinationCountry;
    }

    public ShippingPriority priority() {
        return priority;
    }

    public long insuredValueCents() {
        return insuredValueCents;
    }

    public boolean signatureRequired() {
        return signatureRequired;
    }

    /** Empty for a domestic shipment; always present for an international one. */
    public Optional<CustomsDeclaration> customsDeclaration() {
        return Optional.ofNullable(customsDeclaration);
    }

    public String specialHandlingNotes() {
        return specialHandlingNotes;
    }

    public boolean isInternational() {
        return !DOMESTIC_COUNTRY.equals(destinationCountry);
    }

    public String summary() {
        return "%s via %s to %s [%s]%s, insured for $%d.%02d".formatted(
                trackingId, carrier, destinationCountry, priority,
                customsDeclaration != null ? ", customs declared" : "",
                insuredValueCents / 100, insuredValueCents % 100);
    }

    @Override
    public String toString() {
        return "Shipment[" + summary() + "]";
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Joshua Bloch's Builder: a static nested class that accumulates field
     * values through a fluent interface and checks every cross-field
     * invariant exactly once, at {@link #build()}. Nothing is validated as
     * the chain runs; a half-built shipment is not finished yet, not an
     * invalid one.
     */
    public static final class Builder {
        private String trackingId;
        private String carrier;
        private String destinationCountry;
        private ShippingPriority priority = ShippingPriority.STANDARD;
        private long insuredValueCents = 0L;
        private boolean signatureRequired = false;
        private CustomsDeclaration customsDeclaration;
        private String specialHandlingNotes = "";

        private Builder() {
        }

        public Builder trackingId(String trackingId) {
            this.trackingId = trackingId;
            return this;
        }

        public Builder carrier(String carrier) {
            this.carrier = carrier;
            return this;
        }

        public Builder destinationCountry(String destinationCountry) {
            this.destinationCountry = destinationCountry;
            return this;
        }

        public Builder priority(ShippingPriority priority) {
            this.priority = Objects.requireNonNull(priority, "priority");
            return this;
        }

        public Builder insuredValueCents(long insuredValueCents) {
            this.insuredValueCents = insuredValueCents;
            return this;
        }

        public Builder signatureRequired(boolean signatureRequired) {
            this.signatureRequired = signatureRequired;
            return this;
        }

        public Builder customsDeclaration(CustomsDeclaration customsDeclaration) {
            this.customsDeclaration = customsDeclaration;
            return this;
        }

        public Builder specialHandlingNotes(String specialHandlingNotes) {
            this.specialHandlingNotes = Objects.requireNonNull(specialHandlingNotes, "specialHandlingNotes");
            return this;
        }

        /**
         * Checks every required field and the one cross-field invariant in
         * a single pass, so the claim "a Shipment leaving build() is always
         * complete" has exactly one place it could fail to hold.
         *
         * @throws IllegalStateException if a required field is missing, or
         *     the destination is international and no customs declaration
         *     was set
         */
        public Shipment build() {
            String error = validationError();
            if (error != null) {
                throw new IllegalStateException(error);
            }
            return new Shipment(this);
        }

        private String validationError() {
            if (trackingId == null || trackingId.isBlank()) {
                return "Shipment.Builder: trackingId is required";
            }
            if (carrier == null || carrier.isBlank()) {
                return "Shipment.Builder: carrier is required";
            }
            if (destinationCountry == null || destinationCountry.isBlank()) {
                return "Shipment.Builder: destinationCountry is required";
            }
            boolean international = !DOMESTIC_COUNTRY.equals(destinationCountry);
            if (international && customsDeclaration == null) {
                return "Shipment.Builder: customsDeclaration is required for an international destination ("
                        + destinationCountry + ")";
            }
            return null;
        }
    }
}
