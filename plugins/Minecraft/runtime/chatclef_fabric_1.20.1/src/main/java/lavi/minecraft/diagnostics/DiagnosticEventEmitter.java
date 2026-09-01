package lavi.minecraft.diagnostics;

import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.formatting.DiagnosticBoundedEventFormatter;
import lavi.minecraft.diagnostics.formatting.DiagnosticBoundedEventText;
import lavi.minecraft.diagnostics.formatting.DiagnosticValueFormatter;
import lavi.minecraft.diagnostics.session.admission.DiagnosticEventFamily;
import lavi.minecraft.diagnostics.session.admission.DiagnosticFamilySnapshot;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionLimits;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionSnapshot;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticBoundedGroupEmission;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticCapEventContext;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticDispatchObserver;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticDispatchResult;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticEventFamilyClassifier;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticSessionRuntime;
import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionSnapshotEventFields;

import java.util.StringJoiner;

//20260803_kpopmodder: Keep diagnostic event text emission separate from the public diagnostics facade.
final class DiagnosticEventEmitter {
    private final DiagnosticTraceState traceState;
    private final DiagnosticTaskRegistry tasks;
    private final DiagnosticSessionRuntime session;

    DiagnosticEventEmitter(DiagnosticTraceState traceState,
                           DiagnosticTaskRegistry tasks,
                           DiagnosticSessionRuntime session) {
        this.traceState = traceState;
        this.tasks = tasks;
        this.session = session;
    }

    void logVerboseEvent(String eventType,
                         String phase,
                         String reason,
                         Task task,
                         Object[] fields,
                         boolean startNewTrace,
                         boolean startTaskRun) {
        session.dispatch(
                DiagnosticEventFamilyClassifier.classify(eventType),
                eventType,
                () -> logVerboseEventPhysical(
                        eventType,
                        phase,
                        reason,
                        task,
                        fields,
                        startNewTrace,
                        startTaskRun
                ),
                this::emitCanonicalCap
        );
    }

    private void logVerboseEventPhysical(String eventType,
                                         String phase,
                                         String reason,
                                         Task task,
                                         Object[] fields,
                                         boolean startNewTrace,
                                         boolean startTaskRun) {
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
    }

    void emitEvent(String level,
                   String prefix,
                   String eventName,
                   String reason,
                   Task task,
                   Object[] fields,
                   boolean warning) {
        emitEventWithOutcome(level, prefix, eventName, reason, task, fields, warning);
    }

    DiagnosticDispatchResult emitEventWithOutcome(String level,
                                                   String prefix,
                                                   String eventName,
                                                   String reason,
                                                   Task task,
                                                   Object[] fields,
                                                   boolean warning) {
        return session.dispatch(
                DiagnosticEventFamilyClassifier.classify(eventName),
                eventName,
                () -> emitEventPhysical(level, prefix, eventName, reason, task, fields, warning),
                this::emitCanonicalCap
        );
    }

