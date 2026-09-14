package lavi.minecraft.task.container.deposit.auto.rearm;

import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.budget.AutoDepositExecutionBudget;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Cancel obsolete working-set debt without forgiving failed or stalled storage.
class AutoDepositPendingCancellationTest {
    @Test
    void controlCancellationDoesNotCountAStorageFailureOrResetConsumedWork() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        AutoDepositExecutionBudget budget = policy.beginUnit(pressure(36), "same");
        budget.onExecutionTick(false);
        policy.suspend();
        long episode = policy.episodeSequence();
        policy.cancelPendingUnit("STOPPED");
        assertFalse(policy.hasPendingUnit());
        assertNull(policy.logicalStart());
        assertEquals(episode, policy.episodeSequence());
        assertEquals(1, budget.consumedExecutionTicks());
        assertEquals(1, budget.consecutiveNoProgressTicks());
        assertEquals(0, budget.completedUnits());
        assertEquals(0, budget.recoveryGrants());
        // Execution-control gating remains outside this pure policy.
        assertSame(budget, policy.beginUnit(pressure(34), "same"));
        assertEquals(34, policy.logicalStart().occupiedSlots());
    }

    @Test
    void replacingARootDoesNotClearItsAlreadyBlockedCondition() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        policy.beginUnit(pressure(36), "before");
        policy.suspend();
        policy.markPlanBlocked("no_safe_surplus", "automatic", "blocked");
        policy.cancelPendingUnit("user_root_replaced");
        assertFalse(policy.hasPendingUnit());
        assertEquals("no_safe_surplus", policy.lastReason());
        assertEquals(AutoDepositRearmDecision.SAME_FAILURE, policy.observe(pressure(36), "blocked"));
        assertEquals(AutoDepositRearmDecision.RELATED_CHANGE, policy.observe(pressure(36), "actual_surplus"));
        assertEquals(0, policy.activeBudget().completedUnits());
    }

    private static DepositAllInventoryPressureSnapshot pressure(int occupied) {
        return new DepositAllInventoryPressureSnapshot(occupied, 36);
    }
}
