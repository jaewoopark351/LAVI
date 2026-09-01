package lavi.minecraft.diagnostics.crafting.acquisition.terminal;

import java.util.Objects;

//20260901_kpopmodder: Aggregate existing terminal evidence in two phases without changing lifecycle.
public final class CraftResourceTerminalLedger {
    private static final long RETENTION_TICKS = 200L;
    private static final long RETENTION_NANOS = 10_000_000_000L;

    private final CraftResourceTerminalKey key;
    private final long createdAtTick;
    private final long createdAtNanos;

    private CraftResourcePrimaryTerminationCause primaryCause =
            CraftResourcePrimaryTerminationCause.UNKNOWN;
    private long triggerTick = -1L;
    private long triggerNanos = -1L;
    private String detachReason = "";
    private boolean connectionDetachedObserved;
    private long cancelInvocationId = -1L;
    private String taskTerminationKind = "UNAVAILABLE";
    private String terminalDecisionReason = "UNAVAILABLE";
    private boolean naturalTaskFinished;
    private boolean timedOutAtFinalization;
    private boolean timedOutEver;
    private long firstTimedOutTick = -1L;
    private long lastTimedOutTick = -1L;
    private String classifiedResultStatus = "UNAVAILABLE";
    private String classifiedResultReason = "UNAVAILABLE";
    private String classifiedResultFidelity = "UNAVAILABLE";
    private String evidenceConclusion = "UNAVAILABLE";
    private CraftResourceResultSendStatus sendStatus = CraftResourceResultSendStatus.NOT_ATTEMPTED;
    private CraftResourceResultDeliveryStatus deliveryStatus =
            CraftResourceResultDeliveryStatus.UNKNOWN;
    private boolean terminalSent;
    private boolean sendInFlight;
    private boolean lifecycleCleared;
    private CraftResourceLifecycleClearKind lifecycleClearKind =
            CraftResourceLifecycleClearKind.UNAVAILABLE;
    private boolean queueContextCleared;
    private String contextUnbindReason = "";
    private boolean taskFinishObserved;
    private boolean classificationObserved;
    private boolean sendOutcomeObserved;
    private boolean finalized;
    private CraftResourceFinalizationMode finalizationMode = CraftResourceFinalizationMode.PENDING;
    private CraftResourceCoverageStatus coverageStatus = CraftResourceCoverageStatus.PENDING;
    private long elapsedTicks;
    private long elapsedNanos;
    private long terminalAdmissionRequestCount;
    private boolean admissionOutcomeRecorded;
    private boolean terminalEmissionAttempted;
    private boolean terminalEmissionAdmitted;
    private boolean terminalEmissionCompleted;
    private long lateOrDuplicateSignalCount;
    private boolean counterSaturated;

    public CraftResourceTerminalLedger(
            CraftResourceTerminalKey key,
            long createdAtTick,
            long createdAtNanos
    ) {
        this.key = Objects.requireNonNull(key, "key");
        this.createdAtTick = createdAtTick;
        this.createdAtNanos = createdAtNanos;
    }

    public synchronized CraftResourceTerminalDecision recordDetach(
            String reason,
            long clientTick,
            long monotonicNanos
    ) {
        if (finalized) {
            return late("DETACH_LATE");
        }
        connectionDetachedObserved = true;
        freezeCause(
                CraftResourcePrimaryTerminationCause.CONNECTION_DETACH_CANCEL,
                clientTick,
                monotonicNanos
        );
        if (detachReason.isEmpty()) {
            detachReason = safe(reason);
        }
        return pending("DETACH_CAUSE_FROZEN");
    }

