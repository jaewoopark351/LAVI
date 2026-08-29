package lavi.minecraft.task.container.home.execution.candidate.selection;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.home.timeout.StoreHomeTimeoutDiagnostics;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.home.execution.StoreHomePhase;
import lavi.minecraft.task.container.home.execution.candidate.StoreHomeCandidateAttempt;
import lavi.minecraft.task.container.home.execution.candidate.StoreHomeCandidateValidator;
import lavi.minecraft.task.container.home.execution.candidate.rejection.StoreHomeCandidateFailureKind;
import lavi.minecraft.task.container.home.execution.candidate.view.StoreHomeCandidateRuntimeView;
import lavi.minecraft.task.container.home.execution.state.StoreHomeExecutionState;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutLifecycle;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;
import java.util.Optional;

//20260829_kpopmodder: Added this collaborator to start exactly one validated candidate attempt.
public final class StoreHomeCandidateAttemptStarter {
    private final StoreHomeExecutionState state;
    private final StoreHomeCandidateValidator candidateValidator;
    private final StoreHomeTimeoutLifecycle timeoutLifecycle;
    private final StoreHomeTimeoutDiagnostics timeoutDiagnostics;
    private final StoreHomeCandidateRuntimeView candidateRuntimeView;

    public StoreHomeCandidateAttemptStarter(
            StoreHomeExecutionState state,
            StoreHomeCandidateValidator candidateValidator,
            StoreHomeTimeoutLifecycle timeoutLifecycle,
            StoreHomeTimeoutDiagnostics timeoutDiagnostics,
            StoreHomeCandidateRuntimeView candidateRuntimeView) {
        this.state = Objects.requireNonNull(state, "state");
        this.candidateValidator = Objects.requireNonNull(
                candidateValidator, "candidateValidator"
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

    public boolean ensure(Task diagnosticOwner, AltoClef mod) {
        while (state.candidateAttempt().current() == null) {
            state.lifecycle().transitionTo(StoreHomePhase.SELECT_DESTINATION);
            AutoDepositTrustedDestinationCandidate candidate =
                    state.candidateQueue().current().current()
                            .orElse(null);
            if (candidate == null) {
                return false;
            }
            Optional<String> refusal = candidateValidator.refusal(mod, candidate);
            if (refusal.isPresent()) {
                String rejectionReason = refusal.orElseThrow();
                state.candidateQueue().current().rejectCurrent(rejectionReason);
                state.operation().replace(
                        state.operation().current().unavailableFailure()
                );
                timeoutDiagnostics.recordCandidateRejectedBeforeAttempt(
                        diagnosticOwner,
                        mod,
                        state.lifecycle().phase(),
                        state.operation().current(),
                        timeoutLifecycle.observation(),
                        state.context().current(),
                        candidate,
                        candidateRuntimeView.remainingCandidateCount(),
                        rejectionReason,
                        StoreHomeCandidateFailureKind.UNAVAILABLE.name()
                );
                continue;
            }
            state.candidateAttempt().start(new StoreHomeCandidateAttempt(candidate));
            BlockPos target = candidate.position();
            timeoutLifecycle.startCandidate(
                    target.getX() + 0.5,
                    target.getY() + 0.5,
                    target.getZ() + 0.5
            );
            timeoutDiagnostics.recordCandidateStarted(
                    diagnosticOwner,
                    mod,
                    state.lifecycle().phase(),
                    state.operation().current(),
                    timeoutLifecycle.observation(),
                    state.context().current(),
                    state.candidateAttempt().current(),
                    candidateRuntimeView.remainingCandidateCount()
            );
        }
        return true;
    }
}
