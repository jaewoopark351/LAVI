package lavi.minecraft.diagnostics.container.gui.task;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.gui.correlation.ContainerTargetFamily;

//20260904_kpopmodder: Classify only observed task roles; never select or replace a Task.
public final class ContainerTaskRoleClassifier {
    public boolean relevant(
            ContainerTargetFamily family,
            Task parent,
            Task activeChildBefore,
            Task candidateChild,
            Task activeChildAfter) {
        return relevant(family, parent)
                || relevant(family, activeChildBefore)
                || relevant(family, candidateChild)
                || relevant(family, activeChildAfter);
    }

    public boolean isOpenChild(Task task) {
        String name = className(task);
        return name.endsWith(".InteractWithBlockTask")
                || name.endsWith(".DoToClosestBlockTask");
    }

    public boolean isTransferChild(Task task) {
        String name = className(task);
        return name.contains("MoveItemToSlot")
                || name.contains("HomeStorageTransfer")
                || name.contains("ExactPickupFromContainerTask");
    }

    public String role(ContainerTargetFamily family, Task task) {
        if (task == null) return "NONE";
        if (isOpenChild(task)) return "OPEN_CHILD";
        if (isTransferChild(task)) return "TRANSFER_CHILD";
        String name = className(task);
        if (family == ContainerTargetFamily.FURNACE
                && (name.contains("SmeltInFurnaceTask") || name.contains("DoSmeltInFurnaceTask"))) {
            return "FURNACE_ROUTE_OWNER";
        }
        if (family == ContainerTargetFamily.CHEST
                && (name.contains("StoreInContainerTask")
                || name.contains("StoreHomeTask")
                || name.contains("DepositAllTask"))) {
            return "CHEST_ROUTE_OWNER";
        }
        if (name.contains("container")) return "CONTAINER_DESCENDANT";
        return "OTHER";
    }

    public String className(Task task) {
        return task == null ? "none" : task.getClass().getName();
    }

    public String identity(Task task) {
        return task == null
                ? "unavailable"
                : task.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(task));
    }

    public boolean relevant(ContainerTargetFamily family, Task task) {
        return !"OTHER".equals(role(family, task)) && !"NONE".equals(role(family, task));
    }
}
