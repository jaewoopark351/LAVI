package lavi.minecraft.task.container.deposit.auto.rearm;

import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: A successful observation can recover unavailable evidence within finite grants.
class AutoDepositUnavailableRecoveryTest {
    @Test
    void missingConditionCannotAdmitOrResumeAndDoesNotConsumeAnyBudget() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        DepositAllInventoryPressureSnapshot pressure = new DepositAllInventoryPressureSnapshot(36, 36);
        assertEquals(AutoDepositRearmDecision.UNKNOWN_CONDITION, policy.observe(pressure, null));
        assertThrows(IllegalStateException.class, () -> policy.beginUnit(pressure, null));
        policy.beginUnit(pressure, "known");
        policy.activeBudget().onExecutionTick(false);
        policy.suspend();
        assertEquals(AutoDepositRearmDecision.UNKNOWN_CONDITION, policy.observe(pressure, null));
        assertThrows(IllegalStateException.class, () -> policy.beginUnit(pressure, null));
        policy.beginUnit(pressure, "known");
        policy.finishUnit(false, pressure, "WORKING_SET_DEFICIT", "automatic", "original_reservation:deficit");
        int completed = policy.activeBudget().completedUnits();
        for (int observation = 0; observation < 20; observation++) {
            assertEquals(AutoDepositRearmDecision.UNKNOWN_CONDITION, policy.observe(pressure, null));
            assertThrows(IllegalStateException.class, () -> policy.beginUnit(pressure, null));
        }
        assertEquals(completed, policy.activeBudget().completedUnits());
        assertEquals(1, policy.activeBudget().consumedExecutionTicks());
        assertEquals(1, policy.activeBudget().consecutiveNoProgressTicks());
        assertEquals(0, policy.activeBudget().recoveryGrants());
        assertEquals("WORKING_SET_DEFICIT", policy.lastReason());
    }

    @Test
    void validPressureRecheckDoesNotRequireFabricatedInventoryChange() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        DepositAllInventoryPressureSnapshot pressure = new DepositAllInventoryPressureSnapshot(36, 36);
        policy.beginUnit(pressure, "unchanged");
        policy.finishUnit(false, null, "PRESSURE_UNAVAILABLE", "automatic", "unchanged");
        assertEquals(AutoDepositRearmDecision.UNKNOWN_PRESSURE, policy.observe(null, "unchanged"));
        assertFalse(policy.observe(pressure, null).canEvaluate());
        assertEquals(AutoDepositRearmDecision.RELATED_CHANGE, policy.observe(pressure, "unchanged"));
        policy.beginUnit(pressure, "unchanged");
        assertEquals(1, policy.activeBudget().recoveryGrants());
        assertEquals(1, policy.activeBudget().completedUnits());
    }

    @Test
    void repeatedlyUnavailableWorkingObservationCannotRefillRecoveryGrants() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        DepositAllInventoryPressureSnapshot pressure = new DepositAllInventoryPressureSnapshot(36, 36);
        for (int attempt = 0; attempt < 3; attempt++) {
            policy.beginUnit(pressure, "unchanged");
            policy.finishUnit(false, pressure, "WORKING_SET_UNAVAILABLE", "automatic", "unchanged");
        }
        assertEquals(AutoDepositRearmDecision.BUDGET_EXHAUSTED, policy.observe(pressure, "unchanged"));
        assertEquals(2, policy.activeBudget().recoveryGrants());
        assertEquals(3, policy.activeBudget().completedUnits());
    }

    @Test
    void unavailableResultDoesNotEraseAnUnrelatedActualDestinationFailure() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        DepositAllInventoryPressureSnapshot pressure = new DepositAllInventoryPressureSnapshot(36, 36);
        policy.beginUnit(pressure, "full", "a");
        policy.finishUnit(false, pressure, "container_full", "a", "full");
        policy.beginUnit(pressure, "candidate", "b");
        policy.finishUnit(false, null, "PRESSURE_UNAVAILABLE", "b", "candidate");
        assertEquals(AutoDepositRearmDecision.SAME_FAILURE, policy.observe(pressure, "full", "a"));
        assertEquals(AutoDepositRearmDecision.RELATED_CHANGE, policy.observe(pressure, "candidate", "b"));
    }

    @Test
    void missingPressureDoesNotDowngradeAConfirmedChildFailureToQueryRecovery() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        DepositAllInventoryPressureSnapshot pressure = new DepositAllInventoryPressureSnapshot(36, 36);
        policy.beginUnit(pressure, "unchanged", "a");
        policy.finishUnit(false, null, "TRUSTED_CHILD_FAILED:capacity_unavailable", "a", "unchanged");
        assertEquals("TRUSTED_CHILD_FAILED:capacity_unavailable", policy.lastReason());
        assertEquals(AutoDepositRearmDecision.SAME_FAILURE, policy.observe(pressure, "unchanged", "a"));
        assertEquals(0, policy.activeBudget().recoveryGrants());
    }
}
