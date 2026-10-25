package com.patterncatalyst.designpatterns.ch09.shipping;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Proves the claim this chapter makes about the abstract-method skeleton
 * directly: {@link OrderProcessingPipeline#process(Order)} runs its three
 * hooks in a fixed order, the shared precondition runs before any of them,
 * and the entry point itself cannot be overridden.
 */
class OrderProcessingPipelineTest {

    private static final Order SAMPLE_ORDER = new Order(
            "ORD-1",
            "Customer",
            List.of(new OrderLine("SKU-1", 2, 1_000)),
            "");

    @Test
    void callsHooksInTheFixedSkeletonOrder() {
        RecordingPipeline pipeline = new RecordingPipeline();

        pipeline.process(SAMPLE_ORDER);

        assertThat(pipeline.calls)
                .containsExactly("validateChannelRules", "priceOrder", "recordToLedger");
    }

    @Test
    void theInvariantPreconditionRunsBeforeAnyHook() {
        RecordingPipeline pipeline = new RecordingPipeline();
        Order empty = new Order("ORD-2", "Customer", List.of(), "");

        assertThatIllegalArgumentException().isThrownBy(() -> pipeline.process(empty));
        assertThat(pipeline.calls).isEmpty();
    }

    @Test
    void theResultCarriesTheChannelNameAndTheInvariantQuantity() {
        RecordingPipeline pipeline = new RecordingPipeline();

        ProcessingResult result = pipeline.process(SAMPLE_ORDER);

        assertThat(result.channel()).isEqualTo("recording");
        assertThat(result.lineCount()).isEqualTo(2);
        assertThat(result.totalCents()).isEqualTo(999L);
        assertThat(result.ledgerNote()).isEqualTo("recorded:ORD-1");
    }

    @Test
    void processIsDeclaredFinalSoNoChannelCanOverrideTheSequence() throws NoSuchMethodException {
        Method process = OrderProcessingPipeline.class.getMethod("process", Order.class);

        assertThat(Modifier.isFinal(process.getModifiers())).isTrue();
    }

    /** Test-only subclass that logs each hook call so the fixed order is an observable fact. */
    private static final class RecordingPipeline extends OrderProcessingPipeline {

        final List<String> calls = new ArrayList<>();

        @Override
        protected String channelName() {
            return "recording";
        }

        @Override
        protected void validateChannelRules(Order order) {
            calls.add("validateChannelRules");
        }

        @Override
        protected long priceOrder(Order order) {
            calls.add("priceOrder");
            return 999L;
        }

        @Override
        protected String recordToLedger(Order order, long totalCents) {
            calls.add("recordToLedger");
            return "recorded:" + order.orderId();
        }
    }
}
