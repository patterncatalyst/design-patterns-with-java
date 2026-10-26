package com.patterncatalyst.designpatterns.ch20.shipping;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A merchant's in-progress export manifest: a draft name, a growing list of
 * ManifestEntry line items, and a submitted flag flipped once at the end of
 * an editing session. This is the Originator half of Memento.
 *
 * <p>{@link Snapshot} is the Memento half. It is a package-private record,
 * so neither its canonical constructor nor its accessors are visible
 * outside this package; {@link #save()} and {@link #restore(Snapshot)} are
 * the only way to produce or apply one. A caller in another package can
 * still hold the value a save() call returns, through {@code var}, without
 * ever being able to write out the type {@code ManifestDraft.Snapshot} or
 * read a field off it directly. Every caller in this example happens to
 * live in this same package, so the compiler does not force that
 * discipline on this file, but a real admin-screen controller living in
 * its own package gets it for free the moment it assigns a save() result
 * to a {@code var} instead of a named type.
 */
public final class ManifestDraft {

    private String draftName;
    private final List<ManifestEntry> entries = new ArrayList<>();
    private boolean submitted;

    public ManifestDraft(String draftName) {
        this.draftName = Objects.requireNonNull(draftName, "draftName");
    }

    public String draftName() {
        return draftName;
    }

    public List<ManifestEntry> entries() {
        return List.copyOf(entries);
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public void rename(String newName) {
        this.draftName = Objects.requireNonNull(newName, "newName");
    }

    public void addEntry(ManifestEntry entry) {
        entries.add(Objects.requireNonNull(entry, "entry"));
    }

    public void removeLastEntry() {
        if (!entries.isEmpty()) {
            entries.remove(entries.size() - 1);
        }
    }

    public void submit() {
        this.submitted = true;
    }

    /** A whole-state capture of this draft at the moment {@link #save()} was called. */
    record Snapshot(String draftName, List<ManifestEntry> entries, boolean submitted) {
    }

    /**
     * Captures the whole state of this draft in one call. {@code entries} is
     * copied with {@link List#copyOf}, not aliased, so later edits to this
     * draft's own list cannot be observed through a held Snapshot; no deep
     * clone is needed beyond that copy because every ManifestEntry is
     * already an immutable record.
     */
    public Snapshot save() {
        return new Snapshot(draftName, List.copyOf(entries), submitted);
    }

    /**
     * Replaces this draft's whole state with whatever {@code snapshot}
     * captured, in one call, regardless of how many rename(), addEntry(),
     * removeLastEntry(), or submit() calls happened since save() produced
     * it.
     */
    public void restore(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        this.draftName = snapshot.draftName();
        this.entries.clear();
        this.entries.addAll(snapshot.entries());
        this.submitted = snapshot.submitted();
    }
}
