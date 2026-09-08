package lavi.minecraft.diagnostics.container.home.timeout;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.home.timeout.guard.StoreHomeDiagnosticBookkeepingGuard;
import lavi.minecraft.diagnostics.container.home.timeout.guard.StoreHomeDiagnosticBoundary;
import lavi.minecraft.diagnostics.container.home.timeout.operation.StoreHomeTimeoutLifecycleCoordinator;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.execution.HomeStorageOperationContext;
import lavi.minecraft.task.container.home.execution.HomeStorageTransferExecutor;
import lavi.minecraft.task.container.home.execution.StoreHomePhase;
import lavi.minecraft.task.container.home.execution.StoreHomeResult;
import lavi.minecraft.task.container.home.execution.candidate.StoreHomeCandidateAttempt;
import lavi.minecraft.task.container.home.execution.operation.StoreHomeOperationProgress;
import lavi.minecraft.task.container.home.execution.session.HomeStorageContainerSession;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutObservation;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutPolicy;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutReason;

//20260828_kpopmodder: Preserve the STORE_HOME diagnostics API and strict-OFF boundary while delegating lifecycle ownership.
public final class StoreHomeTimeoutDiagnostics {
    private final StoreHomeTimeoutLifecycleCoordinator lifecycleCoordinator;

    public StoreHomeTimeoutDiagnostics(
            long operationId,
            StoreHomeTimeoutPolicy timeoutPolicy,
            AutoDepositExactOpenContainerBinding exactBinding,
            HomeStorageTransferExecutor transferExecutor) {
        this.lifecycleCoordinator = new StoreHomeTimeoutLifecycleCoordinator(
                operationId,
                timeoutPolicy,
                exactBinding,
                transferExecutor
        );
    }

    public void recordOperationStarted(
            Task owner,
            StoreHomePhase phase,
            StoreHomeOperationProgress operation,
            StoreHomeTimeoutObservation timeout) {
        StoreHomeDiagnosticBoundary.runIfEnabled(() ->
                lifecycleCoordinator.recordOperationStarted(
                        owner, phase, timeout
                )
        );
    }

    public void recordCandidateCatalog(int candidateCount) {
        StoreHomeDiagnosticBoundary.runIfEnabled(() ->
                StoreHomeDiagnosticBookkeepingGuard.runSafely(
                        () -> lifecycleCoordinator.recordCandidateCatalog(
                                candidateCount
                        )
                )
        );
    }

    public void recordCandidateRejectedBeforeAttempt(
            Task owner,
            AltoClef mod,
            StoreHomePhase phase,
            StoreHomeOperationProgress operation,
            StoreHomeTimeoutObservation timeout,
            HomeStorageOperationContext context,
            AutoDepositTrustedDestinationCandidate candidate,
            int remainingAfterRejection,
            String reason,
            String failureKind) {
        StoreHomeDiagnosticBoundary.runIfEnabled(() ->
                lifecycleCoordinator.recordCandidateRejectedBeforeAttempt(
                        owner,
                        mod,
                        phase,
                        timeout,
                        context,
                        candidate,
                        remainingAfterRejection,
                        reason,
                        failureKind
                )
        );
    }

    public void recordCandidateStarted(
            Task owner,
            AltoClef mod,
            StoreHomePhase phase,
            StoreHomeOperationProgress operation,
            StoreHomeTimeoutObservation timeout,
            HomeStorageOperationContext context,
            StoreHomeCandidateAttempt attempt,
            int remainingCandidateCountIncludingCurrent) {
        StoreHomeDiagnosticBoundary.runIfEnabled(() ->
                lifecycleCoordinator.recordCandidateStarted(
                        owner,
                        mod,
                        phase,
                        timeout,
                        context,
                        attempt,
                        remainingCandidateCountIncludingCurrent
                )
        );
    }

    public void recordProgress(
            Task owner,
            AltoClef mod,
            StoreHomePhase phase,
            StoreHomeOperationProgress operation,
            StoreHomeTimeoutObservation timeout,
            HomeStorageOperationContext context,
            StoreHomeCandidateAttempt attempt,
            HomeStorageContainerSession session,
            int remainingCandidateCountIncludingCurrent,
            Task activeChildTask) {
        StoreHomeDiagnosticBoundary.runIfEnabled(() ->
                lifecycleCoordinator.recordProgress(
                        owner,
                        mod,
                        phase,
                        timeout,
                        context,
                        attempt,
                        session,
                        remainingCandidateCountIncludingCurrent,
                        activeChildTask
                )
        );
    }