    void emitOperationalEvent(String level,
                              String prefix,
                              String eventName,
                              String reason,
                              Task task,
                              Object[] fields,
                              boolean warning) {
        try {
            emitEventPhysical(level, prefix, eventName, reason, task, fields, warning);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    private void emitEventPhysical(String level,
                                   String prefix,
                                   String eventName,
                                   String reason,
                                   Task task,
                                   Object[] fields,
                                   boolean warning) {
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
    }

    void emitBoundedBoundaryEvent(String prefix,
                                  String eventName,
                                  String reason,
                                  Task task,
                                  int maxUtf8Bytes,
                                  Object[] requiredFields,
                                  Object[] optionalFields) {
        emitBoundedBoundaryEventWithOutcome(
                prefix,
                eventName,
                reason,
                task,
                maxUtf8Bytes,
                requiredFields,
                optionalFields
        );
    }

    DiagnosticDispatchResult emitBoundedBoundaryEventWithOutcome(
            String prefix,
            String eventName,
            String reason,
            Task task,
            int maxUtf8Bytes,
            Object[] requiredFields,
            Object[] optionalFields) {
        return session.dispatch(
                DiagnosticEventFamilyClassifier.classify(eventName),
                eventName,
                () -> emitBoundedBoundaryEventPhysical(
                        prefix,
                        eventName,
                        reason,
                        task,
                        maxUtf8Bytes,
                        requiredFields,
                        optionalFields
                ),
                this::emitCanonicalCap
        );
    }

    DiagnosticDispatchResult emitCriticalBoundedGroup(
            DiagnosticEventFamily family,
            String groupEventName,
            DiagnosticBoundedGroupEmission groupEmission,
            DiagnosticDispatchObserver observer) {
        if (!family.groupFamily()) {
            throw new IllegalArgumentException("Atomic bounded group requires a group family: " + family);
        }
        return session.dispatch(
                family,
                groupEventName,
                () -> {
                    lavi.minecraft.diagnostics.session.runtime.DiagnosticBoundedGroupEmissionGuard guard =
                            new lavi.minecraft.diagnostics.session.runtime.DiagnosticBoundedGroupEmissionGuard(
                                    family.admissionUnitSlots()
                            );
                    groupEmission.emit(guard.guard(
                            (eventName, reason, task, maxUtf8Bytes, requiredFields, optionalFields) ->
                                    emitBoundedBoundaryEventPhysical(
                                "[LAVI ChatClefBoundary]",
                                eventName,
                                reason,
                                task,
                                maxUtf8Bytes,
                                requiredFields,
                                optionalFields
                                    )
                    ));
                    guard.verifyComplete();
                },
                this::emitCanonicalCap,
                observer
        );
    }

    void emitRawLine(String eventName, String message, boolean warning) {
        session.dispatch(
                DiagnosticEventFamilyClassifier.classify(eventName),
                eventName,
                () -> {
                    if (warning) {
                        Debug.logWarning(message);
                    } else {
                        System.out.println("ALTO CLEF: " + DiagnosticValueFormatter.value(message));
                    }
                },
                this::emitCanonicalCap
        );
    }

    void emitCleanTeardownFinalSnapshotPhysical(DiagnosticSessionSnapshot snapshot,
                                                Object[] lifecycleFields) {
        emitBoundedBoundaryEventPhysical(
                "[LAVI ChatClefBoundary]",
                "DIAGNOSTIC_SESSION_FINAL_SNAPSHOT",
                "clean_teardown",
                null,
                8192,
                DiagnosticSessionSnapshotEventFields.requiredFields(
                        snapshot,
                        "CLEAN_TEARDOWN"
                ),
                lifecycleFields
        );
    }

    private void emitBoundedBoundaryEventPhysical(String prefix,
                                                  String eventName,
                                                  String reason,
                                                  Task task,
                                                  int maxUtf8Bytes,
                                                  Object[] requiredFields,
                                                  Object[] optionalFields) {
            DiagnosticEventIdentity eventIdentity = traceState.nextEventIdentity(false);
            long taskInstanceId = task == null ? -1 : tasks.instanceId(task);
            long taskRunId = task == null ? -1 : tasks.existingRunId(task);
            long parentTaskRunId = task == null ? -1 : tasks.parentRunId(task);
            Object[] baseFields = new Object[]{
                    "traceId", eventIdentity.traceId(),
                    "clientTickId", eventIdentity.clientTickId(),
                    "eventSequence", eventIdentity.eventSequence(),
                    "taskInstanceId", DiagnosticTaskRegistry.idLabel(taskInstanceId),
                    "taskRunId", DiagnosticTaskRegistry.idLabel(taskRunId),
                    "parentTaskRunId", DiagnosticTaskRegistry.idLabel(parentTaskRunId),
                    "threadName", Thread.currentThread().getName(),
                    "level", "BOUNDARY",
                    "event", eventName,
                    "reason", reason,
                    "taskClass", taskName(task)
            };
            DiagnosticBoundedEventText encoded = DiagnosticBoundedEventFormatter.format(
                    "ALTO CLEF: " + prefix + " ",
                    baseFields,
                    requiredFields,
                    optionalFields,
                    maxUtf8Bytes
            );
            System.out.println(encoded.text());
    }

    private void emitCanonicalCap(DiagnosticCapEventContext context) {
        DiagnosticSessionSnapshot snapshot = context.snapshotAfterCapAdmission();
        DiagnosticFamilySnapshot rejected = snapshot.family(context.rejectedFamily());
        long admittedAfter = snapshot.admittedSlots();
        long criticalAfter = snapshot.criticalSlotsUsed();
        emitEventPhysical(
                "BOUNDARY",
                "[LAVI ChatClefBoundary]",
                "DIAGNOSTIC_SESSION_CAP_REACHED",
                "shared_diagnostic_session_cap_reached",
                null,
                new Object[]{
                        "diagnosticSessionId", snapshot.diagnosticSessionId(),
                        "hardCap", DiagnosticSessionLimits.HARD_CAP,
                        "ordinaryBudget", DiagnosticSessionLimits.ORDINARY_CEILING,
                        "trigger", context.trigger(),
                        "admittedTotalBefore", Math.max(0L, admittedAfter - 1L),
                        "admittedTotalAfter", admittedAfter,
                        "ordinaryUsedBefore", snapshot.ordinarySlotsUsed(),
                        "ordinaryUsedAfter", snapshot.ordinarySlotsUsed(),
                        "criticalUsedBefore", Math.max(0L, criticalAfter - 1L),
                        "criticalUsedAfter", criticalAfter,
                        "reserveRemainingBefore", Math.min(
                                DiagnosticSessionLimits.CRITICAL_RESERVE,
                                snapshot.criticalReserveRemaining() + 1L
                        ),
                        "reserveRemainingAfter", snapshot.criticalReserveRemaining(),
                        "firstSuppressedEvent", context.firstSuppressedEvent(),
                        "firstSuppressedSubsystem", context.rejectedFamily(),
                        "rejectedPriority", context.rejectedFamily().tier(),
                        "rejectedFamily", context.rejectedFamily(),
                        "familyAdmittedSlots", rejected == null ? 0L : rejected.admittedSlots(),
                        "familySuppressedRequests", rejected == null ? 0L : rejected.suppressedRequests(),
                        "behavior_effect", "none"
                },
                false
        );
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
        log.add(key + "=" + lavi.minecraft.diagnostics.formatting.DiagnosticFieldValueEncoder.encode(fieldValue));
    }
}
