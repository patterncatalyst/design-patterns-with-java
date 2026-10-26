package com.patterncatalyst.designpatterns.ch14.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class HolderCarrierRateTableTest {

    @Test
    void repeatedCallsReturnTheSameInstance() {
        HolderCarrierRateTable first = HolderCarrierRateTable.getInstance();
        HolderCarrierRateTable second = HolderCarrierRateTable.getInstance();

        assertThat(first).isSameAs(second);
    }

    @Test
    void concurrentFirstAccessFromManyThreadsYieldsExactlyOneInstance() throws Exception {
        int threadCount = 16;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            Callable<HolderCarrierRateTable> fetchInstance = HolderCarrierRateTable::getInstance;
            List<Future<HolderCarrierRateTable>> futures = IntStream.range(0, threadCount)
                    .mapToObj(i -> executor.submit(fetchInstance))
                    .collect(Collectors.toList());

            Set<HolderCarrierRateTable> distinctInstances = new HashSet<>();
            for (Future<HolderCarrierRateTable> future : futures) {
                distinctInstances.add(future.get());
            }

            assertThat(distinctInstances).hasSize(1);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void baseRateForAgreesWithTheEnumSingletonForTheSameCarrier() {
        assertThat(HolderCarrierRateTable.getInstance().baseRateFor("FREIGHT"))
                .isEqualTo(CarrierRateTable.INSTANCE.baseRateFor("FREIGHT"));
    }

    @Test
    void baseRateForUnknownCarrierThrows() {
        assertThatThrownBy(() -> HolderCarrierRateTable.getInstance().baseRateFor("DRONE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DRONE");
    }
}
