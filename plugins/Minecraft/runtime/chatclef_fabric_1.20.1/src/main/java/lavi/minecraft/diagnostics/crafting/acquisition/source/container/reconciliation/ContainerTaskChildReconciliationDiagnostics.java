package lavi.minecraft.diagnostics.crafting.acquisition.source.container.reconciliation;

import adris.altoclef.tasks.container.DoStuffInContainerTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260901_kpopmodder: Emit only applied container child transitions from the scheduler boundary.
public final class ContainerTaskChildReconciliationDiagnostics {
    private ContainerTaskChildReconciliationDiagnostics() {
    }

    public static boolean log(
            Task parent,
            Task activeChildBefore,
            Task candidateChild,
            boolean isEqualResult,
            boolean canInterruptEvaluated,
            boolean canInterruptPreviousChild,
            boolean replacementApplied,
            boolean previousChildStopCalled,
            Task activeChildAfter,
            boolean candidateDiscardedBecauseEqual,
            boolean childCleared) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()
                || !(parent instanceof DoStuffInContainerTask)) {
            return false;
        }

        boolean appliedReplacement = replacementApplied
                && candidateChild != null
                && activeChildAfter == candidateChild;
        boolean appliedClear = childCleared
                && activeChildBefore != null
                && activeChildAfter == null;
        boolean sourceEmissionCompleted = false;
        if (appliedReplacement || appliedClear) {
            sourceEmissionCompleted = ChatClefDiagnostics.logBoundaryWithPhysicalOutcome(
                    "CONTAINER_TASK_CHILD_RECONCILIATION",
                    appliedClear
                            ? "container_task_child_cleared"
                            : "container_task_child_replaced",
                    parent,
                    ChatClefDiagnostics.withCommandContextFields(new Object[]{
                            "parentTaskClass", taskClass(parent),
                            "parentTaskInstanceId", taskInstanceId(parent),
                            "activeChildBeforeClass", taskClass(activeChildBefore),
                            "activeChildBeforeInstanceId", taskInstanceId(activeChildBefore),
                            "candidateChildClass", taskClass(candidateChild),
                            "candidateChildInstanceId", taskInstanceId(candidateChild),
                            "activeChildAfterClass", taskClass(activeChildAfter),
                            "activeChildAfterInstanceId", taskInstanceId(activeChildAfter),
                            "isEqualResult", isEqualResult,
                            "canInterruptEvaluated", canInterruptEvaluated,
                            "canInterruptPreviousChild", canInterruptPreviousChild,
                            "replacementApplied", replacementApplied,
                            "previousChildStopCalled", previousChildStopCalled,
                            "candidateDiscardedBecauseEqual", candidateDiscardedBecauseEqual,
                            "childCleared", childCleared
                    })
            );
        }
        CraftResourceContainerReconciliationSourceEventObserver.observe(
                parent,
                activeChildBefore,
                candidateChild,
                isEqualResult,
                canInterruptEvaluated,
                canInterruptPreviousChild,
                replacementApplied,
                previousChildStopCalled,
                activeChildAfter,
                candidateDiscardedBecauseEqual,
                childCleared,
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
