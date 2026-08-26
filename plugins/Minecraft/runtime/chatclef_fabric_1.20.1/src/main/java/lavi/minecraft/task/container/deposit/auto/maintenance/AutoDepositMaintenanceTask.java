package lavi.minecraft.task.container.deposit.auto.maintenance;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.container.DepositAllTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.WorldHelper;
import lavi.minecraft.task.container.deposit.auto.DepositAllAutoDiagnostics;
import lavi.minecraft.task.container.deposit.auto.recovery.AutoDepositDestinationManifest;
import lavi.minecraft.task.container.deposit.auto.recovery.AutoDepositDestinationManifestTracker;
import lavi.minecraft.task.container.deposit.auto.recovery.RecoverReservedItemsTask;
import lavi.minecraft.task.container.deposit.auto.working.PlayerInventorySnapshotReader;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetSnapshot;
import net.minecraft.item.Item;

import java.util.Map;
import java.util.Objects;

//20260826_kpopmodder: Added one-way surplus deposit, verification, and bounded recovery ownership.
public final class AutoDepositMaintenanceTask extends Task {
    private final WorkingSetSnapshot snapshot;
    private final DepositAllTask depositTask;
    private final AutoDepositDestinationManifest manifest;
    private final PlayerInventorySnapshotReader inventoryReader = new PlayerInventorySnapshotReader();

    private AutoDepositDestinationManifestTracker manifestTracker;
    private RecoverReservedItemsTask recoveryTask;
    private AutoDepositMaintenancePhase phase = AutoDepositMaintenancePhase.DEPOSIT_SURPLUS;

    public AutoDepositMaintenanceTask(WorkingSetSnapshot snapshot, ItemTarget[] surplusTargets) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        depositTask = new DepositAllTask(
                false,
                Objects.requireNonNull(surplusTargets, "surplusTargets").clone()
        );
        manifest = new AutoDepositDestinationManifest(
                snapshot.worldIdentity(), snapshot.dimension(), snapshot.epoch()
        );
    }

    @Override
    protected void onStart() {
        if (phase == AutoDepositMaintenancePhase.DEPOSIT_SURPLUS) {
            startManifestTracking();
        }
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();
        if (!contextMatches(mod)) {
            stopManifestTracking();
            transition(AutoDepositMaintenancePhase.CANCELLED, "working_set_context_changed", 0);
            return null;
        }

        switch (phase) {
            case DEPOSIT_SURPLUS -> {
                if (depositTask.isFinished() || depositTask.stopped()) {
                    stopManifestTracking();
                    transition(AutoDepositMaintenancePhase.VERIFY_WORKING_SET, "surplus_deposit_terminal", 0);
                    return null;
                }
                return depositTask;
            }
            case VERIFY_WORKING_SET -> {
                Map<Item, Integer> deficits = snapshot.deficits(inventoryReader.readMainAndCursor(mod));
                if (deficits.isEmpty()) {
                    transition(AutoDepositMaintenancePhase.DONE, "working_set_preserved", 0);
                    return null;
                }
                recoveryTask = new RecoverReservedItemsTask(snapshot, manifest);
                transition(AutoDepositMaintenancePhase.RECOVER, "working_set_deficit_detected", deficits.size());
                return recoveryTask;
            }
            case RECOVER -> {
                if (recoveryTask == null || recoveryTask.isFinished()) {
                    int remaining = snapshot.deficits(inventoryReader.readMainAndCursor(mod)).size();
                    transition(AutoDepositMaintenancePhase.DONE, "recovery_terminal", remaining);
                    return null;
                }
                return recoveryTask;
            }
            case DONE, CANCELLED -> {
                return null;
            }
        }
        return null;
    }

    private boolean contextMatches(AltoClef mod) {
        return mod.getWorld() == snapshot.worldIdentity()
                && WorldHelper.getCurrentDimension() == snapshot.dimension()
                && mod.getUserTaskChain() != null
                && mod.getUserTaskChain().getCurrentTask() == snapshot.userTaskRoot();
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
                snapshot.epoch(), previous, next, reason, deficitTypes, this
        );
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

    public WorkingSetSnapshot snapshot() {
        return snapshot;
    }

    public DepositAllTask depositTask() {
        return depositTask;
    }

    public AutoDepositMaintenancePhase phase() {
        return phase;
    }

    public AutoDepositDestinationManifest manifest() {
        return manifest;
    }
}
