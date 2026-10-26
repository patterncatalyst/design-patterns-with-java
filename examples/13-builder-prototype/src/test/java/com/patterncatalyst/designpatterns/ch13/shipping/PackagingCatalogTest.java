package com.patterncatalyst.designpatterns.ch13.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PackagingCatalogTest {

    private PackagingCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new PackagingCatalog();
        catalog.register("fragile-electronics",
                new PackagingTemplate("fragile-electronics", 1.2, "FRAGILE", List.of("bubble-wrap", "foam-corner")));
    }

    @Test
    void aPrototypeCopyIsIndependentOfItsExemplar() {
        PackagingTemplate copy = catalog.create("fragile-electronics");
        copy.setWeightKg(5.0);
        copy.addProtectiveMaterial("anti-static-bag");

        PackagingTemplate freshCopy = catalog.create("fragile-electronics");

        assertThat(freshCopy.weightKg()).isEqualTo(1.2);
        assertThat(freshCopy.protectiveMaterials()).containsExactly("bubble-wrap", "foam-corner");
    }

    @Test
    void twoCopiesFromTheSameExemplarAreIndependentOfEachOther() {
        PackagingTemplate first = catalog.create("fragile-electronics");
        PackagingTemplate second = catalog.create("fragile-electronics");

        first.setWeightKg(9.25);
        first.addProtectiveMaterial("anti-static-bag");

        assertThat(second.weightKg()).isEqualTo(1.2);
        assertThat(second.protectiveMaterials()).containsExactly("bubble-wrap", "foam-corner");
        assertThat(first.weightKg()).isEqualTo(9.25);
        assertThat(first.protectiveMaterials()).containsExactly("bubble-wrap", "foam-corner", "anti-static-bag");
    }

    @Test
    void createReturnsANewInstanceEveryCall() {
        PackagingTemplate first = catalog.create("fragile-electronics");
        PackagingTemplate second = catalog.create("fragile-electronics");

        assertThat(first).isNotSameAs(second);
    }

    @Test
    void createRejectsAnUnregisteredName() {
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> catalog.create("refrigerated-perishable"))
                .withMessageContaining("refrigerated-perishable");
    }
}
