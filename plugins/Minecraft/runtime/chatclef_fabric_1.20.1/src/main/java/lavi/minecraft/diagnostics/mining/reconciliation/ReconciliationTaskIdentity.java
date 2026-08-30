package lavi.minecraft.diagnostics.mining.reconciliation;

import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.util.math.BlockPos;

//20260830_kpopmodder: Keep cheap reconciliation identity separate from permit-gated world detail.
public record ReconciliationTaskIdentity(Task task,
                                         String taskClass,
                                         String taskInstanceId,
                                         BlockPos targetPosition,
                                         String targetPositionText) {
    public static ReconciliationTaskIdentity capture(Task task) {
        BlockPos target = task instanceof DestroyBlockTask destroyBlockTask
                ? destroyBlockTask.diagnosticTargetPosition()
                : null;
        return new ReconciliationTaskIdentity(
                task,
                task == null ? "none" : task.getClass().getName(),
                task == null ? "none" : Integer.toHexString(System.identityHashCode(task)),
                target,
                ChatClefDiagnostics.blockPos(target)
        );
    }

    public boolean sameTaskAs(ReconciliationTaskIdentity other) {
        return other != null && task != null && task == other.task;
    }

    public boolean sameTargetAs(ReconciliationTaskIdentity other) {
        return other != null && targetPosition != null && targetPosition.equals(other.targetPosition);
    }
}
