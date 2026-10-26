package com.patterncatalyst.designpatterns.ch28.shipping;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A shared, injectable sink for {@link OperationLoggingInterceptor}, so a
 * test can observe that the interceptor ran without parsing console
 * output.
 */
@ApplicationScoped
public class InvocationLog {

    private final List<String> entries = new CopyOnWriteArrayList<>();

    public void record(String entry) {
        entries.add(entry);
    }

    public List<String> entries() {
        return List.copyOf(entries);
    }
}
