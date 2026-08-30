package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.container.DepositAllTask;
import adris.altoclef.tasks.container.StoreInAnyContainerTask;
import adris.altoclef.tasks.container.StoreInContainerTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.task.container.home.execution.StoreHomeTask;

import java.util.Objects;

//20260826_kpopmodder: Prevent duplicate automatic storage while a user-owned deposit route already exists.
public final class DepositAllAutoConflictGuard {

    public boolean hasExistingDepositTask(AltoClef mod) {
        Objects.requireNonNull(mod, "mod");
        Task userTask = mod.getUserTaskChain().getCurrentTask();
        return hasExistingDepositTask(userTask);
    }

    public boolean hasExistingDepositTask(Task userTaskRoot) {
        return userTaskRoot != null
                && userTaskRoot.thisOrChildSatisfies(this::isDepositRoute);
    }

    //20260829_kpopmodder: Classify only the assigned StoreHome root, never a cached child path.
    public boolean isStoreHomeRoot(Task task) {
        return task instanceof StoreHomeTask;
    }

    boolean isDepositRoute(Task task) {
        return task != null && isDepositRouteClass(task.getClass());
    }

    boolean isDepositRouteClass(Class<? extends Task> taskClass) {
        return DepositAllTask.class.isAssignableFrom(taskClass)
                || StoreInAnyContainerTask.class.isAssignableFrom(taskClass)
                || StoreInContainerTask.class.isAssignableFrom(taskClass);
    }
}
