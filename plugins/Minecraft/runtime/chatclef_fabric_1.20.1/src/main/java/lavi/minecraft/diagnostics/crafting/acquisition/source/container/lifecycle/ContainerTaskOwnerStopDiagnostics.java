package lavi.minecraft.diagnostics.crafting.acquisition.source.container.lifecycle;

import adris.altoclef.tasks.container.DoStuffInContainerTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetObservationKind;

//20260902_kpopmodder: Emit one bounded source record from the existing container onStop callback.
public final class ContainerTaskOwnerStopDiagnostics {
    private ContainerTaskOwnerStopDiagnostics() {
    }

    public static boolean log(Task owner, Task interruptTask) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()
                || !(owner instanceof DoStuffInContainerTask)
                || !CraftResourceContainerLifecycleSourceEventObserver
                        .isOwnerTracked(owner)) {
            return false;
        }
        CraftResourceTargetObservationKind terminationKind = interruptTask == null
                ? CraftResourceTargetObservationKind.OWNER_STOP
                : CraftResourceTargetObservationKind.OWNER_INTERRUPT;
        boolean sourceEmissionCompleted =
                ChatClefDiagnostics.logBoundaryWithPhysicalOutcome(
                        "CONTAINER_TASK_OWNER_STOP",
                        "container_task_owner_stop",
                        owner,
                        new Object[]{
                                "ownerTerminationKind", terminationKind.name(),
                                "ownerTaskClass", taskClass(owner),
                                "ownerTaskInstanceId", taskInstanceId(owner),
                                "interruptTaskClass", taskClass(interruptTask),
                                "interruptTaskInstanceId", taskInstanceId(interruptTask),
                                "interruptTaskPresent", interruptTask != null,
                                "currentContextNotAssociationAuthority", true,
                                "behavior_effect", "none"
                        }
                );
        CraftResourceContainerLifecycleSourceEventObserver.observe(
                owner,
                interruptTask,
                terminationKind,
                sourceEmissionCompleted
        );
        return sourceEmissionCompleted;
    }

    private static String taskClass(Task task) {
        return task == null ? "UNAVAILABLE" : task.getClass().getName();
    }

    private static String taskInstanceId(Task task) {
        return task == null
                ? "UNAVAILABLE"
                : task.getClass().getName()
                        + "@"
                        + Integer.toHexString(System.identityHashCode(task));
    }
}
