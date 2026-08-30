package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.SingleTaskChain;
import adris.altoclef.chains.UserTaskChain;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.container.store.deposit.StoreDepositDiagnostics;
import lavi.minecraft.task.container.deposit.auto.composition.DepositAllInventoryPressureChainPreparation;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceTask;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositDecisionFingerprint;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlanningResult;
import lavi.minecraft.task.container.deposit.auto.policy.diagnostics.AutoDepositPolicyDiagnostics;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPolicyEngine;
import lavi.minecraft.task.container.deposit.auto.pressure.AutoDepositInventoryPressureSource;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.deposit.auto.working.ActiveTaskWorkingSetResolver;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetResolution;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetSnapshot;

import java.util.Objects;
import java.util.Optional;

//20260827_kpopmodder: Apply one automatic-only immutable policy plan at 33/36 inventory pressure.
public final class DepositAllInventoryPressureChain extends SingleTaskChain {
    public static final float PRIORITY = 51.0f;

    private final AltoClef mod;
    private final TaskRunner runner;
    private final AutoDepositInventoryPressureSource pressureSource;
    private final DepositAllInventoryPressureStateMachine stateMachine;
    private final DepositAllAutoConflictGuard conflictGuard;
    private final ActiveTaskWorkingSetResolver workingSetResolver;
    private final AutoDepositPolicyEngine policyEngine;
    private final AutoDepositTrustedDestinationRepository trustedRepository;
    private final AutoDepositExactOpenContainerBinding exactOpenContainerBinding;
    private String lastDeferredReason;
    private Task lastDeferredRoot;
    private WorkingSetSnapshot lastNoSafeWorkingSet;
    private long lastNoSafeEpoch;
    private long waitingTrustedRevision;
    private long activeRunTrustedRevision = -1L;
    private Task activeDiagnosticMaintenanceTask;

    public DepositAllInventoryPressureChain(TaskRunner runner) {
        this(DepositAllInventoryPressureChainPreparation.prepare(
                runner,
                AutoDepositPolicyEngine.inMemoryDefault()
        ));
    }

    public DepositAllInventoryPressureChain(TaskRunner runner,
                                            AutoDepositPolicyEngine policyEngine) {
        this(DepositAllInventoryPressureChainPreparation.prepare(runner, policyEngine));
    }

    public DepositAllInventoryPressureChain(
            TaskRunner runner,
            AutoDepositPolicyEngine policyEngine,
            AutoDepositTrustedDestinationRepository trustedRepository) {
        this(DepositAllInventoryPressureChainPreparation.prepare(
                runner,
                policyEngine,
                trustedRepository,
                AutoDepositExactOpenContainerBinding.UNAVAILABLE
        ));
    }

    public DepositAllInventoryPressureChain(
            TaskRunner runner,
            AutoDepositPolicyEngine policyEngine,
            AutoDepositTrustedDestinationRepository trustedRepository,
            AutoDepositExactOpenContainerBinding exactOpenContainerBinding) {
        this(DepositAllInventoryPressureChainPreparation.prepare(
                runner,
                policyEngine,
                trustedRepository,
                exactOpenContainerBinding
        ));
    }

    private DepositAllInventoryPressureChain(
            DepositAllInventoryPressureChainPreparation preparation) {
        super(preparation.runner());
        runner = preparation.runner();
        mod = preparation.mod();
        pressureSource = preparation.pressureSource();
        stateMachine = preparation.stateMachine();
        conflictGuard = preparation.conflictGuard();
        workingSetResolver = preparation.workingSetResolver();
        policyEngine = preparation.policyEngine();
        trustedRepository = preparation.trustedRepository();
        exactOpenContainerBinding = preparation.exactOpenContainerBinding();
        waitingTrustedRevision = preparation.initialTrustedRevision();
    }

    static DepositAllInventoryPressureChain commit(
            DepositAllInventoryPressureChainPreparation preparation) {
        return new DepositAllInventoryPressureChain(
                Objects.requireNonNull(preparation, "preparation")
        );
    }

    @Override
    public float getPriority() {
        return isActive() ? PRIORITY : Float.NEGATIVE_INFINITY;
    }

