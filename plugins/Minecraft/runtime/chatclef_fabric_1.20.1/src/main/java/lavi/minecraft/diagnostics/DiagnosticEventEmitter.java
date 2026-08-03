package lavi.minecraft.diagnostics;

import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.formatting.DiagnosticValueFormatter;

import java.util.StringJoiner;

//20260803_kpopmodder: Keep diagnostic event text emission separate from the public diagnostics facade.
final class DiagnosticEventEmitter {
    private final DiagnosticTraceState traceState;
    private final DiagnosticTaskRegistry tasks;

    DiagnosticEventEmitter(DiagnosticTraceState traceState, DiagnosticTaskRegistry tasks) {
        this.traceState = traceState;
        this.tasks = tasks;
    }

    void logVerboseEvent(String eventType,
                         String phase,
                         String reason,
                         Task task,
                         Object[] fields,
                         boolean startNewTrace,
                         boolean startTaskRun) {
        try {
            StringJoiner log = new StringJoiner(" ");
            DiagnosticEventIdentity eventIdentity = traceState.nextEventIdentity(startNewTrace);
            long taskInstanceId = task == null ? -1 : tasks.instanceId(task);
            long taskRunId = startTaskRun && task != null
                    ? tasks.createRunId(task)
                    : task == null ? -1 : tasks.existingRunId(task);
            long parentTaskRunId = task == null ? -1 : tasks.parentRunId(task);

            append(log, "traceId", eventIdentity.traceId());
            append(log, "clientTickId", eventIdentity.clientTickId());
            append(log, "eventSequence", eventIdentity.eventSequence());
            append(log, "taskInstanceId", DiagnosticTaskRegistry.idLabel(taskInstanceId));
            append(log, "taskRunId", DiagnosticTaskRegistry.idLabel(taskRunId));
            append(log, "parentTaskRunId", DiagnosticTaskRegistry.idLabel(parentTaskRunId));
            append(log, "threadName", Thread.currentThread().getName());
            append(log, "eventType", eventType);
            append(log, "phase", phase);
            append(log, "reason", reason);
            append(log, "taskClass", taskName(task));
            appendPairs(log, DiagnosticScreenState.currentFields());
            appendPairs(log, DiagnosticInputState.currentStateFields());
            appendPairs(log, fields);

            System.out.println("ALTO CLEF: [LAVI ChatClefDiag] " + log);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    void emitEvent(String level,
                   String prefix,
                   String eventName,
                   String reason,
                   Task task,
                   Object[] fields,
                   boolean warning) {
        try {
            StringJoiner log = new StringJoiner(" ");
            DiagnosticEventIdentity eventIdentity = traceState.nextEventIdentity(false);
            long taskInstanceId = task == null ? -1 : tasks.instanceId(task);
            long taskRunId = task == null ? -1 : tasks.existingRunId(task);
            long parentTaskRunId = task == null ? -1 : tasks.parentRunId(task);

            append(log, "traceId", eventIdentity.traceId());
            append(log, "clientTickId", eventIdentity.clientTickId());
            append(log, "eventSequence", eventIdentity.eventSequence());
            append(log, "taskInstanceId", DiagnosticTaskRegistry.idLabel(taskInstanceId));
            append(log, "taskRunId", DiagnosticTaskRegistry.idLabel(taskRunId));
            append(log, "parentTaskRunId", DiagnosticTaskRegistry.idLabel(parentTaskRunId));
            append(log, "threadName", Thread.currentThread().getName());
            append(log, "level", level);
            append(log, "event", eventName);
            append(log, "reason", reason);
            append(log, "taskClass", taskName(task));
            appendPairs(log, fields);

            if (warning) {
                Debug.logWarning(prefix + " " + log);
            } else {
                System.out.println("ALTO CLEF: " + prefix + " " + log);
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    static Object[] mergeFields(Object[] fields, Object... extra) {
        if (fields == null || fields.length == 0) {
            return extra;
        }
        Object[] merged = new Object[fields.length + extra.length];
        System.arraycopy(fields, 0, merged, 0, fields.length);
        System.arraycopy(extra, 0, merged, fields.length, extra.length);
        return merged;
    }

    static String taskName(Task task) {
        if (task == null) {
            return "none";
        }
        try {
            return task.getClass().getName();
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    private static void appendPairs(StringJoiner log, Object... fields) {
        if (fields == null) {
            return;
        }
        for (int i = 0; i < fields.length; i += 2) {
            Object key = fields[i];
            Object fieldValue = i + 1 < fields.length ? fields[i + 1] : "missing";
            append(log, String.valueOf(key), fieldValue);
        }
    }

    private static void append(StringJoiner log, String key, Object fieldValue) {
        log.add(key + "=" + DiagnosticValueFormatter.value(fieldValue));
    }
}
