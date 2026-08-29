package lavi.minecraft.task.container.home.execution.candidate.rejection;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.home.timeout.StoreHomeTimeoutDiagnostics;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.home.execution.StoreHomeResult;
import lavi.minecraft.task.container.home.execution.candidate.view.StoreHomeCandidateRuntimeView;
import lavi.minecraft.task.container.home.execution.operation.pending.StoreHomePendingOwnershipGuard;
import lavi.minecraft.task.container.home.execution.operation.terminal.StoreHomeOperationTerminator;
import lavi.minecraft.task.container.home.execution.session.HomeStorageContainerSession;
import lavi.minecraft.task.container.home.execution.state.StoreHomeExecutionState;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutLifecycle;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutObservation;

import java.util.Objects;

//20260829_kpopmodder: Added this collaborator to own candidate rejection accounting and cleanup.
public final class StoreHomeCandidateRejector {
    private final StoreHomeExecutionState state;
    private final StoreHomePendingOwnershipGuard pendingGuard;
    private final StoreHomeOperationTerminator operationTerminator;
    private final StoreHomeTimeoutLifecycle timeoutLifecycle;
    private final StoreHomeTimeoutDiagnostics timeoutDiagnostics;
    private final StoreHomeCandidateRuntimeView candidateRuntimeView;

    public StoreHomeCandidateRejector(
            StoreHomeExecutionState state,
            StoreHomePendingOwnershipGuard pendingGuard,
            StoreHomeOperationTerminator operationTerminator,
            StoreHomeTimeoutLifecycle timeoutLifecycle,
            StoreHomeTimeoutDiagnostics timeoutDiagnostics,
            StoreHomeCandidateRuntimeView candidateRuntimeView) {
        this.state = Objects.requireNonNull(state, "state");
        this.pendingGuard = Objects.requireNonNull(pendingGuard, "pendingGuard");
        this.operationTerminator = Objects.requireNonNull(
                operationTerminator, "operationTerminator"
        );
        this.timeoutLifecycle = Objects.requireNonNull(
                timeoutLifecycle, "timeoutLifecycle"
        );
        this.timeoutDiagnostics = Objects.requireNonNull(
                timeoutDiagnostics, "timeoutDiagnostics"
        );
        this.candidateRuntimeView = Objects.requireNonNull(
                candidateRuntimeView, "candidateRuntimeView"
        );
    }

    public void reject(
            Task diagnosticOwner,
            AltoClef mod,
            String reason,
            StoreHomeCandidateFailureKind kind) {
        if (pendingGuard.hasAnyPending()) {
            operationTerminator.finish(
                    diagnosticOwner,
                    mod,
                    StoreHomeResult.TRANSFER_UNCONFIRMED,
                    "candidate_rejection_with_pending_transfer:" + reason
            );
            return;
        }
        AutoDepositTrustedDestinationCandidate rejectedCandidate =
                state.candidateAttempt().current() == null
                        ? state.candidateQueue().current().current().orElse(null)
                        : state.candidateAttempt().current().candidate();
        HomeStorageContainerSession rejectedSession = state.session().current();
        int rejectedCandidateTicks = state.candidateAttempt().current() == null
                ? 0
                : timeoutLifecycle.candidateActiveTicks();
        StoreHomeTimeoutObservation rejectedTimeout = timeoutLifecycle.observation();
        Task rejectedChild = candidateRuntimeView.activeDiagnosticChild();
        state.operation().replace(kind == StoreHomeCandidateFailureKind.CAPACITY
                ? state.operation().current().capacityFailure()
                : state.operation().current().unavailableFailure());
        state.candidateQueue().current().rejectCurrent(reason);
        if (rejectedCandidate != null) {
            timeoutDiagnostics.recordCandidateRejected(
                    diagnosticOwner,
                    mod,
                    state.lifecycle().phase(),
                    state.operation().current(),
                    rejectedTimeout,
                    state.context().current(),
                    rejectedCandidate,
                    rejectedSession,
                    rejectedCandidateTicks,
                    candidateRuntimeView.remainingCandidateCount(),
                    reason,
                    kind.name(),
                    rejectedChild
            );
        }
        state.session().clear();
        state.publishedPlan().clear();
        state.candidateAttempt().clear();
        timeoutLifecycle.clearCandidate();
    }
}
