package lavi.minecraft.task.container.deposit.auto.maintenance;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.container.DepositAllTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.store.deposit.pressure.AutoDepositObservationOwner;
import lavi.minecraft.diagnostics.observation.ObservationScope;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureReader;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.maintenance.child.AutoDepositChildCompletion;
import lavi.minecraft.task.container.deposit.auto.maintenance.child.AutoDepositGeneralTaskFactory;
import lavi.minecraft.task.container.deposit.auto.maintenance.child.AutoDepositTrustedTaskFactory;
import lavi.minecraft.task.container.deposit.auto.maintenance.diagnostics.AutoDepositMaintenanceDiagnostics;
import lavi.minecraft.task.container.deposit.auto.maintenance.relief.AutoDepositFreeSlotVerdict;
import lavi.minecraft.task.container.deposit.auto.maintenance.relief.AutoDepositFreeSlotVerifier;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunCompletion;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunReason;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunResult;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositWorkingSetStatus;
import lavi.minecraft.task.container.deposit.auto.maintenance.plan.AutoDepositVerificationPlan;
import lavi.minecraft.task.container.deposit.auto.maintenance.working.AutoDepositRecoveryResumeState;
import lavi.minecraft.task.container.deposit.auto.maintenance.working.AutoDepositWorkingSetVerifier;
import lavi.minecraft.task.container.deposit.auto.maintenance.working.AutoDepositWorkingSetVerdict;
import lavi.minecraft.task.container.deposit.auto.pressure.AutoDepositInventoryPressureSource;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;
import lavi.minecraft.task.container.deposit.auto.recovery.AutoDepositDestinationManifest;
import lavi.minecraft.task.container.deposit.auto.recovery.AutoDepositDestinationManifestLifecycle;
import lavi.minecraft.task.container.deposit.auto.recovery.AutoDepositWorkingSetRecovery;
import lavi.minecraft.task.container.deposit.auto.recovery.RecoverReservedItemsTask;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.execution.AutoDepositTrustedStoreTask;
import lavi.minecraft.task.container.deposit.auto.trusted.execution.AutoDepositTrustedStoreOutcome;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.deposit.auto.working.PlayerInventorySnapshotReader;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetSnapshot;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

//20260827_kpopmodder: Execute one immutable auto-deposit plan and verify working-set and slot relief.
public final class AutoDepositMaintenanceTask extends Task implements AutoDepositObservationOwner {
    private final AutoDepositPlan plan;
    private final AutoDepositGeneralTaskFactory generalTaskFactory;
    private final AutoDepositTrustedTaskFactory trustedTaskFactory;
    private List<DepositAllTask> generalTasks;
    private AutoDepositTrustedStoreTask trustedTask;
    private final AutoDepositDestinationManifestLifecycle manifestLifecycle;
    private final AutoDepositWorkingSetRecovery workingSetRecovery;
    private final AutoDepositFreeSlotVerifier freeSlotVerifier;
    //20260914_kpopmodder: Keep root-local evidence separate from the chain-owned logical start and pressure budget.
    private final DepositAllInventoryPressureSnapshot startingPressure;
    private final AutoDepositWorkingSetVerifier workingSetVerifier;
    private final AutoDepositRunCompletion completion = new AutoDepositRunCompletion();
    private final Consumer<AutoDepositMaintenanceTask> executionTickObserver;
    private final AutoDepositRecoveryResumeState recoveryResume;
    private final AutoDepositMaintenanceDiagnostics diagnostics;
    private AutoDepositMaintenancePhase phase;
    private AutoDepositMaintenanceOutcome outcome = AutoDepositMaintenanceOutcome.PENDING;
    private int generalTaskIndex;
    private boolean childrenComplete;
    private AutoDepositWorkingSetStatus workingSetStatus = AutoDepositWorkingSetStatus.NOT_EVALUATED;

    public AutoDepositMaintenanceTask(
            AutoDepositPlan plan,
            AutoDepositTrustedDestinationRepository trustedRepository) {
        this(
                plan,
                trustedRepository,
                AutoDepositExactOpenContainerBinding.UNAVAILABLE,
                new DepositAllInventoryPressureReader()
        );
    }

    public AutoDepositMaintenanceTask(
            AutoDepositPlan plan,
            AutoDepositTrustedDestinationRepository trustedRepository,
            AutoDepositExactOpenContainerBinding exactOpenContainerBinding) {
        this(
                plan,
                trustedRepository,
                exactOpenContainerBinding,
                new DepositAllInventoryPressureReader()
        );
    }

