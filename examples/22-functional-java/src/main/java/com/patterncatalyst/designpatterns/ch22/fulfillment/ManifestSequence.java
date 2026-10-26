package com.patterncatalyst.designpatterns.ch22.fulfillment;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The warehouse's one manifest counter, expressed as the idiomatic Java
 * enum Singleton: a single constant, {@code INSTANCE}, is the entire set of
 * values this type can ever hold. Unlike a class with a private constructor
 * and a static {@code getInstance()} method, there is no second constructor
 * path a caller, a deserializer, or reflection can use to produce a second
 * counter; the language itself guarantees exactly one instance exists for
 * the life of the program.
 */
public enum ManifestSequence {

    INSTANCE;

    private final AtomicLong issuedCount = new AtomicLong();

    public String nextManifestId() {
        return "MANIFEST-%06d".formatted(issuedCount.incrementAndGet());
    }

    public long issuedCount() {
        return issuedCount.get();
    }
}
