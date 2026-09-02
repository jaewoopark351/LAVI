package lavi.minecraft.diagnostics.container.home.timeout.candidate;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.home.timeout.event.StoreHomeDiagnosticEmitter;
import lavi.minecraft.diagnostics.container.home.timeout.event.StoreHomeEventFields;
import lavi.minecraft.diagnostics.container.home.timeout.progress.StoreHomeCandidateProgressObservation;
import lavi.minecraft.diagnostics.container.home.timeout.progress.StoreHomeCandidateProgressState;
import lavi.minecraft.diagnostics.container.home.timeout.progress.StoreHomeProgressSnapshot;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.home.execution.HomeStorageOperationContext;
import lavi.minecraft.task.container.home.execution.session.HomeStorageContainerSession;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutObservation;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutReason;

//20260902_kpopmodder: Isolate ordered STORE_HOME candidate boundary assembly and emission.
public final class StoreHomeCandidateBoundaryDiagnostics {
    public void emitRejectedBeforeAttempt(
            StoreHomeDiagnosticEmitter emitter,
            String eventName,
            String reason,
            Task owner,
            Object[] operationFields,
            long operationId,
            HomeStorageOperationContext context,
            AutoDepositTrustedDestinationCandidate candidate,
            int candidateOrdinal,
            int candidateCount,
            int remainingAfterRejection,
            String failureKind) {
        emitter.emitBoundary(
                eventName,
                reason,
                owner,
                StoreHomeDiagnosticEmitter.merge(
                        operationFields,
                        StoreHomeEventFields.operationContext(context, candidate),
                        StoreHomeEventFields.candidateBeforeAttempt(
                                operationId,
                                candidate,
                                candidateOrdinal,
                                candidateCount,
                                remainingAfterRejection
                        ),
                        rejectionFields(
                                false,
                                reason,
                                failureKind,
                                remainingAfterRejection,
                                null
                        )
                )
        );
    }

