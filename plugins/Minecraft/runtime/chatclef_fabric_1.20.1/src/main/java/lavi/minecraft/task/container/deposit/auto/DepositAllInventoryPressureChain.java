package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.SingleTaskChain;
import adris.altoclef.tasks.container.DepositAllTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.container.store.deposit.StoreDepositDiagnostics;
import lavi.minecraft.task.container.deposit.DepositAllInventoryTargetSelector;

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

    public DepositAllInventoryPressureChain(TaskRunner runner) {
        super(Objects.requireNonNull(runner, "runner"));
        this.runner = runner;
        mod = Objects.requireNonNull(runner.getMod(), "mod");
        pressureReader = new DepositAllInventoryPressureReader();
        stateMachine = new DepositAllInventoryPressureStateMachine();
        targetSelector = new DepositAllInventoryTargetSelector();
        conflictGuard = new DepositAllAutoConflictGuard();
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
            return;
        }
        if (signal != DepositAllInventoryPressureSignal.THRESHOLD_REACHED) {
            return;
        }

        if (conflictGuard.hasExistingDepositTask(mod)) {
            transitionArmedToWaiting("existing_deposit_task", snapshot, null);
            return;
        }

        ItemTarget[] targets = targetSelector.select(mod);
        if (targets.length == 0) {
            transitionArmedToWaiting("no_depositable_items", snapshot, null);
            return;
        }

        DepositAllTask task = new DepositAllTask(false, targets);
        boolean runnerWasActive = runner.isActive();
        StoreDepositDiagnostics.registerBareDepositInvocation(
                mod,
                false,
                targets,
                task,
                "AUTO_DEPOSIT_ALL_CHAIN"
        );
        setTask(task);
        previousState = stateMachine.state();
        stateMachine.markRunStarted();
        DepositAllAutoDiagnostics.logTransition(
                previousState,
                stateMachine.state(),
                "automatic_task_started",
                snapshot,
                task
        );
        DepositAllAutoDiagnostics.logTrigger(snapshot, targets.length, task);
        if (!runnerWasActive) {
            runner.enable();
            DepositAllAutoDiagnostics.logRunnerActivated(snapshot, task);
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
}