    public synchronized CraftResourceTerminalDecision recordPrimaryCause(
            CraftResourcePrimaryTerminationCause cause,
            long clientTick,
            long monotonicNanos
    ) {
        if (finalized) {
            return late("PRIMARY_CAUSE_LATE");
        }
        if (cause == null || cause == CraftResourcePrimaryTerminationCause.UNKNOWN) {
            return pending("PRIMARY_CAUSE_UNAVAILABLE");
        }
        boolean causeWasUnknown = primaryCause == CraftResourcePrimaryTerminationCause.UNKNOWN;
        freezeCause(cause, clientTick, monotonicNanos);
        return pending(causeWasUnknown
                ? "PRIMARY_CAUSE_FROZEN"
                : "PRIMARY_CAUSE_ALREADY_FROZEN");
    }

    public synchronized CraftResourceTerminalDecision recordOwnedRootCancellation(
            long invocationId,
            String terminationKind,
            long clientTick,
            long monotonicNanos
    ) {
        if (finalized) {
            return late("CANCELLATION_LATE");
        }
        freezeCause(CraftResourcePrimaryTerminationCause.EXPLICIT_CANCEL, clientTick, monotonicNanos);
        if (cancelInvocationId < 0L) {
            cancelInvocationId = invocationId;
        }
        String boundedTerminationKind = safe(terminationKind);
        if ("UNAVAILABLE".equals(taskTerminationKind)
                && !boundedTerminationKind.isEmpty()) {
            taskTerminationKind = boundedTerminationKind;
        }
        return pending("OWNED_ROOT_CANCELLATION_OBSERVED");
    }

    public synchronized CraftResourceTerminalDecision recordTaskFinished(
            CraftResourceTaskFinishMatch match,
            String terminationKind,
            boolean thisOrChildTimedOut,
            long clientTick,
            long monotonicNanos
    ) {
        if (finalized) {
            return late("TASK_FINISHED_LATE");
        }
        CraftResourceTaskFinishMatch safeMatch = match == null
                ? CraftResourceTaskFinishMatch.UNAVAILABLE
                : match;
        taskFinishObserved = true;
        if (safeMatch == CraftResourceTaskFinishMatch.MATCHING_ROOT) {
            freezeCause(
                    CraftResourcePrimaryTerminationCause.NATURAL_TASK_FINISH,
                    clientTick,
                    monotonicNanos
            );
            naturalTaskFinished = true;
        }
        String boundedTerminationKind = safe(terminationKind);
        if (!boundedTerminationKind.isEmpty()) {
            taskTerminationKind = boundedTerminationKind;
        }
        if (thisOrChildTimedOut) {
            timedOutEver = true;
            if (firstTimedOutTick < 0L) {
                firstTimedOutTick = clientTick;
            }
            lastTimedOutTick = clientTick;
        }
        timedOutAtFinalization = thisOrChildTimedOut;
        return maybeFinalize("TASK_FINISHED_OBSERVED");
    }

    public synchronized CraftResourceTerminalDecision recordClassification(
            String resultStatus,
            String resultReason,
            String resultFidelity,
            String conclusion
    ) {
        return recordClassification(
                "UNAVAILABLE",
                resultStatus,
                resultReason,
                resultFidelity,
                conclusion
        );
    }

    public synchronized CraftResourceTerminalDecision recordClassification(
            String observedTerminalDecisionReason,
            String resultStatus,
            String resultReason,
            String resultFidelity,
            String conclusion
    ) {
        if (finalized) {
            return late("CLASSIFICATION_LATE");
        }
        terminalDecisionReason = unavailableIfBlank(observedTerminalDecisionReason);
        classifiedResultStatus = unavailableIfBlank(resultStatus);
        classifiedResultReason = unavailableIfBlank(resultReason);
        classifiedResultFidelity = unavailableIfBlank(resultFidelity);
        evidenceConclusion = unavailableIfBlank(conclusion);
        classificationObserved = true;
        return maybeFinalize("CLASSIFICATION_OBSERVED");
    }

