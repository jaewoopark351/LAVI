package lavi.minecraft.task.container.deposit.auto.policy;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.UserTaskChain;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetSnapshot;

import java.util.List;
import java.util.Objects;

public final class AutoDepositContextSnapshot {
    private final Object worldIdentity;
    private final Dimension dimension;
    private final String persistentWorldKey;
    private final long epoch;
    private final Task userTaskRoot;
    private final WorkingSetSnapshot workingSet;
    private final List<String> taskPathFingerprint;

    public AutoDepositContextSnapshot(Object worldIdentity,
                                      Dimension dimension,
                                      String persistentWorldKey,
                                      long epoch,
                                      Task userTaskRoot,
                                      WorkingSetSnapshot workingSet,
                                      List<String> taskPathFingerprint) {
        this.worldIdentity = Objects.requireNonNull(worldIdentity, "worldIdentity");
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        this.persistentWorldKey = Objects.requireNonNull(persistentWorldKey, "persistentWorldKey");
        this.epoch = epoch;
        this.userTaskRoot = userTaskRoot;
        this.workingSet = workingSet;
        this.taskPathFingerprint = List.copyOf(taskPathFingerprint);
        if ((userTaskRoot == null) != (workingSet == null)) {
            throw new IllegalArgumentException("active context requires both root and working set");
        }
    }

    public Object worldIdentity() {
        return worldIdentity;
    }

    public Dimension dimension() {
        return dimension;
    }

    public String persistentWorldKey() {
        return persistentWorldKey;
    }

    public long epoch() {
        return epoch;
    }

    public Task userTaskRoot() {
        return userTaskRoot;
    }

    public WorkingSetSnapshot workingSet() {
        return workingSet;
    }

    public boolean activeUserTask() {
        return userTaskRoot != null;
    }

    public List<String> taskPathFingerprint() {
        return taskPathFingerprint;
    }

    public boolean matches(AltoClef mod) {
        if (mod == null || mod.getWorld() != worldIdentity || WorldHelper.getCurrentDimension() != dimension) {
            return false;
        }
        UserTaskChain chain = mod.getUserTaskChain();
        boolean active = chain != null && chain.isActive() && !chain.isRunningIdleTask();
        if (!activeUserTask()) {
            return !active;
        }
        return active && chain.getCurrentTask() == userTaskRoot;
    }
}
