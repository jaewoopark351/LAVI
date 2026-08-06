package lavi.minecraft.diagnostics.mining;

import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260806_kpopmodder: Keep mining diagnostic event contract and emission bounds in one place.
public final class MiningDiagnosticEmitter {
    private MiningDiagnosticEmitter() {
    }

    public static void emit(String eventName,
                            String reason,
                            Task task,
                            String bucket,
                            String fingerprint,
                            Object[] eventFields) {
        MiningDiagnosticEventGate.Decision decision = MiningDiagnosticEventGate.evaluate(bucket, fingerprint);
        if (!decision.emit) {
            return;
        }
        Object[] contractFields = new Object[]{
                "mode", "BOUNDARY",
                "dedupe_key", fingerprint,
                "max_emission", "detail_per_bucket=256,session=5000,summary_ticks=200",
                "terminal", false,
                "behavior_effect", "none",
                "summary", decision.summary,
                "suppressedCount", decision.suppressedCount,
                "firstObservedTick", decision.firstObservedTick,
                "lastObservedTick", decision.lastObservedTick
        };
        ChatClefDiagnostics.logBoundary(eventName, reason, task,
                ChatClefDiagnostics.withCommandContextFields(merge(contractFields, eventFields)));
    }

    static String destroyTarget(Task task) {
        if (task instanceof DestroyBlockTask destroyBlockTask) {
            return ChatClefDiagnostics.blockPos(destroyBlockTask.diagnosticTargetPosition());
        }
        return "none";
    }

    static String safeTaskActive(Task task) {
        return ChatClefDiagnostics.safeValue(() -> task != null && task.isActive());
    }

    static String safeTaskStopped(Task task) {
        return ChatClefDiagnostics.safeValue(() -> task != null && task.stopped());
    }

    static String taskClass(Task task) {
        return task == null ? "none" : task.getClass().getName();
    }

    static String instanceId(Task task) {
        return task == null ? "none" : Integer.toHexString(System.identityHashCode(task));
    }

    public static String joinFingerprint(String... values) {
        return String.join("|", values);
    }

    public static Object[] merge(Object[]... arrays) {
        int length = 0;
        for (Object[] array : arrays) {
            if (array != null) {
                length += array.length;
            }
        }
        Object[] merged = new Object[length];
        int offset = 0;
        for (Object[] array : arrays) {
            if (array == null || array.length == 0) {
                continue;
            }
            System.arraycopy(array, 0, merged, offset, array.length);
            offset += array.length;
        }
        return merged;
    }
}
