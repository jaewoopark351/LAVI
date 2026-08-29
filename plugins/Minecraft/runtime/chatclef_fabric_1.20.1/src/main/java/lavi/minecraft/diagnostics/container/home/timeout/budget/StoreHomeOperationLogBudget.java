package lavi.minecraft.diagnostics.container.home.timeout.budget;

import java.util.LinkedHashMap;
import java.util.Map;

//20260828_kpopmodder: Own only one STORE_HOME operation's diagnostic emission and suppression counters.
public final class StoreHomeOperationLogBudget {
    private static final int NON_RESERVED_LIMIT =
            StoreHomeDiagnosticLimits.OPERATION_HARD_CAP
                    - StoreHomeDiagnosticLimits.OPERATION_RESERVED_BOUNDARY_CAP;

    private final Map<String, Integer> progressByCandidate = new LinkedHashMap<>();
    private final Map<String, Integer> suppressedByEvent = new LinkedHashMap<>();
    private final Map<String, Integer> sessionSuppressedByEvent = new LinkedHashMap<>();
    private int acceptedCount;
    private int progressAcceptedCount;
    private int suppressedCount;
    private int sessionSuppressedCount;
    private boolean operationCapEventEmitted;
    private String firstSuppressedEvent = "none";
    private String lastSuppressedEvent = "none";

    public synchronized boolean canAcquireBoundary(boolean terminal) {
        int capReservation = operationCapEventEmitted ? 0 : 1;
        int terminalReservation = terminal ? 0 : 1;
        int exceptionReservation = 1;
        int limit = StoreHomeDiagnosticLimits.OPERATION_HARD_CAP
                - capReservation
                - terminalReservation
                - exceptionReservation;
        return acceptedCount < limit;
    }

    public synchronized void commitBoundary() {
        acceptedCount++;
    }

    public synchronized boolean canAcquireProgress(String candidateId) {
        String normalizedCandidate = normalize(candidateId);
        int candidateCount = progressByCandidate.getOrDefault(normalizedCandidate, 0);
        return acceptedCount < NON_RESERVED_LIMIT
                && progressAcceptedCount < StoreHomeDiagnosticLimits.OPERATION_PROGRESS_EVENT_CAP
                && candidateCount < StoreHomeDiagnosticLimits.CANDIDATE_PROGRESS_EVENT_CAP;
    }

    public synchronized void commitProgress(String candidateId) {
        String normalizedCandidate = normalize(candidateId);
        int candidateCount = progressByCandidate.getOrDefault(normalizedCandidate, 0);
        progressByCandidate.put(normalizedCandidate, candidateCount + 1);
        progressAcceptedCount++;
        acceptedCount++;
    }

    public synchronized boolean canObserveProgress(String candidateId) {
        return canAcquireProgress(candidateId);
    }

    public synchronized void recordProgressSuppressed(String eventName) {
        recordSuppressed(eventName);
    }

    public synchronized void recordBoundarySuppressed(String eventName) {
        recordSuppressed(eventName);
    }

    public synchronized void recordSessionSuppressed(String eventName) {
        increment(sessionSuppressedByEvent, eventName);
        sessionSuppressedCount = increment(sessionSuppressedCount);
    }

    public synchronized boolean canAcquireOperationCapEvent() {
        return !operationCapEventEmitted
                && acceptedCount < StoreHomeDiagnosticLimits.OPERATION_HARD_CAP - 2;
    }

    public synchronized boolean tryAcquireOperationCapEvent() {
        if (!canAcquireOperationCapEvent()) {
            return false;
        }
        operationCapEventEmitted = true;
        acceptedCount++;
        return true;
    }

    public synchronized Object[] summaryFields() {
        return new Object[]{
                "operationDiagnosticAcceptedCount", acceptedCount,
                "operationDiagnosticHardCap", StoreHomeDiagnosticLimits.OPERATION_HARD_CAP,
                "operationDiagnosticReservedBoundaryCap",
                StoreHomeDiagnosticLimits.OPERATION_RESERVED_BOUNDARY_CAP,
                "operationProgressAcceptedCount", progressAcceptedCount,
                "operationProgressEventCap",
                StoreHomeDiagnosticLimits.OPERATION_PROGRESS_EVENT_CAP,
                "candidateProgressEventCap",
                StoreHomeDiagnosticLimits.CANDIDATE_PROGRESS_EVENT_CAP,
                "operationDiagnosticCapEventEmitted", operationCapEventEmitted,
                "operationDiagnosticSuppressedEventCount", suppressedCount,
                "operationSuppressedByEvent", suppressedByEvent.toString(),
                "operationFirstSuppressedEvent", firstSuppressedEvent,
                "operationLastSuppressedEvent", lastSuppressedEvent,
                "operationTerminalReservationAvailable",
                acceptedCount < StoreHomeDiagnosticLimits.OPERATION_HARD_CAP - 1,
                "operationExceptionReservationAvailable",
                acceptedCount < StoreHomeDiagnosticLimits.OPERATION_HARD_CAP,
                "firstUniqueExceptionOwnedByExistingErrorChannel", false,
                "firstUniqueExceptionObservationStatus",
                "SEPARATE_EXISTING_CHANNEL_NOT_OPERATION_BUDGET_INTEGRATED",
                "exceptionSignatureDedupeCount",
                "unavailable_not_integrated_with_store_home_operation",
                "sessionSuppressedEventCountForOperation", sessionSuppressedCount,
                "sessionSuppressedByEvent", sessionSuppressedByEvent.toString()
        };
    }

    public synchronized Object[] capSummaryFields() {
        return new Object[]{
                "emittedEventCount", acceptedCount,
                "suppressedEventCount", suppressedCount,
                "suppressedEventCountByFamily", suppressedByEvent.toString(),
                "terminalReservationAvailable",
                acceptedCount < StoreHomeDiagnosticLimits.OPERATION_HARD_CAP - 1,
                "exceptionReservationAvailable",
                acceptedCount < StoreHomeDiagnosticLimits.OPERATION_HARD_CAP
        };
    }

    private void recordSuppressed(String eventName) {
        String normalized = normalize(eventName);
        increment(suppressedByEvent, normalized);
        suppressedCount = increment(suppressedCount);
        if ("none".equals(firstSuppressedEvent)) {
            firstSuppressedEvent = normalized;
        }
        lastSuppressedEvent = normalized;
    }

    private static int increment(int value) {
        return value == Integer.MAX_VALUE ? value : value + 1;
    }

    private static void increment(Map<String, Integer> counts, String rawEventName) {
        String eventName = normalize(rawEventName);
        if (!counts.containsKey(eventName)
                && counts.size() >= StoreHomeDiagnosticLimits.MAX_SUPPRESSION_FAMILIES) {
            eventName = "other";
        }
        int current = counts.getOrDefault(eventName, 0);
        counts.put(eventName, current == Integer.MAX_VALUE ? current : current + 1);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.length() <= StoreHomeDiagnosticLimits.MAX_BUDGET_KEY_LENGTH
                ? value
                : value.substring(0, StoreHomeDiagnosticLimits.MAX_BUDGET_KEY_LENGTH);
    }
}
