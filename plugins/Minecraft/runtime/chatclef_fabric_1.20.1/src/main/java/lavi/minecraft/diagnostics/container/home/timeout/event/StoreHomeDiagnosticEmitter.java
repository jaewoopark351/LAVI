package lavi.minecraft.diagnostics.container.home.timeout.event;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.home.timeout.budget.StoreHomeDiagnosticLimits;
import lavi.minecraft.diagnostics.container.home.timeout.budget.StoreHomeLogBudgetDecision;
import lavi.minecraft.diagnostics.container.home.timeout.budget.StoreHomeOperationDiagnosticAggregate;
import lavi.minecraft.diagnostics.container.home.timeout.budget.StoreHomeOperationLogBudget;
import lavi.minecraft.diagnostics.container.home.timeout.budget.StoreHomeSessionLogBudget;

import java.util.Objects;
import java.util.function.Supplier;

//20260828_kpopmodder: Apply STORE_HOME budgets before delegating bounded event text emission.
public final class StoreHomeDiagnosticEmitter {
    private static final String CAP_EVENT = "DIAGNOSTIC_SESSION_CAP_REACHED";
    private static final String OPERATION_CAP_EVENT =
            "STORE_HOME_OPERATION_DIAGNOSTIC_CAP_REACHED";
    private static final StoreHomeSessionLogBudget SESSION_BUDGET =
            new StoreHomeSessionLogBudget();

    private final long operationId;
    private final Supplier<String> runManifestId;
    private final StoreHomeOperationLogBudget operationBudget =
            new StoreHomeOperationLogBudget();
    private final StoreHomeOperationDiagnosticAggregate operationAggregate =
            new StoreHomeOperationDiagnosticAggregate();
    private Object[] frozenCommandContextFields;
    private String frozenRunManifestId;
    private String frozenDiagnosticsMode;

    public StoreHomeDiagnosticEmitter(
            long operationId,
            Supplier<String> runManifestId) {
        this.operationId = operationId;
        this.runManifestId = Objects.requireNonNull(runManifestId, "runManifestId");
    }

    public boolean emitBoundary(
            String eventName,
            String reason,
            Task task,
            Object[] fields) {
        return emit(eventName, reason, task, fields, true, false, null);
    }

    public boolean emitTerminal(
            String eventName,
            String reason,
            Task task,
            Object[] fields) {
        return emit(eventName, reason, task, fields, true, true, null);
    }

    public boolean emitProgress(
            String eventName,
            String reason,
            String candidateId,
            Task task,
            Object[] fields) {
        return emit(eventName, reason, task, fields, false, false, candidateId);
    }

    public boolean canObserveProgress(String candidateId) {
        return operationBudget.canObserveProgress(candidateId)
                && SESSION_BUDGET.canObserveProgress();
    }

    public void recordProgressSuppressed(String eventName) {
        operationBudget.recordProgressSuppressed(eventName);
        if (SESSION_BUDGET.recordProgressSuppressedWhenUnavailable(eventName)) {
            operationBudget.recordSessionSuppressed(eventName);
        }
    }

    public Object[] budgetSummaryFields() {
        return merge(
                operationBudget.summaryFields(),
                operationAggregate.summaryFields(),
                SESSION_BUDGET.summaryFields()
        );
    }

