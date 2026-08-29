package lavi.minecraft.task.container.home.execution.task.lifecycle;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.home.timeout.StoreHomeTimeoutDiagnostics;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositWorldKeyReader;
import lavi.minecraft.task.container.home.execution.StoreHomePhase;
import lavi.minecraft.task.container.home.execution.StoreHomeResult;
import lavi.minecraft.task.container.home.execution.candidate.navigation.StoreHomeCandidateNavigationStep;
import lavi.minecraft.task.container.home.execution.candidate.selection.StoreHomeCandidateAttemptStarter;
import lavi.minecraft.task.container.home.execution.candidate.view.StoreHomeCandidateRuntimeView;
import lavi.minecraft.task.container.home.execution.context.HomeStorageCursorStateReader;
import lavi.minecraft.task.container.home.execution.initialization.StoreHomeRequestInitializer;
import lavi.minecraft.task.container.home.execution.operation.pending.StoreHomePendingOwnershipGuard;
import lavi.minecraft.task.container.home.execution.operation.terminal.StoreHomeOperationTerminator;
import lavi.minecraft.task.container.home.execution.session.flow.StoreHomeSessionStep;
import lavi.minecraft.task.container.home.execution.state.StoreHomeExecutionState;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutLifecycle;
import lavi.minecraft.task.container.home.execution.timeout.candidate.StoreHomeCandidateTimeoutObserver;
import lavi.minecraft.task.container.home.execution.timeout.candidate.StoreHomeCandidateTimeoutPhase;
import lavi.minecraft.task.container.home.execution.timeout.decision.StoreHomeOperationNoProgressCheckpoint;
import lavi.minecraft.task.container.home.execution.timeout.decision.StoreHomeTimeoutDecisionApplier;
import lavi.minecraft.task.container.home.execution.timeout.decision.StoreHomeTimeoutEvaluationOrder;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

//20260829_kpopmodder: Own the ordered StoreHomeTask start, active-root tick, and stop lifecycle.
public final class StoreHomeTaskLifecycleController {
    private final AutoDepositWorldKeyReader worldKeyReader;
    private final StoreHomeTimeoutLifecycle timeoutLifecycle;
    private final StoreHomeTimeoutDiagnostics timeoutDiagnostics;
    private final StoreHomeExecutionState state;
    private final HomeStorageCursorStateReader cursorStateReader;
    private final StoreHomePendingOwnershipGuard pendingGuard;
    private final StoreHomeOperationTerminator terminator;
    private final StoreHomeRequestInitializer initializer;
    private final StoreHomeCandidateAttemptStarter candidateStarter;
    private final StoreHomeCandidateTimeoutObserver candidateTimeoutObserver;
    private final StoreHomeCandidateNavigationStep navigationStep;
    private final StoreHomeSessionStep sessionStep;
    private final StoreHomeTimeoutDecisionApplier timeoutDecisionApplier;
    private final StoreHomeCandidateRuntimeView candidateView;

    public StoreHomeTaskLifecycleController(
            AutoDepositWorldKeyReader worldKeyReader,
            StoreHomeTimeoutLifecycle timeoutLifecycle,
            StoreHomeTimeoutDiagnostics timeoutDiagnostics,
            StoreHomeExecutionState state,
            HomeStorageCursorStateReader cursorStateReader,
            StoreHomePendingOwnershipGuard pendingGuard,
            StoreHomeOperationTerminator terminator,
            StoreHomeRequestInitializer initializer,
            StoreHomeCandidateAttemptStarter candidateStarter,
            StoreHomeCandidateTimeoutObserver candidateTimeoutObserver,
            StoreHomeCandidateNavigationStep navigationStep,
            StoreHomeSessionStep sessionStep,
            StoreHomeTimeoutDecisionApplier timeoutDecisionApplier,
            StoreHomeCandidateRuntimeView candidateView) {
        this.worldKeyReader = Objects.requireNonNull(
                worldKeyReader, "worldKeyReader"
        );
        this.timeoutLifecycle = Objects.requireNonNull(
                timeoutLifecycle, "timeoutLifecycle"
        );
        this.timeoutDiagnostics = Objects.requireNonNull(
                timeoutDiagnostics, "timeoutDiagnostics"
        );
        this.state = Objects.requireNonNull(state, "state");
        this.cursorStateReader = Objects.requireNonNull(
                cursorStateReader, "cursorStateReader"
        );
        this.pendingGuard = Objects.requireNonNull(pendingGuard, "pendingGuard");
        this.terminator = Objects.requireNonNull(terminator, "terminator");
        this.initializer = Objects.requireNonNull(initializer, "initializer");
        this.candidateStarter = Objects.requireNonNull(
                candidateStarter, "candidateStarter"
        );
        this.candidateTimeoutObserver = Objects.requireNonNull(
                candidateTimeoutObserver, "candidateTimeoutObserver"
        );
        this.navigationStep = Objects.requireNonNull(
                navigationStep, "navigationStep"
        );
        this.sessionStep = Objects.requireNonNull(sessionStep, "sessionStep");
        this.timeoutDecisionApplier = Objects.requireNonNull(
                timeoutDecisionApplier, "timeoutDecisionApplier"
        );
        this.candidateView = Objects.requireNonNull(
                candidateView, "candidateView"
        );
    }

