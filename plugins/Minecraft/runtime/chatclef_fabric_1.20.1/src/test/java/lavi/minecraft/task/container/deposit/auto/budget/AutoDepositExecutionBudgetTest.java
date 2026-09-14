package lavi.minecraft.task.container.deposit.auto.budget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Exercise real work limits independently of root registration and wall time.
class AutoDepositExecutionBudgetTest {
    @Test
    void actualNoProgressAccumulatesAcrossArbitrarySafetyWaits() {
        AutoDepositExecutionBudget budget = new AutoDepositExecutionBudget(20, 4, 3, 2, 10);
        budget.onExecutionTick(false);
        budget.onExecutionTick(false);
        // Safety waiting performs no accounting calls, no matter how long it lasts.
        for (int observation = 0; observation < 1000; observation++) {
            assertEquals(AutoDepositBudgetStatus.AVAILABLE, budget.status());
        }
        assertEquals(AutoDepositBudgetStatus.AVAILABLE, budget.onExecutionTick(false));
        assertEquals(AutoDepositBudgetStatus.NO_PROGRESS_LIMIT, budget.onExecutionTick(false));
        assertEquals(4, budget.consumedExecutionTicks());
        assertEquals(4, budget.cumulativeNoProgressTicks());
        assertEquals(0, budget.completedUnits());
    }

    @Test
    void genuineProgressClearsOnlyCurrentStallWindow() {
        AutoDepositExecutionBudget budget = new AutoDepositExecutionBudget(6, 4, 3, 0, 1);
        budget.onExecutionTick(false);
        budget.onExecutionTick(false);
        budget.onExecutionTick(true);
        assertEquals(0, budget.consecutiveNoProgressTicks());
        assertEquals(2, budget.cumulativeNoProgressTicks());
        budget.onExecutionTick(true);
        budget.onExecutionTick(true);
        assertEquals(AutoDepositBudgetStatus.EXECUTION_TICK_LIMIT, budget.onExecutionTick(true));
        assertEquals(6, budget.consumedExecutionTicks());
    }

    @Test
    void recoveryExtendsFiniteAllowanceWithoutErasingConsumedWork() {
        AutoDepositExecutionBudget budget = new AutoDepositExecutionBudget(3, 3, 1, 2, 2);
        for (int tick = 0; tick < 3; tick++) budget.onExecutionTick(false);
        budget.completeUnit();
        assertTrue(budget.grantRelatedChange());
        assertEquals(3, budget.consumedExecutionTicks());
        assertEquals(3, budget.cumulativeNoProgressTicks());
        assertEquals(0, budget.consecutiveNoProgressTicks());
        assertEquals(1, budget.completedUnits());
        assertEquals(5, budget.maxExecutionTicks());
        budget.onExecutionTick(true);
        budget.onExecutionTick(true);
        assertTrue(budget.grantRelatedChange());
        assertEquals(7, budget.maxExecutionTicks());
        assertFalse(budget.grantRelatedChange());
        assertEquals(2, budget.recoveryGrants());
    }

    @Test
    void eachCompletedLogicalUnitConsumesOneFollowupSlot() {
        AutoDepositExecutionBudget budget = new AutoDepositExecutionBudget();
        budget.completeUnit();
        budget.completeUnit();
        assertEquals(AutoDepositBudgetStatus.AVAILABLE, budget.status());
        budget.completeUnit();
        assertEquals(AutoDepositBudgetStatus.FOLLOWUP_LIMIT, budget.status());
        assertTrue(budget.grantRelatedChange());
        assertEquals(AutoDepositBudgetStatus.AVAILABLE, budget.status());
    }

    @Test
    void rejectsInvalidLimits() {
        assertThrows(IllegalArgumentException.class,
                () -> new AutoDepositExecutionBudget(0, 1, 1, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new AutoDepositExecutionBudget(1, 0, 1, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new AutoDepositExecutionBudget(1, 1, 0, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new AutoDepositExecutionBudget(1, 1, 1, -1, 1));
    }

    @Test
    void finalChildTransferClearsOnlyStallWindowWithoutAnotherTick() {
        AutoDepositExecutionBudget budget = new AutoDepositExecutionBudget(10, 3, 3, 2, 5);
        budget.onExecutionTick(false);
        budget.onExecutionTick(false);
        budget.observeConfirmedProgress();
        assertEquals(0, budget.consecutiveNoProgressTicks());
        assertEquals(2, budget.cumulativeNoProgressTicks());
        assertEquals(2, budget.consumedExecutionTicks());
        assertEquals(0, budget.completedUnits());
        assertEquals(0, budget.recoveryGrants());
        budget.onExecutionTick(false);
        assertEquals(1, budget.consecutiveNoProgressTicks());
        assertEquals(3, budget.consumedExecutionTicks());
    }
}
