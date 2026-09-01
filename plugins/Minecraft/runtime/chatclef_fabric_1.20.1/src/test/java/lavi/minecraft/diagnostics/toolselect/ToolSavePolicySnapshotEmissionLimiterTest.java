package lavi.minecraft.diagnostics.toolselect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolSavePolicySnapshotEmissionLimiterTest {
    @Test
    void repeatStateMapNeverExceedsTheFixedMaximum() {
        ToolSavePolicySnapshotEmissionLimiter limiter =
                new ToolSavePolicySnapshotEmissionLimiter();

        for (int index = 0; index <= ToolSavePolicySnapshotEmissionLimiter.MAX_REPEAT_STATES; index++) {
            assertTrue(limiter.evaluate("key-" + index, index).emitEvent);
        }

        assertEquals(
                ToolSavePolicySnapshotEmissionLimiter.MAX_REPEAT_STATES,
                limiter.repeatStateCount()
        );
    }

    @Test
    void tickRegressionCannotTriggerAPrematureRepeatSummary() {
        ToolSavePolicySnapshotEmissionLimiter limiter =
                new ToolSavePolicySnapshotEmissionLimiter();

        assertTrue(limiter.evaluate("same", 100L).emitEvent);
        ToolSavePolicySnapshotEmissionLimiter.Decision regressed =
                limiter.evaluate("same", 20L);
        assertFalse(regressed.emitSummary);

        ToolSavePolicySnapshotEmissionLimiter.Decision summary =
                limiter.evaluate("same", 300L);
        assertTrue(summary.emitSummary);
        assertEquals(2, summary.suppressedRepeatCount);
        assertEquals(100L, summary.firstObservedTick);
        assertEquals(300L, summary.lastObservedTick);
    }

    @Test
    void boundedRepeatKeysStillDistinguishLongSemanticChanges() {
        ToolSavePolicySnapshotEmissionLimiter limiter =
                new ToolSavePolicySnapshotEmissionLimiter();
        String sharedPrefix = "x".repeat(500);

        assertTrue(limiter.evaluate(sharedPrefix + "iron", 1L).emitEvent);
        assertTrue(limiter.evaluate(sharedPrefix + "diamond", 2L).emitEvent);
        assertEquals(2, limiter.repeatStateCount());
    }

    @Test
    void modeOffClearDropsRepeatStateAndRestoresTheLocalEmissionBudget() {
        ToolSavePolicySnapshotEmissionLimiter limiter =
                new ToolSavePolicySnapshotEmissionLimiter();

        for (int index = 0;
             index < ToolSavePolicySnapshotEmissionLimiter.SESSION_HARD_CAP;
             index++) {
            assertTrue(limiter.evaluate("pre-off-" + index, index).emitEvent);
        }
        assertTrue(limiter.evaluate("cap", 5_000L).emitCap);
        assertFalse(limiter.evaluate("cap-again", 5_001L).emitCap);

        limiter.clearForModeOff();

        assertEquals(0, limiter.repeatStateCount());
        assertTrue(limiter.evaluate("pre-off-0", 5_002L).emitEvent);
    }

    @Test
    void subsystemLocalCapEventDoesNotClaimTheCanonicalSessionCapName() {
        assertNotEquals(
                "DIAGNOSTIC_SESSION_CAP_REACHED",
                ToolSavePolicySnapshotDiagnostics.LOCAL_CAP_EVENT
        );
    }
}
