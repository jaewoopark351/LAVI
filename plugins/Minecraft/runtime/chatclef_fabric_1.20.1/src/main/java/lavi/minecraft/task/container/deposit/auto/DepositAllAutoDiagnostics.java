package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.pressure.AutoDepositBoundaryDiagnostics;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceOutcome;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenancePhase;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;
import lavi.minecraft.task.container.deposit.auto.recovery.AutoDepositRecoveryCandidate;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetSnapshot;

//20260826_kpopmodder: Added bounded state-transition diagnostics for automatic deposit_all orchestration.
public final class DepositAllAutoDiagnostics {
    private static final String CATEGORY = "AUTO_DEPOSIT_ALL";

    private DepositAllAutoDiagnostics() {
    }

    public static void logRegistered(float priority) {
        logObserved(
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
        logObserved(
                CATEGORY,
                "STATE_TRANSITION",
                reason,
                task,
                "previousState", previousState,
                "nextState", nextState,
                "occupiedSlots", snapshot == null ? -1 : snapshot.occupiedSlots(),
                "totalSlots", snapshot == null ? -1 : snapshot.totalSlots(),
                "thresholdReached", snapshot == null ? "NOT_EVALUATED" : snapshot.isAtOrAboveThreshold(),
                "lowWaterReached", snapshot == null ? "NOT_EVALUATED" : snapshot.isAtOrBelowLowWater()
        );
    }

    public static void logTrigger(DepositAllInventoryPressureSnapshot snapshot,
                                  int targetStepCount,
                                  Task task) {
        logObserved(
                CATEGORY,
                "TRIGGERED",
                snapshot.isAtOrAboveThreshold() ? "automatic_storage_admitted" : "same_unit_resumed_below_threshold",
                task,
                "occupiedSlots", snapshot.occupiedSlots(),
                "totalSlots", snapshot.totalSlots(),
                "targetStepCount", targetStepCount
        );
    }

    public static void logRunnerActivated(DepositAllInventoryPressureSnapshot snapshot,
                                          Task task) {
        logObserved(
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
        logObserved(
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
        logObserved(
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
        logObserved(
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
                "trustedCandidateCount", plan.trustedCandidates().size(),
                "protectedItemTypes", plan.protectedCounts().size(),
                "targetReliefSlots", plan.targetReliefSlots(),
                "expectedFreedSlots", plan.expectedFreedSlots(),
                "startingOccupiedSlots", plan.startingOccupiedSlots()
        );
    }

    public static void logNoSafeSurplus(String reason,
                                        DepositAllInventoryPressureSnapshot pressure,
                                        Task userTaskRoot) {
        logObserved(
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
        logObserved(
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
        logObserved(
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
        logObserved(
                CATEGORY,
                "FREE_SLOT_POSTCONDITION",
                outcome.name(),
                task,
                "operationEpoch", operationEpoch,
                "startingOccupiedSlots", startingOccupiedSlots,
                "endingOccupiedSlots", endingOccupiedSlots,
                "actualFreedSlots", Math.max(0, startingOccupiedSlots - endingOccupiedSlots),
                "signedFreedSlotDelta", startingOccupiedSlots < 0 || endingOccupiedSlots < 0
                        ? "UNAVAILABLE" : startingOccupiedSlots - endingOccupiedSlots,
                "slotDeltaAttribution", "NOT_PROVEN_BY_OCCUPANCY_CHANGE",
                "transferEvidenceSource", "existing_paired_transfer_and_effect_events",
                "expectedFreedSlots", expectedFreedSlots
        );
    }

    public static void logRecoveryQueue(Task task,
long operationEpoch,
                                        int deficitTypes,
                                        int candidateCount) {
        logObserved(
                CATEGORY,
                "RECOVERY_QUEUE",
                "bounded_candidate_queue_created",
                task,
                "operationEpoch", operationEpoch,
                "deficitTypeCount", deficitTypes,
                "candidateCount", candidateCount
        );
    }

    public static void logRecoveryCandidateSelected(Task task,
long operationEpoch,
                                                    AutoDepositRecoveryCandidate candidate,
                                                    int deficitTypes) {
        logObserved(
                CATEGORY,
                "RECOVERY_CANDIDATE_SELECTED",
                "physical_container_revalidation_started",
                task,
                "operationEpoch", operationEpoch,
                "candidateTier", candidate.tier(),
                "candidatePosition", candidate.position(),
                "deficitTypeCount", deficitTypes
        );
    }

    public static void logRecoveryCandidateTerminal(Task task,
long operationEpoch,
                                                    AutoDepositRecoveryCandidate candidate,
                                                    String result,
                                                    int remainingDeficitTypes) {
        logObserved(
                CATEGORY,
                "RECOVERY_CANDIDATE_TERMINAL",
                result,
                task,
                "operationEpoch", operationEpoch,
                "candidateTier", candidate == null ? "none" : candidate.tier(),
                "candidatePosition", candidate == null ? "none" : candidate.position(),
                "remainingDeficitTypeCount", remainingDeficitTypes
        );
    }

    public static void logTrustedCandidateSelected(Task task,
            long operationEpoch,
            AutoDepositTrustedDestinationCandidate candidate,
            int remainingTargetTypes,
            int remainingCandidates) {
        logObserved(
                CATEGORY,
                "TRUSTED_CANDIDATE_SELECTED",
                "live_container_validation_started",
                task,
                "operationEpoch", operationEpoch,
                "destinationId", candidate.destinationId(),
                "candidatePosition", candidate.position(),
                "cachedEmptySlotsHint", candidate.cachedEmptySlots(),
                "observedStateHint", candidate.observedState(),
                "remainingTargetTypes", remainingTargetTypes,
                "remainingCandidates", remainingCandidates
        );
    }

    public static void logTrustedCandidateTerminal(Task task,
            long operationEpoch,
            AutoDepositTrustedDestinationCandidate candidate,
            String result,
            int remainingTargetTypes,
            int remainingCandidates) {
        logObserved(
                CATEGORY,
                "TRUSTED_CANDIDATE_TERMINAL",
                result,
                task,
                "operationEpoch", operationEpoch,
                "destinationId", candidate == null ? "none" : candidate.destinationId(),
                "candidatePosition", candidate == null ? "none" : candidate.position(),
                "remainingTargetTypes", remainingTargetTypes,
                "remainingCandidates", remainingCandidates
        );
    }

    //20260827_kpopmodder: Log only live acceptance and confirmed transfer boundaries.
    public static void logTrustedCandidateAccepted(Task task,
            long operationEpoch,
            AutoDepositTrustedDestinationCandidate candidate,
            int remainingTargetTypes) {
        logObserved(
                CATEGORY,
                "TRUSTED_CANDIDATE_LIVE_ACCEPTED",
                "exact_open_gui_capacity_confirmed",
                task,
                "operationEpoch", operationEpoch,
                "destinationId", candidate.destinationId(),
                "candidatePosition", candidate.position(),
                "remainingTargetTypes", remainingTargetTypes
        );
    }

    public static void logTrustedTransferProgress(Task task,
            long operationEpoch,
            AutoDepositTrustedDestinationCandidate candidate,
            int confirmedDelta,
            int confirmedTotal,
            int remainingTargetTypes) {
        logObserved(
                CATEGORY,
                "TRUSTED_TRANSFER_PROGRESS",
                "paired_inventory_and_container_delta_confirmed",
                task,
                "operationEpoch", operationEpoch,
                "destinationId", candidate == null ? "none" : candidate.destinationId(),
                "candidatePosition", candidate == null ? "none" : candidate.position(),
                "confirmedDelta", confirmedDelta,
                "confirmedTotal", confirmedTotal,
                "remainingTargetTypes", remainingTargetTypes
        );
    }
    //20260913_kpopmodder: Preserve verbose output while observing existing results through bounded BOUNDARY admission.
    private static void logObserved(String category, String event, String reason, Task task, Object... fields) {
        ChatClefDiagnostics.logEvent(category, event, reason, task, fields);
        AutoDepositBoundaryDiagnostics.log("AUTO_DEPOSIT_" + event, reason, task, fields);
    }
    //20260913_kpopmodder: Keep existing diagnostic call signatures while accepting exact owner provenance.
    public static void logRecoveryQueue(long operationEpoch,
                                        int deficitTypes,
                                        int candidateCount) {
        logRecoveryQueue(null, operationEpoch, deficitTypes, candidateCount);
    }

    public static void logRecoveryCandidateSelected(long operationEpoch,
                                                    AutoDepositRecoveryCandidate candidate,
                                                    int deficitTypes) {
        logRecoveryCandidateSelected(null, operationEpoch, candidate, deficitTypes);
    }

    public static void logRecoveryCandidateTerminal(long operationEpoch,
                                                    AutoDepositRecoveryCandidate candidate,
                                                    String result,
                                                    int remainingDeficitTypes) {
        logRecoveryCandidateTerminal(null, operationEpoch, candidate, result, remainingDeficitTypes);
    }

    public static void logTrustedCandidateSelected(
            long operationEpoch,
            AutoDepositTrustedDestinationCandidate candidate,
            int remainingTargetTypes,
            int remainingCandidates) {
        logTrustedCandidateSelected(null, operationEpoch, candidate, remainingTargetTypes, remainingCandidates);
    }

    public static void logTrustedCandidateTerminal(
            long operationEpoch,
            AutoDepositTrustedDestinationCandidate candidate,
            String result,
            int remainingTargetTypes,
            int remainingCandidates) {
        logTrustedCandidateTerminal(null, operationEpoch, candidate, result, remainingTargetTypes, remainingCandidates);
    }

    public static void logTrustedCandidateAccepted(
            long operationEpoch,
            AutoDepositTrustedDestinationCandidate candidate,
            int remainingTargetTypes) {
        logTrustedCandidateAccepted(null, operationEpoch, candidate, remainingTargetTypes);
    }

    public static void logTrustedTransferProgress(
            long operationEpoch,
            AutoDepositTrustedDestinationCandidate candidate,
            int confirmedDelta,
            int confirmedTotal,
            int remainingTargetTypes) {
        logTrustedTransferProgress(null, operationEpoch, candidate, confirmedDelta, confirmedTotal, remainingTargetTypes);
    }
}