    public void emitStarted(
            StoreHomeDiagnosticEmitter emitter,
            String eventName,
            String reason,
            Task owner,
            Object[] operationFields,
            HomeStorageOperationContext context,
            AutoDepositTrustedDestinationCandidate candidate,
            StoreHomeCandidateProgressState active,
            int remainingCandidateCountIncludingCurrent,
            int candidateTicks,
            long clientTickId,
            StoreHomeProgressSnapshot snapshot,
            StoreHomeCandidateProgressObservation startObservation) {
        emitter.emitBoundary(
                eventName,
                reason,
                owner,
                StoreHomeDiagnosticEmitter.merge(
                        operationFields,
                        StoreHomeEventFields.operationContext(context, candidate),
                        StoreHomeEventFields.candidate(
                                active,
                                remainingCandidateCountIncludingCurrent,
                                candidateTicks,
                                clientTickId
                        ),
                        StoreHomeEventFields.progress(
                                active,
                                snapshot,
                                startObservation,
                                clientTickId,
                                0
                        ),
                        new Object[]{
                                "diagnosticBoundaryKind", eventName
                        },
                        StoreHomeEventFields.session(null)
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
            long clientTickId,
            int candidateTicksBeforeDecision,
            int operationTicksBeforeDecision,
            StoreHomeTimeoutReason timeoutReason,
            StoreHomeTimeoutObservation timeoutObservation,
            boolean pendingAtDecision,
            boolean sessionPendingAtDecision) {
        emitter.emitBoundary(
                eventName,
                reason,
                owner,
                StoreHomeDiagnosticEmitter.merge(
                        operationFields,
                        candidateEvidence,
                        StoreHomeEventFields.timeoutDecision(
                                "CANDIDATE",
                                clientTickId,
                                candidateTicksBeforeDecision,
                                operationTicksBeforeDecision,
                                true,
                                true,
                                true,
                                false,
                                pendingAtDecision
                                        ? "FINISH_TRANSFER_UNCONFIRMED"
                                        : "REJECT_CANDIDATE",
                                pendingAtDecision
                                        ? "not_applicable"
                                        : reason,
                                pendingAtDecision
                                        ? reason + "_with_pending_transfer"
                                        : "not_applicable",
                                pendingAtDecision
                                        ? "TRANSFER_UNCONFIRMED"
                                        : "not_applicable",
                                pendingAtDecision,
                                sessionPendingAtDecision
                        ),
                        new Object[]{
                                "candidateDecisionCounterKind",
                                decisionCounterKind(timeoutReason),
                                "candidateDecisionCounterLimitTicks",
                                timeoutObservation.activeCandidateLimitTicks()
                        }
                )
        );
    }

    public void emitRejected(
            StoreHomeDiagnosticEmitter emitter,
            String eventName,
            String reason,
            Task owner,
            Object[] operationFields,
            Object[] candidateEvidence,
            int candidateActiveTicksAtRejection,
            int remainingCandidateCountAfterRejection,
            String failureKind) {
        emitter.emitBoundary(
                eventName,
                reason,
                owner,
                StoreHomeDiagnosticEmitter.merge(
                        operationFields,
                        candidateEvidence,
                        rejectionFields(
                                true,
                                reason,
                                failureKind,
                                remainingCandidateCountAfterRejection,
                                candidateActiveTicksAtRejection
                        )
                )
        );
    }

    public void emitActivated(
            StoreHomeDiagnosticEmitter emitter,
            String eventName,
            String reason,
            Task owner,
            Object[] operationFields,
            Object[] candidateEvidence) {
        emitter.emitBoundary(
                eventName,
                reason,
                owner,
                StoreHomeDiagnosticEmitter.merge(
                        operationFields,
                        candidateEvidence,
                        new Object[]{
                                "candidateActivated", true,
                                "activationResult", "EXACT_SESSION_INSTALLED"
                        }
                )
        );
    }

    public Object[] unavailableEvidence(
            HomeStorageOperationContext context,
            AutoDepositTrustedDestinationCandidate candidate,
            Object candidateCount,
            Object remainingCandidateCountIncludingCurrent,
            HomeStorageContainerSession session) {
        return StoreHomeDiagnosticEmitter.merge(
                StoreHomeEventFields.operationContext(context, candidate),
                StoreHomeEventFields.candidateUnavailable(
                        candidateCount,
                        remainingCandidateCountIncludingCurrent
                ),
                StoreHomeEventFields.session(session)
        );
    }

    public Object[] unobservedRejectionEvidence(
            HomeStorageOperationContext context,
            AutoDepositTrustedDestinationCandidate candidate,
            int candidateCount,
            int remainingCandidateCountAfterRejection,
            int candidateTicks) {
        return StoreHomeDiagnosticEmitter.merge(
                StoreHomeEventFields.operationContext(context, candidate),
                StoreHomeEventFields.candidateAfterUnobservedAttempt(
                        candidate,
                        candidateCount,
                        remainingCandidateCountAfterRejection,
                        candidateTicks
                )
        );
    }

    public Object[] observedEvidence(
            HomeStorageOperationContext context,
            AutoDepositTrustedDestinationCandidate candidate,
            StoreHomeCandidateProgressState active,
            HomeStorageContainerSession session,
            StoreHomeProgressSnapshot snapshot,
            StoreHomeCandidateProgressObservation observation,
            int candidateQueueRemaining,
            boolean candidateIncludedInQueue,
            int candidateTicks,
            long clientTickId,
            String diagnosticBoundaryKind,
            String diagnosticBoundaryReason) {
        Object[] candidateFields = candidateIncludedInQueue
                ? StoreHomeEventFields.candidate(
                        active,
                        candidateQueueRemaining,
                        candidateTicks,
                        clientTickId
                )
                : StoreHomeEventFields.candidateAfterRejection(
                        active,
                        candidateQueueRemaining,
                        candidateTicks,
                        clientTickId
                );
        return StoreHomeDiagnosticEmitter.merge(
                StoreHomeEventFields.operationContext(context, candidate),
                candidateFields,
                StoreHomeEventFields.progress(
                        active,
                        snapshot,
                        observation,
                        clientTickId,
                        active.suppressedRepeatCount()
                ),
                new Object[]{
                        "diagnosticBoundaryKind", diagnosticBoundaryKind,
                        "diagnosticBoundaryReason", diagnosticBoundaryReason
                },
                StoreHomeEventFields.session(session)
        );
    }

    private static Object[] rejectionFields(
            boolean candidateAttemptStarted,
            String reason,
            String failureKind,
            int remainingCandidateCountAfterRejection,
            Integer candidateActiveTicksAtRejection) {
        Object[] activeAttemptFields = candidateAttemptStarted
                ? new Object[]{
                        "remainingCandidateCountAfterRejection",
                        remainingCandidateCountAfterRejection,
                        "candidateActiveTicksAtRejection",
                        candidateActiveTicksAtRejection
                }
                : new Object[0];
        return StoreHomeDiagnosticEmitter.merge(
                new Object[]{
                        "rejectionStage",
                        candidateAttemptStarted
                                ? "ACTIVE_ATTEMPT"
                                : "PRE_ATTEMPT_VALIDATION",
                        "candidateAttemptStarted", candidateAttemptStarted,
                        "candidateActuallyRemoved", true,
                        "rejectionReason", reason,
                        "actualRejectionReason", reason,
                        "failureKind", failureKind
                },
                activeAttemptFields,
                new Object[]{
                        "actualAction",
                        remainingCandidateCountAfterRejection > 0
                                ? "NEXT_CANDIDATE_SELECTION_PENDING"
                                : "CANDIDATE_QUEUE_EXHAUSTED_PENDING_TERMINAL"
                }
        );
    }

    private static String decisionCounterKind(StoreHomeTimeoutReason reason) {
        return reason == StoreHomeTimeoutReason.CANDIDATE_LOCAL_INTERACTION_TIMEOUT
                ? "CANDIDATE_LOCAL_INTERACTION_TICKS"
                : "CANDIDATE_NAVIGATION_NO_PROGRESS_TICKS";
    }
}
