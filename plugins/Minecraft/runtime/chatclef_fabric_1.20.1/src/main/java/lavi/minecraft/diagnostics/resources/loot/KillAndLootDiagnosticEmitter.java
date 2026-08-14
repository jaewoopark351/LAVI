package lavi.minecraft.diagnostics.resources.loot;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260814_kpopmodder: Keep kill-and-loot diagnostic event shape separate from resource decisions.
final class KillAndLootDiagnosticEmitter {
    private KillAndLootDiagnosticEmitter() {
    }

    static void emit(String eventName,
                     String reason,
                     Task task,
                     String bucket,
                     String fingerprint,
                     Object[] eventFields) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        KillAndLootDiagnosticEventGate.Decision decision = KillAndLootDiagnosticEventGate.evaluate(bucket, fingerprint);
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

    static String joinFingerprint(String... values) {
        return String.join("|", values);
    }

    static Object[] merge(Object[]... arrays) {
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

    static String taskClass(Task task) {
        return task == null ? "none" : task.getClass().getName();
    }

    static String instanceId(Task task) {
        return task == null ? "none" : Integer.toHexString(System.identityHashCode(task));
    }
}