    public synchronized CraftResourceTerminalDecision recordSendOutcome(
            CraftResourceResultSendStatus resultSendStatus,
            CraftResourceResultDeliveryStatus resultDeliveryStatus,
            boolean sent,
            boolean inFlight
    ) {
        if (finalized) {
            return late("SEND_OUTCOME_LATE");
        }
        sendStatus = resultSendStatus == null ? CraftResourceResultSendStatus.UNKNOWN : resultSendStatus;
        deliveryStatus = resultDeliveryStatus == null
                ? CraftResourceResultDeliveryStatus.UNKNOWN
                : resultDeliveryStatus;
        terminalSent = sent;
        sendInFlight = inFlight;
        sendOutcomeObserved = true;
        return maybeFinalize("SEND_OUTCOME_OBSERVED");
    }

    public synchronized CraftResourceTerminalDecision recordLifecycleCleared(
            CraftResourceLifecycleClearKind clearKind
    ) {
        if (finalized) {
            return late("LIFECYCLE_CLEAR_LATE");
        }
        lifecycleCleared = true;
        lifecycleClearKind = clearKind == null
                ? CraftResourceLifecycleClearKind.UNAVAILABLE
                : clearKind;
        return maybeFinalize("LIFECYCLE_CLEARED");
    }

    public synchronized CraftResourceTerminalDecision recordQueueContextCleared(
            boolean mutationApplied,
            String unbindReason
    ) {
        if (finalized) {
            return late("QUEUE_CONTEXT_CLEAR_LATE");
        }
        if (mutationApplied) {
            queueContextCleared = true;
            contextUnbindReason = safe(unbindReason);
        }
        return maybeFinalize("QUEUE_CONTEXT_CLEAR_OBSERVED");
    }

    public synchronized CraftResourceTerminalDecision observeRetention(
            long clientTick,
            long monotonicNanos
    ) {
        if (finalized || triggerTick < 0L || triggerNanos < 0L) {
            return pending(finalized ? "ALREADY_FINALIZED" : "NO_TERMINAL_TRIGGER");
        }
        boolean tickExpired = elapsedAtLeast(clientTick, triggerTick, RETENTION_TICKS);
        boolean timeExpired = elapsedAtLeast(monotonicNanos, triggerNanos, RETENTION_NANOS);
        if (!tickExpired && !timeExpired) {
            return pending("RETENTION_OPEN");
        }
        elapsedTicks = boundedElapsed(clientTick, triggerTick);
        elapsedNanos = boundedElapsed(monotonicNanos, triggerNanos);
        return finalizeOnce(
                CraftResourceFinalizationMode.DIAGNOSTIC_FALLBACK_INCOMPLETE,
                CraftResourceCoverageStatus.INCOMPLETE_TERMINAL_CLEANUP_UNOBSERVED,
                "RETENTION_FALLBACK"
        );
    }

    public synchronized boolean recordAdmissionOutcome(boolean admitted) {
        return recordAdmissionOutcome(admitted, admitted);
    }

    public synchronized boolean recordAdmissionOutcome(
            boolean admitted,
            boolean emissionCompleted) {
        if (!finalized || admissionOutcomeRecorded) {
            return false;
        }
        admissionOutcomeRecorded = true;
        terminalEmissionAttempted = true;
        terminalEmissionAdmitted = admitted;
        terminalEmissionCompleted = emissionCompleted;
        return true;
    }

    public synchronized CraftResourceTerminalDecision recordLateSignal(String signal) {
        return late(unavailableIfBlank(signal));
    }

