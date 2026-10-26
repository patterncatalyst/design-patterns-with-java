package com.patterncatalyst.designpatterns.ch13.shipping;

import java.util.Objects;

/**
 * The customs paperwork an international shipment carries: a harmonized
 * tariff code, the country goods originated in, a plain-language contents
 * description, and a declared value for duty assessment.
 *
 * <p>Every field here is required; a {@code CustomsDeclaration} with a blank
 * harmonized code or a negative declared value cannot exist, because the
 * compact canonical constructor below checks both at construction time, the
 * same validate-once discipline a {@link Shipment.Builder} applies to the
 * shipment that carries this declaration.
 *
 * <p>Deriving a changed copy goes through a wither method rather than a
 * setter, because this record has no setters to call: {@link
 * #withDeclaredValueCents(long)} and {@link
 * #withContentsDescription(String)} each return a brand new, independent
 * instance, leaving the original untouched.
 */
public record CustomsDeclaration(
        String harmonizedCode,
        String originCountry,
        String contentsDescription,
        long declaredValueCents) {

    public CustomsDeclaration {
        Objects.requireNonNull(harmonizedCode, "harmonizedCode");
        Objects.requireNonNull(originCountry, "originCountry");
        Objects.requireNonNull(contentsDescription, "contentsDescription");
        if (harmonizedCode.isBlank()) {
            throw new IllegalArgumentException("CustomsDeclaration: harmonizedCode cannot be blank");
        }
        if (originCountry.isBlank()) {
            throw new IllegalArgumentException("CustomsDeclaration: originCountry cannot be blank");
        }
        if (declaredValueCents < 0) {
            throw new IllegalArgumentException(
                    "CustomsDeclaration: declaredValueCents cannot be negative: " + declaredValueCents);
        }
    }

    /**
     * Returns a new declaration with every field unchanged except the
     * declared value. The receiver is never mutated; it has no mutable
     * state to mutate.
     */
    public CustomsDeclaration withDeclaredValueCents(long newDeclaredValueCents) {
        return new CustomsDeclaration(harmonizedCode, originCountry, contentsDescription, newDeclaredValueCents);
    }

    /** Returns a new declaration with every field unchanged except the contents description. */
    public CustomsDeclaration withContentsDescription(String newContentsDescription) {
        return new CustomsDeclaration(harmonizedCode, originCountry, newContentsDescription, declaredValueCents);
    }
}