    AutoDepositMaintenanceTask(
            AutoDepositPlan plan,
            AutoDepositTrustedDestinationRepository trustedRepository,
            AutoDepositExactOpenContainerBinding exactOpenContainerBinding,
            AutoDepositInventoryPressureSource pressureSource) {
        this(plan, trustedRepository, exactOpenContainerBinding, pressureSource, ignored -> { });
    }

    //20260914_kpopmodder: Report only actual selected maintenance ticks to the owning chain.
    public AutoDepositMaintenanceTask(
            AutoDepositPlan plan,
            AutoDepositTrustedDestinationRepository trustedRepository,
            AutoDepositExactOpenContainerBinding exactOpenContainerBinding,
            AutoDepositInventoryPressureSource pressureSource,
            Consumer<AutoDepositMaintenanceTask> executionTickObserver) {
        this(plan, trustedRepository, exactOpenContainerBinding, pressureSource, executionTickObserver, false);
    }

    //20260914_kpopmodder: Resume verification only when the owning chain retained authoritative child-completion evidence.
    public AutoDepositMaintenanceTask(
            AutoDepositPlan plan,
            AutoDepositTrustedDestinationRepository trustedRepository,
            AutoDepositExactOpenContainerBinding exactOpenContainerBinding,
            AutoDepositInventoryPressureSource pressureSource,
            Consumer<AutoDepositMaintenanceTask> executionTickObserver,
            boolean verificationOnly) {
        this(plan, trustedRepository, exactOpenContainerBinding, pressureSource, executionTickObserver,
                verificationOnly, null);
    }

    private AutoDepositMaintenanceTask(
            AutoDepositPlan plan,
            AutoDepositTrustedDestinationRepository trustedRepository,
            AutoDepositExactOpenContainerBinding exactOpenContainerBinding,
            AutoDepositInventoryPressureSource pressureSource,
            Consumer<AutoDepositMaintenanceTask> executionTickObserver,
            boolean verificationOnly,
            AutoDepositRecoveryResumeState recoveryResume) {
        this.plan = Objects.requireNonNull(plan, "plan");
        this.recoveryResume = recoveryResume;
        this.executionTickObserver = Objects.requireNonNull(executionTickObserver, "executionTickObserver");
        Objects.requireNonNull(trustedRepository, "trustedRepository");
        Objects.requireNonNull(exactOpenContainerBinding, "exactOpenContainerBinding");
        Objects.requireNonNull(pressureSource, "pressureSource");
        if (!plan.hasTargets() && !verificationOnly) {
            throw new IllegalArgumentException("automatic deposit plan must contain at least one target");
        }
        AutoDepositDestinationManifest manifest = recoveryResume != null && recoveryResume.manifest() != null
                ? recoveryResume.manifest() : new AutoDepositDestinationManifest(
                plan.context().worldIdentity(),
                plan.context().dimension(),
                plan.context().epoch()
        );
        generalTaskFactory = new AutoDepositGeneralTaskFactory();
        trustedTaskFactory = new AutoDepositTrustedTaskFactory(
                trustedRepository,
                exactOpenContainerBinding
        );
        manifestLifecycle = new AutoDepositDestinationManifestLifecycle(manifest);
        workingSetRecovery = new AutoDepositWorkingSetRecovery(
                recoveryResume == null ? plan.context().workingSet() : recoveryResume.snapshot(),
                manifest,
                new PlayerInventorySnapshotReader()
        );
        //20260829_kpopmodder: Keep occupied-slot verification behind the same read-only pressure port as the chain.
        freeSlotVerifier = new AutoDepositFreeSlotVerifier(pressureSource);
        // The existing plan's occupied count is the 36-slot main-inventory admission snapshot.
        startingPressure = new DepositAllInventoryPressureSnapshot(plan.startingOccupiedSlots(), 36);
        workingSetVerifier = new AutoDepositWorkingSetVerifier(workingSetRecovery);
        diagnostics = new AutoDepositMaintenanceDiagnostics(plan);
        childrenComplete = recoveryResume == null ? verificationOnly : recoveryResume.childrenComplete();
        phase = verificationOnly ? AutoDepositMaintenancePhase.VERIFY_WORKING_SET : plan.trustedTargets().length > 0
                ? AutoDepositMaintenancePhase.DEPOSIT_TRUSTED
                : AutoDepositMaintenancePhase.DEPOSIT_GENERAL;
    }

