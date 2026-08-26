package lavi.minecraft.task.container.deposit;

import adris.altoclef.tasks.container.StoreInContainerTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

//20260826_kpopmodder: Added stable same-target StoreInContainerTask ownership for deposit_all.
public final class DepositAllStoreTaskGeneration {
    private final StoreTaskFactory storeTaskFactory;
    private final TaskFinishedProbe finishedProbe;

    private BlockPos activeStoreTarget;
    private ItemTarget[] activeStoreSnapshot;
    private Task activeStoreTask;
    private int generationId;

    public DepositAllStoreTaskGeneration() {
        this(
                (target, getIfNotPresent, snapshot) -> new StoreInContainerTask(target, getIfNotPresent, snapshot),
                Task::isFinished
        );
    }

    public DepositAllStoreTaskGeneration(StoreTaskFactory storeTaskFactory, TaskFinishedProbe finishedProbe) {
        this.storeTaskFactory = Objects.requireNonNull(storeTaskFactory, "storeTaskFactory");
        this.finishedProbe = Objects.requireNonNull(finishedProbe, "finishedProbe");
    }

    public Task getOrCreate(BlockPos target, boolean getIfNotPresent, ItemTarget[] notStored) {
        Objects.requireNonNull(target, "target");
        if (activeStoreTask == null || !matches(target)) {
            activeStoreTarget = target.toImmutable();
            activeStoreSnapshot = snapshot(notStored);
            activeStoreTask = Objects.requireNonNull(
                    storeTaskFactory.create(activeStoreTarget, getIfNotPresent, activeStoreSnapshot),
                    "activeStoreTask"
            );
            generationId++;
        }
        return activeStoreTask;
    }

    public boolean clearIfFinishedWithRemainingWork(BlockPos target, ItemTarget[] currentNotStored) {
        if (!hasActiveFor(target) || activeStoreTask == null || currentNotStored == null || currentNotStored.length == 0) {
            return false;
        }
        if (!finishedProbe.isFinished(activeStoreTask)) {
            return false;
        }
        return clear();
    }

    public boolean hasActiveFor(BlockPos target) {
        return activeStoreTask != null && matches(target);
    }

    public Optional<BlockPos> activeStoreTarget() {
        return Optional.ofNullable(activeStoreTarget);
    }

    public ItemTarget[] activeStoreSnapshot() {
        return snapshot(activeStoreSnapshot);
    }

    public int generationId() {
        return generationId;
    }

    public boolean clear() {
        if (activeStoreTask == null && activeStoreTarget == null && activeStoreSnapshot == null) {
            return false;
        }
        activeStoreTarget = null;
        activeStoreSnapshot = null;
        activeStoreTask = null;
        return true;
    }

    private boolean matches(BlockPos target) {
        return activeStoreTarget != null && activeStoreTarget.equals(target);
    }

    private static ItemTarget[] snapshot(ItemTarget[] targets) {
        if (targets == null || targets.length == 0) {
            return new ItemTarget[0];
        }
        return Arrays.copyOf(targets, targets.length);
    }

    @FunctionalInterface
    public interface StoreTaskFactory {
        Task create(BlockPos target, boolean getIfNotPresent, ItemTarget[] snapshot);
    }

    @FunctionalInterface
    public interface TaskFinishedProbe {
        boolean isFinished(Task task);
    }
}
