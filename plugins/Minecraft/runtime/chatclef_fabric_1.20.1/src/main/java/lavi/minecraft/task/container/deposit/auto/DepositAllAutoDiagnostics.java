package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceOutcome;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenancePhase;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;
import lavi.minecraft.task.container.deposit.auto.recovery.AutoDepositRecoveryCandidate;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetSnapshot;

//20260826_kpopmodder: Added bounded state-transition diagnostics for automatic deposit_all orchestration.
public final class DepositAllAutoDiagnostics {
    private static final String CATEGORY = "AUTO_DEPOSIT_ALL";

    private DepositAllAutoDiagnostics() {
    }

    public static void logRegistered(float priority) {
        ChatClefDiagnostics.logEvent(
                CATEGORY,
                "REGISTERED",
                "automatic_chain_registered",
                null,
                "priority", priority
        );
    }

    public static void logTransition(DepositAllInventoryPressureState previousState,
                                     DepositAllInventoryPressureState nextState,
                                     String reason,
                                     DepositAllInventoryPressureSnapshot snapshot,
                                     Task task) {
        ChatClefDiagnostics.logEvent(
                CATEGORY,
                "STATE_TRANSITION",
                reason,
                task,
                "previousState", previousState,
                "nextState", nextState,
                "occupiedSlots", snapshot == null ? -1 : snapshot.occupiedSlots(),
                "totalSlots", snapshot == null ? -1 : snapshot.totalSlots(),
                "thresholdReached", snapshot != null && snapshot.isAtOrAboveThreshold(),
                "lowWaterReached", snapshot != null && snapshot.isAtOrBelowLowWater()
        );
    }

    public static void logTrigger(DepositAllInventoryPressureSnapshot snapshot,
                                  int targetStepCount,
                                  Task task) {
        ChatClefDiagnostics.logEvent(
                CATEGORY,
                "TRIGGERED",
                "high_water_threshold_crossed",
                task,
                "occupiedSlots", snapshot.occupiedSlots(),
                "totalSlots", snapshot.totalSlots(),
                "targetStepCount", targetStepCount
        );
    }

    public static void logRunnerActivated(DepositAllInventoryPressureSnapshot snapshot,
                                          Task task) {
        ChatClefDiagnostics.logEvent(
                CATEGORY,
                "RUNNER_ACTIVATED",
                "automatic_task_required_inactive_runner",
                task,
                "occupiedSlots", snapshot.occupiedSlots(),
                "totalSlots", snapshot.totalSlots()
        );
    }

    public static void logDeferred(String reason,
                                   DepositAllInventoryPressureSnapshot pressure,
                                   Task userTaskRoot) {
        ChatClefDiagnostics.logEvent(
                CATEGORY,
                "DEFERRED",
                reason,
                userTaskRoot,
                "occupiedSlots", pressure == null ? -1 : pressure.occupiedSlots(),
                "totalSlots", pressure == null ? -1 : pressure.totalSlots(),
                "thresholdConsumed", false
        );
    }

    public static void logWorkingSetPlan(WorkingSetSnapshot snapshot,
                                         int surplusTypeCount,
                                         int surplusItemCount,
                                         Task task) {
        ChatClefDiagnostics.logEvent(
                CATEGORY,
                "WORKING_SET_PLAN",
                "automatic_surplus_plan_created",
                task,
                "operationEpoch", snapshot.epoch(),
                "taskPathLength", snapshot.taskPath().size(),
                "reservedTypeCount", snapshot.reservedCounts().size(),
                "surplusTypeCount", surplusTypeCount,
                "surplusItemCount", surplusItemCount
        );
    }

    public static void logPolicyPlan(AutoDepositPlan plan, Task task) {
        ChatClefDiagnostics.logEvent(
                CATEGORY,
                "POLICY_PLAN",
                "immutable_automatic_plan_created",
                task,
                "operationEpoch", plan.context().epoch(),
                "activeUserTask", plan.context().activeUserTask(),
                "persistentWorldKey", plan.context().persistentWorldKey(),
                "generalStepCount", plan.generalTargets().length,
                "trustedStepCount", plan.trustedTargets().length,
                "trustedDestination", plan.trustedDestination().map(Object::toString).orElse("none"),
                "protectedItemTypes", plan.protectedCounts().size(),
                "targetReliefSlots", plan.targetReliefSlots(),
                "expectedFreedSlots", plan.expectedFreedSlots(),
                "startingOccupiedSlots", plan.startingOccupiedSlots()
        );
    }