    //20260914_kpopmodder: Resume the original recovery purpose with fresh children, then require fresh planning.
    public static AutoDepositMaintenanceTask resumeRecovery(AutoDepositPlan currentVerificationPlan,
            AutoDepositMaintenanceTask previous, AutoDepositTrustedDestinationRepository repository,
            AutoDepositExactOpenContainerBinding binding, AutoDepositInventoryPressureSource pressureSource,
            Consumer<AutoDepositMaintenanceTask> executionTickObserver) {
        AutoDepositPlan verification = AutoDepositVerificationPlan.from(currentVerificationPlan);
        AutoDepositRecoveryResumeState recovery = AutoDepositRecoveryResumeState.from(verification, previous);
        return new AutoDepositMaintenanceTask(verification, repository, binding, pressureSource,
                executionTickObserver, true, recovery);
    }

    @Override
    protected void onStart() {
        if (phase == AutoDepositMaintenancePhase.DEPOSIT_TRUSTED
                || phase == AutoDepositMaintenancePhase.DEPOSIT_GENERAL) {
            manifestLifecycle.start(AltoClef.getInstance());
        }
    }

    @Override
    protected Task onTick() {
        executionTickObserver.accept(this);
        if (isFinished()) return null;
        AltoClef mod = AltoClef.getInstance();
        if (!plan.context().matches(mod)) {
            terminate(AutoDepositRunReason.CONTEXT_CHANGED);
            return null;
        }
        if (recoveryResume != null && recoveryResume.refusal().isPresent()) {
            AutoDepositRunReason refused = recoveryResume.refusal().orElseThrow();
            captureTerminal(refused, "prior_recovery_" + refused.name());
            return null;
        }

        switch (phase) {
            case DEPOSIT_TRUSTED -> {
                AutoDepositTrustedStoreTask trustedStore = trustedTask();
                if (trustedStore == null) {
                    captureTerminal(AutoDepositRunReason.TRUSTED_CHILD_FAILED, "trusted_child_unavailable");
                    return null;
                }
                AutoDepositTrustedStoreOutcome trustedOutcome = trustedStore.outcome();
                Optional<AutoDepositRunReason> trustedFailure = AutoDepositChildCompletion.trustedFailure(
                        trustedOutcome, trustedStore.stopped());
                if (trustedFailure.isPresent()) {
                    captureTerminal(trustedFailure.get(), trustedOutcome.name());
                    return null;
                }
                if (trustedOutcome == AutoDepositTrustedStoreOutcome.ALL_STORED) {
                    if (!hasGeneralSteps()) {
                        finishDepositSteps("trusted_steps_complete");
                    } else {
                        transition(AutoDepositMaintenancePhase.DEPOSIT_GENERAL,
                                "trusted_steps_complete", 0);
                    }
                    return null;
                }
                diagnostics.registerTrustedChild(mod, this, trustedStore, plan.trustedTargets());
                return trustedStore;
            }
            case DEPOSIT_GENERAL -> {
                DepositAllTask generalTask = currentGeneralTask();
                if (generalTask == null) {
                    finishDepositSteps("general_steps_complete");
                    return null;
                }
                boolean generalTaskFinished = generalTask.isFinished();
                boolean generalTaskStopped = generalTask.stopped();
                boolean storedTargetsSatisfied = generalTaskFinished && generalTask.automaticStoredTargetsSatisfied();
                Optional<AutoDepositRunReason> generalFailure = AutoDepositChildCompletion.generalFailure(
                        generalTaskFinished, generalTaskStopped, storedTargetsSatisfied);
                if (generalFailure.isPresent()) {
                    captureTerminal(generalFailure.get(), "general_child_" + generalTaskIndex);
                    return null;
                }
                if (generalTaskFinished) {
                    generalTaskIndex++;
                    if (currentGeneralTask() != null) {
                        transition(AutoDepositMaintenancePhase.DEPOSIT_GENERAL,
                                "general_step_terminal", 0);
                    } else {
                        finishDepositSteps("general_steps_complete");
                    }
                    return null;
                }
                diagnostics.registerGeneralChild(
                        mod,
                        this,
                        generalTask,
                        generalTaskIndex,
                        plan.trustedTargets().length > 0,
                        plan.generalTargets()[generalTaskIndex]
                );
                return generalTask;
            }
            case VERIFY_WORKING_SET -> {
                AutoDepositWorkingSetVerdict working = workingSetVerifier.verify(mod);
                workingSetStatus = working.status();
                if (workingSetStatus == AutoDepositWorkingSetStatus.UNAVAILABLE) {
                    captureTerminal(AutoDepositRunReason.WORKING_SET_UNAVAILABLE, "working_set_read_unavailable");
                    return null;
                }
                if (workingSetStatus.satisfiedOrNotApplicable()) {
                    transition(AutoDepositMaintenancePhase.VERIFY_FREE_SLOTS,
                            workingSetStatus.name(), 0);
                    return null;
                }
                RecoverReservedItemsTask recoveryTask = workingSetRecovery.begin();
                if (recoveryTask == null) {
                    captureTerminal(AutoDepositRunReason.WORKING_SET_UNAVAILABLE, "recovery_child_unavailable");
                    return null;
                }
                transition(AutoDepositMaintenancePhase.RECOVER,
                        "working_set_deficit_detected", working.deficitTypes());
                return recoveryTask;
            }
            case RECOVER -> {
                RecoverReservedItemsTask recoveryTask = workingSetRecovery.task();
                if (recoveryTask == null) {
                    captureTerminal(AutoDepositRunReason.WORKING_SET_UNAVAILABLE, "recovery_child_missing");
                    return null;
                }
                RecoverReservedItemsTask.Terminal recoveryTerminal = recoveryTask.terminal();
                if (recoveryTerminal == RecoverReservedItemsTask.Terminal.CANCELLED_CONTEXT_CHANGED) {
                    captureTerminal(AutoDepositRunReason.CONTEXT_CHANGED, recoveryTerminal.name());
                    return null;
                }
                if (recoveryTerminal != RecoverReservedItemsTask.Terminal.RUNNING) {
                    AutoDepositWorkingSetVerdict working = workingSetVerifier.verify(mod);
                    workingSetStatus = working.status();
                    if (workingSetStatus == AutoDepositWorkingSetStatus.UNAVAILABLE) {
                        captureTerminal(AutoDepositRunReason.WORKING_SET_UNAVAILABLE, "recovery_postcondition_unavailable");
                        return null;
                    }
                    if (recoveryTerminal != RecoverReservedItemsTask.Terminal.SATISFIED
                            || !workingSetStatus.satisfiedOrNotApplicable()) {
                        captureTerminal(AutoDepositRunReason.WORKING_SET_DEFICIT,
                                recoveryTerminal.name() + ":remaining=" + working.deficitTypes());
                        return null;
                    }
                    transition(AutoDepositMaintenancePhase.VERIFY_FREE_SLOTS,
                            "recovery_satisfied", 0);
                    return null;
                }
                if (recoveryTask.stopped()) {
                    captureTerminal(AutoDepositRunReason.CHILD_STOPPED, "recovery_child_stopped");
                    return null;
                }
                return recoveryTask;
            }
            case VERIFY_FREE_SLOTS -> {
                //20260914_kpopmodder: The previous phase may be a tick old; use the actual terminal working-set postcondition.
                AutoDepositWorkingSetVerdict working = workingSetVerifier.verify(mod);
                workingSetStatus = working.status();
                if (!workingSetStatus.satisfiedOrNotApplicable()) {
                    captureTerminal(workingSetStatus == AutoDepositWorkingSetStatus.UNAVAILABLE
                                    ? AutoDepositRunReason.WORKING_SET_UNAVAILABLE : AutoDepositRunReason.WORKING_SET_DEFICIT,
                            "terminal_working_set_" + workingSetStatus.name());
                    return null;
                }
                AutoDepositFreeSlotVerdict verdict = freeSlotVerifier.verify(
                        mod,
                        startingPressure,
                        plan.targetReliefSlots()
                );
                outcome = verdict.outcome();
                diagnostics.recordFreeSlotVerdict(
                        plan.context().epoch(),
                        plan.startingOccupiedSlots(),
                        plan.expectedFreedSlots(),
                        verdict,
                        this
                );
                captureCandidate(verdict.available() ? recoveryResume == null ? AutoDepositRunReason.NORMAL
                                : AutoDepositRunReason.REPLAN_REQUIRED
                                : AutoDepositRunReason.PRESSURE_UNAVAILABLE,
                        verdict.observationStatus().name(), verdict.endingPressure());
                transition(AutoDepositMaintenancePhase.DONE, "free_slot_postcondition_observed", 0);
                return null;
            }
            case DONE, CANCELLED -> {
                return null;
            }
        }
        return null;
    }

