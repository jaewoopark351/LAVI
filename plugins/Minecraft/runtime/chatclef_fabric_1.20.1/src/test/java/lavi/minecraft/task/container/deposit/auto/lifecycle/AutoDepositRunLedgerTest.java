package lavi.minecraft.task.container.deposit.auto.lifecycle;

import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceOutcome;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceTask;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunReason;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunResult;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositWorkingSetStatus;
import lavi.minecraft.task.container.deposit.auto.rearm.AutoDepositRearmDecision;
import lavi.minecraft.task.container.deposit.auto.rearm.AutoDepositRearmPolicy;
import lavi.minecraft.testsupport.TestObjects;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Map;
import java.util.List;
import lavi.minecraft.task.container.deposit.auto.admission.conditions.AutoDepositConditions;

import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Exercise cross-root correlation, logical baselines and actual selected-tick accounting.
class AutoDepositRunLedgerTest {
    @Test void unavailableTerminalObservationUsesAdmissionFactsInTheSameFailureFamily() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        AutoDepositRunLedger ledger = new AutoDepositRunLedger(policy);
        AutoDepositConditions facts = new AutoDepositConditions("automatic", Map.of("cobblestone", 64),
                Map.of(), Map.of(), List.of("general:A:cachedEmpty=0"));
        AutoDepositMaintenanceTask root = task();
        ledger.begin(root, pressure(36), facts.conditionKey("initial"), facts.scope(), facts);
        ledger.settle(root, result(AutoDepositRunReason.GENERAL_CHILD_UNCONFIRMED, 36, 36), null);
        assertEquals(AutoDepositRearmDecision.SAME_FAILURE,
                policy.observe(pressure(36), facts.conditionKey(policy.lastReason()), facts.scope()));
    }

    @Test void explicitStopAndRootReplacementPreserveWorkConsumptionWithoutInventingFailure() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        AutoDepositRunLedger ledger = new AutoDepositRunLedger(policy);
        AutoDepositMaintenanceTask first = task();
        ledger.begin(first, pressure(36), "items", "chestA");
        ledger.executionTick(first);
        ledger.settle(first, result(AutoDepositRunReason.STOPPED, 36, 36), "items");
        assertFalse(policy.hasPendingUnit());
        assertEquals(0, policy.activeBudget().completedUnits());
        AutoDepositMaintenanceTask next = task();
        ledger.begin(next, pressure(36), "items", "chestA");
        ledger.executionTick(next);
        ledger.settle(next, result(AutoDepositRunReason.CONTEXT_CHANGED, 36, 36), "items", true);
        assertEquals(2, policy.activeBudget().consecutiveNoProgressTicks());
        assertEquals(0, policy.activeBudget().completedUnits());
        assertEquals(0, policy.activeBudget().recoveryGrants());
    }
    @Test void capturedTerminalReliefSurvivesRefillBeforeNextObservation() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        AutoDepositRunLedger ledger = new AutoDepositRunLedger(policy);
        AutoDepositMaintenanceTask task = task();
        ledger.begin(task, pressure(33), "items", "chestA");
        assertTrue(ledger.settle(task, result(AutoDepositRunReason.NORMAL, 33, 30), "items"));
        assertEquals(AutoDepositRearmDecision.PRESSURE_RISE, policy.observe(pressure(33), "items", "chestA"));
    }

    @Test void resumedVerificationUsesOriginalReliefAndCountsOneLogicalCompletion() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        AutoDepositRunLedger ledger = new AutoDepositRunLedger(policy);
        AutoDepositMaintenanceTask first = task();
        ledger.begin(first, pressure(36), "items", "chestA");
        ledger.executionTick(first);
        ledger.settle(first, result(AutoDepositRunReason.SAFETY_INTERRUPTED, 36, 32), "items");
        assertEquals(0, policy.activeBudget().completedUnits());
        AutoDepositMaintenanceTask resumed = task();
        ledger.begin(resumed, pressure(32), "items", "chestA");
        ledger.executionTick(resumed);
        assertEquals(36, policy.logicalStart().occupiedSlots());
        assertEquals(2, policy.activeBudget().consumedExecutionTicks());
        ledger.settle(resumed, result(AutoDepositRunReason.NORMAL, 32, 32), "items");
        assertEquals(1, policy.activeBudget().completedUnits());
        assertEquals(AutoDepositRearmDecision.PRESSURE_RISE, policy.observe(pressure(33), "items", "chestA"));
    }

    @Test void separateFollowupCannotReuseEarlierSlotRelief() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        AutoDepositRunLedger ledger = new AutoDepositRunLedger(policy);
        AutoDepositMaintenanceTask first = task();
        ledger.begin(first, pressure(36), "items", "chestA");
        ledger.settle(first, result(AutoDepositRunReason.NORMAL, 36, 34), "items");
        AutoDepositMaintenanceTask followup = task();
        ledger.begin(followup, pressure(34), "items", "chestA");
        ledger.settle(followup, result(AutoDepositRunReason.NORMAL, 34, 34), "items");
        assertEquals("no_slot_relief", policy.lastReason());
        assertEquals(AutoDepositRearmDecision.SAME_FAILURE, policy.observe(pressure(34), "items", "chestA"));
    }

    @Test void staleRootCallbacksCannotChargeOrFinishItsSuccessor() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        AutoDepositRunLedger ledger = new AutoDepositRunLedger(policy);
        AutoDepositMaintenanceTask old = task();
        ledger.begin(old, pressure(36), "items", "chestA");
        ledger.settle(old, result(AutoDepositRunReason.SAFETY_INTERRUPTED, 36, 35), "items");
        AutoDepositMaintenanceTask current = task();
        ledger.begin(current, pressure(35), "items", "chestA");
        ledger.executionTick(old);
        assertFalse(ledger.settle(old, result(AutoDepositRunReason.NORMAL, 36, 28), "items"));
        assertEquals(0, policy.activeBudget().consumedExecutionTicks());
        assertEquals(AutoDepositRearmDecision.RUNNING, policy.observe(pressure(35), "items", "chestA"));
        assertTrue(ledger.settle(current, result(AutoDepositRunReason.NORMAL, 35, 32), "items"));
        assertFalse(ledger.settle(current, result(AutoDepositRunReason.NORMAL, 35, 32), "items"));
        assertEquals(1, policy.activeBudget().completedUnits());
    }

    @Test void repeatedDefensePreservesActualNoProgressWithoutChargingRegistration() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        AutoDepositRunLedger ledger = new AutoDepositRunLedger(policy);
        for (int cycle = 0; cycle < 100; cycle++) {
            AutoDepositMaintenanceTask root = task();
            ledger.begin(root, pressure(36), "items", "chestA");
            ledger.executionTick(root);
            ledger.settle(root, result(AutoDepositRunReason.SAFETY_INTERRUPTED, 36, 36), "items");
        }
        assertEquals(100, policy.activeBudget().consumedExecutionTicks());
        assertEquals(100, policy.activeBudget().consecutiveNoProgressTicks());
        assertEquals(0, policy.activeBudget().completedUnits());
        assertEquals(0, policy.activeBudget().recoveryGrants());
    }

    private static AutoDepositMaintenanceTask task() { return TestObjects.allocate(AutoDepositMaintenanceTask.class); }
    private static DepositAllInventoryPressureSnapshot pressure(int occupied) {
        return new DepositAllInventoryPressureSnapshot(occupied, 36);
    }
    private static AutoDepositRunResult result(AutoDepositRunReason reason, int start, int end) {
        return new AutoDepositRunResult(reason, Optional.of(pressure(start)), Optional.of(pressure(end)),
                true, AutoDepositWorkingSetStatus.SATISFIED, true, "observed", AutoDepositMaintenanceOutcome.NO_SLOT_RELIEF);
    }
}
