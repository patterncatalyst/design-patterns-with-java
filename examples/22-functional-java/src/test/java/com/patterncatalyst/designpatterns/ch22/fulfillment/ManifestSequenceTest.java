package com.patterncatalyst.designpatterns.ch22.fulfillment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * {@code ManifestSequence} is the enum Singleton idiom: exactly one constant,
 * {@code INSTANCE}, so every caller in the program shares one counter.
 */
class ManifestSequenceTest {

    @Test
    void exactlyOneConstantExists() {
        assertThat(ManifestSequence.values()).containsExactly(ManifestSequence.INSTANCE);
    }

    @Test
    void everyReferenceToInstanceIsTheSameObject() {
        ManifestSequence first = ManifestSequence.INSTANCE;
        ManifestSequence second = ManifestSequence.valueOf("INSTANCE");

        assertThat(first).isSameAs(second);
    }

    @Test
    void nextManifestIdNeverRepeatsAndCountsUpByOne() {
        long before = ManifestSequence.INSTANCE.issuedCount();

        String first = ManifestSequence.INSTANCE.nextManifestId();
        String second = ManifestSequence.INSTANCE.nextManifestId();

        assertThat(first).isNotEqualTo(second);
        assertThat(ManifestSequence.INSTANCE.issuedCount()).isEqualTo(before + 2);
    }
}
