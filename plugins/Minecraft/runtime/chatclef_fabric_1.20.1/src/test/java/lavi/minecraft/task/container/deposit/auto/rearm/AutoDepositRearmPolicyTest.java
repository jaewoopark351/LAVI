package lavi.minecraft.task.container.deposit.auto.rearm;

import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.budget.AutoDepositBudgetStatus;
import lavi.minecraft.task.container.deposit.auto.budget.AutoDepositExecutionBudget;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Verify capture-before-refill, defense continuity and finite scoped recovery.
class AutoDepositRearmPolicyTest {
    @Test
    void capturedThirtyAllowsNextThirtyThreeWithoutAnIntermediateObservation() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        AutoDepositExecutionBudget first = policy.beginUnit(pressure(33), "same");
        policy.finishUnit(true, pressure(30), "normal", "automatic", "same");
        long episode = policy.episodeSequence();
        for (int repeat = 0; repeat < 100; repeat++) {
            assertEquals(AutoDepositRearmDecision.PRESSURE_RISE, policy.observe(pressure(33), "same"));
        }
        assertEquals(episode, policy.episodeSequence());
        assertNotSame(first, policy.beginUnit(pressure(33), "same"));
        assertEquals(episode + 1, policy.episodeSequence());
        assertEquals(AutoDepositRearmDecision.RUNNING, policy.observe(pressure(33), "same"));
    }

    @Test
    void normalLowWaterStartsFreshAfterPriorFollowupsWereConsumed() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        AutoDepositExecutionBudget old = policy.beginUnit(pressure(36), "condition");
        policy.finishUnit(true, pressure(35), "normal", "automatic", "condition");
        policy.beginUnit(pressure(35), "condition");
        policy.finishUnit(true, pressure(34), "normal", "automatic", "condition");
        policy.beginUnit(pressure(34), "condition");
        policy.finishUnit(true, pressure(28), "normal", "automatic", "condition");
        assertEquals(AutoDepositBudgetStatus.FOLLOWUP_LIMIT, old.status());
        AutoDepositExecutionBudget fresh = policy.beginUnit(pressure(33), "condition");
        assertNotSame(old, fresh);
        assertEquals(0, fresh.completedUnits());
    }

    @Test
    void failureThenLowWaterDoesNotEraseFailureOrConsumedWork() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        AutoDepositExecutionBudget budget = policy.beginUnit(pressure(33), "full-chest", "chest-a");
        budget.onExecutionTick(false);
        policy.finishUnit(false, pressure(33), "container_full", "chest-a", "full-chest");
        assertEquals(AutoDepositRearmDecision.BELOW_THRESHOLD,
                policy.observe(pressure(28), "full-chest", "chest-a"));
        assertEquals(AutoDepositRearmDecision.SAME_FAILURE,
                policy.observe(pressure(33), "full-chest", "chest-a"));
        assertEquals(1, budget.completedUnits());
        assertEquals(1, budget.consumedExecutionTicks());
    }

    @Test
    void repeatedDefenseDoesNotConsumeUnitsOrEraseOriginalStart() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        AutoDepositExecutionBudget budget = policy.beginUnit(pressure(36), "initial");
        budget.onExecutionTick(false);
        for (int repeat = 0; repeat < 100; repeat++) {
            policy.suspend();
            assertEquals(AutoDepositRearmDecision.RESUME, policy.observe(pressure(32), "current"));
            assertSame(budget, policy.beginUnit(pressure(32), "current"));
            assertEquals(36, policy.logicalStart().occupiedSlots());
        }
        assertEquals(0, budget.completedUnits());
        assertEquals(1, budget.consumedExecutionTicks());
        assertEquals(1, budget.consecutiveNoProgressTicks());
        assertEquals(0, budget.recoveryGrants());
        policy.finishUnit(true, pressure(32), "normal", "automatic", "current");
        assertEquals(AutoDepositRearmDecision.PRESSURE_RISE, policy.observe(pressure(33), "current"));
    }

    @Test
    void successorDoesNotReusePrecedingReliefAndHighPressureDoesNotResetLimits() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        AutoDepositExecutionBudget budget = policy.beginUnit(pressure(36), "same");
        policy.finishUnit(true, pressure(34), "normal", "automatic", "same");
        assertSame(budget, policy.beginUnit(pressure(34), "same"));
        assertEquals(34, policy.logicalStart().occupiedSlots());
        policy.finishUnit(true, pressure(34), "normal", "automatic", "same");
        assertEquals("no_slot_relief", policy.lastReason());
        assertEquals(AutoDepositRearmDecision.SAME_FAILURE, policy.observe(pressure(34), "same"));
        assertEquals(2, budget.completedUnits());
    }

    @Test
    void continuousPartialReliefStopsAtTheCompletedUnitLimit() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        for (int unit = 0; unit < 3; unit++) {
            policy.beginUnit(pressure(36), "same");
            policy.finishUnit(true, pressure(34), "normal", "automatic", "same");
        }
        assertFalse(policy.observe(pressure(36), "same").canEvaluate());
        assertEquals(3, policy.activeBudget().completedUnits());
        assertEquals("normal", policy.lastReason());
        assertEquals(AutoDepositBudgetStatus.FOLLOWUP_LIMIT, policy.activeBudget().status());
    }

    @Test
    void unrelatedIdentitiesCannotRecoverFailureAndConditionOscillationDoesNotRecharge() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        AutoDepositExecutionBudget budget = policy.beginUnit(pressure(36), "condition-a");
        policy.finishUnit(false, pressure(36), "no_progress", "automatic", "condition-a");
        // Root/hand/slot/defense identity is deliberately absent from the gameplay condition API.
        for (int root = 0; root < 100; root++) {
            policy.suspend();
            assertEquals(AutoDepositRearmDecision.SAME_FAILURE, policy.observe(pressure(36), "condition-a"));
        }
        policy.beginUnit(pressure(36), "condition-b");
        policy.finishUnit(false, pressure(36), "no_progress", "automatic", "condition-b");
        assertEquals(1, budget.recoveryGrants());
        assertEquals(AutoDepositRearmDecision.SAME_FAILURE, policy.observe(pressure(36), "condition-a"));
        assertEquals(AutoDepositRearmDecision.SAME_FAILURE, policy.observe(pressure(36), "condition-b"));
        policy.beginUnit(pressure(36), "condition-c");
        policy.finishUnit(false, pressure(36), "no_progress", "automatic", "condition-c");
        assertEquals(AutoDepositRearmDecision.BUDGET_EXHAUSTED, policy.observe(pressure(36), "condition-d"));
        assertEquals(2, budget.recoveryGrants());
    }

    @Test
    void newDestinationUsesRemainingBudgetAndOldFailureSurvivesNormalRenewal() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        AutoDepositExecutionBudget old = policy.beginUnit(pressure(36), "blocked-a", "chest-a");
        policy.finishUnit(false, pressure(36), "access_failed", "chest-a", "blocked-a");
        assertEquals(AutoDepositRearmDecision.RELATED_CHANGE,
                policy.observe(pressure(36), "usable-b", "chest-b"));
        assertSame(old, policy.beginUnit(pressure(36), "usable-b", "chest-b"));
        assertEquals(0, old.recoveryGrants());
        policy.finishUnit(true, pressure(30), "normal", "chest-b", "usable-b");
        assertEquals(AutoDepositRearmDecision.SAME_FAILURE,
                policy.observe(pressure(36), "blocked-a", "chest-a"));
        assertEquals(AutoDepositRearmDecision.PRESSURE_RISE,
                policy.observe(pressure(36), "usable-b", "chest-b"));
        policy.beginUnit(pressure(36), "usable-b", "chest-b");
        policy.finishUnit(false, pressure(36), "access_failed", "chest-b", "usable-b");
        assertEquals(AutoDepositRearmDecision.SAME_FAILURE,
                policy.observe(pressure(36), "blocked-a", "chest-a"));
    }

    @Test
    void unavailableAndMismatchedSnapshotsNeverCreateNormalRelief() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        assertEquals(AutoDepositRearmDecision.UNKNOWN_PRESSURE, policy.observe(null, "condition"));
        policy.beginUnit(pressure(36), "condition");
        policy.finishUnit(true, null, "normal", "automatic", "condition");
        assertEquals("pressure_unavailable_or_scope_mismatch", policy.lastReason());
        assertEquals(AutoDepositRearmDecision.RELATED_CHANGE, policy.observe(pressure(33), "condition"));
        assertEquals(1, policy.activeBudget().completedUnits());
        policy.resetContext();
        policy.beginUnit(pressure(36), "condition");
        policy.finishUnit(true, new DepositAllInventoryPressureSnapshot(10, 27),
                "normal", "automatic", "condition");
        assertEquals(AutoDepositRearmDecision.RELATED_CHANGE, policy.observe(pressure(33), "condition"));
        assertEquals(1, policy.activeBudget().completedUnits());
    }

    @Test
    void partialSuccessCannotSendFollowupBackToAnUnchangedFailedDestination() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        policy.beginUnit(pressure(36), "blocked-a", "chest-a");
        policy.finishUnit(false, pressure(36), "access_failed", "chest-a", "blocked-a");
        policy.beginUnit(pressure(36), "usable-b", "chest-b");
        policy.finishUnit(true, pressure(34), "normal", "chest-b", "usable-b");
        assertEquals(AutoDepositRearmDecision.SAME_FAILURE,
                policy.observe(pressure(34), "blocked-a", "chest-a"));
        assertEquals(AutoDepositRearmDecision.CONTINUE,
                policy.observe(pressure(34), "usable-b", "chest-b"));
    }

    @Test
    void invalidTerminalMetadataDoesNotConsumeAUnitOrLosePendingWork() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        policy.beginUnit(pressure(36), "condition");
        assertThrows(NullPointerException.class,
                () -> policy.finishUnit(true, pressure(30), null, "automatic", "condition"));
        assertThrows(NullPointerException.class,
                () -> policy.finishUnit(true, pressure(30), "normal", null, "condition"));
        assertEquals(0, policy.activeBudget().completedUnits());
        assertTrue(policy.hasPendingUnit());
    }

    @Test
    void failedPlanningIsBoundedAndDoesNotFabricateCompletedExecution() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        policy.markPlanBlocked("no_safe_surplus", "automatic", "a");
        assertEquals(AutoDepositRearmDecision.SAME_FAILURE, policy.observe(pressure(36), "a"));
        assertEquals(AutoDepositRearmDecision.RELATED_CHANGE, policy.observe(pressure(36), "b"));
        policy.markPlanBlocked("no_safe_surplus", "automatic", "b");
        policy.markPlanBlocked("no_safe_surplus", "automatic", "c");
        assertEquals(AutoDepositRearmDecision.BUDGET_EXHAUSTED, policy.observe(pressure(36), "d"));
        assertEquals(0, policy.activeBudget().completedUnits());
        assertEquals(0, policy.activeBudget().consumedExecutionTicks());
        assertEquals(2, policy.activeBudget().recoveryGrants());
    }

    @Test
    void normalRenewalWithBlockedNewPlanStartsOnlyOneFreshEpisode() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        policy.beginUnit(pressure(36), "old");
        policy.finishUnit(true, pressure(30), "normal", "automatic", "old");
        long sequence = policy.episodeSequence();
        policy.markPlanBlocked("no_safe_surplus", "automatic", "new");
        assertEquals(sequence + 1, policy.episodeSequence());
        assertEquals(0, policy.activeBudget().recoveryGrants());
        policy.markPlanBlocked("no_safe_surplus", "automatic", "new");
        assertEquals(sequence + 1, policy.episodeSequence());
        assertEquals(AutoDepositRearmDecision.SAME_FAILURE, policy.observe(pressure(36), "new"));
    }

    @Test
    void blockedResumeKeepsUnitBaselineWhenARelevantConditionLaterImproves() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        AutoDepositExecutionBudget budget = policy.beginUnit(pressure(36), "before");
        policy.suspend();
        policy.markPlanBlocked("destination_full", "automatic", "full");
        assertTrue(policy.hasPendingUnit());
        assertSame(budget, policy.beginUnit(pressure(32), "space_opened"));
        assertEquals(36, policy.logicalStart().occupiedSlots());
        assertEquals(0, budget.completedUnits());
        policy.finishUnit(true, pressure(32), "normal", "automatic", "space_opened");
        assertEquals(1, budget.completedUnits());
    }

    @Test
    void repeatedTerminalAndReadsDoNotSpendAdditionalBudget() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        policy.beginUnit(pressure(33), "condition");
        policy.finishUnit(false, pressure(33), "failed", "automatic", "condition");
        policy.finishUnit(true, pressure(28), "late", "automatic", "condition");
        assertEquals(1, policy.activeBudget().completedUnits());
        assertEquals("failed", policy.lastReason());
        assertEquals(AutoDepositRearmDecision.SAME_FAILURE, policy.observe(pressure(33), "condition"));
        assertEquals(AutoDepositRearmDecision.UNKNOWN_CONDITION, policy.observe(pressure(33), null));
    }

    private static DepositAllInventoryPressureSnapshot pressure(int occupied) {
        return new DepositAllInventoryPressureSnapshot(occupied, 36);
    }
}
