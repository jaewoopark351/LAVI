package lavi.minecraft.diagnostics.container.home.timeout.budget;

import java.util.LinkedHashMap;
import java.util.Map;

//20260828_kpopmodder: Bound only the JVM-session STORE_HOME timeout diagnostic family.
public final class StoreHomeSessionLogBudget {
    private static final int NON_RESERVED_LIMIT =
            StoreHomeDiagnosticLimits.SESSION_HARD_CAP
                    - StoreHomeDiagnosticLimits.SESSION_RESERVED_BOUNDARY_CAP;

    private final Map<String, Integer> suppressedByEvent = new LinkedHashMap<>();
    private int emittedCount;
    private int suppressedCount;
    private boolean capEventEmitted;
    private String firstSuppressedEvent = "none";
    private String lastSuppressedEvent = "none";

    public synchronized boolean canObserveProgress() {
        return emittedCount < NON_RESERVED_LIMIT;
    }

    public synchronized boolean recordProgressSuppressedWhenUnavailable(
            String eventName) {
        if (emittedCount >= NON_RESERVED_LIMIT) {
            incrementSuppressed(eventName);
            return true;
        }
        return false;
    }

    public synchronized StoreHomeLogBudgetDecision reserve(
            String eventName,
            boolean boundary,
            boolean terminal) {
        if (!boundary) {
            if (emittedCount < NON_RESERVED_LIMIT) {
                emittedCount++;
                return decision(true, false);
            }
            incrementSuppressed(eventName);
            return reserveCapIfPossible(false);
        }

        if (terminal) {
            if (!capEventEmitted
                    && emittedCount == StoreHomeDiagnosticLimits.SESSION_HARD_CAP - 3) {
                capEventEmitted = true;
                emittedCount += 2;
                return decision(true, true);
            }
            if (emittedCount < StoreHomeDiagnosticLimits.SESSION_HARD_CAP) {
                emittedCount++;
                return decision(true, false);
            }
            incrementSuppressed(eventName);
            return decision(false, false);
        }

        int capReservation = capEventEmitted ? 0 : 1;
        int terminalReservation = 1;
        int exceptionReservation = 1;
        int nonTerminalLimit = StoreHomeDiagnosticLimits.SESSION_HARD_CAP
                - capReservation
                - terminalReservation
                - exceptionReservation;
        if (emittedCount < nonTerminalLimit) {
            emittedCount++;
            return decision(true, false);
        }

        incrementSuppressed(eventName);
        return reserveCapIfPossible(false);
    }

    public synchronized Object[] summaryFields() {
        return new Object[]{
                "storeHomeDiagnosticSessionEmittedCount", emittedCount,
                "storeHomeDiagnosticSessionHardCap",
                StoreHomeDiagnosticLimits.SESSION_HARD_CAP,
                "storeHomeDiagnosticSessionReservedBoundaryCap",
                StoreHomeDiagnosticLimits.SESSION_RESERVED_BOUNDARY_CAP,
                "storeHomeDiagnosticSessionCapEventEmitted", capEventEmitted,
                "storeHomeDiagnosticSessionSuppressedEventCount", suppressedCount,
                "storeHomeDiagnosticSessionSuppressedByEvent",
                suppressedByEvent.toString(),
                "storeHomeDiagnosticSessionFirstSuppressedEvent",
                firstSuppressedEvent,
                "storeHomeDiagnosticSessionLastSuppressedEvent",
                lastSuppressedEvent,
                "storeHomeDiagnosticSessionTerminalReservationAvailable",
                emittedCount < StoreHomeDiagnosticLimits.SESSION_HARD_CAP - 1,
                "storeHomeDiagnosticSessionExceptionReservationAvailable",
                emittedCount < StoreHomeDiagnosticLimits.SESSION_HARD_CAP,
                "firstUniqueExceptionOwnedByExistingErrorChannel", false,
                "firstUniqueExceptionObservationStatus",
                "SEPARATE_EXISTING_CHANNEL_NOT_SESSION_BUDGET_INTEGRATED",
                "exceptionSignatureDedupeCount",
                "unavailable_not_integrated_with_store_home_session"
        };
    }

    private StoreHomeLogBudgetDecision reserveCapIfPossible(boolean emitOriginal) {
        if (!capEventEmitted
                && emittedCount < StoreHomeDiagnosticLimits.SESSION_HARD_CAP - 2) {
            capEventEmitted = true;
            emittedCount++;
            return decision(emitOriginal, true);
        }
        return decision(emitOriginal, false);
    }

    private StoreHomeLogBudgetDecision decision(
            boolean emitOriginal,
            boolean emitCap) {
        return new StoreHomeLogBudgetDecision(
                emitOriginal,
                emitCap,
                emittedCount,
                capEventEmitted,
                suppressedCount,
                suppressedByEvent.toString(),
                firstSuppressedEvent,
                lastSuppressedEvent,
                emittedCount < StoreHomeDiagnosticLimits.SESSION_HARD_CAP - 1,
                emittedCount < StoreHomeDiagnosticLimits.SESSION_HARD_CAP
        );
    }

    private void incrementSuppressed(String rawEventName) {
        String eventName = normalize(rawEventName);
        if (!suppressedByEvent.containsKey(eventName)
                && suppressedByEvent.size()
                >= StoreHomeDiagnosticLimits.MAX_SUPPRESSION_FAMILIES) {
            eventName = "other";
        }
        int current = suppressedByEvent.getOrDefault(eventName, 0);
        suppressedByEvent.put(
                eventName,
                current == Integer.MAX_VALUE ? current : current + 1
        );
        suppressedCount = suppressedCount == Integer.MAX_VALUE
                ? suppressedCount
                : suppressedCount + 1;
        if ("none".equals(firstSuppressedEvent)) {
            firstSuppressedEvent = eventName;
        }
        lastSuppressedEvent = eventName;
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
