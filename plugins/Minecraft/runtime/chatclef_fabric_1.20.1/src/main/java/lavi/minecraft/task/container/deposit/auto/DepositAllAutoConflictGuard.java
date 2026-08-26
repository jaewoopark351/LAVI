package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.container.DepositAllTask;
import adris.altoclef.tasks.container.StoreInAnyContainerTask;
import adris.altoclef.tasks.container.StoreInContainerTask;
import adris.altoclef.tasksystem.Task;

import java.util.Objects;

//20260826_kpopmodder: Prevent duplicate automatic storage while a user-owned deposit route already exists.
public final class DepositAllAutoConflictGuard {

    public boolean hasExistingDepositTask(AltoClef mod) {
        Objects.requireNonNull(mod, "mod");
        Task userTask = mod.getUserTaskChain().getCurrentTask();
        return userTask != null && userTask.thisOrChildSatisfies(task ->
                task instanceof DepositAllTask
                        || task instanceof StoreInAnyContainerTask
                        || task instanceof StoreInContainerTask
        );
    }
}
