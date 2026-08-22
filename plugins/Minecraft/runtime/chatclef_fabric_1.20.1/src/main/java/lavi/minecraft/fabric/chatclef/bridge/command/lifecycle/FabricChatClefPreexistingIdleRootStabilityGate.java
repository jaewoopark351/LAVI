package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence.FabricChatClefStableRequestQuiescenceObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;

//20260822_kpopmodder: Gate sync-finish idle-root UNKNOWN on stable end-tick ownership evidence.
public final class FabricChatClefPreexistingIdleRootStabilityGate {
    public static final int MIN_DISTINCT_CLIENT_TICKS = 3;
    public static final long MIN_STABLE_DURATION_MS = 500L;
    public static final long MAX_NEWEST_EVIDENCE_AGE_MS = 1000L;

    private static final String POLICY_VERSION = "2026-08-22.preexisting-idle-root-stability.v1";
    private static final String SIGNATURE_VERSION = "preexisting-idle-root-v1";

    private FabricChatClefCommandExecution trackedExecution;
    private String trackedSignature = "";
    private long firstObservedAtNanos;
    private long firstObservedAtMs;
    private long lastObservedAtMs;
    private long firstClientTickId;
    private long lastClientTickId = Long.MIN_VALUE;
    private int distinctTickCount;

    public void reset() {
        trackedExecution = null;
        resetWindow();
    }

    public FabricChatClefStableRequestQuiescenceObservation observe(
            FabricChatClefCommandExecution execution,
            FabricChatClefTaskOwnershipEvidence currentEvidence,
            long nowMs,
            long nowNanos,
            long clientTickId
    ) {
        if (execution != trackedExecution) {
            reset();
            trackedExecution = execution;
        }
        String blockedReason = blockedReason(execution, currentEvidence, nowNanos);
        if (!blockedReason.isEmpty()) {
            resetWindow();
            return observation(false, blockedReason, currentEvidence, nowMs, nowNanos, clientTickId, execution);
        }
        String signature = signature(execution, currentEvidence);
        if (!signature.equals(trackedSignature)) {
            trackedSignature = signature;
            firstObservedAtNanos = nowNanos;
            firstObservedAtMs = nowMs;
            firstClientTickId = clientTickId;
            lastClientTickId = Long.MIN_VALUE;
            distinctTickCount = 0;
        }
        if (clientTickId != lastClientTickId) {
            distinctTickCount++;
            lastClientTickId = clientTickId;
        }
        lastObservedAtMs = nowMs;
        long stableDurationMs = stableDurationMs(nowNanos);
        boolean qualified = distinctTickCount >= MIN_DISTINCT_CLIENT_TICKS
                && stableDurationMs >= MIN_STABLE_DURATION_MS;
        return observation(
                qualified,
                qualified ? "none" : "stable_window_not_satisfied",
                currentEvidence,
                nowMs,
                nowNanos,
                clientTickId,
                execution
        );
    }

    private String blockedReason(
            FabricChatClefCommandExecution execution,
            FabricChatClefTaskOwnershipEvidence currentEvidence,
            long nowNanos
    ) {
        if (execution == null) {
            return "missing_execution";
        }
        if (execution.rootOwnershipClassification()
                != FabricChatClefRootOwnershipClassification.PREEXISTING_UNCHANGED_IDLE_ROOT) {
            return "root_ownership_not_preexisting_idle";
        }
        if (!execution.finishCallbackFirstObservedBeforeDispatchReturn()) {
            return "finish_callback_not_synchronous";
        }
        if (execution.taskFinishedObservation() != null) {
            return "task_finished_event_attached";
        }
        if (currentEvidence == null || !currentEvidence.available()) {
            return "current_ownership_unavailable";
        }
        if (snapshotAgeMs(currentEvidence, nowNanos) > MAX_NEWEST_EVIDENCE_AGE_MS) {
            return "current_ownership_stale";
        }
        FabricChatClefTaskOwnershipEvidence before = execution.taskBeforeDispatchEvidence();
        FabricChatClefTaskOwnershipEvidence after = execution.taskAfterDispatchEvidence();
        if (before == null || after == null || !before.available() || !after.available()) {
            return "dispatch_ownership_unavailable";
        }
        if (currentEvidence.rootTask() != before.rootTask()
                || currentEvidence.rootTask() != after.rootTask()) {
            return "current_root_changed";
        }
        if (!sameAssignmentAndGeneration(after, currentEvidence)) {
            return "assignment_or_generation_changed";
        }
        if (!currentEvidence.userTaskRunningIdle()) {
            return "running_idle_changed";
        }
        if (currentEvidence.nextTaskIdleFlag()) {
            return "next_task_idle_flag_true";
        }
        return "";
    }