    public static void logNoSafeSurplus(String reason,
                                        DepositAllInventoryPressureSnapshot pressure,
                                        Task userTaskRoot) {
        ChatClefDiagnostics.logEvent(
                CATEGORY,
                "NO_SAFE_SURPLUS_LATCHED",
                reason,
                userTaskRoot,
                "occupiedSlots", pressure == null ? -1 : pressure.occupiedSlots(),
                "totalSlots", pressure == null ? -1 : pressure.totalSlots()
        );
    }

    public static void logMeaningfulReevaluation(DepositAllInventoryPressureSnapshot pressure,
                                                 Task userTaskRoot) {
        ChatClefDiagnostics.logEvent(
                CATEGORY,
                "NO_SAFE_SURPLUS_RELEASED",
                "semantic_fingerprint_changed",
                userTaskRoot,
                "occupiedSlots", pressure == null ? -1 : pressure.occupiedSlots(),
                "totalSlots", pressure == null ? -1 : pressure.totalSlots()
        );
    }

    public static void logMaintenanceTransition(long operationEpoch,
                                                AutoDepositMaintenancePhase previous,
                                                AutoDepositMaintenancePhase next,
                                                String reason,
                                                int deficitTypes,
                                                Task task) {
        ChatClefDiagnostics.logEvent(
                CATEGORY,
                "MAINTENANCE_TRANSITION",
                reason,
                task,
                "operationEpoch", operationEpoch,
                "previousPhase", previous,
                "nextPhase", next,
                "deficitTypeCount", deficitTypes
        );
    }

    public static void logFreeSlotOutcome(long operationEpoch,
                                          int startingOccupiedSlots,
                                          int endingOccupiedSlots,
                                          int expectedFreedSlots,
                                          AutoDepositMaintenanceOutcome outcome,
                                          Task task) {
        ChatClefDiagnostics.logEvent(
                CATEGORY,
                "FREE_SLOT_POSTCONDITION",
                outcome.name(),
                task,
                "operationEpoch", operationEpoch,
                "startingOccupiedSlots", startingOccupiedSlots,
                "endingOccupiedSlots", endingOccupiedSlots,
                "actualFreedSlots", Math.max(0, startingOccupiedSlots - endingOccupiedSlots),
                "expectedFreedSlots", expectedFreedSlots
        );
    }

    public static void logRecoveryQueue(long operationEpoch,
                                        int deficitTypes,
                                        int candidateCount) {
        ChatClefDiagnostics.logEvent(
                CATEGORY,
                "RECOVERY_QUEUE",
                "bounded_candidate_queue_created",
                null,
                "operationEpoch", operationEpoch,
                "deficitTypeCount", deficitTypes,
                "candidateCount", candidateCount
        );
    }

    public static void logRecoveryCandidateSelected(long operationEpoch,
                                                    AutoDepositRecoveryCandidate candidate,
                                                    int deficitTypes) {
        ChatClefDiagnostics.logEvent(
                CATEGORY,
                "RECOVERY_CANDIDATE_SELECTED",
                "physical_container_revalidation_started",
                null,
                "operationEpoch", operationEpoch,
                "candidateTier", candidate.tier(),
                "candidatePosition", candidate.position(),
                "deficitTypeCount", deficitTypes
        );
    }

    public static void logRecoveryCandidateTerminal(long operationEpoch,
                                                    AutoDepositRecoveryCandidate candidate,
                                                    String result,
                                                    int remainingDeficitTypes) {
        ChatClefDiagnostics.logEvent(
                CATEGORY,
                "RECOVERY_CANDIDATE_TERMINAL",
                result,
                null,
                "operationEpoch", operationEpoch,
                "candidateTier", candidate == null ? "none" : candidate.tier(),
                "candidatePosition", candidate == null ? "none" : candidate.position(),
                "remainingDeficitTypeCount", remainingDeficitTypes
        );
    }
}
