//#if MC == 12001
package lavi.minecraft.task.movement.gotopreflight.diagnostics;

import lavi.minecraft.task.movement.gotopreflight.GotoMaterialPlan;

import java.util.Set;

//20260913_kpopmodder: Format only owner-produced observations without querying movement state.
public final class GotoPreparationLogFormatter {
    static final int MAX_DETAIL_CHARS = 1536;
    static final int MAX_CONTEXT_VALUE_CHARS = 256;
    private static final Set<String> PHASES = Set.of(
            "NATIVE", "DRAIN_TO_PREPARE", "PREPARE", "DRAIN_TO_FINAL",
            "FINAL_NAVIGATION", "ARRIVAL_CLEANUP", "TERMINAL");
    private static final Set<String> EVENTS = Set.of(
            "NATIVE_FIRST", "PREPARATION_SELECTED", "PREPARATION_CHECK",
            "MATERIALS_ALREADY_SUFFICIENT", "PREPARED", "ACQUIRE_START", "RESUME_ORIGINAL", "ARRIVED");
    private static final Set<String> NATIVE_REASONS = Set.of(
            "AIR_COLUMN_UNAVAILABLE", "AERIAL_CLASSIFIED", "NATIVE_NOT_SAFE_TO_PAUSE",
            "MATERIALS_SUFFICIENT_NATIVE_CONTINUES", "FAR_FROM_FOUNDATION",
            "BELOW_PREPARATION_AREA", "ABOVE_PREPARATION_AREA", "PREPARATION_AREA_UNLOADED",
            "NOT_DRY_GROUNDED", "PREPARATION_UNDER_COVER", "PREPARATION_BODY_OBSTRUCTED",
            "PREPARATION_FOOTING_UNSAFE");

    private GotoPreparationLogFormatter() {
    }

    static String operation(Object owner) {
        return Integer.toHexString(System.identityHashCode(owner));
    }

    static String event(String message) {
        if (message == null) return "UNKNOWN";
        int end = message.indexOf(' ');
        return end < 0 ? message : message.substring(0, end);
    }

    static String value(String message, String name) {
        if (message == null) return "";
        String marker = " " + name + "=";
        int start = message.indexOf(marker);
        if (start < 0) return "";
        start += marker.length();
        int end = message.indexOf(' ', start);
        return message.substring(start, end < 0 ? message.length() : end);
    }

    /** Closed semantic vocabulary bounds reservation storage; runtime IDs never form signatures. */
    static String requiredSignature(String message) {
        String event = event(message);
        if (EVENTS.contains(event)) return event;
        if (event.equals("PHASE")) {
            String from = value(message, "from");
            String to = value(message, "to");
            return PHASES.contains(from) && PHASES.contains(to) ? event + ":" + from + ":" + to : null;
        }
        if (event.equals("NATIVE_DECISION")) {
            String reason = value(message, "reason");
            return NATIVE_REASONS.contains(reason) ? event + ":" + reason : null;
        }
        if (event.equals("HANDOFF_CHECK")) {
            String ready = value(message, "quantityReady");
            return ready.equals("true") || ready.equals("false") ? event + ":" + ready : null;
        }
        if (event.equals("CLEANUP_WAIT")) {
            String phase = value(message, "phase");
            String ticks = value(message, "ticks");
            return PHASES.contains(phase)
                    && (ticks.equals("1") || ticks.equals(Integer.toString(GotoMaterialPlan.MAX_CLEANUP_TICKS)))
                    ? event + ":" + phase + ":" + ticks : null;
        }
        if (event.equals("FAILED")) {
            String reason = value(message, "reason");
            for (GotoMaterialPlan.FailureReason known : GotoMaterialPlan.FailureReason.values()) {
                if (known.name().equals(reason)) return event + ":" + reason;
            }
        }
        return null;
    }

    static Object[] fields(String operation, String message, String signature, String admission, Object[] context) {
        Object[] local = new Object[]{
                "owner", "PreparedGotoTask",
                "operationId", operation,
                "taskIdentity", operation,
                "ownerEvent", event(message),
                "requiredBoundarySignature", signature == null ? "none" : signature,
                "diagnosticAdmission", admission,
                "behavior_effect", "none",
                "detail", bounded(message, MAX_DETAIL_CHARS)
        };
        Object[] result = new Object[local.length + context.length];
        System.arraycopy(local, 0, result, 0, local.length);
        System.arraycopy(context, 0, result, local.length, context.length);
        return result;
    }

    static Object[] contextSnapshot(Object[] fields) {
        if (fields == null || fields.length % 2 != 0) {
            return new Object[]{"commandContextAvailable", false, "commandContextError", "invalid_snapshot"};
        }
        // The existing provider has eight pairs; retain at most nine bounded pairs for compatibility.
        int length = Math.min(fields.length, 18);
        Object[] snapshot = new Object[length];
        for (int index = 0; index < length; index++) {
            Object value = fields[index];
            snapshot[index] = value instanceof String text ? bounded(text, MAX_CONTEXT_VALUE_CHARS) : value;
        }
        return snapshot;
    }

    static String bounded(String text, int limit) {
        if (text == null) return "unavailable";
        String clean = text.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ');
        return clean.length() <= limit ? clean : clean.substring(0, limit);
    }
}
//#endif
