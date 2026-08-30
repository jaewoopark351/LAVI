package lavi.minecraft.diagnostics.mining.correlation;

import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260830_kpopmodder: Resolve generic mining correlation without querying Store-owned registries.
public record MiningDiagnosticCorrelation(String key,
                                          String source,
                                          Object[] commandContextFields) {
    public static MiningDiagnosticCorrelation capture(Task task) {
        return capture(task, null, null);
    }

    public static MiningDiagnosticCorrelation capture(Task task,
                                                       String fallbackKey,
                                                       String fallbackSource) {
        Object[] commandFields = withExplicitStoreAvailability(
                ChatClefDiagnostics.withCommandContextFields());
        String correlationId = field(commandFields, "commandCorrelationId");
        if (!correlationId.isBlank()) {
            return new MiningDiagnosticCorrelation(bound(correlationId), "COMMAND_CORRELATION_ID", commandFields);
        }
        String requestId = field(commandFields, "commandRequestId");
        if (!requestId.isBlank()) {
            return new MiningDiagnosticCorrelation(bound(requestId), "COMMAND_REQUEST_ID", commandFields);
        }
        if (task instanceof DestroyBlockTask destroyBlockTask) {
            String target = ChatClefDiagnostics.blockPos(destroyBlockTask.diagnosticTargetPosition());
            return new MiningDiagnosticCorrelation(
                    bound("DESTROY_TARGET|" + target),
                    "DESTROY_TARGET_FALLBACK",
                    commandFields
            );
        }
        if (task != null) {
            String fallback = task.getClass().getName()
                    + '#'
                    + Integer.toHexString(System.identityHashCode(task));
            return new MiningDiagnosticCorrelation(bound(fallback), "TASK_INSTANCE_FALLBACK", commandFields);
        }
        if (fallbackKey != null && !fallbackKey.isBlank()) {
            String source = fallbackSource == null || fallbackSource.isBlank()
                    ? "CALLER_FALLBACK"
                    : fallbackSource;
            return new MiningDiagnosticCorrelation(bound(fallbackKey), bound(source), commandFields);
        }
        return new MiningDiagnosticCorrelation("UNAVAILABLE", "UNAVAILABLE", commandFields);
    }

    private static Object[] withExplicitStoreAvailability(Object[] fields) {
        if (!field(fields, "storeOperationId").isBlank()) {
            return fields;
        }
        Object[] safeFields = fields == null ? new Object[0] : fields;
        Object[] withUnavailable = new Object[safeFields.length + 2];
        System.arraycopy(safeFields, 0, withUnavailable, 0, safeFields.length);
        withUnavailable[safeFields.length] = "storeOperationId";
        withUnavailable[safeFields.length + 1] = "UNAVAILABLE";
        return withUnavailable;
    }

    private static String field(Object[] fields, String key) {
        if (fields == null) {
            return "";
        }
        for (int index = 0; index + 1 < fields.length; index += 2) {
            if (key.equals(String.valueOf(fields[index]))) {
                Object value = fields[index + 1];
                return value == null ? "" : String.valueOf(value);
            }
        }
        return "";
    }

    private static String bound(String value) {
        return value.length() <= 360 ? value : value.substring(0, 360) + "...";
    }
}
