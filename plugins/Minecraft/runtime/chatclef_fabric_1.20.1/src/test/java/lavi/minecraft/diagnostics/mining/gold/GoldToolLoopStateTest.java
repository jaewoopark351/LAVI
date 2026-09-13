package lavi.minecraft.diagnostics.mining.gold;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: Verify causal evidence survives more child restarts than the recent-history capacity.
class GoldToolLoopStateTest {
    @Test
    void eightyChildInterruptionsPreserveFirstCausalLinkAndCumulativeCounts() {
        GoldToolLoopState state = new GoldToolLoopState();
        state.preparation(true, "READY", 0, 1);
        String first = null;
        for (int run = 1; run <= 80; run++) {
            Object child = new Object();
            state.childStarted(child, "940,11,-1816");
            state.equip(run, "shovel_displaced_access_pickaxe_" + run);
            assertEquals("READY_TO_PREPARATION", state.preparation(false, "MOVE_ACCESS_PICKAXE_TO_HOTBAR", 0, run * 2L));
            assertTrue(state.childStopped(child, true, "childRun=" + run));
            assertFalse(state.childStopped(child, true, "duplicate_stop"));
            state.preparation(true, "READY", 0, run * 2L + 1);
            if (first == null) first = state.firstCausalLink();
            assertEquals(first, state.firstCausalLink());
        }
        assertEquals(80, state.preparationReentries());
        assertEquals(80, state.preparationInterruptions());
        assertEquals(80, state.childRuns());
        assertEquals(80L, field(state.fields(200), "sameTargetConsecutivePreparationInterruptions"));
        assertTrue(state.firstCausalLink().contains("equipAttempt=1"));
    }

    @Test
    void delayedOtherEquipCannotRewriteThePreparationDecisionEvidence() {
        GoldToolLoopState state = new GoldToolLoopState();
        Object child = new Object();
        state.childStarted(child);
        state.preparation(true, "READY", 0, 1);
        state.equip(11, "DIAMOND_SHOVEL");
        state.preparation(false, "MOVE_ACCESS_PICKAXE_TO_HOTBAR", 0, 2);
        state.equip(12, "UNRELATED_LATER_EQUIP");
        state.childStopped(child, true, "stop");
        assertTrue(state.firstCausalLink().contains("DIAMOND_SHOVEL"));
        assertFalse(state.firstCausalLink().contains("UNRELATED_LATER_EQUIP"));
    }

    @Test
    void otherChildAndNonPreparationStopsDoNotFabricateTheCausalLink() {
        GoldToolLoopState state = new GoldToolLoopState();
        Object child = new Object();
        state.childStarted(child);
        assertFalse(state.childStopped(new Object(), true, "stale_child"));
        assertTrue(state.childStopped(child, false, "automatic_defense_interrupt"));
        assertEquals("UNAVAILABLE", state.firstCausalLink());
        assertEquals(0, state.preparationInterruptions());
    }

    @Test
    void newScopeHasNoPreviousRequestEvidenceAndQuantityChangesRemainUnattributed() {
        GoldToolLoopState previous = new GoldToolLoopState();
        previous.preparation(true, "READY", 2, 3);
        previous.preparation(true, "READY", 5, 4);
        Object[] fields = previous.fields(8);
        assertEquals(3, field(fields, "rawGoldSignedQuantityChange"));
        assertEquals("UNVERIFIED_SOURCE_DROP_PICKUP", field(fields, "quantityChangeAttribution"));
        GoldToolLoopState next = new GoldToolLoopState();
        assertEquals(0, next.childRuns());
        assertEquals(-1, next.latestEquipAttempt());
        assertEquals("UNAVAILABLE", next.firstCausalLink());
    }

    private static Object field(Object[] fields, String key) {
        for (int i = 0; i < fields.length; i += 2) if (key.equals(fields[i])) return fields[i + 1];
        throw new AssertionError(key);
    }
}