    private void transition(AutoDepositMaintenancePhase next, String reason, int deficitTypes) {
        AutoDepositMaintenancePhase previous = phase;
        phase = next;
        diagnostics.recordTransition(
                plan.context().epoch(), previous, next, reason, deficitTypes, this
        );
    }

    private void finishDepositSteps(String reason) {
        childrenComplete = true;
        manifestLifecycle.stop();
        transition(AutoDepositMaintenancePhase.VERIFY_WORKING_SET, reason, 0);
    }

    private DepositAllTask currentGeneralTask() {
        List<DepositAllTask> tasks = generalTasks();
        return generalTaskIndex < tasks.size() ? tasks.get(generalTaskIndex) : null;
    }

    private List<DepositAllTask> generalTasks() {
        if (generalTasks == null) {
            generalTasks = generalTaskFactory.create(plan.generalTargets());
        }
        return generalTasks;
    }

    private AutoDepositTrustedStoreTask trustedTask() {
        if (trustedTask == null) {
            trustedTask = trustedTaskFactory.create(plan).orElse(null);
        }
        return trustedTask;
    }

    private boolean hasGeneralSteps() {
        return plan.generalTargets().length > 0;
    }

    @Override
    protected void onStop(Task interruptTask) {
        if (completion.candidate().isEmpty()) terminate(AutoDepositRunReason.UNEXPECTED_STOP);
        manifestLifecycle.stop();
        diagnostics.recordTerminal(
                this,
                "TASK_STOP_CALLBACK",
                phase == null ? "UNAVAILABLE" : phase.name()
        );
    }

