package lavi.minecraft.diagnostics.container.store.deposit.budget;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;

import java.util.LinkedHashSet;
import java.util.Set;

//20260830_kpopmodder: Route every Slice A projection through the canonical UTF-8 payload cap.
public final class StoreDepositBoundedEventLogger {
    public static final int MAX_EVENT_UTF8_BYTES = 8192;

    private StoreDepositBoundedEventLogger() {
    }

    public static void log(String eventName,
                           String reason,
                           Task task,
                           Object[] requiredFields) {
        log(
                eventName,
                reason,
                task,
                MAX_EVENT_UTF8_BYTES,
                requiredFields,
                new Object[0]
        );
    }

    public static void log(String eventName,
                           String reason,
                           Task task,
                           int maxUtf8Bytes,
                           Object[] requiredFields,
                           Object[] optionalFields) {
        ChatClefDiagnostics.logBoundedBoundary(
                eventName,
                reason,
                task,
                maxUtf8Bytes,
                withCommonObservationFields(requiredFields),
                optionalFields
        );
    }

    private static Object[] commonObservationFields() {
        return new Object[]{
                "diagnosticsMode", ChatClefDiagnostics.isVerboseEnabled() ? "VERBOSE" : "BOUNDARY",
                "gameTick", ChatClefDiagnostics.currentClientTickId(),
                "dimension", "UNAVAILABLE_NOT_RETAINED",
                "topLevelTask", "UNAVAILABLE_NOT_RETAINED",
                "activeParentTask", "UNAVAILABLE_NOT_RETAINED",
                "activeChildTask", "UNAVAILABLE_NOT_RETAINED",
                "autoOperationEpoch", "UNAVAILABLE",
                "maintenanceGenerationId", "UNAVAILABLE",
                "autoChildOperationId", "UNAVAILABLE",
                "storeOperationId", "UNAVAILABLE",
                "selectedCandidateGenerationId", "UNAVAILABLE",
                "storeAttemptId", "UNAVAILABLE",
                "routeChildLifecycleId", "UNAVAILABLE",
                "transferAttemptId", "UNAVAILABLE",
                "slotActionId", "UNAVAILABLE",
                "slotMutationId", "UNAVAILABLE"
        };
    }

    private static Object[] withCommonObservationFields(Object[] requiredFields) {
        Object[] merged = StoreDepositEventFields.merge(commonObservationFields(), requiredFields);
        Object callerComplete = value(requiredFields, "observationComplete");
        Object callerMissing = value(requiredFields, "missingBoundaries");
        Set<String> missing = new LinkedHashSet<>();
        addCallerMissingBoundary(missing, callerComplete, callerMissing);
        addUnavailableBoundary(missing, merged, "dimension", "DIMENSION");
        addUnavailableBoundary(missing, merged, "topLevelTask", "TOP_LEVEL_TASK");
        addUnavailableBoundary(missing, merged, "activeParentTask", "ACTIVE_PARENT_TASK");
        addUnavailableBoundary(missing, merged, "activeChildTask", "ACTIVE_CHILD_TASK");
        boolean complete = Boolean.TRUE.equals(callerComplete) && missing.isEmpty();
        return StoreDepositEventFields.merge(
                merged,
                new Object[]{
                        "observationComplete", complete,
                        "missingBoundaries", missing.isEmpty() ? "NONE" : String.join(",", missing)
                }
        );
    }

    private static void addCallerMissingBoundary(Set<String> missing,
                                                 Object callerComplete,
                                                 Object callerMissing) {
        String normalized = callerMissing == null ? "UNAVAILABLE" : String.valueOf(callerMissing);
        if (!"NONE".equals(normalized)) {
            missing.add(normalized.startsWith("UNAVAILABLE")
                    ? "CALLER_MISSING_BOUNDARIES_NOT_RETAINED"
                    : normalized);
        }
        if (!Boolean.TRUE.equals(callerComplete) && "NONE".equals(normalized)) {
            missing.add("CALLER_OBSERVATION_INCOMPLETE");
        }
        if (callerComplete == null) {
            missing.add("CALLER_OBSERVATION_COMPLETENESS_NOT_RETAINED");
        }
    }

    private static void addUnavailableBoundary(Set<String> missing,
                                               Object[] fields,
                                               String key,
                                               String boundary) {
        Object retained = value(fields, key);
        String normalized = retained == null ? "UNAVAILABLE" : String.valueOf(retained);
        if (normalized.isBlank()
                || normalized.equalsIgnoreCase("none")
                || normalized.toUpperCase().startsWith("UNAVAILABLE")) {
            missing.add(boundary);
        }
    }

    private static Object value(Object[] fields, String key) {
        if (fields == null) {
            return null;
        }
        for (int index = 0; index + 1 < fields.length; index += 2) {
            if (key.equals(String.valueOf(fields[index]))) {
                return fields[index + 1];
            }
        }
        return null;
    }
}
