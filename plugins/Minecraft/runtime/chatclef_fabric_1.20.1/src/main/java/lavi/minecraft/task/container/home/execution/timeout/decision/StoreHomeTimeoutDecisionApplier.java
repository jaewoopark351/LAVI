package lavi.minecraft.task.container.home.execution.timeout.decision;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.home.timeout.StoreHomeTimeoutDiagnostics;
import lavi.minecraft.task.container.home.execution.StoreHomeResult;
import lavi.minecraft.task.container.home.execution.candidate.rejection.StoreHomeCandidateFailureKind;
import lavi.minecraft.task.container.home.execution.candidate.rejection.StoreHomeCandidateRejector;
import lavi.minecraft.task.container.home.execution.candidate.view.StoreHomeCandidateRuntimeView;
import lavi.minecraft.task.container.home.execution.operation.pending.StoreHomePendingOwnershipGuard;
import lavi.minecraft.task.container.home.execution.operation.terminal.StoreHomeOperationTerminator;
import lavi.minecraft.task.container.home.execution.state.StoreHomeExecutionState;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutLifecycle;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutReason;

import java.util.Objects;
import java.util.Optional;

//20260829_kpopmodder: Apply already-resolved timeout decisions at their exact task boundary.
public final class StoreHomeTimeoutDecisionApplier {
    private final StoreHomeExecutionState state;
    private final StoreHomePendingOwnershipGuard pendingGuard;
    private final StoreHomeOperationTerminator terminator;
    private final StoreHomeTimeoutLifecycle timeoutLifecycle;
    private final StoreHomeTimeoutDiagnostics timeoutDiagnostics;
    private final StoreHomeCandidateRuntimeView candidateView;
    private final StoreHomeCandidateRejector candidateRejector;

    public StoreHomeTimeoutDecisionApplier(
            StoreHomeExecutionState state,
            StoreHomePendingOwnershipGuard pendingGuard,
            StoreHomeOperationTerminator terminator,
            StoreHomeTimeoutLifecycle timeoutLifecycle,
            StoreHomeTimeoutDiagnostics timeoutDiagnostics,
            StoreHomeCandidateRuntimeView candidateView,
            StoreHomeCandidateRejector candidateRejector) {
        this.state = Objects.requireNonNull(state, "state");
        this.pendingGuard = Objects.requireNonNull(pendingGuard, "pendingGuard");
        this.terminator = Objects.requireNonNull(terminator, "terminator");
        this.timeoutLifecycle = Objects.requireNonNull(
                timeoutLifecycle, "timeoutLifecycle"
        );
        this.timeoutDiagnostics = Objects.requireNonNull(
                timeoutDiagnostics, "timeoutDiagnostics"
        );
        this.candidateView = Objects.requireNonNull(candidateView, "candidateView");
        this.candidateRejector = Objects.requireNonNull(
                candidateRejector, "candidateRejector"
        );
    }

    public boolean applyOperation(
            Task diagnosticOwner,
            AltoClef mod,
            Optional<StoreHomeTimeoutReason> reason) {
        boolean pendingAtDecision = pendingGuard.hasAnyPending();
        Optional<StoreHomeTimeoutDecision> resolved =
                StoreHomeTimeoutResolver.resolveOperation(
                        pendingAtDecision, reason
                );
        if (resolved.isEmpty()) {
            return false;
        }
        StoreHomeTimeoutDecision decision = resolved.orElseThrow();
        timeoutDiagnostics.recordOperationTimeoutDecision(
                diagnosticOwner,
                mod,
                state.lifecycle().phase(),
                state.operation().current(),
                timeoutLifecycle.observation(),
                state.context().current(),
                state.candidateAttempt().current(),
                state.session().current(),
                candidateView.remainingCandidateCount(),
                decision.reason(),
                pendingAtDecision,
                candidateView.activeDiagnosticChild()
        );
        if (decision.action()
                == StoreHomeTimeoutAction.FINISH_TRANSFER_UNCONFIRMED) {
            terminator.finish(
                    diagnosticOwner,
                    mod,
                    StoreHomeResult.TRANSFER_UNCONFIRMED,
                    decision.terminalReason()
            );
        } else {
            terminator.finishExhausted(
                    diagnosticOwner, mod, decision.terminalReason()
            );
        }
        return true;
    }

    public boolean applyCandidate(Task diagnosticOwner, AltoClef mod) {
        if (state.candidateAttempt().current() == null
                || state.session().current() != null) {
            return false;
        }
        boolean pendingAtDecision = pendingGuard.hasAnyPending();
        Optional<StoreHomeTimeoutDecision> resolved =
                StoreHomeTimeoutResolver.resolveCandidate(
                        pendingAtDecision,
                        timeoutLifecycle.candidateTimeoutReason()
                );
        if (resolved.isEmpty()) {
            return false;
        }
        StoreHomeTimeoutDecision decision = resolved.orElseThrow();
        timeoutDiagnostics.recordCandidateTimeoutDecision(
                diagnosticOwner,
                mod,
                state.lifecycle().phase(),
                state.operation().current(),
                timeoutLifecycle.observation(),
                state.context().current(),
                state.candidateAttempt().current(),
                state.session().current(),
                candidateView.remainingCandidateCount(),
                decision.reason(),
                pendingAtDecision,
                candidateView.activeDiagnosticChild()
        );
        if (decision.action()
                == StoreHomeTimeoutAction.FINISH_TRANSFER_UNCONFIRMED) {
            terminator.finish(
                    diagnosticOwner,
                    mod,
                    StoreHomeResult.TRANSFER_UNCONFIRMED,
                    decision.terminalReason()
            );
        } else {
            candidateRejector.reject(
                    diagnosticOwner,
                    mod,
                    decision.reason().stableReason(),
                    StoreHomeCandidateFailureKind.UNAVAILABLE
            );
        }
        return true;
    }
}
