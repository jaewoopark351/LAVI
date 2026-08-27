package lavi.minecraft.task.container.deposit.auto.maintenance;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.container.DepositAllTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.task.container.deposit.auto.DepositAllAutoDiagnostics;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureReader;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;
import lavi.minecraft.task.container.deposit.auto.recovery.AutoDepositDestinationManifest;
import lavi.minecraft.task.container.deposit.auto.recovery.AutoDepositDestinationManifestTracker;
import lavi.minecraft.task.container.deposit.auto.recovery.RecoverReservedItemsTask;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.execution.AutoDepositTrustedStoreTask;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.deposit.auto.working.PlayerInventorySnapshotReader;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetSnapshot;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

//20260827_kpopmodder: Execute one immutable auto-deposit plan and verify working-set and slot relief.
public final class AutoDepositMaintenanceTask extends Task {
    private final AutoDepositPlan plan;
    private final WorkingSetSnapshot workingSet;
    private final AutoDepositTrustedDestinationRepository trustedRepository;
    private final AutoDepositExactOpenContainerBinding exactOpenContainerBinding;
    private List<DepositAllTask> generalTasks;
    private AutoDepositTrustedStoreTask trustedTask;
    private final AutoDepositDestinationManifest manifest;
    private final PlayerInventorySnapshotReader inventoryReader = new PlayerInventorySnapshotReader();
    private final DepositAllInventoryPressureReader pressureReader = new DepositAllInventoryPressureReader();

    private AutoDepositDestinationManifestTracker manifestTracker;
    private RecoverReservedItemsTask recoveryTask;
    private AutoDepositMaintenancePhase phase;
    private AutoDepositMaintenanceOutcome outcome = AutoDepositMaintenanceOutcome.PENDING;
    private int generalTaskIndex;

    public AutoDepositMaintenanceTask(
            AutoDepositPlan plan,
            AutoDepositTrustedDestinationRepository trustedRepository) {
        this(
                plan,
                trustedRepository,
                AutoDepositExactOpenContainerBinding.UNAVAILABLE
        );
    }

    public AutoDepositMaintenanceTask(
            AutoDepositPlan plan,
            AutoDepositTrustedDestinationRepository trustedRepository,
            AutoDepositExactOpenContainerBinding exactOpenContainerBinding) {
        this.plan = Objects.requireNonNull(plan, "plan");
        this.trustedRepository = Objects.requireNonNull(trustedRepository, "trustedRepository");
        this.exactOpenContainerBinding = Objects.requireNonNull(
                exactOpenContainerBinding,
                "exactOpenContainerBinding"
        );
        if (!plan.hasTargets()) {
            throw new IllegalArgumentException("automatic deposit plan must contain at least one target");
        }
        workingSet = plan.context().workingSet();
        phase = plan.trustedTargets().length > 0
                ? AutoDepositMaintenancePhase.DEPOSIT_TRUSTED
                : AutoDepositMaintenancePhase.DEPOSIT_GENERAL;
        manifest = new AutoDepositDestinationManifest(
                plan.context().worldIdentity(), plan.context().dimension(), plan.context().epoch()
        );
    }

