package com.patterncatalyst.designpatterns.ch17.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

/**
 * {@link FlyweightPool} must return the identical instance for repeated
 * lookups of the same key, grow its pool only on keys actually requested,
 * and refuse a key nothing registered rather than materialize one silently.
 */
class FlyweightPoolTest {

    @Test
    void twoLookupsForTheSameKeyReturnTheIdenticalInstance() {
        FlyweightPool<String, PackagingSpec> pool = new FlyweightPool<>(Map.of(
                "STD-BOX", new PackagingSpec("STD-BOX", "corrugated cardboard", 180, HazmatClass.NONE)));

        PackagingSpec first = pool.get("STD-BOX");
        PackagingSpec second = pool.get("STD-BOX");

        assertThat(second).isSameAs(first);
    }

    @Test
    void sizeOnlyCountsKeysActuallyRequested() {
        FlyweightPool<String, PackagingSpec> pool = new FlyweightPool<>(Map.of(
                "STD-BOX", new PackagingSpec("STD-BOX", "corrugated cardboard", 180, HazmatClass.NONE),
                "LI-PACK", new PackagingSpec("LI-PACK", "UN 3480 lithium pack", 95, HazmatClass.LITHIUM)));

        assertThat(pool.size()).isZero();

        pool.get("STD-BOX");
        pool.get("STD-BOX");
        pool.get("STD-BOX");

        assertThat(pool.size()).isEqualTo(1);
    }

    @Test
    void anUnregisteredKeyFailsInsteadOfMaterializingSilently() {
        FlyweightPool<String, PackagingSpec> pool = new FlyweightPool<>(Map.of());

        assertThatThrownBy(() -> pool.get("CRATE-999")).isInstanceOf(NoSuchElementException.class);
    }
}
