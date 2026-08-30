package lavi.minecraft.diagnostics.mining.formatting;

import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260830_kpopmodder: Own passive task labels used by mining diagnostic projections.
public final class MiningDiagnosticTaskFields {
    private MiningDiagnosticTaskFields() {
    }

    public static String destroyTarget(Task task) {
        if (task instanceof DestroyBlockTask destroyBlockTask) {
            return ChatClefDiagnostics.blockPos(destroyBlockTask.diagnosticTargetPosition());
        }
        return "none";
    }

    public static String safeTaskActive(Task task) {
        return ChatClefDiagnostics.safeValue(() -> task != null && task.isActive());
    }

    public static String safeTaskStopped(Task task) {
        return ChatClefDiagnostics.safeValue(() -> task != null && task.stopped());
    }

    public static String taskClass(Task task) {
        return task == null ? "none" : task.getClass().getName();
    }

    public static String instanceId(Task task) {
        return task == null ? "none" : Integer.toHexString(System.identityHashCode(task));
    }
}