    public void onStart(Task diagnosticOwner) {
        if (!state.lifecycle().pending()) {
            return;
        }
        state.lifecycle().transitionTo(state.lifecycle().initialized()
                ? StoreHomePhase.REVALIDATE_AFTER_RESUME
                : StoreHomePhase.ACCEPT_REQUEST);
        timeoutDiagnostics.recordOperationStarted(
                diagnosticOwner,
                state.lifecycle().phase(),
                state.operation().current(),
                timeoutLifecycle.observation()
        );
    }

    public Task onTick(Task diagnosticOwner) {
        AltoClef mod = AltoClef.getInstance();
        if (!state.lifecycle().pending()) {
            return null;
        }
        if (!state.lifecycle().initialized()) {
            initializer.initialize(diagnosticOwner, mod);
            if (!state.lifecycle().pending()) {
                return null;
            }
        }

        return runSafetySequence(
                timeoutLifecycle::onActiveRootTick,
                () -> {
                    if (pendingGuard.ownershipMatches()) {
                        return false;
                    }
                    terminator.finish(
                            diagnosticOwner,
                            mod,
                            StoreHomeResult.TRANSFER_UNCONFIRMED,
                            "task_executor_pending_ownership_mismatch"
                    );
                    return true;
                },
                () -> {
                    if (state.context().current().matches(mod, worldKeyReader)) {
                        return false;
                    }
                    terminator.finish(
                            diagnosticOwner,
                            mod,
                            StoreHomeResult.CONTEXT_CHANGED,
                            "world_or_dimension_changed"
                    );
                    return true;
                },
                () -> {
                    if (cursorStateReader.isEmpty(mod)) {
                        return false;
                    }
                    terminator.finish(
                            diagnosticOwner,
                            mod,
                            StoreHomeResult.CURSOR_NOT_EMPTY,
                            "cursor_not_empty_during_operation"
                    );
                    return true;
                },
                () -> timeoutDecisionApplier.applyOperation(
                        diagnosticOwner,
                        mod,
                        timeoutLifecycle.operationEmergencyTimeoutReason()
                ),
                () -> tickBehavior(diagnosticOwner, mod)
        );
    }

    private Task tickBehavior(Task diagnosticOwner, AltoClef mod) {
        StoreHomeOperationNoProgressCheckpoint noProgressCheckpoint =
                StoreHomeTimeoutEvaluationOrder.operationNoProgressCheckpoint(
                        state.candidateAttempt().current() != null,
                        state.session().current() != null,
                        pendingGuard.hasAnyPending()
                );
        if (runCandidateEntrySequence(
                () -> noProgressCheckpoint
                        == StoreHomeOperationNoProgressCheckpoint
                        .RESOLVE_BEFORE_BEHAVIOR
                        && timeoutDecisionApplier.applyOperation(
                        diagnosticOwner,
                        mod,
                        timeoutLifecycle.operationNoProgressTimeoutReason()
                ),
                () -> {
                    if (candidateStarter.ensure(diagnosticOwner, mod)) {
                        return false;
                    }
                    terminator.finishExhausted(
                            diagnosticOwner,
                            mod,
                            "trusted_candidates_exhausted"
                    );
                    return true;
                },
                () -> candidateTimeoutObserver.observe(mod)
        )) {
            return null;
        }

        if (state.session().current() == null) {
            state.lifecycle().transitionTo(timeoutLifecycle.candidatePhase()
                    == StoreHomeCandidateTimeoutPhase.OPEN_AND_BIND_CANDIDATE
                    ? StoreHomePhase.OPEN_AND_BIND_CANDIDATE
                    : StoreHomePhase.NAVIGATE_TO_CANDIDATE);
        }
        if (noProgressCheckpoint == StoreHomeOperationNoProgressCheckpoint
                .OBSERVE_POSITION_AND_TRY_EXACT_ACTIVATION_ONLY
                && timeoutLifecycle.operationNoProgressTimeoutReason().isPresent()
                && runNoProgressActivationSequence(
                () -> navigationStep.tryExactActivationAtNoProgressBoundary(
                        diagnosticOwner, mod
                ),
                () -> state.lifecycle().pending(),
                () -> timeoutDecisionApplier.applyOperation(
                        diagnosticOwner,
                        mod,
                        timeoutLifecycle.operationNoProgressTimeoutReason()
                ),
                () -> recordProgress(diagnosticOwner, mod, null)
        )) {
            return null;
        }

        return runNormalStepSequence(
                () -> state.session().current() == null
                        ? navigationStep.tick(diagnosticOwner, mod)
                        : sessionStep.tick(diagnosticOwner, mod),
                () -> state.lifecycle().pending(),
                () -> timeoutDecisionApplier.applyOperation(
                        diagnosticOwner,
                        mod,
                        timeoutLifecycle.operationNoProgressTimeoutReason()
                ),
                () -> state.candidateAttempt().current() != null,
                () -> timeoutDecisionApplier.applyCandidate(
                        diagnosticOwner, mod
                ),
                () -> state.lifecycle().pending()
                        && state.candidateAttempt().current() != null,
                nextTask -> recordProgress(diagnosticOwner, mod, nextTask)
        );
    }

