package lavi.minecraft.diagnostics.mining.gold;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: Keep skipped filters distinct from actual candidate rejection and diagnostic recomputation.
class MiningToolFilterTraceTest {
    @Test
    void recordsOnlyTheBranchesThePolicyActuallyExecuted() {
        MiningToolFilterTrace trace = new MiningToolFilterTrace(true);
        trace.nonPlayerSlot();
        trace.wrongKind();
        trace.savedByPolicy();
        trace.durability(false);
        trace.durability(true);
        assertTrue(trace.snapshot().contains("unsuitable=0"));
        assertTrue(trace.snapshot().contains("savedByPolicy=1"));
        assertTrue(trace.snapshot().contains("durabilityRejected=1,accepted=1"));
        assertTrue(trace.snapshot().contains("ACTUAL_POLICY_BRANCHES"));
        assertTrue(trace.snapshot().contains("NOT_EVALUATED"));
    }

    @Test
    void offCaptureNeverReportsZeroAsAnObservedInventoryFact() {
        MiningToolFilterTrace trace = new MiningToolFilterTrace(false);
        trace.wrongKind();
        trace.durability(true);
        assertEquals("NOT_CAPTURED_MODE_OFF", trace.snapshot());
    }
}
