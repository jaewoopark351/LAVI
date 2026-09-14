package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.SingleTaskChain;
import adris.altoclef.chains.UserTaskChain;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.container.store.deposit.StoreDepositDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.pressure.AutoDepositPressureObserver;
import lavi.minecraft.task.container.deposit.auto.composition.DepositAllInventoryPressureChainPreparation;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceTask;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlanningResult;
import lavi.minecraft.task.container.deposit.auto.policy.diagnostics.AutoDepositPolicyDiagnostics;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPolicyEngine;
import lavi.minecraft.task.container.deposit.auto.pressure.AutoDepositInventoryPressureSource;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.deposit.auto.working.ActiveTaskWorkingSetResolver;

import lavi.minecraft.task.container.deposit.auto.admission.AutoDepositPlanAdmission;
import lavi.minecraft.task.container.deposit.auto.admission.AutoDepositAdmissionResult;
import lavi.minecraft.task.container.deposit.auto.admission.conditions.AutoDepositConditionReader;
import lavi.minecraft.task.container.deposit.auto.admission.conditions.AutoDepositConditions;
import lavi.minecraft.task.container.deposit.auto.lifecycle.AutoDepositExecutionControl;
import lavi.minecraft.task.container.deposit.auto.lifecycle.AutoDepositSafetyAdmission;
import lavi.minecraft.task.container.deposit.auto.lifecycle.AutoDepositRunLedger;
import lavi.minecraft.task.container.deposit.auto.lifecycle.cleanup.AutoDepositSurvivalCleanup;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunReason;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunResult;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositWorkingSetStatus;
import lavi.minecraft.task.container.deposit.auto.maintenance.plan.AutoDepositVerificationPlan;
import lavi.minecraft.task.container.deposit.auto.maintenance.relief.AutoDepositFreeSlotVerifier;
import lavi.minecraft.task.container.deposit.auto.rearm.AutoDepositRearmPolicy;
import lavi.minecraft.task.container.deposit.auto.rearm.AutoDepositRearmDecision;
import lavi.minecraft.task.container.deposit.auto.rearm.diagnostics.AutoDepositRearmDiagnostics;
import java.util.Objects;

//20260827_kpopmodder: Apply one automatic-only immutable policy plan at 33/36 inventory pressure.
public final class DepositAllInventoryPressureChain extends SingleTaskChain {
    public static final float PRIORITY = 51.0f;

