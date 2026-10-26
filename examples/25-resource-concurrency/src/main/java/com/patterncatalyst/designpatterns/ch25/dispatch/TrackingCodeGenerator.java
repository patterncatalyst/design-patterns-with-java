package com.patterncatalyst.designpatterns.ch25.dispatch;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Computes a short tracking code from a parcel id by hashing it.
 *
 * <p>{@link MessageDigest} is neither thread-safe nor cheap to obtain
 * through {@link MessageDigest#getInstance}, so a lock around one shared
 * instance would serialize every caller, and resolving a fresh instance on
 * every call pays the provider lookup repeatedly. A {@link ThreadLocal}
 * confines one {@code MessageDigest} per calling thread instead: each
 * thread reuses its own instance across calls, with no lock and no
 * per-call provider lookup.
 *
 * <p>That confinement has a cost this chapter's body discusses: a
 * {@code ThreadLocal} retains its value for as long as the thread lives. A
 * bounded worker pool amortizes that retained instance across many calls.
 * A virtual thread created fresh per task gets a brand-new
 * {@code MessageDigest} that is used once and then discarded along with the
 * thread, which pays the allocation this class exists to avoid and keeps
 * nothing to show for it.
 */
public final class TrackingCodeGenerator {

    private static final HexFormat HEX = HexFormat.of();

    private final ThreadLocal<MessageDigest> digestPerThread = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is a mandatory JDK algorithm", e);
        }
    });

    public String trackingCodeFor(String parcelId) {
        MessageDigest digest = digestPerThread.get();
        byte[] hash = digest.digest(parcelId.getBytes(StandardCharsets.UTF_8));
        return HEX.formatHex(hash, 0, 5);
    }
}