    private boolean emit(
            String eventName,
            String reason,
            Task task,
            Object[] fields,
            boolean boundary,
            boolean terminal,
            String candidateId) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return false;
        }
        try {
            operationAggregate.observe(fields, boundary);
            synchronized (operationBudget) {
                boolean operationPermit = boundary
                        ? operationBudget.canAcquireBoundary(terminal)
                        : operationBudget.canAcquireProgress(candidateId);
                if (!operationPermit) {
                    if (boundary) {
                        operationBudget.recordBoundarySuppressed(eventName);
                        emitOperationCapReached(task, eventName, fields);
                    } else {
                        operationBudget.recordProgressSuppressed(eventName);
                    }
                    return false;
                }

                StoreHomeLogBudgetDecision session = SESSION_BUDGET.reserve(
                        eventName, boundary, terminal
                );
                if (session.emitCap()) {
                    emitCapReached(task, eventName, fields, session);
                }
                if (!session.emitOriginal()) {
                    operationBudget.recordSessionSuppressed(eventName);
                    return false;
                }

                if (boundary) {
                    operationBudget.commitBoundary();
                } else {
                    operationBudget.commitProgress(candidateId);
                }
            }

            emitBounded(eventName, reason, task, fields);
            return true;
        } catch (RuntimeException | LinkageError ignored) {
            // This catches only diagnostic bookkeeping/formatting/emission failures.
            return false;
        }
    }

    private void emitCapReached(
            Task task,
            String suppressedEvent,
            Object[] suppressedFields,
            StoreHomeLogBudgetDecision session) {
        emitBounded(
                CAP_EVENT,
                "store_home_diagnostic_session_cap_reached",
                task,
                merge(
                    selectedAggregateFields(suppressedFields),
                    operationAggregate.summaryFields(),
                    new Object[]{
                        "suppressedEvent", suppressedEvent,
                        "capScope", "STORE_HOME_DIAGNOSTIC_SESSION",
                        "configuredCap", StoreHomeDiagnosticLimits.SESSION_HARD_CAP,
                        "emittedEventCount", session.emittedCount(),
                        "suppressedEventCount", session.suppressedCount(),
                        "suppressedEventCountByFamily", session.suppressedByEvent(),
                        "firstSuppressedEvent", session.firstSuppressedEvent(),
                        "lastSuppressedEvent", session.lastSuppressedEvent(),
                        "terminalReservationAvailable",
                        session.terminalReservationAvailable(),
                        "exceptionReservationAvailable",
                        session.exceptionReservationAvailable(),
                        "reservedBoundaryCap",
                        StoreHomeDiagnosticLimits.SESSION_RESERVED_BOUNDARY_CAP,
                        "capEventEmittedExactlyOnce", session.capEventEmitted(),
                        "taskBehaviorAffected", false
                    }
                )
        );
    }

    private void emitOperationCapReached(
            Task task,
            String suppressedEvent,
            Object[] suppressedFields) {
        if (!operationBudget.canAcquireOperationCapEvent()) {
            return;
        }
        StoreHomeLogBudgetDecision session = SESSION_BUDGET.reserve(
                OPERATION_CAP_EVENT, true, false
        );
        if (session.emitCap()) {
            emitCapReached(task, OPERATION_CAP_EVENT, suppressedFields, session);
        }
        if (!session.emitOriginal()) {
            operationBudget.recordSessionSuppressed(OPERATION_CAP_EVENT);
            return;
        }
        if (!operationBudget.tryAcquireOperationCapEvent()) {
            return;
        }
        emitBounded(
                OPERATION_CAP_EVENT,
                "store_home_operation_diagnostic_cap_reached",
                task,
                merge(
                        selectedAggregateFields(suppressedFields),
                        new Object[]{
                                "suppressedEvent", suppressedEvent,
                                "capScope", "STORE_HOME_OPERATION",
                                "configuredCap",
                                StoreHomeDiagnosticLimits.OPERATION_HARD_CAP,
                                "taskBehaviorAffected", false
                        },
                        operationBudget.capSummaryFields(),
                        operationBudget.summaryFields(),
                        operationAggregate.summaryFields()
                )
        );
    }

    private void emitBounded(
            String eventName,
            String reason,
            Task task,
            Object[] fields) {
        freezeOperationEnvelope();
        Object[] envelope = new Object[]{
                "runManifestId", frozenRunManifestId,
                "operationId", operationId,
                "diagnosticsMode", frozenDiagnosticsMode,
                "eventFingerprint", eventFingerprint(eventName, reason, fields)
        };
        Object[] required = StoreHomeEventValueEncoder.encode(
                merge(envelope, fields, frozenCommandContextFields)
        );
        ChatClefDiagnostics.logBoundedBoundary(
                eventName,
                reason,
                task,
                StoreHomeDiagnosticLimits.MAX_EVENT_UTF8_BYTES,
                required,
                new Object[0]
        );
    }

    private synchronized void freezeOperationEnvelope() {
        if (frozenCommandContextFields != null) {
            return;
        }
        frozenRunManifestId = safeRunManifestId();
        frozenDiagnosticsMode = ChatClefDiagnostics.isVerboseEnabled()
                ? "VERBOSE"
                : "BOUNDARY";
        Object[] captured = ChatClefDiagnostics.withCommandContextFields(
                new Object[0]
        );
        frozenCommandContextFields = captured == null
                ? new Object[0]
                : captured.clone();
    }

    private String safeRunManifestId() {
        try {
            String value = runManifestId.get();
            return value == null || value.isBlank() ? "unavailable" : value;
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    private static Object[] selectedAggregateFields(Object[] fields) {
        String[] selectedKeys = {
                "phase",
                "candidateId",
                "candidateAttemptId",
                "destinationId",
                "candidateOrdinal",
                "candidateAttemptOrdinal",
                "candidateCount",
                "candidateQueueRemaining"
        };
        Object[] selected = new Object[selectedKeys.length * 2];
        for (int index = 0; index < selectedKeys.length; index++) {
            String key = selectedKeys[index];
            selected[index * 2] = key;
            selected[index * 2 + 1] = findField(fields, key);
        }
        return selected;
    }

    private static Object findField(Object[] fields, String key) {
        if (fields == null) {
            return "unavailable";
        }
        for (int index = 0; index + 1 < fields.length; index += 2) {
            if (key.equals(String.valueOf(fields[index]))) {
                return fields[index + 1];
            }
        }
        return "unavailable";
    }

    private String eventFingerprint(
            String eventName,
            String reason,
            Object[] fields) {
        String value = String.join(
                "|",
                fingerprintPart(eventName),
                "operation=" + operationId,
                "candidateAttempt="
                        + fingerprintPart(findField(fields, "candidateAttemptOrdinal")),
                fingerprintPart(findField(fields, "destinationId")),
                fingerprintPart(findField(fields, "phase")),
                fingerprintPart(findField(fields, "childTaskClass")),
                fingerprintPart(findField(fields, "baritonePathingActive")),
                fingerprintPart(findField(fields, "baritoneCalculationState")),
                fingerprintPart(findField(fields, "normalizedGoalType")),
                fingerprintPart(findField(fields, "normalizedGoalTarget")),
                fingerprintPart(findField(fields, "exactBindingMatched")),
                fingerprintPart(reason),
                fingerprintPart(findField(fields, "coarseDistanceBucket"))
        );
        return value.length() <= 768 ? value : value.substring(0, 768);
    }

    private static String fingerprintPart(Object value) {
        String text = value == null ? "unavailable" : String.valueOf(value);
        String flattened = text
                .replace('|', '_')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ');
        return flattened.length() <= 96
                ? flattened
                : flattened.substring(0, 96);
    }

    public static Object[] merge(Object[]... arrays) {
        int length = 0;
        for (Object[] array : arrays) {
            if (array != null) {
                length += array.length;
            }
        }
        Object[] result = new Object[length];
        int offset = 0;
        for (Object[] array : arrays) {
            if (array == null) {
                continue;
            }
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }
}