    private final AltoClef mod;
    private final TaskRunner runner;
    private final AutoDepositInventoryPressureSource pressureSource;
    private final AutoDepositFreeSlotVerifier pressureObservation;
    private final DepositAllInventoryPressureStateMachine stateMachine;
    private final DepositAllAutoConflictGuard conflictGuard;
    private final ActiveTaskWorkingSetResolver workingSetResolver;
    private final AutoDepositPolicyEngine policyEngine;
    private final AutoDepositTrustedDestinationRepository trustedRepository;
    private final AutoDepositExactOpenContainerBinding exactOpenContainerBinding;
    //20260914_kpopmodder: Execution rights, root correlation and retry policy have separate owners.
    private final AutoDepositRearmPolicy rearm = new AutoDepositRearmPolicy();
    private final AutoDepositRunLedger runs = new AutoDepositRunLedger(rearm);
    private final AutoDepositExecutionControl executionControl = new AutoDepositExecutionControl();
    private final AutoDepositConditionReader conditions;
    private final AutoDepositPlanAdmission admission;
    private boolean pendingAdmission;
    private int nextEvaluation;
    private Object worldIdentity;
    private Task activeDiagnosticMaintenanceTask;
    private String lastDeferredReason;
    private Task lastDeferredRoot;
    //20260913_kpopmodder: Keep passive pressure decisions separate from orchestration and policy ownership.
    private final AutoDepositPressureObserver pressureDiagnostics = new AutoDepositPressureObserver();

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
        pressureObservation = new AutoDepositFreeSlotVerifier(pressureSource);
        stateMachine = preparation.stateMachine();
        conflictGuard = preparation.conflictGuard();
        workingSetResolver = preparation.workingSetResolver();
        policyEngine = preparation.policyEngine();
        trustedRepository = preparation.trustedRepository();
        exactOpenContainerBinding = preparation.exactOpenContainerBinding();
        admission = new AutoDepositPlanAdmission(mod, runner, conflictGuard, workingSetResolver, policyEngine);
        conditions = new AutoDepositConditionReader(trustedRepository, policyEngine.trustedMaximumDistance());
    }

    static DepositAllInventoryPressureChain commit(
            DepositAllInventoryPressureChainPreparation preparation) {
        return new DepositAllInventoryPressureChain(
                Objects.requireNonNull(preparation, "preparation")
        );
    }

    @Override
    public float getPriority() {
        return isActive() && !AutoDepositSafetyAdmission.claimed(mod) ? PRIORITY : Float.NEGATIVE_INFINITY;
    }

    public void onEndClientTick() {
        pressureDiagnostics.begin(stateMachine.state(), pendingAdmission, mainTask);
        if (!AltoClef.inGame()) {
            stopOwnedRun(AutoDepositRunReason.CONTEXT_CHANGED, null);
            runs.resetContext();
            worldIdentity = null;
            decide("left_game");
            return;
        }
        if (!mod.getAiBridge().getEnabled()) {
            stopOwnedRun(AutoDepositRunReason.AUTOMATION_DISABLED, null);
            decide("chatclef_disabled");
            return;
        }
        if (!executionControl.permitsExecution(runner.isActive())) {
            pendingAdmission = false;
            decide("explicit_stop");
            return;
        }
        if (worldIdentity != mod.getWorld()) {
            stopOwnedRun(AutoDepositRunReason.CONTEXT_CHANGED, null);
            runs.resetContext();
            worldIdentity = mod.getWorld();
            nextEvaluation = 0;
        }
        if (mainTask != null || pendingAdmission) {
            decide(mainTask == null ? "awaiting_native_selection" : "already_running");
            return;
        }
        DepositAllInventoryPressureSnapshot pressure = currentSnapshot();
        pressureDiagnostics.pressure(pressure);
        if (pressure == null) { decide("pressure_read_unavailable"); return; }
        if (!pressure.isAtOrAboveThreshold() && !rearm.hasPendingUnit()) {
            decide("below_threshold");
            return;
        }
        String refusal = admission.controlDeferral(this);
        if (refusal != null) { defer(refusal, pressure); return; }
        // Bounded read-only evaluation cadence; neither ticks nor plans recharge execution limits.
        if (nextEvaluation > 0) { nextEvaluation--; decide("evaluation_interval"); return; }
        nextEvaluation = 19;
        evaluate(pressure, false);
    }

    @Override
    protected void onTick() {
        if (pendingAdmission) {
            pendingAdmission = false;
            if (!executionControl.permitsExecution(runner.isActive())) return;
            // Priorities have now been evaluated exactly once. Rebuild from current state before any storage action.
            evaluate(currentSnapshot(), true);
        }
        if (mainTask != null) super.onTick();
    }

    private void evaluate(DepositAllInventoryPressureSnapshot pressure, boolean selected) {
        if (pressure == null) { decide("pressure_read_unavailable"); return; }
        if (runs.reconcileUserRoot(admission.currentRoot())) {
            AutoDepositRearmDiagnostics.decision("FORMER_USER_ROOT_REPLACED", rearm, pressure, null);
        }
        AutoDepositAdmissionResult evaluated = admission.plan(this, pressure);
        if (evaluated.deferred()) { defer(evaluated.reason(), pressure); return; }
        AutoDepositPlanningResult planning = evaluated.planning();
        pressureDiagnostics.planning(planning);
        AutoDepositPlan plan = planning.plan().orElse(null);
        if (plan == null) { defer(planning.reason(), pressure); return; }
        AutoDepositConditions observed = conditions.read(mod, plan, rearm.episodeSequence());
        if (!observed.available()) { defer("destination_observation_unavailable", pressure); return; }
        String condition = observed.conditionKey(rearm.lastReason());
        AutoDepositRearmDecision decision = rearm.observe(pressure, condition, observed.scope());
        AutoDepositRearmDiagnostics.decision(decision.name(), rearm, pressure, null);
        if (!decision.canEvaluate()) {
            stateMachine.markPolicyWaiting(false);
            decide(decision.name());
            return;
        }
        boolean verificationOnly = runs.mayResumeVerification();
        boolean continuation = rearm.hasPendingUnit()
                && planning.status() == AutoDepositPlanningResult.Status.NO_SAFE_SURPLUS;
        if (planning.status() != AutoDepositPlanningResult.Status.READY && !continuation) {
            rearm.markPlanBlocked(planning.reason(), observed.scope(), observed.conditionKey(planning.reason()));
            stateMachine.markPolicyWaiting(true);
            AutoDepositRearmDiagnostics.decision("PLAN_BLOCKED:" + planning.reason(), rearm, pressure, null);
            decide("policy_not_ready");
            return;
        }
        if (!selected) {
            pendingAdmission = true;
            // Native selection cannot run while disabled, but a latched explicit STOP never reaches this branch.
            if (!runner.isActive()) runner.enable();
            decide("awaiting_native_selection");
            return;
        }
        AutoDepositWorkingSetStatus retained = runs.verifyRetainedWorkingSet();
        if (retained == AutoDepositWorkingSetStatus.DEFICIT && runs.recoverySource() != null) {
            startRecovery(pressure, plan, condition, observed);
            return;
        }
        if (!retained.satisfiedOrNotApplicable()) {
            rearm.markPlanBlocked("resume_working_set_" + retained.name(), observed.scope(),
                    observed.conditionKey("working_set_deficit"));
            stateMachine.markPolicyWaiting(true);
            AutoDepositRearmDiagnostics.decision("RESUME_WORKING_SET_" + retained.name(), rearm, pressure, null);
            return;
        }
        if (!verificationOnly && planning.status() != AutoDepositPlanningResult.Status.READY) {
            rearm.markPlanBlocked(planning.reason(), observed.scope(), observed.conditionKey(planning.reason()));
            stateMachine.markPolicyWaiting(true);
            decide("resumed_plan_not_ready");
            return;
        }
        startPlan(pressure, verificationOnly ? AutoDepositVerificationPlan.from(plan) : plan,
                condition, observed, verificationOnly);
    }

    private void startRecovery(DepositAllInventoryPressureSnapshot pressure, AutoDepositPlan plan,
                               String condition, AutoDepositConditions observed) {
        AutoDepositPlan verification = AutoDepositVerificationPlan.from(plan);
        AutoDepositMaintenanceTask task = AutoDepositMaintenanceTask.resumeRecovery(verification,
                runs.recoverySource(), trustedRepository, exactOpenContainerBinding, pressureSource, runs::executionTick);
        bindAndStart(pressure, verification, condition, observed, task, "resume_reserved_item_recovery");
    }

    private void startPlan(DepositAllInventoryPressureSnapshot pressure, AutoDepositPlan plan,
                           String condition, AutoDepositConditions observed, boolean verificationOnly) {
        AutoDepositMaintenanceTask task = new AutoDepositMaintenanceTask(plan, trustedRepository,
                exactOpenContainerBinding, pressureSource, runs::executionTick, verificationOnly);
        bindAndStart(pressure, plan, condition, observed, task, verificationOnly ? "resume_verification" : "ready");
    }

    private void bindAndStart(DepositAllInventoryPressureSnapshot pressure, AutoDepositPlan plan,
                              String condition, AutoDepositConditions observed,
                              AutoDepositMaintenanceTask task, String reason) {
        runs.begin(task, pressure, condition, observed.scope(), observed);
        stateMachine.prepareAdmittedRun();
        if (task.diagnosticAutomaticRunEnabled()) {
            activeDiagnosticMaintenanceTask = task;
            StoreDepositDiagnostics.beginAutomaticRun(task, plan.context().userTaskRoot(), plan.context().epoch(), null);
        }
        DepositAllAutoDiagnostics.logPolicyPlan(plan, task);
        AutoDepositPolicyDiagnostics.log(plan, pressure, "READY", reason, task);
        startTask(pressure, plan.allTargets(), task);
        AutoDepositRearmDiagnostics.decision("ROOT_STARTED", rearm, pressure, task);
        lastDeferredReason = null;
        lastDeferredRoot = null;
    }

    private void startTask(DepositAllInventoryPressureSnapshot snapshot, ItemTarget[] targets, Task chainTask) {
        boolean runnerWasActive = runner.isActive();
        setTask(chainTask);
        DepositAllInventoryPressureState previous = stateMachine.state();
        stateMachine.markRunStarted();
        DepositAllAutoDiagnostics.logTransition(previous, stateMachine.state(), "automatic_task_started", snapshot, chainTask);
        DepositAllAutoDiagnostics.logTrigger(snapshot, targets.length, chainTask);
        if (!runnerWasActive) {
            runner.enable();
            DepositAllAutoDiagnostics.logRunnerActivated(snapshot, chainTask);
        }
    }

    @Override
    protected void onTaskFinish(AltoClef mod) {
        stopOwnedRun(AutoDepositRunReason.UNEXPECTED_STOP, null);
    }

    @Override
    public void onInterrupt(TaskChain other) {
        Task interrupting = other instanceof SingleTaskChain ? ((SingleTaskChain) other).getCurrentTask() : null;
        stopOwnedRun(AutoDepositRunReason.SAFETY_INTERRUPTED, interrupting);
    }

    @Override
    protected void onStop() {
        executionControl.stop();
        stopOwnedRun(AutoDepositRunReason.STOPPED, null);
        runs.cancelPendingUnit("explicit_stop");
    }

    @Override
    public boolean isActive() {
        return pendingAdmission || stateMachine.state() == DepositAllInventoryPressureState.RUNNING && mainTask != null;
    }

    @Override
    public String getName() { return "Automatic Deposit All"; }

    private void stopOwnedRun(AutoDepositRunReason reason, Task interruptingTask) {
        pendingAdmission = false;
        Task owned = mainTask;
        if (owned == null) return;
        AutoDepositMaintenanceTask maintenance = owned instanceof AutoDepositMaintenanceTask
                ? (AutoDepositMaintenanceTask) owned : null;
        if (maintenance != null) maintenance.terminate(reason);
        boolean clean = false;
        try {
            AutoDepositSurvivalCleanup.run(mod, () -> {
                if (owned.isActive()) owned.stop(interruptingTask);
            });
            clean = true;
        } finally {
            mainTask = null;
            // Maintenance owns the safe post-cleanup verification and preserves its immutable candidate.
            DepositAllInventoryPressureSnapshot end = maintenance == null ? currentSnapshot() : null;
            if (maintenance != null) {
                AutoDepositRunResult result = maintenance.finalizeAfterCleanup(clean).orElseThrow();
                end = result.endingPressure().orElse(null);
                String workingFailureKey = result.reason() == AutoDepositRunReason.WORKING_SET_DEFICIT
                        ? conditions.captureWorkingSetFailure(maintenance.snapshot()) : null;
                AutoDepositConditions terminalConditions = conditions.read(mod, maintenance.plan(), rearm.episodeSequence());
                String terminalKey = result.reason() == AutoDepositRunReason.WORKING_SET_DEFICIT ? workingFailureKey
                        : terminalConditions.available()
                        ? terminalConditions.conditionKey(result.reason().name() + ":" + result.detail()) : null;
                boolean userRootReplaced = maintenance.plan().context().worldIdentity() == mod.getWorld()
                        && maintenance.plan().context().userTaskRoot() != admission.currentRoot();
                runs.settle(maintenance, result, terminalKey, userRootReplaced);
                AutoDepositRearmDiagnostics.result(result, rearm, maintenance);
            }
            DepositAllInventoryPressureState previous = stateMachine.state();
            if (previous == DepositAllInventoryPressureState.RUNNING) stateMachine.markRunTerminated();
            pressureDiagnostics.transition(previous, stateMachine.state(), reason.name(), owned, trustedRepository.revision());
            DepositAllAutoDiagnostics.logTransition(previous, stateMachine.state(), reason.name(), end, owned);
            if (activeDiagnosticMaintenanceTask != null) {
                StoreDepositDiagnostics.recordAutomaticPressureRunClosed(activeDiagnosticMaintenanceTask,
                        rearm.lastReason(), stateMachine.state().name());
                activeDiagnosticMaintenanceTask = null;
            }
            nextEvaluation = 0;
        }
    }

    // Preserve the exact manual StoreHome classifier used by existing callers and regression tests.
    boolean suppressExistingStoreHome(DepositAllInventoryPressureSnapshot snapshot,
                                     UserTaskChain userTaskChain, Task capturedRoot) {
        if (snapshot == null || !snapshot.isAtOrAboveThreshold()
                || stateMachine.state() != DepositAllInventoryPressureState.ARMED
                || userTaskChain == null || capturedRoot == null || userTaskChain.isRunningIdleTask()
                || userTaskChain.getCurrentTask() != capturedRoot || !conflictGuard.isStoreHomeRoot(capturedRoot)) return false;
        stateMachine.observe(snapshot);
        stateMachine.markThresholdSuppressed();
        DepositAllAutoDiagnostics.logTransition(DepositAllInventoryPressureState.ARMED, stateMachine.state(),
                "existing_store_home_task", snapshot, capturedRoot);
        return true;
    }

    private DepositAllInventoryPressureSnapshot currentSnapshot() { return pressureObservation.observe(mod).orElse(null); }
    private void decide(String reason) { pressureDiagnostics.decision(reason, stateMachine.state(), pendingAdmission); }
    private void defer(String reason, DepositAllInventoryPressureSnapshot pressure) {
        if ("manual_storage".equals(reason) && mainTask == null) stateMachine.markPolicyWaiting(false);
        Task root = admission.currentRoot();
        if (!Objects.equals(lastDeferredReason, reason) || lastDeferredRoot != root) {
            DepositAllAutoDiagnostics.logDeferred(reason, pressure, root);
            lastDeferredReason = reason;
            lastDeferredRoot = root;
        }
        decide(reason);
    }
}
