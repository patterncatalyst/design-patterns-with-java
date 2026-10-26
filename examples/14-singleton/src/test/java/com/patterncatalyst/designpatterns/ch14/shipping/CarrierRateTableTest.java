package com.patterncatalyst.designpatterns.ch14.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;

class CarrierRateTableTest {

    @Test
    void exactlyOneConstantExists() {
        assertThat(CarrierRateTable.values()).hasSize(1).containsExactly(CarrierRateTable.INSTANCE);
    }

    @Test
    void everyReferenceResolvesToTheSameInstance() {
        CarrierRateTable first = CarrierRateTable.INSTANCE;
        CarrierRateTable second = CarrierRateTable.valueOf("INSTANCE");

        assertThat(first).isSameAs(second);
    }

    @Test
    void baseRateForKnownCarrierReturnsItsRate() {
        assertThat(CarrierRateTable.INSTANCE.baseRateFor("GROUND")).isEqualTo(Money.ofCents(180));
        assertThat(CarrierRateTable.INSTANCE.baseRateFor("AIR")).isEqualTo(Money.ofCents(640));
    }

    @Test
    void baseRateForUnknownCarrierThrows() {
        assertThatThrownBy(() -> CarrierRateTable.INSTANCE.baseRateFor("DRONE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DRONE");
    }

    @Test
    void reflectionCannotForgeASecondInstance() throws NoSuchMethodException {
        Constructor<CarrierRateTable> constructor =
                CarrierRateTable.class.getDeclaredConstructor(String.class, int.class);
        constructor.setAccessible(true);

        assertThatThrownBy(() -> constructor.newInstance("FORGED", 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