    @Override
    protected void onStart() {
        if (phase == AutoDepositMaintenancePhase.DEPOSIT_TRUSTED
                || phase == AutoDepositMaintenancePhase.DEPOSIT_GENERAL) {
            startManifestTracking();
        }
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();
        if (!plan.context().matches(mod)) {
            stopManifestTracking();
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
                return trustedStore;
            }
            case DEPOSIT_GENERAL -> {
                DepositAllTask generalTask = currentGeneralTask();
                if (generalTask == null) {
                    finishDepositSteps("general_steps_complete");
                    return null;
                }
                if (generalTask.isFinished() || generalTask.stopped()) {
                    generalTaskIndex++;
                    if (currentGeneralTask() != null) {
                        transition(AutoDepositMaintenancePhase.DEPOSIT_GENERAL,
                                "general_step_terminal", 0);
                    } else {
                        finishDepositSteps("general_steps_complete");
                    }
                    return null;
                }
                return generalTask;
            }
            case VERIFY_WORKING_SET -> {
                if (workingSet == null) {
                    transition(AutoDepositMaintenancePhase.VERIFY_FREE_SLOTS,
                            "idle_context_has_no_working_set", 0);
                    return null;
                }
                Map<Item, Integer> deficits = workingSet.deficits(inventoryReader.readMainAndCursor(mod));
                if (deficits.isEmpty()) {
                    transition(AutoDepositMaintenancePhase.VERIFY_FREE_SLOTS,
                            "working_set_preserved", 0);
                    return null;
                }
                recoveryTask = new RecoverReservedItemsTask(workingSet, manifest);
                transition(AutoDepositMaintenancePhase.RECOVER,
                        "working_set_deficit_detected", deficits.size());
                return recoveryTask;
            }
            case RECOVER -> {
                if (recoveryTask == null || recoveryTask.isFinished()) {
                    int remaining = workingSet == null ? 0
                            : workingSet.deficits(inventoryReader.readMainAndCursor(mod)).size();
                    transition(AutoDepositMaintenancePhase.VERIFY_FREE_SLOTS,
                            "recovery_terminal", remaining);
                    return null;
                }
                return recoveryTask;
            }
            case VERIFY_FREE_SLOTS -> {
                verifySlotRelief(mod);
                transition(AutoDepositMaintenancePhase.DONE, "free_slot_postcondition_observed", 0);
                return null;
            }
            case DONE, CANCELLED -> {
                return null;
            }
        }
        return null;
    }

    private void verifySlotRelief(AltoClef mod) {
        DepositAllInventoryPressureSnapshot current = pressureReader.read(mod).orElse(null);
        int endingOccupied = current == null ? plan.startingOccupiedSlots() : current.occupiedSlots();
        int freedSlots = Math.max(0, plan.startingOccupiedSlots() - endingOccupied);
        if (current != null
                && (current.isAtOrBelowLowWater() || freedSlots >= plan.targetReliefSlots())) {
            outcome = AutoDepositMaintenanceOutcome.FULL_RELIEF;
        } else if (freedSlots > 0) {
            outcome = AutoDepositMaintenanceOutcome.PARTIAL_RELIEF;
        } else {
            outcome = AutoDepositMaintenanceOutcome.NO_SLOT_RELIEF;
        }
        DepositAllAutoDiagnostics.logFreeSlotOutcome(
                plan.context().epoch(),
                plan.startingOccupiedSlots(),
                endingOccupied,
                plan.expectedFreedSlots(),
                outcome,
                this
        );
    }

    private void startManifestTracking() {
        if (manifestTracker == null) {
            manifestTracker = new AutoDepositDestinationManifestTracker(AltoClef.getInstance(), manifest);
        }
        manifestTracker.start();
    }

    private void stopManifestTracking() {
        if (manifestTracker != null) {
            manifestTracker.stop();
        }
    }

    private void transition(AutoDepositMaintenancePhase next, String reason, int deficitTypes) {
        AutoDepositMaintenancePhase previous = phase;
        phase = next;
        DepositAllAutoDiagnostics.logMaintenanceTransition(
                plan.context().epoch(), previous, next, reason, deficitTypes, this
        );
    }

    private void finishDepositSteps(String reason) {
        stopManifestTracking();
        transition(AutoDepositMaintenancePhase.VERIFY_WORKING_SET, reason, 0);
    }

    private DepositAllTask currentGeneralTask() {
        List<DepositAllTask> tasks = generalTasks();
        return generalTaskIndex < tasks.size() ? tasks.get(generalTaskIndex) : null;
    }

    private static List<DepositAllTask> createGeneralTasks(AutoDepositPlan plan) {
        List<DepositAllTask> result = new ArrayList<>();
        for (ItemTarget target : plan.generalTargets()) {
            result.add(new DepositAllTask(false, target));
        }
        return List.copyOf(result);
    }

    private List<DepositAllTask> generalTasks() {
        if (generalTasks == null) {
            generalTasks = createGeneralTasks(plan);
        }
        return generalTasks;
    }

    private AutoDepositTrustedStoreTask trustedTask() {
        if (trustedTask == null && plan.trustedTargets().length > 0
                && !plan.trustedCandidates().isEmpty()) {
            trustedTask = new AutoDepositTrustedStoreTask(
                    plan.context(),
                    trustedRepository,
                    plan.trustedCandidates(),
                    exactOpenContainerBinding,
                    plan.trustedTargets()
            );
        }
        return trustedTask;
    }

    private boolean hasGeneralSteps() {
        return plan.generalTargets().length > 0;
    }

    @Override
    protected void onStop(Task interruptTask) {
        stopManifestTracking();
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

    public WorkingSetSnapshot snapshot() {
        return workingSet;
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
        return manifest;
    }
}
