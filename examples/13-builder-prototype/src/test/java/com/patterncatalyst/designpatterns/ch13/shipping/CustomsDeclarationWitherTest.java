package com.patterncatalyst.designpatterns.ch13.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class CustomsDeclarationWitherTest {

    private static final CustomsDeclaration ORIGINAL =
            new CustomsDeclaration("8517.12", "US", "Wireless router", 24_900L);

    @Test
    void witherProducesAnIndependentChangedCopyLeavingTheOriginalUnchanged() {
        CustomsDeclaration revalued = ORIGINAL.withDeclaredValueCents(29_900L);

        assertThat(revalued.declaredValueCents()).isEqualTo(29_900L);
        assertThat(revalued.harmonizedCode()).isEqualTo(ORIGINAL.harmonizedCode());
        assertThat(revalued.contentsDescription()).isEqualTo(ORIGINAL.contentsDescription());

        // The original is untouched: records have no setters to have mutated it with.
        assertThat(ORIGINAL.declaredValueCents()).isEqualTo(24_900L);
        assertThat(revalued).isNotEqualTo(ORIGINAL);
    }

    @Test
    void withContentsDescriptionProducesAnIndependentChangedCopy() {
        CustomsDeclaration relabeled = ORIGINAL.withContentsDescription("Wireless mesh router");

        assertThat(relabeled.contentsDescription()).isEqualTo("Wireless mesh router");
        assertThat(ORIGINAL.contentsDescription()).isEqualTo("Wireless router");
    }

    @Test
    void rejectsABlankHarmonizedCode() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CustomsDeclaration("  ", "US", "Wireless router", 24_900L))
                .withMessageContaining("harmonizedCode");
    }

    @Test
    void rejectsANegativeDeclaredValue() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CustomsDeclaration("8517.12", "US", "Wireless router", -1L))
                .withMessageContaining("declaredValueCents");
    }
}
