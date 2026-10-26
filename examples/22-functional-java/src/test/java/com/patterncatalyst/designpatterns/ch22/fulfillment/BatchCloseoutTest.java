package com.patterncatalyst.designpatterns.ch22.fulfillment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class BatchCloseoutTest {

    @Test
    void runInvokesTheSuppliedRunnableExactlyOnce() {
        AtomicBoolean ran = new AtomicBoolean(false);

        BatchCloseout.run(() -> ran.set(true));

        assertThat(ran).isTrue();
    }

    @Test
    void rejectsANullRunnable() {
        assertThatThrownBy(() -> BatchCloseout.run(null)).isInstanceOf(NullPointerException.class);
    }
}
