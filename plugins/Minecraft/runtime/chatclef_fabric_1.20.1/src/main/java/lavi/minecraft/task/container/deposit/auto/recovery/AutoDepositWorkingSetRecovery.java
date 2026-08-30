package lavi.minecraft.task.container.deposit.auto.recovery;

import adris.altoclef.AltoClef;
import lavi.minecraft.task.container.deposit.auto.working.PlayerInventorySnapshotReader;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetSnapshot;

import java.util.Objects;

//20260831_kpopmodder: Keep working-set deficit recovery local to one maintenance operation.
/** Owns working-set deficit observation and its operation-local recovery child. */
public final class AutoDepositWorkingSetRecovery {
    private final WorkingSetSnapshot workingSet;
    private final AutoDepositDestinationManifest manifest;
    private final PlayerInventorySnapshotReader inventoryReader;
    private RecoverReservedItemsTask recoveryTask;

    public AutoDepositWorkingSetRecovery(
            WorkingSetSnapshot workingSet,
            AutoDepositDestinationManifest manifest,
            PlayerInventorySnapshotReader inventoryReader) {
        this.workingSet = workingSet;
        this.manifest = Objects.requireNonNull(manifest, "manifest");
        this.inventoryReader = Objects.requireNonNull(inventoryReader, "inventoryReader");
    }

    public boolean available() {
        return workingSet != null;
    }

    public int deficitTypeCount(AltoClef mod) {
        if (workingSet == null) {
            return 0;
        }
        return workingSet.deficits(inventoryReader.readMainAndCursor(mod)).size();
    }

    public RecoverReservedItemsTask begin() {
        if (workingSet == null) {
            return null;
        }
        if (recoveryTask == null) {
            recoveryTask = new RecoverReservedItemsTask(workingSet, manifest);
        }
        return recoveryTask;
    }

    public RecoverReservedItemsTask task() {
        return recoveryTask;
    }

    public WorkingSetSnapshot snapshot() {
        return workingSet;
    }
}
