package com.patterncatalyst.designpatterns.ch11.warehouse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RestockLoggingTest {

    @Test
    void theLambdaShortcutAppendsTheExpectedReceiptLine() {
        List<String> receiptLines = new ArrayList<>();
        ReceiptLogger logger = RestockLog.appendingTo(receiptLines);

        logger.log(30, "B2");

        assertThat(receiptLines).containsExactly("+30 into B2");
    }

    @Test
    void theFullCommandAndTheLambdaProduceTheIdenticalReceiptLine() {
        List<String> lambdaLines = new ArrayList<>();
        List<String> commandLines = new ArrayList<>();
        ReceiptLogger logger = RestockLog.appendingTo(lambdaLines);
        LogRestockCommand command = new LogRestockCommand(commandLines, 30, "B2");

        logger.log(30, "B2");
        command.execute();

        assertThat(commandLines).isEqualTo(lambdaLines);
    }

    @Test
    void theFullCommandHasNoHonestUndoAndThrowsInstead() {
        LogRestockCommand command = new LogRestockCommand(new ArrayList<>(), 30, "B2");
        command.execute();

        assertThatThrownBy(command::undo).isInstanceOf(UnsupportedOperationException.class);
    }
}
