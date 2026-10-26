package com.patterncatalyst.designpatterns.ch20.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ManifestDraftTest {

    private final ManifestEntry firstEntry = new StandardParcel("PCL-1", Money.ofCents(500), 3.0);
    private final ManifestEntry secondEntry = new StandardParcel("PCL-2", Money.ofCents(900), 4.0);

    @Test
    void restoreReplacesWholeStateEvenAfterSeveralEdits() {
        ManifestDraft draft = new ManifestDraft("Q4-export-batch");
        draft.addEntry(firstEntry);

        var checkpoint = draft.save();

        draft.rename("DRAFT-EDIT");
        draft.addEntry(secondEntry);
        draft.submit();

        assertThat(draft.draftName()).isEqualTo("DRAFT-EDIT");
        assertThat(draft.entries()).hasSize(2);
        assertThat(draft.isSubmitted()).isTrue();

        draft.restore(checkpoint);

        assertThat(draft.draftName()).isEqualTo("Q4-export-batch");
        assertThat(draft.entries()).containsExactly(firstEntry);
        assertThat(draft.isSubmitted()).isFalse();
    }

    @Test
    void savingTwiceCapturesIndependentSnapshots() {
        ManifestDraft draft = new ManifestDraft("batch-one");
        draft.addEntry(firstEntry);
        var afterFirstEntry = draft.save();

        draft.addEntry(secondEntry);
        var afterSecondEntry = draft.save();

        draft.restore(afterSecondEntry);
        assertThat(draft.entries()).containsExactly(firstEntry, secondEntry);

        draft.restore(afterFirstEntry);
        assertThat(draft.entries()).containsExactly(firstEntry);
    }

    @Test
    void laterMutationOfTheDraftDoesNotLeakIntoAHeldSnapshot() {
        ManifestDraft draft = new ManifestDraft("batch-one");
        draft.addEntry(firstEntry);
        var checkpoint = draft.save();

        draft.addEntry(secondEntry);

        draft.restore(checkpoint);
        assertThat(draft.entries()).containsExactly(firstEntry);
    }

    @Test
    void removeLastEntryDropsTheMostRecentlyAddedEntryOnly() {
        ManifestDraft draft = new ManifestDraft("batch-one");
        draft.addEntry(firstEntry);
        draft.addEntry(secondEntry);

        draft.removeLastEntry();

        assertThat(draft.entries()).containsExactly(firstEntry);
    }

    @Test
    void removeLastEntryOnAnEmptyDraftIsANoOp() {
        ManifestDraft draft = new ManifestDraft("batch-one");

        draft.removeLastEntry();

        assertThat(draft.entries()).isEmpty();
    }

    @Test
    void entriesReturnsAnImmutableView() {
        ManifestDraft draft = new ManifestDraft("batch-one");
        draft.addEntry(firstEntry);

        assertThatThrownBy(() -> draft.entries().add(secondEntry))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
