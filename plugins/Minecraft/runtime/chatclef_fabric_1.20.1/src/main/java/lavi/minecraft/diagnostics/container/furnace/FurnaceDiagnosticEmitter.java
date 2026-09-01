package lavi.minecraft.diagnostics.container.furnace;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260807_kpopmodder: Keep furnace diagnostic event contract separate from task decisions.
final class FurnaceDiagnosticEmitter {
    private FurnaceDiagnosticEmitter() {
    }

    static boolean emit(String eventName,
                        String reason,
                        Task task,
                        String bucket,
                        String fingerprint,
                        Object[] eventFields) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return false;
        }
        FurnaceDiagnosticEventGate.Decision decision = FurnaceDiagnosticEventGate.evaluate(bucket, fingerprint);
        if (!decision.emit) {
            return false;
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
        boolean sourceEmissionCompleted = ChatClefDiagnostics.logBoundaryWithPhysicalOutcome(
                eventName,
                reason,
                task,
                ChatClefDiagnostics.withCommandContextFields(merge(contractFields, eventFields))
        );
        return sourceEmissionCompleted;
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
