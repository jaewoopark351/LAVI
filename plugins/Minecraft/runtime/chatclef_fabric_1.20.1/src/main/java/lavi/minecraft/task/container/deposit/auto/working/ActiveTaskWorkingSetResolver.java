package lavi.minecraft.task.container.deposit.auto.working;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.UserTaskChain;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class ActiveTaskWorkingSetResolver {
    private final AtomicLong epochs = new AtomicLong();
    private final PlayerInventorySnapshotReader inventoryReader = new PlayerInventorySnapshotReader();
    private final WorkingSetRequirementCollector requirementCollector = new WorkingSetRequirementCollector();

    public WorkingSetResolution resolve(AltoClef mod) {
        return resolve(mod, null);
    }

    //20260914_kpopmodder: Revalidate the current user tree after this exact automatic owner wins native selection.
    public WorkingSetResolution resolve(AltoClef mod, TaskChain selectedAutomaticOwner) {
        UserTaskChain userTaskChain = mod.getUserTaskChain();
        if (userTaskChain == null || !userTaskChain.isActive() || userTaskChain.isRunningIdleTask()) {
            return WorkingSetResolution.unsupported("no_active_non_idle_user_task");
        }
        TaskChain selected = mod.getTaskRunner().getCurrentTaskChain();
        if (selected != userTaskChain && (selectedAutomaticOwner == null || selected != selectedAutomaticOwner)) {
            return WorkingSetResolution.unsupported("user_task_chain_not_selected");
        }
        Task root = userTaskChain.getCurrentTask();
        if (root == null) {
            return WorkingSetResolution.unsupported("user_task_root_missing");
        }
        if (root.stopped()) return WorkingSetResolution.unsupported("user_task_root_stopped");
        List<Task> path = List.copyOf(userTaskChain.getTasks());
        if (path.isEmpty() || path.get(0) != root
                || path.stream().anyMatch(cached -> !root.thisOrChildSatisfies(actual -> actual == cached))) {
            return WorkingSetResolution.unsupported("stale_user_task_path");
        }
        if (mod.getWorld() == null || mod.getPlayer() == null) {
            return WorkingSetResolution.unsupported("world_or_player_missing");
        }

        Object worldIdentity = mod.getWorld();
        Dimension dimension = WorldHelper.getCurrentDimension();
        WorkingSetResolution result = requirementCollector.collect(
                mod,
                root,
                path,
                worldIdentity,
                dimension,
                epochs.incrementAndGet(),
                inventoryReader.readMain(mod),
                inventoryReader.readMainAndCursor(mod)
        );
        if (!userTaskChain.isActive() || userTaskChain.isRunningIdleTask()
                || root.stopped() || root != userTaskChain.getCurrentTask()
                || selected != mod.getTaskRunner().getCurrentTaskChain()
                || worldIdentity != mod.getWorld()
                || dimension != WorldHelper.getCurrentDimension()) {
            return WorkingSetResolution.unsupported("working_set_context_changed");
        }
        return result;
    }
}
