package lavi.minecraft.diagnostics.mining.reconciliation;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.resources.MineAndCollectTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.util.math.BlockPos;

//20260814_kpopmodder: Split task child reconciliation snapshots from emission so active/candidate churn can be proven.
public record ReconciliationTaskSnapshot(
        Task task,
        String taskClass,
        String taskInstanceId,
        String taskSummary,
        BlockPos targetPosition,
        String targetPositionText,
        String targetBlockState,
        String blockStillExists,
        String chunkLoaded,
        String worldCanBreak,
        String scannerUnreachable,
        String localBlacklistContains,
        String taskActive,
        String taskStopped
) {
    public static ReconciliationTaskSnapshot capture(AltoClef mod,
                                                     MineAndCollectTask.MineOrCollectTask parent,
                                                     Task task) {
        BlockPos target = destroyTargetPosition(task);
        return new ReconciliationTaskSnapshot(
                task,
                taskClass(task),
                taskInstanceId(task),
                ChatClefDiagnostics.taskSummary(task),
                target,
                ChatClefDiagnostics.blockPos(target),
                targetBlockState(mod, target),
                blockStillExists(mod, target),
                chunkLoaded(mod, target),
                target == null ? "not_destroy_target" : "NOT_CAPTURED_WITHOUT_BEHAVIOR_REEVALUATION",
                scannerUnreachable(mod, target),
                localBlacklistContains(parent, target),
                safeTaskActive(task),
                safeTaskStopped(task)
        );
    }

    public boolean isDestroyBlockTask() {
        return task instanceof DestroyBlockTask;
    }

    public boolean sameTaskAs(ReconciliationTaskSnapshot other) {
        return other != null && task != null && task == other.task;
    }

    public boolean sameTargetAs(ReconciliationTaskSnapshot other) {
        return other != null && targetPosition != null && targetPosition.equals(other.targetPosition);
    }

    private static BlockPos destroyTargetPosition(Task task) {
        if (task instanceof DestroyBlockTask destroyBlockTask) {
            return destroyBlockTask.diagnosticTargetPosition();
        }
        return null;
    }

    private static String taskClass(Task task) {
        return task == null ? "none" : task.getClass().getName();
    }

    private static String taskInstanceId(Task task) {
        return task == null ? "none" : Integer.toHexString(System.identityHashCode(task));
    }

    private static String targetBlockState(AltoClef mod, BlockPos target) {
        if (target == null) {
            return "not_destroy_target";
        }
        return ChatClefDiagnostics.safeValue(() -> mod == null ? "unavailable" : mod.getWorld().getBlockState(target));
    }

    private static String blockStillExists(AltoClef mod, BlockPos target) {
        if (target == null) {
            return "not_destroy_target";
        }
        return ChatClefDiagnostics.safeValue(() -> mod == null ? "unavailable" : !mod.getWorld().getBlockState(target).isAir());
    }

    private static String chunkLoaded(AltoClef mod, BlockPos target) {
        if (target == null) {
            return "not_destroy_target";
        }
        return ChatClefDiagnostics.safeValue(() -> mod == null ? "unavailable" : mod.getChunkTracker().isChunkLoaded(target));
    }

    private static String scannerUnreachable(AltoClef mod, BlockPos target) {
        if (target == null) {
            return "not_destroy_target";
        }
        return ChatClefDiagnostics.safeValue(() -> mod == null ? "unavailable" : mod.getBlockScanner().isUnreachable(target));
    }

    private static String localBlacklistContains(MineAndCollectTask.MineOrCollectTask parent, BlockPos target) {
        if (target == null) {
            return "not_destroy_target";
        }
        return ChatClefDiagnostics.safeValue(() -> parent.diagnosticLocalBlacklistContains(target));
    }

    private static String safeTaskActive(Task task) {
        return ChatClefDiagnostics.safeValue(() -> task != null && task.isActive());
    }

    private static String safeTaskStopped(Task task) {
        return ChatClefDiagnostics.safeValue(() -> task != null && task.stopped());
    }
}
