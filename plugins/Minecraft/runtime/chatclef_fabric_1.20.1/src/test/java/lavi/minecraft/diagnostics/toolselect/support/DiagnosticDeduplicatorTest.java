package lavi.minecraft.diagnostics.toolselect.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticDeduplicatorTest {
    @Test
    void mutableFingerprintStateNeverExceedsTheFixedMaximum() {
        DiagnosticDeduplicator deduplicator = new DiagnosticDeduplicator();

        for (int index = 0; index <= DiagnosticDeduplicator.MAX_TRACKED_KEYS; index++) {
            assertTrue(deduplicator.shouldEmit("key-" + index, "fingerprint-" + index));
        }

        assertEquals(DiagnosticDeduplicator.MAX_TRACKED_KEYS, deduplicator.trackedKeyCount());
        assertTrue(deduplicator.shouldEmit("key-0", "fingerprint-0"));
        assertEquals(DiagnosticDeduplicator.MAX_TRACKED_KEYS, deduplicator.trackedKeyCount());
    }

    @Test
    void unchangedFingerprintIsSuppressedAndMeaningfulChangeEmits() {
        DiagnosticDeduplicator deduplicator = new DiagnosticDeduplicator();

        assertTrue(deduplicator.shouldEmit("selection", "iron"));
        assertFalse(deduplicator.shouldEmit("selection", "iron"));
        assertTrue(deduplicator.shouldEmit("selection", "diamond"));
    }

    @Test
    void modeOffClearPreventsTheOldFingerprintFromRevivingAfterReenable() {
        DiagnosticDeduplicator deduplicator = new DiagnosticDeduplicator();

        assertTrue(deduplicator.shouldEmit("selection", "iron"));
        assertFalse(deduplicator.shouldEmit("selection", "iron"));

        deduplicator.clearForModeOff();

        assertEquals(0, deduplicator.trackedKeyCount());
        assertTrue(deduplicator.shouldEmit("selection", "iron"));
    }

    @Test
    void boundedStorageStillDistinguishesLongFingerprintChanges() {
        DiagnosticDeduplicator deduplicator = new DiagnosticDeduplicator();
        String sharedPrefix = "x".repeat(500);

        assertTrue(deduplicator.shouldEmit("selection", sharedPrefix + "iron"));
        assertTrue(deduplicator.shouldEmit("selection", sharedPrefix + "diamond"));
    }
}
