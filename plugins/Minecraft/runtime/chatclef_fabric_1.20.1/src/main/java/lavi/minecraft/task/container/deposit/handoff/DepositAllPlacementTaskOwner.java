package lavi.minecraft.task.container.deposit.handoff;

import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import net.minecraft.block.Block;

import java.util.Objects;
import java.util.function.Supplier;

//20260831_kpopmodder: Preserve the scheduler-owned placement identity for a deposit-all handoff.
/**
 * Owns the candidate identity used for one deposit-all placement child.
 */
public final class DepositAllPlacementTaskOwner {
    private final boolean retaining;
    private Block currentBlock;
    private PlaceBlockNearbyTask currentTask;

    private DepositAllPlacementTaskOwner(boolean retaining) {
        this.retaining = retaining;
    }

    public static DepositAllPlacementTaskOwner ephemeral() {
        return new DepositAllPlacementTaskOwner(false);
    }

    public static DepositAllPlacementTaskOwner retaining() {
        return new DepositAllPlacementTaskOwner(true);
    }

    public PlaceBlockNearbyTask getOrCreate(
            Block requestedBlock,
            Supplier<PlaceBlockNearbyTask> taskFactory) {
        Objects.requireNonNull(requestedBlock, "requestedBlock");
        Objects.requireNonNull(taskFactory, "taskFactory");
        if (!retaining) {
            return Objects.requireNonNull(taskFactory.get(), "taskFactory result");
        }
        if (currentTask == null
                || currentTask.stopped()
                || (!currentTask.isActive() && !Objects.equals(currentBlock, requestedBlock))) {
            currentBlock = requestedBlock;
            currentTask = Objects.requireNonNull(taskFactory.get(), "taskFactory result");
        }
        return currentTask;
    }

    public PlaceBlockNearbyTask currentTask() {
        return currentTask;
    }

    public void clear(PlaceBlockNearbyTask expectedTask) {
        if (currentTask == expectedTask) {
            currentBlock = null;
            currentTask = null;
        }
    }

    public boolean retainingIdentity() {
        return retaining;
    }
}
