package com.patterncatalyst.designpatterns.ch13.shipping;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A configured packaging exemplar: a kind of package a warehouse reuses
 * constantly (fragile electronics, bulk dry goods, refrigerated
 * perishables), each with its own default weight, handling code, and list
 * of protective materials.
 *
 * <p>This is the Prototype this chapter clones from. It is deliberately a
 * mutable class rather than a record: a warehouse worker adjusts one
 * package's weight or adds a protective material after it is packed, and
 * {@link #copy()} has to hand back an instance independent enough that
 * doing so never reaches back into the registered exemplar or any other
 * package cloned from it.
 */
public final class PackagingTemplate {

    private final String kind;
    private double weightKg;
    private String handlingCode;
    private final List<String> protectiveMaterials;

    public PackagingTemplate(String kind, double weightKg, String handlingCode, List<String> protectiveMaterials) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.weightKg = weightKg;
        this.handlingCode = Objects.requireNonNull(handlingCode, "handlingCode");
        this.protectiveMaterials = new ArrayList<>(Objects.requireNonNull(protectiveMaterials, "protectiveMaterials"));
    }

    /** Copy constructor: every field, including the mutable material list, is copied by value. */
    private PackagingTemplate(PackagingTemplate source) {
        this.kind = source.kind;
        this.weightKg = source.weightKg;
        this.handlingCode = source.handlingCode;
        this.protectiveMaterials = new ArrayList<>(source.protectiveMaterials);
    }

    /**
     * Returns a new, independently owned package built from this
     * exemplar's current state. Mutating the result never changes this
     * exemplar, and mutating this exemplar afterward never changes a
     * package already handed out by an earlier {@code copy()} call.
     */
    public PackagingTemplate copy() {
        return new PackagingTemplate(this);
    }

    public String kind() {
        return kind;
    }

    public double weightKg() {
        return weightKg;
    }

    public void setWeightKg(double weightKg) {
        this.weightKg = weightKg;
    }

    public String handlingCode() {
        return handlingCode;
    }

    public void setHandlingCode(String handlingCode) {
        this.handlingCode = Objects.requireNonNull(handlingCode, "handlingCode");
    }

    /** An immutable snapshot of the current protective materials; mutating it never affects this instance. */
    public List<String> protectiveMaterials() {
        return List.copyOf(protectiveMaterials);
    }

    public void addProtectiveMaterial(String material) {
        protectiveMaterials.add(Objects.requireNonNull(material, "material"));
    }

    @Override
    public String toString() {
        return "PackagingTemplate[kind=%s, weightKg=%.2f, handlingCode=%s, protectiveMaterials=%s]"
                .formatted(kind, weightKg, handlingCode, protectiveMaterials);
    }
}
