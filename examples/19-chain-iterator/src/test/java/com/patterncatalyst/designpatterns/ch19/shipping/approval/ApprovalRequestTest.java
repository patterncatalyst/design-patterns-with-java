package com.patterncatalyst.designpatterns.ch19.shipping.approval;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApprovalRequestTest {

    @Test
    void rejectsNegativeWeight() {
        assertThatThrownBy(() -> new ApprovalRequest(-1.0, false, "US"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullDestination() {
        assertThatThrownBy(() -> new ApprovalRequest(10.0, false, null))
                .isInstanceOf(NullPointerException.class);
    }
}
