package com.patterncatalyst.designpatterns.ch23.shipping;

import java.util.List;
import java.util.Objects;

/**
 * A facility's manifest for one outbound run: a batch identifier and the
 * confirmation numbers it carries. The compact canonical constructor copies
 * the incoming list with {@link List#copyOf}, which is where this record
 * retires the defensive-copying ceremony a hand-written value class used to
 * need in two places at once, a copy on the way in so a caller's mutation
 * of its own list afterward cannot change this manifest, and an
 * unmodifiable wrapper on the way out so a caller cannot mutate this
 * manifest's list through the reference the accessor hands back. One line
 * here gets both, because {@code List.copyOf} is already unmodifiable.
 */
public record DeliveryManifest(String batchId, List<String> confirmationNumbers) {

    public DeliveryManifest {
        Objects.requireNonNull(batchId, "batchId");
        Objects.requireNonNull(confirmationNumbers, "confirmationNumbers");
        confirmationNumbers = List.copyOf(confirmationNumbers);
    }
}