    @Override
    public boolean isFinished() {
        return phase == AutoDepositMaintenancePhase.DONE
                || phase == AutoDepositMaintenancePhase.CANCELLED;
    }

    @Override
    protected boolean isEqual(Task other) {
        return this == other;
    }

    @Override
    protected String toDebugString() {
        return "Automatic deposit maintenance: " + phase;
    }

    public AutoDepositPlan plan() {
        return plan;
    }

    //20260913_kpopmodder: Expose only the already-captured diagnostic handle, never re-evaluate Task state.
    @Override
    public ObservationScope diagnosticObservationScope() {
        return diagnostics == null ? ObservationScope.NOOP : diagnostics.observationScope();
    }

    public boolean diagnosticAutomaticRunEnabled() {
        return diagnostics.automaticRunEnabled();
    }

    public WorkingSetSnapshot snapshot() {
        return workingSetRecovery.snapshot();
    }

    //20260914_kpopmodder: Check the original reservation debt; a fresh resume plan must not erase missing items.
    public AutoDepositWorkingSetStatus verifyWorkingSetBeforeResume() {
        return workingSetVerifier.verify(AltoClef.getInstance()).status();
    }

    public DepositAllTask depositTask() {
        List<DepositAllTask> tasks = generalTasks();
        return tasks.isEmpty() ? null : tasks.get(0);
    }

    public Task primaryDepositTask() {
        if (plan.trustedTargets().length > 0) {
            return trustedTask();
        }
        return depositTask();
    }

    public AutoDepositMaintenancePhase phase() {
        return phase;
    }

    public AutoDepositMaintenanceOutcome outcome() {
        return outcome;
    }

    //20260914_kpopmodder: Capture only; the chain performs owned stop/detach and settles cleanup afterwards.
    public void terminate(AutoDepositRunReason reason) {
        Objects.requireNonNull(reason, "reason");
        if (completion.candidate().isPresent()) return;
        // Preserve the current original-reservation evidence without treating defense itself as a failure.
        workingSetStatus = verifyWorkingSetBeforeResume();
        // Preserve authoritative child failures even when safety arrives before the next parent evaluation.
        if (completion.candidate().isEmpty() && trustedTask != null) {
            AutoDepositTrustedStoreOutcome childOutcome = trustedTask.outcome();
            if (childOutcome == AutoDepositTrustedStoreOutcome.CANDIDATES_EXHAUSTED) {
                captureTerminal(AutoDepositRunReason.TRUSTED_CHILD_FAILED, childOutcome.name());
                return;
            }
            if (childOutcome == AutoDepositTrustedStoreOutcome.CONTEXT_CHANGED) {
                captureTerminal(AutoDepositRunReason.CONTEXT_CHANGED, childOutcome.name());
                return;
            }
        }
        if (completion.candidate().isEmpty() && workingSetRecovery.task() != null) {
            RecoverReservedItemsTask.Terminal recoveryTerminal = workingSetRecovery.task().terminal();
            if (recoveryTerminal == RecoverReservedItemsTask.Terminal.EXHAUSTED) {
                workingSetStatus = AutoDepositWorkingSetStatus.DEFICIT;
                captureTerminal(AutoDepositRunReason.WORKING_SET_DEFICIT, recoveryTerminal.name());
                return;
            }
            if (recoveryTerminal == RecoverReservedItemsTask.Terminal.CANCELLED_CONTEXT_CHANGED) {
                captureTerminal(AutoDepositRunReason.CONTEXT_CHANGED, recoveryTerminal.name());
                return;
            }
        }
        if (completion.candidate().isEmpty() && generalTasks != null && generalTaskIndex < generalTasks.size()
                && generalTasks.get(generalTaskIndex).stopped()
                && !generalTasks.get(generalTaskIndex).automaticStoredTargetsSatisfied()) {
            captureTerminal(AutoDepositRunReason.CHILD_STOPPED, "general_child_stopped_before_termination");
            return;
        }
        captureTerminal(reason, reason.name());
    }