    public void onEndClientTick() {
        if (!AltoClef.inGame()) {
            stopOwnedRun("left_game", null);
            return;
        }
        if (!mod.getAiBridge().getEnabled()) {
            stopOwnedRun("chatclef_disabled", null);
            return;
        }
        if (stateMachine.state() == DepositAllInventoryPressureState.RUNNING) {
            if (mainTask == null) {
                if (activeDiagnosticMaintenanceTask != null) {
                    StoreDepositDiagnostics.recordAutomaticMaintenanceTerminal(
                            activeDiagnosticMaintenanceTask,
                            "RUNNING_TASK_MISSING",
                            "UNAVAILABLE_MAINTENANCE_TASK"
                    );
                }
                transitionRunToWaiting("running_task_missing", currentSnapshot(), null);
            }
            return;
        }

        Optional<DepositAllInventoryPressureSnapshot> snapshotOptional = pressureSource.read(mod);
        if (snapshotOptional.isEmpty()) {
            return;
        }
        DepositAllInventoryPressureSnapshot snapshot = snapshotOptional.get();

        if (snapshot.isAtOrBelowLowWater()) {
            clearDeferredFingerprint();
            clearNoSafeContext();
            observeLowWater(snapshot);
            return;
        }

        if (stateMachine.state() == DepositAllInventoryPressureState.WAIT_FOR_REARM) {
            long currentTrustedRevision = trustedRepository.revision();
            if (currentTrustedRevision != waitingTrustedRevision
                    && stateMachine.observeExplicitPolicyChange()
                    == DepositAllInventoryPressureSignal.MEANINGFUL_CHANGE) {
                waitingTrustedRevision = currentTrustedRevision;
                clearDeferredFingerprint();
                clearNoSafeContext();
                DepositAllAutoDiagnostics.logMeaningfulReevaluation(
                        snapshot, currentUserTaskRoot()
                );
            }
        }

        if (stateMachine.state() == DepositAllInventoryPressureState.NO_SAFE_SURPLUS_WAIT) {
            if (!snapshot.isAtOrAboveThreshold()) {
                return;
            }
            AutoDepositDecisionFingerprint current = policyEngine.captureDecisionFingerprint(
                    mod, snapshot, lastNoSafeWorkingSet, lastNoSafeEpoch
            );
            if (stateMachine.observeMeaningfulChange(current)
                    != DepositAllInventoryPressureSignal.MEANINGFUL_CHANGE) {
                return;
            }
            Task root = currentUserTaskRoot();
            clearNoSafeContext();
            DepositAllAutoDiagnostics.logMeaningfulReevaluation(snapshot, root);
        }

        if (!snapshot.isAtOrAboveThreshold()) {
            stateMachine.observe(snapshot);
            return;
        }
        if (stateMachine.state() != DepositAllInventoryPressureState.ARMED) {
            return;
        }

        UserTaskChain userTaskChain = mod.getUserTaskChain();
        Task userTaskRoot = userTaskChain == null
                ? null
                : userTaskChain.getCurrentTask();
        boolean activeUserTask = userTaskRoot != null
                && !userTaskChain.isRunningIdleTask();

        if (suppressExistingStoreHome(snapshot, userTaskChain, userTaskRoot)) {
            return;
        }

        if (activeUserTask && runner.getCurrentTaskChain() != userTaskChain) {
            latchNoSafe(
                    "user_task_chain_not_selected",
                    snapshot,
                    userTaskRoot,
                    policyEngine.captureGateFingerprint(mod),
                    null,
                    0L
            );
            return;
        }

        if (conflictGuard.hasExistingDepositTask(userTaskRoot)) {
            DepositAllInventoryPressureSignal signal = stateMachine.observe(snapshot);
            if (signal == DepositAllInventoryPressureSignal.THRESHOLD_REACHED) {
                transitionArmedToWaiting("existing_deposit_task", snapshot, userTaskRoot);
            }
            return;
        }

        WorkingSetSnapshot workingSet = null;
        if (activeUserTask) {
            WorkingSetResolution resolution = workingSetResolver.resolve(mod);
            if (resolution.status() != WorkingSetResolution.Status.SUPPORTED) {
                latchNoSafe(
                        resolution.reason(),
                        snapshot,
                        userTaskRoot,
                        policyEngine.captureGateFingerprint(mod),
                        null,
                        0L
                );
                return;
            }
            workingSet = resolution.snapshot();
        }

        AutoDepositPlanningResult planning = policyEngine.plan(mod, snapshot, workingSet);
        if (planning.status() != AutoDepositPlanningResult.Status.READY) {
            AutoDepositPolicyDiagnostics.log(
                    planning.diagnosticPlan().orElse(null),
                    snapshot,
                    planning.status().name(),
                    planning.reason(),
                    userTaskRoot
            );
        }
        if (planning.status() == AutoDepositPlanningResult.Status.CONTEXT_CHANGED) {
            deferChanged(planning.reason(), snapshot, userTaskRoot);
            return;
        }
        if (planning.status() != AutoDepositPlanningResult.Status.READY) {
            AutoDepositPlan retainedPlan = planning.plan().orElse(null);
            WorkingSetSnapshot retainedWorkingSet = retainedPlan == null
                    ? workingSet
                    : retainedPlan.context().workingSet();
            long retainedEpoch = retainedPlan == null ? 0L : retainedPlan.context().epoch();
            latchNoSafe(
                    planning.reason(),
                    snapshot,
                    userTaskRoot,
                    policyEngine.captureDecisionFingerprint(
                            mod, snapshot, retainedWorkingSet, retainedEpoch
                    ),
                    retainedWorkingSet,
                    retainedEpoch
            );
            return;
        }

        startPlan(snapshot, planning.plan().orElseThrow());
    }