    public synchronized CraftResourceTerminalSnapshot snapshot() {
        return new CraftResourceTerminalSnapshot(
                key,
                finalized,
                primaryCause,
                detachReason,
                connectionDetachedObserved,
                cancelInvocationId,
                taskTerminationKind,
                terminalDecisionReason,
                naturalTaskFinished,
                timedOutAtFinalization,
                timedOutEver,
                firstTimedOutTick,
                lastTimedOutTick,
                classifiedResultStatus,
                classifiedResultReason,
                classifiedResultFidelity,
                evidenceConclusion,
                sendStatus,
                deliveryStatus,
                terminalSent,
                sendInFlight,
                lifecycleCleared,
                lifecycleClearKind,
                queueContextCleared,
                contextUnbindReason,
                taskFinishObserved,
                classificationObserved,
                sendOutcomeObserved,
                finalizationMode,
                coverageStatus,
                elapsedTicks,
                elapsedNanos,
                terminalAdmissionRequestCount,
                terminalEmissionAttempted,
                terminalEmissionAdmitted,
                terminalEmissionCompleted,
                lateOrDuplicateSignalCount,
                counterSaturated
        );
    }

    private CraftResourceTerminalDecision maybeFinalize(String pendingReason) {
        if (primaryCause == CraftResourcePrimaryTerminationCause.CONNECTION_DETACH_CANCEL
                && taskFinishObserved
                && classificationObserved
                && sendOutcomeObserved
                && lifecycleCleared
                && lifecycleClearKind == CraftResourceLifecycleClearKind.CONNECTION_DETACHED
                && queueContextCleared
                && !sendInFlight) {
            return finalizeOnce(
                    CraftResourceFinalizationMode.OBSERVED_BARRIER,
                    CraftResourceCoverageStatus.COMPLETE,
                    "DETACH_CLEANUP_BARRIER"
            );
        }
        if (primaryCause == CraftResourcePrimaryTerminationCause.NATURAL_TASK_FINISH
                && taskFinishObserved
                && classificationObserved
                && sendOutcomeObserved
                && terminalSent
                && lifecycleCleared
                && lifecycleClearKind == CraftResourceLifecycleClearKind.NORMAL_TERMINAL_RESULT_SENT
                && queueContextCleared
                && !sendInFlight) {
            return finalizeOnce(
                    CraftResourceFinalizationMode.OBSERVED_BARRIER,
                    CraftResourceCoverageStatus.COMPLETE,
                    "NORMAL_TERMINAL_BARRIER"
            );
        }
        return pending(pendingReason);
    }

    private CraftResourceTerminalDecision finalizeOnce(
            CraftResourceFinalizationMode mode,
            CraftResourceCoverageStatus coverage,
            String reason
    ) {
        if (finalized) {
            return pending("ALREADY_FINALIZED");
        }
        finalized = true;
        finalizationMode = mode;
        coverageStatus = coverage;
        terminalAdmissionRequestCount = increment(terminalAdmissionRequestCount);
        return new CraftResourceTerminalDecision(true, true, reason, snapshot());
    }

    private CraftResourceTerminalDecision pending(String reason) {
        return new CraftResourceTerminalDecision(finalized, false, reason, snapshot());
    }

    private CraftResourceTerminalDecision late(String reason) {
        lateOrDuplicateSignalCount = increment(lateOrDuplicateSignalCount);
        return new CraftResourceTerminalDecision(finalized, false, reason, snapshot());
    }

    private void freezeCause(
            CraftResourcePrimaryTerminationCause cause,
            long clientTick,
            long monotonicNanos
    ) {
        if (primaryCause != CraftResourcePrimaryTerminationCause.UNKNOWN) {
            return;
        }
        primaryCause = cause;
        triggerTick = clientTick;
        triggerNanos = monotonicNanos;
    }

    private long increment(long value) {
        if (value == Long.MAX_VALUE) {
            counterSaturated = true;
            return value;
        }
        return value + 1L;
    }

    private static boolean elapsedAtLeast(long current, long start, long bound) {
        return current >= start && current - start >= bound;
    }

    private static long boundedElapsed(long current, long start) {
        return current >= start ? current - start : 0L;
    }

    private static String safe(String value) {
        return CraftResourceTerminalTextBound.safe(value);
    }

    private static String unavailableIfBlank(String value) {
        return CraftResourceTerminalTextBound.unavailableIfBlank(value);
    }
}