    private void captureTerminal(AutoDepositRunReason reason, String detail) {
        if (completion.candidate().isPresent()) return;
        outcome = AutoDepositMaintenanceOutcome.CANCELLED;
        captureCandidate(reason, detail, freeSlotVerifier.observe(AltoClef.getInstance()));
        transition(AutoDepositMaintenancePhase.CANCELLED, detail, 0);
    }

    private void captureCandidate(AutoDepositRunReason reason, String detail,
            Optional<DepositAllInventoryPressureSnapshot> endingPressure) {
        childrenComplete = childrenComplete || capturedChildrenComplete();
        AutoDepositRunResult candidate = new AutoDepositRunResult(reason, Optional.of(startingPressure),
                endingPressure, childrenComplete, workingSetStatus, false, detail, outcome);
        if (completion.capture(candidate)) diagnostics.recordRunResult(candidate, this, false);
    }

    public Optional<AutoDepositRunResult> completionCandidate() { return completion.candidate(); }
    public Optional<AutoDepositRunResult> result() { return completion.result(); }

    public Optional<AutoDepositRunResult> finalizeAfterCleanup(boolean cleanupComplete) {
        if (completion.candidate().isEmpty()) terminate(AutoDepositRunReason.UNEXPECTED_STOP);
        if (completion.result().isPresent()) return completion.result();
        Optional<AutoDepositRunResult> result;
        if (cleanupComplete) {
            AltoClef mod = AltoClef.getInstance();
            AutoDepositWorkingSetStatus postCleanupWorking = workingSetVerifier.verify(mod).status();
            AutoDepositFreeSlotVerdict postCleanupPressure = freeSlotVerifier.verify(mod, startingPressure, plan.targetReliefSlots());
            result = completion.finalizeAfterCleanup(true, postCleanupPressure, postCleanupWorking);
        } else {
            result = completion.finalizeAfterCleanup(false);
        }
        result.ifPresent(value -> diagnostics.recordRunResult(value, this, true));
        return result;
    }

    /** Read native transfer totals without creating children or evaluating any Task's priority/completion. */
    public int confirmedStoredCount() {
        long count = trustedTask == null ? 0 : trustedTask.confirmedStoredCount();
        if (generalTasks != null) {
            for (DepositAllTask task : generalTasks) count += task.automaticStoredCount();
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, count));
    }

    /** Read confirmed recovery separately; restoring required items is not storage transfer progress. */
    public int confirmedRecoveredCount() {
        RecoverReservedItemsTask recovery = workingSetRecovery == null ? null : workingSetRecovery.task();
        return recovery == null ? 0 : recovery.confirmedRecoveredCount();
    }

    private boolean capturedChildrenComplete() {
        if (recoveryResume != null) return recoveryResume.childrenComplete();
        if (plan.trustedTargets().length > 0
                && (trustedTask == null || trustedTask.outcome() != AutoDepositTrustedStoreOutcome.ALL_STORED)) return false;
        if (plan.generalTargets().length == 0) return true;
        if (generalTasks == null || generalTasks.size() != plan.generalTargets().length) return false;
        for (DepositAllTask child : generalTasks) {
            if (!child.automaticStoredTargetsSatisfied()) return false;
        }
        return true;
    }

    public AutoDepositDestinationManifest manifest() {
        return manifestLifecycle.manifest();
    }
}