    //20260829_kpopmodder: Suppress automatic storage from the exact StoreHome root before selected-chain admission.
    boolean suppressExistingStoreHome(
            DepositAllInventoryPressureSnapshot snapshot,
            UserTaskChain userTaskChain,
            Task capturedRoot) {
        if (snapshot == null
                || !snapshot.isAtOrAboveThreshold()
                || stateMachine.state() != DepositAllInventoryPressureState.ARMED
                || userTaskChain == null
                || capturedRoot == null
                || userTaskChain.isRunningIdleTask()
                || userTaskChain.getCurrentTask() != capturedRoot
                || !conflictGuard.isStoreHomeRoot(capturedRoot)) {
            return false;
        }
        stateMachine.observe(snapshot);
        transitionArmedToWaiting(
                "existing_store_home_task",
                snapshot,
                capturedRoot
        );
        return true;
    }

    private void observeLowWater(DepositAllInventoryPressureSnapshot snapshot) {
        DepositAllInventoryPressureState previousState = stateMachine.state();
        DepositAllInventoryPressureSignal signal = stateMachine.observe(snapshot);
        if (signal == DepositAllInventoryPressureSignal.REARMED) {
            waitingTrustedRevision = trustedRepository.revision();
            DepositAllAutoDiagnostics.logTransition(
                    previousState,
                    stateMachine.state(),
                    "inventory_at_or_below_low_water",
                    snapshot,
                    null
            );
        }
    }

    private void startPlan(DepositAllInventoryPressureSnapshot pressure,
                           AutoDepositPlan plan) {
        DepositAllInventoryPressureSignal signal = stateMachine.observe(pressure);
        if (signal != DepositAllInventoryPressureSignal.THRESHOLD_REACHED) {
            return;
        }
        AutoDepositMaintenanceTask task = new AutoDepositMaintenanceTask(
                plan,
                trustedRepository,
                exactOpenContainerBinding
        );
        activeRunTrustedRevision = trustedRepository.revision();
        clearDeferredFingerprint();
        clearNoSafeContext();
        if (task.diagnosticAutomaticRunEnabled()) {
            activeDiagnosticMaintenanceTask = task;
            StoreDepositDiagnostics.beginAutomaticRun(
                    task,
                    plan.context().userTaskRoot(),
                    plan.context().epoch(),
                    null
            );
        }
        DepositAllAutoDiagnostics.logPolicyPlan(plan, task);
        AutoDepositPolicyDiagnostics.log(plan, pressure, "READY", "ready", task);
        startTask(pressure, plan.allTargets(), task);
    }

    private void startTask(DepositAllInventoryPressureSnapshot snapshot,
                           ItemTarget[] targets,
                           Task chainTask) {
        boolean runnerWasActive = runner.isActive();
        setTask(chainTask);
        DepositAllInventoryPressureState previousState = stateMachine.state();
        stateMachine.markRunStarted();
        DepositAllAutoDiagnostics.logTransition(
                previousState,
                stateMachine.state(),
                "automatic_task_started",
                snapshot,
                chainTask
        );
        DepositAllAutoDiagnostics.logTrigger(snapshot, targets.length, chainTask);
        if (!runnerWasActive) {
            runner.enable();
            DepositAllAutoDiagnostics.logRunnerActivated(snapshot, chainTask);
        }
    }

    @Override
    protected void onTaskFinish(AltoClef mod) {
        Task finishedTask = mainTask;
        setTask(null);
        transitionRunToWaiting("automatic_task_terminal", currentSnapshot(), finishedTask);
    }