    public void recordOperationTimeoutDecision(
            Task owner,
            AltoClef mod,
            StoreHomePhase phase,
            StoreHomeOperationProgress operation,
            StoreHomeTimeoutObservation timeout,
            HomeStorageOperationContext context,
            StoreHomeCandidateAttempt attempt,
            HomeStorageContainerSession session,
            int remainingCandidateCountIncludingCurrent,
            StoreHomeTimeoutReason timeoutReason,
            boolean pendingAtDecision,
            Task activeChildTask) {
        String stableReason = timeoutReason == null
                ? "unavailable_timeout_reason"
                : timeoutReason.stableReason();
        StoreHomeDiagnosticBoundary.runIfEnabled(() ->
                lifecycleCoordinator.recordOperationTimeoutDecision(
                        owner,
                        mod,
                        phase,
                        timeout,
                        context,
                        attempt,
                        session,
                        remainingCandidateCountIncludingCurrent,
                        timeoutReason,
                        stableReason,
                        pendingAtDecision,
                        activeChildTask
                )
        );
    }

    public void recordCandidateTimeoutDecision(
            Task owner,
            AltoClef mod,
            StoreHomePhase phase,
            StoreHomeOperationProgress operation,
            StoreHomeTimeoutObservation timeout,
            HomeStorageOperationContext context,
            StoreHomeCandidateAttempt attempt,
            HomeStorageContainerSession session,
            int remainingCandidateCountIncludingCurrent,
            StoreHomeTimeoutReason timeoutReason,
            boolean pendingAtDecision,
            Task activeChildTask) {
        String stableReason = timeoutReason == null
                ? "unavailable_timeout_reason"
                : timeoutReason.stableReason();
        StoreHomeDiagnosticBoundary.runIfEnabled(() ->
                lifecycleCoordinator.recordCandidateTimeoutDecision(
                        owner,
                        mod,
                        phase,
                        timeout,
                        context,
                        attempt,
                        session,
                        remainingCandidateCountIncludingCurrent,
                        timeoutReason,
                        stableReason,
                        pendingAtDecision,
                        activeChildTask
                )
        );
    }

    public void recordCandidateRejected(
            Task owner,
            AltoClef mod,
            StoreHomePhase phase,
            StoreHomeOperationProgress operation,
            StoreHomeTimeoutObservation timeout,
            HomeStorageOperationContext context,
            AutoDepositTrustedDestinationCandidate candidate,
            HomeStorageContainerSession session,
            int candidateActiveTicksAtRejection,
            int remainingCandidateCountAfterRejection,
            String reason,
            String failureKind,
            Task activeChildTask) {
        StoreHomeDiagnosticBoundary.runIfEnabled(() ->
                lifecycleCoordinator.recordCandidateRejected(
                        owner,
                        mod,
                        phase,
                        timeout,
                        context,
                        candidate,
                        session,
                        candidateActiveTicksAtRejection,
                        remainingCandidateCountAfterRejection,
                        reason,
                        failureKind,
                        activeChildTask
                )
        );
    }

    public void recordCandidateActivated(
            Task owner,
            AltoClef mod,
            StoreHomePhase phase,
            StoreHomeOperationProgress operation,
            StoreHomeTimeoutObservation timeout,
            HomeStorageOperationContext context,
            StoreHomeCandidateAttempt attempt,
            HomeStorageContainerSession session,
            int remainingCandidateCountIncludingCurrent) {
        StoreHomeDiagnosticBoundary.runIfEnabled(() ->
                lifecycleCoordinator.recordCandidateActivated(
                        owner,
                        mod,
                        phase,
                        timeout,
                        context,
                        attempt,
                        session,
                        remainingCandidateCountIncludingCurrent
                )
        );
    }

    public void recordTerminal(
            Task owner,
            AltoClef mod,
            StoreHomePhase phase,
            StoreHomeOperationProgress operation,
            StoreHomeTimeoutObservation timeout,
            HomeStorageOperationContext context,
            StoreHomeCandidateAttempt attempt,
            HomeStorageContainerSession session,
            int remainingCandidateCountIncludingCurrent,
            StoreHomeResult result,
            String reason) {
        StoreHomeDiagnosticBoundary.runIfEnabled(() ->
                lifecycleCoordinator.recordTerminal(
                        owner,
                        mod,
                        phase,
                        operation,
                        timeout,
                        context,
                        attempt,
                        session,
                        remainingCandidateCountIncludingCurrent,
                        result,
                        reason
                )
        );
    }
}
