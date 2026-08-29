package lavi.minecraft.task.container.home.execution.candidate.navigation;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.execution.HomeStorageTransferExecutor;
import lavi.minecraft.task.container.home.execution.StoreHomePhase;
import lavi.minecraft.task.container.home.execution.StoreHomeResult;
import lavi.minecraft.task.container.home.execution.candidate.StoreHomeCandidateValidator;
import lavi.minecraft.task.container.home.execution.candidate.rejection.StoreHomeCandidateFailureKind;
import lavi.minecraft.task.container.home.execution.candidate.rejection.StoreHomeCandidateRejector;
import lavi.minecraft.task.container.home.execution.operation.terminal.StoreHomeOperationTerminator;
import lavi.minecraft.task.container.home.execution.session.HomeStorageContainerActivation;
import lavi.minecraft.task.container.home.execution.session.HomeStorageContainerActivationGate;
import lavi.minecraft.task.container.home.execution.session.HomeStorageContainerActivationStatus;
import lavi.minecraft.task.container.home.execution.session.activation.StoreHomeSessionActivator;
import lavi.minecraft.task.container.home.execution.state.StoreHomeExecutionState;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutLifecycle;
import lavi.minecraft.task.container.home.execution.timeout.candidate.StoreHomeCandidateTimeoutPhase;

import java.util.Objects;
import java.util.Optional;

//20260829_kpopmodder: Own one candidate navigation and exact-activation step.
public final class StoreHomeCandidateNavigationStep {
    private final StoreHomeExecutionState state;
    private final AutoDepositExactOpenContainerBinding binding;
    private final StoreHomeCandidateValidator candidateValidator;
    private final HomeStorageContainerActivationGate activationGate;
    private final HomeStorageTransferExecutor transferExecutor;
    private final StoreHomeTimeoutLifecycle timeoutLifecycle;
    private final StoreHomeCandidateRejector candidateRejector;
    private final StoreHomeSessionActivator sessionActivator;
    private final StoreHomeOperationTerminator terminator;

    public StoreHomeCandidateNavigationStep(
            StoreHomeExecutionState state,
            AutoDepositExactOpenContainerBinding binding,
            StoreHomeCandidateValidator candidateValidator,
            HomeStorageContainerActivationGate activationGate,
            HomeStorageTransferExecutor transferExecutor,
            StoreHomeTimeoutLifecycle timeoutLifecycle,
            StoreHomeCandidateRejector candidateRejector,
            StoreHomeSessionActivator sessionActivator,
            StoreHomeOperationTerminator terminator) {
        this.state = Objects.requireNonNull(state, "state");
        this.binding = Objects.requireNonNull(binding, "binding");
        this.candidateValidator = Objects.requireNonNull(
                candidateValidator, "candidateValidator"
        );
        this.activationGate = Objects.requireNonNull(activationGate, "activationGate");
        this.transferExecutor = Objects.requireNonNull(
                transferExecutor, "transferExecutor"
        );
        this.timeoutLifecycle = Objects.requireNonNull(
                timeoutLifecycle, "timeoutLifecycle"
        );
        this.candidateRejector = Objects.requireNonNull(
                candidateRejector, "candidateRejector"
        );
        this.sessionActivator = Objects.requireNonNull(
                sessionActivator, "sessionActivator"
        );
        this.terminator = Objects.requireNonNull(terminator, "terminator");
    }

    public Task tick(Task diagnosticOwner, AltoClef mod) {
        AutoDepositTrustedDestinationCandidate candidate =
                state.candidateAttempt().current().candidate();
        Optional<String> refusal = candidateValidator.refusal(mod, candidate);
        if (refusal.isPresent()) {
            candidateRejector.reject(
                    diagnosticOwner,
                    mod,
                    refusal.orElseThrow(),
                    StoreHomeCandidateFailureKind.UNAVAILABLE
            );
            return null;
        }
        if (!binding.matches(candidate.position())) {
            transitionToCandidatePhase();
            return state.candidateAttempt().current().openTask();
        }

        state.lifecycle().transitionTo(StoreHomePhase.VALIDATE_CONTAINER);
        HomeStorageContainerActivation activation = activationGate.evaluate(
                mod,
                candidate,
                state.context().current(),
                transferExecutor.hasPending()
        );
        if (!activation.ready()) {
            handleActivationRefusal(diagnosticOwner, mod, activation);
            return null;
        }
        sessionActivator.activate(diagnosticOwner, mod, candidate, activation);
        return null;
    }

    public boolean tryExactActivationAtNoProgressBoundary(
            Task diagnosticOwner,
            AltoClef mod) {
        AutoDepositTrustedDestinationCandidate candidate =
                state.candidateAttempt().current().candidate();
        if (!binding.matches(candidate.position())
                || candidateValidator.refusal(mod, candidate).isPresent()) {
            return false;
        }
        HomeStorageContainerActivation activation = activationGate.evaluate(
                mod, candidate, state.context().current(), false
        );
        if (!activation.ready()) {
            return false;
        }
        sessionActivator.activate(diagnosticOwner, mod, candidate, activation);
        return state.session().current() != null;
    }

    private void handleActivationRefusal(
            Task diagnosticOwner,
            AltoClef mod,
            HomeStorageContainerActivation activation) {
        if (activation.status()
                == HomeStorageContainerActivationStatus.EXACT_BINDING_MISSING) {
            transitionToCandidatePhase();
            return;
        }
        if (activation.status()
                == HomeStorageContainerActivationStatus.CONTEXT_CHANGED) {
            terminator.finish(
                    diagnosticOwner,
                    mod,
                    StoreHomeResult.CONTEXT_CHANGED,
                    activation.reason()
            );
            return;
        }
        if (activation.status()
                == HomeStorageContainerActivationStatus.CURSOR_NOT_EMPTY) {
            terminator.finish(
                    diagnosticOwner,
                    mod,
                    StoreHomeResult.CURSOR_NOT_EMPTY,
                    activation.reason()
            );
            return;
        }
        if (activation.status()
                == HomeStorageContainerActivationStatus.PENDING_TRANSFER) {
            terminator.finish(
                    diagnosticOwner,
                    mod,
                    StoreHomeResult.TRANSFER_UNCONFIRMED,
                    activation.reason()
            );
            return;
        }
        candidateRejector.reject(
                diagnosticOwner,
                mod,
                activation.reason(),
                StoreHomeCandidateFailureKind.UNAVAILABLE
        );
    }

    private void transitionToCandidatePhase() {
        state.lifecycle().transitionTo(timeoutLifecycle.candidatePhase()
                == StoreHomeCandidateTimeoutPhase.OPEN_AND_BIND_CANDIDATE
                ? StoreHomePhase.OPEN_AND_BIND_CANDIDATE
                : StoreHomePhase.NAVIGATE_TO_CANDIDATE);
    }
}