    @Override
    public void onInterrupt(TaskChain other) {
        Task interruptingTask = other instanceof SingleTaskChain
                ? ((SingleTaskChain) other).getCurrentTask()
                : null;
        stopOwnedRun("automatic_chain_interrupted", interruptingTask);
    }

    @Override
    protected void onStop() {
        boolean wasRunning = stateMachine.state() == DepositAllInventoryPressureState.RUNNING;
        Task stoppedTask = mainTask;
        super.onStop();
        if (wasRunning) {
            transitionRunToWaiting("automatic_chain_stopped", currentSnapshot(), stoppedTask);
        }
    }

    @Override
    public boolean isActive() {
        return stateMachine.state() == DepositAllInventoryPressureState.RUNNING
                && mainTask != null;
    }

    @Override
    public String getName() {
        return "Automatic Deposit All";
    }

    private void latchNoSafe(String reason,
                             DepositAllInventoryPressureSnapshot snapshot,
                             Task userTaskRoot,
                             AutoDepositDecisionFingerprint fingerprint,
                             WorkingSetSnapshot retainedWorkingSet,
                             long retainedEpoch) {
        DepositAllInventoryPressureSignal signal = stateMachine.observe(snapshot);
        if (signal != DepositAllInventoryPressureSignal.THRESHOLD_REACHED) {
            return;
        }
        stateMachine.markNoSafeSurplus(fingerprint);
        lastNoSafeWorkingSet = retainedWorkingSet;
        lastNoSafeEpoch = retainedEpoch;
        clearDeferredFingerprint();
        DepositAllAutoDiagnostics.logNoSafeSurplus(reason, snapshot, userTaskRoot);
    }

    private void transitionArmedToWaiting(String reason,
                                          DepositAllInventoryPressureSnapshot snapshot,
                                          Task task) {
        DepositAllInventoryPressureState previousState = stateMachine.state();
        stateMachine.markThresholdSuppressed();
        waitingTrustedRevision = trustedRepository.revision();
        DepositAllAutoDiagnostics.logTransition(previousState, stateMachine.state(), reason, snapshot, task);
    }

    private void transitionRunToWaiting(String reason,
                                        DepositAllInventoryPressureSnapshot snapshot,
                                        Task task) {
        DepositAllInventoryPressureState previousState = stateMachine.state();
        stateMachine.markRunTerminated();
        waitingTrustedRevision = activeRunTrustedRevision >= 0L
                ? activeRunTrustedRevision
                : trustedRepository.revision();
        activeRunTrustedRevision = -1L;
        DepositAllAutoDiagnostics.logTransition(previousState, stateMachine.state(), reason, snapshot, task);
        if (activeDiagnosticMaintenanceTask != null) {
            StoreDepositDiagnostics.recordAutomaticPressureRunClosed(
                    activeDiagnosticMaintenanceTask,
                    reason,
                    stateMachine.state().name()
            );
            activeDiagnosticMaintenanceTask = null;
        }
    }

    private void stopOwnedRun(String reason, Task interruptingTask) {
        if (stateMachine.state() != DepositAllInventoryPressureState.RUNNING) {
            return;
        }
        Task ownedTask = mainTask;
        if (ownedTask != null && ownedTask.isActive()) {
            ownedTask.stop(interruptingTask);
        }
        mainTask = null;
        transitionRunToWaiting(reason, currentSnapshot(), ownedTask);
    }

    private DepositAllInventoryPressureSnapshot currentSnapshot() {
        return pressureSource.read(mod).orElse(null);
    }

    private Task currentUserTaskRoot() {
        UserTaskChain chain = mod.getUserTaskChain();
        return chain != null && chain.isActive() && !chain.isRunningIdleTask()
                ? chain.getCurrentTask()
                : null;
    }

    private void deferChanged(String reason,
                              DepositAllInventoryPressureSnapshot snapshot,
                              Task userTaskRoot) {
        if (!Objects.equals(lastDeferredReason, reason) || lastDeferredRoot != userTaskRoot) {
            DepositAllAutoDiagnostics.logDeferred(reason, snapshot, userTaskRoot);
            lastDeferredReason = reason;
            lastDeferredRoot = userTaskRoot;
        }
    }

    private void clearDeferredFingerprint() {
        lastDeferredReason = null;
        lastDeferredRoot = null;
    }

    private void clearNoSafeContext() {
        lastNoSafeWorkingSet = null;
        lastNoSafeEpoch = 0L;
    }
}