    public void onStop(Task diagnosticOwner, Task interruptTask) {
        if (!state.lifecycle().pending()) {
            return;
        }
        if (interruptTask != null) {
            terminator.finish(
                    diagnosticOwner,
                    AltoClef.getInstance(),
                    StoreHomeResult.INTERRUPTED,
                    "replaced_by_new_user_task"
            );
        } else {
            state.lifecycle().transitionTo(StoreHomePhase.SUSPENDED);
        }
    }

    //20260829_kpopmodder: Expose behavior-neutral sequencing for direct lifecycle order tests.
    static <T> T runSafetySequence(
            Runnable activeRootTick,
            BooleanSupplier pendingOwnershipStops,
            BooleanSupplier contextStops,
            BooleanSupplier cursorStops,
            BooleanSupplier emergencyStops,
            Supplier<T> behavior) {
        activeRootTick.run();
        if (pendingOwnershipStops.getAsBoolean()) {
            return null;
        }
        if (contextStops.getAsBoolean()) {
            return null;
        }
        if (cursorStops.getAsBoolean()) {
            return null;
        }
        if (emergencyStops.getAsBoolean()) {
            return null;
        }
        return behavior.get();
    }

    static boolean runCandidateEntrySequence(
            BooleanSupplier operationNoProgressStops,
            BooleanSupplier candidateAcquisitionStops,
            Runnable candidateObservation) {
        if (operationNoProgressStops.getAsBoolean()) {
            return true;
        }
        if (candidateAcquisitionStops.getAsBoolean()) {
            return true;
        }
        candidateObservation.run();
        return false;
    }

    static boolean runNoProgressActivationSequence(
            BooleanSupplier exactActivation,
            BooleanSupplier rootStillPending,
            BooleanSupplier operationNoProgressStops,
            Runnable activatedProgress) {
        boolean activated = exactActivation.getAsBoolean();
        if (!rootStillPending.getAsBoolean()) {
            return true;
        }
        if (operationNoProgressStops.getAsBoolean()) {
            return true;
        }
        if (!activated) {
            return false;
        }
        activatedProgress.run();
        return true;
    }

    static <T> T runNormalStepSequence(
            Supplier<T> step,
            BooleanSupplier rootStillPending,
            BooleanSupplier operationNoProgressStops,
            BooleanSupplier candidateStillActive,
            BooleanSupplier candidateTimeoutStops,
            BooleanSupplier progressStillEligible,
            Consumer<T> progress) {
        T nextTask = step.get();
        if (!rootStillPending.getAsBoolean()) {
            return null;
        }
        if (operationNoProgressStops.getAsBoolean()) {
            return null;
        }
        if (!candidateStillActive.getAsBoolean()) {
            return null;
        }
        if (candidateTimeoutStops.getAsBoolean()) {
            return null;
        }
        if (progressStillEligible.getAsBoolean()) {
            progress.accept(nextTask);
        }
        return nextTask;
    }

    private void recordProgress(
            Task diagnosticOwner,
            AltoClef mod,
            Task nextTask) {
        timeoutDiagnostics.recordProgress(
                diagnosticOwner,
                mod,
                state.lifecycle().phase(),
                state.operation().current(),
                timeoutLifecycle.observation(),
                state.context().current(),
                state.candidateAttempt().current(),
                state.session().current(),
                candidateView.remainingCandidateCount(),
                nextTask
        );
    }
}
