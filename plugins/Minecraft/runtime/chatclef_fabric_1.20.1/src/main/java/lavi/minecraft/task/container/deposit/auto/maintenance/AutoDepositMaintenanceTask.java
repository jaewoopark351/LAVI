package lavi.minecraft.task.container.deposit.auto.maintenance;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.container.DepositAllTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureReader;
import lavi.minecraft.task.container.deposit.auto.maintenance.child.AutoDepositGeneralTaskFactory;
import lavi.minecraft.task.container.deposit.auto.maintenance.child.AutoDepositTrustedTaskFactory;
import lavi.minecraft.task.container.deposit.auto.maintenance.diagnostics.AutoDepositMaintenanceDiagnostics;
import lavi.minecraft.task.container.deposit.auto.maintenance.relief.AutoDepositFreeSlotVerdict;
import lavi.minecraft.task.container.deposit.auto.maintenance.relief.AutoDepositFreeSlotVerifier;
import lavi.minecraft.task.container.deposit.auto.pressure.AutoDepositInventoryPressureSource;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;
import lavi.minecraft.task.container.deposit.auto.recovery.AutoDepositDestinationManifest;
import lavi.minecraft.task.container.deposit.auto.recovery.AutoDepositDestinationManifestLifecycle;
import lavi.minecraft.task.container.deposit.auto.recovery.AutoDepositWorkingSetRecovery;
import lavi.minecraft.task.container.deposit.auto.recovery.RecoverReservedItemsTask;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.execution.AutoDepositTrustedStoreTask;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.deposit.auto.working.PlayerInventorySnapshotReader;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetSnapshot;

import java.util.List;
import java.util.Objects;

//20260827_kpopmodder: Execute one immutable auto-deposit plan and verify working-set and slot relief.
public final class AutoDepositMaintenanceTask extends Task {
    private final AutoDepositPlan plan;
    private final AutoDepositGeneralTaskFactory generalTaskFactory;
    private final AutoDepositTrustedTaskFactory trustedTaskFactory;
    private List<DepositAllTask> generalTasks;
    private AutoDepositTrustedStoreTask trustedTask;
    private final AutoDepositDestinationManifestLifecycle manifestLifecycle;
    private final AutoDepositWorkingSetRecovery workingSetRecovery;
    private final AutoDepositFreeSlotVerifier freeSlotVerifier;
    private final AutoDepositMaintenanceDiagnostics diagnostics;
    private AutoDepositMaintenancePhase phase;
    private AutoDepositMaintenanceOutcome outcome = AutoDepositMaintenanceOutcome.PENDING;
    private int generalTaskIndex;

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
        this.plan = Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(trustedRepository, "trustedRepository");
        Objects.requireNonNull(exactOpenContainerBinding, "exactOpenContainerBinding");
        Objects.requireNonNull(pressureSource, "pressureSource");
        if (!plan.hasTargets()) {
            throw new IllegalArgumentException("automatic deposit plan must contain at least one target");
        }
        AutoDepositDestinationManifest manifest = new AutoDepositDestinationManifest(
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
                plan.context().workingSet(),
                manifest,
                new PlayerInventorySnapshotReader()
        );
        //20260829_kpopmodder: Keep occupied-slot verification behind the same read-only pressure port as the chain.
        freeSlotVerifier = new AutoDepositFreeSlotVerifier(pressureSource);
        diagnostics = new AutoDepositMaintenanceDiagnostics(plan);
        phase = plan.trustedTargets().length > 0
                ? AutoDepositMaintenancePhase.DEPOSIT_TRUSTED
                : AutoDepositMaintenancePhase.DEPOSIT_GENERAL;
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
        AltoClef mod = AltoClef.getInstance();
        if (!plan.context().matches(mod)) {
            manifestLifecycle.stop();
            outcome = AutoDepositMaintenanceOutcome.CANCELLED;
            transition(AutoDepositMaintenancePhase.CANCELLED, "automatic_context_changed", 0);
            return null;
        }

        switch (phase) {
            case DEPOSIT_TRUSTED -> {
                AutoDepositTrustedStoreTask trustedStore = trustedTask();
                if (trustedStore == null) {
                    if (!hasGeneralSteps()) {
                        finishDepositSteps("trusted_steps_complete");
                    } else {
                        transition(AutoDepositMaintenancePhase.DEPOSIT_GENERAL,
                                "trusted_steps_complete", 0);
                    }
                    return null;
                }
                if (trustedStore.isFinished() || trustedStore.stopped()) {
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
                boolean generalTaskStopped = !generalTaskFinished && generalTask.stopped();
                if (generalTaskFinished || generalTaskStopped) {
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
                if (!workingSetRecovery.available()) {
                    transition(AutoDepositMaintenancePhase.VERIFY_FREE_SLOTS,
                            "idle_context_has_no_working_set", 0);
                    return null;
                }
                int deficitTypes = workingSetRecovery.deficitTypeCount(mod);
                if (deficitTypes == 0) {
                    transition(AutoDepositMaintenancePhase.VERIFY_FREE_SLOTS,
                            "working_set_preserved", 0);
                    return null;
                }
                RecoverReservedItemsTask recoveryTask = workingSetRecovery.begin();
                transition(AutoDepositMaintenancePhase.RECOVER,
                        "working_set_deficit_detected", deficitTypes);
                return recoveryTask;
            }
            case RECOVER -> {
                RecoverReservedItemsTask recoveryTask = workingSetRecovery.task();
                if (recoveryTask == null || recoveryTask.isFinished()) {
                    int remaining = workingSetRecovery.deficitTypeCount(mod);
                    transition(AutoDepositMaintenancePhase.VERIFY_FREE_SLOTS,
                            "recovery_terminal", remaining);
                    return null;
                }
                return recoveryTask;
            }
            case VERIFY_FREE_SLOTS -> {
                AutoDepositFreeSlotVerdict verdict = freeSlotVerifier.verify(
                        mod,
                        plan.startingOccupiedSlots(),
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

    public boolean diagnosticAutomaticRunEnabled() {
        return diagnostics.automaticRunEnabled();
    }

    public WorkingSetSnapshot snapshot() {
        return workingSetRecovery.snapshot();
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

    public AutoDepositDestinationManifest manifest() {
        return manifestLifecycle.manifest();
    }
}