    private boolean sameAssignmentAndGeneration(
            FabricChatClefTaskOwnershipEvidence expected,
            FabricChatClefTaskOwnershipEvidence actual
    ) {
        return nullToEmpty(expected.userTaskRootAssignmentId()).equals(nullToEmpty(actual.userTaskRootAssignmentId()))
                && expected.userTaskRootGeneration() == actual.userTaskRootGeneration();
    }

    private String signature(
            FabricChatClefCommandExecution execution,
            FabricChatClefTaskOwnershipEvidence currentEvidence
    ) {
        return System.identityHashCode(execution)
                + "|"
                + execution.requestId()
                + "|"
                + execution.context().sessionId()
                + "|"
                + execution.context().connectionGeneration()
                + "|"
                + execution.context().correlationId()
                + "|root="
                + currentEvidence.userTaskRootIdentity()
                + "|assignment="
                + currentEvidence.userTaskRootAssignmentId()
                + "|generation="
                + currentEvidence.userTaskRootGeneration()
                + "|running_idle="
                + currentEvidence.userTaskRunningIdle()
                + "|next_idle="
                + currentEvidence.nextTaskIdleFlag();
    }

    private FabricChatClefStableRequestQuiescenceObservation observation(
            boolean qualified,
            String blockedReason,
            FabricChatClefTaskOwnershipEvidence currentEvidence,
            long nowMs,
            long nowNanos,
            long clientTickId,
            FabricChatClefCommandExecution execution
    ) {
        long firstMs = firstObservedAtMs == 0L ? nowMs : firstObservedAtMs;
        long lastMs = lastObservedAtMs == 0L ? nowMs : lastObservedAtMs;
        long firstTick = firstClientTickId == 0L ? clientTickId : firstClientTickId;
        long finishAt = execution == null ? 0L : execution.finishCallbackReceivedAtMs();
        long stableDurationMs = stableDurationMs(nowNanos);
        return FabricChatClefStableRequestQuiescenceObservation.of(
                qualified,
                blockedReason,
                distinctTickCount,
                firstMs,
                lastMs,
                firstTick,
                clientTickId,
                stableDurationMs,
                finishAt <= 0L ? -1L : Math.max(0L, nowMs - finishAt),
                snapshotAgeMs(currentEvidence, nowNanos),
                sameSessionGeneration(execution),
                requestRootReappeared(execution, currentEvidence),
                "NEVER_OBSERVED",
                SIGNATURE_VERSION,
                POLICY_VERSION,
                MIN_DISTINCT_CLIENT_TICKS,
                MIN_STABLE_DURATION_MS,
                MAX_NEWEST_EVIDENCE_AGE_MS
        );
    }

    private long snapshotAgeMs(FabricChatClefTaskOwnershipEvidence currentEvidence, long nowNanos) {
        if (currentEvidence == null || currentEvidence.capturedAtNanos() <= 0L) {
            return Long.MAX_VALUE;
        }
        if (nowNanos < currentEvidence.capturedAtNanos()) {
            return Long.MAX_VALUE;
        }
        return (nowNanos - currentEvidence.capturedAtNanos()) / 1_000_000L;
    }

    private boolean sameSessionGeneration(FabricChatClefCommandExecution execution) {
        return execution != null
                && execution == trackedExecution
                && !nullToEmpty(execution.context().sessionId()).isBlank()
                && execution.context().connectionGeneration() >= 0L;
    }

    private boolean requestRootReappeared(
            FabricChatClefCommandExecution execution,
            FabricChatClefTaskOwnershipEvidence currentEvidence
    ) {
        return execution != null
                && currentEvidence != null
                && currentEvidence.rootTaskPresent()
                && execution.hasBoundRootTask()
                && execution.matchesBoundRootTask(currentEvidence.rootTask());
    }

    private long stableDurationMs(long nowNanos) {
        if (firstObservedAtNanos <= 0L) {
            return 0L;
        }
        return Math.max(0L, (nowNanos - firstObservedAtNanos) / 1_000_000L);
    }

    private void resetWindow() {
        trackedSignature = "";
        firstObservedAtNanos = 0L;
        firstObservedAtMs = 0L;
        lastObservedAtMs = 0L;
        firstClientTickId = 0L;
        lastClientTickId = Long.MIN_VALUE;
        distinctTickCount = 0;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
