package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.UserTaskChain;
import adris.altoclef.chains.SingleTaskChain;
import adris.altoclef.tasks.container.DepositAllTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.container.store.deposit.StoreDepositDiagnostics;
import lavi.minecraft.task.container.deposit.DepositAllInventoryTargetSelector;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceTask;
import lavi.minecraft.task.container.deposit.auto.working.ActiveTaskWorkingSetResolver;
import lavi.minecraft.task.container.deposit.auto.working.AutoDepositSurplusTargetSelector;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetResolution;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetSnapshot;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

//20260826_kpopmodder: Added a dedicated one-shot automatic deposit_all chain at four-fifths inventory pressure.
public final class DepositAllInventoryPressureChain extends SingleTaskChain {
    public static final float PRIORITY = 51.0f;

    private final AltoClef mod;
    private final TaskRunner runner;
    private final DepositAllInventoryPressureReader pressureReader;
    private final DepositAllInventoryPressureStateMachine stateMachine;
    private final DepositAllInventoryTargetSelector targetSelector;
    private final DepositAllAutoConflictGuard conflictGuard;
    private final ActiveTaskWorkingSetResolver workingSetResolver;
    private final AutoDepositSurplusTargetSelector surplusTargetSelector;
    private String lastDeferredReason;
    private Task lastDeferredRoot;

    public DepositAllInventoryPressureChain(TaskRunner runner) {
        super(Objects.requireNonNull(runner, "runner"));
        this.runner = runner;
        mod = Objects.requireNonNull(runner.getMod(), "mod");
        pressureReader = new DepositAllInventoryPressureReader();
        stateMachine = new DepositAllInventoryPressureStateMachine();
        targetSelector = new DepositAllInventoryTargetSelector();
        conflictGuard = new DepositAllAutoConflictGuard();
        workingSetResolver = new ActiveTaskWorkingSetResolver();
        surplusTargetSelector = new AutoDepositSurplusTargetSelector();
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
                transitionRunToWaiting("running_task_missing", currentSnapshot(), null);
            }
            return;
        }

        Optional<DepositAllInventoryPressureSnapshot> snapshotOptional = pressureReader.read(mod);
        if (snapshotOptional.isEmpty()) {
            return;
        }

        DepositAllInventoryPressureSnapshot snapshot = snapshotOptional.get();
        if (!snapshot.isAtOrAboveThreshold()) {
            clearDeferredFingerprint();
            observeBelowThreshold(snapshot);
            return;
        }
        if (stateMachine.state() != DepositAllInventoryPressureState.ARMED) {
            return;
        }

        UserTaskChain userTaskChain = mod.getUserTaskChain();
        boolean activeUserTask = userTaskChain != null
                && userTaskChain.isActive()
                && !userTaskChain.isRunningIdleTask();
        Task userTaskRoot = activeUserTask ? userTaskChain.getCurrentTask() : null;

        if (activeUserTask && runner.getCurrentTaskChain() != userTaskChain) {
            deferChanged("user_task_chain_not_selected", snapshot, userTaskRoot);
            return;
        }

        if (conflictGuard.hasExistingDepositTask(mod)) {
            DepositAllInventoryPressureSignal signal = stateMachine.observe(snapshot);
            if (signal == DepositAllInventoryPressureSignal.THRESHOLD_REACHED) {
                transitionArmedToWaiting("existing_deposit_task", snapshot, userTaskRoot);
            }
            return;
        }

        if (activeUserTask) {
            startWorkingSetMaintenance(snapshot, userTaskRoot);
            return;
        }

        startFullDeposit(snapshot);
    }

    private void observeBelowThreshold(DepositAllInventoryPressureSnapshot snapshot) {
        DepositAllInventoryPressureState previousState = stateMachine.state();
        DepositAllInventoryPressureSignal signal = stateMachine.observe(snapshot);
        if (signal == DepositAllInventoryPressureSignal.REARMED) {
            DepositAllAutoDiagnostics.logTransition(
                    previousState,
                    stateMachine.state(),
                    "inventory_below_four_fifths",
                    snapshot,
                    null
            );
        }
    }

    private void startWorkingSetMaintenance(DepositAllInventoryPressureSnapshot pressure,
                                            Task userTaskRoot) {
        WorkingSetResolution resolution = workingSetResolver.resolve(mod);
        if (resolution.status() != WorkingSetResolution.Status.SUPPORTED) {
            deferChanged(resolution.reason(), pressure, userTaskRoot);
            return;
        }
        WorkingSetSnapshot workingSet = resolution.snapshot();
        ItemTarget[] targets = surplusTargetSelector.select(workingSet);
        if (targets.length == 0) {
            deferChanged("no_safe_surplus", pressure, userTaskRoot);
            return;
        }
        DepositAllInventoryPressureSignal signal = stateMachine.observe(pressure);
        if (signal != DepositAllInventoryPressureSignal.THRESHOLD_REACHED) {
            return;
        }

        AutoDepositMaintenanceTask task = new AutoDepositMaintenanceTask(workingSet, targets);
        clearDeferredFingerprint();
        DepositAllAutoDiagnostics.logWorkingSetPlan(
                workingSet,
                targets.length,
                Arrays.stream(targets).mapToInt(ItemTarget::getTargetCount).sum(),
                task
        );
        startTask(pressure, targets, task, task.depositTask());
    }

    private void startFullDeposit(DepositAllInventoryPressureSnapshot snapshot) {
        DepositAllInventoryPressureSignal signal = stateMachine.observe(snapshot);
        if (signal != DepositAllInventoryPressureSignal.THRESHOLD_REACHED) {
            return;
        }

        ItemTarget[] targets = targetSelector.select(mod);
        if (targets.length == 0) {
            transitionArmedToWaiting("no_depositable_items", snapshot, null);
            return;
        }

        DepositAllTask task = new DepositAllTask(false, targets);
        startTask(snapshot, targets, task, task);
    }

    private void startTask(DepositAllInventoryPressureSnapshot snapshot,
                           ItemTarget[] targets,
                           Task chainTask,
                           Task diagnosticDepositTask) {
        boolean runnerWasActive = runner.isActive();
        StoreDepositDiagnostics.registerBareDepositInvocation(
                mod,
                false,
                targets,
                diagnosticDepositTask,
                "AUTO_DEPOSIT_ALL_CHAIN"
        );
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

    private void transitionArmedToWaiting(String reason,
                                          DepositAllInventoryPressureSnapshot snapshot,
                                          Task task) {
        DepositAllInventoryPressureState previousState = stateMachine.state();
        stateMachine.markThresholdSuppressed();
        DepositAllAutoDiagnostics.logTransition(previousState, stateMachine.state(), reason, snapshot, task);
    }

    private void transitionRunToWaiting(String reason,
                                        DepositAllInventoryPressureSnapshot snapshot,
                                        Task task) {
        DepositAllInventoryPressureState previousState = stateMachine.state();
        stateMachine.markRunTerminated();
        DepositAllAutoDiagnostics.logTransition(previousState, stateMachine.state(), reason, snapshot, task);
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
        return pressureReader.read(mod).orElse(null);
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
}
