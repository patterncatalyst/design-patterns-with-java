package com.patterncatalyst.designpatterns.ch22.fulfillment;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * A fixed three-step sequence, validate, price, assign a manifest number,
 * written as a single static method instead of an abstract base class with
 * {@code protected} hooks. Two of the three steps, {@code validate} and
 * {@code price}, arrive as {@link Function} parameters supplied per sales
 * channel at the call site; the third, assigning a manifest number through
 * {@link ManifestSequence}, never varies and is not exposed as a parameter
 * at all. A reader confirms the sequence never changes by reading this one
 * method; confirming what varies per channel means reading whatever
 * {@code Function} a caller passed in, exactly the same split a
 * {@code final} skeleton method and its {@code protected abstract} hooks
 * draw with a class hierarchy instead of two parameters.
 */
public final class FulfillmentPipeline {

    private FulfillmentPipeline() {
    }

    public static FulfillmentRecord process(
            Shipment shipment,
            Function<Shipment, Optional<String>> validate,
            Function<Shipment, Money> price) {
        Objects.requireNonNull(shipment, "shipment");
        Objects.requireNonNull(validate, "validate");
        Objects.requireNonNull(price, "price");

        Optional<String> rejection = validate.apply(shipment);
        if (rejection.isPresent()) {
            return FulfillmentRecord.rejected(shipment, rejection.get());
        }

        Money total = price.apply(shipment);
        String manifestId = ManifestSequence.INSTANCE.nextManifestId();
        return FulfillmentRecord.accepted(manifestId, shipment, total);
    }
}
