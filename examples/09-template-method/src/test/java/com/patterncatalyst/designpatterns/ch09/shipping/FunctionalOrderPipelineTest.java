package com.patterncatalyst.designpatterns.ch09.shipping;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Proves the same two claims as {@link OrderProcessingPipelineTest}, against
 * the functional form instead: the fixed sequence calls its three supplied
 * steps in order, and the shared precondition runs before any of them, this
 * time with the steps as mocked collaborators rather than overridden
 * methods.
 */
@ExtendWith(MockitoExtension.class)
class FunctionalOrderPipelineTest {

    private static final Order SAMPLE_ORDER = new Order(
            "ORD-1",
            "Customer",
            List.of(new OrderLine("SKU-1", 2, 1_000)),
            "");

    @Mock
    private ChannelValidator validator;

    @Mock
    private ChannelPricer pricer;

    @Mock
    private ChannelRecorder recorder;

    @Test
    void callsTheSuppliedStepsInTheFixedSkeletonOrder() {
        when(pricer.price(SAMPLE_ORDER)).thenReturn(1_800L);
        when(recorder.record(eq(SAMPLE_ORDER), anyLong())).thenReturn("recorded:ORD-1");
        FunctionalOrderPipeline pipeline = new FunctionalOrderPipeline("test-channel", validator, pricer, recorder);

        ProcessingResult result = pipeline.process(SAMPLE_ORDER);

        InOrder order = inOrder(validator, pricer, recorder);
        order.verify(validator).validate(SAMPLE_ORDER);
        order.verify(pricer).price(SAMPLE_ORDER);
        order.verify(recorder).record(SAMPLE_ORDER, 1_800L);
        assertThat(result.totalCents()).isEqualTo(1_800L);
        assertThat(result.ledgerNote()).isEqualTo("recorded:ORD-1");
    }

    @Test
    void theInvariantPreconditionRunsBeforeAnySuppliedStep() {
        FunctionalOrderPipeline pipeline = new FunctionalOrderPipeline("test-channel", validator, pricer, recorder);
        Order empty = new Order("ORD-2", "Customer", List.of(), "");

        assertThatIllegalArgumentException().isThrownBy(() -> pipeline.process(empty));

        verifyNoInteractions(validator, pricer, recorder);
    }

    @Test
    void rejectsABlankChannelName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new FunctionalOrderPipeline(" ", validator, pricer, recorder));
    }
}
