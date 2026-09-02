package lavi.minecraft.diagnostics.container.home.timeout.operation;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.home.timeout.event.StoreHomeDiagnosticEmitter;
import lavi.minecraft.diagnostics.container.home.timeout.event.StoreHomeEventFields;
import lavi.minecraft.task.container.home.execution.StoreHomePhase;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutObservation;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutReason;

//20260902_kpopmodder: Isolate ordered STORE_HOME operation boundary assembly and emission.
public final class StoreHomeOperationDiagnostics {
    public Object[] operationFields(
            Task topLevelTask,
            String topLevelTaskRunId,
            StoreHomePhase phase,
            long clientTickId,
            long operationStartClientTickId,
            boolean operationStartClientTickKnown,
            StoreHomeTimeoutObservation timeoutObservation) {
        return StoreHomeEventFields.operation(
                topLevelTask,
                topLevelTaskRunId,
                phase,
                clientTickId,
                operationStartClientTickId,
                operationStartClientTickKnown,
                timeoutObservation
        );
    }

    public void emitStarted(
            StoreHomeDiagnosticEmitter emitter,
            String eventName,
            Task owner,
            Object[] operationFields,
            boolean startClientTickKnown,
            boolean candidateCatalogCaptured,
            Object candidateCount) {
        Object unavailableCandidateCount = candidateCatalogCaptured
                ? candidateCount
                : "unavailable_not_built";
        emitter.emitBoundary(
                eventName,
                startClientTickKnown
                        ? "store_home_task_started"
                        : "store_home_diagnostics_attached_after_start",
                owner,
                StoreHomeDiagnosticEmitter.merge(
                        operationFields,
                        StoreHomeEventFields.operationContext(null, null),
                        StoreHomeEventFields.candidateUnavailable(
                                unavailableCandidateCount,
                                unavailableCandidateCount
                        ),
                        new Object[]{
                                "operationStartObservation",
                                startClientTickKnown
                                        ? "TASK_ON_START"
                                        : "FIRST_VISIBLE_DIAGNOSTIC_BOUNDARY",
                                "candidateCatalogCaptured",
                                candidateCatalogCaptured
                        }
                )
        );
    }

    public void emitTimeoutDecision(
            StoreHomeDiagnosticEmitter emitter,
            String eventName,
            String reason,
            Task owner,
            Object[] operationFields,
            Object[] candidateEvidence,
            boolean activeCandidatePresent,
            long clientTickId,
            Object candidateTicksObserved,
            int operationTicksBeforeDecision,
            StoreHomeTimeoutReason timeoutReason,
            StoreHomeTimeoutObservation timeoutObservation,
            boolean pendingAtDecision,
            boolean sessionPendingAtDecision) {
        Object[] captureStatus = activeCandidatePresent
                ? new Object[0]
                : new Object[]{
                        "diagnosticCaptureStatus", "complete_no_active_candidate",
                        "diagnosticErrorClass", "none"
                };
        emitter.emitBoundary(
                eventName,
                reason,
                owner,
                StoreHomeDiagnosticEmitter.merge(
                        operationFields,
                        candidateEvidence,
                        captureStatus,
                        StoreHomeEventFields.timeoutDecision(
                                "OPERATION",
                                clientTickId,
                                candidateTicksObserved,
                                operationTicksBeforeDecision,
                                false,
                                "unavailable_not_evaluated",
                                true,
                                true,
                                pendingAtDecision
                                        ? "FINISH_TRANSFER_UNCONFIRMED"
                                        : "FINISH_EXHAUSTED",
                                "not_applicable",
                                pendingAtDecision
                                        ? reason + "_with_pending_transfer"
                                        : reason,
                                pendingAtDecision
                                        ? "TRANSFER_UNCONFIRMED"
                                        : "EXISTING_EXHAUSTED_CLASSIFIER",
                                pendingAtDecision,
                                sessionPendingAtDecision
                        ),
                        new Object[]{
                                "candidateTicksObservedAtOperationDecision",
                                candidateTicksObserved,
                                "operationDecisionCounterKind",
                                decisionCounterKind(timeoutReason),
                                "operationDecisionCounterLimitTicks",
                                decisionCounterLimit(
                                        timeoutReason,
                                        timeoutObservation
                                )
                        }
                )
        );
    }

    private static String decisionCounterKind(StoreHomeTimeoutReason reason) {
        return reason == StoreHomeTimeoutReason.OPERATION_EMERGENCY_HARD_CAP
                ? "OPERATION_ACTIVE_TICKS"
                : "OPERATION_NO_PROGRESS_TICKS";
    }

    private static int decisionCounterLimit(
            StoreHomeTimeoutReason reason,
            StoreHomeTimeoutObservation observation) {
        return reason == StoreHomeTimeoutReason.OPERATION_EMERGENCY_HARD_CAP
                ? observation.maxOperationEmergencyHardCapTicks()
                : observation.maxOperationNoProgressTicks();
    }
}
